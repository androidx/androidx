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

package androidx.activity.result.contract;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A contract specifying that an activity can be called with an input of type I
 * and produce an output of type O
 *
 * Makes calling an activity for result type-safe.
 *
 * @param <I> input type
 * @param <O> output type
 *
 * @see ActivityResultCaller
 */
public abstract class ActivityResultContract<I, O> {

    /** Create an intent that can be used for {@link Activity#startActivityForResult} */
    public abstract @NonNull Intent createIntent(@NonNull Context context,
            @SuppressLint("UnknownNullness") I input);

    /** Convert result obtained from {@link Activity#onActivityResult} to O */
    @SuppressLint("UnknownNullness")
    public abstract O parseResult(int resultCode, @Nullable Intent intent);

    /**
     * An optional method you can implement that can be used to potentially provide a result in
     * lieu of starting an activity.
     *
     * @return the result wrapped in a {@link SynchronousResult} or {@code null} if the call
     * should proceed to start an activity.
     */
    public @Nullable SynchronousResult<O> getSynchronousResult(
            @NonNull Context context,
            @SuppressLint("UnknownNullness") I input) {
        return null;
    }

    /**
     * The wrapper for a result provided in {@link #getSynchronousResult}
     *
     * @param <T> type of the result
     */
    public static final class SynchronousResult<T> {
        private final @SuppressLint("UnknownNullness") T mValue;

        /**
         * Create a new result wrapper
         *
         * @param value the result value
         */
        public SynchronousResult(@SuppressLint("UnknownNullness") T value) {
            this.mValue = value;
        }

        /**
         * @return the result value
         */
        public @SuppressLint("UnknownNullness") T getValue() {
            return mValue;
        }
    }
}
