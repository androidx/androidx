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

package androidx.browser.customtabs;

import static androidx.annotation.Dimension.DP;
import static androidx.annotation.Dimension.PX;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Network;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.LocaleList;
import android.provider.Browser;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.AnimRes;
import androidx.annotation.ColorInt;
import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Class holding the {@link Intent} and start bundle for a Custom Tabs Activity.
 *
 * <p>
 * <strong>Note:</strong> The constants below are public for the browser implementation's benefit.
 * You are strongly encouraged to use {@link CustomTabsIntent.Builder}.</p>
 */
public final class CustomTabsIntent {

    /**
     * Indicates that the user explicitly opted out of Custom Tabs in the calling application.
     * <p>
     * If an application provides a mechanism for users to opt out of Custom Tabs, this extra should
     * be provided with {@link Intent#FLAG_ACTIVITY_NEW_TASK} to ensure the browser does not attempt
     * to trigger any Custom Tab-like experiences as a result of the VIEW intent.
     * <p>
     * If this extra is present with {@link Intent#FLAG_ACTIVITY_NEW_TASK}, all Custom Tabs
     * customizations will be ignored.
     */
    private static final String EXTRA_USER_OPT_OUT_FROM_CUSTOM_TABS =
            "android.support.customtabs.extra.user_opt_out";

    /**
     * Extra used to match the session. This has to be included in the intent to open in
     * a custom tab. This is the same IBinder that gets passed to ICustomTabsService#newSession.
     * Null if there is no need to match any service side sessions with the intent.
     */
    public static final String EXTRA_SESSION = "android.support.customtabs.extra.SESSION";

    /**
     * Extra used to match the session ID. This is PendingIntent which is created with
     * {@link CustomTabsClient#createSessionId}.
     *
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static final String EXTRA_SESSION_ID = "android.support.customtabs.extra.SESSION_ID";

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({COLOR_SCHEME_SYSTEM, COLOR_SCHEME_LIGHT, COLOR_SCHEME_DARK})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ColorScheme {
    }

    /**
     * Applies either a light or dark color scheme to the user interface in the custom tab depending
     * on the user's system settings.
     */
    public static final int COLOR_SCHEME_SYSTEM = 0;

    /**
     * Applies a light color scheme to the user interface in the custom tab.
     */
    public static final int COLOR_SCHEME_LIGHT = 1;

    /**
     * Applies a dark color scheme to the user interface in the custom tab. Colors set through
     * {@link #EXTRA_TOOLBAR_COLOR} may be darkened to match user expectations.
     */
    public static final int COLOR_SCHEME_DARK = 2;

    /**
     * Maximum value for the COLOR_SCHEME_* configuration options. For validation purposes only.
     */
    private static final int COLOR_SCHEME_MAX = 2;

    /**
     * Extra (int) that specifies which color scheme should be applied to the custom tab. Default is
     * {@link #COLOR_SCHEME_SYSTEM}.
     */
    public static final String EXTRA_COLOR_SCHEME =
            "androidx.browser.customtabs.extra.COLOR_SCHEME";

    /**
     * Extra that changes the background color for the toolbar. colorRes is an int that specifies a
     * {@link Color}, not a resource id.
     */
    public static final String EXTRA_TOOLBAR_COLOR =
            "android.support.customtabs.extra.TOOLBAR_COLOR";

    /**
     * Boolean extra that enables the url bar to hide as the user scrolls down the page
     */
    public static final String EXTRA_ENABLE_URLBAR_HIDING =
            "android.support.customtabs.extra.ENABLE_URLBAR_HIDING";

    /**
     * Extra bitmap that specifies the icon of the back button on the toolbar. If the client chooses
     * not to customize it, a default close button will be used.
     */
    public static final String EXTRA_CLOSE_BUTTON_ICON =
            "android.support.customtabs.extra.CLOSE_BUTTON_ICON";

    /**
     * Extra (int) that specifies state for showing the page title. Default is {@link #NO_TITLE}.
     */
    public static final String EXTRA_TITLE_VISIBILITY_STATE =
            "android.support.customtabs.extra.TITLE_VISIBILITY";

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({OPEN_IN_BROWSER_STATE_DEFAULT, OPEN_IN_BROWSER_STATE_ON, OPEN_IN_BROWSER_STATE_OFF})
    @Retention(RetentionPolicy.SOURCE)
    @ExperimentalOpenInBrowser
    public @interface OpenInBrowserState {
    }

    /**
     * Applies the default Open in Browser button state in the toolbar depending on the browser.
     */
    @ExperimentalOpenInBrowser
    public static final int OPEN_IN_BROWSER_STATE_DEFAULT = 0;

    /**
     * Shows the Open in Browser button in the toolbar.
     */
    @ExperimentalOpenInBrowser
    public static final int OPEN_IN_BROWSER_STATE_ON = 1;

    /**
     * Explicitly does not show the Open in Browser button in the toolbar.
     */
    @ExperimentalOpenInBrowser
    public static final int OPEN_IN_BROWSER_STATE_OFF = 2;

    /**
     * Maximum value for the OPEN_IN_BROWSER_STATE_* configuration options. For validation purposes
     * only.
     */
    @ExperimentalOpenInBrowser
    private static final int OPEN_IN_BROWSER_STATE_MAX = 2;

    /**
     * Extra to set the state for the Open in Browser button in the toolbar.
     */
    @ExperimentalOpenInBrowser
    public static final String EXTRA_OPEN_IN_BROWSER_STATE =
            "androidx.browser.customtabs.extra.OPEN_IN_BROWSER_STATE";

    /**
     * Extra to disable the bookmarks button in the overflow menu.
     */
    public static final String EXTRA_DISABLE_BOOKMARKS_BUTTON =
            "org.chromium.chrome.browser.customtabs.EXTRA_DISABLE_STAR_BUTTON";

    /**
     * Extra to disable the download button in the overflow menu.
     */
    public static final String EXTRA_DISABLE_DOWNLOAD_BUTTON =
            "org.chromium.chrome.browser.customtabs.EXTRA_DISABLE_DOWNLOAD_BUTTON";

    /**
     * Extra to favor sending redirects off of the initial URL to external handler apps, if
     * possible.
     *
     * A Custom Tab Intent from a Custom Tab session will always have the package set,
     * so the Intent will always be to the browser. This extra can be used to allow
     * the initial Intent navigation chain to leave the browser.
     */
    public static final String EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER =
            "android.support.customtabs.extra.SEND_TO_EXTERNAL_HANDLER";

    /**
     * Extra to favor the Custom Tab forwarding the initial URL sent to the Custom Tab to the
     * default handling app for that URL instead of loading it (including following any
     * redirects) in the Custom Tab.
     */
    @ExperimentalInitialNavigationCanLeaveBrowser
    public static final String EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER =
            "androidx.browser.customtabs.extra.INITIAL_NAVIGATION_CAN_LEAVE_BROWSER";

    /**
     * Extra that specifies the target locale the Translate UI should be triggered with.
     * The locale is represented as a well-formed IETF BCP 47 language tag.
     */
    public static final String EXTRA_TRANSLATE_LANGUAGE_TAG =
            "androidx.browser.customtabs.extra.TRANSLATE_LANGUAGE_TAG";

    /**
     * Extra tha disables interactions with the background app when a Partial Custom Tab
     * is launched.
     */
    public static final String EXTRA_DISABLE_BACKGROUND_INTERACTION =
            "androidx.browser.customtabs.extra.DISABLE_BACKGROUND_INTERACTION";

    /**
     * Extra that specifies the {@link PendingIntent} to be sent when the user swipes up from
     * the secondary (bottom) toolbar.
     */
    public static final String EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE =
            "androidx.browser.customtabs.extra.SECONDARY_TOOLBAR_SWIPE_UP_GESTURE";

    /**
     * Don't show any title. Shows only the domain.
     */
    public static final int NO_TITLE = 0;

    /**
     * Shows the page title and the domain.
     */
    public static final int SHOW_PAGE_TITLE = 1;

    /**
     * Bundle used for adding a custom action button to the custom tab toolbar. The client should
     * provide a description, an icon {@link Bitmap} and a {@link PendingIntent} for the button.
     * All three keys must be present.
     */
    public static final String EXTRA_ACTION_BUTTON_BUNDLE =
            "android.support.customtabs.extra.ACTION_BUTTON_BUNDLE";

    /**
     * List<Bundle> used for adding items to the top and bottom toolbars. The client should
     * provide an ID, a description, an icon {@link Bitmap} for each item. They may also provide a
     * {@link PendingIntent} if the item is a button.
     */
    public static final String EXTRA_TOOLBAR_ITEMS =
            "android.support.customtabs.extra.TOOLBAR_ITEMS";

    /**
     * Extra that changes the background color for the secondary toolbar. The value should be an
     * int that specifies a {@link Color}, not a resource id.
     */
    public static final String EXTRA_SECONDARY_TOOLBAR_COLOR =
            "android.support.customtabs.extra.SECONDARY_TOOLBAR_COLOR";

    /**
     * Key that specifies the {@link Bitmap} to be used as the image source for the action button.
     *  The icon should't be more than 24dp in height (No padding needed. The button itself will be
     *  48dp in height) and have a width/height ratio of less than 2.
     */
    public static final String KEY_ICON = "android.support.customtabs.customaction.ICON";

    /**
     * Key that specifies the content description for the custom action button.
     */
    public static final String KEY_DESCRIPTION =
            "android.support.customtabs.customaction.DESCRIPTION";

    /**
     * Key that specifies the PendingIntent to launch when the action button or menu item was
     * clicked. The custom tab will be calling {@link PendingIntent#send()} on clicks after adding
     * the url as data. The client app can call {@link Intent#getDataString()} to get the url.
     */
    public static final String KEY_PENDING_INTENT =
            "android.support.customtabs.customaction.PENDING_INTENT";

    /**
     * Extra boolean that specifies whether the custom action button should be tinted. Default is
     * false and the action button will not be tinted.
     */
    public static final String EXTRA_TINT_ACTION_BUTTON =
            "android.support.customtabs.extra.TINT_ACTION_BUTTON";

    /**
     * Use an {@code ArrayList<Bundle>} for specifying menu related params. There should be a
     * separate {@link Bundle} for each custom menu item.
     */
    public static final String EXTRA_MENU_ITEMS = "android.support.customtabs.extra.MENU_ITEMS";

