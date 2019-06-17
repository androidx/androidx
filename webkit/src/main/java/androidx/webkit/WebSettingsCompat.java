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
import android.webkit.WebSettings;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresFeature;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.WebSettingsAdapter;
import androidx.webkit.internal.WebViewFeatureInternal;
import androidx.webkit.internal.WebViewGlueCommunicator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.OFF_SCREEN_PRERASTER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setOffscreenPreRaster(@NonNull WebSettings settings, boolean enabled) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        if (webviewFeature.isSupportedByFramework()) {
            settings.setOffscreenPreRaster(enabled);
        } else if (webviewFeature.isSupportedByWebView()) {
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.OFF_SCREEN_PRERASTER,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean getOffscreenPreRaster(@NonNull WebSettings settings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        if (webviewFeature.isSupportedByFramework()) {
            return settings.getOffscreenPreRaster();
        } else if (webviewFeature.isSupportedByWebView()) {
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_ENABLE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setSafeBrowsingEnabled(@NonNull WebSettings settings, boolean enabled) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        if (webviewFeature.isSupportedByFramework()) {
            settings.setSafeBrowsingEnabled(enabled);
        } else if (webviewFeature.isSupportedByWebView()) {
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.SAFE_BROWSING_ENABLE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean getSafeBrowsingEnabled(@NonNull WebSettings settings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        if (webviewFeature.isSupportedByFramework()) {
            return settings.getSafeBrowsingEnabled();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(settings).getSafeBrowsingEnabled();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setDisabledActionModeMenuItems(@NonNull WebSettings settings,
            @MenuItemFlags int menuItems) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS);
        if (webviewFeature.isSupportedByFramework()) {
            settings.setDisabledActionModeMenuItems(menuItems);
        } else if (webviewFeature.isSupportedByWebView()) {
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
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @MenuItemFlags int getDisabledActionModeMenuItems(@NonNull WebSettings settings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS);
        if (webviewFeature.isSupportedByFramework()) {
            return settings.getDisabledActionModeMenuItems();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(settings).getDisabledActionModeMenuItems();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Sets whether the WebView’s internal error page should be suppressed or displayed
     * for bad navigations. True means suppressed (not shown), false means it will be
     * displayed.
     * The default value is false.
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SUPPRESS_ERROR_PAGE}.
     *
     * @param suppressed whether the WebView should suppress its internal error page
     *
     * TODO(cricke): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.SUPPRESS_ERROR_PAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setWillSuppressErrorPage(@NonNull WebSettings settings,
            boolean suppressed) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SUPPRESS_ERROR_PAGE);
        if (webviewFeature.isSupportedByWebView()) {
            getAdapter(settings).setWillSuppressErrorPage(suppressed);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }


    /**
     * Gets whether the WebView’s internal error page will be suppressed or displayed
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#SUPPRESS_ERROR_PAGE}.
     *
     * @return true if the WebView will suppress its internal error page
     * @see #setWillSuppressErrorPage
     *
     * TODO(cricke): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.SUPPRESS_ERROR_PAGE,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static boolean willSuppressErrorPage(@NonNull WebSettings settings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SUPPRESS_ERROR_PAGE);
        if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(settings).willSuppressErrorPage();
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
     * TODO(amalova): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int FORCE_DARK_OFF = 0;

    /**
     * Enable force dark dependent on the state of the WebView parent view. If the WebView parent
     * view is being automatically force darkened
     * (@see android.view.View#setForceDarkAllowed), then WebView content will be rendered
     * so as to emulate a dark theme. WebViews that are not attached to the view hierarchy will not
     * be inverted.
     *
     * @see #setForceDark
     * TODO(amalova): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int FORCE_DARK_AUTO = 1;

    /**
     * Used with {@link #setForceDark}
     *
     * Unconditionally enable force dark. In this mode WebView content will always be rendered so
     * as to emulate a dark theme.
     *
     * @see #setForceDark
     * TODO(amalova): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final int FORCE_DARK_ON = 2;

    /**
     * @hide
     */
    // TODO(amalova): redefine with framework constants when AndroidX compiles against Q
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
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
     * @param forceDarkMode the force dark mode to set.
     *
     * TODO(amalova): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.FORCE_DARK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static void setForceDark(@NonNull WebSettings settings,
            @ForceDark int forceDarkMode) {
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.FORCE_DARK);
        if (webViewFeature.isSupportedByFramework()) {
            settings.setForceDark(forceDarkMode);
        } else if (webViewFeature.isSupportedByWebView()) {
            getAdapter(settings).setForceDark(forceDarkMode);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    /**
     * Get the force dark mode for this WebView.
     *
     * <p>
     * The default force dark mode is {@link #FORCE_DARK_AUTO}
     *
     * <p>
     * This method should only be called if
     * {@link WebViewFeature#isFeatureSupported(String)}
     * returns true for {@link WebViewFeature#FORCE_DARK}.
     *
     * @return the currently set force dark mode.
     *
     * TODO(amalova): unhide
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressLint("NewApi")
    @RequiresFeature(name = WebViewFeature.FORCE_DARK,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    public static @ForceDark int getForceDark(@NonNull WebSettings settings) {
        WebViewFeatureInternal webViewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.FORCE_DARK);
        if (webViewFeature.isSupportedByFramework()) {
            return settings.getForceDark();
        } else if (webViewFeature.isSupportedByWebView()) {
            return getAdapter(settings).getForceDark();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }

    private static WebSettingsAdapter getAdapter(WebSettings settings) {
        return WebViewGlueCommunicator.getCompatConverter().convertSettings(settings);
    }
}

