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

package androidx.work.impl

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.os.UserManagerCompat
import androidx.lifecycle.LiveData
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkRequest
import com.google.common.util.concurrent.ListenableFuture
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * A proxy wrapper for [WorkManager] that defers creation of [WorkManagerImpl] until the user is
 * unlocked or the first public API is invoked.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class WorkManagerWrapper
internal constructor(
    context: Context,
    private val implSingletonSupplier: () -> WorkManagerImpl,
) : WorkManager() {

    private val context: Context = context.applicationContext

    override val configuration: Configuration
        get() = getWorkManagerImpl().configuration

    init {
        tryInitializeIfUnlocked()
    }

    /** Single choke point for accessing the internal [WorkManagerImpl]. */
    private fun getWorkManagerImpl(): WorkManagerImpl {
        return implSingletonSupplier()
    }

    private fun tryInitializeIfUnlocked() {
        // TODO(b/514165396): Add a user unlock receiver to try to initialize in case the device is
        //  not currently unlocked.
        if (UserManagerCompat.isUserUnlocked(context)) {
            getWorkManagerImpl()
        }
    }

    override fun enqueue(requests: List<WorkRequest>): Operation {
        return getWorkManagerImpl().enqueue(requests)
    }

    override fun beginWith(requests: List<OneTimeWorkRequest>): WorkContinuation {
        return getWorkManagerImpl().beginWith(requests)
    }

    override fun beginUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ): WorkContinuation {
        return getWorkManagerImpl().beginUniqueWork(uniqueWorkName, existingWorkPolicy, requests)
    }

    override fun enqueueUniqueWork(
        uniqueWorkName: String,
        existingWorkPolicy: ExistingWorkPolicy,
        requests: List<OneTimeWorkRequest>,
    ): Operation {
        return getWorkManagerImpl().enqueueUniqueWork(uniqueWorkName, existingWorkPolicy, requests)
    }

    override fun enqueueUniquePeriodicWork(
        uniqueWorkName: String,
        existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy,
        request: PeriodicWorkRequest,
    ): Operation {
        return getWorkManagerImpl()
            .enqueueUniquePeriodicWork(uniqueWorkName, existingPeriodicWorkPolicy, request)
    }

    override fun cancelWorkById(id: UUID): Operation {
        return getWorkManagerImpl().cancelWorkById(id)
    }

    override fun cancelAllWorkByTag(tag: String): Operation {
        return getWorkManagerImpl().cancelAllWorkByTag(tag)
    }

    override fun cancelUniqueWork(uniqueWorkName: String): Operation {
        return getWorkManagerImpl().cancelUniqueWork(uniqueWorkName)
    }

    override fun cancelAllWork(): Operation {
        return getWorkManagerImpl().cancelAllWork()
    }

    override fun createCancelPendingIntent(id: UUID): PendingIntent {
        return getWorkManagerImpl().createCancelPendingIntent(id)
    }

    override fun getLastCancelAllTimeMillisLiveData(): LiveData<Long> {
        return getWorkManagerImpl().getLastCancelAllTimeMillisLiveData()
    }

    override fun getLastCancelAllTimeMillis(): ListenableFuture<Long> {
        return getWorkManagerImpl().getLastCancelAllTimeMillis()
    }

    override fun pruneWork(): Operation {
        return getWorkManagerImpl().pruneWork()
    }

    override fun getWorkInfoByIdLiveData(id: UUID): LiveData<WorkInfo?> {
        return getWorkManagerImpl().getWorkInfoByIdLiveData(id)
    }

    override fun getWorkInfoByIdFlow(id: UUID): Flow<WorkInfo?> {
        return getWorkManagerImpl().getWorkInfoByIdFlow(id)
    }

    override fun getWorkInfoById(id: UUID): ListenableFuture<WorkInfo?> {
        return getWorkManagerImpl().getWorkInfoById(id)
    }

    override fun getWorkInfosByTagFlow(tag: String): Flow<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosByTagFlow(tag)
    }

    override fun getWorkInfosByTagLiveData(tag: String): LiveData<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosByTagLiveData(tag)
    }

    override fun getWorkInfosByTag(tag: String): ListenableFuture<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosByTag(tag)
    }

    override fun getWorkInfosForUniqueWorkLiveData(
        uniqueWorkName: String
    ): LiveData<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosForUniqueWorkLiveData(uniqueWorkName)
    }

    override fun getWorkInfosForUniqueWorkFlow(uniqueWorkName: String): Flow<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosForUniqueWorkFlow(uniqueWorkName)
    }

    override fun getWorkInfosForUniqueWork(
        uniqueWorkName: String
    ): ListenableFuture<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosForUniqueWork(uniqueWorkName)
    }

    override fun getWorkInfosLiveData(workQuery: WorkQuery): LiveData<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosLiveData(workQuery)
    }

    override fun getWorkInfosFlow(workQuery: WorkQuery): Flow<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfosFlow(workQuery)
    }

    override fun getWorkInfos(workQuery: WorkQuery): ListenableFuture<List<WorkInfo>> {
        return getWorkManagerImpl().getWorkInfos(workQuery)
    }

    override fun updateWork(request: WorkRequest): ListenableFuture<WorkManager.UpdateResult> {
        return getWorkManagerImpl().updateWork(request)
    }

    public companion object {
        @GuardedBy("lock") private var instance: WorkManagerWrapper? = null
        private val lock = Any()

        /**
         * Retrieves the singleton instance of [WorkManager].
         *
         * @return The singleton instance of [WorkManager]
         */
        @Deprecated("Call WorkManagerWrapper.getInstance(Context) instead.")
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun getInstance(): WorkManager? {
            synchronized(lock) {
                return instance
            }
        }

        /**
         * Provides a way to check if [WorkManager] is initialized in this process.
         *
         * Note that "initialized" here means [WorkManager] APIs are ready to use (e.g. the database
         * is accessible).
         *
         * @return `true` if [WorkManager] has been initialized in this process.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun isInitialized(): Boolean {
            return WorkManagerImpl.isInitialized()
        }

        /**
         * Retrieves the singleton instance of [WorkManager].
         *
         * If the instance doesn't exist, this creates the singleton instance and attempts to
         * initialize it if the user is unlocked.
         *
         * @param context A context for on-demand initialization.
         * @return The singleton instance of [WorkManager]
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun getInstance(context: Context): WorkManager {
            synchronized(lock) {
                if (instance == null) {
                    val appContext = context.applicationContext
                    instance =
                        WorkManagerWrapper(appContext) { WorkManagerImpl.getInstance(appContext) }
                }
                return instance!!
            }
        }

        /**
         * Initializes the singleton instance of [WorkManagerWrapper].
         *
         * @param context A [Context] object for configuration purposes.
         * @param configuration The [Configuration] used to set up WorkManager.
         * @throws IllegalStateException if the device is in Direct Boot mode.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun initialize(context: Context, configuration: Configuration) {
            WorkManagerImpl.initialize(context, configuration)
            getInstance(context)
        }

        @VisibleForTesting
        internal fun resetInstanceForTesting() {
            synchronized(lock) { instance = null }
        }
    }
}
