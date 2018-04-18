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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.res.TypedArrayUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A container for multiple
 * {@link Preference} objects. It is a base class for  Preference objects that are
 * parents, such as {@link PreferenceCategory} and {@link PreferenceScreen}.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about building a settings UI with Preferences,
 * read the <a href="{@docRoot}guide/topics/ui/settings.html">Settings</a>
 * guide.</p>
 * </div>
 *
 * @attr name android:orderingFromXml
 * @attr name initialExpandedChildrenCount
 */
public abstract class PreferenceGroup extends Preference {
    private static final String TAG = "PreferenceGroup";

    /**
     * The container for child {@link Preference}s. This is sorted based on the
     * ordering, please use {@link #addPreference(Preference)} instead of adding
     * to this directly.
     */
    private List<Preference> mPreferenceList;

    private boolean mOrderingAsAdded = true;

    private int mCurrentPreferenceOrder = 0;

    private boolean mAttachedToHierarchy = false;

    private int mInitialExpandedChildrenCount = Integer.MAX_VALUE;

    private final SimpleArrayMap<String, Long> mIdRecycleCache = new SimpleArrayMap<>();
    private final Handler mHandler = new Handler();
    private final Runnable mClearRecycleCacheRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (this) {
                mIdRecycleCache.clear();
            }
        }
    };

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mPreferenceList = new ArrayList<>();

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.PreferenceGroup, defStyleAttr, defStyleRes);

        mOrderingAsAdded =
                TypedArrayUtils.getBoolean(a, R.styleable.PreferenceGroup_orderingFromXml,
                        R.styleable.PreferenceGroup_orderingFromXml, true);

        if (a.hasValue(R.styleable.PreferenceGroup_initialExpandedChildrenCount)) {
            setInitialExpandedChildrenCount((TypedArrayUtils.getInt(
                    a, R.styleable.PreferenceGroup_initialExpandedChildrenCount,
                    R.styleable.PreferenceGroup_initialExpandedChildrenCount, Integer.MAX_VALUE)));
        }
        a.recycle();
    }

    public PreferenceGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public PreferenceGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Whether to order the {@link Preference} children of this group as they
     * are added. If this is false, the ordering will follow each Preference
     * order and default to alphabetic for those without an order.
     * <p>
     * If this is called after preferences are added, they will not be
     * re-ordered in the order they were added, hence call this method early on.
     *
     * @param orderingAsAdded Whether to order according to the order added.
     * @see Preference#setOrder(int)
     */
    public void setOrderingAsAdded(boolean orderingAsAdded) {
        mOrderingAsAdded = orderingAsAdded;
    }

    /**
     * Whether this group is ordering preferences in the order they are added.
     *
     * @return Whether this group orders based on the order the children are added.
     * @see #setOrderingAsAdded(boolean)
     */
    public boolean isOrderingAsAdded() {
        return mOrderingAsAdded;
    }

    /**
     * Sets the maximal number of children that are shown when the preference group is launched
     * where the rest of the children will be hidden.
     * If some children are hidden an expand button will be provided to show all the hidden
     * children. Any child in any level of the hierarchy that is also a preference group (e.g.
     * preference category) will not be counted towards the limit. But instead the children of such
     * group will be counted.
     * By default, all children will be shown, so the default value of this attribute is equal to
     * Integer.MAX_VALUE.
     * <p>
     * Note: The group should have a key defined if an expandable preference is present to
     * correctly persist state.
     *
     * @param expandedCount the number of children that is initially shown.
     *
     * @attr ref R.styleable#PreferenceGroup_initialExpandedChildrenCount
     */
    public void setInitialExpandedChildrenCount(int expandedCount) {
        if (expandedCount != Integer.MAX_VALUE && !hasKey()) {
            Log.e(TAG, this.getClass().getSimpleName()
                    + " should have a key defined if it contains an expandable preference");
        }
        mInitialExpandedChildrenCount = expandedCount;
    }

    /**
     * Gets the maximal number of children that are initially shown.
     *
     * @return the maximal number of children that are initially shown.
     *
     * @attr ref R.styleable#PreferenceGroup_initialExpandedChildrenCount
     */
    public int getInitialExpandedChildrenCount() {
        return mInitialExpandedChildrenCount;
    }

    /**
     * Called by the inflater to add an item to this group.
     */
    public void addItemFromInflater(Preference preference) {
        addPreference(preference);
    }

    /**
     * Returns the number of children {@link Preference}s.
     * @return The number of preference children in this group.
     */
    public int getPreferenceCount() {
        return mPreferenceList.size();
    }

    /**
     * Returns the {@link Preference} at a particular index.
     *
     * @param index The index of the {@link Preference} to retrieve.
     * @return The {@link Preference}.
     */
    public Preference getPreference(int index) {
        return mPreferenceList.get(index);
    }

    /**
     * Adds a {@link Preference} at the correct position based on the
     * preference's order.
     *
     * @param preference The preference to add.
     * @return Whether the preference is now in this group.
     */
    public boolean addPreference(Preference preference) {
        if (mPreferenceList.contains(preference)) {
            return true;
        }
        if (preference.getKey() != null) {
            PreferenceGroup root = this;
            while (root.getParent() != null) {
                root = root.getParent();
            }
            final String key = preference.getKey();
            if (root.findPreference(key) != null) {
                Log.e(TAG, "Found duplicated key: \"" + key
                        + "\". This can cause unintended behaviour,"
                        + " please use unique keys for every preference.");
            }
        }

        if (preference.getOrder() == DEFAULT_ORDER) {
            if (mOrderingAsAdded) {
                preference.setOrder(mCurrentPreferenceOrder++);
            }

            if (preference instanceof PreferenceGroup) {
                // TODO: fix (method is called tail recursively when inflating,
                // so we won't end up properly passing this flag down to children
                ((PreferenceGroup)preference).setOrderingAsAdded(mOrderingAsAdded);
            }
        }

        int insertionIndex = Collections.binarySearch(mPreferenceList, preference);
        if (insertionIndex < 0) {
            insertionIndex = insertionIndex * -1 - 1;
        }

        if (!onPrepareAddPreference(preference)) {
            return false;
        }

        synchronized(this) {
            mPreferenceList.add(insertionIndex, preference);
        }

        final PreferenceManager preferenceManager = getPreferenceManager();
        final String key = preference.getKey();
        final long id;
        if (key != null && mIdRecycleCache.containsKey(key)) {
            id = mIdRecycleCache.get(key);
            mIdRecycleCache.remove(key);
        } else {
            id = preferenceManager.getNextId();
        }
        preference.onAttachedToHierarchy(preferenceManager, id);
        preference.assignParent(this);

        if (mAttachedToHierarchy) {
            preference.onAttached();
        }

        notifyHierarchyChanged();

        return true;
    }

    /**
     * Removes a {@link Preference} from this group.
     *
     * @param preference The preference to remove.
     * @return Whether the preference was found and removed.
     */
    public boolean removePreference(Preference preference) {
        final boolean returnValue = removePreferenceInt(preference);
        notifyHierarchyChanged();
        return returnValue;
    }

    private boolean removePreferenceInt(Preference preference) {
        synchronized(this) {
            preference.onPrepareForRemoval();
            if (preference.getParent() == this) {
                preference.assignParent(null);
            }
            boolean success = mPreferenceList.remove(preference);
            if (success) {
                // If this preference, or another preference with the same key, gets re-added
                // immediately, we want it to have the same id so that it can be correctly tracked
                // in the adapter by RecyclerView, to make it appear as if it has only been
                // seamlessly updated. If the preference is not re-added by the time the handler
                // runs, we take that as a signal that the preference will not be re-added soon
                // in which case it does not need to retain the same id.

                // If two (or more) preferences have the same (or null) key and both are removed
                // and then re-added, only one id will be recycled and the second (and later)
                // preferences will receive a newly generated id. This use pattern of the preference
                // API is strongly discouraged.
                final String key = preference.getKey();
                if (key != null) {
                    mIdRecycleCache.put(key, preference.getId());
                    mHandler.removeCallbacks(mClearRecycleCacheRunnable);
                    mHandler.post(mClearRecycleCacheRunnable);
                }
                if (mAttachedToHierarchy) {
                    preference.onDetached();
                }
            }

            return success;
        }
    }

    /**
     * Removes all {@link Preference Preferences} from this group.
     */
    public void removeAll() {
        synchronized(this) {
            List<Preference> preferenceList = mPreferenceList;
            for (int i = preferenceList.size() - 1; i >= 0; i--) {
                removePreferenceInt(preferenceList.get(0));
            }
        }
        notifyHierarchyChanged();
    }

    /**
     * Prepares a {@link Preference} to be added to the group.
     *
     * @param preference The preference to add.
     * @return Whether to allow adding the preference (true), or not (false).
     */
    protected boolean onPrepareAddPreference(Preference preference) {
        preference.onParentChanged(this, shouldDisableDependents());
        return true;
    }

    /**
     * Finds a {@link Preference} based on its key. If two {@link Preference}
     * share the same key (not recommended), the first to appear will be
     * returned (to retrieve the other preference with the same key, call this
     * method on the first preference). If this preference has the key, it will
     * not be returned.
     * <p>
     * This will recursively search for the preference into children that are
     * also {@link PreferenceGroup PreferenceGroups}.
     *
     * @param key The key of the preference to retrieve.
     * @return The {@link Preference} with the key, or null.
     */
    public Preference findPreference(CharSequence key) {
        if (TextUtils.equals(getKey(), key)) {
            return this;
        }
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            final Preference preference = getPreference(i);
            final String curKey = preference.getKey();

            if (curKey != null && curKey.equals(key)) {
                return preference;
            }

            if (preference instanceof PreferenceGroup) {
                final Preference returnedPreference = ((PreferenceGroup)preference)
                        .findPreference(key);
                if (returnedPreference != null) {
                    return returnedPreference;
                }
            }
        }

        return null;
    }

    /**
     * Whether this preference group should be shown on the same screen as its
     * contained preferences.
     *
     * @return True if the contained preferences should be shown on the same
     *         screen as this preference.
     */
    protected boolean isOnSameScreenAsChildren() {
        return true;
    }

    /**
     * Returns true if we're between {@link #onAttached()} and {@link #onPrepareForRemoval()}
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isAttached() {
        return mAttachedToHierarchy;
    }

    @Override
    public void onAttached() {
        super.onAttached();

        // Mark as attached so if a preference is later added to this group, we
        // can tell it we are already attached
        mAttachedToHierarchy = true;

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onAttached();
        }
    }

    @Override
    public void onDetached() {
        super.onDetached();

        // We won't be attached to the activity anymore
        mAttachedToHierarchy = false;

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onDetached();
        }
    }

    @Override
    public void notifyDependencyChange(boolean disableDependents) {
        super.notifyDependencyChange(disableDependents);

        // Child preferences have an implicit dependency on their containing
        // group. Dispatch dependency change to all contained preferences.
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).onParentChanged(this, disableDependents);
        }
    }

    void sortPreferences() {
        synchronized (this) {
            Collections.sort(mPreferenceList);
        }
    }

    @Override
    protected void dispatchSaveInstanceState(Bundle container) {
        super.dispatchSaveInstanceState(container);

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).dispatchSaveInstanceState(container);
        }
    }

    @Override
    protected void dispatchRestoreInstanceState(Bundle container) {
        super.dispatchRestoreInstanceState(container);

        // Dispatch to all contained preferences
        final int preferenceCount = getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            getPreference(i).dispatchRestoreInstanceState(container);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, mInitialExpandedChildrenCount);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in saveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState groupState = (SavedState) state;
        if (mInitialExpandedChildrenCount != groupState.mInitialExpandedChildrenCount) {
            mInitialExpandedChildrenCount = groupState.mInitialExpandedChildrenCount;
            notifyHierarchyChanged();
        }
        super.onRestoreInstanceState(groupState.getSuperState());
    }

    /**
     * Interface for PreferenceGroup Adapters to implement so that
     * {@link androidx.preference.PreferenceFragment#scrollToPreference(String)} and
     * {@link androidx.preference.PreferenceFragment#scrollToPreference(Preference)} or
     * {@link PreferenceFragmentCompat#scrollToPreference(String)} and
     * {@link PreferenceFragmentCompat#scrollToPreference(Preference)}
     * can determine the correct scroll position to request.
     */
    public interface PreferencePositionCallback {

        /**
         * Return the adapter position of the first {@link Preference} with the specified key
         * @param key Key of {@link Preference} to find
         * @return Adapter position of the {@link Preference} or
         *         {@link RecyclerView#NO_POSITION} if not found
         */
        int getPreferenceAdapterPosition(String key);

        /**
         * Return the adapter position of the specified {@link Preference} object
         * @param preference {@link Preference} object to find
         * @return Adapter position of the {@link Preference} or
         *         {@link RecyclerView#NO_POSITION} if not found
         */
        int getPreferenceAdapterPosition(Preference preference);
    }

    /**
     * A class for managing the instance state of a {@link PreferenceGroup}.
     */
    static class SavedState extends Preference.BaseSavedState {

        int mInitialExpandedChildrenCount;

        SavedState(Parcel source) {
            super(source);
            mInitialExpandedChildrenCount = source.readInt();
        }

        SavedState(Parcelable superState, int initialExpandedChildrenCount) {
            super(superState);
            mInitialExpandedChildrenCount = initialExpandedChildrenCount;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mInitialExpandedChildrenCount);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
