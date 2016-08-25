/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v7.widget;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.view.ActionProvider;
import android.support.v7.appcompat.R;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.ActivityChooserModel.OnChooseActivityListener;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import android.view.View;

/**
 * Provides a share action, which is suitable for an activity's app bar. Creates
 * views that enable data sharing. If the provider appears in the
 * overflow menu, it creates a submenu with the appropriate sharing
 * actions.
 *
 * <h3 id="add-share-action">Adding a share action</h3>
 *
 * <p>To add a "share" action to your activity, put a
 * <code>ShareActionProvider</code> in the app bar's menu resource. For
 * example:</p>
 *
 * <pre>
 * &lt;item android:id="&#64;+id/action_share"
 *      android:title="&#64;string/share"
 *      app:showAsAction="ifRoom"
 *      app:actionProviderClass="android.support.v7.widget.ShareActionProvider"/&gt;
 * </pre>
 *
 * <p>You do not need to specify an icon, since the
 * <code>ShareActionProvider</code> widget takes care of its own appearance and
 * behavior. However, you do need to specify a title with
 * <code>android:title</code>, in case the action ends up in the overflow
 * menu.</p>
 *
 * <p>Next, set up the intent that contains the content your activity is
 * able to share. You should create this intent in your handler for
 * {@link android.app.Activity#onCreateOptionsMenu onCreateOptionsMenu()},
 * and update it every time the shareable content changes. To set up the
 * intent:</p>
 *
 * <ol>
 * <li>Get a reference to the ShareActionProvider by calling {@link
 * android.view.MenuItem#getActionProvider getActionProvider()} and
 * passing the share action's {@link android.view.MenuItem}. For
 * example:
 *
 * <pre>
 * MenuItem shareItem = menu.findItem(R.id.action_share);
 * ShareActionProvider myShareActionProvider =
 *     (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);</pre></li>
 *
 * <li>Create an intent with the {@link android.content.Intent#ACTION_SEND}
 * action, and attach the content shared by the activity. For example, the
 * following intent shares an image:
 *
 * <pre>
 * Intent myShareIntent = new Intent(Intent.ACTION_SEND);
 * myShareIntent.setType("image/*");
 * myShareIntent.putExtra(Intent.EXTRA_STREAM, myImageUri);</pre></li>
 *
 * <li>Call {@link #setShareIntent setShareIntent()} to attach this intent to
 * the action provider:
 *
 * <pre>
 * myShareActionProvider.setShareIntent(myShareIntent);
 * </pre></li>
 *
 * <li>When the content changes, modify the intent or create a new one,
 * and call {@link #setShareIntent setShareIntent()} again. For example:
 *
 * <pre>
 * // Image has changed! Update the intent:
 * myShareIntent.putExtra(Intent.EXTRA_STREAM, myNewImageUri);
 * myShareActionProvider.setShareIntent(myShareIntent);</pre></li>
 * </ol>
 *
 * <h3 id="rankings">Share target rankings</h3>
 *
 * <p>The share action provider retains a ranking for each share target,
 * based on how often the user chooses each one. The more often a user
 * chooses a target, the higher its rank; the
 * most-commonly used target appears in the app bar as the default target.</p>
 *
 * <p>By default, the target ranking information is stored in a private
 * file with the name specified by {@link
 * #DEFAULT_SHARE_HISTORY_FILE_NAME}. Ordinarily, the share action provider stores
 * all the history in this single file. However, using a single set of
 * rankings may not make sense if the
 * share action provider is used for different kinds of content. For
 * example, if the activity sometimes shares images and sometimes shares
 * contacts, you would want to maintain two different sets of rankings.</p>
 *
 * <p>To set the history file, call {@link #setShareHistoryFileName
 * setShareHistoryFileName()} and pass the name of an XML file. The file
 * you specify is used until the next time you call {@link
 * #setShareHistoryFileName setShareHistoryFileName()}.</p>
 *
 * @see ActionProvider
 */
public class ShareActionProvider extends ActionProvider {

    /**
     * Listener for the event of selecting a share target.
     */
    public interface OnShareTargetSelectedListener {

        /**
         * Called when a share target has been selected. The client can
         * decide whether to perform some action before the sharing is
         * actually performed.
         * <p>
         * <strong>Note:</strong> Modifying the intent is not permitted and
         *     any changes to the latter will be ignored.
         * </p>
         * <p>
         * <strong>Note:</strong> You should <strong>not</strong> handle the
         *     intent here. This callback aims to notify the client that a
         *     sharing is being performed, so the client can update the UI
         *     if necessary.
         * </p>
         *
         * @param source The source of the notification.
         * @param intent The intent for launching the chosen share target.
         * @return The return result is ignored. Always return false for consistency.
         */
        public boolean onShareTargetSelected(ShareActionProvider source, Intent intent);
    }

