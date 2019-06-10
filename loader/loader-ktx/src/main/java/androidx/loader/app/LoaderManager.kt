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

package androidx.loader.app

import android.os.Bundle
import androidx.annotation.MainThread
import androidx.loader.content.Loader

/**
 * Ensures a loader is initialized and active.  If the loader doesn't
 * already exist, the given [loader] is used and (if the activity/fragment is currently
 * started) starts the loader.  Otherwise the last created
 * loader is re-used and [loader] is ignored.
 *
 * In either case, the given [onLoaderReset] and [onLoadFinished] methods will be
 * associated with the loader, and
 * will be called as the loader state changes.  If at the point of call
 * the caller is in its started state, and the requested loader
 * already exists and has generated its data, then
 * callback [onLoadFinished] will
 * be called immediately (inside of this function), so you must be prepared
 * for this to happen.
 *
 * ```
 * LoaderManager.getInstance(this).initLoader(LOADER_ID, MyLoader()) { data ->
 *   // Handle onLoadFinished
 * }
 *
 * // Or, if you need an onLoaderReset callback:
 * LoaderManager.getInstance(this).initLoader(LOADER_ID, MyLoader(), {
 *   // Handle onLoaderReset
 * }) { data ->
 *   // Handle onLoadFinished
 * }
 * ```
 *
 * @param id A unique identifier for this loader.  Can be whatever you want.
 * Identifiers are scoped to a particular LoaderManager instance.
 * @param loader The [Loader] to be used when no loader is already created with this [id].
 * @param onLoaderReset Lambda to call to handle [LoaderManager.LoaderCallbacks.onLoaderReset]
 * @param onLoadFinished Lambda to call to handle [LoaderManager.LoaderCallbacks.onLoadFinished]
 */
@MainThread
inline fun <D> LoaderManager.initLoader(
    id: Int,
    loader: Loader<D>,
    crossinline onLoaderReset: () -> Unit = {},
    crossinline onLoadFinished: (data: D) -> Unit
) {
    initLoader(id, null, object : LoaderManager.LoaderCallbacks<D> {
        override fun onCreateLoader(id: Int, args: Bundle?) = loader

        override fun onLoadFinished(loader: Loader<D>, data: D) {
            onLoadFinished(data)
        }

        override fun onLoaderReset(loader: Loader<D>) {
            onLoaderReset()
        }
    })
}

/**
 * Starts a new or restarts an existing [Loader] in
 * this manager, registers the given [onLoaderReset] and [onLoadFinished] methods to it,
 * and (if the activity/fragment is currently started) starts loading it.
 * If a loader with the same id has previously been
 * started it will automatically be destroyed when the new loader completes
 * its work.
 *
 * ```
 * LoaderManager.getInstance(this).restartLoader(LOADER_ID, MyLoader()) { data ->
 *   // Handle onLoadFinished
 * }
 *
 * // Or, if you need an onLoaderReset callback:
 * LoaderManager.getInstance(this).restartLoader(LOADER_ID, MyLoader(), {
 *   // Handle onLoaderReset
 * }) { data ->
 *   // Handle onLoadFinished
 * }
 * ```
 *
 * @param id A unique identifier for this loader.  Can be whatever you want.
 * Identifiers are scoped to a particular LoaderManager instance.
 * @param loader The [Loader] to be used
 * @param onLoaderReset Lambda to call to handle [LoaderManager.LoaderCallbacks.onLoaderReset]
 * @param onLoadFinished Lambda to call to handle [LoaderManager.LoaderCallbacks.onLoadFinished]
 */
@MainThread
inline fun <D> LoaderManager.restartLoader(
    id: Int,
    loader: Loader<D>,
    crossinline onLoaderReset: () -> Unit = {},
    crossinline onLoadFinished: (data: D) -> Unit
) {
    restartLoader(id, null, object : LoaderManager.LoaderCallbacks<D> {
        override fun onCreateLoader(id: Int, args: Bundle?) = loader

        override fun onLoadFinished(loader: Loader<D>, data: D) {
            onLoadFinished(data)
        }

        override fun onLoaderReset(loader: Loader<D>) {
            onLoaderReset()
        }
    })
}
