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

package androidx.recyclerview.selection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkState;

import static androidx.recyclerview.selection.Shared.DEBUG;

import android.content.Loader;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.util.Log;

/**
 * ContentLock provides a mechanism to block content from reloading while selection
 * activities like gesture and band selection are active. Clients using live data
 * (data loaded, for example by a {@link Loader}), should route calls to load
 * content through this lock using {@link ContentLock#runWhenUnlocked(Runnable)}.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class ContentLock {

    private static final String TAG = "ContentLock";

    private int mLocks = 0;
    private @Nullable Runnable mCallback;

    /**
     * Increment the block count by 1
     */
    @MainThread
    synchronized void block() {
        mLocks++;
        if (DEBUG) Log.v(TAG, "Incremented content lock count to " + mLocks + ".");
    }

    /**
     * Decrement the block count by 1; If no other object is trying to block and there exists some
     * callback, that callback will be run
     */
    @MainThread
    synchronized void unblock() {
        checkState(mLocks > 0);

        mLocks--;
        if (DEBUG) Log.v(TAG, "Decremented content lock count to " + mLocks + ".");

        if (mLocks == 0 && mCallback != null) {
            mCallback.run();
            mCallback = null;
        }
    }

    /**
     * Attempts to run the given Runnable if not-locked, or else the Runnable is set to be ran next
     * (replacing any previous set Runnables).
     */
    @SuppressWarnings("unused")
    public synchronized void runWhenUnlocked(Runnable runnable) {
        if (mLocks == 0) {
            runnable.run();
        } else {
            mCallback = runnable;
        }
    }

    /**
     * Allows other selection code to perform a precondition check asserting the state is locked.
     */
    void checkLocked() {
        checkState(mLocks > 0);
    }

    /**
     * Allows other selection code to perform a precondition check asserting the state is unlocked.
     */
    void checkUnlocked() {
        checkState(mLocks == 0);
    }
}
