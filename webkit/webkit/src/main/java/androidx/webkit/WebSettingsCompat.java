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

import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.ApiFeature;
import androidx.webkit.internal.ApiHelperForM;
import androidx.webkit.internal.ApiHelperForN;
import androidx.webkit.internal.ApiHelperForO;
import androidx.webkit.internal.ApiHelperForQ;
import androidx.webkit.internal.WebSettingsAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import org.chromium.support_lib_boundary.WebSettingsBoundaryInterface;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Compatibility version of {@link android.webkit.WebSettings}
 */
public class WebSettingsCompat {
    private WebSettingsCompat() {}

    /**
     * Sets whether this WebView should raster tiles when it is
     * offscreen but attached to a window. Turning this on can avoid
     * rendering artifacts when animating an offscreen WebView on-screen.
     * Offscreen WebViews in this mode use more memory. The default value is
     * false.<br>
     * Please follow these guidelines to limit memory usage:
     * <ul>
     * <li> WebView size should be not be larger than the device screen size.
     * <li> Limit use of this mode to a small number of WebViews. Use it for
     *   visible WebViews and WebViews about to be animated to visible.
     * </ul>
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#OFF_SCREEN_PRERASTER}.
     */
    @RequiresFeature(name = WebViewFeature.OFF_SCREEN_PRERASTER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setOffscreenPreRaster(@NonNull WebSettings settings, boolean enabled) {
        ApiFeature.M feature = WebViewFeatureInternal.OFF_SCREEN_PRERASTER;
        if (feature.isSupportedByFramework()) {
            ApiHelperForM.setOffscreenPreRaster(settings, enabled);
        } else if (feature.isSupportedByWebView()) {
            getAdapter(settings).setOffscreenPreRaster(enabled);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets whether this WebView should raster tiles when it is
     * offscreen but attached to a window.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#OFF_SCREEN_PRERASTER}.
     *
     * @return {@code true} if this WebView will raster tiles when it is
     * offscreen but attached to a window.
     */
    @RequiresFeature(name = WebViewFeature.OFF_SCREEN_PRERASTER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean getOffscreenPreRaster(@NonNull WebSettings settings) {
        ApiFeature.M feature = WebViewFeatureInternal.OFF_SCREEN_PRERASTER;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForM.getOffscreenPreRaster(settings);
        } else if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getOffscreenPreRaster();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets whether Safe Browsing is enabled. Safe Browsing allows WebView to
     * protect against malware and phishing attacks by verifying the links.
     *
     * <p>
     * Safe Browsing can be disabled for all WebViews using a manifest tag (read <a
     * href="{@docRoot}reference/android/webkit/WebView.html">general Safe Browsing info</a>). The
     * manifest tag has a lower precedence than this API.
     *
     * <p>
     * Safe Browsing is enabled by default for devices which support it.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SAFE_BROWSING_ENABLE}.
     *
     * @param enabled Whether Safe Browsing is enabled.
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_ENABLE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setSafeBrowsingEnabled(@NonNull WebSettings settings, boolean enabled) {
        ApiFeature.O feature = WebViewFeatureInternal.SAFE_BROWSING_ENABLE;
        if (feature.isSupportedByFramework()) {
            ApiHelperForO.setSafeBrowsingEnabled(settings, enabled);
        } else if (feature.isSupportedByWebView()) {
            getAdapter(settings).setSafeBrowsingEnabled(enabled);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets whether Safe Browsing is enabled.
     * See {@link #setSafeBrowsingEnabled}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SAFE_BROWSING_ENABLE}.
     *
     * @return {@code true} if Safe Browsing is enabled and {@code false} otherwise.
     */
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_ENABLE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean getSafeBrowsingEnabled(@NonNull WebSettings settings) {
        ApiFeature.O feature = WebViewFeatureInternal.SAFE_BROWSING_ENABLE;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForO.getSafeBrowsingEnabled(settings);
        } else if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getSafeBrowsingEnabled();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {
            WebSettings.MENU_ITEM_NONE,
            WebSettings.MENU_ITEM_SHARE,
            WebSettings.MENU_ITEM_WEB_SEARCH,
            WebSettings.MENU_ITEM_PROCESS_TEXT
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface MenuItemFlags {}

    /**
     * Disables the action mode menu items according to {@code menuItems} flag.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#DISABLED_ACTION_MODE_MENU_ITEMS}.
     *
     * @param menuItems an integer field flag for the menu items to be disabled.
     */
    @RequiresFeature(name = WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setDisabledActionModeMenuItems(@NonNull WebSettings settings,
            @MenuItemFlags int menuItems) {
        ApiFeature.N feature = WebViewFeatureInternal.DISABLED_ACTION_MODE_MENU_ITEMS;
        if (feature.isSupportedByFramework()) {
            ApiHelperForN.setDisabledActionModeMenuItems(settings, menuItems);
        } else if (feature.isSupportedByWebView()) {
            getAdapter(settings).setDisabledActionModeMenuItems(menuItems);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets the action mode menu items that are disabled, expressed in an integer field flag.
     * The default value is {@link WebSettings#MENU_ITEM_NONE}
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#DISABLED_ACTION_MODE_MENU_ITEMS}.
     *
     * @return all the disabled menu item flags combined with bitwise OR.
     */
    @RequiresFeature(name = WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @MenuItemFlags int getDisabledActionModeMenuItems(@NonNull WebSettings settings) {
        ApiFeature.N feature = WebViewFeatureInternal.DISABLED_ACTION_MODE_MENU_ITEMS;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForN.getDisabledActionModeMenuItems(settings);
        } else if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getDisabledActionModeMenuItems();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Disable force dark, irrespective of the force dark mode of the WebView parent. In this mode,
     * WebView content will always be rendered as-is, regardless of whether native views are being
     * automatically darkened.
     *
     * @see #setForceDark
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int FORCE_DARK_OFF = WebSettings.FORCE_DARK_OFF;

    /**
     * Enable force dark dependent on the state of the WebView parent view. If the WebView parent
     * view is being automatically force darkened
     * (@see android.view.View#setForceDarkAllowed), then WebView content will be rendered
     * so as to emulate a dark theme. WebViews that are not attached to the view hierarchy will not
     * be inverted.
     *
     * <p class="note"> If your app uses a dark theme, WebView will not be inverted. Similarly, if
     * your app's theme inherits from a {@code DayNight} theme, WebView will not be inverted.
     * In either of these cases, you should control the mode manually with
     * {@link ForceDark#FORCE_DARK_ON} or {@link ForceDark#FORCE_DARK_OFF}.
     *
     * <p> See <a href="https://developer.android.com/guide/topics/ui/look-and-feel/darktheme#force_dark">
     * Force Dark documentation</a> for more information.
     *
     * @see #setForceDark
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int FORCE_DARK_AUTO = WebSettings.FORCE_DARK_AUTO;

    /**
     * Unconditionally enable force dark. In this mode WebView content will always be rendered so
     * as to emulate a dark theme.
     *
     * @see #setForceDark
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int FORCE_DARK_ON = WebSettings.FORCE_DARK_ON;

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            FORCE_DARK_OFF,
            FORCE_DARK_AUTO,
            FORCE_DARK_ON,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ForceDark {}

    /**
     * Set the force dark mode for this WebView.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#FORCE_DARK}.
     *
     * <p>
     * If equals to {@link ForceDark#FORCE_DARK_ON} then {@link #setForceDarkStrategy} is used to
     * specify darkening strategy.
     *
     * @param forceDarkMode the force dark mode to set.
     * @see #getForceDark
     * @deprecated The "force dark" model previously implemented by WebView was complex
     * and didn't interoperate well with current Web standards for
     * {@code prefers-color-scheme} and {@code color-scheme}. In apps with
     * {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU}
     * this API is a no-op and WebView will always use the dark style defined by web content
     * authors if the app's theme is dark. To customize the behavior, refer to
     * {@link #setAlgorithmicDarkeningAllowed}.
     */
    @Deprecated
    @RequiresFeature(name = WebViewFeature.FORCE_DARK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setForceDark(@NonNull WebSettings settings,
            @ForceDark int forceDarkMode) {
        ApiFeature.Q feature = WebViewFeatureInternal.FORCE_DARK;
        if (feature.isSupportedByFramework()) {
            ApiHelperForQ.setForceDark(settings, forceDarkMode);
        } else if (feature.isSupportedByWebView()) {
            getAdapter(settings).setForceDark(forceDarkMode);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get the force dark mode for this WebView.
     *
     * <p>
     * The default force dark mode is {@link #FORCE_DARK_AUTO}.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#FORCE_DARK}.
     *
     * @return the currently set force dark mode.
     * @see #setForceDark
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    @RequiresFeature(name = WebViewFeature.FORCE_DARK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @ForceDark int getForceDark(@NonNull WebSettings settings) {
        ApiFeature.Q feature = WebViewFeatureInternal.FORCE_DARK;
        if (feature.isSupportedByFramework()) {
            return ApiHelperForQ.getForceDark(settings);
        } else if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getForceDark();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Control whether algorithmic darkening is allowed.
     *
     * <p class="note">
     * <b>Note:</b> This API and the behaviour described only apply to apps with
     * {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU}.
     *
     * <p>
     * WebView always sets the media query {@code prefers-color-scheme} according to the app's
     * theme attribute {@link android.R.styleable#Theme_isLightTheme isLightTheme}, i.e.
     * {@code prefers-color-scheme} is {@code light} if isLightTheme is true or not specified,
     * otherwise it is {@code dark}. This means that the web content's light or dark style will
     * be applied automatically to match the app's theme if the content supports it.
     *
     * <p>
     * Algorithmic darkening is disallowed by default.
     * <p>
     * If the app's theme is dark and it allows algorithmic darkening, WebView will attempt to
     * darken web content using an algorithm, if the content doesn't define its own dark styles
     * and doesn't explicitly disable darkening.
     *
     * <p>
     * If Android is applying Force Dark to WebView then WebView will ignore the value of
     * this setting and behave as if it were set to true.
     *
     * <p>
     * The deprecated {@link #setForceDark} and related API are no-ops in apps with
     * {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
     * but they still apply to apps with
     * {@code targetSdkVersion} &lt; {@link android.os.Build.VERSION_CODES#TIRAMISU}.
     *
     * <p>
     * The below table summarizes how APIs work with different apps.
     *
     * <table border="2" width="85%" align="center" cellpadding="5">
     *     <thead>
     *         <tr>
     *             <th>App</th>
     *             <th>Web content which uses prefers-color-scheme</th>
     *             <th>Web content which does not use prefers-color-scheme</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *     <tr>
     *         <td>App with {@code isLightTheme} True or not set</td>
     *         <td>Renders with the light theme defined by the content author.</td>
     *         <td>Renders with the default styling defined by the content author.</td>
     *     </tr>
     *     <tr>
     *         <td>App with Android forceDark in effect</td>
     *         <td>Renders with the dark theme defined by the content author.</td>
     *         <td>Renders with the styling modified to dark colors by an algorithm
     *             if allowed by the content author.</td>
     *     </tr>
     *     <tr>
     *         <td>App with {@code isLightTheme} False,
     *            {@code targetSdkVersion} &lt; {@link android.os.Build.VERSION_CODES#TIRAMISU},
     *             and has {@code FORCE_DARK_AUTO}</td>
     *         <td>Renders with the dark theme defined by the content author.</td>
     *         <td>Renders with the default styling defined by the content author.</td>
     *     </tr>
     *     <tr>
     *         <td>App with {@code isLightTheme} False,
     *            {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
     *             and {@code setAlgorithmicDarkening(false)}</td>
     *         <td>Renders with the dark theme defined by the content author.</td>
     *         <td>Renders with the default styling defined by the content author.</td>
     *     </tr>
     *     <tr>
     *         <td>App with {@code isLightTheme} False,
     *            {@code targetSdkVersion} &ge; {@link android.os.Build.VERSION_CODES#TIRAMISU},
     *             and {@code setAlgorithmicDarkening(true)}</td>
     *         <td>Renders with the dark theme defined by the content author.</td>
     *         <td>Renders with the styling modified to dark colors by an algorithm if allowed
     *             by the content author.</td>
     *     </tr>
     *     </tbody>
     * </table>
     * </p>
     *
     * <p>
     * To check if {@code  WebViewFeature.ALGORITHMIC_DARKENING} is supported,
     * {@link androidx.webkit.WebViewFeature#isFeatureSupported} should be called after WebView
     * is created.
     *
     * <p>
     * @param allow allow algorithmic darkening or not.
     *
     */
    @RequiresFeature(name = WebViewFeature.ALGORITHMIC_DARKENING,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setAlgorithmicDarkeningAllowed(@NonNull WebSettings settings,
            boolean allow) {
        ApiFeature.T feature = WebViewFeatureInternal.ALGORITHMIC_DARKENING;
        if (feature.isSupportedByWebView()) {
            getAdapter(settings).setAlgorithmicDarkeningAllowed(allow);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get if algorithmic darkening is allowed or not for this WebView.
     * The default is false.
     *
     * @return if the algorithmic darkening is allowed or not.
     * @see #setAlgorithmicDarkeningAllowed
     */
    @RequiresFeature(name = WebViewFeature.ALGORITHMIC_DARKENING,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean isAlgorithmicDarkeningAllowed(@NonNull WebSettings settings) {
        ApiFeature.T feature = WebViewFeatureInternal.ALGORITHMIC_DARKENING;
        if (feature.isSupportedByWebView()) {
            return getAdapter(settings).isAlgorithmicDarkeningAllowed();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * In this mode WebView content will be darkened by a user agent and it will ignore the
     * web page's dark theme if it exists. To avoid mixing two different darkening strategies,
     * the {@code prefers-color-scheme} media query will evaluate to light.
     *
     * <p> See <a href="https://drafts.csswg.org/css-color-adjust-1/">specification</a>
     * for more information.
     *
     * @see #setForceDarkStrategy
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int DARK_STRATEGY_USER_AGENT_DARKENING_ONLY =
            WebSettingsBoundaryInterface.ForceDarkBehavior.FORCE_DARK_ONLY;

    /**
     * In this mode WebView content will always be darkened using dark theme provided by web page.
     * If web page does not provide dark theme support WebView content will be rendered with a
     * default theme.
     *
     * <p> See <a href="https://drafts.csswg.org/css-color-adjust-1/">specification</a>
     * for more information.
     *
     * @see #setForceDarkStrategy
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int DARK_STRATEGY_WEB_THEME_DARKENING_ONLY =
            WebSettingsBoundaryInterface.ForceDarkBehavior.MEDIA_QUERY_ONLY;

    /**
     * In this mode WebView content will be darkened by a user agent unless web page supports dark
     * theme. WebView determines whether web pages supports dark theme by the presence of
     * {@code color-scheme} metadata containing "dark" value. For example,
     * {@code <meta name="color-scheme" content="dark light">}.
     * If the metadata is not presented WebView content will be darkened by a user agent and
     * {@code prefers-color-scheme} media query will evaluate to light.
     *
     * <p> See <a href="https://drafts.csswg.org/css-color-adjust-1/">specification</a>
     * for more information.
     *
     * @see #setForceDarkStrategy
     * @deprecated refer to {@link #setForceDark}
     */
    @Deprecated
    public static final int DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING =
            WebSettingsBoundaryInterface.ForceDarkBehavior.PREFER_MEDIA_QUERY_OVER_FORCE_DARK;

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {
            DARK_STRATEGY_USER_AGENT_DARKENING_ONLY,
            DARK_STRATEGY_WEB_THEME_DARKENING_ONLY,
            DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING,
    })
    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    public @interface ForceDarkStrategy {}

    /**
     * Set how WebView content should be darkened.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#FORCE_DARK_STRATEGY}.
     *
     * <p>
     * The specified strategy is only used if force dark mode is on.
     * See {@link #setForceDark}.
     *
     * @param forceDarkBehavior the force dark strategy to set.
     * @see #getForceDarkStrategy
     * @deprecated refer to {@link #setForceDark}
     */
    @RequiresFeature(name = WebViewFeature.FORCE_DARK_STRATEGY,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @Deprecated
    public static void setForceDarkStrategy(@NonNull WebSettings settings,
            @ForceDarkStrategy int forceDarkBehavior) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.FORCE_DARK_STRATEGY;
        if (feature.isSupportedByWebView()) {
            getAdapter(settings).setForceDarkStrategy(forceDarkBehavior);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get how content is darkened for this WebView.
     *
     * <p>
     * The default force dark strategy is
     * {@link #DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING}
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#FORCE_DARK_STRATEGY}.
     *
     * @return the currently set force dark strategy.
     * @see #setForceDarkStrategy
     * @deprecated refer to {@link #setForceDark}
     */
    @RequiresFeature(name = WebViewFeature.FORCE_DARK_STRATEGY,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @Deprecated
    public static @ForceDarkStrategy int getForceDarkStrategy(@NonNull WebSettings settings) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.FORCE_DARK_STRATEGY;
        if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getForceDark();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets whether EnterpriseAuthenticationAppLinkPolicy if set by admin is allowed to have any
     * effect on WebView.
     * <p>
     * EnterpriseAuthenticationAppLinkPolicy in WebView allows admins to specify authentication
     * urls. When WebView is redirected to authentication url, and an app on the device has
     * registered as the default handler for the url, that app is launched.
     * <p>
     * EnterpriseAuthenticationAppLinkPolicy is enabled by default.
     *
     * <p> See <a href="https://source.chromium.org/chromium/chromium/src/+/main:components/policy/resources/policy_templates.json;l=32321?q=EnterpriseAuthenticationAppLinkPolicy%20file:policy_templates.json">
     * this</a> for more information on EnterpriseAuthenticationAppLinkPolicy.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY}.
     *
     * @param enabled Whether EnterpriseAuthenticationAppLinkPolicy should be enabled.
     */
    @RequiresFeature(name = WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setEnterpriseAuthenticationAppLinkPolicyEnabled(
            @NonNull WebSettings settings,
            boolean enabled) {
        ApiFeature.NoFramework feature =
                WebViewFeatureInternal.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY;
        if (feature.isSupportedByWebView()) {
            getAdapter(settings).setEnterpriseAuthenticationAppLinkPolicyEnabled(enabled);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Gets whether EnterpriseAuthenticationAppLinkPolicy is allowed to have any effect on WebView.
     *
     * <p> See <a href="https://source.chromium.org/chromium/chromium/src/+/main:components/policy/resources/policy_templates.json;l=32321?q=EnterpriseAuthenticationAppLinkPolicy%20file:policy_templates.json">
     * this</a> for more information on EnterpriseAuthenticationAppLinkPolicy.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY}.
     *
     * @return {@code true} if EnterpriseAuthenticationAppLinkPolicy is enabled and {@code false}
     * otherwise.
     */
    @RequiresFeature(name = WebViewFeature.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean getEnterpriseAuthenticationAppLinkPolicyEnabled(
            @NonNull WebSettings settings) {
        ApiFeature.NoFramework feature =
                WebViewFeatureInternal.ENTERPRISE_AUTHENTICATION_APP_LINK_POLICY;
        if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getEnterpriseAuthenticationAppLinkPolicyEnabled();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get the currently configured allow-list of origins, which is guaranteed to receive the
     * {@code X-Requested-With} HTTP header on requests from the {@link WebView} owning the passed
     * {@link WebSettings}.
     * <p>
     * Any origin <em>not</em> on this allow-list may not receive the header, depending on the
     * current installed WebView provider.
     * <p>
     * The format of the strings in the allow-list follows the origin rules of
     * {@link WebViewCompat#addWebMessageListener(WebView, String, Set, WebViewCompat.WebMessageListener)}.
     *
     * @param settings Settings retrieved from {@link WebView#getSettings()}.
     * @return The configured set of allow-listed origins.
     * @see #setRequestedWithHeaderOriginAllowList(WebSettings, Set)
     */
    @RequiresFeature(name = WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    public static Set<String> getRequestedWithHeaderOriginAllowList(@NonNull WebSettings settings) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.REQUESTED_WITH_HEADER_ALLOW_LIST;
        if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getRequestedWithHeaderOriginAllowList();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Set an allow-list of origins to receive the {@code X-Requested-With} HTTP header from the
     * WebView owning the passed {@link WebSettings}.
     * <p>
     * Historically, this header was sent on all requests from WebView, containing the
     * app package name of the embedding app. Depending on the version of installed WebView, this
     * may no longer be the case, as the header was deprecated in late 2022, and its use
     * discontinued.
     * <p>
     * Apps can use this method to restore the legacy behavior for servers that still rely on
     * the deprecated header, but it should not be used to identify the webview to first-party
     * servers under the control of the app developer.
     * <p>
     * The format of the strings in the allow-list follows the origin rules of
     * {@link WebViewCompat#addWebMessageListener(WebView, String, Set, WebViewCompat.WebMessageListener)}.
     *
     * @param settings Settings retrieved from {@link WebView#getSettings()}.
     * @param allowList Set of origins to allow-list.
     * @throws IllegalArgumentException if the allow-list contains a malformed origin.
     */
    @RequiresFeature(name = WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setRequestedWithHeaderOriginAllowList(@NonNull WebSettings settings,
            @NonNull Set<String> allowList) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.REQUESTED_WITH_HEADER_ALLOW_LIST;
        if (feature.isSupportedByWebView()) {
            getAdapter(settings).setRequestedWithHeaderOriginAllowList(allowList);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets the WebView's user-agent metadata to generate user-agent client hints.
     * <p>
     * UserAgentMetadata in WebView is used to populate user-agent client hints, they can provide
     * the client’s branding and version information, the underlying operating system’s branding
     * and major version, as well as details about the underlying device.
     * <p>
     * The user-agent string can be set with {@link WebSettings#setUserAgentString(String)}, here
     * are the details on how this API interacts it to generate user-agent client hints.
     * <p>
     * If the UserAgentMetadata is null and the overridden user-agent contains the system default
     * user-agent, the system default value will be used.
     * <p>
     * If the UserAgentMetadata is null but the overridden user-agent doesn't contain the system
     * default user-agent, only the
     * <a href="https://wicg.github.io/client-hints-infrastructure/#low-entropy-hint-table">low-entry user-agent client hints</a> will be generated.
     *
     * <p> See <a href="https://wicg.github.io/ua-client-hints/">
     * this</a> for more information about User-Agent Client Hints.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#USER_AGENT_METADATA}.
     *
     * @param metadata the WebView's user-agent metadata.
     *
     * TODO(b/294183509): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.USER_AGENT_METADATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setUserAgentMetadata(@NonNull WebSettings settings,
            @NonNull UserAgentMetadata metadata) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.USER_AGENT_METADATA;
        if (feature.isSupportedByWebView()) {
            getAdapter(settings).setUserAgentMetadata(metadata);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get the WebView's user-agent metadata which used to generate user-agent client hints.
     *
     * <p> See <a href="https://wicg.github.io/ua-client-hints/"> this</a> for more information
     * about User-Agent Client Hints.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#USER_AGENT_METADATA}.
     *
     * TODO(b/294183509): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresFeature(name = WebViewFeature.USER_AGENT_METADATA,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @NonNull
    public static UserAgentMetadata getUserAgentMetadata(@NonNull WebSettings settings) {
        final ApiFeature.NoFramework feature =
                WebViewFeatureInternal.USER_AGENT_METADATA;
        if (feature.isSupportedByWebView()) {
            return getAdapter(settings).getUserAgentMetadata();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private static WebSettingsAdapter getAdapter(WebSettings settings) {
        return WebViewGlueCommunicator.getCompatConverter().convertSettings(settings);
    }
}

