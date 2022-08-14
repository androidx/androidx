/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.browser.browseractions;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class holding the {@link Intent} and start bundle for a Browser Actions Activity.
 *
 * <p>
 * <strong>Note:</strong> The constants below are public for the browser implementation's benefit.
 * You are strongly encouraged to use {@link BrowserActionsIntent.Builder}.
 *
 * @deprecated Browser Actions are deprecated as of release 1.2.0.
 */
@SuppressWarnings("deprecation")
@Deprecated
public class BrowserActionsIntent {
    private static final String TAG = "BrowserActions";
    // Used to verify that an URL intent handler exists.
    private static final String TEST_URL = "https://www.example.com";

    /**
     * Extra that specifies {@link PendingIntent} indicating which Application sends the {@link
     * BrowserActionsIntent}.
     * <p>
     * <strong>Note:</strong> The PendingIntent is self-reported and untrusted, sending application
     * can modify it to use PendingIntent from other apps. This would return the package name from
     * the app who creates the PendintIntent.
     */
    public static final String EXTRA_APP_ID = "androidx.browser.browseractions.APP_ID";

    /**
     * Indicates that the user explicitly opted out of Browser Actions in the calling application.
     */
    public static final String ACTION_BROWSER_ACTIONS_OPEN =
            "androidx.browser.browseractions.browser_action_open";

    /**
     * Extra resource id that specifies the icon of a custom item shown in the Browser Actions menu.
     */
    public static final String KEY_ICON_ID = "androidx.browser.browseractions.ICON_ID";

    /**
     * Extra {@link Uri} that specifies location of file for the icon of a custom item shown in the
     * Browser Actions menu.
     */
    private static final String KEY_ICON_URI = "androidx.browser.browseractions.ICON_URI";

    /**
     * Extra string that specifies the title of a custom item shown in the Browser Actions menu.
     */
    public static final String KEY_TITLE = "androidx.browser.browseractions.TITLE";

    /**
     * Extra PendingIntent to be launched when a custom item is selected in the Browser Actions
     * menu.
     */
    public static final String KEY_ACTION = "androidx.browser.browseractions.ACTION";

    /**
     * Extra that specifies the type of url for the Browser Actions menu.
     */
    public static final String EXTRA_TYPE = "androidx.browser.browseractions.extra.TYPE";

    /**
     * Extra that specifies List<Bundle> used for adding custom items to the Browser Actions menu.
     */
    public static final String EXTRA_MENU_ITEMS =
            "androidx.browser.browseractions.extra.MENU_ITEMS";

    /**
     * Extra that specifies the PendingIntent to be launched when a browser specified menu item is
     * selected. The id of the chosen item will be notified through the data of its Intent.
     */
    public static final String EXTRA_SELECTED_ACTION_PENDING_INTENT =
            "androidx.browser.browseractions.extra.SELECTED_ACTION_PENDING_INTENT";

    /**
     * The maximum allowed number of custom items.
     */
    @SuppressLint("MinMaxConstant")
    public static final int MAX_CUSTOM_ITEMS = 5;

