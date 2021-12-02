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

import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_IMMEDIATE_STATE;
import static androidx.enterprise.feedback.KeyedAppStatesReporter.WHAT_STATE;

import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class SendableMessage {
    private static final String LOG_TAG = "SendableMessage";

    private final Bundle mAppStatesBundle;
    private final KeyedAppStatesCallback mCallback;
    private final boolean mImmediate;

    SendableMessage(@NonNull Bundle appStatesBundle, @Nullable KeyedAppStatesCallback callback,
            boolean immediate) {
        this.mAppStatesBundle = appStatesBundle;
        this.mCallback = callback;
        this.mImmediate = immediate;
    }

    @Nullable
    KeyedAppStatesCallback getCallback() {
        return mCallback;
    }

    Message createStateMessage() {
        Message message = Message.obtain();
        message.what = mImmediate ? WHAT_IMMEDIATE_STATE : WHAT_STATE;
        message.obj = mAppStatesBundle;
        return message;
    }

    void onSuccess() {
        if (mCallback != null) {
            mCallback.onResult(KeyedAppStatesCallback.STATUS_SUCCESS, /* throwable= */ null);
        }
    }

    void dealWithError(int errorType, @Nullable Throwable throwable) {
        if (mCallback != null) {
            mCallback.onResult(errorType, throwable);
        } else {
            Log.e(LOG_TAG, "Error sending message. error: " + errorType, throwable);
        }
    }
}
