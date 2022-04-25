/*
 * Copyright 2019 The Android Open Source Project
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

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * An {@link android.app.Activity} to demonstrate medium ("Quiet") interstitials. WebView displays a
 * grey error page with a small bit of description when it's "medium" sized (large enough to show
 * text, but small enough that it's likely not the predominant part of the UI), when loading
 * malicious resources.
 * <p>
 * Medium interstitials are triggered when the WebView is either taller or wider than an otherwise
 * "small" WebView. This {@link android.app.Activity} can show either case ("tall" and "wide",
 * respectively), based on the boolean extra {@link #LAYOUT_HORIZONTAL}.
 */
public class MediumInterstitialActivity extends AppCompatActivity {

    public static final String LAYOUT_HORIZONTAL = "layoutHorizontal";
    private static final int STRETCH_THIS_DIMENSION = 0;
    private static final int MARGIN_DP = 5;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Decide whether to show the WebViews side-by-side ("tall") or stacked on top of each
        // other ("wide").
        Intent intent = getIntent();
        boolean isHorizontal = intent.getBooleanExtra(LAYOUT_HORIZONTAL, true);

        setContentView(R.layout.activity_medium_interstitial);
        setTitle(isHorizontal
                ? R.string.medium_tall_interstitial_activity_title
                : R.string.medium_wide_interstitial_activity_title);
        LinearLayout layout = findViewById(R.id.activity_medium_interstitial);
        layout.setOrientation(isHorizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        WebkitHelpers.appendWebViewVersionToTitle(this);

        Map<Integer, String> map = new ImmutableMap.Builder<Integer, String>()
                .put(R.id.malware_webview, SafeBrowsingHelpers.MALWARE_URL)
                .put(R.id.phishing_webview, SafeBrowsingHelpers.PHISHING_URL)
                .put(R.id.unwanted_software_webview, SafeBrowsingHelpers.UNWANTED_SOFTWARE_URL)
                .put(R.id.billing_webview, SafeBrowsingHelpers.BILLING_URL)
                .build();
        // Add more threat types (here and in the layout), if we support more in the future.

        int width = isHorizontal
                ? STRETCH_THIS_DIMENSION
                : LinearLayout.LayoutParams.MATCH_PARENT;
        int height = isHorizontal
                ? LinearLayout.LayoutParams.MATCH_PARENT
                : STRETCH_THIS_DIMENSION;
        int weight = 1; // equal weights for each WebView
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
        params.setMargins(MARGIN_DP, MARGIN_DP, MARGIN_DP, MARGIN_DP);

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            int id = entry.getKey();
            String url = entry.getValue();
            WebView webView = findViewById(id);
            setupWebView(webView, params);
            webView.loadUrl(url);
        }
    }

    private void setupWebView(WebView webView, LinearLayout.LayoutParams params) {
        webView.setLayoutParams(params);
        // A medium interstitial may have links on it in the future; allow this WebView to handle
        // opening those by setting a WebViewClient.
        webView.setWebViewClient(new WebViewClient());
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webView.getSettings(), true);
        }
    }
}
