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

package androidx.appsearch.localstorage;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.localstorage.visibilitystore.VisibilityStore;
import androidx.appsearch.localstorage.visibilitystore.VisibilityUtil;
import androidx.appsearch.observer.AppSearchObserverCallback;
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverSpec;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Manages {@link AppSearchObserverCallback} instances and queues notifications to them for later
 * dispatch.
 *
 * <p>This class is thread-safe.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ObserverManager {
    private static final String TAG = "AppSearchObserverManage";

    /** The combination of fields by which {@link DocumentChangeInfo} is grouped. */
    private static final class DocumentChangeGroupKey {
        final String mPackageName;
        final String mDatabaseName;
        final String mNamespace;
        final String mSchemaName;

        DocumentChangeGroupKey(
                @NonNull String packageName,
                @NonNull String databaseName,
                @NonNull String namespace,
                @NonNull String schemaName) {
            mPackageName = Preconditions.checkNotNull(packageName);
            mDatabaseName = Preconditions.checkNotNull(databaseName);
            mNamespace = Preconditions.checkNotNull(namespace);
            mSchemaName = Preconditions.checkNotNull(schemaName);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof DocumentChangeGroupKey)) return false;
            DocumentChangeGroupKey that = (DocumentChangeGroupKey) o;
            return mPackageName.equals(that.mPackageName)
                    && mDatabaseName.equals(that.mDatabaseName)
                    && mNamespace.equals(that.mNamespace)
                    && mSchemaName.equals(that.mSchemaName);
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(mPackageName, mDatabaseName, mNamespace, mSchemaName);
        }
    }

    private static final class ObserverInfo {
        /** The package which registered the observer. */
        final CallerAccess mListeningPackageAccess;
        final ObserverSpec mObserverSpec;
        final Executor mExecutor;
        final AppSearchObserverCallback mObserver;
        // Values is a set of document IDs
        volatile Map<DocumentChangeGroupKey, Set<String>> mDocumentChanges = new ArrayMap<>();

        ObserverInfo(
                @NonNull CallerAccess listeningPackageAccess,
                @NonNull ObserverSpec observerSpec,
                @NonNull Executor executor,
                @NonNull AppSearchObserverCallback observer) {
            mListeningPackageAccess = Preconditions.checkNotNull(listeningPackageAccess);
            mObserverSpec = Preconditions.checkNotNull(observerSpec);
            mExecutor = Preconditions.checkNotNull(executor);
            mObserver = Preconditions.checkNotNull(observer);
        }
    }

    private final Object mLock = new Object();

    /** Maps observed package to observer infos watching something in that package. */
    @GuardedBy("mLock")
    private final Map<String, List<ObserverInfo>> mObserversLocked = new ArrayMap<>();

    private volatile boolean mHasNotifications = false;

    /**
     * Adds an {@link AppSearchObserverCallback} to monitor changes within the
     * databases owned by {@code targetPackageName} if they match the given
     * {@link androidx.appsearch.observer.ObserverSpec}.
     *
     * <p>If the data owned by {@code targetPackageName} is not visible to you, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} changes its schema visibility settings.
     *
     * <p>If no package matching {@code targetPackageName} exists on the system, the registration
     * call will succeed but no notifications will be dispatched. Notifications could start flowing
     * later if {@code targetPackageName} is installed and starts indexing data.
     *
     * <p>Note that this method does not take the standard read/write lock that guards I/O, so it
     * will not queue behind I/O. Therefore it is safe to call from any thread including UI or
     * binder threads.
     *
     * @param listeningPackageAccess Visibility information about the app that wants to receive
     *                               notifications.
     * @param targetPackageName      The package that owns the data the observer wants to be
     *                               notified for.
     * @param spec                   Describes the kind of data changes the observer should trigger
     *                               for.
     * @param executor               The executor on which to trigger the observer callback to
     *                               deliver notifications.
     * @param observer               The callback to trigger on notifications.
     */
    public void addObserver(
            @NonNull CallerAccess listeningPackageAccess,
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull AppSearchObserverCallback observer) {
        synchronized (mLock) {
            List<ObserverInfo> infos = mObserversLocked.get(targetPackageName);
            if (infos == null) {
                infos = new ArrayList<>();
                mObserversLocked.put(targetPackageName, infos);
            }
            infos.add(new ObserverInfo(listeningPackageAccess, spec, executor, observer));
        }
    }

    /**
     * Removes all observers that match via {@link AppSearchObserverCallback#equals} to the given
     * observer from watching the targetPackageName.
     *
     * <p>Pending notifications queued for this observer, if any, are discarded.
     */
    public void removeObserver(
            @NonNull String targetPackageName, @NonNull AppSearchObserverCallback observer) {
        synchronized (mLock) {
            List<ObserverInfo> infos = mObserversLocked.get(targetPackageName);
            if (infos == null) {
                return;
            }
            for (int i = 0; i < infos.size(); i++) {
                if (infos.get(i).mObserver.equals(observer)) {
                    infos.remove(i);
                    i--;
                }
            }
        }
    }

    /**
     * Should be called when a change occurs to a document.
     *
     * <p>The notification will be queued in memory for later dispatch. You must call
     * {@link #dispatchAndClearPendingNotifications} to dispatch all such pending notifications.
     *
     * @param visibilityStore   Store for visibility information. If not provided, only access to
     *                          own data will be allowed.
     * @param visibilityChecker Checker for visibility access. If not provided, only access to own
     *                          data will be allowed.
     */
    public void onDocumentChange(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String namespace,
            @NonNull String schemaType,
            @NonNull String documentId,
            @Nullable VisibilityStore visibilityStore,
            @Nullable VisibilityChecker visibilityChecker) {
        synchronized (mLock) {
            List<ObserverInfo> allObserverInfosForPackage = mObserversLocked.get(packageName);
            if (allObserverInfosForPackage == null || allObserverInfosForPackage.isEmpty()) {
                return; // No observers for this type
            }
            // Enqueue changes for later dispatch once the call returns
            DocumentChangeGroupKey key = null;
            for (int i = 0; i < allObserverInfosForPackage.size(); i++) {
                ObserverInfo observerInfo = allObserverInfosForPackage.get(i);
                if (!matchesSpec(schemaType, observerInfo.mObserverSpec)) {
                    continue;  // Observer doesn't want this notification
                }
                String prefixedSchema =
                        PrefixUtil.createPrefix(packageName, databaseName)
                                + schemaType;
                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        /*callerAccess=*/observerInfo.mListeningPackageAccess,
                        /*targetPackageName=*/packageName,
                        /*prefixedSchema=*/prefixedSchema,
                        visibilityStore,
                        visibilityChecker)) {
                    continue;  // Observer can't have this notification.
                }
                if (key == null) {
                    key = new DocumentChangeGroupKey(
                            packageName, databaseName, namespace, schemaType);
                }
                Set<String> changedDocumentIds = observerInfo.mDocumentChanges.get(key);
                if (changedDocumentIds == null) {
                    changedDocumentIds = new ArraySet<>();
                    observerInfo.mDocumentChanges.put(key, changedDocumentIds);
                }
                changedDocumentIds.add(documentId);
            }
            mHasNotifications = true;
        }
    }

    /** Returns whether there are any observers registered to watch the given package. */
    public boolean isPackageObserved(@NonNull String packageName) {
        synchronized (mLock) {
            return mObserversLocked.containsKey(packageName);
        }
    }

    /**
     * Returns whether there are any observers registered to watch the given package and
     * unprefixed schema type.
     */
    public boolean isSchemaTypeObserved(@NonNull String packageName, @NonNull String schemaType) {
        synchronized (mLock) {
            List<ObserverInfo> allObserverInfosForPackage = mObserversLocked.get(packageName);
            if (allObserverInfosForPackage == null) {
                return false;
            }
            for (int i = 0; i < allObserverInfosForPackage.size(); i++) {
                ObserverInfo observerInfo = allObserverInfosForPackage.get(i);
                if (matchesSpec(schemaType, observerInfo.mObserverSpec)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Returns whether any notifications have been queued for dispatch. */
    public boolean hasNotifications() {
        return mHasNotifications;
    }

    /** Dispatches notifications on their corresponding executors. */
    public void dispatchAndClearPendingNotifications() {
        if (!mHasNotifications) {
            return;
        }
        synchronized (mLock) {
            if (mObserversLocked.isEmpty() || !mHasNotifications) {
                return;
            }
            for (List<ObserverInfo> observerInfos : mObserversLocked.values()) {
                for (int i = 0; i < observerInfos.size(); i++) {
                    dispatchAndClearPendingNotificationsLocked(observerInfos.get(i));
                }
            }
            mHasNotifications = false;
        }
    }

    /** Dispatches pending notifications for the given observerInfo and clears the pending list. */
    @GuardedBy("mLock")
    private void dispatchAndClearPendingNotificationsLocked(@NonNull ObserverInfo observerInfo) {
        // Get and clear the pending changes
        Map<DocumentChangeGroupKey, Set<String>> documentChanges = observerInfo.mDocumentChanges;
        if (documentChanges.isEmpty()) {
            return;
        }
        observerInfo.mDocumentChanges = new ArrayMap<>();

        // Dispatch the pending changes
        observerInfo.mExecutor.execute(() -> {
            for (Map.Entry<DocumentChangeGroupKey, Set<String>> entry
                    : documentChanges.entrySet()) {
                DocumentChangeInfo documentChangeInfo = new DocumentChangeInfo(
                        entry.getKey().mPackageName,
                        entry.getKey().mDatabaseName,
                        entry.getKey().mNamespace,
                        entry.getKey().mSchemaName,
                        entry.getValue());

                try {
                    // TODO(b/193494000): Add code to dispatch SchemaChangeInfo too.
                    observerInfo.mObserver.onDocumentChanged(documentChangeInfo);
                } catch (Throwable t) {
                    Log.w(TAG, "AppSearchObserverCallback threw exception during dispatch", t);
                }
            }
        });
    }

    /**
     * Checks whether a change in the given {@code databaseName}, {@code namespace} and
     * {@code schemaType} passes all the filters defined in the given {@code observerSpec}.
     *
     * <p>Note that this method does not check packageName; you must only use it to check
     * observerSpecs which you know are observing the same package as the change.
     */
    private static boolean matchesSpec(
            @NonNull String schemaType, @NonNull ObserverSpec observerSpec) {
        Set<String> schemaFilters = observerSpec.getFilterSchemas();
        return schemaFilters.isEmpty() || schemaFilters.contains(schemaType);
    }
}
