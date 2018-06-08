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

import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.chromium.support_lib_boundary.WebViewClientBoundaryInterface;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationHandler;

/**
 * Compatibility version of {@link android.webkit.WebViewClient}.
 */
// Note: some methods are marked as RequiresApi 21, because only an up-to-date WebView APK would
// ever invoke these methods (and WebView can only be updated on Lollipop and above). The app can
// still construct a WebViewClientCompat on a pre-Lollipop devices, and explicitly invoke these
// methods, so each of these methods must also handle this case.
public class WebViewClientCompat extends WebViewClient implements WebViewClientBoundaryInterface {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(value = {
            WebViewClient.SAFE_BROWSING_THREAT_UNKNOWN,
            WebViewClient.SAFE_BROWSING_THREAT_MALWARE,
            WebViewClient.SAFE_BROWSING_THREAT_PHISHING,
            WebViewClient.SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SafeBrowsingThreat {}

    @Override
    public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
    }

    /**
     * Invoked by chromium for the {@code onReceivedError} event. Applications are not meant to
     * override this, and should instead override the non-final {@code onReceivedError} method.
     * TODO(ntfschr): link to that method once it's implemented.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
            /* WebResourceError */ @NonNull InvocationHandler error) {
        // TODO(ntfschr): implement this (b/73151460).
    }

    @Override
    public void onReceivedHttpError(@NonNull WebView view, @NonNull WebResourceRequest request,
            @NonNull WebResourceResponse errorResponse) {
    }

    /**
     * Invoked by chromium for the {@code onSafeBrowsingHit} event. Applications are not meant to
     * override this, and should instead override the non-final {@code onSafeBrowsingHit} method.
     * TODO(ntfschr): link to that method once it's implemented.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
            @SafeBrowsingThreat int threatType,
            /* SafeBrowsingResponse */ @NonNull InvocationHandler callback) {
        // TODO(ntfschr): implement this (b/73151460).
    }

    // Default behavior in WebViewClient is to invoke the other (deprecated)
    // shouldOverrideUrlLoading method.
    @Override
    @SuppressWarnings("deprecation")
    @RequiresApi(21)
    public boolean shouldOverrideUrlLoading(@NonNull WebView view,
            @NonNull WebResourceRequest request) {
        if (Build.VERSION.SDK_INT < 21) return false;
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }
}
