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

package com.android.support.lifecycle;

import android.support.v4.util.Pair;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ObserverList {
    private boolean mLocked = false;
    private static final Object TO_BE_ADDED = new Object();
    private static final Object TO_BE_REMOVED = new Object();
    private List<Pair<LifecycleObserver, Object>> mPendingModifications = new ArrayList<>(0);
    private List<Pair<LifecycleObserver, GenericLifecycleObserver>> mData = new ArrayList<>(5);

    private static final String ERR_RE_ADD = "Trying to re-add an already existing observer. "
            + "Ignoring the call for ";
    private static final String WARN_RE_REMOVE = "Trying to remove a non-existing observer. ";

    private boolean exists(LifecycleObserver observer) {
        final int size = mData.size();
        for (int i = 0; i < size; i++) {
            Pair<LifecycleObserver, GenericLifecycleObserver> pair = mData.get(i);
            if (pair.first == observer) {
                return true;
            }
        }
        return false;
    }

    private boolean hasPendingRemoval(LifecycleObserver observer) {
        final int size = mPendingModifications.size();
        for (int i = 0; i < size; i++) {
            Pair<LifecycleObserver, Object> pair = mPendingModifications.get(i);
            if (pair.first == observer && pair.second == TO_BE_REMOVED) {
                return true;
            }
        }
        return false;
    }

    void add(LifecycleObserver observer) {
        if (!mLocked) {
            addInternal(observer);
            return;
        }
        mPendingModifications.add(new Pair<>(observer, TO_BE_ADDED));
    }

    private void addInternal(LifecycleObserver observer) {
        if (exists(observer)) {
            Log.e(LifecycleRegistry.TAG, ERR_RE_ADD + observer);
            return;
        }
        GenericLifecycleObserver genericObserver = Lifecycling.getCallback(observer);
        mData.add(new Pair<>(observer, genericObserver));
    }

    void remove(LifecycleObserver observer) {
        if (!mLocked) {
            removeInternal(observer);
            return;
        }
        mPendingModifications.add(new Pair<>(observer, TO_BE_REMOVED));
    }

    private void removeInternal(LifecycleObserver observer) {
        Iterator<Pair<LifecycleObserver, GenericLifecycleObserver>> iterator = mData.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().first == observer) {
                iterator.remove();
                return;
            }
        }
        Log.w(LifecycleRegistry.TAG, WARN_RE_REMOVE + observer);
    }

    void forEach(Callback func) {
        mLocked = true;
        try {
            final int size = mData.size();
            for (int i = 0; i < size; i++) {
                Pair<LifecycleObserver, GenericLifecycleObserver> pair = mData.get(i);
                if (hasPendingRemoval(pair.first)) {
                    continue;
                }
                func.run(pair.second);
            }
        } finally {
            mLocked = false;
            syncPending();
        }
    }

    private void syncPending() {
        // apply everything instead of trying to get clever.
        // it is unlikely for someone to add/rm same item and when that happens, it is important
        // to be consistent in the behavior
        final int size = mPendingModifications.size();
        //noinspection StatementWithEmptyBody
        for (int i = 0; i < size; i++) {
            Pair<LifecycleObserver, Object> pair = mPendingModifications.get(i);
            if (pair.second == TO_BE_REMOVED) {
                removeInternal(pair.first);
            } else { // TO_BE_ADDED
                addInternal(pair.first);
            }
        }
        mPendingModifications.clear();
    }

    int size() {
        return mData.size();
    }

    interface Callback {
        void run(GenericLifecycleObserver observer);
    }

}
