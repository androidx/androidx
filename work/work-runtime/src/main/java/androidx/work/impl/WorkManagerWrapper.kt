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
 * A proxy wrapper for [WorkManager] that defers creation of [WorkManagerImpl] when retrieved via
 * [getInstance] while the user is locked (in direct boot mode).
 *
 * Note that invoking any [WorkManager] API on a deferred wrapper while the user is still locked
 * will immediately attempt to initialize [WorkManagerImpl] and result in a direct boot failure.
 */
internal class WorkManagerWrapper : WorkManager {

    @GuardedBy("WorkManagerImpl.sLock") private var context: Context?
    @GuardedBy("WorkManagerImpl.sLock") private var implSingletonSupplier: (() -> WorkManagerImpl)?
    @Volatile private var cachedImpl: WorkManagerImpl? = null

    internal constructor(
        context: Context,
        implSingletonSupplier: () -> WorkManagerImpl,
    ) : super() {
        this.context = context
        this.implSingletonSupplier = implSingletonSupplier
    }

    internal constructor(impl: WorkManagerImpl) : super() {
        this.context = null
        this.implSingletonSupplier = null
        this.cachedImpl = impl
    }

    /** Must be called with [WorkManagerImpl.sLock] held. */
    @GuardedBy("WorkManagerImpl.sLock")
    internal fun tryInitializeEagerly() {
        if (cachedImpl != null) return
        val appContext = context ?: return
        // TODO(b/514165396): Add a user unlock receiver to try to initialize in case the device is
        //  not currently unlocked (and avoid repeated isUserUnlocked() IPCs under sLock).
        if (UserManagerCompat.isUserUnlocked(appContext)) {
            getWorkManagerImpl()
        }
    }

    override val configuration: Configuration
        get() = getWorkManagerImpl().configuration

    /** Single choke point for accessing the internal [WorkManagerImpl]. */
    private fun getWorkManagerImpl(): WorkManagerImpl {
        cachedImpl?.let {
            return it
        }
        synchronized(WorkManagerImpl.sLock) {
            cachedImpl?.let {
                return it
            }
            // TODO(b/514165396): Evaluate failing fast when the user is still locked instead of
            //  invoking implSingletonSupplier and caching a WorkManagerImpl whose ForceStopRunnable
            //  fails asynchronously in direct boot.
            val resolved =
                checkNotNull(implSingletonSupplier) {
                        "This WorkManager instance was reset (e.g. via setDelegate(null) or " +
                            "resetInstanceForTesting()); call WorkManager.getInstance(Context) " +
                            "again."
                    }
                    .invoke()
            bindImplLocked(resolved)
            return resolved
        }
    }

    @GuardedBy("WorkManagerImpl.sLock")
    private fun bindImplLocked(impl: WorkManagerImpl) {
        cachedImpl = impl
        clearDeferredStateLocked()
    }

    @GuardedBy("WorkManagerImpl.sLock")
    private fun clearDeferredStateLocked() {
        context = null
        implSingletonSupplier = null
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

    companion object {
        @GuardedBy("WorkManagerImpl.sLock") private var instance: WorkManagerWrapper? = null

        @JvmStatic
        fun onDelegateChanged(delegate: WorkManagerImpl?) {
            synchronized(WorkManagerImpl.sLock) {
                val current = instance
                instance =
                    when {
                        delegate == null -> {
                            // Clear any discarded deferred wrapper so a stale reference fails fast
                            // rather than silently initializing a new default WorkManagerImpl.
                            if (current != null && current.cachedImpl == null) {
                                current.clearDeferredStateLocked()
                            }
                            null
                        }
                        current == null -> WorkManagerWrapper(delegate)
                        current.cachedImpl == null -> current.also { it.bindImplLocked(delegate) }
                        current.cachedImpl === delegate -> current
                        else -> WorkManagerWrapper(delegate)
                    }
            }
        }

        @JvmStatic
        fun onDefaultInitialized(defaultImpl: WorkManagerImpl) {
            synchronized(WorkManagerImpl.sLock) {
                val current = instance
                if (current == null) {
                    instance = WorkManagerWrapper(defaultImpl)
                } else if (current.cachedImpl == null) {
                    current.bindImplLocked(defaultImpl)
                } else {
                    check(current.cachedImpl === defaultImpl) {
                        "WorkManagerWrapper singleton is bound to a different WorkManagerImpl " +
                            "than the default instance"
                    }
                }
            }
        }

        /**
         * Retrieves the singleton instance of [WorkManager].
         *
         * @return The singleton instance of [WorkManager]
         */
        @Deprecated("Call WorkManagerWrapper.getInstance(Context) instead.")
        @JvmStatic
        fun getInstance(): WorkManager? {
            synchronized(WorkManagerImpl.sLock) {
                return instance
            }
        }

        /**
         * Provides a way to check if [WorkManager] is initialized in this process.
         *
         * @return `true` if [WorkManager] has been initialized in this process.
         */
        @JvmStatic
        fun isInitialized(): Boolean {
            return WorkManagerImpl.isInitialized()
        }

        /**
         * Retrieves the singleton instance of [WorkManager].
         *
         * If the instance doesn't exist, this creates the singleton instance and attempts to
         * initialize it if the user is unlocked. When initialization is deferred during Direct
         * Boot, the [Context] from the first call is retained and used for subsequent on-demand
         * initialization.
         *
         * @param context A context for on-demand initialization.
         * @return The singleton instance of [WorkManager]
         */
        @JvmStatic
        fun getInstance(context: Context): WorkManager {
            synchronized(WorkManagerImpl.sLock) {
                var wrapper = instance
                val isNewWrapper = wrapper == null
                if (wrapper == null) {
                    @Suppress("DEPRECATION") val existingImpl = WorkManagerImpl.getInstance()
                    wrapper =
                        // Defensive fallback in case WorkManagerImpl was already initialized.
                        if (existingImpl != null) {
                            WorkManagerWrapper(existingImpl)
                        } else {
                            val appContext = context.applicationContext ?: context
                            WorkManagerWrapper(appContext) {
                                WorkManagerImpl.getInstance(appContext)
                            }
                        }
                    instance = wrapper
                }
                try {
                    wrapper.tryInitializeEagerly()
                } catch (t: Throwable) {
                    if (isNewWrapper && instance === wrapper) {
                        instance = null
                    }
                    throw t
                }
                return wrapper
            }
        }

        /**
         * Initializes the singleton instance of [WorkManagerWrapper].
         *
         * Initialization is *not* deferred here. Initializing while the user is locked results in
         * an asynchronous [IllegalStateException] delivered to
         * [Configuration.initializationExceptionHandler] (or thrown on the task thread if none is
         * set).
         *
         * @param context A [Context] object for configuration purposes.
         * @param configuration The [Configuration] used to set up WorkManager.
         */
        @JvmStatic
        fun initialize(context: Context, configuration: Configuration) {
            synchronized(WorkManagerImpl.sLock) {
                WorkManagerImpl.initialize(context, configuration)
                // Ensures wrapper instance exists even if a test delegate was set and initialize
                // was a no-op.
                getInstance(context)
            }
        }
    }
}