    /**
     * Defines the types of url for Browser Actions menu.
     */
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({URL_TYPE_NONE, URL_TYPE_IMAGE, URL_TYPE_VIDEO, URL_TYPE_AUDIO, URL_TYPE_FILE,
            URL_TYPE_PLUGIN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BrowserActionsUrlType {}
    public static final int URL_TYPE_NONE = 0;
    public static final int URL_TYPE_IMAGE = 1;
    public static final int URL_TYPE_VIDEO = 2;
    public static final int URL_TYPE_AUDIO = 3;
    public static final int URL_TYPE_FILE = 4;
    public static final int URL_TYPE_PLUGIN = 5;

    /**
     * Defines the the ids of the browser specified menu items in Browser Actions.
     * TODO(ltian): A long term solution need, since other providers might have customized menus.
     */
    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef({ITEM_INVALID_ITEM, ITEM_OPEN_IN_NEW_TAB, ITEM_OPEN_IN_INCOGNITO, ITEM_DOWNLOAD,
            ITEM_COPY, ITEM_SHARE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BrowserActionsItemId {}
    public static final int ITEM_INVALID_ITEM = -1;
    public static final int ITEM_OPEN_IN_NEW_TAB = 0;
    public static final int ITEM_OPEN_IN_INCOGNITO = 1;
    public static final int ITEM_DOWNLOAD = 2;
    public static final int ITEM_COPY = 3;
    public static final int ITEM_SHARE = 4;

    /**
     * An {@link Intent} used to start the Browser Actions Activity.
     */
    @NonNull private final Intent mIntent;

    /**
     * Gets the Intent of {@link BrowserActionsIntent}.
     * @return the Intent of {@link BrowserActionsIntent}.
     */
    @NonNull public Intent getIntent() {
        return mIntent;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    BrowserActionsIntent(@NonNull Intent intent) {
        this.mIntent = intent;
    }

    /** @hide */
    @VisibleForTesting
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    interface BrowserActionsFallDialogListener {
        void onDialogShown();
    }

    @Nullable
    private static BrowserActionsFallDialogListener sDialogListenter;

    /**
     * Builder class for opening a Browser Actions context menu.
     */
    public static final class Builder {
        private final Intent mIntent = new Intent(BrowserActionsIntent.ACTION_BROWSER_ACTIONS_OPEN);
        private Context mContext;
        private Uri mUri;
        @BrowserActionsUrlType
        private int mType = URL_TYPE_NONE;
        private ArrayList<Bundle> mMenuItems = new ArrayList<>();
        @Nullable private PendingIntent mOnItemSelectedPendingIntent = null;
        private List<Uri> mImageUris = new ArrayList<>();

        /**
         * Constructs a {@link BrowserActionsIntent.Builder} object associated with default setting
         * for a selected url.
         * @param context The context requesting the Browser Actions context menu.
         * @param uri The selected url for Browser Actions menu.
         */
        public Builder(@NonNull Context context, @NonNull Uri uri) {
            mContext = context;
            mUri = uri;
        }

        /**
         * Sets the type of Browser Actions context menu.
         * @param type The type of url.
         */
        @NonNull
        public Builder setUrlType(@BrowserActionsUrlType int type) {
            mType = type;
            return this;
        }

        /**
         * Sets the custom items list.
         * Only maximum MAX_CUSTOM_ITEMS custom items are allowed,
         * otherwise throws an {@link IllegalStateException}.
         * @param items The list of {@link BrowserActionItem} for custom items.
         */
        @NonNull
        public Builder setCustomItems(@NonNull ArrayList<BrowserActionItem> items) {
            if (items.size() > MAX_CUSTOM_ITEMS) {
                throw new IllegalStateException(
                        "Exceeded maximum toolbar item count of " + MAX_CUSTOM_ITEMS);
            }
            for (int i = 0; i < items.size(); i++) {
                if (TextUtils.isEmpty(items.get(i).getTitle())
                        || items.get(i).getAction() == null) {
                    throw new IllegalArgumentException(
                            "Custom item should contain a non-empty title and non-null intent.");
                } else {
                    mMenuItems.add(getBundleFromItem(items.get(i)));
                    if (items.get(i).getIconUri() != null) {
                        mImageUris.add(items.get(i).getIconUri());
                    }
                }
            }
            return this;
        }

        /**
         * Sets the custom items list.
         * Only maximum MAX_CUSTOM_ITEMS custom items are allowed,
         * otherwise throws an {@link IllegalStateException}.
         * @param items The varargs of {@link BrowserActionItem} for custom items.
         */
        @NonNull
        public Builder setCustomItems(@NonNull BrowserActionItem... items) {
            return setCustomItems(new ArrayList<BrowserActionItem>(Arrays.asList(items)));
        }

        /**
         * Set the PendingIntent to be launched when a a browser specified menu item is selected.
         * @param onItemSelectedPendingIntent The PendingIntent to be launched.
         */
        @NonNull
        public Builder setOnItemSelectedAction(@NonNull PendingIntent onItemSelectedPendingIntent) {
            mOnItemSelectedPendingIntent = onItemSelectedPendingIntent;
            return this;
        }

        /**
         * Populates a {@link Bundle} to hold a custom item for Browser Actions menu.
         * @param item A custom item for Browser Actions menu.
         * @return The Bundle of custom item.
         */
        @NonNull
        private Bundle getBundleFromItem(@NonNull BrowserActionItem item) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_TITLE, item.getTitle());
            bundle.putParcelable(KEY_ACTION, item.getAction());
            if (item.getIconId() != 0) bundle.putInt(KEY_ICON_ID, item.getIconId());
            if (item.getIconUri() != null) bundle.putParcelable(KEY_ICON_URI, item.getIconUri());
            return bundle;
        }

        /**
         * Combines all the options that have been set and returns a new {@link
         * BrowserActionsIntent} object.
         */
        @NonNull
        public BrowserActionsIntent build() {
            mIntent.setData(mUri);
            mIntent.putExtra(EXTRA_TYPE, mType);
            mIntent.putParcelableArrayListExtra(EXTRA_MENU_ITEMS, mMenuItems);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    mContext, 0, new Intent(), PendingIntent.FLAG_IMMUTABLE);
            mIntent.putExtra(EXTRA_APP_ID, pendingIntent);
            if (mOnItemSelectedPendingIntent != null) {
                mIntent.putExtra(
                        EXTRA_SELECTED_ACTION_PENDING_INTENT, mOnItemSelectedPendingIntent);
            }
            BrowserServiceFileProvider.grantReadPermission(mIntent, mImageUris, mContext);
            return new BrowserActionsIntent(mIntent);
        }
    }

    /**
     * Construct a BrowserActionsIntent with default settings and launch it to open a Browser
     * Actions menu.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     */
    public static void openBrowserAction(@NonNull Context context, @NonNull Uri uri) {
        BrowserActionsIntent intent = new BrowserActionsIntent.Builder(context, uri).build();
        launchIntent(context, intent.getIntent());
    }

    /**
     * Construct a BrowserActionsIntent with custom settings and launch it to open a Browser Actions
     * menu.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     * @param type The type of the url for context menu to be opened.
     * @param items List of custom items to be added to Browser Actions menu.
     * @param pendingIntent The PendingIntent to be launched when a browser specified menu item is
     * selected.
     */
    public static void openBrowserAction(@NonNull Context context, @NonNull Uri uri, int type,
            @NonNull ArrayList<BrowserActionItem> items, @NonNull PendingIntent pendingIntent) {
        BrowserActionsIntent intent = new BrowserActionsIntent.Builder(context, uri)
                .setUrlType(type)
                .setCustomItems(items)
                .setOnItemSelectedAction(pendingIntent)
                .build();
        launchIntent(context, intent.getIntent());
    }

    /**
     * Launch an Intent to open a Browser Actions menu.
     * It first checks if any Browser Actions provider is available to create the menu.
     * If the default Browser supports Browser Actions, menu will be opened by the default Browser,
     * otherwise show a intent picker.
     * If not provider, a Browser Actions menu is opened locally from support library.
     * @param context The context requesting for a Browser Actions menu.
     * @param intent The {@link Intent} holds the setting for Browser Actions menu.
     */
    public static void launchIntent(@NonNull Context context, @NonNull Intent intent) {
        List<ResolveInfo> handlers = getBrowserActionsIntentHandlers(context);
        launchIntent(context, intent, handlers);
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    @SuppressWarnings("deprecation")
    static void launchIntent(Context context, Intent intent, List<ResolveInfo> handlers) {
        if (handlers == null || handlers.size() == 0) {
            openFallbackBrowserActionsMenu(context, intent);
            return;
        } else if (handlers.size() == 1) {
            intent.setPackage(handlers.get(0).activityInfo.packageName);
        } else {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(TEST_URL));
            PackageManager pm = context.getPackageManager();
            ResolveInfo defaultHandler =
                    pm.resolveActivity(viewIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (defaultHandler != null) {
                String defaultPackageName = defaultHandler.activityInfo.packageName;
                for (int i = 0; i < handlers.size(); i++) {
                    if (defaultPackageName.equals(handlers.get(i).activityInfo.packageName)) {
                        intent.setPackage(defaultPackageName);
                        break;
                    }
                }
            }
        }
        ContextCompat.startActivity(context, intent, null);
    }

    /**
     * Returns a list of Browser Actions providers available to handle the {@link
     * BrowserActionsIntent}.
     * @param context The context requesting for a Browser Actions menu.
     * @return List of Browser Actions providers available to handle the intent.
     * @hide
     */
    @RestrictTo(LIBRARY)
    @SuppressWarnings("deprecation")
    @NonNull
    public static List<ResolveInfo> getBrowserActionsIntentHandlers(@NonNull Context context) {
        Intent intent =
                new Intent(BrowserActionsIntent.ACTION_BROWSER_ACTIONS_OPEN, Uri.parse(TEST_URL));
        PackageManager pm = context.getPackageManager();
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
    }

    @SuppressWarnings({"NullAway", "deprecation"}) // TODO: b/141869398
    private static void openFallbackBrowserActionsMenu(Context context, Intent intent) {
        Uri uri = intent.getData();
        ArrayList<Bundle> bundles = intent.getParcelableArrayListExtra(EXTRA_MENU_ITEMS);
        List<BrowserActionItem> items = bundles != null ? parseBrowserActionItems(bundles) : null;
        openFallbackBrowserActionsMenu(context, uri, items);
    }

    /** @hide */
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @VisibleForTesting
    static void setDialogShownListenter(BrowserActionsFallDialogListener dialogListener) {
        sDialogListenter = dialogListener;
    }

    /**
     * Open a Browser Actions menu from support library.
     * @param context The context requesting for a Browser Actions menu.
     * @param uri The url for Browser Actions menu.
     * @param menuItems List of {@link BrowserActionItem} to add to the fallback menu.
     */
    private static void openFallbackBrowserActionsMenu(
            Context context, Uri uri, List<BrowserActionItem> menuItems) {
        BrowserActionsFallbackMenuUi menuUi =
                new BrowserActionsFallbackMenuUi(context, uri, menuItems);
        menuUi.displayMenu();
        if (sDialogListenter != null) {
            sDialogListenter.onDialogShown();
        }
    }

    /**
     * Gets custom item list for browser action menu.
     * @param bundles Data for custom items from {@link BrowserActionsIntent}.
     * @return List of {@link BrowserActionItem}
     */
    @NonNull
    @SuppressWarnings("deprecation")
    public static List<BrowserActionItem> parseBrowserActionItems(
            @NonNull ArrayList<Bundle> bundles) {
        List<BrowserActionItem> mActions = new ArrayList<>();
        for (int i = 0; i < bundles.size(); i++) {
            Bundle bundle = bundles.get(i);
            String title = bundle.getString(KEY_TITLE);
            PendingIntent action = bundle.getParcelable(KEY_ACTION);
            @DrawableRes
            int iconId = bundle.getInt(KEY_ICON_ID);
            Uri iconUri = bundle.getParcelable(KEY_ICON_URI);
            if (!TextUtils.isEmpty(title) && action != null) {
                BrowserActionItem item;
                if (iconId != 0) {
                    item = new BrowserActionItem(title, action, iconId);
                } else {
                    item = new BrowserActionItem(title, action, iconUri);
                }
                mActions.add(item);
            } else {
                throw new IllegalArgumentException(
                        "Custom item should contain a non-empty title and non-null intent.");
            }
        }
        return mActions;
    }

    /**
     * Get the package name of the creator application.
     * <p>
     * <strong> Note:</strong> This is self-reported and could be untrusted. Intent sender can
     * modify it to return the package name from a different application.
     *
     * @param intent The {@link BrowserActionsIntent}.
     * @return The creator package name.
     */
    @SuppressWarnings("deprecation")
    @Nullable
    public static String getUntrustedCreatorPackageName(@NonNull Intent intent) {
        PendingIntent pendingIntent = intent.getParcelableExtra(BrowserActionsIntent.EXTRA_APP_ID);
        if (pendingIntent != null) {
            return pendingIntent.getTargetPackage();
        }
        return null;
    }

    /**
     * @deprecated This return value of this method cannot be trusted, it is kept around for
     *             compatibility. Use {@link #getUntrustedCreatorPackageName}, or an alternative
     *             method if you rely on the return value.
     */
    @Deprecated
    @Nullable
    public static String getCreatorPackageName(@NonNull Intent intent) {
        return getUntrustedCreatorPackageName(intent);
    }
}
