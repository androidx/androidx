/*
 * Copyright 2025 The Android Open Source Project
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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appsearch.app.GlobalSearchSession
import androidx.appsearch.app.SearchResult
import androidx.appsearch.app.SearchResults
import androidx.appsearch.platformstorage.PlatformStorage
import kotlinx.coroutines.guava.await

/**
 * Create a [GlobalSearchSession] asynchronously.
 *
 * @param context the [Context] for the application.
 * @return a new [GlobalSearchSession].
 */
@RequiresApi(Build.VERSION_CODES.S)
internal suspend fun createSearchSession(context: Context): GlobalSearchSession {
    return PlatformStorage.createGlobalSearchSessionAsync(
            PlatformStorage.GlobalSearchContext.Builder(context)
                .setWorkerExecutor(Runnable::run)
                .build()
        )
        .await()
}

/**
 * Reads all [SearchResults] and returns a list of [T] by transforming the [SearchResult] to [T] or
 * null.
 *
 * @param T the type of document to transform the [SearchResult] to.
 * @param transformToDocumentClassOrNull a function to transform the [SearchResult] to [T] or `null`
 *   if it can't.
 * @return a list of [T] instances or null which contains the results from the query. Returns an
 *   empty list if nothing is found.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public suspend fun <T : Any> SearchResults.readAll(
    transformToDocumentClassOrNull: (SearchResult) -> T?
): List<T?> {
    return buildList<T?> {
        var nextPage = nextPageAsync.await()
        while (nextPage.isNotEmpty()) {
            nextPage.map(transformToDocumentClassOrNull).forEach(::add)
            nextPage = nextPageAsync.await()
        }
    }
}
