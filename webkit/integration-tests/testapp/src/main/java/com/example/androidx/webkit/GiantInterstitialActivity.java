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

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.webkit.WebView;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate Quiet Giant interstitials. Similar to {@link
 * SmallInterstitialActivity}, WebView displays a grey error page with very little text when it
 * extends at least partially off-screen. This is because we're not confident the user can properly
 * interact with the Loud interstitial UI, as much of it would be off-screen in this case.
 */
public class GiantInterstitialActivity extends AppCompatActivity {

    private static final double SCALE_FACTOR = 1.1;

    @Override
    @SuppressWarnings("deprecation") /* defaultDisplay */
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_giant_interstitial);
        setTitle(R.string.giant_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        WebView webview = findViewById(R.id.giant_webview);

        // Scale up the WebView so it's guaranteed to be partially off-screen. Round up the
        // multiplication result, so this is always a bigger number (display size should never be
        // 0x0).
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = (int) Math.ceil(displayMetrics.heightPixels * SCALE_FACTOR);
        int width = (int) Math.ceil(displayMetrics.widthPixels * SCALE_FACTOR);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        webview.setLayoutParams(params);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webview.getSettings(), true);
        }
        webview.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
    }
}
