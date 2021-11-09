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
import androidx.paging.map
import androidx.paging.rxjava2.insertSeparatorsAsync
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.Futures
import io.reactivex.Maybe
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class Item(
    @JvmField
    val id: String,
    @JvmField
    val label: String
)

private lateinit var pagingDataStream: Flow<PagingData<Item>>

@Sampled
fun insertSeparatorsUiModelSample() {
    open class UiModel
    data class ItemUiModel(val item: Item) : UiModel()
    data class SeparatorUiModel(val char: Char) : UiModel()

    /*
     * Create letter separators in an alphabetically sorted list of Items, with UiModel objects.
     *
     * For example, if the input is (each an `Item`):
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output a list of UiModels corresponding to:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData
            .map { item ->
                ItemUiModel(item) // convert items in stream to ItemUiModel
            }
            .insertSeparators<ItemUiModel, UiModel> { before: ItemUiModel?, after: ItemUiModel? ->
                if (after != null && before?.item?.label?.first() != after.item.label.first()) {
                    // separator - after is first item that starts with its first letter
                    SeparatorUiModel(after.item.label.first().uppercaseChar())
                } else {
                    // no separator - either end of list, or first letters of before/after are the same
                    null
                }
            }
    }
}

@Sampled
fun insertSeparatorsUiModelRxSample() {
    open class UiModel
    data class ItemUiModel(val item: Item) : UiModel()
    data class SeparatorUiModel(val char: Char) : UiModel()

    /*
     * Create letter separators in an alphabetically sorted list of Items, with UiModel objects.
     *
     * For example, if the input is (each an `Item`):
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output a list of UiModels corresponding to:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData
            .map { item ->
                ItemUiModel(item) // convert items in stream to ItemUiModel
            }
            .insertSeparatorsAsync { before: ItemUiModel?, after: ItemUiModel? ->
                Maybe.fromCallable<UiModel> {
                    if (after != null && before?.item?.label?.first() != after.item.label.first()) {
                        // separator - after is first item that starts with its first letter
                        SeparatorUiModel(after.item.label.first().uppercaseChar())
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
fun insertSeparatorsUiModelFutureSample() {
    open class UiModel
    data class ItemUiModel(val item: Item) : UiModel()
    data class SeparatorUiModel(val char: Char) : UiModel()

    /*
     * Create letter separators in an alphabetically sorted list of Items, with UiModel objects.
     *
     * For example, if the input is (each an `Item`):
     *     "apple", "apricot", "banana", "carrot"
     *
     * The operator would output a list of UiModels corresponding to:
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     */
    pagingDataStream.map { pagingData ->
        // map outer stream, so we can perform transformations on each paging generation
        pagingData
            .map { item ->
                ItemUiModel(item) // convert items in stream to ItemUiModel
            }
            .insertSeparatorsAsync(
                AsyncFunction<AdjacentItems<ItemUiModel>, UiModel?> {
                    Futures.submit(
                        Callable<UiModel> {
                            val (before, after) = it!!
                            if (after != null &&
                                before?.item?.label?.first() != after.item.label.first()
                            ) {
                                // separator - after is first item that starts with its first letter
                                SeparatorUiModel(after.item.label.first().uppercaseChar())
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
