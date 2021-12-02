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
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresFeature;
import androidx.annotation.UiThread;
import androidx.webkit.internal.WebMessagePortImpl;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;
import androidx.webkit.internal.WebViewProviderAdapter;
import androidx.webkit.internal.WebViewProviderFactory;
import androidx.webkit.internal.WebViewRenderProcessClientFrameworkAdapter;
import androidx.webkit.internal.WebViewRenderProcessImpl;

import org.chromium.support_lib_boundary.WebViewProviderBoundaryInterface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

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
        @UiThread
        void onComplete(long requestId);
    }

    /**
     * This listener receives messages sent on the JavaScript object which was injected by {@link
     * #addWebMessageListener(WebView, String, Set, WebViewCompat.WebMessageListener)}.
     */
    public interface WebMessageListener {
        /**
         * Receives a message sent by a {@code postMessage()} on the injected JavaScript object.
         *
         * <p> Note that when the frame is {@code file:} or {@code content:} origin, the value of
         * {@code sourceOrigin} is a string {@code "null"}. However we highly recommend to not use
         * {@code file:} or {@code content:} URLs, see {@link WebViewAssetLoader} for serving local
         * content under {@code http:} or {@code https:} domain.
         *
         * @param view The {@link WebView} containing the frame which sent this message.
         * @param message The message from JavaScript.
         * @param sourceOrigin The origin of the frame that the message is from.
         * @param isMainFrame {@code true} If the message is from the main frame.
         * @param replyProxy Used to reply back to the JavaScript object.
         */
        @UiThread
        void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin, boolean isMainFrame,
                @NonNull JavaScriptReplyProxy replyProxy);
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#VISUAL_STATE_CALLBACK}.
     *
     * @param requestId An id that will be returned in the callback to allow callers to match
     *                  requests with callbacks.
     * @param callback  The callback to be invoked.
     */
    @RequiresFeature(name = WebViewFeature.VISUAL_STATE_CALLBACK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void postVisualStateCallback(@NonNull WebView webview, long requestId,
            @NonNull final VisualStateCallback callback) {
        WebViewFeatureInternal feature = WebViewFeatureInternal.VISUAL_STATE_CALLBACK;
        if (feature.isSupportedByFramework()) {
            webview.postVisualStateCallback(requestId,
                    new android.webkit.WebView.VisualStateCallback() {
                        @Override
                        public void onComplete(long l) {
                            callback.onComplete(l);
                        }
                    });
        } else if (feature.isSupportedByWebView()) {
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#START_SAFE_BROWSING}.
     *
     * @param context Application Context.
     * @param callback will be called on the UI thread with {@code true} if initialization is
     * successful, {@code false} otherwise.
     */
    @RequiresFeature(name = WebViewFeature.START_SAFE_BROWSING,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void startSafeBrowsing(@NonNull Context context,
            @Nullable ValueCallback<Boolean> callback) {
        WebViewFeatureInternal feature = WebViewFeatureInternal.START_SAFE_BROWSING;
        if (feature.isSupportedByFramework()) {
            WebView.startSafeBrowsing(context, callback);
        } else if (feature.isSupportedByWebView()) {
            getFactory().getStatics().initSafeBrowsing(context, callback);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Configures a set of hosts (domain names/IP addresses) that are exempt from SafeBrowsing
     * checks. The set is global for all the WebViews.
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SAFE_BROWSING_ALLOWLIST}.
     *
     * @param hosts the set of hosts for which to skip Safe Browsing checks
     * @param callback will be called with {@code true} if hosts are successfully added to the
     * allowlist, {@code false} if any hosts are malformed. The callback will be run on the UI
     * thread
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_ALLOWLIST,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setSafeBrowsingAllowlist(@NonNull Set<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        WebViewFeatureInternal preferredFeature =
                WebViewFeatureInternal.SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_PREFERRED;
        WebViewFeatureInternal deprecatedFeature =
                WebViewFeatureInternal.SAFE_BROWSING_ALLOWLIST_PREFERRED_TO_DEPRECATED;
        if (preferredFeature.isSupportedByWebView()) {
            getFactory().getStatics().setSafeBrowsingAllowlist(hosts, callback);
            return;
        }
        List<String> hostsList = new ArrayList<>(hosts);
        if (deprecatedFeature.isSupportedByFramework()) {
            WebView.setSafeBrowsingWhitelist(hostsList, callback);
        } else if (deprecatedFeature.isSupportedByWebView()) {
            getFactory().getStatics().setSafeBrowsingWhitelist(hostsList, callback);
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SAFE_BROWSING_WHITELIST}.
     *
     * @param hosts the list of hosts
     * @param callback will be called with {@code true} if hosts are successfully added to the
     * allowlist. It will be called with {@code false} if any hosts are malformed. The callback
     * will be run on the UI thread
     *
     * @deprecated Please use {@link #setSafeBrowsingAllowlist(Set, ValueCallback)} instead.
     */
    @Deprecated
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_WHITELIST,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setSafeBrowsingWhitelist(@NonNull List<String> hosts,
            @Nullable ValueCallback<Boolean> callback) {
        setSafeBrowsingAllowlist(new HashSet<>(hosts), callback);
    }

    /**
     * Returns a URL pointing to the privacy policy for Safe Browsing reporting.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SAFE_BROWSING_PRIVACY_POLICY_URL}.
     *
     * @return the url pointing to a privacy policy document which can be displayed to users.
     */
    @NonNull
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_PRIVACY_POLICY_URL,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static Uri getSafeBrowsingPrivacyPolicyUrl() {
        WebViewFeatureInternal feature =
                WebViewFeatureInternal.SAFE_BROWSING_PRIVACY_POLICY_URL;
        if (feature.isSupportedByFramework()) {
            return WebView.getSafeBrowsingPrivacyPolicyUrl();
        } else if (feature.isSupportedByWebView()) {
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
    @SuppressLint("PrivateApi")
    private static PackageInfo getLoadedWebViewPackageInfo()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");
        return (PackageInfo) webViewFactoryClass.getMethod(
                "getLoadedPackageInfo").invoke(null);
    }

    /**
     * Return the PackageInfo of the WebView APK that would have been used as WebView implementation
     * if WebView was to be loaded right now.
     */
    @SuppressLint("PrivateApi")
    private static PackageInfo getNotYetLoadedWebViewPackageInfo(Context context) {
        String webviewPackageName;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                    && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                Class<?> webViewFactoryClass = Class.forName("android.webkit.WebViewFactory");

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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#CREATE_WEB_MESSAGE_CHANNEL}.
     *
     * @return an array of size two, containing the two message ports that form the message channel.
     */
    @RequiresFeature(name = WebViewFeature.CREATE_WEB_MESSAGE_CHANNEL,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @NonNull WebMessagePortCompat[] createWebMessageChannel(
            @NonNull WebView webview) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.CREATE_WEB_MESSAGE_CHANNEL;
        if (feature.isSupportedByFramework()) {
            return WebMessagePortImpl.portsToCompat(webview.createWebMessageChannel());
        } else if (feature.isSupportedByWebView()) {
            return getProvider(webview).createWebMessageChannel();
        } else {
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
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#POST_WEB_MESSAGE}.
     *
     * @param message the WebMessage
     * @param targetOrigin the target origin.
     */
    @RequiresFeature(name = WebViewFeature.POST_WEB_MESSAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void postWebMessage(@NonNull WebView webview, @NonNull WebMessageCompat message,
            @NonNull Uri targetOrigin) {
        // The wildcard ("*") Uri was first supported in WebView 60, see
        // crrev/5ec5b67cbab33cea51b0ee11a286c885c2de4d5d, so on some Android versions using "*"
        // won't work. WebView has always supported using an empty Uri "" as a wildcard - so convert
        // "*" into "" here.
        if (WILDCARD_URI.equals(targetOrigin)) {
            targetOrigin = EMPTY_URI;
        }

        final WebViewFeatureInternal feature = WebViewFeatureInternal.POST_WEB_MESSAGE;
        if (feature.isSupportedByFramework()) {
            webview.postWebMessage(
                    WebMessagePortImpl.compatToFrameworkMessage(message),
                    targetOrigin);
        } else if (feature.isSupportedByWebView()) {
            getProvider(webview).postWebMessage(message, targetOrigin);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Adds a {@link WebMessageListener} to the {@link WebView} and injects a JavaScript object into
     * each frame that the {@link WebMessageListener} will listen on.
     *
     * <p>
     * The injected JavaScript object will be named {@code jsObjectName} in the global scope. This
     * will inject the JavaScript object in any frame whose origin matches {@code
     * allowedOriginRules} for every navigation after this call, and the JavaScript object will be
     * available immediately when the page begins to load.
     *
     * <p>
     * Each {@code allowedOriginRules} entry must follow the format {@code SCHEME "://" [
     * HOSTNAME_PATTERN [ ":" PORT ] ]}, each part is explained in the below table:
     *
     * <table>
     * <tr><th>Rule</th><th>Description</th><th>Example</th></tr>
     *
     * <tr>
     * <td>http/https with hostname</td>
     * <td>{@code SCHEME} is http or https; {@code HOSTNAME_PATTERN} is a regular hostname; {@code
     * PORT} is optional, when not present, the rule will match port {@code 80} for http and port
     * {@code 443} for https.</td>
     * <td><ul>
     * <li>{@code https://foobar.com:8080} - Matches https:// URL on port 8080, whose normalized
     * host is foobar.com.</li>
     * <li>{@code https://www.example.com} - Matches https:// URL on port 443, whose normalized host
     * is www.example.com.</li>
     * </ul></td>
     * </tr>
     *
     * <tr>
     * <td>http/https with pattern matching</td>
     * <td>{@code SCHEME} is http or https; {@code HOSTNAME_PATTERN} is a sub-domain matching
     * pattern with a leading {@code *.}; {@code PORT} is optional, when not present, the rule will
     * match port {@code 80} for http and port {@code 443} for https.</td>
     *
     * <td><ul>
     * <li>{@code https://*.example.com} - Matches https://calendar.example.com and
     * https://foo.bar.example.com but not https://example.com.</li>
     * <li>{@code https://*.example.com:8080} - Matches https://calendar.example.com:8080</li>
     * </ul></td>
     * </tr>
     *
     * <tr>
     * <td>http/https with IP literal</td>
     * <td>{@code SCHEME} is https or https; {@code HOSTNAME_PATTERN} is IP literal; {@code PORT} is
     * optional, when not present, the rule will match port {@code 80} for http and port {@code 443}
     * for https.</td>
     *
     * <td><ul>
     * <li>{@code https://127.0.0.1} - Matches https:// URL on port 443, whose IPv4 address is
     * 127.0.0.1</li>
     * <li>{@code https://[::1]} or {@code https://[0:0::1]}- Matches any URL to the IPv6 loopback
     * address with port 443.</li>
     * <li>{@code https://[::1]:99} - Matches any https:// URL to the IPv6 loopback on port 99.</li>
     * </ul></td>
     * </tr>
     *
     * <tr>
     * <td>Custom scheme</td>
     * <td>{@code SCHEME} is a custom scheme; {@code HOSTNAME_PATTERN} and {@code PORT} must not be
     * present.</td>
     * <td><ul>
     * <li>{@code my-app-scheme://} - Matches any my-app-scheme:// URL.</li>
     * </ul></td>
     * </tr>
     *
     * <tr><td>{@code *}</td>
     * <td>Wildcard rule, matches any origin.</td>
     * <td><ul><li>{@code *}</li></ul></td>
     * </table>
     *
     * <p>
     * Note that this is a powerful API, as the JavaScript object will be injected when the frame's
     * origin matches any one of the allowed origins. The HTTPS scheme is strongly recommended for
     * security; allowing HTTP origins exposes the injected object to any potential network-based
     * attackers. If a wildcard {@code "*"} is provided, it will inject the JavaScript object to all
     * frames. A wildcard should only be used if the app wants <b>any</b> third party web page to be
     * able to use the injected object. When using a wildcard, the app must treat received messages
     * as untrustworthy and validate any data carefully.
     *
     * <p>
     * This method can be called multiple times to inject multiple JavaScript objects.
     *
     * <p>
     * Let's say the injected JavaScript object is named {@code myObject}. We will have following
     * methods on that object once it is available to use:
     * <pre class="prettyprint">
     * // Web page (in JavaScript)
     * // message needs to be a JavaScript String, MessagePorts is an optional parameter.
     * myObject.postMessage(message[, MessagePorts])
     *
     * // To receive messages posted from the app side, assign a function to the "onmessage"
     * // property. This function should accept a single "event" argument. "event" has a "data"
     * // property, which is the message string from the app side.
     * myObject.onmessage = function(event) { ... }
     *
     * // To be compatible with DOM EventTarget's addEventListener, it accepts type and listener
     * // parameters, where type can be only "message" type and listener can only be a JavaScript
     * // function for myObject. An event object will be passed to listener with a "data" property,
     * // which is the message string from the app side.
     * myObject.addEventListener(type, listener)
     *
     * // To be compatible with DOM EventTarget's removeEventListener, it accepts type and listener
     * // parameters, where type can be only "message" type and listener can only be a JavaScript
     * // function for myObject.
     * myObject.removeEventListener(type, listener)
     * </pre>
     *
     * <p>
     * We start the communication between JavaScript and the app from the JavaScript side. In order
     * to send message from the app to JavaScript, it needs to post a message from JavaScript first,
     * so the app will have a {@link JavaScriptReplyProxy} object to respond. Example:
     * <pre class="prettyprint">
     * // Web page (in JavaScript)
     * myObject.onmessage = function(event) {
     *   // prints "Got it!" when we receive the app's response.
     *   console.log(event.data);
     * }
     * myObject.postMessage("I'm ready!");
     * </pre>
     * <pre class="prettyprint">
     * // App (in Java)
     * WebMessageListener myListener = new WebMessageListener() {
     *   &#064;Override
     *   public void onPostMessage(WebView view, WebMessageCompat message, Uri sourceOrigin,
     *            boolean isMainFrame, JavaScriptReplyProxy replyProxy) {
     *     // do something about view, message, sourceOrigin and isMainFrame.
     *     replyProxy.postMessage("Got it!");
     *   }
     * };
     * if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
     *   WebViewCompat.addWebMessageListener(webView, "myObject", rules, myListener);
     * }
     * </pre>
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#WEB_MESSAGE_LISTENER}.
     *
     * @param webView The {@link WebView} instance that we are interacting with.
     * @param jsObjectName The name for the injected JavaScript object for this {@link
     *         WebMessageListener}.
     * @param allowedOriginRules A set of matching rules for the allowed origins.
     * @param listener The {@link WebMessageListener WebMessageListener} to handle postMessage()
     *         calls on the JavaScript object.
     * @throws IllegalArgumentException If one of the {@code allowedOriginRules} is invalid.
     *
     * @see JavaScriptReplyProxy
     * @see WebMessageListener
     */
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void addWebMessageListener(@NonNull WebView webView, @NonNull String jsObjectName,
            @NonNull Set<String> allowedOriginRules, @NonNull WebMessageListener listener) {
        // TODO(b/159823546): Remove the SDK_INT check and put it into
        // WebViewFeatureInternal.isSupportedByWebView() after the bug is resolved.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new AssertionError("Should be on Lollipop and above.");
        }
        final WebViewFeatureInternal feature = WebViewFeatureInternal.WEB_MESSAGE_LISTENER;
        if (feature.isSupportedByWebView()) {
            getProvider(webView).addWebMessageListener(
                    jsObjectName, allowedOriginRules.toArray(new String[0]), listener);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Removes the {@link WebMessageListener WebMessageListener} associated with {@code
     * jsObjectName}.
     *
     * <p>
     * Note that after this call, the injected JavaScript object is still in the JavaScript context,
     * however any message sent after this call won't reach the {@link WebMessageListener
     * WebMessageListener}.
     *
     * <p>
     * This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#WEB_MESSAGE_LISTENER}.
     *
     * @param jsObjectName The JavaScript object's name that was previously passed to {@link
     *         #addWebMessageListener(WebView, String, Set, WebMessageListener)}.
     *
     * @see #addWebMessageListener(WebView, String, Set, WebMessageListener)
     */
    @RequiresFeature(name = WebViewFeature.WEB_MESSAGE_LISTENER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void removeWebMessageListener(
            @NonNull WebView webview, @NonNull String jsObjectName) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.WEB_MESSAGE_LISTENER;
        if (feature.isSupportedByWebView()) {
            getProvider(webview).removeWebMessageListener(jsObjectName);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Adds a JavaScript script to the {@link WebView} which will be executed in any frame whose
     * origin matches {@code allowedOriginRules} when the document begins to load.
     *
     * <p>Note that the script will run before any of the page's JavaScript code and the DOM tree
     * might not be ready at this moment. It will block the loadng of the page until it's finished,
     * so should be kept as short as possible.
     *
     * <p>The injected object from {@link #addWebMessageListener(WebView, String, Set,
     * WebMessageListener)} API will be injected first and the script can rely on the injected
     * object to send messages to the app.
     *
     * <p>The script will only run in frames which begin loading after the call returns, therefore
     * it should typically be called before making any {@code loadUrl()}, {@code loadData()} or
     * {@code loadDataWithBaseURL()} call to load the page.
     *
     * <p>This method can be called multiple times to inject multiple scripts. If more than one
     * script matches a frame's origin, they will be executed in the order they were added.
     *
     * <p>See {@link #addWebMessageListener(WebView, String, Set, WebMessageListener)} for the rules
     * of the {@code allowedOriginRules} parameter.
     *
     * <p>This method should only be called if {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#DOCUMENT_START_SCRIPT}.
     *
     * @param webview The {@link WebView} instance that we are interacting with.
     * @param script The JavaScript script to be executed.
     * @param allowedOriginRules A set of matching rules for the allowed origins.
     * @return the {@link ScriptHandler}, which is a handle for removing the script.
     * @throws IllegalArgumentException If one of the {@code allowedOriginRules} is invalid.
     * @see #addWebMessageListener(WebView, String, Set, WebMessageListener)
     * @see ScriptHandler
     */
    @RequiresFeature(
            name = WebViewFeature.DOCUMENT_START_SCRIPT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @NonNull ScriptHandler addDocumentStartJavaScript(
            @NonNull WebView webview,
            @NonNull String script,
            @NonNull Set<String> allowedOriginRules) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.DOCUMENT_START_SCRIPT;
        if (feature.isSupportedByWebView()) {
            return getProvider(webview)
                    .addDocumentStartJavaScript(script, allowedOriginRules.toArray(new String[0]));
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets the WebViewClient for the WebView argument.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#GET_WEB_VIEW_CLIENT}.
     *
     * @return the WebViewClient, or a default client if not yet set
     */
    @RequiresFeature(name = WebViewFeature.GET_WEB_VIEW_CLIENT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @NonNull WebViewClient getWebViewClient(@NonNull WebView webview) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.GET_WEB_VIEW_CLIENT;
        if (feature.isSupportedByFramework()) {
            return webview.getWebViewClient();
        } else if (feature.isSupportedByWebView()) {
            return getProvider(webview).getWebViewClient();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets the WebChromeClient.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#GET_WEB_CHROME_CLIENT}.
     *
     * @return the WebChromeClient, or {@code null} if not yet set
     */
    @RequiresFeature(name = WebViewFeature.GET_WEB_CHROME_CLIENT,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @Nullable WebChromeClient getWebChromeClient(@NonNull WebView webview) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.GET_WEB_CHROME_CLIENT;
        if (feature.isSupportedByFramework()) {
            return webview.getWebChromeClient();
        } else if (feature.isSupportedByWebView()) {
            return getProvider(webview).getWebChromeClient();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets the WebView renderer associated with this WebView.
     *
     * <p>In Android O and above, WebView may run in "multiprocess"
     * mode. In multiprocess mode, rendering of web content is performed by
     * a sandboxed renderer process separate to the application process.
     * This renderer process may be shared with other WebViews in the
     * application, but is not shared with other application processes.
     *
     * <p>If WebView is running in multiprocess mode, this method returns a
     * handle to the renderer process associated with the WebView, which can
     * be used to control the renderer process.
     *
     * <p>This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#GET_WEB_VIEW_RENDERER}.
     *
     * @return the {@link WebViewRenderProcess} renderer handle associated
     *         with this {@link android.webkit.WebView}, or {@code null} if
     *         WebView is not running in multiprocess mode.
     */
    @RequiresFeature(name = WebViewFeature.GET_WEB_VIEW_RENDERER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @Nullable WebViewRenderProcess getWebViewRenderProcess(@NonNull WebView webview) {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.GET_WEB_VIEW_RENDERER;
        if (feature.isSupportedByFramework()) {
            android.webkit.WebViewRenderProcess renderer = webview.getWebViewRenderProcess();
            return renderer != null ? WebViewRenderProcessImpl.forFrameworkObject(renderer) : null;
        } else if (feature.isSupportedByWebView()) {
            return getProvider(webview).getWebViewRenderProcess();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the renderer client object associated with this WebView.
     *
     * <p>The renderer client encapsulates callbacks relevant to WebView renderer
     * state. See {@link WebViewRenderProcessClient} for details.
     *
     * <p>Although many WebView instances may share a single underlying renderer, and renderers may
     * live either in the application process, or in a sandboxed process that is isolated from
     * the application process, instances of {@link WebViewRenderProcessClient} are set per-WebView.
     * Callbacks represent renderer events from the perspective of this WebView, and may or may
     * not be correlated with renderer events affecting other WebViews.
     *
     * <p>The renderer client encapsulates callbacks relevant to WebView renderer
     * state. See {@link WebViewRenderProcessClient} for details.
     *
     * <p>Although many WebView instances may share a single underlying renderer, and renderers may
     * live either in the application process, or in a sandboxed process that is isolated from
     * the application process, instances of {@link WebViewRenderProcessClient} are set per-WebView.
     * Callbacks represent renderer events from the perspective of this WebView, and may or may
     * not be correlated with renderer events affecting other WebViews.
     *
     * <p>This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE}.
     *
     * @param webview the {@link WebView} on which to monitor responsiveness.
     * @param executor the {@link Executor} that will be used to execute callbacks.
     * @param webViewRenderProcessClient the {@link WebViewRenderProcessClient} to set for
     *                                   callbacks.
     */
    // WebViewRenderProcessClient is a callback class, so it should be last. See
    // https://issuetracker.google.com/issues/139770271.
    @SuppressLint({"LambdaLast"})
    @RequiresFeature(name = WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setWebViewRenderProcessClient(
            @NonNull WebView webview,
            @NonNull /* @CallbackExecutor */ Executor executor,
            @NonNull WebViewRenderProcessClient webViewRenderProcessClient) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            webview.setWebViewRenderProcessClient(executor, webViewRenderProcessClient != null
                    ? new WebViewRenderProcessClientFrameworkAdapter(webViewRenderProcessClient)
                    : null);
        } else if (feature.isSupportedByWebView()) {
            getProvider(webview).setWebViewRenderProcessClient(
                    executor, webViewRenderProcessClient);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the renderer client object associated with this WebView.
     *
     * <p>See {@link WebViewCompat#setWebViewRenderProcessClient(WebView,Executor,WebViewRenderProcessClient)} for
     * details, with the following differences:
     *
     * <p>Callbacks will execute directly on the thread on which this WebView was instantiated.
     *
     * <p>Passing {@code null} for {@code webViewRenderProcessClient} will clear the renderer client
     * object for this WebView.
     *
     * <p>This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE}.
     *
     * @param webview the {@link WebView} on which to monitor responsiveness.
     * @param webViewRenderProcessClient the {@link WebViewRenderProcessClient} to set for
     *                                   callbacks.
     */
    @RequiresFeature(name = WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setWebViewRenderProcessClient(
            @NonNull WebView webview,
            @Nullable WebViewRenderProcessClient webViewRenderProcessClient) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            webview.setWebViewRenderProcessClient(webViewRenderProcessClient != null
                    ? new WebViewRenderProcessClientFrameworkAdapter(webViewRenderProcessClient)
                    : null);
        } else if (feature.isSupportedByWebView()) {
            getProvider(webview).setWebViewRenderProcessClient(null, webViewRenderProcessClient);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets the renderer client object associated with this WebView.
     *
     * <p>This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE}.
     *
     * @return the {@link WebViewRenderProcessClient} object associated with this WebView, if
     * one has been set via
     * {@link #setWebViewRenderProcessClient(WebView,WebViewRenderProcessClient)} or {@code null}
     * otherwise.
     */
    @RequiresFeature(name = WebViewFeature.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @Nullable WebViewRenderProcessClient getWebViewRenderProcessClient(
            @NonNull WebView webview) {
        final WebViewFeatureInternal feature =
                WebViewFeatureInternal.WEB_VIEW_RENDERER_CLIENT_BASIC_USAGE;
        if (feature.isSupportedByFramework()) {
            android.webkit.WebViewRenderProcessClient renderer =
                    webview.getWebViewRenderProcessClient();
            if (renderer == null
                    || !(renderer instanceof WebViewRenderProcessClientFrameworkAdapter)) {
                return null;
            }
            return ((WebViewRenderProcessClientFrameworkAdapter) renderer)
                .getFrameworkRenderProcessClient();
        } else if (feature.isSupportedByWebView()) {
            return getProvider(webview).getWebViewRenderProcessClient();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Returns true if {@link WebView} is running in multi process mode.
     *
     * <p>In Android O and above, WebView may run in "multiprocess"
     * mode. In multiprocess mode, rendering of web content is performed by
     * a sandboxed renderer process separate to the application process.
     * This renderer process may be shared with other WebViews in the
     * application, but is not shared with other application processes.
     */
    @RequiresFeature(name = WebViewFeature.MULTI_PROCESS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean isMultiProcessEnabled() {
        final WebViewFeatureInternal feature = WebViewFeatureInternal.MULTI_PROCESS;
        if (feature.isSupportedByWebView()) {
            return getFactory().getStatics().isMultiProcessEnabled();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private static WebViewProviderFactory getFactory() {
        return WebViewGlueCommunicator.getFactory();
    }

    private static WebViewProviderBoundaryInterface createProvider(WebView webview) {
        return getFactory().createWebView(webview);
    }

    @SuppressWarnings({"JavaReflectionMemberAccess", "PrivateApi"})
    private static void checkThread(WebView webview) {
        if (Build.VERSION.SDK_INT >= 28) {
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
