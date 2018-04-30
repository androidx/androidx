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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.core.os.BuildCompat;
import androidx.webkit.internal.WebMessagePortImpl;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebViewProviderAdapter;
import androidx.webkit.internal.WebViewProviderFactory;

import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility version of {@link android.webkit.WebView}
 */
public class WebViewCompat {
    private static final Uri WILDCARD_URI = Uri.parse("*");
    private static final Uri EMPTY_URI = Uri.parse("");

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
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#VISUAL_STATE_CALLBACK}.
     *
     * @param requestId An id that will be returned in the callback to allow callers to match
     *                  requests with callbacks.
     * @param callback  The callback to be invoked.
     */
    @SuppressWarnings("NewApi")
    @RequiresFeature(name = WebViewFeature.VISUAL_STATE_CALLBACK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void postVisualStateCallback(@NonNull WebView webview, long requestId,
            @NonNull final VisualStateCallback callback) {
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.VISUAL_STATE_CALLBACK);
        if (webViewFeature.isSupportedByFramework()) {
            webview.postVisualStateCallback(requestId,
                    new android.webkit.WebView.VisualStateCallback() {
                        @Override
                        public void onComplete(long l) {
                            callback.onComplete(l);
                        }
                    });
        } else if (webViewFeature.isSupportedByWebView()) {
            checkThread(webview);
            getProvider(webview).insertVisualStateCallback(requestId, callback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.START_SAFE_BROWSING,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void startSafeBrowsing(@NonNull Context context,
            @Nullable ValueCallback<Boolean> callback) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.START_SAFE_BROWSING);
        if (webviewFeature.isSupportedByFramework()) {
            WebView.startSafeBrowsing(context, callback);
        } else if (webviewFeature.isSupportedByWebView()) {
            getFactory().getStatics().initSafeBrowsing(context, callback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_WHITELIST,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setSafeBrowsingWhitelist(@NonNull List<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_WHITELIST);
        if (webviewFeature.isSupportedByFramework()) {
            WebView.setSafeBrowsingWhitelist(hosts, callback);
        } else if (webviewFeature.isSupportedByWebView()) {
            getFactory().getStatics().setSafeBrowsingWhitelist(hosts, callback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns a URL pointing to the privacy policy for Safe Browsing reporting.
     *
     * @return the url pointing to a privacy policy document which can be displayed to users.
     */
    @SuppressLint("NewApi")
    @NonNull
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL);
        if (webviewFeature.isSupportedByFramework()) {
            return WebView.getSafeBrowsingPrivacyPolicyUrl();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getFactory().getStatics().getSafeBrowsingPrivacyPolicyUrl();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * If WebView has already been loaded into the current process this method will return the
     * package that was used to load it. Otherwise, the package that would be used if the WebView
     * was loaded right now will be returned; this does not cause WebView to be loaded, so this
     * information may become outdated at any time.
     * The WebView package changes either when the current WebView package is updated, disabled, or
     * uninstalled. It can also be changed through a Developer Setting.
     * If the WebView package changes, any app process that has loaded WebView will be killed. The
     * next time the app starts and loads WebView it will use the new WebView package instead.
     * @return the current WebView package, or {@code null} if there is none.
     */
    // Note that this API is not protected by a {@link androidx.webkit.WebViewFeature} since
    // this feature is not dependent on the WebView APK.
    @Nullable
    public static PackageInfo getCurrentWebViewPackage(@NonNull Context context) {
        // There was no WebView Package before Lollipop, the WebView code was part of the framework
        // back then.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WebView.getCurrentWebViewPackage();
        } else { // L-N
            try {
                PackageInfo loadedWebViewPackageInfo = getLoadedWebViewPackageInfo();
                if (loadedWebViewPackageInfo != null) return loadedWebViewPackageInfo;
            } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException
                | NoSuchMethodException  e) {
                return null;
            }

            // If WebViewFactory.getLoadedPackageInfo() returns null then WebView hasn't been loaded
            // yet, in that case we need to fetch the name of the WebView package, and fetch the
            // corresponding PackageInfo through the PackageManager
            return getNotYetLoadedWebViewPackageInfo(context);
        }
    }

    /**
     * Return the PackageInfo of the currently loaded WebView APK. This method uses reflection and
     * propagates any exceptions thrown, to the caller.
     */
    private static PackageInfo getLoadedWebViewPackageInfo()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
        PackageInfo webviewPackageInfo =
                (PackageInfo) webViewFactoryClass.getMethod(
                        "getLoadedPackageInfo").invoke(null);
        return webviewPackageInfo;
    }

    /**
     * Return the PackageInfo of the WebView APK that would have been used as WebView implementation
     * if WebView was to be loaded right now.
     */
    private static PackageInfo getNotYetLoadedWebViewPackageInfo(Context context) {
        String webviewPackageName = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                Class<?> webViewFactoryClass = null;
                webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");

                webviewPackageName = (String) webViewFactoryClass.getMethod(
                        "getWebViewPackageName").invoke(null);
            } else {
                Class<?> webviewUpdateServiceClass =
                        Class.forName("android.webkit.WebViewUpdateService");
                webviewPackageName = (String) webviewUpdateServiceClass.getMethod(
                        "getCurrentWebViewPackageName").invoke(null);
            }
        } catch (ClassNotFoundException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        }
        if (webviewPackageName == null) return null;
        PackageManager pm = context.getPackageManager();
        try {
            return pm.getPackageInfo(webviewPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static WebViewProviderAdapter getProvider(WebView webview) {
        return new WebViewProviderAdapter(createProvider(webview));
    }

    /**
     * Creates a message channel to communicate with JS and returns the message
     * ports that represent the endpoints of this message channel. The HTML5 message
     * channel functionality is described
     * <a href="https://html.spec.whatwg.org/multipage/comms.html#messagechannel">here
     * </a>
     *
     * <p>The returned message channels are entangled and already in started state.
     *
     * @return an array of size two, containing the two message ports that form the message channel.
     */
    public static @NonNull WebMessagePortCompat[] createWebMessageChannel(
            @NonNull WebView webview) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return WebMessagePortImpl.portsToCompat(webview.createWebMessageChannel());
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Post a message to main frame. The embedded application can restrict the
     * messages to a certain target origin. See
     * <a href="https://html.spec.whatwg.org/multipage/comms.html#posting-messages">
     * HTML5 spec</a> for how target origin can be used.
     * <p>
     * A target origin can be set as a wildcard ("*"). However this is not recommended.
     * See the page above for security issues.
     *
     * @param message the WebMessage
     * @param targetOrigin the target origin.
     */
    public static void postWebMessage(@NonNull WebView webview, @NonNull WebMessageCompat message,
            @NonNull Uri targetOrigin) {
        // The wildcard ("*") Uri was first supported in WebView 60, see
        // crrev/5ec5b67cbab33cea51b0ee11a286c885c2de4d5d, so on some Android versions using "*"
        // won't work. WebView has always supported using an empty Uri "" as a wildcard - so convert
        // "*" into "" here.
        if (WILDCARD_URI.equals(targetOrigin)) {
            targetOrigin = EMPTY_URI;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webview.postWebMessage(
                    WebMessagePortImpl.compatToFrameworkMessage(message),
                    targetOrigin);
        } else { // TODO(gsennton) add reflection-based implementation
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private static WebViewProviderFactory getFactory() {
        return WebViewGlueCommunicator.getFactory();
    }

    private static WebViewProviderBoundaryInterface createProvider(WebView webview) {
        return getFactory().createWebView(webview);
    }

    @SuppressWarnings("NewApi")
    private static void checkThread(WebView webview) {
        if (BuildCompat.isAtLeastP()) {
            if (webview.getWebViewLooper() != Looper.myLooper()) {
                throw new RuntimeException("A WebView method was called on thread '"
                        + Thread.currentThread().getName() + "'. "
                        + "All WebView methods must be called on the same thread. "
                        + "(Expected Looper " + webview.getWebViewLooper() + " called on "
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
