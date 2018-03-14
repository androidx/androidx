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

package androidx.preference;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;

/**
 * Used to help create {@link Preference} hierarchies
 * from activities or XML.
 * <p>
 * In most cases, clients should use
 * {@link androidx.preference.PreferenceFragment#addPreferencesFromResource(int)}, or
 * {@link PreferenceFragmentCompat#addPreferencesFromResource(int)}.
 *
 * @see androidx.preference.PreferenceFragment
 * @see PreferenceFragmentCompat
 */
public class PreferenceManager {

    public static final String KEY_HAS_SET_DEFAULT_VALUES = "_has_set_default_values";

    /**
     * The context to use. This should always be set.
     */
    private Context mContext;

    /**
     * The counter for unique IDs.
     */
    private long mNextId = 0;

    /**
     * Cached shared preferences.
     */
    @Nullable
    private SharedPreferences mSharedPreferences;

    /**
     * Data store to be used by the Preferences or null if {@link android.content.SharedPreferences}
     * should be used.
     */
    @Nullable
    private PreferenceDataStore mPreferenceDataStore;

    /**
     * If in no-commit mode, the shared editor to give out (which will be
     * committed when exiting no-commit mode).
     */
    @Nullable
    private SharedPreferences.Editor mEditor;

    /**
     * Blocks commits from happening on the shared editor. This is used when
     * inflating the hierarchy. Do not set this directly, use {@link #setNoCommit(boolean)}
     */
    private boolean mNoCommit;

    /**
     * The SharedPreferences name that will be used for all {@link Preference}s
     * managed by this instance.
     */
    private String mSharedPreferencesName;

    /**
     * The SharedPreferences mode that will be used for all {@link Preference}s
     * managed by this instance.
     */
    private int mSharedPreferencesMode;

    private static final int STORAGE_DEFAULT = 0;
    private static final int STORAGE_DEVICE_PROTECTED = 1;

    private int mStorage = STORAGE_DEFAULT;

    /**
     * The {@link PreferenceScreen} at the root of the preference hierarchy.
     */
    private PreferenceScreen mPreferenceScreen;

    private PreferenceComparisonCallback mPreferenceComparisonCallback;
    private OnPreferenceTreeClickListener mOnPreferenceTreeClickListener;
    private OnDisplayPreferenceDialogListener mOnDisplayPreferenceDialogListener;
    private OnNavigateToScreenListener mOnNavigateToScreenListener;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public PreferenceManager(Context context) {
        mContext = context;

        setSharedPreferencesName(getDefaultSharedPreferencesName(context));
    }

    /**
     * Inflates a preference hierarchy from XML. If a preference hierarchy is
     * given, the new preference hierarchies will be merged in.
     *
     * @param context The context of the resource.
     * @param resId The resource ID of the XML to inflate.
     * @param rootPreferences Optional existing hierarchy to merge the new
     *            hierarchies into.
     * @return The root hierarchy (if one was not provided, the new hierarchy's
     *         root).
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public PreferenceScreen inflateFromResource(Context context, int resId,
            PreferenceScreen rootPreferences) {
        // Block commits
        setNoCommit(true);

        final PreferenceInflater inflater = new PreferenceInflater(context, this);
        rootPreferences = (PreferenceScreen) inflater.inflate(resId, rootPreferences);
        rootPreferences.onAttachedToHierarchy(this);

        // Unblock commits
        setNoCommit(false);

        return rootPreferences;
    }

    public PreferenceScreen createPreferenceScreen(Context context) {
        final PreferenceScreen preferenceScreen = new PreferenceScreen(context, null);
        preferenceScreen.onAttachedToHierarchy(this);
        return preferenceScreen;
    }

    /**
     * Called by a preference to get a unique ID in its hierarchy.
     *
     * @return A unique ID.
     */
    long getNextId() {
        synchronized (this) {
            return mNextId++;
        }
    }

    /**
     * Returns the current name of the {@link SharedPreferences} file that preferences managed by
     * this will use.
     *
     * @return The name that can be passed to {@link Context#getSharedPreferences(String, int)}.
     * @see Context#getSharedPreferences(String, int)
     */
    public String getSharedPreferencesName() {
        return mSharedPreferencesName;
    }