    /**
     * Key for specifying the title of a menu item.
     */
    public static final String KEY_MENU_ITEM_TITLE =
            "android.support.customtabs.customaction.MENU_ITEM_TITLE";

    /**
     * Bundle constructed out of {@link ActivityOptionsCompat} that will be running when the
     * {@link Activity} that holds the custom tab gets finished. A similar ActivityOptions
     * for creation should be constructed and given to the startActivity() call that
     * launches the custom tab.
     */
    public static final String EXTRA_EXIT_ANIMATION_BUNDLE =
            "android.support.customtabs.extra.EXIT_ANIMATION_BUNDLE";

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({SHARE_STATE_DEFAULT, SHARE_STATE_ON, SHARE_STATE_OFF})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ShareState {
    }

    /**
     * Applies the default share settings depending on the browser.
     */
    public static final int SHARE_STATE_DEFAULT = 0;

    /**
     * Shows a share option in the tab.
     */
    public static final int SHARE_STATE_ON = 1;

    /**
     * Explicitly does not show a share option in the tab.
     */
    public static final int SHARE_STATE_OFF = 2;

    /**
     * Maximum value for the SHARE_STATE_* configuration options. For validation purposes only.
     */
    private static final int SHARE_STATE_MAX = 2;

    /**
     * Extra (int) that specifies which share state should be applied to the custom tab. Default is
     * {@link CustomTabsIntent#SHARE_STATE_DEFAULT}.
     */
    public static final String EXTRA_SHARE_STATE = "androidx.browser.customtabs.extra.SHARE_STATE";

    /**
     * Boolean extra that specifies whether a default share button will be shown in the menu.
     *
     * @deprecated Use {@link CustomTabsIntent#EXTRA_SHARE_STATE} instead.
     */
    @Deprecated
    public static final String EXTRA_DEFAULT_SHARE_MENU_ITEM =
            "android.support.customtabs.extra.SHARE_MENU_ITEM";

    /**
     * Extra that specifies the {@link RemoteViews} showing on the secondary toolbar. If this extra
     * is set, the other secondary toolbar configurations will be overriden. The height of the
     * {@link RemoteViews} should not exceed 56dp.
     * @see CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent).
     */
    public static final String EXTRA_REMOTEVIEWS =
            "android.support.customtabs.extra.EXTRA_REMOTEVIEWS";

    /**
     * Extra that specifies an array of {@link View} ids. When these {@link View}s are clicked, a
     * {@link PendingIntent} will be sent, carrying the current url of the custom tab as data.
     * <p>
     * Note that Custom Tabs will override the default onClick behavior of the listed {@link View}s.
     * If you do not care about the current url, you can safely ignore this extra and use
     * {@link RemoteViews#setOnClickPendingIntent(int, PendingIntent)} instead.
     * @see CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent).
     */
    public static final String EXTRA_REMOTEVIEWS_VIEW_IDS =
            "android.support.customtabs.extra.EXTRA_REMOTEVIEWS_VIEW_IDS";

    /**
     * Extra that specifies the {@link PendingIntent} to be sent when the user clicks on the
     * {@link View}s that is listed by {@link #EXTRA_REMOTEVIEWS_VIEW_IDS}.
     * <p>
     * Note when this {@link PendingIntent} is triggered, it will have the current url as data
     * field, also the id of the clicked {@link View}, specified by
     * {@link #EXTRA_REMOTEVIEWS_CLICKED_ID}.
     * @see CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent).
     */
    public static final String EXTRA_REMOTEVIEWS_PENDINGINTENT =
            "android.support.customtabs.extra.EXTRA_REMOTEVIEWS_PENDINGINTENT";

    /**
     * Extra that specifies which {@link View} has been clicked. This extra will be put to the
     * {@link PendingIntent} sent from Custom Tabs when a view in the {@link RemoteViews} is clicked
     * @see CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent).
     */
    public static final String EXTRA_REMOTEVIEWS_CLICKED_ID =
            "android.support.customtabs.extra.EXTRA_REMOTEVIEWS_CLICKED_ID";

    /**
     * Extra that specifies whether Instant Apps is enabled.
     */
    public static final String EXTRA_ENABLE_INSTANT_APPS =
            "android.support.customtabs.extra.EXTRA_ENABLE_INSTANT_APPS";

    /**
     * Extra that contains a SparseArray, mapping color schemes (except
     * {@link CustomTabsIntent#COLOR_SCHEME_SYSTEM}) to {@link Bundle} representing
     * {@link CustomTabColorSchemeParams}.
     */
    public static final String EXTRA_COLOR_SCHEME_PARAMS =
            "androidx.browser.customtabs.extra.COLOR_SCHEME_PARAMS";

    /**
     * Extra that contains the color of the navigation bar.
     * See {@link Builder#setNavigationBarColor}.
     */
    public static final String EXTRA_NAVIGATION_BAR_COLOR =
            "androidx.browser.customtabs.extra.NAVIGATION_BAR_COLOR";

    /**
     * Extra that, if set, makes the Custom Tab Activity's height to be x pixels, the Custom Tab
     * will behave as a bottom sheet. x will be clamped between 50% and 100% of screen height.
     * Bottom sheet does not take effect in landscape mode or in multi-window mode.
     */
    public static final String EXTRA_INITIAL_ACTIVITY_HEIGHT_PX =
            "androidx.browser.customtabs.extra.INITIAL_ACTIVITY_HEIGHT_PX";

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ACTIVITY_HEIGHT_DEFAULT, ACTIVITY_HEIGHT_ADJUSTABLE, ACTIVITY_HEIGHT_FIXED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivityHeightResizeBehavior {
    }

    /**
     * Applies the default height resize behavior for the Custom Tab Activity when it behaves as a
     * bottom sheet.
     */
    public static final int ACTIVITY_HEIGHT_DEFAULT = 0;

    /**
     * The Custom Tab Activity, when it behaves as a bottom sheet, can have its height manually
     * resized by the user.
     */
    public static final int ACTIVITY_HEIGHT_ADJUSTABLE = 1;

    /**
     * The Custom Tab Activity, when it behaves as a bottom sheet, cannot have its height manually
     * resized by the user.
     */
    public static final int ACTIVITY_HEIGHT_FIXED = 2;

    /**
     * Maximum value for the ACTIVITY_HEIGHT_* configuration options. For validation purposes only.
     */
    private static final int ACTIVITY_HEIGHT_MAX = 2;

    /**
     * Extra that, if set in combination with
     * {@link CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX}, defines the height resize
     * behavior of the Custom Tab Activity when it behaves as a bottom sheet.
     * Default is {@link CustomTabsIntent#ACTIVITY_HEIGHT_DEFAULT}.
     */
    public static final String EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR =
            "androidx.browser.customtabs.extra.ACTIVITY_HEIGHT_RESIZE_BEHAVIOR";

    /**
     * Extra that, if set, makes the Custom Tab Activity's width to be x pixels, the Custom Tab
     * will behave as a side sheet. A minimum width will be enforced, thus the width will be
     * clamped as such (based on the window size classes as defined by the Android documentation):
     * <ul>
     *     <li>Compact, window width <600dp - a side sheet will not be displayed.</li>
     *     <li>Medium, window width >=600dp and <840 dp - between 50% and 100% of the window's
     *     width.</li>
     *     <li>Expanded, window width >=840dp - between 33% and 100% of the window's width.</li>
     * </ul>
     *
     * <a
     * href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes">Android
     * Size Classes</a>
     */
    public static final String EXTRA_INITIAL_ACTIVITY_WIDTH_PX =
            "androidx.browser.customtabs.extra.INITIAL_ACTIVITY_WIDTH_PX";

    /** Extra that enables the maximization button on the side sheet Custom Tab toolbar. */
    public static final String EXTRA_ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION =
            "androidx.browser.customtabs.extra.ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION";

    /**
     * Extra that, if set, allows you to set a custom breakpoint for the Custom Tab -
     * a value, x, for which if the screen's width is higher than x, the Custom Tab will behave as a
     * side sheet (if {@link CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_WIDTH_PX} is set), otherwise
     * it will behave as a bottom sheet (if
     * {@link CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX} is set).
     *
     * If this Intent Extra is not set the browser implementation should set as default value
     * 840dp. If x is set to <600dp the browser implementation should default it to 600dp.
     */
    public static final String EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP =
            "androidx.browser.customtabs.extra.ACTIVITY_SIDE_SHEET_BREAKPOINT_DP";

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ACTIVITY_SIDE_SHEET_POSITION_DEFAULT, ACTIVITY_SIDE_SHEET_POSITION_START,
            ACTIVITY_SIDE_SHEET_POSITION_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivitySideSheetPosition {}

    /**
     * Applies the default position for the Custom Tab Activity when it behaves as a
     * side sheet. Same as {@link #ACTIVITY_SIDE_SHEET_POSITION_END}.
     */
    public static final int ACTIVITY_SIDE_SHEET_POSITION_DEFAULT = 0;

    /** Position the side sheet on the start side of the screen. */
    public static final int ACTIVITY_SIDE_SHEET_POSITION_START = 1;

    /** Position the side sheet on the end side of the screen. */
    public static final int ACTIVITY_SIDE_SHEET_POSITION_END = 2;

    /**
     * Maximum value for the ACTIVITY_SIDE_SHEET_POSITION_* configuration options. For validation
     * purposes only.
     */
    private static final int ACTIVITY_SIDE_SHEET_POSITION_MAX = 2;

    /**
     * Extra that specifies the position of the side sheet. By default it is set to
     * {@link #ACTIVITY_SIDE_SHEET_POSITION_END}, which is on the right side in left-to-right
     * layout.
     */
    public static final String EXTRA_ACTIVITY_SIDE_SHEET_POSITION =
            "androidx.browser.customtabs.extra.ACTIVITY_SIDE_SHEET_POSITION";

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT, ACTIVITY_SIDE_SHEET_DECORATION_TYPE_NONE,
            ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW,
            ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivitySideSheetDecorationType {}
    /**
     * Side sheet's default decoration type. Same as
     * {@link CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW}.
     */
    public static final int ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT = 0;
    /**
     * Side sheet with no decorations - the activity is not bordered by any shadow or divider line.
     */
    public static final int ACTIVITY_SIDE_SHEET_DECORATION_TYPE_NONE = 1;
    /**
     * Side sheet with shadow decoration - the activity is bordered by a shadow effect.
     */
    public static final int ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW = 2;
    /**
     * Side sheet with a divider line - the activity is bordered by a thin opaque line.
     */
    public static final int ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER = 3;

