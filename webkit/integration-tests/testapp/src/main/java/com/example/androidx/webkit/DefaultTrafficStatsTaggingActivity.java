/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidx.webkit;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.concurrent.Executors;

public class DefaultTrafficStatsTaggingActivity extends AppCompatActivity {

    private static final int TRAFFIC_TAG = 1234567;
    private static final String URL = "https://example.com";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_default_traffic_tagging);
        WebkitHelpers.enableEdgeToEdge(this);
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DEFAULT_TRAFFICSTATS_TAGGING)) {
            WebkitHelpers.showMessageInActivity(DefaultTrafficStatsTaggingActivity.this,
                    R.string.default_trafficstats_tagging_unsupported);
            return;
        }

        setupWebView();
        findViewById(R.id.fetch_traffic_stats).setOnClickListener(this::fetchTrafficStats);
    }

    private void setupWebView() {
        WebView webView = findViewById(R.id.default_trafficstats_tagging_webview);
        WebViewCompat.setDefaultTrafficStatsTag(TRAFFIC_TAG);
        webView.loadUrl(URL);
    }

    private void fetchTrafficStats(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            showToast("Unable to fetch stats tag. Require Android N+");
            return;
        }

        // Fetch stats in background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            NetworkStatsManager statsManager = this.getSystemService(NetworkStatsManager.class);
            NetworkStats.Bucket bucket = fetchTrafficStatsBucket(
                    NetworkCapabilities.TRANSPORT_CELLULAR, statsManager);
            String transportType = "Cellular";
            if (bucket == null) {
                bucket = fetchTrafficStatsBucket(NetworkCapabilities.TRANSPORT_WIFI, statsManager);
                transportType = "WiFi";
            }

            String text = makeBytesTransferredText(transportType, bucket);
            runOnUiThread(() -> showToast(text));
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private NetworkStats.@Nullable Bucket fetchTrafficStatsBucket(int transportType,
            NetworkStatsManager statsManager) {
        try (NetworkStats stats = statsManager.queryDetailsForUidTag(transportType, null,
                Long.MIN_VALUE, Long.MAX_VALUE, Process.myUid(), TRAFFIC_TAG)) {
            if (stats.hasNextBucket()) {
                NetworkStats.Bucket outputBucket = new NetworkStats.Bucket();
                stats.getNextBucket(outputBucket);
                return outputBucket;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String makeBytesTransferredText(String transportType, NetworkStats.Bucket b) {
        if (b == null) {
            return "No tagged bytes transferred. This may be due to caching.";
        }

        return String.format(Locale.getDefault(),
                "The total bytes transferred via %s for tag %d is %d.", transportType, b.getTag(),
                b.getTxBytes() + b.getRxBytes());
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}

