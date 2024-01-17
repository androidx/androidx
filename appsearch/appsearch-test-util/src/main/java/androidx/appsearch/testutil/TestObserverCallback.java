/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appsearch.testutil;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.SchemaChangeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link androidx.appsearch.observer.ObserverCallback} for testing
 * that caches its notifications in memory.
 *
 * <p>You should wait for all notifications to be delivered using {@link #waitForNotificationCount}
 * before using the public lists to avoid concurrency issues.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TestObserverCallback implements ObserverCallback {
    private final Object mLock = new Object();

    private final List<SchemaChangeInfo> mSchemaChanges = new ArrayList<>();
    private final List<DocumentChangeInfo> mDocumentChanges = new ArrayList<>();

    @GuardedBy("mLock")
    private int mNotificationCountLocked = 0;

    @Override
    public void onSchemaChanged(@NonNull SchemaChangeInfo changeInfo) {
        synchronized (mLock) {
            mSchemaChanges.add(changeInfo);
            incrementNotificationCountLocked();
        }
    }

    @Override
    public void onDocumentChanged(@NonNull DocumentChangeInfo changeInfo) {
        synchronized (mLock) {
            mDocumentChanges.add(changeInfo);
            incrementNotificationCountLocked();
        }
    }

    /**
     * Waits for the total number of notifications this observer has seen to be equal to
     * {@code expectedCount}.
     *
     * <p>If the number of cumulative received notifications is currently less than
     * {@code expectedCount}, this method will block.
     *
     * @throws IllegalStateException If the current count of received notifications is currently
     *                               greater than {@code expectedCount}.
     */
    public void waitForNotificationCount(int expectedCount) throws InterruptedException {
        while (true) {
            synchronized (mLock) {
                int actualCount = mNotificationCountLocked;
                if (actualCount > expectedCount) {
                    throw new IllegalStateException(
                            "Caller was waiting for "
                                    + expectedCount
                                    + " notifications but there are"
                                    + " already "
                                    + actualCount
                                    + ".\n  Schema changes: "
                                    + mSchemaChanges
                                    + "\n  Document changes: "
                                    + mDocumentChanges);
                } else if (actualCount == expectedCount) {
                    return;
                } else {
                    mLock.wait();
                }
            }
        }
    }

    /** Gets all schema changes that have been observed so far. */
    // Note: although these are lists, their order is arbitrary and depends on the order in which
    // the executor provided to GlobalSearchSession#adObserver dispatches the notifications.
    // Therefore they are declared as iterables instead of lists, to reduce the risk that they will
    // be inspected by index.
    @NonNull
    public Iterable<SchemaChangeInfo> getSchemaChanges() {
        return mSchemaChanges;
    }

    /** Gets all document changes that have been observed so far. */
    // Note: although these are lists, their order is arbitrary and depends on the order in which
    // the executor provided to GlobalSearchSession#adObserver dispatches the notifications.
    // Therefore they are declared as iterables instead of lists, to reduce the risk that they will
    // be inspected by index.
    @NonNull
    public Iterable<DocumentChangeInfo> getDocumentChanges() {
        return mDocumentChanges;
    }

    /** Removes all notifications captured by this callback and resets the count to 0. */
    public void clear() {
        synchronized (mLock) {
            mSchemaChanges.clear();
            mDocumentChanges.clear();
            mNotificationCountLocked = 0;
            mLock.notifyAll();
        }
    }

    private void incrementNotificationCountLocked() {
        synchronized (mLock) {
            mNotificationCountLocked++;
            mLock.notifyAll();
        }
    }
}
