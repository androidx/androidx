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
 * limitations under the License
 */

package android.support.v7.preference;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v4.os.BuildCompat;

/**
 * Used to help create {@link Preference} hierarchies
 * from activities or XML.
 * <p>
 * In most cases, clients should use
 * {@link android.support.v14.preference.PreferenceFragment#addPreferencesFromResource(int)}, or
 * {@link PreferenceFragmentCompat#addPreferencesFromResource(int)}.
 *
 * @see android.support.v14.preference.PreferenceFragment
 * @see PreferenceFragmentCompat
 */
public class PreferenceManager {

    private static final String TAG = "PreferenceManager";

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
    private SharedPreferences mSharedPreferences;

    /**
     * If in no-commit mode, the shared editor to give out (which will be
     * committed when exiting no-commit mode).
     */
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

    private OnPreferenceTreeClickListener mOnPreferenceTreeClickListener;
    private OnDisplayPreferenceDialogListener mOnDisplayPreferenceDialogListener;
    private OnNavigateToScreenListener mOnNavigateToScreenListener;

    /**
     * @hide
     */
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
     * Returns the current name of the SharedPreferences file that preferences managed by
     * this will use.
     *
     * @return The name that can be passed to {@link Context#getSharedPreferences(String, int)}.
     * @see Context#getSharedPreferences(String, int)
     */
    public String getSharedPreferencesName() {
        return mSharedPreferencesName;
    }

    /**
     * Sets the name of the SharedPreferences file that preferences managed by this
     * will use.
     *
     * @param sharedPreferencesName The name of the SharedPreferences file.
     * @see Context#getSharedPreferences(String, int)
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
        if (BuildCompat.isAtLeastN()) {
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
     * Prior to {@link BuildCompat#isAtLeastN()} this method has no effect,
     * since device-protected storage is not available.
     *
     * @see Context#createDeviceProtectedStorageContext()
     */
    public void setStorageDeviceProtected() {
        if (BuildCompat.isAtLeastN()) {
            mStorage = STORAGE_DEVICE_PROTECTED;
            mSharedPreferences = null;
        }
    }

    /**
     * @removed
     * @deprecated
     */
    @Deprecated
    public void setStorageDeviceEncrypted() {
        setStorageDeviceProtected();
    }

    /**
     * Indicates if the storage location used internally by this class is the
     * default provided by the hosting {@link Context}.
     *
     * @see #setStorageDefault()
     * @see #setStorageDeviceProtected()
     */
    public boolean isStorageDefault() {
        if (BuildCompat.isAtLeastN()) {
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
        if (BuildCompat.isAtLeastN()) {
            return mStorage == STORAGE_DEVICE_PROTECTED;
        } else {
            return false;
        }
    }

    /**
     * Gets a SharedPreferences instance that preferences managed by this will
     * use.
     *
     * @return A SharedPreferences instance pointing to the file that contains
     *         the values of preferences that are managed by this.
     */
    public SharedPreferences getSharedPreferences() {
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

            SharedPreferences.Editor editor =
                    defaultValueSp.edit().putBoolean(KEY_HAS_SET_DEFAULT_VALUES, true);

            SharedPreferencesCompat.EditorCompat.getInstance().apply(editor);
        }
    }

    /**
     * Returns an editor to use when modifying the shared preferences.
     * <p>
     * Do NOT commit unless {@link #shouldCommit()} returns true.
     *
     * @return An editor to use to write to shared preferences.
     * @see #shouldCommit()
     */
    SharedPreferences.Editor getEditor() {

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
     * @return Whether the client should commit.
     */
    boolean shouldCommit() {
        return !mNoCommit;
    }

    private void setNoCommit(boolean noCommit) {
        if (!noCommit && mEditor != null) {
            SharedPreferencesCompat.EditorCompat.getInstance().apply(mEditor);
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
     * {@link android.support.v7.preference.Preference} requests to display a dialog.
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
     * {@link android.support.v7.preference.PreferenceScreen} requests navigation.
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
