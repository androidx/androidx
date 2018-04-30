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
import android.webkit.SafeBrowsingResponse;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.WebResourceErrorImpl;
import androidx.webkit.internal.WebViewFeatureInternal;

import org.chromium.support_lib_boundary.WebViewClientBoundaryInterface;
import org.chromium.support_lib_boundary.util.Features;

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
    private static final String[] sSupportedFeatures = new String[] {
        Features.VISUAL_STATE_CALLBACK,
        Features.RECEIVE_WEB_RESOURCE_ERROR,
        Features.RECEIVE_HTTP_ERROR,
        Features.SHOULD_OVERRIDE_WITH_REDIRECTS,
        Features.SAFE_BROWSING_HIT,
    };

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

    /**
     * Returns the list of features this client supports. This feature list should always be a
     * subset of the Features declared in WebViewFeature.
     *
     * @hide
     */
    @Override
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public final String[] getSupportedFeatures() {
        return sSupportedFeatures;
    }

    /**
     * Notify the host application that {@link android.webkit.WebView} content left over from
     * previous page navigations will no longer be drawn.
     *
     * <p>This callback can be used to determine the point at which it is safe to make a recycled
     * {@link android.webkit.WebView} visible, ensuring that no stale content is shown. It is called
     * at the earliest point at which it can be guaranteed that {@link WebView#onDraw} will no
     * longer draw any content from previous navigations. The next draw will display either the
     * {@link WebView#setBackgroundColor background color} of the {@link WebView}, or some of the
     * contents of the newly loaded page.
     *
     * <p>This method is called when the body of the HTTP response has started loading, is reflected
     * in the DOM, and will be visible in subsequent draws. This callback occurs early in the
     * document loading process, and as such you should expect that linked resources (for example,
     * CSS and images) may not be available.
     *
     * <p>For more fine-grained notification of visual state updates, see {@link
     * WebViewCompat#postVisualStateCallback}.
     *
     * <p>Please note that all the conditions and recommendations applicable to
     * {@link WebViewCompat#postVisualStateCallback} also apply to this API.
     *
     * <p>This callback is only called for main frame navigations.
     *
     * <p>This method is called only if {@link WebViewFeature#VISUAL_STATE_CALLBACK} is supported.
     * You can check whether that flag is supported using {@link
     * WebViewFeature#isFeatureSupported(String)}.
     *
     * @param view The {@link android.webkit.WebView} for which the navigation occurred.
     * @param url  The URL corresponding to the page navigation that triggered this callback.
     */
    @Override
    public void onPageCommitVisible(@NonNull WebView view, @NonNull String url) {
    }

    /**
     * Invoked by chromium (for WebView APks 67+) for the {@code onReceivedError} event.
     * Applications are not meant to override this, and should instead override the non-final {@link
     * onReceivedError(WebView, WebResourceRequest, WebResourceErrorCompat)} method.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    @RequiresApi(21)
    public final void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
            /* WebResourceError */ @NonNull InvocationHandler handler) {
        onReceivedError(view, request, new WebResourceErrorImpl(handler));
    }

    /**
     * Invoked by chromium (in legacy WebView APKs) for the {@code onReceivedError} event on {@link
     * Build.VERSION_CODES.M} and above. Applications are not meant to override this, and should
     * instead override the non-final {@link onReceivedError(WebView, WebResourceRequest,
     * WebResourceErrorCompat)} method.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    @RequiresApi(23)
    public final void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
            @NonNull WebResourceError error) {
        if (Build.VERSION.SDK_INT < 23) return;
        onReceivedError(view, request, new WebResourceErrorImpl(error));
    }

    /**
     * Report web resource loading error to the host application. These errors usually indicate
     * inability to connect to the server. Note that unlike the deprecated version of the callback,
     * the new version will be called for any resource (iframe, image, etc.), not just for the main
     * page. Thus, it is recommended to perform minimum required work in this callback.
     *
     * <p>This method is called only if {@link WebViewFeature#RECEIVE_WEB_RESOURCE_ERROR} is
     * supported. You can check whether that flag is supported using {@link
     * WebViewFeature#isFeatureSupported(String)}.
     *
     * @param view The WebView that is initiating the callback.
     * @param request The originating request.
     * @param error Information about the error occurred.
     */
    @SuppressWarnings("deprecation") // for invoking the old onReceivedError.
    @RequiresApi(21)
    public void onReceivedError(@NonNull WebView view, @NonNull WebResourceRequest request,
            @NonNull WebResourceErrorCompat error) {
        if (Build.VERSION.SDK_INT < 21) return;
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_RESOURCE_ERROR_GET_CODE)
                || !WebViewFeature.isFeatureSupported(
                        WebViewFeature.WEB_RESOURCE_ERROR_GET_DESCRIPTION)) {
            // If the WebView APK drops supports for these APIs in the future, simply do nothing.
            return;
        }
        if (request.isForMainFrame()) {
            onReceivedError(view,
                    error.getErrorCode(), error.getDescription().toString(),
                    request.getUrl().toString());
        }
    }

    /**
     * Notify the host application that an HTTP error has been received from the server while
     * loading a resource.  HTTP errors have status codes &gt;= 400.  This callback will be called
     * for any resource (iframe, image, etc.), not just for the main page. Thus, it is recommended
     * to perform minimum required work in this callback. Note that the content of the server
     * response may not be provided within the {@code errorResponse} parameter.
     *
     * <p>This method is called only if {@link WebViewFeature#RECEIVE_HTTP_ERROR} is supported. You
     * can check whether that flag is supported using {@link
     * WebViewFeature#isFeatureSupported(String)}.
     *
     * @param view The WebView that is initiating the callback.
     * @param request The originating request.
     * @param errorResponse Information about the error occurred.
     */
    @Override
    public void onReceivedHttpError(@NonNull WebView view, @NonNull WebResourceRequest request,
            @NonNull WebResourceResponse errorResponse) {
    }

    /**
     * Invoked by chromium (for WebView APks 67+) for the {@code onSafeBrowsingHit} event.
     * Applications are not meant to override this, and should instead override the non-final {@link
     * onSafeBrowsingHit(WebView, WebResourceRequest, int, SafeBrowsingResponseCompat)} method.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public final void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
            @SafeBrowsingThreat int threatType,
            /* SafeBrowsingResponse */ @NonNull InvocationHandler handler) {
        onSafeBrowsingHit(view, request, threatType,
                SafeBrowsingResponseCompat.fromInvocationHandler(handler));
    }

    /**
     * Invoked by chromium (in legacy WebView APKs) for the {@code onSafeBrowsingHit} event on
     * {@link Build.VERSION_CODES.O_MR1} and above. Applications are not meant to override this, and
     * should instead override the non-final {@link onSafeBrowsingHit(WebView, WebResourceRequest,
     * int, SafeBrowsingResponseCompat)} method.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    @RequiresApi(27)
    public final void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
            @SafeBrowsingThreat int threatType, @NonNull SafeBrowsingResponse response) {
        onSafeBrowsingHit(view, request, threatType,
                SafeBrowsingResponseCompat.fromSafeBrowsingResponse(response));
    }

    /**
     * Notify the host application that a loading URL has been flagged by Safe Browsing.
     *
     * The application must invoke the callback to indicate the preferred response. The default
     * behavior is to show an interstitial to the user, with the reporting checkbox visible.
     *
     * If the application needs to show its own custom interstitial UI, the callback can be invoked
     * asynchronously with {@link SafeBrowsingResponseCompat#backToSafety} or {@link
     * SafeBrowsingResponseCompat#proceed}, depending on user response.
     *
     * @param view The WebView that hit the malicious resource.
     * @param request Object containing the details of the request.
     * @param threatType The reason the resource was caught by Safe Browsing, corresponding to a
     *                   {@code SAFE_BROWSING_THREAT_*} value.
     * @param callback Applications must invoke one of the callback methods.
     */
    public void onSafeBrowsingHit(@NonNull WebView view, @NonNull WebResourceRequest request,
            @SafeBrowsingThreat int threatType, @NonNull SafeBrowsingResponseCompat callback) {
        if (WebViewFeature.isFeatureSupported(
                WebViewFeature.SAFE_BROWSING_RESPONSE_SHOW_INTERSTITIAL)) {
            callback.showInterstitial(true);
        } else {
            // This should not happen, but in case the WebView APK eventually drops support for
            // showInterstitial(), raise a runtime exception and require the WebView APK to handle
            // this.
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Give the host application a chance to take over the control when a new
     * url is about to be loaded in the current WebView. If WebViewClient is not
     * provided, by default WebView will ask Activity Manager to choose the
     * proper handler for the url. If WebViewClient is provided, return {@code true}
     * means the host application handles the url, while return {@code false} means the
     * current WebView handles the url.
     *
     * <p>Notes:
     * <ul>
     * <li>This method is not called for requests using the POST &quot;method&quot;.</li>
     * <li>This method is also called for subframes with non-http schemes, thus it is
     * strongly disadvised to unconditionally call {@link WebView#loadUrl(String)}
     * with the request's url from inside the method and then return {@code true},
     * as this will make WebView to attempt loading a non-http url, and thus fail.</li>
     * </ul>
     *
     * <p>This method is called only if {@link WebViewFeature#SHOULD_OVERRIDE_WITH_REDIRECTS} is
     * supported. You can check whether that flag is supported using {@link
     * WebViewFeature#isFeatureSupported(String)}.
     *
     * @param view The WebView that is initiating the callback.
     * @param request Object containing the details of the request.
     * @return {@code true} if the host application wants to leave the current WebView
     *         and handle the url itself, otherwise return {@code false}.
     */
    @Override
    @SuppressWarnings("deprecation") // for invoking the old shouldOverrideUrlLoading.
    @RequiresApi(21)
    public boolean shouldOverrideUrlLoading(@NonNull WebView view,
            @NonNull WebResourceRequest request) {
        if (Build.VERSION.SDK_INT < 21) return false;
        return shouldOverrideUrlLoading(view, request.getUrl().toString());
    }
}
