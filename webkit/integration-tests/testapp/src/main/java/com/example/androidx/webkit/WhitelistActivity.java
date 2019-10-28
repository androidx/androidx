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

import android.app.Activity;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.CompoundButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Activity} to demonstrate how to whitelist a set of domains from Safe Browsing checks.
 * This includes buttons to toggle whether the whitelist is on or off.
 */
public class WhitelistActivity extends AppCompatActivity {

    private WebView mWhitelistWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whitelist);
        setTitle(R.string.whitelist_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_WHITELIST)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        SwitchCompat whitelistSwitch = findViewById(R.id.whitelist_switch);
        whitelistSwitch.setChecked(true);
        whitelistSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    whitelistSafeBrowsingTestSite(null);
                } else {
                    clearWhitelist();
                }
            }
        });

        mWhitelistWebView = findViewById(R.id.whitelist_webview);

        // Allow mWhitelistWebView to handle navigations.
        mWhitelistWebView.setWebViewClient(new WebViewClientCompat());

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(mWhitelistWebView.getSettings(), true);
        }

        // Set the whitelist and load the test site.
        whitelistSafeBrowsingTestSite(new Runnable() {
            @Override
            public void run() {
                mWhitelistWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearWhitelist();
    }

    @Override
    public void onBackPressed() {
        if (mWhitelistWebView.canGoBack()) {
            mWhitelistWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void clearWhitelist() {
        // To clear the whitelist (and check all domains with Safe Browsing), pass an empty list.
        final List<String> emptyWhitelist = new ArrayList<>();
        final Activity activity = this;
        WebViewCompat.setSafeBrowsingWhitelist(emptyWhitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                if (!success) {
                    WebkitHelpers.showMessageInActivity(activity,
                            R.string.invalid_whitelist_input_message);
                } // Nothing interesting to do if this succeeds, let user continue to use the app.
            }
        });
    }

    private void whitelistSafeBrowsingTestSite(@Nullable Runnable onSuccess) {
        // Configure a whitelist of domains. Pages/resources loaded from these domains will never be
        // checked by Safe Browsing (until a new whitelist is applied).
        final List<String> whitelist = new ArrayList<>();
        whitelist.add(SafeBrowsingHelpers.TEST_SAFE_BROWSING_DOMAIN);
        final Activity activity = this;
        WebViewCompat.setSafeBrowsingWhitelist(whitelist, new ValueCallback<Boolean>() {
            @Override
            public void onReceiveValue(Boolean success) {
                if (success) {
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                } else {
                    WebkitHelpers.showMessageInActivity(activity,
                            R.string.invalid_whitelist_input_message);
                }
            }
        });
    }
}
