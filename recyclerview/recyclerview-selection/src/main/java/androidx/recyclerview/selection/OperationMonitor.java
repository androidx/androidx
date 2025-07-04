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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.util.Preconditions.checkArgument;
import static androidx.core.util.Preconditions.checkState;
import static androidx.recyclerview.selection.Shared.DEBUG;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * OperationMonitor provides a mechanism to coordinate application
 * logic with ongoing user selection activities (such as active band selection
 * and active gesture selection).
 *
 * <p>
 * The host {@link android.app.Activity} or {@link android.app.Fragment} should avoid changing
 * {@link androidx.recyclerview.widget.RecyclerView.Adapter Adapter} data while there
 * are active selection operations, as this can result in a poor user experience.
 *
 * <p>
 * To know when an operation is active listen to changes using an {@link OnChangeListener}.
 */
public final class OperationMonitor {

    private static final String TAG = "OperationMonitor";

    private final List<OnChangeListener> mListeners = new ArrayList<>();

    // Ideally OperationMonitor would implement Resettable
    // directly, but Metalava couldn't understand that
    // `OperationMonitor` was public API while `Resettable` was
    // not. This is our clever workaround :)
    private final Resettable mResettable = new Resettable() {

        @Override
        public boolean isResetRequired() {
            return OperationMonitor.this.isResetRequired();
        }

        @Override
        public void reset() {
            OperationMonitor.this.reset();
        }
    };

    private int mNumOps = 0;

    private final Object mLock = new Object();

    @MainThread
    void start() {
        synchronized (mLock) {
            mNumOps++;

            if (mNumOps == 1) {
                notifyStateChanged();
            }

            if (DEBUG) Log.v(TAG, "Incremented content lock count to " + mNumOps + ".");
        }
    }

    @MainThread
    void stop() {
        synchronized (mLock) {
            if (mNumOps == 0) {
                if (DEBUG) Log.w(TAG, "Stop called whith opt count of 0.");
                return;
            }

            mNumOps--;
            if (DEBUG) Log.v(TAG, "Decremented content lock count to " + mNumOps + ".");

            if (mNumOps == 0) {
                notifyStateChanged();
            }
        }
    }

    @RestrictTo(LIBRARY)
    @MainThread
    void reset() {
        synchronized (mLock) {
            if (DEBUG) Log.d(TAG, "Received reset request.");
            if (mNumOps > 0) {
                Log.w(TAG, "Resetting OperationMonitor with " + mNumOps + " active operations.");
            }
            mNumOps = 0;
            notifyStateChanged();
        }
    }

    @RestrictTo(LIBRARY)
    boolean isResetRequired() {
        synchronized (mLock) {
            return isStarted();
        }
    }

    /**
     * @return true if there are any running operations.
     */
    public boolean isStarted() {
        synchronized (mLock) {
            return mNumOps > 0;
        }
    }

    /**
     * Registers supplied listener to be notified when operation status changes.
     */
    public void addListener(@NonNull OnChangeListener listener) {
        checkArgument(listener != null);
        mListeners.add(listener);
    }

    /**
     * Unregisters listener for further notifications.
     */
    public void removeListener(@NonNull OnChangeListener listener) {
        checkArgument(listener != null);
        mListeners.remove(listener);
    }

    /**
     * Allows other selection code to perform a precondition check asserting the state is locked.
     */
    void checkStarted(boolean started) {
        if (started) {
            checkState(mNumOps > 0);
        } else {
            checkState(mNumOps == 0);
        }
    }

    private void notifyStateChanged() {
        for (OnChangeListener l : mListeners) {
            l.onChanged();
        }
    }

    /**
     * Work around b/139109223.
     */
    @RestrictTo(LIBRARY)
    @NonNull Resettable asResettable() {
        return mResettable;
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