    /**
     * Sets the name of the {@link SharedPreferences} file that preferences managed by this
     * will use.
     *
     * <p>If custom {@link PreferenceDataStore} is set, this won't override its usage.
     *
     * @param sharedPreferencesName The name of the SharedPreferences file.
     * @see Context#getSharedPreferences(String, int)
     * @see #setPreferenceDataStore(PreferenceDataStore)
     */
    public void setSharedPreferencesName(String sharedPreferencesName) {
        mSharedPreferencesName = sharedPreferencesName;
        mSharedPreferences = null;
    }

    /**
     * Returns the current mode of the SharedPreferences file that preferences managed by
     * this will use.
     *
     * @return The mode that can be passed to {@link Context#getSharedPreferences(String, int)}.
     * @see Context#getSharedPreferences(String, int)
     */
    public int getSharedPreferencesMode() {
        return mSharedPreferencesMode;
    }

    /**
     * Sets the mode of the SharedPreferences file that preferences managed by this
     * will use.
     *
     * @param sharedPreferencesMode The mode of the SharedPreferences file.
     * @see Context#getSharedPreferences(String, int)
     */
    public void setSharedPreferencesMode(int sharedPreferencesMode) {
        mSharedPreferencesMode = sharedPreferencesMode;
        mSharedPreferences = null;
    }

