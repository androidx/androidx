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
    public static void setOffscreenPreRaster(WebSettings webSettings, boolean enabled) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        if (webviewFeature.isSupportedByFramework()) {
            webSettings.setOffscreenPreRaster(enabled);
        } else if (webviewFeature.isSupportedByWebView()) {
            getAdapter(webSettings).setOffscreenPreRaster(enabled);
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
    public static boolean getOffscreenPreRaster(WebSettings webSettings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.OFF_SCREEN_PRERASTER);
        if (webviewFeature.isSupportedByFramework()) {
            return webSettings.getOffscreenPreRaster();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(webSettings).getOffscreenPreRaster();
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
    public static void setSafeBrowsingEnabled(WebSettings webSettings, boolean enabled) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        if (webviewFeature.isSupportedByFramework()) {
            webSettings.setSafeBrowsingEnabled(enabled);
        } else if (webviewFeature.isSupportedByWebView()) {
            getAdapter(webSettings).setSafeBrowsingEnabled(enabled);
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
    public static boolean getSafeBrowsingEnabled(WebSettings webSettings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SAFE_BROWSING_ENABLE);
        if (webviewFeature.isSupportedByFramework()) {
            return webSettings.getSafeBrowsingEnabled();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(webSettings).getSafeBrowsingEnabled();
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
    public static void setDisabledActionModeMenuItems(WebSettings webSettings,
            @MenuItemFlags int menuItems) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS);
        if (webviewFeature.isSupportedByFramework()) {
            webSettings.setDisabledActionModeMenuItems(menuItems);
        } else if (webviewFeature.isSupportedByWebView()) {
            getAdapter(webSettings).setDisabledActionModeMenuItems(menuItems);
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
    public static @MenuItemFlags int getDisabledActionModeMenuItems(WebSettings webSettings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.DISABLED_ACTION_MODE_MENU_ITEMS);
        if (webviewFeature.isSupportedByFramework()) {
            return webSettings.getDisabledActionModeMenuItems();
        } else if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(webSettings).getDisabledActionModeMenuItems();
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
    public static void setWillSuppressErrorPage(WebSettings webSettings, boolean suppressed) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SUPPRESS_ERROR_PAGE);
        if (webviewFeature.isSupportedByWebView()) {
            getAdapter(webSettings).setWillSuppressErrorPage(suppressed);
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
    public static boolean willSuppressErrorPage(WebSettings webSettings) {
        WebViewFeatureInternal webviewFeature =
                WebViewFeatureInternal.getFeature(WebViewFeature.SUPPRESS_ERROR_PAGE);
        if (webviewFeature.isSupportedByWebView()) {
            return getAdapter(webSettings).willSuppressErrorPage();
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }



    private static WebSettingsAdapter getAdapter(WebSettings webSettings) {
        return WebViewGlueCommunicator.getCompatConverter().convertSettings(webSettings);
    }
}

