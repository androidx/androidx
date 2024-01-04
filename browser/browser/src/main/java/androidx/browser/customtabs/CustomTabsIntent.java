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
import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.BundleCompat;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
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
     * Extra to favor sending initial urls to external handler apps, if possible.
     *
     * A Custom Tab Intent from a Custom Tab session will always have the package set,
     * so the Intent will always be to the browser. This extra can be used to allow
     * the initial Intent navigation chain to leave the browser.
     */
    public static final String EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER =
            "android.support.customtabs.extra.SEND_TO_EXTERNAL_HANDLER";

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
     * Extra that enables the client to add an additional action button to the toolbar.
     * If the bitmap icon does not fit on the toolbar then the action button will be
     * added to the secondary toolbar.
     */
    public static final String EXTRA_SHOW_ON_TOOLBAR =
            "android.support.customtabs.customaction.SHOW_ON_TOOLBAR";

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
     * Key that specifies the unique ID for an action button. To make a button to show on the
     * toolbar, use {@link #TOOLBAR_ACTION_BUTTON_ID} as its ID.
     */
    public static final String KEY_ID = "android.support.customtabs.customaction.ID";

    /**
     * The ID allocated to the custom action button that is shown on the toolbar.
     */
    public static final int TOOLBAR_ACTION_BUTTON_ID = 0;

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
    @NonNull public final Intent intent;

    /**
     * A {@link Bundle} containing the start animation for the Custom Tabs Activity.
     */
    @Nullable public final Bundle startAnimationBundle;

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
        @Nullable private ArrayList<Bundle> mMenuItems;
        @Nullable private ActivityOptions mActivityOptions;
        @Nullable private ArrayList<Bundle> mActionButtons;
        @Nullable private SparseArray<Bundle> mColorSchemeParamBundles;
        @Nullable private Bundle mDefaultColorSchemeBundle;
        @ShareState private int mShareState = SHARE_STATE_DEFAULT;
        private boolean mInstantAppsEnabled = true;
        private boolean mShareIdentity;

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
        @NonNull
        public Builder setSession(@NonNull CustomTabsSession session) {
            mIntent.setPackage(session.getComponentName().getPackageName());
            setSessionParameters(session.getBinder(), session.getId());
            return this;
        }

        /**
         * Associates the {@link Intent} with the given {@link CustomTabsSession.PendingSession}.
         * Overrides the effect of {@link #setSession}.
         *
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        public Builder setPendingSession(@NonNull CustomTabsSession.PendingSession session) {
            setSessionParameters(null, session.getId());
            return this;
        }

        private void setSessionParameters(@Nullable IBinder binder,
                @Nullable PendingIntent sessionId) {
            Bundle bundle = new Bundle();
            BundleCompat.putBinder(bundle, EXTRA_SESSION, binder);
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
        @NonNull
        public Builder setToolbarColor(@ColorInt int color) {
            mDefaultColorSchemeBuilder.setToolbarColor(color);
            return this;
        }

        /**
         * Enables the url bar to hide as the user scrolls down on the page.
         * @deprecated Use {@link #setUrlBarHidingEnabled(boolean)} instead.
         */
        @Deprecated
        @NonNull
        public Builder enableUrlBarHiding() {
            mIntent.putExtra(EXTRA_ENABLE_URLBAR_HIDING, true);
            return this;
        }

        /**
         * Set whether the url bar should hide as the user scrolls down on the page.
         *
         * @param enabled Whether url bar hiding is enabled.
         */
        @NonNull
        public Builder setUrlBarHidingEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_ENABLE_URLBAR_HIDING, enabled);
            return this;
        }

        /**
         * Sets the Close button icon for the custom tab.
         *
         * @param icon The icon {@link Bitmap}
         */
        @NonNull
        public Builder setCloseButtonIcon(@NonNull Bitmap icon) {
            mIntent.putExtra(EXTRA_CLOSE_BUTTON_ICON, icon);
            return this;
        }

        /**
         * Sets whether the title should be shown in the custom tab.
         *
         * @param showTitle Whether the title should be shown.
         */
        @NonNull
        public Builder setShowTitle(boolean showTitle) {
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
        @NonNull
        public Builder addMenuItem(@NonNull String label, @NonNull PendingIntent pendingIntent) {
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
        @NonNull
        public Builder addDefaultShareMenuItem() {
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
        @NonNull
        public Builder setDefaultShareMenuItemEnabled(boolean enabled) {
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
        @NonNull
        public Builder setShareState(@ShareState int shareState) {
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
        @NonNull
        public Builder setActionButton(@NonNull Bitmap icon, @NonNull String description,
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
        @NonNull
        public Builder setActionButton(@NonNull Bitmap icon, @NonNull String description,
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
        @NonNull
        public Builder addToolbarItem(int id, @NonNull Bitmap icon, @NonNull String description,
                @NonNull PendingIntent pendingIntent) throws IllegalStateException {
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
        @NonNull
        public Builder setSecondaryToolbarColor(@ColorInt int color) {
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
        @NonNull
        public Builder setNavigationBarColor(@ColorInt int color) {
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
        @NonNull
        public Builder setNavigationBarDividerColor(@ColorInt int color) {
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
        @NonNull
        public Builder setSecondaryToolbarViews(@NonNull RemoteViews remoteViews,
                @Nullable int[] clickableIDs, @Nullable PendingIntent pendingIntent) {
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
        @NonNull
        public Builder setSecondaryToolbarSwipeUpGesture(@Nullable PendingIntent pendingIntent) {
            mIntent.putExtra(EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE, pendingIntent);
            return this;
        }

        /**
         * Sets whether Instant Apps is enabled for this Custom Tab.

         * @param enabled Whether Instant Apps should be enabled.
         */
        @NonNull
        public Builder setInstantAppsEnabled(boolean enabled) {
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
        @NonNull
        @SuppressWarnings("NullAway") // TODO: b/141869399
        public Builder setStartAnimations(
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
        @NonNull
        public Builder setExitAnimations(
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
        @NonNull
        public Builder setColorScheme(@ColorScheme int colorScheme) {
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
        @NonNull
        public Builder setColorSchemeParams(@ColorScheme int colorScheme,
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
        @NonNull
        public Builder setDefaultColorSchemeParams(@NonNull CustomTabColorSchemeParams params) {
            mDefaultColorSchemeBundle = params.toBundle();
            return this;
        }

        /**
         * Sets the Custom Tab Activity's initial height in pixels and the desired resize behavior.
         * The Custom Tab will behave as a bottom sheet.
         *
         * @param initialHeightPx The Custom Tab Activity's initial height in pixels.
         * @param activityHeightResizeBehavior Desired height behavior.
         * @see CustomTabsIntent#EXTRA_INITIAL_ACTIVITY_HEIGHT_PX
         * @see CustomTabsIntent#EXTRA_ACTIVITY_HEIGHT_RESIZE_BEHAVIOR
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_DEFAULT
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_ADJUSTABLE
         * @see CustomTabsIntent#ACTIVITY_HEIGHT_FIXED
         */
        @NonNull
        public Builder setInitialActivityHeightPx(@Dimension(unit = PX) int initialHeightPx,
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
         * The Custom Tab will behave as a bottom sheet.
         *
         * @see CustomTabsIntent.Builder#setInitialActivityHeightPx(int, int)
         */
        @NonNull
        public Builder setInitialActivityHeightPx(@Dimension(unit = PX) int initialHeightPx) {
            return setInitialActivityHeightPx(initialHeightPx, ACTIVITY_HEIGHT_DEFAULT);
        }

        /**
         * Sets the toolbar's top corner radii in dp.
         *
         * @param cornerRadiusDp The toolbar's top corner radii in dp.
         * @see CustomTabsIntent#EXTRA_TOOLBAR_CORNER_RADIUS_DP
         */
        @NonNull
        public Builder setToolbarCornerRadiusDp(@Dimension(unit = DP) int cornerRadiusDp) {
            if (cornerRadiusDp < 0 || cornerRadiusDp > MAX_TOOLBAR_CORNER_RADIUS_DP) {
                throw new IllegalArgumentException("Invalid value for the cornerRadiusDp argument");
            }

            mIntent.putExtra(EXTRA_TOOLBAR_CORNER_RADIUS_DP, cornerRadiusDp);
            return this;
        }

        /**
         * Sets the position of the close button.
         *
         * @param position The desired position.
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_DEFAULT
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_START
         * @see CustomTabsIntent#CLOSE_BUTTON_POSITION_END
         */
        @NonNull
        public Builder setCloseButtonPosition(@CloseButtonPosition int position) {
            if (position < 0 || position > CLOSE_BUTTON_POSITION_MAX) {
                throw new IllegalArgumentException("Invalid value for the position argument");
            }

            mIntent.putExtra(EXTRA_CLOSE_BUTTON_POSITION, position);
            return this;
        }

        /**
         * Enables or disables the bookmarks button in the overflow menu. The button
         * is enabled by default.
         *
         * @param enabled Whether the start button is enabled.
         * @see CustomTabsIntent#EXTRA_DISABLE_BOOKMARKS_BUTTON
         */
        @NonNull
        public Builder setBookmarksButtonEnabled(boolean enabled) {
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
        @NonNull
        public Builder setDownloadButtonEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_DISABLE_DOWNLOAD_BUTTON, !enabled);
            return this;
        }

        /**
         * Enables sending initial urls to external handler apps, if possible.
         *
         * @param enabled Whether to send urls to external handler.
         * @see CustomTabsIntent#EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER
         */
        @NonNull
        public Builder setSendToExternalDefaultHandlerEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_SEND_TO_EXTERNAL_DEFAULT_HANDLER, enabled);
            return this;
        }

        /**
         * Specifies the target locale the Translate UI should be triggered with.
         *
         * @param locale {@link Locale} object that represents the target locale.
         * @see CustomTabsIntent#EXTRA_TRANSLATE_LANGUAGE_TAG
         */
        @NonNull
        public Builder setTranslateLocale(@NonNull Locale locale) {
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
        @NonNull
        public Builder setBackgroundInteractionEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_DISABLE_BACKGROUND_INTERACTION, !enabled);
            return this;
        }

        /**
         * Enables the client to add an additional action button to the toolbar. If the bitmap
         * icon does not fit on the toolbar then the action button will be added to the secondary
         * toolbar.
         *
         * @param enabled Whether the additional actions can be added to the toolbar.
         * @see CustomTabsIntent#EXTRA_SHOW_ON_TOOLBAR
         */
        @NonNull
        public Builder setShowOnToolbarEnabled(boolean enabled) {
            mIntent.putExtra(EXTRA_SHOW_ON_TOOLBAR, enabled);
            return this;
        }

        /**
         * Allow Custom Tabs to obtain the caller's identity i.e. package name.
         * @param enabled Whether the identity sharing is enabled.
         */
        @NonNull
        public Builder setShareIdentityEnabled(boolean enabled) {
            mShareIdentity = enabled;
            return this;
        }

        /**
         * Combines all the options that have been set and returns a new {@link CustomTabsIntent}
         * object.
         */
        @NonNull
        public CustomTabsIntent build() {
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setCurrentLocaleAsDefaultAcceptLanguage();
            }

            Bundle bundle = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                setShareIdentityEnabled();
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
    @NonNull
    public static Intent setAlwaysUseBrowserUI(@Nullable Intent intent) {
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
    @NonNull
    @SuppressWarnings("deprecation")
    public static CustomTabColorSchemeParams getColorSchemeParams(@NonNull Intent intent,
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
     * Gets the target locale for the Translate UI.
     *
     * @return The target locale the Translate UI should be triggered with.
     * @see CustomTabsIntent#EXTRA_TRANSLATE_LANGUAGE_TAG
     */
    @Nullable
    public static Locale getTranslateLocale(@NonNull Intent intent) {
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = getLocaleForLanguageTag(intent);
        }
        return locale;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Nullable
    private static Locale getLocaleForLanguageTag(Intent intent) {
        return Api21Impl.getLocaleForLanguageTag(intent);
    }

    /**
     * @return Whether the background interaction is enabled.
     * @see CustomTabsIntent#EXTRA_DISABLE_BACKGROUND_INTERACTION
     */
    public static boolean isBackgroundInteractionEnabled(@NonNull Intent intent) {
        return !intent.getBooleanExtra(EXTRA_DISABLE_BACKGROUND_INTERACTION, false);
    }

    /**
     * @return Whether the additional actions can be added to the toolbar.
     * @see CustomTabsIntent#EXTRA_SHOW_ON_TOOLBAR
     */
    public static boolean isShowOnToolbarEnabled(@NonNull Intent intent) {
        return intent.getBooleanExtra(EXTRA_SHOW_ON_TOOLBAR, false);
    }

    /**
     * @return The {@link PendingIntent} that will be sent when the user swipes up
     *     from the secondary toolbar.
     * @see CustomTabsIntent#EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static PendingIntent getSecondaryToolbarSwipeUpGesture(@NonNull Intent intent) {
        return intent.getParcelableExtra(EXTRA_SECONDARY_TOOLBAR_SWIPE_UP_GESTURE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class Api21Impl {
        @DoNotInline
        static void setLanguageTag(Intent intent, Locale locale) {
            intent.putExtra(EXTRA_TRANSLATE_LANGUAGE_TAG, locale.toLanguageTag());
        }

        @DoNotInline
        @Nullable
        static Locale getLocaleForLanguageTag(Intent intent) {
            String languageTag = intent.getStringExtra(EXTRA_TRANSLATE_LANGUAGE_TAG);
            return languageTag != null ? Locale.forLanguageTag(languageTag) : null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static class Api23Impl {
        @DoNotInline
        static ActivityOptions makeBasicActivityOptions() {
            return ActivityOptions.makeBasic();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static class Api24Impl {
        @DoNotInline
        @Nullable
        static String getDefaultLocale() {
            LocaleList defaultLocaleList = LocaleList.getAdjustedDefault();
            return (defaultLocaleList.size() > 0) ? defaultLocaleList.get(0).toLanguageTag(): null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static class Api34Impl {
        @DoNotInline
        static void setShareIdentityEnabled(ActivityOptions activityOptions, boolean enabled) {
            activityOptions.setShareIdentityEnabled(enabled);
        }
    }
}
