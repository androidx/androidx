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

import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;
import static androidx.recyclerview.selection.Shared.DEBUG;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * OperationMonitor provides a mechanism to coordinate application
 * logic with ongoing user selection activities (such as active band selection
 * and active gesture selection).
 *
 * <p>
 * The host {@link android.app.Activity} or {@link android.app.Fragment} should avoid changing
 * {@link RecyclerView.Adapter Adapter} data while there
 * are active selection operations, as this can result in a poor user experience.
 *
 * <p>
 * To know when an operation is active listen to changes using an {@link OnChangeListener}.
 */
public final class OperationMonitor {

    private static final String TAG = "OperationMonitor";

    private int mNumOps = 0;
    private List<OnChangeListener> mListeners = new ArrayList<>();

    @MainThread
    synchronized void start() {
        mNumOps++;

        if (mNumOps == 1) {
            for (OnChangeListener l : mListeners) {
                l.onChanged();
            }
        }

        if (DEBUG) Log.v(TAG, "Incremented content lock count to " + mNumOps + ".");
    }

    @MainThread
    synchronized void stop() {
        checkState(mNumOps > 0);

        mNumOps--;
        if (DEBUG) Log.v(TAG, "Decremented content lock count to " + mNumOps + ".");

        if (mNumOps == 0) {
            for (OnChangeListener l : mListeners) {
                l.onChanged();
            }
        }
    }

    /**
     * @return true if there are any running operations.
     */
    @SuppressWarnings("unused")
    public synchronized boolean isStarted() {
        return mNumOps > 0;
    }

    /**
     * Registers supplied listener to be notified when operation status changes.
     * @param listener
     */
    public void addListener(@NonNull OnChangeListener listener) {
        checkArgument(listener != null);
        mListeners.add(listener);
    }

    /**
     * Unregisters listener for further notifications.
     * @param listener
     */
    public void removeListener(@NonNull OnChangeListener listener) {
        checkArgument(listener != null);
        mListeners.remove(listener);
    }

    /**
     * Allows other selection code to perform a precondition check asserting the state is locked.
     */
    void checkStarted() {
        checkState(mNumOps > 0);
    }

    /**
     * Allows other selection code to perform a precondition check asserting the state is unlocked.
     */
    void checkStopped() {
        checkState(mNumOps == 0);
    }

    /**
     * Listen to changes in operation status. Authors should avoid
     * changing the Adapter model while there are active operations.
     */
    public interface OnChangeListener {

        /**
         * Called when operation status changes. Call {@link OperationMonitor#isStarted()}
         * to determine the current status.
         */
        void onChanged();
    }
}
