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

import android.app.Activity;
import android.content.Intent;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;

/**
 * A class that can call {@link Activity#startActivityForResult}-style APIs without having to manage
 * request codes, and converting request/response to an {@link Intent}
 */
public interface ActivityResultCaller {

    /**
     * Register a request to {@link Activity#startActivityForResult start an activity for result},
     * designated by the given {@link ActivityResultContract contract}.
     *
     * This creates a record in the {@link ActivityResultRegistry registry} associated with this
     * caller, managing request code, as well as conversions to/from {@link Intent} under the hood.
     *
     * This *must* be called unconditionally, as part of initialization path, typically as a field
     * initializer of an Activity or Fragment.
     *
     * @param <I> the type of the input(if any) required to call the activity
     * @param <O> the type of output returned as an activity result
     *
     * @param contract the contract, specifying conversions to/from {@link Intent}s
     * @param callback the callback to be called on the main thread when activity result
     *                 is available
     *
     * @return the launcher that can be used to start the activity or dispose of the prepared call.
     */
    @NonNull
    <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull ActivityResultContract<I, O> contract,
            @NonNull ActivityResultCallback<O> callback);

    /**
     * Register a request to {@link Activity#startActivityForResult start an activity for result},
     * designated by the given {@link ActivityResultContract contract}.
     *
     * This creates a record in the given {@link ActivityResultRegistry registry}, managing request
     * code, as well as conversions to/from {@link Intent} under the hood.
     *
     * This *must* be called unconditionally, as part of initialization path, typically as a field
     * initializer of an Activity or Fragment.
     *
     * @param <I> the type of the input(if any) required to call the activity
     * @param <O> the type of output returned as an activity result
     *
     * @param contract the contract, specifying conversions to/from {@link Intent}s
     * @param registry the registry where to hold the record.
     * @param callback the callback to be called on the main thread when activity result
     *                 is available
     *
     * @return the launcher that can be used to start the activity or dispose of the prepared call.
     */
    @NonNull
    <I, O> ActivityResultLauncher<I> registerForActivityResult(
            @NonNull ActivityResultContract<I, O> contract,
            @NonNull ActivityResultRegistry registry,
            @NonNull ActivityResultCallback<O> callback);
}
