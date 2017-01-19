/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.support.apptoolkit.internal;

import android.support.annotation.RestrictTo;
import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A set class that can keep a list of items and allows modifications while traversing.
 * It is NOT thread safe.
 * @param <T> Item type
 * @hide
 */
// TODO this class is doing more than it should. Needs cleanup / change / removal.
// also, see InvalidationTracker.SyncObserverSet where we need a sync version.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ObserverSet<T> {
    private static final String TAG = "ObserverSet";
    private int mLockCounter = 0;

    private boolean mPendingSyncRequest = false;

    private static final Object TO_BE_ADDED = new Object();

    private static final Object TO_BE_REMOVED = new Object();

    private List<Pair<T, Object>> mPendingModifications = new ArrayList<>(0);

    private List<T> mData = new ArrayList<>(5);

    private static final String ERR_RE_ADD = "Trying to re-add an already existing observer. "
            + "Ignoring the call for ";

    private static final String WARN_RE_REMOVE = "Trying to remove a non-existing observer. ";

    /**
     * Return true if the set is locked, false otherwise.
     * @return True if set is locked, false otherwise.
     */
    public boolean isLocked() {
        return mLockCounter > 0;
    }

    /**
     * Can be called to enqueue an onSync call. Useful if owner does not want to make changes while
     * the Set is locked.
     */
    public void invokeSyncOnUnlock() {
        mPendingSyncRequest = true;
    }

    private boolean exists(T observer) {
        final int size = mData.size();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            if (checkEquality(mData.get(i), observer)) {
                return true;
            }
        }
        return false;
    }

    protected abstract boolean checkEquality(T existing, T added);

    private boolean hasPendingRemoval(T observer) {
        final int size = mPendingModifications.size();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            Pair<T, Object> pair = mPendingModifications.get(i);
            if (checkEquality(pair.first, observer) && pair.second == TO_BE_REMOVED) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the given observer to the set if it does not already exists.
     *
     * @param observer The observer to add.
     */
    public void add(T observer) {
        if (mLockCounter < 1) {
            addInternal(observer);
            return;
        }
        mPendingModifications.add(new Pair<>(observer, TO_BE_ADDED));
    }

    private void addInternal(T observer) {
        if (exists(observer)) {
            Log.e(TAG, ERR_RE_ADD + observer);
            return;
        }
        mData.add(observer);
        onAdded(observer);
    }

    protected void onAdded(T observer) {

    }

    /**
     * Removes the given observer from the set.
     *
     * @param observer The observer to remove.
     */
    public void remove(T observer) {
        if (mLockCounter < 1) {
            removeInternal(observer);
            return;
        }
        mPendingModifications.add(new Pair<>(observer, TO_BE_REMOVED));
    }

    private void removeInternal(T observer) {
        Iterator<T> iterator = mData.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (checkEquality(next, observer)) {
                iterator.remove();
                onRemoved(next);
                return;
            }
        }
        Log.w(TAG, WARN_RE_REMOVE + observer);
    }

    protected void onRemoved(T item) {
    }

    /**
     * Run an action on each item in the set.
     * <p>
     * If set changes during this function, the changes are not reflected until forEach returns.
     *
     * @param func The function to execute on each item.
     */
    public void forEach(Callback<T> func) {
        mLockCounter++;
        try {
            final int size = mData.size();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < size; i++) {
                T item = mData.get(i);
                if (hasPendingRemoval(item)) {
                    continue;
                }
                func.run(item);
            }
        } finally {
            mLockCounter--;
            if (mLockCounter == 0) {
                syncPending();
            }
        }
    }

    private void syncPending() {
        // apply everything instead of trying to get clever.
        // it is unlikely for someone to add/rm same item and when that happens, it is important
        // to be consistent in the behavior
        final int size = mPendingModifications.size();
        //noinspection StatementWithEmptyBody,ForLoopReplaceableByForEach
        for (int i = 0; i < size; i++) {
            Pair<T, Object> pair = mPendingModifications.get(i);
            if (pair.second == TO_BE_REMOVED) {
                removeInternal(pair.first);
            } else { // TO_BE_ADDED
                addInternal(pair.first);
            }
        }
        mPendingModifications.clear();
        if (size > 0 || mPendingSyncRequest) {
            mPendingSyncRequest = false;
            onSync();
        }
    }

    // called after pending changes are applied to the list
    protected void onSync() {
    }

    /**
     * The size of the list.
     * @return The size of the list.
     */
    public int size() {
        return mData.size();
    }

    /**
     * @hide
     * @param <T> Type T of the items in the list
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Callback<T> {
        /**
         * Evaluate an action on an item in the set.
         *
         * @param key The item in the set.
         */
        void run(T key);
    }
}
