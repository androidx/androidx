/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.customtabs;

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
import android.support.annotation.AnimRes;
import android.support.annotation.NonNull;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Class holding the {@link Intent} and start bundle for a Custom Tabs Activity.
 *
 * <p>
 * <strong>Note:</strong> The constants below are public for the browser implementation's benefit.
 * You are strongly encouraged to use {@link CustomTabsIntent.Builder}.</p>
 */
public final class CustomTabsIntent {

    /**
     * Extra used to match the session. This has to be included in the intent to open in
     * a custom tab. This is the same IBinder that gets passed to ICustomTabsService#newSession.
     * Null if there is no need to match any service side sessions with the intent.
     */
    public static final String EXTRA_SESSION = "android.support.customtabs.extra.SESSION";

    /**
     * Extra that changes the background color for the toolbar. colorRes is an int that specifies a
     * {@link Color}, not a resource id.
     */
    public static final String EXTRA_TOOLBAR_COLOR =
            "android.support.customtabs.extra.TOOLBAR_COLOR";

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
     * Bundle constructed out of {@link ActivityOptions} that will be running when the
     * {@link Activity} that holds the custom tab gets finished. A similar ActivityOptions
     * for creation should be constructed and given to the startActivity() call that
     * launches the custom tab.
     */
    public static final String EXTRA_EXIT_ANIMATION_BUNDLE =
            "android.support.customtabs.extra.EXIT_ANIMATION_BUNDLE";

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
     * @param context The source Activity.
     * @param url The URL to load in the Custom Tab.
     */
    public void launchUrl(Activity context, Uri url) {
        intent.setData(url);
        if (startAnimationBundle != null){
            context.startActivity(intent, startAnimationBundle);
        } else {
            context.startActivity(intent);
        }
    }

    private CustomTabsIntent(Intent intent, Bundle startAnimationBundle) {
        this.intent = intent;
        this.startAnimationBundle = startAnimationBundle;
    }

    /**
     * Builder class for {@link CustomTabsIntent} objects.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(Intent.ACTION_VIEW);
        private ArrayList<Bundle> mMenuItems = null;
        private Bundle mStartAnimationBundle = null;

        /**
         * Creates a {@link CustomTabsIntent.Builder} object associated with no
         * {@link CustomTabsSession}.
         */
        public Builder() {
            this(null);
        }

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
            if (session != null) mIntent.setPackage(session.getComponentName().getPackageName());
            Bundle bundle = new Bundle();
            safePutBinder(bundle, EXTRA_SESSION, session == null ? null : session.getBinder());
            mIntent.putExtras(bundle);
        }

        /**
         * Sets the toolbar color.
         *
         * @param color {@link Color}
         */
        public Builder setToolbarColor(@ColorInt int color) {
            mIntent.putExtra(EXTRA_TOOLBAR_COLOR, color);
            return this;
        }

        /**
         * Sets the Close button icon for the custom tab.
         *
         * @param icon The icon {@link Bitmap}
         */
        public Builder setCloseButtonIcon(@NonNull Bitmap icon) {
            mIntent.putExtra(EXTRA_CLOSE_BUTTON_ICON, icon);
            return this;
        }

        /**
         * Sets whether the title should be shown in the custom tab.
         *
         * @param showTitle Whether the title should be shown.
         */
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
        public Builder addMenuItem(@NonNull String label, @NonNull PendingIntent pendingIntent) {
            if (mMenuItems == null) mMenuItems = new ArrayList<>();
            Bundle bundle = new Bundle();
            bundle.putString(KEY_MENU_ITEM_TITLE, label);
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent);
            mMenuItems.add(bundle);
            return this;
        }

        /**
         * Set the action button.
         *
         * @param icon The icon.
         * @param description The description for the button. To be used for accessibility.
         * @param pendingIntent pending intent delivered when the button is clicked.
         */
        public Builder setActionButton(@NonNull Bitmap icon,
                @NonNull String description, @NonNull PendingIntent pendingIntent) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(KEY_ICON, icon);
            bundle.putString(KEY_DESCRIPTION, description);
            bundle.putParcelable(KEY_PENDING_INTENT, pendingIntent);
            mIntent.putExtra(EXTRA_ACTION_BUTTON_BUNDLE, bundle);
            return this;
        }

        /**
         * Sets the start animations,
         *
         * @param context Application context.
         * @param enterResId Resource ID of the "enter" animation for the browser.
         * @param exitResId Resource ID of the "exit" animation for the application.
         */
        public Builder setStartAnimations(
                @NonNull Context context, @AnimRes int enterResId, @AnimRes int exitResId) {
            mStartAnimationBundle =
                    ActivityOptions.makeCustomAnimation(context, enterResId, exitResId).toBundle();
            return this;
        }

        /**
         * Sets the exit animations,
         *
         * @param context Application context.
         * @param enterResId Resource ID of the "enter" animation for the application.
         * @param exitResId Resource ID of the "exit" animation for the browser.
         */
        public Builder setExitAnimations(
                @NonNull Context context, @AnimRes int enterResId, @AnimRes int exitResId) {
            Bundle bundle =
                    ActivityOptions.makeCustomAnimation(context, enterResId, exitResId).toBundle();
            mIntent.putExtra(EXTRA_EXIT_ANIMATION_BUNDLE, bundle);
            return this;
        }

        /**
         * Combines all the options that have been set and returns a new {@link CustomTabsIntent}
         * object.
         */
        public CustomTabsIntent build() {
            if (mMenuItems != null) {
                mIntent.putParcelableArrayListExtra(CustomTabsIntent.EXTRA_MENU_ITEMS, mMenuItems);
            }
            return new CustomTabsIntent(mIntent, mStartAnimationBundle);
        }

        /**
         * A convenience method to handle putting an {@link IBinder} inside a {@link Bundle} for all
         * Android version.
         * @param bundle The bundle to insert the {@link IBinder}.
         * @param key    The key to use while putting the {@link IBinder}.
         * @param binder The {@link IBinder} to put.
         * @return       Whether the operation was successful.
         */
        private boolean safePutBinder(Bundle bundle, String key, IBinder binder) {
            try {
                // {@link Bundle#putBinder} exists since JB MR2, but we have
                // {@link Bundle#putIBinder} which is the same method since the dawn of time. Use
                // reflection when necessary to call it.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    bundle.putBinder(key, binder);
                } else {
                    Method putBinderMethod =
                            Bundle.class.getMethod("putIBinder", String.class, IBinder.class);
                    putBinderMethod.invoke(bundle, key, binder);
                }
            } catch (InvocationTargetException | IllegalAccessException
                    | IllegalArgumentException | NoSuchMethodException e) {
                return false;
            }
            return true;
        }
    }
}