    /**
     * The default for the maximal number of activities shown in the sub-menu.
     */
    private static final int DEFAULT_INITIAL_ACTIVITY_COUNT = 4;

    /**
     * The the maximum number activities shown in the sub-menu.
     */
    private int mMaxShownActivityCount = DEFAULT_INITIAL_ACTIVITY_COUNT;

    /**
     * Listener for handling menu item clicks.
     */
    private final ShareMenuItemOnMenuItemClickListener mOnMenuItemClickListener =
            new ShareMenuItemOnMenuItemClickListener();

    /**
     * The default name for storing share history.
     */
    public static final String DEFAULT_SHARE_HISTORY_FILE_NAME = "share_history.xml";

    /**
     * Context for accessing resources.
     */
    final Context mContext;

    /**
     * The name of the file with share history data.
     */
    String mShareHistoryFileName = DEFAULT_SHARE_HISTORY_FILE_NAME;

    OnShareTargetSelectedListener mOnShareTargetSelectedListener;

    private OnChooseActivityListener mOnChooseActivityListener;

    /**
     * Creates a new instance.
     *
     * @param context Context for accessing resources.
     */
    public ShareActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    /**
     * Sets a listener to be notified when a share target has been selected.
     * The listener can optionally decide to handle the selection and
     * not rely on the default behavior which is to launch the activity.
     * <p>
     * <strong>Note:</strong> If you choose the backing share history file
     *     you will still be notified in this callback.
     * </p>
     * @param listener The listener.
     */
    public void setOnShareTargetSelectedListener(OnShareTargetSelectedListener listener) {
        mOnShareTargetSelectedListener = listener;
        setActivityChooserPolicyIfNeeded();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateActionView() {
        // Create the view and set its data model.
        ActivityChooserView activityChooserView = new ActivityChooserView(mContext);
        if (!activityChooserView.isInEditMode()) {
            ActivityChooserModel dataModel = ActivityChooserModel.get(mContext, mShareHistoryFileName);
            activityChooserView.setActivityChooserModel(dataModel);
        }

        // Lookup and set the expand action icon.
        TypedValue outTypedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(R.attr.actionModeShareDrawable, outTypedValue, true);
        Drawable drawable = AppCompatResources.getDrawable(mContext, outTypedValue.resourceId);
        activityChooserView.setExpandActivityOverflowButtonDrawable(drawable);
        activityChooserView.setProvider(this);

        // Set content description.
        activityChooserView.setDefaultActionButtonContentDescription(
                R.string.abc_shareactionprovider_share_with_application);
        activityChooserView.setExpandActivityOverflowButtonContentDescription(
                R.string.abc_shareactionprovider_share_with);

        return activityChooserView;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasSubMenu() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareSubMenu(SubMenu subMenu) {
        // Clear since the order of items may change.
        subMenu.clear();

        ActivityChooserModel dataModel = ActivityChooserModel.get(mContext, mShareHistoryFileName);
        PackageManager packageManager = mContext.getPackageManager();

        final int expandedActivityCount = dataModel.getActivityCount();
        final int collapsedActivityCount = Math.min(expandedActivityCount, mMaxShownActivityCount);

        // Populate the sub-menu with a sub set of the activities.
        for (int i = 0; i < collapsedActivityCount; i++) {
            ResolveInfo activity = dataModel.getActivity(i);
            subMenu.add(0, i, i, activity.loadLabel(packageManager))
                    .setIcon(activity.loadIcon(packageManager))
                    .setOnMenuItemClickListener(mOnMenuItemClickListener);
        }

        if (collapsedActivityCount < expandedActivityCount) {
            // Add a sub-menu for showing all activities as a list item.
            SubMenu expandedSubMenu = subMenu.addSubMenu(Menu.NONE, collapsedActivityCount,
                    collapsedActivityCount,
                    mContext.getString(R.string.abc_activity_chooser_view_see_all));
            for (int i = 0; i < expandedActivityCount; i++) {
                ResolveInfo activity = dataModel.getActivity(i);
                expandedSubMenu.add(0, i, i, activity.loadLabel(packageManager))
                        .setIcon(activity.loadIcon(packageManager))
                        .setOnMenuItemClickListener(mOnMenuItemClickListener);
            }
        }
    }

    /**
     * Sets the file name of a file for persisting the share history which
     * history will be used for ordering share targets. This file will be used
     * for all view created by {@link #onCreateActionView()}. Defaults to
     * {@link #DEFAULT_SHARE_HISTORY_FILE_NAME}. Set to <code>null</code>
     * if share history should not be persisted between sessions.
     *
     * <p class="note">
     * <strong>Note:</strong> The history file name can be set any time, however
     * only the action views created by {@link #onCreateActionView()} after setting
     * the file name will be backed by the provided file. Therefore, if you want to
     * use different history files for sharing specific types of content, every time
     * you change the history file with {@link #setShareHistoryFileName(String)} you must
     * call {@link android.support.v7.app.AppCompatActivity#supportInvalidateOptionsMenu()}
     * to recreate the action view. You should <strong>not</strong> call
     * {@link android.support.v7.app.AppCompatActivity#supportInvalidateOptionsMenu()} from
     * {@link android.support.v7.app.AppCompatActivity#onCreateOptionsMenu(Menu)}.
     *
     * <pre>
     * private void doShare(Intent intent) {
     *     if (IMAGE.equals(intent.getMimeType())) {
     *         mShareActionProvider.setHistoryFileName(SHARE_IMAGE_HISTORY_FILE_NAME);
     *     } else if (TEXT.equals(intent.getMimeType())) {
     *         mShareActionProvider.setHistoryFileName(SHARE_TEXT_HISTORY_FILE_NAME);
     *     }
     *     mShareActionProvider.setIntent(intent);
     *     supportInvalidateOptionsMenu();
     * }
     * </pre>
     *
     * @param shareHistoryFile The share history file name.
     */
    public void setShareHistoryFileName(String shareHistoryFile) {
        mShareHistoryFileName = shareHistoryFile;
        setActivityChooserPolicyIfNeeded();
    }

    /**
     * Sets an intent with information about the share action. Here is a
     * sample for constructing a share intent:
     *
     * <pre>
     *  Intent shareIntent = new Intent(Intent.ACTION_SEND);
     *  shareIntent.setType("image/*");
     *  Uri uri = Uri.fromFile(new File(getFilesDir(), "foo.jpg"));
     *  shareIntent.putExtra(Intent.EXTRA_STREAM, uri.toString());
     * </pre>
     *
     * @param shareIntent The share intent.
     *
     * @see Intent#ACTION_SEND
     * @see Intent#ACTION_SEND_MULTIPLE
     */
    public void setShareIntent(Intent shareIntent) {
        if (shareIntent != null) {
            final String action = shareIntent.getAction();
            if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                updateIntent(shareIntent);
            }
        }
        ActivityChooserModel dataModel = ActivityChooserModel.get(mContext,
                mShareHistoryFileName);
        dataModel.setIntent(shareIntent);
    }

    /**
     * Reusable listener for handling share item clicks.
     */
    private class ShareMenuItemOnMenuItemClickListener implements OnMenuItemClickListener {
        ShareMenuItemOnMenuItemClickListener() {
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            ActivityChooserModel dataModel = ActivityChooserModel.get(mContext,
                    mShareHistoryFileName);
            final int itemId = item.getItemId();
            Intent launchIntent = dataModel.chooseActivity(itemId);
            if (launchIntent != null) {
                final String action = launchIntent.getAction();
                if (Intent.ACTION_SEND.equals(action) ||
                        Intent.ACTION_SEND_MULTIPLE.equals(action)) {
                    updateIntent(launchIntent);
                }
                mContext.startActivity(launchIntent);
            }
            return true;
        }
    }

    /**
     * Set the activity chooser policy of the model backed by the current
     * share history file if needed which is if there is a registered callback.
     */
    private void setActivityChooserPolicyIfNeeded() {
        if (mOnShareTargetSelectedListener == null) {
            return;
        }
        if (mOnChooseActivityListener == null) {
            mOnChooseActivityListener = new ShareActivityChooserModelPolicy();
        }
        ActivityChooserModel dataModel = ActivityChooserModel.get(mContext, mShareHistoryFileName);
        dataModel.setOnChooseActivityListener(mOnChooseActivityListener);
    }

    /**
     * Policy that delegates to the {@link OnShareTargetSelectedListener}, if such.
     */
    private class ShareActivityChooserModelPolicy implements OnChooseActivityListener {
        ShareActivityChooserModelPolicy() {
        }

        @Override
        public boolean onChooseActivity(ActivityChooserModel host, Intent intent) {
            if (mOnShareTargetSelectedListener != null) {
                mOnShareTargetSelectedListener.onShareTargetSelected(
                        ShareActionProvider.this, intent);
            }
            return false;
        }
    }

    void updateIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= 21) {
            // If we're on Lollipop, we can open the intent as a document
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            // Else, we will use the old CLEAR_WHEN_TASK_RESET flag
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
    }
}