    /**
     * Maximum value for the ACTIVITY_SIDE_SHEET_DECORATION_TYPE_* configuration options. For
     * validation purposes only.
     */
    private static final int ACTIVITY_SIDE_SHEET_DECORATION_TYPE_MAX = 3;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_DEFAULT,
            ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_NONE,
            ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActivitySideSheetRoundedCornersPosition {}

    /**
     * Side sheet's default rounded corner configuration. Same as
     * {@link CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_NONE}
     */
    public static final int ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_DEFAULT = 0;
    /**
     * Side sheet with no rounded corners.
     */
    public static final int ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_NONE = 1;
    /**
     * Side sheet with the inner top corner rounded (if positioned on the right of the screen, this
     * will be the top left corner)
     */
    public static final int ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP = 2;

    /**
     * Maximum value for the ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_* configuration options.
     * For validation purposes only.
     */
    private static final int ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_MAX = 2;

    /**
     * Extra that, if set, allows you to set how you want to distinguish the Partial Custom Tab
     * side sheet from the rest of the display. Options include shadow, a divider line, or no
     * decoration.
     */
    public static final String EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE =
            "androidx.browser.customtabs.extra.ACTIVITY_SIDE_SHEET_DECORATION_TYPE";

    /**
     *  Extra that, if set, allows you to choose which side sheet corners should be rounded, if any
     *  at all. Options include top or none.
     */
    public static final String EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION =
            "androidx.browser.customtabs.extra.ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION";

    /**
     * Extra that sets the toolbar's top corner radii in dp. This will only have
     * effect if the custom tab is behaving as a bottom sheet. Currently, this is capped at 16dp.
     */
    public static final String EXTRA_TOOLBAR_CORNER_RADIUS_DP =
            "androidx.browser.customtabs.extra.TOOLBAR_CORNER_RADIUS_DP";

    /**
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({CLOSE_BUTTON_POSITION_DEFAULT, CLOSE_BUTTON_POSITION_START, CLOSE_BUTTON_POSITION_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface CloseButtonPosition {
    }

    /** Same as {@link #CLOSE_BUTTON_POSITION_START}. */
    public static final int CLOSE_BUTTON_POSITION_DEFAULT = 0;

    /** Positions the close button at the start of the toolbar. */
    public static final int CLOSE_BUTTON_POSITION_START = 1;

    /** Positions the close button at the end of the toolbar. */
    public static final int CLOSE_BUTTON_POSITION_END = 2;

    /**
     * Maximum value for the CLOSE_BUTTON_POSITION_* configuration options. For validation purposes
     * only.
     */
    private static final int CLOSE_BUTTON_POSITION_MAX = 2;

    /**
     * Extra that specifies the position of the close button on the toolbar. Default is
     * {@link #CLOSE_BUTTON_POSITION_DEFAULT}.
     */
    public static final String EXTRA_CLOSE_BUTTON_POSITION =
            "androidx.browser.customtabs.extra.CLOSE_BUTTON_POSITION";

    /**
     * Extra that contains the color of the navigation bar divider.
     * See {@link Builder#setNavigationBarDividerColor}.
     */
    public static final String EXTRA_NAVIGATION_BAR_DIVIDER_COLOR =
            "androidx.browser.customtabs.extra.NAVIGATION_BAR_DIVIDER_COLOR";

    /**
     * Extra that specifies the {@link Network} to be bound when launching a Custom Tab or using
     * mayLaunchUrl.
     * See {@link Builder#setNetwork}.
     */
    public static final String EXTRA_NETWORK = "androidx.browser.customtabs.extra.NETWORK";

    /** Extra that enables ephemeral browsing within the Custom Tab. */
    public static final String EXTRA_ENABLE_EPHEMERAL_BROWSING =
            "androidx.browser.customtabs.extra.ENABLE_EPHEMERAL_BROWSING";

    /**
     * Extra that enables the close button and shows it in the toolbar. The close button is enabled
     * by default, this extra provides a way to disable the close button in the toolbar.
     */
    public static final String EXTRA_CLOSE_BUTTON_ENABLED =
            "androidx.browser.customtabs.extra.CLOSE_BUTTON_ENABLED";

    /**
     * Key that specifies the unique ID for an action button. To make a button to show on the
     * toolbar, use {@link #TOOLBAR_ACTION_BUTTON_ID} as its ID.
     */
    public static final String KEY_ID = "android.support.customtabs.customaction.ID";

    /**
     * The ID allocated to the custom action button that is shown on the toolbar.
     */
    public static final int TOOLBAR_ACTION_BUTTON_ID = 0;

    /**
     * Content target type: Image. Used with
     * {@link CustomContentAction.Builder#Builder(int, String, PendingIntent, int)}.
     * An action with this target type may be shown by the browser when the user interacts
     * with an image element on the web page.
     */
    @ExperimentalCustomContentAction
    public static final int CONTENT_TARGET_TYPE_IMAGE = 1;

    /**
     * Content target type: Link. Used with
     * {@link CustomContentAction.Builder#Builder(int, String, PendingIntent, int)}.
     * An action with this target type may be shown by the browser when the user interacts
     * with a link (hyperlink) element on the web page.
     */
    @ExperimentalCustomContentAction
    public static final int CONTENT_TARGET_TYPE_LINK = 2;

    /**
     * Defines the type of content a {@link CustomContentAction} can target.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef({CONTENT_TARGET_TYPE_IMAGE, CONTENT_TARGET_TYPE_LINK})
    @Retention(RetentionPolicy.SOURCE)
    @ExperimentalCustomContentAction
    public @interface ContentTargetType {
    }

    /**
     * Extra for an {@link ArrayList} of {@link Bundle} objects, each representing a
     * {@link CustomContentAction}. The browser reads this to display custom actions.
     * The client application adds this via
     * {@link Builder#addCustomContentAction(CustomContentAction)}.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CUSTOM_CONTENT_ACTIONS =
            "androidx.browser.customtabs.extra.CUSTOM_CONTENT_ACTIONS";

    /**
     * Extra added to the {@link PendingIntent} by the browser when a custom content action is
     * triggered. This extra contains the integer ID of the specific
     * {@link CustomContentAction} that was selected by the user. The client application
     * should use this ID to determine which custom action was invoked. The action id cannot be set
     * to negative numbers.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_TRIGGERED_CUSTOM_CONTENT_ACTION_ID =
            "androidx.browser.customtabs.extra.TRIGGERED_CUSTOM_CONTENT_ACTION_ID";

    /**
     * Extra added to the {@link PendingIntent} by the browser when a custom content action is
     * triggered. This extra contains an integer indicating the
     * {@link ContentTargetType} (e.g., {@link #CONTENT_TARGET_TYPE_IMAGE} or
     * {@link #CONTENT_TARGET_TYPE_LINK}) of the web content element that was interacted with.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CLICKED_CONTENT_TARGET_TYPE =
            "androidx.browser.customtabs.extra.CLICKED_CONTENT_TARGET_TYPE";

    /**
     * Extra added to the custom content action {@link PendingIntent} by the browser.
     * If the action was triggered on an image, this extra may contain the URL of that image.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CONTEXT_IMAGE_URL =
            "androidx.browser.customtabs.extra.CONTEXT_IMAGE_URL";

    /**
     * Extra added to the custom content action {@link PendingIntent} by the browser.
     * If the action was triggered on an image, this extra may contain the byte data of that image.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CONTEXT_IMAGE_DATA_URI =
            "androidx.browser.customtabs.extra.CONTEXT_IMAGE_DATA_URI";

    /**
     * Extra added to the custom content action {@link PendingIntent} by the browser.
     * If the action was triggered on an image, this extra may contain the URL of the page
     * displaying that image.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CONTEXT_LINK_URL =
            "androidx.browser.customtabs.extra.CONTEXT_LINK_URL";

    /**
     * Extra added to the custom content action {@link PendingIntent} by the browser.
     * If the action was triggered on an image, this extra may contain the alt text of the
     * clicked image, if available.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CONTEXT_IMAGE_ALT_TEXT =
            "androidx.browser.customtabs.extra.CONTEXT_IMAGE_ALT_TEXT";

    /**
     * Extra added to the custom content action {@link PendingIntent} by the browser.
     * If the action was triggered on a link, this extra may contain the visible text if available.
     */
    @ExperimentalCustomContentAction
    public static final String EXTRA_CONTEXT_LINK_TEXT =
            "androidx.browser.customtabs.extra.CONTEXT_LINK_TEXT";

    /**
     * The maximum allowed number of toolbar items.
     */
    private static final int MAX_TOOLBAR_ITEMS = 5;

    /**
     * The maximum toolbar corner radius in dp.
     */
    private static final int MAX_TOOLBAR_CORNER_RADIUS_DP = 16;

    /**
     * The name of the accept language HTTP header.
     */
    private static final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";

    /**
     * An {@link Intent} used to start the Custom Tabs Activity.
     */
    public final @NonNull Intent intent;

    /**
     * A {@link Bundle} containing the start animation for the Custom Tabs Activity.
     */
    public final @Nullable Bundle startAnimationBundle;

