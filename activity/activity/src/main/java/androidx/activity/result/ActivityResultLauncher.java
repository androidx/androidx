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
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

/**
 * A launcher for a previously-{@link ActivityResultCaller#registerForActivityResult prepared call}
 * to start the process of executing an {@link ActivityResultContract}.
 *
 * @param <I> type of the input required to launch
 */
public abstract class ActivityResultLauncher<I> {

    /**
     * Executes an {@link ActivityResultContract}.
     *
     * @param input the input required to execute an {@link ActivityResultContract}.
     */
    public void launch(@SuppressLint("UnknownNullness") I input) {
        launch(input, null);
    }

    /**
     * Executes an {@link ActivityResultContract}.
     *
     * @param input the input required to execute an {@link ActivityResultContract}.
     * @param options Additional options for how the Activity should be started.
     */
    public abstract void launch(@SuppressLint("UnknownNullness") I input,
            @Nullable ActivityOptionsCompat options);

    /**
     * Unregisters this launcher, releasing the underlying result callback, and any references
     * captured within it.
     *
     * You should call this if the registry may live longer than the callback registered for this
     * launcher.
     */
    @MainThread
    public abstract void unregister();

    /**
     * Get the {@link ActivityResultContract} that was used to create this launcher.
     *
     * @return the contract that was used to create this launcher
     */
    @NonNull
    public abstract ActivityResultContract<I, ?> getContract();
}
