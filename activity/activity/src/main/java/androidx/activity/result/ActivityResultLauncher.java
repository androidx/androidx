/*
 * Copyright (C) 2020 The Android Open Source Project
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


package androidx.activity.result;

import android.annotation.SuppressLint;

import androidx.activity.result.contract.ActivityResultContract;

/**
 * A launcher for a prevoiusly-{@link ActivityResultCaller#prepareCall prepared call} to start
 * the process of executing an {@link ActivityResultContract}.
 *
 * @param <I> type of the input required to launch
 */
public interface ActivityResultLauncher<I> {

    /**
     * Executes an {@link ActivityResultContract}.
     *
     * @param input the input required to execute an {@link ActivityResultContract}.
     */
    void launch(@SuppressLint("UnknownNullness") I input);

    /**
     * Disposes of this launcher, releasing the underlying result callback, and any references
     * captured within it.
     *
     * You should call this if the registry may live longer than the callback registered for this
     * launcher.
     */
    void dispose();
}
