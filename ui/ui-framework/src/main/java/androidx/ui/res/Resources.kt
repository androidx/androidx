/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.res

import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.TypedValue
import androidx.annotation.GuardedBy
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.remember
import androidx.ui.core.ContextAmbient
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private val cacheLock = Object()
private const val CACHE_SIZE = 500
/**
 * Exposed only for testing purposes. Do not touch directly.
 */
@GuardedBy("cacheLock")
internal val requestCache = mutableMapOf<String, MutableList<DeferredResource<*>>>()
@GuardedBy("cacheLock")
internal val resourceCache = LruCache<String, Any>(CACHE_SIZE)

// TODO(nona): Reimplement coroutine once IR compiler support suspend function.
private val executor = Executors.newFixedThreadPool(1) { Thread(it, "ResourceThread") }
private val handler by lazy { Handler(Looper.getMainLooper()) }
private val postAtFrontOfQueue: (() -> Unit) -> Unit = { handler.postAtFrontOfQueue(it) }

internal enum class LoadingState { PENDING, LOADED, FAILED }

/**
 * A class used for the result of the asynchronous resource loading.
 */
@Model class DeferredResource<T> internal constructor(
    internal var state: LoadingState = LoadingState.PENDING,
    private val pendingResource: T? = null,
    private val failedResource: T? = null
) {
    private var loadedResource: T? = null
    private var failedReason: Throwable? = null

    internal fun loadCompleted(loadedResource: T) {
        state = LoadingState.LOADED
        this.loadedResource = loadedResource
    }

    internal fun failed(t: Throwable) {
        state = LoadingState.FAILED
        failedReason = t
    }

    /**
     * Returns the resource.
     *
     * The resource can be [PendingResource], [LoadedResource], or [FailedResource].
     */
    val resource: Resource<T>
        get() = when (state) {
            LoadingState.FAILED -> FailedResource(failedResource, failedReason)
            LoadingState.PENDING -> PendingResource(pendingResource)
            LoadingState.LOADED -> LoadedResource(loadedResource!!)
        }
}

/**
 * The base resource class for background resource loading.
 *
 * @param resource the resource
 */
sealed class Resource<T>(val resource: T?)

/**
 * A class represents the loaded resource.
 */
class LoadedResource<T>(resource: T) : Resource<T>(resource)

/**
 * A class represents the alternative resource due to background loading.
 */
class PendingResource<T>(resource: T?) : Resource<T>(resource)

/**
 * A class represents the alternative resource due to failed to load the requested resource.
 * @param throwable the reason of the failure.
 */
class FailedResource<T>(resource: T?, val throwable: Throwable?) : Resource<T>(resource)

/**
 * A common resource loading method.
 */
// TODO(nona): Accept CoroutineScope for customizing fetching coroutine.
@Composable
internal fun <T> loadResource(
    id: Int,
    pendingResource: T? = null,
    failedResource: T? = null,
    loader: (Int) -> T
): DeferredResource<T> {
    return loadResourceInternal(
        id,
        pendingResource,
        failedResource,
        executor,
        postAtFrontOfQueue,
        cacheLock,
        requestCache,
        resourceCache,
        loader)
}

/**
 * This function is exposed only for testing purpose. Do not use this directly.
 */
@Suppress("UNCHECKED_CAST")
@Composable
internal fun <T> loadResourceInternal(
    id: Int,
    pendingResource: T? = null,
    failedResource: T? = null,
    executor: Executor,
    uiThreadHandler: (() -> Unit) -> Unit,
    cacheLock: Any,
    requestCache: MutableMap<String, MutableList<DeferredResource<*>>>,
    resourceCache: LruCache<String, Any>,
    loader: (Int) -> T
): DeferredResource<T> {
    val context = ContextAmbient.current
    val value = remember { TypedValue() }
    context.resources.getValue(id, value, true)
    // We use the file path as a key of the request cache.
    // TODO(nona): Add density to the key?
    val key = value.string?.toString()
    val deferred = remember(key, pendingResource, failedResource) {
        DeferredResource(
            state = LoadingState.PENDING,
            pendingResource = pendingResource,
            failedResource = failedResource
        )
    }

    // First, if the deferred is not pending, the loading is completed or failed. Do nothing and
    // return the memorized result.
    if (deferred.state != LoadingState.PENDING) {
        return deferred
    }

    if (key == null) {
        return deferred.apply { failed(Resources.NotFoundException("path not found")) }
    }

    synchronized(cacheLock) {

        // Check if we already know the loadedresource, return with marking load completed.
        resourceCache.get(key)?.let { return deferred.apply { loadCompleted(it as T) } }

        requestCache.getOrPut(key, { mutableListOf() }).let {
            it.add(deferred)
            if (it.size == 1) {
                // This is the first time to request the resource. schedule the background loading.

                executor.execute {
                    try {
                        val loaded = loader(id)
                        uiThreadHandler {
                            synchronized(cacheLock) {
                                requestCache.remove(key)?.forEach {
                                    (it as DeferredResource<T>).loadCompleted(loaded)
                                }
                            }
                        }
                    } catch (t: Throwable) {
                        uiThreadHandler {
                            synchronized(cacheLock) {
                                requestCache.remove(key)?.forEach {
                                    (it as DeferredResource<T>).failed(t)
                                }
                            }
                        }
                    }
                }
            }
        }
        return deferred
    }
}
