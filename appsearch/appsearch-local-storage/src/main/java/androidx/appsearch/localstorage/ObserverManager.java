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
import androidx.appsearch.observer.DocumentChangeInfo;
import androidx.appsearch.observer.ObserverCallback;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.observer.SchemaChangeInfo;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.ObjectsCompat;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Manages {@link ObserverCallback} instances and queues notifications to them for later
 * dispatch.
 *
 * <p>This class is thread-safe.
 *
 * @exportToFramework:hide
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
        final ObserverCallback mObserverCallback;
        // Values is a set of document IDs
        volatile Map<DocumentChangeGroupKey, Set<String>> mDocumentChanges = new ArrayMap<>();
        // Keys are database prefixes, values are a set of schema names
        volatile Map<String, Set<String>> mSchemaChanges = new ArrayMap<>();

        ObserverInfo(
                @NonNull CallerAccess listeningPackageAccess,
                @NonNull ObserverSpec observerSpec,
                @NonNull Executor executor,
                @NonNull ObserverCallback observerCallback) {
            mListeningPackageAccess = Preconditions.checkNotNull(listeningPackageAccess);
            mObserverSpec = Preconditions.checkNotNull(observerSpec);
            mExecutor = Preconditions.checkNotNull(executor);
            mObserverCallback = Preconditions.checkNotNull(observerCallback);
        }
    }

    private final Object mLock = new Object();

    /** Maps target packages to ObserverInfos watching something in that package. */
    @GuardedBy("mLock")
    private final Map<String, List<ObserverInfo>> mObserversLocked = new ArrayMap<>();

    private volatile boolean mHasNotifications = false;

    /**
     * Adds an {@link ObserverCallback} to monitor changes within the databases owned by
     * {@code targetPackageName} if they match the given
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
     * @param targetPackageName      The package that owns the data the observerCallback wants to be
     *                               notified for.
     * @param spec                   Describes the kind of data changes the observerCallback should
     *                               trigger for.
     * @param executor               The executor on which to trigger the observerCallback callback
     *                               to deliver notifications.
     * @param observerCallback       The callback to trigger on notifications.
     */
    public void registerObserverCallback(
            @NonNull CallerAccess listeningPackageAccess,
            @NonNull String targetPackageName,
            @NonNull ObserverSpec spec,
            @NonNull Executor executor,
            @NonNull ObserverCallback observerCallback) {
        synchronized (mLock) {
            List<ObserverInfo> infos = mObserversLocked.get(targetPackageName);
            if (infos == null) {
                infos = new ArrayList<>();
                mObserversLocked.put(targetPackageName, infos);
            }
            infos.add(new ObserverInfo(listeningPackageAccess, spec, executor, observerCallback));
        }
    }

    /**
     * Removes all observers that match via {@link ObserverCallback#equals} to the given observer
     * from watching the targetPackageName.
     *
     * <p>Pending notifications queued for this observer, if any, are discarded.
     */
    public void unregisterObserverCallback(
            @NonNull String targetPackageName, @NonNull ObserverCallback observer) {
        synchronized (mLock) {
            List<ObserverInfo> infos = mObserversLocked.get(targetPackageName);
            if (infos == null) {
                return;
            }
            for (int i = 0; i < infos.size(); i++) {
                if (infos.get(i).mObserverCallback.equals(observer)) {
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
            String prefixedSchema =
                    PrefixUtil.createPrefix(packageName, databaseName) + schemaType;
            DocumentChangeGroupKey key = null;
            for (int i = 0; i < allObserverInfosForPackage.size(); i++) {
                ObserverInfo observerInfo = allObserverInfosForPackage.get(i);
                if (!matchesSpec(schemaType, observerInfo.mObserverSpec)) {
                    continue;  // Observer doesn't want this notification
                }
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

    /**
     * Enqueues a change to a schema type for a single observer.
     *
     * <p>The notification will be queued in memory for later dispatch. You must call
     * {@link #dispatchAndClearPendingNotifications} to dispatch all such pending notifications.
     *
     * <p>Note that unlike {@link #onDocumentChange}, the changes reported here are not dropped
     * for observers that don't have visibility. This is because the observer might have had
     * visibility before the schema change, and a final deletion needs to be sent to it. Caller
     * is responsible for checking visibility of these notifications.
     *
     * @param listeningPackageName Name of package that subscribed to notifications and has been
     *                             validated by the caller to have the right access to receive
     *                             this notification.
     * @param targetPackageName Name of package that owns the changed schema types.
     * @param databaseName Database in which the changed schema types reside.
     * @param schemaName Unprefixed name of the changed schema type.
     */
    public void onSchemaChange(
            @NonNull String listeningPackageName,
            @NonNull String targetPackageName,
            @NonNull String databaseName,
            @NonNull String schemaName) {
        synchronized (mLock) {
            List<ObserverInfo> allObserverInfosForPackage = mObserversLocked.get(targetPackageName);
            if (allObserverInfosForPackage == null || allObserverInfosForPackage.isEmpty()) {
                return; // No observers for this type
            }
            // Enqueue changes for later dispatch once the call returns
            String prefix = null;
            for (int i = 0; i < allObserverInfosForPackage.size(); i++) {
                ObserverInfo observerInfo = allObserverInfosForPackage.get(i);
                if (!observerInfo.mListeningPackageAccess.getCallingPackageName()
                        .equals(listeningPackageName)) {
                    continue;  // Not the observer we've been requested to update right now.
                }
                if (!matchesSpec(schemaName, observerInfo.mObserverSpec)) {
                    continue;  // Observer doesn't want this notification
                }
                if (prefix == null) {
                    prefix = PrefixUtil.createPrefix(targetPackageName, databaseName);
                }
                Set<String> changedSchemaNames = observerInfo.mSchemaChanges.get(prefix);
                if (changedSchemaNames == null) {
                    changedSchemaNames = new ArraySet<>();
                    observerInfo.mSchemaChanges.put(prefix, changedSchemaNames);
                }
                changedSchemaNames.add(schemaName);
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

    /**
     * Returns package names of listening packages registered for changes on the given
     * {@code packageName}, {@code databaseName} and unprefixed {@code schemaType}, only if they
     * have access to that type according to the provided {@code visibilityChecker}.
     */
    @NonNull
    public Set<String> getObserversForSchemaType(
            @NonNull String packageName,
            @NonNull String databaseName,
            @NonNull String schemaType,
            @Nullable VisibilityStore visibilityStore,
            @Nullable VisibilityChecker visibilityChecker) {
        synchronized (mLock) {
            List<ObserverInfo> allObserverInfosForPackage = mObserversLocked.get(packageName);
            if (allObserverInfosForPackage == null) {
                return Collections.emptySet();
            }
            Set<String> result = new ArraySet<>();
            String prefixedSchema = PrefixUtil.createPrefix(packageName, databaseName) + schemaType;
            for (int i = 0; i < allObserverInfosForPackage.size(); i++) {
                ObserverInfo observerInfo = allObserverInfosForPackage.get(i);
                if (!matchesSpec(schemaType, observerInfo.mObserverSpec)) {
                    continue;  // Observer doesn't want this notification
                }
                if (!VisibilityUtil.isSchemaSearchableByCaller(
                        /*callerAccess=*/observerInfo.mListeningPackageAccess,
                        /*targetPackageName=*/packageName,
                        /*prefixedSchema=*/prefixedSchema,
                        visibilityStore,
                        visibilityChecker)) {
                    continue;  // Observer can't have this notification.
                }
                result.add(observerInfo.mListeningPackageAccess.getCallingPackageName());
            }
            return result;
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
        Map<String, Set<String>> schemaChanges = observerInfo.mSchemaChanges;
        Map<DocumentChangeGroupKey, Set<String>> documentChanges = observerInfo.mDocumentChanges;
        if (schemaChanges.isEmpty() && documentChanges.isEmpty()) {
            return;
        }
        if (!schemaChanges.isEmpty()) {
            observerInfo.mSchemaChanges = new ArrayMap<>();
        }
        if (!documentChanges.isEmpty()) {
            observerInfo.mDocumentChanges = new ArrayMap<>();
        }

        // Dispatch the pending changes
        observerInfo.mExecutor.execute(() -> {
            // Schema changes
            if (!schemaChanges.isEmpty()) {
                for (Map.Entry<String, Set<String>> entry : schemaChanges.entrySet()) {
                    SchemaChangeInfo schemaChangeInfo = new SchemaChangeInfo(
                            /*packageName=*/PrefixUtil.getPackageName(entry.getKey()),
                            /*databaseName=*/PrefixUtil.getDatabaseName(entry.getKey()),
                            /*changedSchemaNames=*/entry.getValue());

                    try {
                        observerInfo.mObserverCallback.onSchemaChanged(schemaChangeInfo);
                    } catch (Throwable t) {
                        Log.w(TAG, "ObserverCallback threw exception during dispatch", t);
                    }
                }
            }

            // Document changes
            if (!documentChanges.isEmpty()) {
                for (Map.Entry<DocumentChangeGroupKey, Set<String>> entry
                        : documentChanges.entrySet()) {
                    DocumentChangeInfo documentChangeInfo = new DocumentChangeInfo(
                            entry.getKey().mPackageName,
                            entry.getKey().mDatabaseName,
                            entry.getKey().mNamespace,
                            entry.getKey().mSchemaName,
                            entry.getValue());

                    try {
                        observerInfo.mObserverCallback.onDocumentChanged(documentChangeInfo);
                    } catch (Throwable t) {
                        Log.w(TAG, "ObserverCallback threw exception during dispatch", t);
                    }
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
