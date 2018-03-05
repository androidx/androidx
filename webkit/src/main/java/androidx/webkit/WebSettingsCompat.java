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
import android.webkit.WebSettings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;
import androidx.webkit.internal.WebSettingsAdapter;
import androidx.webkit.internal.WebViewGlueCommunicator;

/**
 * Compatibility version of {@link android.webkit.WebSettings}
 */
public class WebSettingsCompat {
    private WebSettingsCompat() {}

    // TODO(gsennton): add feature detection

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
     */
    public static void setOffscreenPreRaster(WebSettings webSettings, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            webSettings.setOffscreenPreRaster(enabled);
        } else {
            getAdapter(webSettings).setOffscreenPreRaster(enabled);
        }
    }

    /**
     * Gets whether this WebView should raster tiles when it is
     * offscreen but attached to a window.
     * @return {@code true} if this WebView will raster tiles when it is
     * offscreen but attached to a window.
     */
    public static boolean getOffscreenPreRaster(WebSettings webSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return webSettings.getOffscreenPreRaster();
        } else {
            return getAdapter(webSettings).getOffscreenPreRaster();
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
     * @param enabled Whether Safe Browsing is enabled.
     */
    public static void setSafeBrowsingEnabled(WebSettings webSettings, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webSettings.setSafeBrowsingEnabled(enabled);
        } else {
            getAdapter(webSettings).setSafeBrowsingEnabled(enabled);
        }
    }

    /**
     * Gets whether Safe Browsing is enabled.
     * See {@link #setSafeBrowsingEnabled}.
     *
     * @return {@code true} if Safe Browsing is enabled and {@code false} otherwise.
     */
    public static boolean getSafeBrowsingEnabled(WebSettings webSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return webSettings.getSafeBrowsingEnabled();
        } else {
            return getAdapter(webSettings).getSafeBrowsingEnabled();
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
     * @param menuItems an integer field flag for the menu items to be disabled.
     */
    public static void setDisabledActionModeMenuItems(WebSettings webSettings,
            @MenuItemFlags int menuItems) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            webSettings.setDisabledActionModeMenuItems(menuItems);
        } else {
            getAdapter(webSettings).setDisabledActionModeMenuItems(menuItems);
        }
    }

    /**
     * Gets the action mode menu items that are disabled, expressed in an integer field flag.
     * The default value is {@link WebSettings#MENU_ITEM_NONE}
     *
     * @return all the disabled menu item flags combined with bitwise OR.
     */
    public static @MenuItemFlags int getDisabledActionModeMenuItems(WebSettings webSettings) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return webSettings.getDisabledActionModeMenuItems();
        } else {
            return getAdapter(webSettings).getDisabledActionModeMenuItems();
        }
    }

    private static WebSettingsAdapter getAdapter(WebSettings webSettings) {
        return WebViewGlueCommunicator.getCompatConverter().convertSettings(webSettings);
    }
}

