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

package androidx.paging.samples.java;

import android.annotation.SuppressLint;

import androidx.annotation.Nullable;
import androidx.paging.PagingData;
import androidx.paging.PagingDataTransforms;

import java.util.concurrent.Executor;

import io.reactivex.Flowable;
import kotlin.NotImplementedError;

/**
 * NOTE - MANUALLY COPIED SAMPLE
 *
 * Since @sample from kdoc doesn't support Java, this code must manually kept in sync with
 * the `PagingDataTransforms.insertSeparators` method
 */
@SuppressWarnings({"unused", "WeakerAccess"})
class InsertSeparatorsJavaSample {

    public Flowable<PagingData<String>> pagingDataStream = create();

    private Flowable<PagingData<String>> create() {
        throw new NotImplementedError();
    }

    public Executor bgExecutor;

    @SuppressLint("CheckResult")
    @SuppressWarnings({"unused", "ResultOfMethodCallIgnored", "RxReturnValueIgnored"})
    public void insertSeparatorsSample() {
        /*
         * Create letter separators in an alphabetically sorted list.
         *
         * For example, if the input is:
         *     "apple", "apricot", "banana", "carrot"
         *
         * The operator would output:
         *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
         */
        pagingDataStream.map(pagingData ->
                // map outer stream, so we can perform transformations on each paging generation
                PagingDataTransforms.insertSeparators(pagingData, bgExecutor,
                        (@Nullable String before, @Nullable String after) -> {
                            if (after != null && (before == null
                                    || before.charAt(0) != after.charAt(0))) {
                                // separator - after is first item that starts with its first
                                // letter
                                return Character.toString(
                                        Character.toUpperCase(after.charAt(0)));
                            } else {
                                // no separator - either end of list, or first
                                // letters of items are the same
                                return null;
                            }
                        }));
    }
}