    /**
     * Sets the storage location used internally by this class to be the default
     * provided by the hosting {@link Context}.
     */
    public void setStorageDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            mStorage = STORAGE_DEFAULT;
            mSharedPreferences = null;
        }
    }

    /**
     * Explicitly set the storage location used internally by this class to be
     * device-protected storage.
     * <p>
     * On devices with direct boot, data stored in this location is encrypted
     * with a key tied to the physical device, and it can be accessed
     * immediately after the device has booted successfully, both
     * <em>before and after</em> the user has authenticated with their
     * credentials (such as a lock pattern or PIN).
     * <p>
     * Because device-protected data is available without user authentication,
     * you should carefully limit the data you store using this Context. For
     * example, storing sensitive authentication tokens or passwords in the
     * device-protected area is strongly discouraged.
     * <p>
     * Prior to API 24 this method has no effect,
     * since device-protected storage is not available.
     *
     * @see Context#createDeviceProtectedStorageContext()
     */
    public void setStorageDeviceProtected() {
        if (Build.VERSION.SDK_INT >= 24) {
            mStorage = STORAGE_DEVICE_PROTECTED;
            mSharedPreferences = null;
        }
    }

    /**
     * Indicates if the storage location used internally by this class is the
     * default provided by the hosting {@link Context}.
     *
     * @see #setStorageDefault()
     * @see #setStorageDeviceProtected()
     */
    public boolean isStorageDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return mStorage == STORAGE_DEFAULT;
        } else {
            return true;
        }
    }

    /**
     * Indicates if the storage location used internally by this class is backed
     * by device-protected storage.
     *
     * @see #setStorageDefault()
     * @see #setStorageDeviceProtected()
     */
    public boolean isStorageDeviceProtected() {
        if (Build.VERSION.SDK_INT >= 24) {
            return mStorage == STORAGE_DEVICE_PROTECTED;
        } else {
            return false;
        }
    }

    /**
     * Sets a {@link PreferenceDataStore} to be used by all Preferences associated with this manager
     * that don't have a custom {@link PreferenceDataStore} assigned via
     * {@link Preference#setPreferenceDataStore(PreferenceDataStore)}. Also if the data store is
     * set, the child preferences won't use {@link android.content.SharedPreferences} as long as
     * they are assigned to this manager.
     *
     * @param dataStore the {@link PreferenceDataStore} to be used by this manager
     * @see Preference#setPreferenceDataStore(PreferenceDataStore)
     */
    public void setPreferenceDataStore(PreferenceDataStore dataStore) {
        mPreferenceDataStore = dataStore;
    }

    /**
     * Returns the {@link PreferenceDataStore} associated with this manager or {@code null} if
     * the default {@link android.content.SharedPreferences} are used instead.
     *
     * @return The {@link PreferenceDataStore} associated with this manager or {@code null} if none.
     * @see #setPreferenceDataStore(PreferenceDataStore)
     */
    @Nullable
    public PreferenceDataStore getPreferenceDataStore() {
        return mPreferenceDataStore;
    }

    /**
     * Gets a {@link SharedPreferences} instance that preferences managed by this will
     * use.
     *
     * @return a {@link SharedPreferences} instance pointing to the file that contain the values of
     *         preferences that are managed by this PreferenceManager. If
     *         a {@link PreferenceDataStore} has been set, this method returns {@code null}.
     */
    public SharedPreferences getSharedPreferences() {
        if (getPreferenceDataStore() != null) {
            return null;
        }

        if (mSharedPreferences == null) {
            final Context storageContext;
            switch (mStorage) {
                case STORAGE_DEVICE_PROTECTED:
                    storageContext = ContextCompat.createDeviceProtectedStorageContext(mContext);
                    break;
                default:
                    storageContext = mContext;
                    break;
            }

            mSharedPreferences = storageContext.getSharedPreferences(mSharedPreferencesName,
                    mSharedPreferencesMode);
        }

        return mSharedPreferences;
    }

    /**
     * Gets a SharedPreferences instance that points to the default file that is
     * used by the preference framework in the given context.
     *
     * @param context The context of the preferences whose values are wanted.
     * @return A SharedPreferences instance that can be used to retrieve and
     *         listen to values of the preferences.
     */
    public static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode());
    }

    private static String getDefaultSharedPreferencesName(Context context) {
        return context.getPackageName() + "_preferences";
    }

    private static int getDefaultSharedPreferencesMode() {
        return Context.MODE_PRIVATE;
    }

    /**
     * Returns the root of the preference hierarchy managed by this class.
     *
     * @return The {@link PreferenceScreen} object that is at the root of the hierarchy.
     */
    public PreferenceScreen getPreferenceScreen() {
        return mPreferenceScreen;
    }

    /**
     * Sets the root of the preference hierarchy.
     *
     * @param preferenceScreen The root {@link PreferenceScreen} of the preference hierarchy.
     * @return Whether the {@link PreferenceScreen} given is different than the previous.
     */
    public boolean setPreferences(PreferenceScreen preferenceScreen) {
        if (preferenceScreen != mPreferenceScreen) {
            if (mPreferenceScreen != null) {
                mPreferenceScreen.onDetached();
            }
            mPreferenceScreen = preferenceScreen;
            return true;
        }

        return false;
    }

    /**
     * Finds a {@link Preference} based on its key.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     * @see PreferenceGroup#findPreference(CharSequence)
     */
    public Preference findPreference(CharSequence key) {
        if (mPreferenceScreen == null) {
            return null;
        }

        return mPreferenceScreen.findPreference(key);
    }

    /**
     * Sets the default values from an XML preference file by reading the values defined
     * by each {@link Preference} item's {@code android:defaultValue} attribute. This should
     * be called by the application's main activity.
     * <p>
     *
     * @param context The context of the shared preferences.
     * @param resId The resource ID of the preference XML file.
     * @param readAgain Whether to re-read the default values.
     * If false, this method sets the default values only if this
     * method has never been called in the past (or if the
     * {@link #KEY_HAS_SET_DEFAULT_VALUES} in the default value shared
     * preferences file is false). To attempt to set the default values again
     * bypassing this check, set {@code readAgain} to true.
     *            <p class="note">
     *            Note: this will NOT reset preferences back to their default
     *            values. For that functionality, use
     *            {@link PreferenceManager#getDefaultSharedPreferences(Context)}
     *            and clear it followed by a call to this method with this
     *            parameter set to true.
     */
    public static void setDefaultValues(Context context, int resId, boolean readAgain) {
        // Use the default shared preferences name and mode
        setDefaultValues(context, getDefaultSharedPreferencesName(context),
                getDefaultSharedPreferencesMode(), resId, readAgain);
    }

    /**
     * Similar to {@link #setDefaultValues(Context, int, boolean)} but allows
     * the client to provide the filename and mode of the shared preferences
     * file.
     *
     * @param context The context of the shared preferences.
     * @param sharedPreferencesName A custom name for the shared preferences file.
     * @param sharedPreferencesMode The file creation mode for the shared preferences file, such
     * as {@link android.content.Context#MODE_PRIVATE} or {@link
     * android.content.Context#MODE_PRIVATE}
     * @param resId The resource ID of the preference XML file.
     * @param readAgain Whether to re-read the default values.
     * If false, this method will set the default values only if this
     * method has never been called in the past (or if the
     * {@link #KEY_HAS_SET_DEFAULT_VALUES} in the default value shared
     * preferences file is false). To attempt to set the default values again
     * bypassing this check, set {@code readAgain} to true.
     *            <p class="note">
     *            Note: this will NOT reset preferences back to their default
     *            values. For that functionality, use
     *            {@link PreferenceManager#getDefaultSharedPreferences(Context)}
     *            and clear it followed by a call to this method with this
     *            parameter set to true.
     *
     * @see #setDefaultValues(Context, int, boolean)
     * @see #setSharedPreferencesName(String)
     * @see #setSharedPreferencesMode(int)
     */
    public static void setDefaultValues(Context context, String sharedPreferencesName,
            int sharedPreferencesMode, int resId, boolean readAgain) {
        final SharedPreferences defaultValueSp = context.getSharedPreferences(
                KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE);

        if (readAgain || !defaultValueSp.getBoolean(KEY_HAS_SET_DEFAULT_VALUES, false)) {
            final PreferenceManager pm = new PreferenceManager(context);
            pm.setSharedPreferencesName(sharedPreferencesName);
            pm.setSharedPreferencesMode(sharedPreferencesMode);
            pm.inflateFromResource(context, resId, null);

            defaultValueSp.edit()
                    .putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true)
                    .apply();
        }
    }

    /**
     * Returns an editor to use when modifying the shared preferences.
     *
     * <p>Do NOT commit unless {@link #shouldCommit()} returns true.
     *
     * @return an editor to use to write to shared preferences. If a {@link PreferenceDataStore} has
     *         been set, this method returns {@code null}.
     * @see #shouldCommit()
     */
    SharedPreferences.Editor getEditor() {
        if (mPreferenceDataStore != null) {
            return null;
        }

        if (mNoCommit) {
            if (mEditor == null) {
                mEditor = getSharedPreferences().edit();
            }

            return mEditor;
        } else {
            return getSharedPreferences().edit();
        }
    }

    /**
     * Whether it is the client's responsibility to commit on the
     * {@link #getEditor()}. This will return false in cases where the writes
     * should be batched, for example when inflating preferences from XML.
     *
     * <p>If preferences are using {@link PreferenceDataStore} this value is irrelevant.
     *
     * @return Whether the client should commit.
     */
    boolean shouldCommit() {
        return !mNoCommit;
    }

    private void setNoCommit(boolean noCommit) {
        if (!noCommit && mEditor != null) {
            mEditor.apply();
        }
        mNoCommit = noCommit;
    }

    /**
     * Returns the context.
     *
     * @return The context.
     */
    public Context getContext() {
        return mContext;
    }

    public PreferenceComparisonCallback getPreferenceComparisonCallback() {
        return mPreferenceComparisonCallback;
    }

    public void setPreferenceComparisonCallback(
            PreferenceComparisonCallback preferenceComparisonCallback) {
        mPreferenceComparisonCallback = preferenceComparisonCallback;
    }

    public OnDisplayPreferenceDialogListener getOnDisplayPreferenceDialogListener() {
        return mOnDisplayPreferenceDialogListener;
    }

    public void setOnDisplayPreferenceDialogListener(
            OnDisplayPreferenceDialogListener onDisplayPreferenceDialogListener) {
        mOnDisplayPreferenceDialogListener = onDisplayPreferenceDialogListener;
    }

    /**
     * Called when a preference requests that a dialog be shown to complete a user interaction.
     *
     * @param preference The preference requesting the dialog.
     */
    public void showDialog(Preference preference) {
        if (mOnDisplayPreferenceDialogListener != null) {
            mOnDisplayPreferenceDialogListener.onDisplayPreferenceDialog(preference);
        }
    }

    /**
     * Sets the callback to be invoked when a {@link Preference} in the
     * hierarchy rooted at this {@link PreferenceManager} is clicked.
     *
     * @param listener The callback to be invoked.
     */
    public void setOnPreferenceTreeClickListener(OnPreferenceTreeClickListener listener) {
        mOnPreferenceTreeClickListener = listener;
    }

    public OnPreferenceTreeClickListener getOnPreferenceTreeClickListener() {
        return mOnPreferenceTreeClickListener;
    }

    /**
     * Sets the callback to be invoked when a {@link PreferenceScreen} in the hierarchy rooted at
     * this {@link PreferenceManager} is clicked.
     *
     * @param listener The callback to be invoked.
     */
    public void setOnNavigateToScreenListener(OnNavigateToScreenListener listener) {
        mOnNavigateToScreenListener = listener;
    }

    /**
     * Returns the {@link PreferenceManager.OnNavigateToScreenListener}, if one has been set.
     */
    public OnNavigateToScreenListener getOnNavigateToScreenListener() {
        return mOnNavigateToScreenListener;
    }

    /**
     * Callback class to be used by the {@link androidx.recyclerview.widget.RecyclerView.Adapter}
     * associated with the {@link PreferenceScreen}, used to determine when two {@link Preference}
     * objects are semantically and visually the same.
     */
    public static abstract class PreferenceComparisonCallback {
        /**
         * Called to determine if two {@link Preference} objects represent the same item
         *
         * @param p1 {@link Preference} object to compare
         * @param p2 {@link Preference} object to compare
         * @return {@code true} if the objects represent the same item
         */
        public abstract boolean arePreferenceItemsTheSame(Preference p1, Preference p2);

        /**
         * Called to determine if two {@link Preference} objects will display the same data
         *
         * @param p1 {@link Preference} object to compare
         * @param p2 {@link Preference} object to compare
         * @return {@code true} if the objects are visually identical
         */
        public abstract boolean arePreferenceContentsTheSame(Preference p1, Preference p2);
    }

    /**
     * A basic implementation of {@link PreferenceComparisonCallback} suitable for use with the
     * default {@link Preference} classes. If the {@link PreferenceScreen} contains custom
     * {@link Preference} subclasses, you must override
     * {@link #arePreferenceContentsTheSame(Preference, Preference)}
     */
    public static class SimplePreferenceComparisonCallback extends PreferenceComparisonCallback {
        /**
         * {@inheritDoc}
         *
         * <p>This method will not be able to track replaced {@link Preference} objects if they
         * do not have a unique key.</p>
         *
         * @see Preference#setKey(String)
         */
        @Override
        public boolean arePreferenceItemsTheSame(Preference p1, Preference p2) {
            return p1.getId() == p2.getId();
        }

        /**
         * {@inheritDoc}
         *
         * <p>The result of this method is only valid for the default {@link Preference} objects,
         * and custom subclasses which do not override
         * {@link Preference#onBindViewHolder(PreferenceViewHolder)}. This method also assumes
         * that if a preference object is being replaced by a new instance, the old instance was
         * not modified after being removed from its containing {@link PreferenceGroup}.</p>
         */
        @Override
        public boolean arePreferenceContentsTheSame(Preference p1, Preference p2) {
            if (p1.getClass() != p2.getClass()) {
                return false;
            }
            if (p1 == p2 && p1.wasDetached()) {
                // Defensively handle the case where a preference was removed, updated and re-added.
                // Hopefully this is rare.
                return false;
            }
            if (!TextUtils.equals(p1.getTitle(), p2.getTitle())) {
                return false;
            }
            if (!TextUtils.equals(p1.getSummary(), p2.getSummary())) {
                return false;
            }
            final Drawable p1Icon = p1.getIcon();
            final Drawable p2Icon = p2.getIcon();
            if (p1Icon != p2Icon && (p1Icon == null || !p1Icon.equals(p2Icon))) {
                return false;
            }
            if (p1.isEnabled() != p2.isEnabled()) {
                return false;
            }
            if (p1.isSelectable() != p2.isSelectable()) {
                return false;
            }
            if (p1 instanceof TwoStatePreference) {
                if (((TwoStatePreference) p1).isChecked()
                        != ((TwoStatePreference) p2).isChecked()) {
                    return false;
                }
            }
            if (p1 instanceof DropDownPreference && p1 != p2) {
                // Different object, must re-bind spinner adapter
                return false;
            }

            return true;
        }
    }

    /**
     * Interface definition for a callback to be invoked when a
     * {@link Preference} in the hierarchy rooted at this {@link PreferenceScreen} is
     * clicked.
     */
    public interface OnPreferenceTreeClickListener {
        /**
         * Called when a preference in the tree rooted at this
         * {@link PreferenceScreen} has been clicked.
         *
         * @param preference The preference that was clicked.
         * @return Whether the click was handled.
         */
        boolean onPreferenceTreeClick(Preference preference);
    }

    /**
     * Interface definition for a class that will be called when a
     * {@link androidx.preference.Preference} requests to display a dialog.
     */
    public interface OnDisplayPreferenceDialogListener {

        /**
         * Called when a preference in the tree requests to display a dialog.
         *
         * @param preference The Preference object requesting the dialog.
         */
        void onDisplayPreferenceDialog(Preference preference);
    }

    /**
     * Interface definition for a class that will be called when a
     * {@link androidx.preference.PreferenceScreen} requests navigation.
     */
    public interface OnNavigateToScreenListener {

        /**
         * Called when a PreferenceScreen in the tree requests to navigate to its contents.
         *
         * @param preferenceScreen The PreferenceScreen requesting navigation.
         */
        void onNavigateToScreen(PreferenceScreen preferenceScreen);
    }

}
