/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.car.app.dialer;

import android.annotation.SuppressLint;

import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.dialer.TelephoneKeypadTemplate.KeypadKey;

import org.jspecify.annotations.NonNull;

/**
 * A host-side interface for reporting keypad key events to clients. This delegate exists to
 * abstract away the success/failure calls caused by the app, host, or android OS (due to the ipc
 * transaction) through a more structured interface. All IPC events must be executed on the main
 * thread.
 */
@CarProtocol
@ExperimentalCarApi
public interface TelephoneKeypadCallbackDelegate {
    /**
     * Send the key long press event to the client.
     *
     * @param key The code for the key that was pressed allowed by {@link KeypadKey}.
     * @param callback The callback to be notified when the operation is complete. The callback
     *                 will be executed on the main thread.
     */
    @SuppressLint("ExecutorRegistration")
    void sendKeyLongPress(@KeypadKey int key, @NonNull OnDoneCallback callback);

    /**
     * Send the key down event to the client.
     *
     * @param key The code for the key that was pressed allowed by {@link KeypadKey}.
     * @param callback The callback to be notified when the operation is complete. The callback
     *                 will be executed on the main thread.
     */
    @SuppressLint("ExecutorRegistration")
    void sendKeyDown(@KeypadKey int key, @NonNull OnDoneCallback callback);

    /**
     * Send the key up event to the client.
     *
     * @param key The code for the key that was pressed allowed by {@link KeypadKey}.
     * @param callback The callback to be notified when the operation is complete. The callback
     *                 will be executed on the main thread.
     */
    @SuppressLint("ExecutorRegistration")
    void sendKeyUp(@KeypadKey int key, @NonNull OnDoneCallback callback);
}
