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
    public abstract @NonNull Intent createIntent(@SuppressLint("UnknownNullness") I input);

    /** Convert result obtained from {@link Activity#onActivityResult} to O */
    public abstract @SuppressLint("UnknownNullness") O parseResult(
            int resultCode,
            @Nullable Intent intent);
}
