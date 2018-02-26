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

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.BuildCompat;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebViewProviderAdapter;
import androidx.webkit.internal.WebViewProviderFactoryAdapter;

/**
 * Compatibility version of {@link android.webkit.WebView}
 */
public class WebViewCompat {
    private WebViewCompat() {} // Don't allow instances of this class to be constructed.

    /**
     * Callback interface supplied to {@link #postVisualStateCallback} for receiving
     * notifications about the visual state.
     */
    public interface VisualStateCallback {
        /**
         * Invoked when the visual state is ready to be drawn in the next {@link WebView#onDraw}.
         *
         * @param requestId The identifier passed to {@link #postVisualStateCallback} when this
         *                  callback was posted.
         */
        void onComplete(long requestId);
    }

    /**
     * Posts a {@link VisualStateCallback}, which will be called when
     * the current state of the WebView is ready to be drawn.
     *
     * <p>Because updates to the DOM are processed asynchronously, updates to the DOM may not
     * immediately be reflected visually by subsequent {@link WebView#onDraw} invocations. The
     * {@link VisualStateCallback} provides a mechanism to notify the caller when the contents
     * of the DOM at the current time are ready to be drawn the next time the {@link WebView} draws.
     *
     * <p>The next draw after the callback completes is guaranteed to reflect all the updates to the
     * DOM up to the point at which the {@link VisualStateCallback} was posted, but it may
     * also contain updates applied after the callback was posted.
     *
     * <p>The state of the DOM covered by this API includes the following:
     * <ul>
     * <li>primitive HTML elements (div, img, span, etc..)</li>
     * <li>images</li>
     * <li>CSS animations</li>
     * <li>WebGL</li>
     * <li>canvas</li>
     * </ul>
     * It does not include the state of:
     * <ul>
     * <li>the video tag</li>
     * </ul>
     *
     * <p>To guarantee that the {@link WebView} will successfully render the first frame
     * after the {@link VisualStateCallback#onComplete} method has been called a set of
     * conditions must be met:
     * <ul>
     * <li>If the {@link WebView}'s visibility is set to {@link android.view.View#VISIBLE VISIBLE}
     * then * the {@link WebView} must be attached to the view hierarchy.</li>
     * <li>If the {@link WebView}'s visibility is set to
     * {@link android.view.View#INVISIBLE INVISIBLE} then the {@link WebView} must be attached to
     * the view hierarchy and must be made {@link android.view.View#VISIBLE VISIBLE} from the
     * {@link VisualStateCallback#onComplete} method.</li>
     * <li>If the {@link WebView}'s visibility is set to {@link android.view.View#GONE GONE} then
     * the {@link WebView} must be attached to the view hierarchy and its
     * {@link android.widget.AbsoluteLayout.LayoutParams LayoutParams}'s width and height need to be
     * set to fixed values and must be made {@link android.view.View#VISIBLE VISIBLE} from the
     * {@link VisualStateCallback#onComplete} method.</li>
     * </ul>
     *
     * <p>When using this API it is also recommended to enable pre-rasterization if the {@link
     * WebView} is off screen to avoid flickering. See
     * {@link android.webkit.WebSettings#setOffscreenPreRaster} for more details and do consider its
     * caveats.
     *
     * @param requestId An id that will be returned in the callback to allow callers to match
     *                  requests with callbacks.
     * @param callback  The callback to be invoked.
     */
    public static void postVisualStateCallback(@NonNull WebView webview, long requestId,
            @NonNull final VisualStateCallback callback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webview.postVisualStateCallback(requestId,
                    new android.webkit.WebView.VisualStateCallback() {
                        @Override
                        public void onComplete(long l) {
                            callback.onComplete(l);
                        }
                    });
        } else {
            // TODO(gsennton): guard with if WebViewApk.hasFeature(POSTVISUALSTATECALLBACK)
            checkThread(webview);
            getProvider(webview).insertVisualStateCallback(requestId, callback);
        }
    }

    /**
     * Starts Safe Browsing initialization.
     * <p>
     * URL loads are not guaranteed to be protected by Safe Browsing until after {@code callback} is
     * invoked with {@code true}. Safe Browsing is not fully supported on all devices. For those
     * devices {@code callback} will receive {@code false}.
     * <p>
     * This should not be called if Safe Browsing has been disabled by manifest tag or {@link
     * android.webkit.WebSettings#setSafeBrowsingEnabled}. This prepares resources used for Safe
     * Browsing.
     * <p>
     * This should be called with the Application Context (and will always use the Application
     * context to do its work regardless).
     *
     * @param context Application Context.
     * @param callback will be called on the UI thread with {@code true} if initialization is
     * successful, {@code false} otherwise.
     */
    public static void startSafeBrowsing(@NonNull Context context,
            @Nullable ValueCallback<Boolean> callback) {
        if (Build.VERSION.SDK_INT >= 27) {
            WebView.startSafeBrowsing(context, callback);
        } else { // TODO(gsennton): guard with WebViewApk.hasFeature(SafeBrowsing)
            getFactory().getStatics().initSafeBrowsing(context, callback);
        }
    }

    /**
     * Sets the list of hosts (domain names/IP addresses) that are exempt from SafeBrowsing checks.
     * The list is global for all the WebViews.
     * <p>
     * Each rule should take one of these:
     * <table>
     * <tr><th> Rule </th> <th> Example </th> <th> Matches Subdomain</th> </tr>
     * <tr><td> HOSTNAME </td> <td> example.com </td> <td> Yes </td> </tr>
     * <tr><td> .HOSTNAME </td> <td> .example.com </td> <td> No </td> </tr>
     * <tr><td> IPV4_LITERAL </td> <td> 192.168.1.1 </td> <td> No </td></tr>
     * <tr><td> IPV6_LITERAL_WITH_BRACKETS </td><td>[10:20:30:40:50:60:70:80]</td><td>No</td></tr>
     * </table>
     * <p>
     * All other rules, including wildcards, are invalid.
     * <p>
     * The correct syntax for hosts is defined by <a
     * href="https://tools.ietf.org/html/rfc3986#section-3.2.2">RFC 3986</a>.
     *
     * @param hosts the list of hosts
     * @param callback will be called with {@code true} if hosts are successfully added to the
     * whitelist. It will be called with {@code false} if any hosts are malformed. The callback
     * will be run on the UI thread
     */
    public static void setSafeBrowsingWhitelist(@NonNull List<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        if (Build.VERSION.SDK_INT >= 27) {
            WebView.setSafeBrowsingWhitelist(hosts, callback);
        } else { // TODO(gsennton): guard with WebViewApk.hasFeature(SafeBrowsing)
            getFactory().getStatics().setSafeBrowsingWhitelist(hosts, callback);
        }
    }

    /**
     * Returns a URL pointing to the privacy policy for Safe Browsing reporting.
     *
     * @return the url pointing to a privacy policy document which can be displayed to users.
     */
    @NonNull
    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        if (Build.VERSION.SDK_INT >= 27) {
            return WebView.getSafeBrowsingPrivacyPolicyUrl();
        } else { // TODO(gsennton): guard with WebViewApk.hasFeature(SafeBrowsing)
            return getFactory().getStatics().getSafeBrowsingPrivacyPolicyUrl();
        }
    }

    private static WebViewProviderAdapter getProvider(WebView webview) {
        return new WebViewProviderAdapter(createProvider(webview));
    }

    private static WebViewProviderFactoryAdapter getFactory() {
        return WebViewGlueCommunicator.getFactory();
    }

    private static WebViewProviderBoundaryInterface createProvider(WebView webview) {
        return getFactory().createWebView(webview);
    }

    @SuppressWarnings("NewApi")
    private static void checkThread(WebView webview) {
        if (BuildCompat.isAtLeastP()) {
            if (webview.getLooper() != Looper.myLooper()) {
                throw new RuntimeException("A WebView method was called on thread '"
                        + Thread.currentThread().getName() + "'. "
                        + "All WebView methods must be called on the same thread. "
                        + "(Expected Looper " + webview.getLooper() + " called on "
                        + Looper.myLooper() + ", FYI main Looper is " + Looper.getMainLooper()
                        + ")");
            }
        } else {
            try {
                Method checkThreadMethod = WebView.class.getDeclaredMethod("checkThread");
                checkThreadMethod.setAccessible(true);
                // WebView.checkThread() performs some logging and potentially throws an exception
                // if WebView is used on the wrong thread.
                checkThreadMethod.invoke(webview);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
