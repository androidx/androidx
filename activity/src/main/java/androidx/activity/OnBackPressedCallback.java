/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.activity;

import androidx.annotation.MainThread;
import androidx.lifecycle.LifecycleOwner;

/**
 * Interface for handling {@link ComponentActivity#onBackPressed()} callbacks without
 * strongly coupling that implementation to a subclass of {@link ComponentActivity}.
 *
 * @see ComponentActivity#addOnBackPressedCallback(LifecycleOwner, OnBackPressedCallback)
 * @see ComponentActivity#removeOnBackPressedCallback(OnBackPressedCallback)
 */
public interface OnBackPressedCallback {
    /**
     * Callback for handling the {@link ComponentActivity#onBackPressed()} event.
     *
     * @return True if you handled the {@link ComponentActivity#onBackPressed()} event. No
     * further {@link OnBackPressedCallback} instances will be called if you return true.
     */
    @MainThread
    boolean handleOnBackPressed();
}
