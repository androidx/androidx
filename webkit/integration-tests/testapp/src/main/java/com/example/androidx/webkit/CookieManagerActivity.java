/*
 * Copyright 2022 The Android Open Source Project
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
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.CookieManagerCompat;
import androidx.webkit.WebViewFeature;

import java.util.List;

/**
 * An {@link android.app.Activity} to demonstrate {@link CookieManagerCompat#getCookieInfo}.
 */
public class CookieManagerActivity extends AppCompatActivity {
    private static final String MAIN_PAGE_URL = "https://developer.android"
            + ".com/reference/androidx/webkit/package-summary";
    private static final String COOKIE_URL = "https://developer.android.com";
    private static final String COOKIE_NAME = "signin=";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_cookie_info);
        setTitle(R.string.cookie_manager_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.GET_COOKIE_INFO)) {
            WebkitHelpers.showMessageInActivity(CookieManagerActivity.this,
                    R.string.cookie_manager_get_cookie_info_not_supported);
            return;
        }

        WebView webView = findViewById(R.id.webView);
        TextView oldApiText = findViewById(R.id.textViewTop);
        TextView newApiText = findViewById(R.id.textViewBottom);

        WebViewClient client = new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String oldCookie = CookieManager.getInstance().getCookie(COOKIE_URL);
                if (oldCookie != null) {
                    String[] oldCookies = oldCookie.split("; ");
                    for (String cookie : oldCookies) {
                        if (cookie.startsWith(COOKIE_NAME)) {
                            oldApiText.append(cookie);
                            break;
                        }
                    }
                }

                List<String> cookies = CookieManagerCompat.getCookieInfo(
                        CookieManager.getInstance(), COOKIE_URL);
                for (String cookie : cookies) {
                    if (cookie.startsWith(COOKIE_NAME)) {
                        newApiText.append(cookie);
                        break;
                    }
                }
            }
        };

        webView.setWebViewClient(client);
        webView.loadUrl(MAIN_PAGE_URL);
    }
}