    /**
     * Convenience method to launch a Custom Tabs Activity.
     * @param context The source Context.
     * @param url The URL to load in the Custom Tab.
     */
    public void launchUrl(@NonNull Context context, @NonNull Uri url) {
        intent.setData(url);
        ContextCompat.startActivity(context, intent, startAnimationBundle);
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    CustomTabsIntent(@NonNull Intent intent, @Nullable Bundle startAnimationBundle) {
        this.intent = intent;
        this.startAnimationBundle = startAnimationBundle;
    }

    /**
     * Builder class for {@link CustomTabsIntent} objects.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(Intent.ACTION_VIEW);
        private final CustomTabColorSchemeParams.Builder mDefaultColorSchemeBuilder =
                new CustomTabColorSchemeParams.Builder();
        private @Nullable ArrayList<Bundle> mMenuItems;
        private @Nullable ActivityOptions mActivityOptions;
        private @Nullable ArrayList<Bundle> mActionButtons;
        private @Nullable SparseArray<Bundle> mColorSchemeParamBundles;
        private @Nullable Bundle mDefaultColorSchemeBundle;
        @ShareState private int mShareState = SHARE_STATE_DEFAULT;
        private boolean mInstantAppsEnabled = true;
        private boolean mShareIdentity;
        private @Nullable ArrayList<Bundle> mCustomContentActionBundles;

        /**
         * Creates a {@link CustomTabsIntent.Builder} object associated with no
         * {@link CustomTabsSession}.
         */
        public Builder() {}

        /**
         * Creates a {@link CustomTabsIntent.Builder} object associated with a given
         * {@link CustomTabsSession}.
         *
         * Guarantees that the {@link Intent} will be sent to the same component as the one the
         * session is associated with.
         *
         * @param session The session to associate this Builder with.
         */
        public Builder(@Nullable CustomTabsSession session) {
            if (session != null) {
                setSession(session);
            }
        }

        /**
         * Associates the {@link Intent} with the given {@link CustomTabsSession}.
         *
         * Guarantees that the {@link Intent} will be sent to the same component as the one the
         * session is associated with.
         */
        public @NonNull Builder setSession(@NonNull CustomTabsSession session) {
            mIntent.setPackage(session.getComponentName().getPackageName());
            setSessionParameters(session.getBinder(), session.getId());
            return this;
        }

        /**
         * Associates the {@link Intent} with the given {@link CustomTabsSession.PendingSession}.
         * Overrides the effect of {@link #setSession}.
         *
         */
        public @NonNull Builder setPendingSession(
                CustomTabsSession.@NonNull PendingSession session) {
            setSessionParameters(null, session.getId());
            return this;
        }

        private void setSessionParameters(@Nullable IBinder binder,
                @Nullable PendingIntent sessionId) {
            Bundle bundle = new Bundle();
            bundle.putBinder(EXTRA_SESSION, binder);
            if (sessionId != null) {
                bundle.putParcelable(EXTRA_SESSION_ID, sessionId);
            }

            mIntent.putExtras(bundle);
        }

        /**
         * Sets the toolbar color.
         *
         * On Android L and above, this color is also applied to the status bar. To ensure good
         * contrast between status bar icons and the background, Custom Tab implementations may use
         * {@link View#SYSTEM_UI_FLAG_LIGHT_STATUS_BAR} on Android M and above, and use a darkened
         * color for the status bar on Android L.
         *
         * Can be overridden for particular color schemes, see {@link #setColorSchemeParams}.
         *
         * @param color {@link Color}
         *
         * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
         */
        @Deprecated
        public @NonNull Builder setToolbarColor(@ColorInt int color) {
            mDefaultColorSchemeBuilder.setToolbarColor(color);
            return this;
        }

        /**
         * Enables the url bar to hide as the user scrolls down on the page.
         * @deprecated Use {@link #setUrlBarHidingEnabled(boolean)} instead.
         */
        @Deprecated
        public @NonNull Builder enableUrlBarHiding() {
            mIntent.putExtra(EXTRA_ENABLE_URLBAR_HIDING, true);
            return this;
        }

        /**
         * Set whether the url bar should hide as the user scrolls down on the page.
         *
         * @param enabled Whether url bar hiding is enabled.
         */
        public @NonNull Builder setUrlBarHidingEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_ENABLE_URLBAR_HIDING, enabled);
            return this;
        }

        /**
         * Sets the Close button icon for the custom tab.
         *
         * If the close button is disabled (see {@link #setCloseButtonEnabled(boolean)}), then
         * calling this function has no effect.
         *
         * @param icon The icon {@link Bitmap}
         */
        public @NonNull Builder setCloseButtonIcon(@NonNull Bitmap icon) {
            mIntent.putExtra(EXTRA_CLOSE_BUTTON_ICON, icon);
            return this;
        }

        /**
         * Sets whether the title should be shown in the custom tab.
         *
         * @param showTitle Whether the title should be shown.
         */
        public @NonNull Builder setShowTitle(boolean showTitle) {
            mIntent.putExtra(EXTRA_TITLE_VISIBILITY_STATE,
                    showTitle ? SHOW_PAGE_TITLE : NO_TITLE);
            return this;
        }

        /**
         * Adds a menu item.
         *
         * @param label Menu label.
         * @param pendingIntent Pending intent delivered when the menu item is clicked.
         */
        public @NonNull Builder addMenuItem(@NonNull String label,
                @NonNull PendingIntent pendingIntent) {
            if (mMenuItems == null) mMenuItems = new ArrayList<>();
            Bundle bundle = new Bundle();
            bundle.putString(KEY_MENU_ITEM_TITLE, label);
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent);
            mMenuItems.add(bundle);
            return this;
        }

        /**
         * Adds a default share item to the menu.
         * @deprecated Use {@link #setShareState(int)} instead. This will set the share state to
         * {@link CustomTabsIntent#SHARE_STATE_ON}.
         */
        @Deprecated
        public @NonNull Builder addDefaultShareMenuItem() {
            setShareState(SHARE_STATE_ON);
            return this;
        }

        /**
         * Set whether a default share item is added to the menu.
         *
         * @param enabled Whether default share item is added.
         * @deprecated Use {@link #setShareState(int)} instead. This will set the share state to
         * {@link CustomTabsIntent#SHARE_STATE_ON} or {@link CustomTabsIntent#SHARE_STATE_OFF}
         * based on {@code enabled}.
         */
        @Deprecated
        public @NonNull Builder setDefaultShareMenuItemEnabled(boolean enabled) {
            if (enabled) {
                setShareState(SHARE_STATE_ON);
            } else {
                setShareState(SHARE_STATE_OFF);
            }
            return this;
        }

        /**
         * Sets the share state that should be applied to the custom tab.
         *
         * @param shareState Desired share state.
         *
         * @see CustomTabsIntent#SHARE_STATE_DEFAULT
         * @see CustomTabsIntent#SHARE_STATE_ON
         * @see CustomTabsIntent#SHARE_STATE_OFF
         */
        public @NonNull Builder setShareState(@ShareState int shareState) {
            if (shareState < 0 || shareState > SHARE_STATE_MAX) {
                throw new IllegalArgumentException("Invalid value for the shareState argument");
            }
            mShareState = shareState;
            // Add share menu item extra for backwards compatibility with {@link
            // #addDefaultShareMenuItem} and {@link #setDefaultShareMenuItemEnabled}.
            if (shareState == SHARE_STATE_ON) {
                mIntent.putExtra(EXTRA_DEFAULT_SHARE_MENU_ITEM, true);
            } else if (shareState == SHARE_STATE_OFF) {
                mIntent.putExtra(EXTRA_DEFAULT_SHARE_MENU_ITEM, false);
            } else {
                mIntent.removeExtra(EXTRA_DEFAULT_SHARE_MENU_ITEM);
            }
            return this;
        }

        /**
         * Sets the action button that is displayed in the Toolbar.
         * <p>
         * This is equivalent to calling
         * {@link CustomTabsIntent.Builder#addToolbarItem(int, Bitmap, String, PendingIntent)}
         * with {@link #TOOLBAR_ACTION_BUTTON_ID} as id.
         *
         * @param icon The icon.
         * @param description The description for the button. To be used for accessibility.
         * @param pendingIntent pending intent delivered when the button is clicked.
         * @param shouldTint Whether the action button should be tinted..
         *
         * @see CustomTabsIntent.Builder#addToolbarItem(int, Bitmap, String, PendingIntent)
         */
        public @NonNull Builder setActionButton(@NonNull Bitmap icon, @NonNull String description,
                @NonNull PendingIntent pendingIntent, boolean shouldTint) {
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, TOOLBAR_ACTION_BUTTON_ID);
            bundle.putParcelable(KEY_ICON, icon);
            bundle.putString(KEY_DESCRIPTION, description);
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent);
            mIntent.putExtra(EXTRA_ACTION_BUTTON_BUNDLE, bundle);
            mIntent.putExtra(EXTRA_TINT_ACTION_BUTTON, shouldTint);
            return this;
        }

        /**
         * Sets the action button that is displayed in the Toolbar with default tinting behavior.
         *
         * @see CustomTabsIntent.Builder#setActionButton(
         * Bitmap, String, PendingIntent, boolean)
         */
        public @NonNull Builder setActionButton(@NonNull Bitmap icon, @NonNull String description,
                @NonNull PendingIntent pendingIntent) {
            return setActionButton(icon, description, pendingIntent, false);
        }

        /**
         * Adds an action button to the custom tab. Multiple buttons can be added via this method.
         * If the given id equals {@link #TOOLBAR_ACTION_BUTTON_ID}, the button will be placed on
         * the toolbar; if the bitmap is too wide, it will be put to the bottom bar instead. If
         * the id is not {@link #TOOLBAR_ACTION_BUTTON_ID}, it will be directly put on secondary
         * toolbar. The maximum number of allowed toolbar items in a single intent is
         * {@link CustomTabsIntent#getMaxToolbarItems()}. Throws an
         * {@link IllegalStateException} when that number is exceeded per intent.
         *
         * @param id The unique id of the action button. This should be non-negative.
         * @param icon The icon.
         * @param description The description for the button. To be used for accessibility.
         * @param pendingIntent The pending intent delivered when the button is clicked.
         *
         * @see CustomTabsIntent#getMaxToolbarItems()
         * @deprecated Use
         * CustomTabsIntent.Builder#setSecondaryToolbarViews(RemoteViews, int[], PendingIntent).
         */
        @Deprecated
        public @NonNull Builder addToolbarItem(int id, @NonNull Bitmap icon,
                @NonNull String description, @NonNull PendingIntent pendingIntent)
                throws IllegalStateException {
            if (mActionButtons == null) {
                mActionButtons = new ArrayList<>();
            }
            if (mActionButtons.size() >= MAX_TOOLBAR_ITEMS) {
                throw new IllegalStateException(
                        "Exceeded maximum toolbar item count of " + MAX_TOOLBAR_ITEMS);
            }
            Bundle bundle = new Bundle();
            bundle.putInt(KEY_ID, id);
            bundle.putParcelable(KEY_ICON, icon);
            bundle.putString(KEY_DESCRIPTION, description);
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent);
            mActionButtons.add(bundle);
            return this;
        }

        /**
         * Sets the color of the secondary toolbar.
         * Can be overridden for particular color schemes, see {@link #setColorSchemeParams}.
         *
         * @param color The color for the secondary toolbar.
         *
         * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
         */
        @Deprecated
        public @NonNull Builder setSecondaryToolbarColor(@ColorInt int color) {
            mDefaultColorSchemeBuilder.setSecondaryToolbarColor(color);
            return this;
        }

        /**
         * Sets the navigation bar color. Has no effect on API versions below L.
         *
         * To ensure good contrast between navigation bar icons and the background, Custom Tab
         * implementations may use {@link View#SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR} on Android O and
         * above, and darken the provided color on Android L-N.
         *
         * Can be overridden for particular color schemes, see {@link #setColorSchemeParams}.
         *
         * @param color The color for the navigation bar.
         *
         * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
         */
        @Deprecated
        public @NonNull Builder setNavigationBarColor(@ColorInt int color) {
            mDefaultColorSchemeBuilder.setNavigationBarColor(color);
            return this;
        }

        /**
         * Sets the navigation bar divider color. Has no effect on API versions below P.
         *
         * Can be overridden for particular color schemes, see {@link #setColorSchemeParams}.
         *
         * @param color The color for the navigation bar divider.
         *
         * @deprecated Use {@link #setDefaultColorSchemeParams} instead.
         */
        @Deprecated
        public @NonNull Builder setNavigationBarDividerColor(@ColorInt int color) {
            mDefaultColorSchemeBuilder.setNavigationBarDividerColor(color);
            return this;
        }

        /**
         * Sets the remote views displayed in the secondary toolbar in a custom tab.
         *
         * @param remoteViews   The {@link RemoteViews} that will be shown on the secondary toolbar.
         * @param clickableIDs  The IDs of clickable views. The onClick event of these views will be
         *                      handled by custom tabs.
         * @param pendingIntent The {@link PendingIntent} that will be sent when the user clicks on
         *                      one of the {@link View}s in clickableIDs. When the
         *                      {@link PendingIntent} is sent, it will have the current URL as its
         *                      intent data.
         * @see CustomTabsIntent#EXTRA_REMOTEVIEWS
         * @see CustomTabsIntent#EXTRA_REMOTEVIEWS_VIEW_IDS
         * @see CustomTabsIntent#EXTRA_REMOTEVIEWS_PENDINGINTENT
         * @see CustomTabsIntent#EXTRA_REMOTEVIEWS_CLICKED_ID
         */
        public @NonNull Builder setSecondaryToolbarViews(@NonNull RemoteViews remoteViews,
                int @Nullable [] clickableIDs, @Nullable PendingIntent pendingIntent) {
            mIntent.putExtra(EXTRA_REMOTEVIEWS, remoteViews);
            mIntent.putExtra(EXTRA_REMOTEVIEWS_VIEW_IDS, clickableIDs);
            mIntent.putExtra(EXTRA_REMOTEVIEWS_PENDINGINTENT, pendingIntent);
            return this;
        }

        /**
         * Sets the {@link PendingIntent} to be sent when the user swipes up from
         * the secondary (bottom) toolbar.
         * @param pendingIntent The {@link PendingIntent} that will be sent when
         *                      the user swipes up from the secondary toolbar.
         */
        public @NonNull Builder setSecondaryToolbarSwipeUpGesture(
                @Nullable PendingIntent pendingIntent) {
            mIntent.putExtra(EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE, pendingIntent);
            return this;
        }

        /**
         * Sets whether Instant Apps is enabled for this Custom Tab.

         * @param enabled Whether Instant Apps should be enabled.
         */
        public @NonNull Builder setInstantAppsEnabled(boolean enabled) {
            mInstantAppsEnabled = enabled;
            return this;
        }

        /**
         * Sets the start animations.
         *
         * @param context Application context.
         * @param enterResId Resource ID of the "enter" animation for the browser.
         * @param exitResId Resource ID of the "exit" animation for the application.
         */
        @SuppressWarnings("NullAway") // TODO: b/141869399
        public @NonNull Builder setStartAnimations(
                @NonNull Context context, @AnimRes int enterResId, @AnimRes int exitResId) {
            // We use ActivityOptions, not ActivityOptionsCompat, to build the start activity
            // options, since we might set another option (share identity, which is not
            // available yet via ActivityOptionsCompat) before turning it to a Bundle.
            // TODO(b/296463161): Update androidx.core.core lib to support the new option via
            // ActivityOptionsCompat and use it here instead of ActivityOptions.
            mActivityOptions = ActivityOptions.makeCustomAnimation(
                    context, enterResId, exitResId);
            return this;
        }

        /**
         * Sets the exit animations.
         *
         * @param context Application context.
         * @param enterResId Resource ID of the "enter" animation for the application.
         * @param exitResId Resource ID of the "exit" animation for the browser.
         */
        public @NonNull Builder setExitAnimations(
                @NonNull Context context, @AnimRes int enterResId, @AnimRes int exitResId) {
            Bundle bundle = ActivityOptionsCompat.makeCustomAnimation(
                    context, enterResId, exitResId).toBundle();
            mIntent.putExtra(EXTRA_EXIT_ANIMATION_BUNDLE, bundle);
            return this;
        }

        /**
         * Sets the color scheme that should be applied to the user interface in the custom tab.
         *
         * @param colorScheme Desired color scheme.
         * @see CustomTabsIntent#COLOR_SCHEME_SYSTEM
         * @see CustomTabsIntent#COLOR_SCHEME_LIGHT
         * @see CustomTabsIntent#COLOR_SCHEME_DARK
         */
        public @NonNull Builder setColorScheme(@ColorScheme int colorScheme) {
            if (colorScheme < 0 || colorScheme > COLOR_SCHEME_MAX) {
                throw new IllegalArgumentException("Invalid value for the colorScheme argument");
            }
            mIntent.putExtra(EXTRA_COLOR_SCHEME, colorScheme);
            return this;
        }

        /**
         * Sets {@link CustomTabColorSchemeParams} for the given color scheme.
         *
         * This allows specifying two different toolbar colors for light and dark schemes.
         * It can be useful if {@link CustomTabsIntent#COLOR_SCHEME_SYSTEM} is set: Custom Tabs
         * will follow the system settings and apply the corresponding
         * {@link CustomTabColorSchemeParams} "on the fly" when the settings change.
         *
         * If there is no {@link CustomTabColorSchemeParams} for the current scheme, or a particular
         * field of it is null, Custom Tabs will fall back to the defaults provided via
         * {@link #setDefaultColorSchemeParams}.
         *
         * Example:
         * <pre><code>
         *     CustomTabColorSchemeParams darkParams = new CustomTabColorSchemeParams.Builder()
         *             .setToolbarColor(darkColor)
         *             .build();
         *     CustomTabColorSchemeParams otherParams = new CustomTabColorSchemeParams.Builder()
         *             .setNavigationBarColor(otherColor)
         *             .build();
         *     CustomTabIntent intent = new CustomTabIntent.Builder()
         *             .setColorScheme(COLOR_SCHEME_SYSTEM)
         *             .setColorSchemeParams(COLOR_SCHEME_DARK, darkParams)
         *             .setDefaultColorSchemeParams(otherParams)
         *             .build();
         * </code></pre>
         *
         * @param colorScheme A constant representing a color scheme (see {@link #setColorScheme}).
         *                    It should not be {@link #COLOR_SCHEME_SYSTEM}, because that represents
         *                    a behavior rather than a particular color scheme.
         * @param params An instance of {@link CustomTabColorSchemeParams}.
         */
        public @NonNull Builder setColorSchemeParams(@ColorScheme int colorScheme,
                @NonNull CustomTabColorSchemeParams params) {
            if (colorScheme < 0 || colorScheme > COLOR_SCHEME_MAX
                    || colorScheme == COLOR_SCHEME_SYSTEM) {
                throw new IllegalArgumentException("Invalid colorScheme: " + colorScheme);
            }
            if (mColorSchemeParamBundles == null) {
                mColorSchemeParamBundles = new SparseArray<>();
            }
            mColorSchemeParamBundles.put(colorScheme, params.toBundle());
            return this;
        }


        /**
         * Sets the default {@link CustomTabColorSchemeParams}.
         *
         * This will set a default color scheme that applies when no CustomTabColorSchemeParams
         * specified for current color scheme via {@link #setColorSchemeParams}.
         *
         * @param params An instance of {@link CustomTabColorSchemeParams}.
         */
        public @NonNull Builder setDefaultColorSchemeParams(
                @NonNull CustomTabColorSchemeParams params) {
            mDefaultColorSchemeBundle = params.toBundle();
            return this;
        }

        /**
         * Sets the Custom Tab Activity's initial height in pixels and the desired resize behavior.
         * The Custom Tab will behave as a bottom sheet if the screen's width is smaller than
         * the breakpoint value set by
         * {@link CustomTabsIntent.Builder#setActivitySideSheetBreakpointDp(int)}.
         *
         * @param initialHeightPx The Custom Tab Activity's initial height in pixels.
         * @param activityHeightResizeBehavior Desired height behavior.
         * @see CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX
         * @see CustomTabsIntent#EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_DEFAULT
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_ADJUSTABLE
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_FIXED
         */
        public @NonNull Builder setInitialActivityHeightPx(
                @Dimension(unit = PX) int initialHeightPx,
                @ActivityHeightResizeBehavior int activityHeightResizeBehavior) {
            if (initialHeightPx <= 0) {
                throw new IllegalArgumentException("Invalid value for the initialHeightPx "
                        + "argument");
            }
            if (activityHeightResizeBehavior < 0
                    || activityHeightResizeBehavior > ACTIVITY_HEIGHT_MAX) {
                throw new IllegalArgumentException(
                        "Invalid value for the activityHeightResizeBehavior argument");
            }

            mIntent.putExtra(EXTRA_INITIAL_ACTIVITY_HEIGHT_PX, initialHeightPx);
            mIntent.putExtra(EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR, activityHeightResizeBehavior);
            return this;
        }

        /**
         * Sets the Custom Tab Activity's initial height in pixels with default resize behavior.
         * The Custom Tab will behave as a bottom sheet if the screen's width is smaller than
         * the breakpoint value set by
         * {@link CustomTabsIntent.Builder#setActivitySideSheetBreakpointDp(int)}.
         *
         * @see CustomTabsIntent.Builder#setInitialActivityHeightPx(int, int)
         */
        public @NonNull Builder setInitialActivityHeightPx(
                @Dimension(unit = PX) int initialHeightPx) {
            return setInitialActivityHeightPx(initialHeightPx, ACTIVITY_HEIGHT_DEFAULT);
        }

        /**
         * Sets the Custom Tab Activity's initial width in pixels. The Custom Tab will behave as
         * a side sheet if the screen's width is bigger than the breakpoint value set by
         * {@link CustomTabsIntent.Builder#setActivitySideSheetBreakpointDp(int)} and the screen is
         * big enough, see doc for {@link CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_WIDTH_PX}.
         * @param initialWidthPx  The Custom Tab Activity's initial width in pixels.
         * @see CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_WIDTH_PX
         */
        public @NonNull Builder setInitialActivityWidthPx(
                @Dimension(unit = PX) int initialWidthPx) {
            if (initialWidthPx <= 0) {
                throw new IllegalArgumentException("Invalid value for the initialWidthPx "
                        + "argument");
            }

            mIntent.putExtra(EXTRA_INITIAL_ACTIVITY_WIDTH_PX, initialWidthPx);
            return this;
        }

        /**
         * Sets the Custom Tab Activity's transition breakpoint in DP.
         * @param breakpointDp The Custom Tab Activity's breakpoint in DP.
         * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP
         */
        public @NonNull Builder setActivitySideSheetBreakpointDp(
                @Dimension(unit = DP) int breakpointDp) {
            if (breakpointDp <= 0) {
                throw new IllegalArgumentException("Invalid value for the initialWidthPx "
                        + "argument");
            }

            mIntent.putExtra(EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP, breakpointDp);
            return this;
        }

        /**
         * Enables or disables the maximization button for when the Custom Tab Activity is acting
         * as a side sheet. The button is disabled by default.
         * @param enabled Whether the maximization button is enabled.
         * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION
         */
        public @NonNull Builder setActivitySideSheetMaximizationEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION, enabled);
            return this;
        }

        /**
         * Sets the Custom Tab Activity's position when acting as a side sheet.
         * @param position The Custom Tab Activity's position.
         * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_POSITION
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_DEFAULT
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_START
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_END
         */
        public @NonNull Builder setActivitySideSheetPosition(
                @ActivitySideSheetPosition int position) {
            if (position < 0 || position > ACTIVITY_SIDE_SHEET_POSITION_MAX) {
                throw new IllegalArgumentException(
                        "Invalid value for the sideSheetPosition argument");
            }

            mIntent.putExtra(EXTRA_ACTIVITY_SIDE_SHEET_POSITION, position);
            return this;
        }

        /**
         * Sets the Custom Tab Activity's decoration type that will be displayed when it is
         * acting as a side sheet.
         * @param decorationType The Custom Tab Activity's decoration type.
         * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_NONE
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER
         */
        public @NonNull Builder setActivitySideSheetDecorationType(
                @ActivitySideSheetDecorationType int decorationType) {
            if (decorationType < 0 || decorationType > ACTIVITY_SIDE_SHEET_DECORATION_TYPE_MAX) {
                throw new IllegalArgumentException("Invalid value for the decorationType argument");
            }

            mIntent.putExtra(EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE, decorationType);
            return this;
        }

        /**
         * Sets the Custom Tab Activity's rounded corners position when it is acting as a
         * side sheet.
         * @param roundedCornersPosition The Custom Tab Activity's rounded corners position.
         * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_DEFAULT
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_NONE
         * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
         */
        public @NonNull Builder setActivitySideSheetRoundedCornersPosition(
                @ActivitySideSheetDecorationType int roundedCornersPosition) {
            if (roundedCornersPosition < 0
                    || roundedCornersPosition > ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_MAX) {
                throw new IllegalArgumentException("Invalid value for the roundedCornersPosition./"
                        + " argument");
            }

            mIntent.putExtra(EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION,
                    roundedCornersPosition);
            return this;
        }

        /**
         * Sets the toolbar's top corner radii in dp.
         *
         * @param cornerRadiusDp The toolbar's top corner radii in dp.
         * @see CustomTabsIntent#EXTRA_TOOLBAR_CORNER_RADIUS_DP
         */
        public @NonNull Builder setToolbarCornerRadiusDp(@Dimension(unit = DP) int cornerRadiusDp) {
            if (cornerRadiusDp < 0 || cornerRadiusDp > MAX_TOOLBAR_CORNER_RADIUS_DP) {
                throw new IllegalArgumentException("Invalid value for the cornerRadiusDp argument");
            }

            mIntent.putExtra(EXTRA_TOOLBAR_CORNER_RADIUS_DP, cornerRadiusDp);
            return this;
        }

        /**
         * Sets the position of the close button.
         *
         * If the close button is disabled (see {@link #setCloseButtonEnabled(boolean)}), then
         * calling this function has no effect.
         *
         * @param position The desired position.
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_DEFAULT
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_START
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_END
         */
        public @NonNull Builder setCloseButtonPosition(@CloseButtonPosition int position) {
            if (position < 0 || position > CLOSE_BUTTON_POSITION_MAX) {
                throw new IllegalArgumentException("Invalid value for the position argument");
            }

            mIntent.putExtra(EXTRA_CLOSE_BUTTON_POSITION, position);
            return this;
        }

        /**
         * Sets the state for the Open in Browser button in the toolbar for this Custom Tab.
         *
         * @param openInBrowserState Desired Open in Browser state.
         * @see CustomTabsIntent#EXTRA_OPEN_IN_BROWSER_STATE
         * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_DEFAULT
         * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_ON
         * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_OFF
         * @throws IllegalArgumentException when an invalid option is provided.
         */
        @ExperimentalOpenInBrowser
        public @NonNull Builder setOpenInBrowserButtonState(
                @OpenInBrowserState int openInBrowserState) {
            if (openInBrowserState < 0 || openInBrowserState > OPEN_IN_BROWSER_STATE_MAX) {
                throw new IllegalArgumentException(
                        "Invalid value for the openInBrowserState argument.");
            }

            mIntent.putExtra(EXTRA_OPEN_IN_BROWSER_STATE, openInBrowserState);
            return this;
        }

        /**
         * Enables or disables the bookmarks button in the overflow menu. The button
         * is enabled by default.
         *
         * @param enabled Whether the start button is enabled.
         * @see CustomTabsIntent#EXTRA_DISABLE_BOOKMARKS_BUTTON
         */
        public @NonNull Builder setBookmarksButtonEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_DISABLE_BOOKMARKS_BUTTON, !enabled);
            return this;
        }

        /**
         * Enables or disables the download button in the overflow menu. The button
         * is enabled by default.
         *
         * @param enabled Whether the download button is enabled.
         * @see CustomTabsIntent#EXTRA_DISABLE_DOWNLOAD_BUTTON
         */
        public @NonNull Builder setDownloadButtonEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_DISABLE_DOWNLOAD_BUTTON, !enabled);
            return this;
        }

        /**
         * Enables sending any redirects off of the initial URL sent to the Custom Tab to default
         * handling apps. Unless #EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER is also set, the
         * initial URL will still always load in the Custom Tab.
         *
         * @param enabled Whether to send urls to external handler.
         * @see CustomTabsIntent#EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER
         */
        public @NonNull Builder setSendToExternalDefaultHandlerEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER, enabled);
            return this;
        }

        /**
         * Allows the Custom Tab to forward the initial URL sent to the Custom Tab to the default
         * handling app for that URL instead of loading it (including following any redirects) in
         * the Custom Tab.
         * <p>
         * Note: This is not the default behavior. The default for this is to be set to false.
         *
         * @param enabled Whether the initial URL sent to the custom tab should be sent to the
         *                default handling app for that URL.
         * @see CustomTabsIntent#EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER
         */
        @ExperimentalInitialNavigationCanLeaveBrowser
        public @NonNull Builder setInitialNavigationAllowedToLeaveBrowser(boolean enabled) {
            mIntent.putExtra(EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER, enabled);
            return this;
        }

        /**
         * Specifies the target locale the Translate UI should be triggered with.
         *
         * @param locale {@link Locale} object that represents the target locale.
         * @see CustomTabsIntent#EXTRA_TRANSLATE_LANGUAGE_TAG
         */
        public @NonNull Builder setTranslateLocale(@NonNull Locale locale) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setLanguageTag(locale);
            }
            return this;
        }

        /**
         * Enables the capability of the interaction with background.
         *
         * Enables the interactions with the background app when a Partial Custom Tab is launched.
         *
         * @param enabled Whether the background interaction is enabled.
         * @see CustomTabsIntent#EXTRA_DISABLE_BACKGROUND_INTERACTION
         */
        public @NonNull Builder setBackgroundInteractionEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_DISABLE_BACKGROUND_INTERACTION, !enabled);
            return this;
        }

        /**
         * Allow Custom Tabs to obtain the caller's identity i.e. package name.
         * @param enabled Whether the identity sharing is enabled.
         */
        public @NonNull Builder setShareIdentityEnabled(boolean enabled) {
            mShareIdentity = enabled;
            return this;
        }

        /**
         * Sets the target network {@link Network} to bind when launching a custom tab.
         *
         * This API allows the caller to specify the target network to bind when launching a URL
         * via Custom Tabs, e.g. may want to open a custom tab over a Wi-Fi network, while the
         * default network is a cellular connection. All URLRequests created in the future via this
         * tab will be bound to {@link Network}.
         *
         * If the browser does not support this feature it will be ignored and a Custom Tab will
         * be opened using the default network. Check the support by calling {@link
         * CustomTabsClient#isSetNetworkSupported}.
         *
         * @param network {@link Network} the target network to be bound.
         * @see CustomTabsIntent#EXTRA_NETWORK
         */
        public @NonNull Builder setNetwork(@NonNull Network network) {
            mIntent.putExtra(EXTRA_NETWORK, network);
            return this;
        }

        /**
         * Sets whether to enable ephemeral browsing within the Custom Tab. If ephemeral browsing is
         * enabled, and the browser supports it, the Custom Tab does not share cookies or other data
         * with the browser.
         *
         *
         * @param enabled Whether ephemeral browsing is enabled.
         * @see CustomTabsIntent#EXTRA_ENABLE_EPHEMERAL_BROWSING
         */
        public @NonNull Builder setEphemeralBrowsingEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_ENABLE_EPHEMERAL_BROWSING, enabled);
            return this;
        }

        /**
         * Sets whether to enable the close button for custom tab.
         *
         * The close button is enabled by default. If the close button is disabled, calls
         * to {@link #setCloseButtonIcon(Bitmap)} or {@link #setCloseButtonPosition(int)}
         * have no effect.
         *
         * @param enabled Whether the close button is enabled.
         * @see CustomTabsIntent#EXTRA_CLOSE_BUTTON_ENABLED
         */
        public @NonNull Builder setCloseButtonEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_CLOSE_BUTTON_ENABLED, enabled);
            return this;
        }

        /**
         * Adds a custom content action that can be invoked by the user on specific content types
         * (image or link) within the Custom Tab.
         * <p>
         * The number of actions shown to the user is at the browser's discretion. The browser
         * may choose not to display all the actions provided. Additionally, priority will be based
         * on the order the actions were added, with the earliest added action being given the
         * highest priority.
         * <p>
         * When the user triggers this action, the browser will send the
         * {@link CustomContentAction#getPendingIntent()} associated with this action. The
         * {@link Intent} within the {@link PendingIntent} will include the current URL of the
         * Custom Tab as its data, {@link CustomTabsIntent#EXTRA_TRIGGERED_CUSTOM_CONTENT_ACTION_ID}
         * with the ID of this action, {@link CustomTabsIntent#EXTRA_CLICKED_CONTENT_TARGET_TYPE}
         * with the type of content interacted with, and potentially other contextual extras
         * (e.g., {@link CustomTabsIntent#EXTRA_CONTEXT_IMAGE_URL}).
         *
         * @param action The {@link CustomContentAction} to add. Must not be null.
         * @return This Builder.
         * @throws IllegalArgumentException if an action with the same ID has already been added.
         */
        @ExperimentalCustomContentAction
        public @NonNull Builder addCustomContentAction(@NonNull CustomContentAction action) {
            if (mCustomContentActionBundles == null) {
                mCustomContentActionBundles = new ArrayList<>();
            }
            // Check for duplicate IDs
            for (Bundle existingBundle : mCustomContentActionBundles) {
                if (existingBundle.getInt(CustomContentAction.KEY_ID) == action.getId()) {
                    throw new IllegalArgumentException("CustomContentAction with ID "
                            + action.getId() + " already exists.");
                }
            }
            mCustomContentActionBundles.add(action.toBundle());
            return this;
        }

        /**
         * Combines all the options that have been set and returns a new {@link CustomTabsIntent}
         * object.
         */
        public @NonNull CustomTabsIntent build() {
            if (!mIntent.hasExtra(EXTRA_SESSION)) {
                // The intent must have EXTRA_SESSION, even if it is null.
                setSessionParameters(null, null);
            }
            if (mMenuItems != null) {
                mIntent.putParcelableArrayListExtra(CustomTabsIntent.EXTRA_MENU_ITEMS, mMenuItems);
            }
            if (mActionButtons != null) {
                mIntent.putParcelableArrayListExtra(EXTRA_TOOLBAR_ITEMS, mActionButtons);
            }
            mIntent.putExtra(EXTRA_ENABLE_INSTANT_APPS, mInstantAppsEnabled);

            mIntent.putExtras(mDefaultColorSchemeBuilder.build().toBundle());
            if (mDefaultColorSchemeBundle != null) {
                mIntent.putExtras(mDefaultColorSchemeBundle);
            }

            if (mColorSchemeParamBundles != null) {
                Bundle bundle = new Bundle();
                bundle.putSparseParcelableArray(EXTRA_COLOR_SCHEME_PARAMS,
                        mColorSchemeParamBundles);
                mIntent.putExtras(bundle);
            }
            mIntent.putExtra(EXTRA_SHARE_STATE, mShareState);
            if (mCustomContentActionBundles != null && !mCustomContentActionBundles.isEmpty()) {
                mIntent.putParcelableArrayListExtra(EXTRA_CUSTOM_CONTENT_ACTIONS,
                        mCustomContentActionBundles);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setCurrentLocaleAsDefaultAcceptLanguage();
            }

            Bundle bundle = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setShareIdentityEnabled();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                setAllowPassThroughOnTouchOutside();
            }
            if (mActivityOptions != null) {
                bundle = mActivityOptions.toBundle();
            }
            return new CustomTabsIntent(mIntent, bundle);
        }

        /**
         * Sets the current app's locale as default Accept-Language. If the app has its own locale,
         * we set it to Accept-Language, otherwise use the system locale.
         */
        @RequiresApi(api = Build.VERSION_CODES.N)
        private void setCurrentLocaleAsDefaultAcceptLanguage() {
            String defaultLocale = Api24Impl.getDefaultLocale();
            if (!TextUtils.isEmpty(defaultLocale)) {
                Bundle header = mIntent.hasExtra(Browser.EXTRA_HEADERS) ?
                        mIntent.getBundleExtra(Browser.EXTRA_HEADERS) : new Bundle();
                if (!header.containsKey(HTTP_ACCEPT_LANGUAGE)) {
                    header.putString(HTTP_ACCEPT_LANGUAGE, defaultLocale);
                    mIntent.putExtra(Browser.EXTRA_HEADERS, header);
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        private void setLanguageTag(@NonNull Locale locale) {
            Api21Impl.setLanguageTag(mIntent, locale);
        }

        @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private void setShareIdentityEnabled() {
            if (mActivityOptions == null) {
                mActivityOptions = Api23Impl.makeBasicActivityOptions();
            }
            Api34Impl.setShareIdentityEnabled(mActivityOptions, mShareIdentity);
        }

        @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
        private void setAllowPassThroughOnTouchOutside() {
            if (mActivityOptions == null) {
                mActivityOptions = Api23Impl.makeBasicActivityOptions();
            }
            boolean enabled = isBackgroundInteractionEnabled(mIntent);
            Api36Impl.setAllowPassThroughOnTouchOutside(mActivityOptions, enabled);
        }
    }

    /**
     * Returns whether ephemeral browsing is enabled.
     */
    public boolean isEphemeralBrowsingEnabled() {
        return intent.getBooleanExtra(EXTRA_ENABLE_EPHEMERAL_BROWSING, false);
    }

    /**
     * @return The maximum number of allowed toolbar items for
     * {@link CustomTabsIntent.Builder#addToolbarItem(int, Bitmap, String, PendingIntent)} and
     * {@link CustomTabsIntent#EXTRA_TOOLBAR_ITEMS}.
     */
    public static int getMaxToolbarItems() {
        return MAX_TOOLBAR_ITEMS;
    }

    /**
     * Adds the necessary flags and extras to signal any browser supporting custom tabs to use the
     * browser UI at all times and avoid showing custom tab like UI. Calling this with an intent
     * will override any custom tabs related customizations.
     * @param intent The intent to modify for always showing browser UI.
     * @return The same intent with the necessary flags and extras added.
     */
    public static @NonNull Intent setAlwaysUseBrowserUI(@Nullable Intent intent) {
        if (intent == null) intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_USER_OPT_OUT_FROM_CUSTOM_TABS, true);
        return intent;
    }

    /**
     * Whether a browser receiving the given intent should always use browser UI and avoid using any
     * custom tabs UI.
     *
     * @param intent The intent to check for the required flags and extras.
     * @return Whether the browser UI should be used exclusively.
     */
    public static boolean shouldAlwaysUseBrowserUI(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_USER_OPT_OUT_FROM_CUSTOM_TABS, false)
                && (intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
    }

    /**
     * Retrieves the instance of {@link CustomTabColorSchemeParams} from an Intent for a given
     * color scheme. Uses values passed directly into {@link CustomTabsIntent.Builder} (e.g. via
     * {@link Builder#setToolbarColor}) as defaults.
     *
     * @param intent Intent to retrieve the color scheme parameters from.
     * @param colorScheme A constant representing a color scheme. Should not be
     *                    {@link #COLOR_SCHEME_SYSTEM}.
     * @return An instance of {@link CustomTabColorSchemeParams} with retrieved parameters.
     */
    @SuppressWarnings("deprecation")
    public static @NonNull CustomTabColorSchemeParams getColorSchemeParams(@NonNull Intent intent,
            @ColorScheme int colorScheme) {
        if (colorScheme < 0 || colorScheme > COLOR_SCHEME_MAX
                || colorScheme == COLOR_SCHEME_SYSTEM) {
            throw new IllegalArgumentException("Invalid colorScheme: " + colorScheme);
        }

        Bundle extras = intent.getExtras();
        if (extras == null) {
            return CustomTabColorSchemeParams.fromBundle(null);
        }

        CustomTabColorSchemeParams defaults = CustomTabColorSchemeParams.fromBundle(extras);
        SparseArray<Bundle> paramBundles = extras.getSparseParcelableArray(
                EXTRA_COLOR_SCHEME_PARAMS);
        if (paramBundles != null) {
            Bundle bundleForScheme = paramBundles.get(colorScheme);
            if (bundleForScheme != null) {
                return CustomTabColorSchemeParams.fromBundle(bundleForScheme)
                        .withDefaults(defaults);
            }
        }
        return defaults;
    }

    /**
     * Gets the Custom Tab Activity's resize behavior.
     *
     * @param intent Intent to retrieve the resize behavior from.
     * @return The resize behavior. If {@link CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX}
     *         is not set as part of the same intent, the value has no effect.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR
     * @see CustomTabsIntent#ACTIVITY_HEIGHT_DEFAULT
     * @see CustomTabsIntent#ACTIVITY_HEIGHT_ADJUSTABLE
     * @see CustomTabsIntent#ACTIVITY_HEIGHT_FIXED
     */
    @ActivityHeightResizeBehavior
    public static int getActivityResizeBehavior(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR,
                CustomTabsIntent.ACTIVITY_HEIGHT_DEFAULT);
    }

    /**
     * Gets the Custom Tab Activity's initial height.
     *
     * @param intent Intent to retrieve the initial Custom Tab Activity's height from.
     * @return The initial Custom Tab Activity's height or 0 if it is not set.
     * @see CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX
     */
    @Dimension(unit = PX)
    public static int getInitialActivityHeightPx(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_INITIAL_ACTIVITY_HEIGHT_PX, 0);
    }

    /**
     * Gets the Custom Tab Activity's initial width.
     *
     * @param intent Intent to retrieve the initial Custom Tab Activity's width from.
     * @return The initial Custom Tab Activity's width or 0 if it is not set.
     * @see CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_WIDTH_PX
     */
    @Dimension(unit = PX)
    public static int getInitialActivityWidthPx(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_INITIAL_ACTIVITY_WIDTH_PX, 0);
    }

    /**
     * Gets the breakpoint value in dp that will be used to decide if the Custom Tab will be
     * displayed as a bottom sheet or as a side sheet.
     *
     * @param intent Intent to retrieve the breakpoint value from.
     * @return The breakpoint value or 0 if it is not set.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP
     */
    @Dimension(unit = DP)
    public static int getActivitySideSheetBreakpointDp(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_ACTIVITY_SIDE_SHEET_BREAKPOINT_DP, 0);
    }

    /**
     * Whether the Custom Tab Activity, when acting as a side sheet, can be maximized.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION
     */
    public static boolean isActivitySideSheetMaximizationEnabled(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_ACTIVITY_SIDE_SHEET_ENABLE_MAXIMIZATION, false);
    }

    /**
     * Gets the position where the side sheet should be displayed on the screen.
     *
     * @param intent Intent to retrieve the side sheet position from.
     * @return The position of the side sheet or the default value if it is not set.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_POSITION
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_POSITION_DEFAULT
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_POSITION_START
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_POSITION_END
     */
    @ActivitySideSheetPosition
    public static int getActivitySideSheetPosition(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_ACTIVITY_SIDE_SHEET_POSITION,
                ACTIVITY_SIDE_SHEET_POSITION_DEFAULT);
    }

    /**
     * Gets the type of the decoration that will be used to separate the side sheet from the
     * Custom Tabs embedder.
     *
     * @param intent Intent to retrieve the decoration type from.
     * @return The position of the side sheet or the default value if it is not set.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_NONE
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_SHADOW
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DIVIDER
     */
    @ActivitySideSheetDecorationType
    public static int getActivitySideSheetDecorationType(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE,
                ACTIVITY_SIDE_SHEET_DECORATION_TYPE_DEFAULT);
    }

    /**
     * Gets the type of rounded corners that will be used for the side sheet.
     * @param intent Intent to retrieve the decoration type from.
     * @return The position of the side sheet or the default value if it is not set.
     * @see CustomTabsIntent#EXTRA_ACTIVITY_SIDE_SHEET_DECORATION_TYPE
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_DEFAULT
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_NONE
     * @see CustomTabsIntent#ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION_TOP
     */
    @ActivitySideSheetRoundedCornersPosition
    public static int getActivitySideSheetRoundedCornersPosition(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_ACTIVITY_SIDE_SHEET_ROUNDED_CORNERS_POSITION,
                ACTIVITY_SIDE_SHEET_POSITION_DEFAULT);
    }

    /**
     * Gets the toolbar's top corner radii in dp.
     *
     * @param intent Intent to retrieve the toolbar's top corner radii from.
     * @return The toolbar's top corner radii in dp.
     * @see CustomTabsIntent#EXTRA_TOOLBAR_CORNER_RADIUS_DP
     */
    @Dimension(unit = DP)
    public static int getToolbarCornerRadiusDp(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_TOOLBAR_CORNER_RADIUS_DP, MAX_TOOLBAR_CORNER_RADIUS_DP);
    }

    /**
     * Gets the position of the close button.
     * @param intent Intent to retrieve the position of the close button from.
     * @return The position of the close button, or the default position if the extra is not set.
     * @see CustomTabsIntent#EXTRA_CLOSE_BUTTON_POSITION
     * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_DEFAULT
     * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_START
     * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_END
     */
    @CloseButtonPosition
    public static int getCloseButtonPosition(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_CLOSE_BUTTON_POSITION, CLOSE_BUTTON_POSITION_DEFAULT);
    }

    /**
     * @return The state for the Open in Browser button in the toolbar.
     * @see CustomTabsIntent#EXTRA_OPEN_IN_BROWSER_STATE
     * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_DEFAULT
     * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_ON
     * @see CustomTabsIntent#OPEN_IN_BROWSER_STATE_OFF
     */
    @ExperimentalOpenInBrowser
    @OpenInBrowserState
    public static int getOpenInBrowserButtonState(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_OPEN_IN_BROWSER_STATE, OPEN_IN_BROWSER_STATE_DEFAULT);
    }

    /**
     * @return Whether the bookmarks button is enabled.
     * @see CustomTabsIntent#EXTRA_DISABLE_BOOKMARKS_BUTTON
     */
    public static boolean isBookmarksButtonEnabled(@NonNull Intent intent) {
        return !intent.getBooleanExtra(EXTRA_DISABLE_BOOKMARKS_BUTTON, false);
    }

    /**
     * @return Whether the download button is enabled.
     * @see CustomTabsIntent#EXTRA_DISABLE_DOWNLOAD_BUTTON
     */
    public static boolean isDownloadButtonEnabled(@NonNull Intent intent) {
        return !intent.getBooleanExtra(EXTRA_DISABLE_DOWNLOAD_BUTTON, false);
    }

    /**
     * @return Whether initial urls are to be sent to external handler apps.
     * @see CustomTabsIntent#EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER
     */
    public static boolean isSendToExternalDefaultHandlerEnabled(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER, false);
    }

    /**
     * @return Whether the initial URL sent to the custom tab should be sent to the default
     * handling app for that URL.
     * @see CustomTabsIntent#EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER
     */
    @ExperimentalInitialNavigationCanLeaveBrowser
    public static boolean isInitialNavigationAllowedToLeaveBrowser(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_INITIAL_NAVIGATION_CAN_LEAVE_BROWSER, false);
    }

    /**
     * Gets the target locale for the Translate UI.
     *
     * @return The target locale the Translate UI should be triggered with.
     * @see CustomTabsIntent#EXTRA_TRANSLATE_LANGUAGE_TAG
     */
    public static @Nullable Locale getTranslateLocale(@NonNull Intent intent) {
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = getLocaleForLanguageTag(intent);
        }
        return locale;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static @Nullable Locale getLocaleForLanguageTag(Intent intent) {
        return Api21Impl.getLocaleForLanguageTag(intent);
    }

    /**
     * Gets the target network that the custom tab is currently bound to if any.
     *
     * @return The target {@link Network} is bound to.
     * @see CustomTabsIntent#EXTRA_NETWORK
     */
    public static @Nullable Network getNetwork(@NonNull Intent intent) {
        return IntentCompat.getParcelableExtra(intent, EXTRA_NETWORK, Network.class);
    }

    /**
     * @return Whether the background interaction is enabled.
     * @see CustomTabsIntent#EXTRA_DISABLE_BACKGROUND_INTERACTION
     */
    public static boolean isBackgroundInteractionEnabled(@NonNull Intent intent) {
        return !intent.getBooleanExtra(EXTRA_DISABLE_BACKGROUND_INTERACTION, false);
    }

    /**
     * @return The {@link PendingIntent} that will be sent when the user swipes up
     *     from the secondary toolbar.
     * @see CustomTabsIntent#EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE
     */
    @SuppressWarnings("deprecation")
    public static @Nullable PendingIntent getSecondaryToolbarSwipeUpGesture(
            @NonNull Intent intent) {
        return intent.getParcelableExtra(EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE);
    }

    /**
     * @return Whether the close button is enabled.
     * @see CustomTabsIntent#EXTRA_CLOSE_BUTTON_ENABLED
     */
    public static boolean isCloseButtonEnabled(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_CLOSE_BUTTON_ENABLED, true);
    }

    /**
     * Retrieves the list of {@link CustomContentAction}s configured for the given intent.
     * <p>
     * This method is primarily for inspection or debugging by the client application.
     * The browser implementation directly consumes the {@link #EXTRA_CUSTOM_CONTENT_ACTIONS} extra.
     *
     * @param intent The intent from which to retrieve the custom content actions. Must not be null.
     * @return A {@link List} of {@link CustomContentAction}s. Returns an empty list if
     * no actions were set or if the extra is malformed or missing. The list is unmodifiable.
     */
    @ExperimentalCustomContentAction
    public static @NonNull List<CustomContentAction> getCustomContentActions(
            @NonNull Intent intent) {
        ArrayList<Bundle> bundles = IntentCompat.getParcelableArrayListExtra(
                intent,
                EXTRA_CUSTOM_CONTENT_ACTIONS,
                Bundle.class);
        if (bundles == null) {
            return Collections.emptyList();
        }
        List<CustomContentAction> actions = new ArrayList<>(bundles.size());
        for (Bundle bundle : bundles) {
            CustomContentAction action = CustomContentAction.fromBundle(bundle);
            if (action != null) {
                actions.add(action);
            }
        }
        return Collections.unmodifiableList(actions);
    }

    private static class Api21Impl {
        static void setLanguageTag(Intent intent, Locale locale) {
            intent.putExtra(EXTRA_TRANSLATE_LANGUAGE_TAG, locale.toLanguageTag());
        }

        static @Nullable Locale getLocaleForLanguageTag(Intent intent) {
            String languageTag = intent.getStringExtra(EXTRA_TRANSLATE_LANGUAGE_TAG);
            return languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static class Api23Impl {
        static ActivityOptions makeBasicActivityOptions() {
            return ActivityOptions.makeBasic();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static class Api24Impl {
        static @Nullable String getDefaultLocale() {
            LocaleList defaultLocaleList = LocaleList.getAdjustedDefault();
            return (defaultLocaleList.size() > 0) ? defaultLocaleList.get(0).toLanguageTag(): null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static class Api34Impl {
        static void setShareIdentityEnabled(ActivityOptions activityOptions, boolean enabled) {
            activityOptions.setShareIdentityEnabled(enabled);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.BAKLAVA)
    private static class Api36Impl {
        static void setAllowPassThroughOnTouchOutside(
                ActivityOptions activityOptions, boolean enabled) {
            activityOptions.setAllowPassThroughOnTouchOutside(enabled);
        }
    }
}
