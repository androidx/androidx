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
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * An {@link Activity} to demonstrate how to allowlist a set of domains from Safe Browsing checks.
 * This includes buttons to toggle whether the allowlist is on or off.
 */
public class AllowlistActivity extends AppCompatActivity {

    private WebView mAllowlistWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allowlist);
        setTitle(R.string.allowlist_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ALLOWLIST)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        SwitchCompat allowlistSwitch = findViewById(R.id.allowlist_switch);
        allowlistSwitch.setChecked(true);
        allowlistSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                allowlistSafeBrowsingTestSite(null);
            } else {
                clearAllowlist();
            }
        });

        mAllowlistWebView = findViewById(R.id.allowlist_webview);

        // Allow mAllowlistWebView to handle navigations.
        mAllowlistWebView.setWebViewClient(new WebViewClientCompat());

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(mAllowlistWebView.getSettings(), true);
        }

        // Set the allowlist and load the test site.
        allowlistSafeBrowsingTestSite(
                () -> mAllowlistWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearAllowlist();
    }

    @Override
    public void onBackPressed() {
        if (mAllowlistWebView.canGoBack()) {
            mAllowlistWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void clearAllowlist() {
        // To clear the allowlist (and check all domains with Safe Browsing), pass an empty list.
        final Activity activity = this;
        WebViewCompat.setSafeBrowsingAllowlist(
                Collections.emptySet(), success -> {
                    if (!success) {
                        WebkitHelpers.showMessageInActivity(activity,
                                R.string.invalid_allowlist_input_message);
                    }
                    // Nothing interesting to do if this succeeds, let user continue to use the app.
                });
    }

    private void allowlistSafeBrowsingTestSite(@Nullable Runnable onSuccess) {
        // Configure an allowlist of domains. Pages/resources loaded from these domains will never
        // be checked by Safe Browsing (until a new allowlist is applied).
        final Set<String> allowlist = new HashSet<>();
        allowlist.add(SafeBrowsingHelpers.TEST_SAFE_BROWSING_DOMAIN);
        final Activity activity = this;
        WebViewCompat.setSafeBrowsingAllowlist(allowlist, success -> {
            if (success) {
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } else {
                WebkitHelpers.showMessageInActivity(activity,
                        R.string.invalid_allowlist_input_message);
            }
        });
    }
}
