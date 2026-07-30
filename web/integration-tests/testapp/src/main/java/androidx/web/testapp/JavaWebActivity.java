/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.web.testapp;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.web.WebContent;
import androidx.web.WebContentView;

import org.jspecify.annotations.Nullable;

/**
 * An Activity that demonstrates how to embed and use {@link WebContent}
 * using traditional Android Views. It interacts with {@link JavaWebViewModel}
 * to retain the WebContent state across configuration changes.
 */
public class JavaWebActivity extends ComponentActivity {

    private static final String INITIAL_URL = "https://www.google.com";

    private JavaWebViewModel mViewModel;
    private FrameLayout mContainer;
    private WebContentView mWebContentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContainer = new FrameLayout(this);
        setContentView(
                mContainer,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        mViewModel = new ViewModelProvider(this).get(JavaWebViewModel.class);
        attachWebContentView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        detachWebContentView();
    }

    private void attachWebContentView() {
        WebContent webContent = mViewModel.getWebContent();

        if (webContent == null) {
            TextView unsupportedTextView = new TextView(this);
            unsupportedTextView.setText("WebContent feature is not supported on this device.");
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.gravity = Gravity.CENTER;
            mContainer.addView(unsupportedTextView, lp);
            return;
        }

        mWebContentView = webContent.attach(this, WebContentView::new);
        mWebContentView.getSettings().setJavaScriptEnabled(true);

        if (mWebContentView.getParent() == null) {
            mContainer.addView(
                    mWebContentView,
                    new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                    )
            );
        }

        if (!mViewModel.isInitialUrlLoaded()) {
            mWebContentView.loadUrl(INITIAL_URL);
            mViewModel.setInitialUrlLoaded(true);
        }
    }

    private void detachWebContentView() {
        WebContent webContent = mViewModel.getWebContent();

        if (mWebContentView != null) {
            mContainer.removeView(mWebContentView);
        }

        if (webContent != null) {
            webContent.detach();
            mWebContentView = null;
        }
    }
}
