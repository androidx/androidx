/*
 * Copyright 2025 The Android Open Source Project
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * An Activity that launches a payment app in WebView through PaymentRequest API.
 */
public class PaymentRequestActivity extends AppCompatActivity implements OnCheckedChangeListener {
    private static final String EXAMPLE_SITE_WITH_PAYMENT_REQUEST_API =
            "https://rsolomakhin.github.io/pr/bob/";

    private WebView mWebView;
    private WebSettings mWebSettings;
    private CompoundButton mPaymentRequestToggle;
    private CompoundButton mHasEnrolledInstrumentToggle;

    // AppCompatActivity:
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_request);
        setTitle(R.string.payment_request_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
            WebkitHelpers.showMessageInActivity(
                    PaymentRequestActivity.this, R.string.webkit_api_not_available);
            return;
        }

        mWebView = findViewById(R.id.webview_supports_payment_request);
        mWebView.setWebContentsDebuggingEnabled(true);
        mWebSettings = mWebView.getSettings();
        mPaymentRequestToggle = findViewById(R.id.payment_request_toggle);
        mHasEnrolledInstrumentToggle = findViewById(R.id.has_enrolled_instrument_toggle);

        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setAllowFileAccess(true);
        mWebSettings.setAllowContentAccess(true);

        // Default layout behavior for chrome on android:
        mWebSettings.setBuiltInZoomControls(true);
        mWebSettings.setDisplayZoomControls(false);
        mWebSettings.setUseWideViewPort(true);
        mWebSettings.setLoadWithOverviewMode(true);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        mWebView.setWebViewClient(new WebViewClient());

        if (savedInstanceState == null) {
            mWebView.loadUrl(EXAMPLE_SITE_WITH_PAYMENT_REQUEST_API);
        } else {
            mWebView.restoreState(savedInstanceState);
        }

        mPaymentRequestToggle.setChecked(
                WebSettingsCompat.getPaymentRequestEnabled(mWebSettings));
        mHasEnrolledInstrumentToggle.setChecked(
                WebSettingsCompat.getHasEnrolledInstrumentEnabled(mWebSettings));

        mPaymentRequestToggle.setOnCheckedChangeListener(this);
        mHasEnrolledInstrumentToggle.setOnCheckedChangeListener(this);
    }

    // OnCheckedChangeListener
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        assert WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST);
        WebSettingsCompat.setPaymentRequestEnabled(
                mWebSettings, mPaymentRequestToggle.isChecked());
        WebSettingsCompat.setHasEnrolledInstrumentEnabled(
                mWebSettings, mHasEnrolledInstrumentToggle.isChecked());
        mWebView.reload();
    }

    @Override
    public void onBackPressed() {
        if (mWebView == null || !mWebView.canGoBack()) {
            super.onBackPressed();
            return;
        }

        mWebView.goBack();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        mWebView.saveState(state);
    }

    @Override
    public void onRestoreInstanceState(@NonNull Bundle state) {
        super.onRestoreInstanceState(state);
        mWebView.restoreState(state);
    }
}
