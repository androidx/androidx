/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appfunctions.internal

import android.os.Build
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionName
import androidx.appfunctions.AppFunctionsChangeEvent
import androidx.appfunctions.internal.AppFunctionLibraryConfiguration.isLoggingEnabled
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_NAMESPACE
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_RUNTIME_NAMESPACE
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.APP_FUNCTIONS_STATIC_DATABASE_NAME
import androidx.appfunctions.internal.AppSearchAppFunctionReader.Companion.OBSERVER_DEBOUNCE_MILLIS
import androidx.appfunctions.internal.ChangeEventsFlowUtils.debounceAndMerge
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionMetadataDocument
import androidx.appsearch.observer.DocumentChangeInfo
import androidx.appsearch.observer.ObserverCallback
import androidx.appsearch.observer.SchemaChangeInfo
import java.io.Closeable
import kotlin.time.Duration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * An [ObserverCallback] that translates AppSearch schema and document change notifications into
 * debounced [AppFunctionsChangeEvent]s.
 *
 * When observing AppSearch directly from the caller's process (used on SDK < 37 via
 * [AppSearchAppFunctionReader]), AppSearch configures visibility for
 * `AppFunctionStaticMetadata-<pkg>` using `setPubliclyVisibleSchema`, which requires `<pkg>` to
 * still be installed in `PackageManager` when evaluating observer visibility. Consequently, when a
 * package is uninstalled, AppSearch filters out non-system observers and does not deliver
 * [onSchemaChanged] or [onDocumentChanged] for the removed package.
 *
 * To match the platform behavior on SDK 37+ of notifying callers when a package providing
 * AppFunctions is uninstalled, this class tracks [knownAppFunctionPackages] (seeded via
 * [seedKnownPackages] and updated as new AppFunction schemas/documents are indexed) and emits
 * [AppFunctionsChangeEvent.MetadataChanged] when [onPackageRemoved] is invoked for a known package.
 *
 * To avoid missing events, callers should register this callback with AppSearch and the package
 * removal source *before* querying the initial set of known packages, then pass the result to
 * [seedKnownPackages]. Packages removed between registration and seeding are excluded from the seed
 * so that a stale snapshot cannot resurrect them.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal class AppFunctionObserverCallback(
    private val debounceDuration: Duration = OBSERVER_DEBOUNCE_MILLIS
) : ObserverCallback, Closeable {
    private val lock = Any()
    @GuardedBy("lock") private val knownAppFunctionPackages = mutableSetOf<String>()
    @GuardedBy("lock") private var isSeeded = false
    @GuardedBy("lock") private val packagesRemovedBeforeSeed = mutableSetOf<String>()

    private val changeEvents = Channel<AppFunctionsChangeEvent>(Channel.UNLIMITED)

    override fun onSchemaChanged(schemaChangeInfo: SchemaChangeInfo) {
        if (!isAppFunctionDb(schemaChangeInfo.getDatabaseName())) {
            return
        }
        val changedPackages =
            schemaChangeInfo.changedSchemaNames
                .mapNotNull { schemaName ->
                    AppFunctionMetadataDocument.getPackageFromSchemaName(schemaName)
                }
                .distinct()
                .toSet()
        if (changedPackages.isNotEmpty()) {
            // AppSearch never delivers onSchemaChanged for the uninstall itself (the visibility
            // check fails once the package is removed). However, a change made shortly before an
            // uninstall may still be dispatched after onPackageRemoved, since the two callbacks
            // arrive on different threads. That can leave a stale entry here, which at worst
            // causes one extra MetadataChanged event later.
            synchronized(lock) {
                knownAppFunctionPackages.addAll(changedPackages)
                if (!isSeeded) {
                    packagesRemovedBeforeSeed.removeAll(changedPackages)
                }
            }
            changeEvents.trySend(AppFunctionsChangeEvent.MetadataChanged(changedPackages))
        }
    }

    override fun onDocumentChanged(docChangeInfo: DocumentChangeInfo) {
        val isStatic = isStaticDbChange(docChangeInfo)
        val isRuntime = isRuntimeDbChange(docChangeInfo)
        if (!isStatic && !isRuntime) {
            return
        }
        val changedFunctions =
            docChangeInfo.changedDocumentIds
                .mapNotNull { docId ->
                    try {
                        AppFunctionName.fromQualifiedId(docId)
                    } catch (e: IllegalArgumentException) {
                        if (isLoggingEnabled) {
                            Log.w(
                                APP_FUNCTIONS_TAG,
                                "Failed to parse changed function ID $docId",
                                e,
                            )
                        }
                        null
                    }
                }
                .distinct()
                .toSet()
        if (changedFunctions.isNotEmpty()) {
            val changedPackages = changedFunctions.map { it.packageName }.toSet()
            // See onSchemaChanged for why adding to knownAppFunctionPackages here is safe.
            synchronized(lock) {
                knownAppFunctionPackages.addAll(changedPackages)
                if (!isSeeded) {
                    packagesRemovedBeforeSeed.removeAll(changedPackages)
                }
            }
            if (isStatic) {
                changeEvents.trySend(AppFunctionsChangeEvent.MetadataChanged(changedPackages))
            }
            if (isRuntime) {
                changeEvents.trySend(AppFunctionsChangeEvent.StatesChanged(changedFunctions))
            }
        }
    }

    /**
     * Seeds the set of known packages that provide AppFunctions.
     *
     * Any package reported via [onPackageRemoved] before this call is excluded from [packages],
     * since the snapshot may have been taken before the removal.
     */
    fun seedKnownPackages(packages: Set<String>) {
        val removedKnownPackages =
            synchronized(lock) {
                if (isSeeded) return
                val removed =
                    (packages intersect packagesRemovedBeforeSeed) - knownAppFunctionPackages
                knownAppFunctionPackages.addAll(packages - packagesRemovedBeforeSeed)
                packagesRemovedBeforeSeed.clear()
                isSeeded = true
                removed
            }
        // These packages were removed before seeding, so the caller's snapshot is stale.
        if (removedKnownPackages.isNotEmpty()) {
            changeEvents.trySend(AppFunctionsChangeEvent.MetadataChanged(removedKnownPackages))
        }
    }

    /**
     * Notifies the observer that [packageName] has been uninstalled from the device.
     *
     * If [packageName] is in the set of known packages that provide AppFunctions, removes it from
     * the known set and emits an [AppFunctionsChangeEvent.MetadataChanged] event. Packages that
     * never provided AppFunctions are ignored.
     */
    fun onPackageRemoved(packageName: String) {
        if (packageName.isEmpty()) {
            return
        }
        val wasKnown =
            synchronized(lock) {
                if (!isSeeded) {
                    packagesRemovedBeforeSeed.add(packageName)
                }
                knownAppFunctionPackages.remove(packageName)
            }
        if (wasKnown) {
            changeEvents.trySend(AppFunctionsChangeEvent.MetadataChanged(setOf(packageName)))
        }
    }

    public fun observe(): Flow<AppFunctionsChangeEvent> =
        changeEvents.receiveAsFlow().debounceAndMerge(debounceDuration)

    override fun close() {
        changeEvents.close()
    }

    private companion object {
        private fun isStaticDbChange(changeInfo: DocumentChangeInfo): Boolean =
            changeInfo.databaseName == APP_FUNCTIONS_STATIC_DATABASE_NAME &&
                changeInfo.namespace == APP_FUNCTIONS_NAMESPACE

        private fun isRuntimeDbChange(changeInfo: DocumentChangeInfo): Boolean =
            changeInfo.databaseName == APP_FUNCTIONS_RUNTIME_DATABASE_NAME &&
                changeInfo.namespace == APP_FUNCTIONS_RUNTIME_NAMESPACE

        private fun isAppFunctionDb(databaseName: String): Boolean =
            databaseName == APP_FUNCTIONS_STATIC_DATABASE_NAME ||
                databaseName == APP_FUNCTIONS_RUNTIME_DATABASE_NAME
    }
}
