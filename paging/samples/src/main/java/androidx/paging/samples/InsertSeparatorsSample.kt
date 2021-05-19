/*
 * Copyright 2020 The Android Open Source Project
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

@file:Suppress("unused") // Currently, all samples incorrectly labeled as unused.

package androidx.paging.samples

import androidx.annotation.Sampled
import androidx.paging.AdjacentItems
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.insertSeparatorsAsync
import androidx.paging.rxjava2.insertSeparatorsAsync
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.Callable
import java.util.concurrent.Executors

private lateinit var pagingDataStream: Flow<PagingData<String>>

@Sampled
fun insertSeparatorsSample() {
    /*
     * Create letter separators in an alphabetically sorted list.
     *
     * For example, if the input is:
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData.insertSeparators { before: String?, after: String? ->
            if (after != null && before?.first() != after.first()) {
                // separator - after is first item that starts with its first letter
                after.first().uppercaseChar().toString()
            } else {
                // no separator - either end of list, or first letters of before/after are the same
                null
            }
        }
    }
}

@Sampled
fun insertSeparatorsRxSample() {
    /*
     * Create letter separators in an alphabetically sorted list.
     *
     * For example, if the input is:
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData.insertSeparatorsAsync { before: String?, after: String? ->
            Maybe.fromCallable<String> {
                if (after != null && before?.first() != after.first()) {
                    // separator - after is first item that starts with its first letter
                    after.first().uppercaseChar().toString()
                } else {
                    // no separator - either end of list, or first letters of before/after are the same
                    null
                }
            }.subscribeOn(Schedulers.computation())
        }
    }
}

private val executor = Executors.newSingleThreadExecutor()

@Suppress("UnstableApiUsage")
@Sampled
fun insertSeparatorsFutureSample() {
    /*
     * Create letter separators in an alphabetically sorted list.
     *
     * For example, if the input is:
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData.insertSeparatorsAsync(
            AsyncFunction<AdjacentItems<String>, String?> {
                Futures.submit(
                    Callable<String?> {
                        val (before, after) = it!!
                        if (after != null && before?.first() != after.first()) {
                            // separator - after is first item that starts with its first letter
                            after.first().uppercaseChar().toString()
                        } else {
                            // no separator - either end of list, or first letters of before/after are the same
                            null
                        }
                    },
                    executor
                )
            },
            executor
        )
    }
}
