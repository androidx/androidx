/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.View;

/**
 * Maintains a bundle of states for a group of views. Each view must have a unique id to identify
 * it. There are four different strategies {@link #SAVE_NO_CHILD} {@link #SAVE_ON_SCREEN_CHILD}
 * {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}.
 * <p>
 * This class serves purpose of nested "listview" e.g.  a vertical list of horizontal list.
 * Vertical list maintains id->bundle mapping of all it's children (even the children is offscreen
 * and being pruned).
 * <p>
 * The class is currently used within {@link GridLayoutManager}, but it might be used by other
 * ViewGroup.
 */
class ViewsStateBundle {

    /** dont save states of any child views */
    public static final int SAVE_NO_CHILD = 0;
    /** only save on screen child views, the states are lost when they become off screen */
    public static final int SAVE_ON_SCREEN_CHILD = 1;
    /** save on screen views plus save off screen child views states up to {@link #getLimitNumber()} */
    public static final int SAVE_LIMITED_CHILD = 2;
    /**
     * save on screen views plus save off screen child views without any limitation. This might cause out
     * of memory, only use it when you are dealing with limited data
     */
    public static final int SAVE_ALL_CHILD = 3;

    public static final int DEFAULT_LIMIT = 100;

    private int mSavePolicy;
    private int mLimitNumber;

    private final Bundle mChildStates;

    public ViewsStateBundle(int policy, int limit) {
        mSavePolicy = policy;
        mLimitNumber = limit;
        mChildStates = new Bundle();
    }

    public void clear() {
        mChildStates.clear();
    }

    /**
     * @return the saved views states
     */
    public final Bundle getChildStates() {
        return mChildStates;
    }

    /**
     * @return the savePolicy, see {@link #SAVE_NO_CHILD} {@link #SAVE_ON_SCREEN_CHILD}
     *         {@link #SAVE_LIMITED_CHILD} {@link #SAVE_ALL_CHILD}
     */
    public final int getSavePolicy() {
        return mSavePolicy;
    }

    /**
     * @return the limitNumber, only works when {@link #getSavePolicy()} is
     *         {@link #SAVE_LIMITED_CHILD}
     */
    public final int getLimitNumber() {
        return mLimitNumber;
    }

    /**
     * @see ViewsStateBundle#getSavePolicy()
     */
    public final void setSavePolicy(int savePolicy) {
        this.mSavePolicy = savePolicy;
    }

    /**
     * @see ViewsStateBundle#getLimitNumber()
     */
    public final void setLimitNumber(int limitNumber) {
        this.mLimitNumber = limitNumber;
    }

    /**
     * Load view from states, it's none operation if the there is no state associated with the id.
     *
     * @param view view where loads into
     * @param id unique id for the view within this ViewsStateBundle
     */
    public final void loadView(View view, int id) {
        String key = getSaveStatesKey(id);
        SparseArray<Parcelable> container = mChildStates.getSparseParcelableArray(key);
        if (container != null) {
            view.restoreHierarchyState(container);
        }
    }

    /**
     * Save views regardless what's the current policy is.
     *
     * @param view view to save
     * @param id unique id for the view within this ViewsStateBundle
     */
    protected final void saveViewUnchecked(View view, int id) {
        String key = getSaveStatesKey(id);
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        view.saveHierarchyState(container);
        mChildStates.putSparseParcelableArray(key, container);
    }

    /**
     * The on screen view is saved when policy is not {@link #SAVE_NO_CHILD}.
     *
     * @param view
     * @param id
     */
    public final void saveOnScreenView(View view, int id) {
        if (mSavePolicy != SAVE_NO_CHILD) {
            saveViewUnchecked(view, id);
        }
    }

    /**
     * Save off screen views according to policy.
     *
     * @param view view to save
     * @param id unique id for the view within this ViewsStateBundle
     */
    public final void saveOffscreenView(View view, int id) {
        switch (mSavePolicy) {
            case SAVE_LIMITED_CHILD:
                if (mChildStates.size() > mLimitNumber) {
                    // TODO prune the Bundle to be under limit
                }
                // slip through next case section to save view
            case SAVE_ALL_CHILD:
                saveViewUnchecked(view, id);
                break;
            default:
                break;
        }
    }

    static String getSaveStatesKey(int id) {
        return Integer.toString(id);
    }
}
