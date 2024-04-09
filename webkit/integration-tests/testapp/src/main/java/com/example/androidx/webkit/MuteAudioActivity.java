/*
 * Copyright 2024 The Android Open Source Project
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
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

public class MuteAudioActivity extends AppCompatActivity {
    @Nullable
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mute_audio);
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)) {
            WebkitHelpers.showMessageInActivity(MuteAudioActivity.this,
                    R.string.mute_audio_not_supported);
            return;
        }

        setupWebView();

        findViewById(R.id.mute_audio_mute).setOnClickListener(this::mute);
        findViewById(R.id.mute_audio_unmute).setOnClickListener(this::unmute);
        findViewById(R.id.mute_audio_check).setOnClickListener(this::checkMuted);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mWebView != null) {
            // We don't want the WebView to continue playing sound in the background once the
            // activity is destroyed. Remove it from the view hierarchy and destroy it.
            final ViewGroup parent = (ViewGroup) mWebView.getParent();
            parent.removeView(mWebView);
            mWebView.destroy();
        }
    }

    private void setupWebView() {
        mWebView = findViewById(R.id.mute_audio_webview);

        mWebView.getSettings().setJavaScriptEnabled(true);

        WebViewCompat.setAudioMuted(mWebView, true);

        try (InputStream is = getAssets().open("www/mute_audio.html")) {
            String webContent = new String(ByteStreams.toByteArray(is), Charsets.UTF_8);
            mWebView.loadDataWithBaseURL("https://example.com", webContent, "text/html", null,
                    null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void mute(View view) {
        WebViewCompat.setAudioMuted(mWebView, true);
    }

    private void unmute(View view) {
        WebViewCompat.setAudioMuted(mWebView, false);
    }

    private void checkMuted(View view) {
        boolean muted = WebViewCompat.isAudioMuted(mWebView);
        final String text = getResources().getString(
                muted ? R.string.mute_audio_audio_is_muted : R.string.mute_audio_audio_is_unmuted);
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }
}
