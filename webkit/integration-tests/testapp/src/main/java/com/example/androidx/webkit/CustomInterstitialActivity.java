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
import android.content.Intent;
import android.os.Bundle;
import android.util.SparseArray;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.SafeBrowsingResponseCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} which shows a custom interstitial if {@link WebView} encounters malicious
 * resources. This class contains the logic for responding to user interaction with custom
 * interstitials. The UI for these interstitials is implemented by {@link
 * PopupInterstitialActivity}.
 */
public class CustomInterstitialActivity extends AppCompatActivity {

    private WebView mWebView;
    private CustomInterstitialWebViewClient mWebViewClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_interstitial);
        setTitle(R.string.custom_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_HIT)
                && WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED)
                && WebViewFeature.isFeatureSupported(
                WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY)) {
            mWebView = findViewById(R.id.custom_interstitial_webview);
            mWebViewClient = new CustomInterstitialWebViewClient(this);
            mWebView.setWebViewClient(mWebViewClient);
            mWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
        } else {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mWebViewClient.handleInterstitialResponse(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private static class CustomInterstitialWebViewClient extends WebViewClientCompat {
        private final Activity mActivity;
        private final SparseArray<SafeBrowsingResponseCompat> mSafeBrowsingResponseMap;
        int mActivityRequestCounter;

        CustomInterstitialWebViewClient(Activity activity) {
            mActivity = activity;
            mSafeBrowsingResponseMap = new SparseArray<>();
            mActivityRequestCounter = 0;
        }

        @Override
        @RequiresApi(21) // This won't be called on < L, so we can safely apply @RequiresApi.
        public void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
                int threatType, @NonNull SafeBrowsingResponseCompat callback) {
            mSafeBrowsingResponseMap.put(mActivityRequestCounter, callback);
            createInterstitial(threatType, request);
            mActivityRequestCounter++;
        }

        @RequiresApi(21) // for WebResourceRequest
        private void createInterstitial(int threatType, @NonNull WebResourceRequest request) {
            Intent myIntent = new Intent(mActivity, PopupInterstitialActivity.class);
            myIntent.putExtra(PopupInterstitialActivity.THREAT_TYPE, threatType);
            myIntent.putExtra(PopupInterstitialActivity.THREAT_URL,
                    Api21Impl.getUrl(request).toString());
            mActivity.startActivityForResult(myIntent, mActivityRequestCounter);
        }

        void handleInterstitialResponse(int requestCode, int resultCode, Intent data) {
            // Get the correct SafeBrowsingResponse for the given interstitial Intent (there can be
            // multiple Intents at the same time if multiple resources are malicious).
            final SafeBrowsingResponseCompat response = mSafeBrowsingResponseMap.get(requestCode);
            mSafeBrowsingResponseMap.delete(requestCode);

            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                if (response == null) {
                    return;
                }

                // Figure out what navigation action we should take.
                String result = data.getStringExtra(PopupInterstitialActivity.ACTION_RESPONSE);
                // Figure out whether we should report this event.
                boolean shouldSendReport = data.getBooleanExtra(
                        PopupInterstitialActivity.SHOULD_SEND_REPORT, false);

                switch (result) {
                    case PopupInterstitialActivity.ACTION_RESPONSE_BACK_TO_SAFETY:
                        response.backToSafety(shouldSendReport);
                        break;
                    case PopupInterstitialActivity.ACTION_RESPONSE_PROCEED:
                        response.proceed(shouldSendReport);
                        break;
                    default:
                        break;
                }
            } else if (resultCode == RESULT_CANCELED) {
                // User pressed the system's back button, treat this like backToSafety().
                response.backToSafety(false);
            } else {
                throw new IllegalStateException("PopupInterstitialActivity shouldn't return any "
                        + "nonstandard resultCodes");
            }
        }

    }
}
