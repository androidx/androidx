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
import androidx.arch.core.util.Cancellable;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;

/**
 * Class for handling {@link OnBackPressedDispatcher#onBackPressed()} callbacks without
 * strongly coupling that implementation to a subclass of {@link ComponentActivity}.
 * <p>
 * This class maintains its own {@link #isEnabled() enabled state}. Only when this callback
 * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
 * <p>
 * Note that the enabled state is an additional layer on top of the
 * {@link androidx.lifecycle.LifecycleOwner} passed to
 * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
 * which controls when the callback is added and removed to the dispatcher.
 *
 * @see ComponentActivity#getOnBackPressedDispatcher()
 */
public abstract class OnBackPressedCallback {

    private boolean mEnabled;
    private ArrayList<Cancellable> mCancellables = new ArrayList<>();

    /**
     * Create a {@link OnBackPressedCallback}.
     *
     * @param enabled The default enabled state for this callback.
     * @see #setEnabled(boolean)
     */
    public OnBackPressedCallback(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Set the enabled state of the callback. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     * <p>
     * Note that the enabled state is an additional layer on top of the
     * {@link androidx.lifecycle.LifecycleOwner} passed to
     * {@link OnBackPressedDispatcher#addCallback(LifecycleOwner, OnBackPressedCallback)}
     * which controls when the callback is added and removed to the dispatcher.
     *
     * @param enabled whether the callback should be considered enabled
     */
    @MainThread
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    /**
     * Checks whether this callback should be considered enabled. Only when this callback
     * is enabled will it receive callbacks to {@link #handleOnBackPressed()}.
     *
     * @return Whether this callback should be considered enabled.
     */
    @MainThread
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Callback for handling the {@link OnBackPressedDispatcher#onBackPressed()} event.
     */
    @MainThread
    public abstract void handleOnBackPressed();
}
