/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.messaging.model;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.RequiresCarApi;

/** Used by the host to invoke {@link ConversationCallback} methods on the client */
@ExperimentalCarApi
@CarProtocol
@RequiresCarApi(7)
public interface ConversationCallbackDelegate {

    /** Called from the host to invoke {@link ConversationCallback#onMarkAsRead()} on the client. */
    // This mirrors the AIDL class and is not supported to support an executor as an input.
    @SuppressLint("ExecutorRegistration")
    void sendMarkAsRead(@NonNull OnDoneCallback onDoneCallback);

    /**
     * Called from the host to invoke {@link ConversationCallback#onTextReply(String)} on the
     * client.
     */
    // This mirrors the AIDL class and is not supported to support an executor as an input.
    @SuppressLint("ExecutorRegistration")
    void sendTextReply(@NonNull String replyText, @NonNull OnDoneCallback onDoneCallback);
}
