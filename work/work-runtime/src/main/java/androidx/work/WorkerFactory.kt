/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.work

import android.content.Context
import androidx.annotation.RestrictTo

/**
 * A factory object that creates [ListenableWorker] instances. The factory is invoked every
 * time a work runs. You can override the default implementation of this factory by manually
 * initializing [WorkManager] (see [WorkManager.initialize] and
 * specifying a new WorkerFactory in [Configuration.Builder.setWorkerFactory].
 */
abstract class WorkerFactory {
    /**
     * Override this method to implement your custom worker-creation logic.  Use
     * [Configuration.Builder.setWorkerFactory] to use your custom class.
     *
     * Throwing an [Exception] here and no [ListenableWorker] will be created. If a
     * [WorkerFactory] is unable to create an instance of the [ListenableWorker], it should
     * return `null` so it can delegate to the default [WorkerFactory].
     *
     * Returns a new instance of the specified `workerClassName` given the arguments.  The
     * returned worker must be a newly-created instance and must not have been previously returned
     * or invoked by WorkManager. Otherwise, WorkManager will throw an
     * [IllegalStateException].
     *
     * @param appContext The application context
     * @param workerClassName The class name of the worker to create
     * @param workerParameters Parameters for worker initialization
     * @return A new [ListenableWorker] instance of type `workerClassName`, or
     * `null` if the worker could not be created
     */
    abstract fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker?

    /**
     * Returns a new instance of the specified `workerClassName` given the arguments.  If no
     * worker is found, default reflection-based code will be used to instantiate the worker with
     * the current ClassLoader.  The returned worker should be a newly-created instance and must not
     * have been previously returned or used by WorkManager.
     *
     * @param appContext       The application context
     * @param workerClassName  The class name of the worker to create
     * @param workerParameters Parameters for worker initialization
     * @return A new [ListenableWorker] instance of type `workerClassName`, or
     * `null` if the worker could not be created
     * @throws IllegalStateException when `workerClassName` cannot be instantiated or the
     * [WorkerFactory] returns an instance of the [ListenableWorker] which is used.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun createWorkerWithDefaultFallback(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker {
        fun getWorkerClass(workerClassName: String): Class<out ListenableWorker> {
            return try {
                Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
            } catch (throwable: Throwable) {
                Logger.get().error(TAG, "Invalid class: $workerClassName", throwable)
                throw throwable
            }
        }
        fun fallbackToReflection(
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker {
            val clazz = getWorkerClass(workerClassName)
            return try {
                val constructor = clazz.getDeclaredConstructor(
                    Context::class.java, WorkerParameters::class.java
                )
                constructor.newInstance(appContext, workerParameters)
            } catch (e: Throwable) {
                Logger.get().error(TAG, "Could not instantiate $workerClassName", e)
                throw e
            }
        }
        val worker = createWorker(appContext, workerClassName, workerParameters)
                ?: fallbackToReflection(workerClassName, workerParameters)
        if (worker.isUsed) {
            val message = "WorkerFactory (${javaClass.name}) returned an instance of" +
                " a ListenableWorker ($workerClassName) which has already been invoked. " +
                "createWorker() must always return a new instance of a ListenableWorker."
            throw IllegalStateException(message)
        }
        return worker
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DefaultWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ) = null
}

private val TAG = Logger.tagWithPrefix("WorkerFactory")
