/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.webkit;

import android.support.test.InstrumentationRegistry;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewOnUiThread {
    private WebView mWebView;

    public WebViewOnUiThread() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView = new WebView(InstrumentationRegistry.getTargetContext());
            }
        });
    }

    public void loadUrl(final String url) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl(url);
            }
        });
    }

    public void setWebViewClient(final WebViewClient webviewClient) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mWebView.setWebViewClient(webviewClient);
            }
        });
    }

    public void postVisualStateCallbackCompat(final long requestId,
            final WebViewCompat.VisualStateCallback callback) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                WebViewCompat.postVisualStateCallback(mWebView, requestId, callback);
            }
        });
    }

    public WebSettings getSettings() {
        return getValue(new ValueGetter<WebSettings>() {
            @Override
            public WebSettings capture() {
                return mWebView.getSettings();
            }
        });
    }

    public WebView getWebViewOnCurrentThread() {
        return mWebView;
    }

    private <T> T getValue(ValueGetter<T> getter) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(getter);
        return getter.getValue();
    }

    private abstract class ValueGetter<T> implements Runnable {
        private T mValue;

        @Override
        public void run() {
            mValue = capture();
        }

        protected abstract T capture();

        public T getValue() {
            return mValue;
        }
    }
}
