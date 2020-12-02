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

package androidx.enterprise.feedback;

import androidx.annotation.Nullable;

import java.util.Collection;

/**
 * Interface used to listen for the result when using
 * {@link KeyedAppStatesReporter#setStates(Collection, KeyedAppStatesCallback)} or
 * {@link KeyedAppStatesReporter#setStatesImmediate(Collection, KeyedAppStatesCallback)}.
 *
 * <p>{@link #onResult(int, Throwable)} will only only report errors which occur inside this app.
 * If a failure occurs in the Device Policy Controller then this will not be reported.
 *
 * <p>{@link #STATUS_SUCCESS} will be reported if the states are sent to all Device Policy
 * Controllers.
 */
public interface KeyedAppStatesCallback {
    /**
     * Used when the states have been sent to all eligible receivers.
     *
     * <p>If there are 0 eligible receivers on the device, then this will be recorded as success.
     */
    int STATUS_SUCCESS = 0;

    /** Used when an error has occurred which stopped the states being set that isn't covered by
     * the other error types. */
    int STATUS_UNKNOWN_ERROR = 1;

    /** An error has occurred because the transaction setting the states has exceeded the Android
     * binder limit (1MB). This can occur because the app is filling up the 1MB limit with other
     * IPC calls, or because the size or number of states being set is too large.
     */
    int STATUS_TRANSACTION_TOO_LARGE_ERROR = 2;

    /** An error occurred because the local app buffer was exceeded. This means too many setState
     * or setStateImmediate calls have been made without a connection to the DPC being formed. */
    int STATUS_EXCEEDED_BUFFER_ERROR = 3;

    /**
     * Called either when an error happens in this app, or when the states have been sent to all
     * eligible receivers.
     *
     * <p>If there is an error, this will be called with the first error encountered.
     *
     * <p>If there are 0 eligible receivers on the device, then this will be recorded as success.
     */
    void onResult(int state, @Nullable Throwable throwable);
}
