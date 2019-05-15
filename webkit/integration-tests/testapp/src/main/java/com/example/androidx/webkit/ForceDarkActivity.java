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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

// TODO(amalova): enable the following attributes once Q SDK is available.
// <item name="android:forceDarkAllowed">true</item>
// <item name="android:isLightTheme">true</item>
// This attributes are necessary to make force dark auto mode works.

/**
 * An {@link Activity} to exercise Force Dark functionality.
 * It shows WebViews side by side with different dark mode settings.
 */
public class ForceDarkActivity extends AppCompatActivity {
    private static final String DESCRIPTION =
            "<html><body><h1>Force Dark Mode is %s </h1></body></html>";

    @Override
    @SuppressLint("RestrictedApi")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_force_dark);
        setTitle(R.string.force_dark_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            setupWebView(findViewById(R.id.webview_force_dark_on), WebSettingsCompat.FORCE_DARK_ON);
            setupWebView(findViewById(R.id.webview_force_dark_off),
                    WebSettingsCompat.FORCE_DARK_OFF);
            setupWebView(findViewById(R.id.webview_force_dark_auto),
                    WebSettingsCompat.FORCE_DARK_AUTO);
        } else {
            WebkitHelpers.showMessageInActivity(ForceDarkActivity.this,
                    R.string.webkit_api_not_available);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setupWebView(WebView webView, int forceDarkMode) {
        webView.setWebViewClient(new WebViewClient());
        String formatedDescription;
        switch (forceDarkMode) {
            case WebSettingsCompat.FORCE_DARK_ON:
                formatedDescription = Base64.encodeToString(
                        String.format(DESCRIPTION, "ON").getBytes(), Base64.NO_PADDING);
                break;
            case WebSettingsCompat.FORCE_DARK_OFF:
                formatedDescription = Base64.encodeToString(
                        String.format(DESCRIPTION, "OFF").getBytes(), Base64.NO_PADDING);
                break;
            case WebSettingsCompat.FORCE_DARK_AUTO:
                formatedDescription = Base64.encodeToString(
                        String.format(DESCRIPTION, "AUTO").getBytes(), Base64.NO_PADDING);
                break;
            default:
                throw new UnsupportedOperationException("Unknown force dark mode");
        }
        webView.loadData(formatedDescription, null, "base64");
        WebSettingsCompat.setForceDark(webView.getSettings(), forceDarkMode);
    }
}
