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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.IOnDoneCallback;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.car.app.annotations.KeepFields;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.utils.RemoteUtils;

/**
 * Handles binder transactions related to {@link ConversationCallback}
 *
 * <p> This class exists because we don't want to expose {@link IConversationCallback} to the A4C
 * client.
 *
 * @hide
 */
@ExperimentalCarApi
@RestrictTo(LIBRARY)
@CarProtocol
@RequiresCarApi(7)
@KeepFields
class ConversationCallbackDelegateImpl implements ConversationCallbackDelegate {

    @Nullable
    private final IConversationCallback mConversationCallbackBinder;

    ConversationCallbackDelegateImpl(@NonNull ConversationCallback conversationCallback) {
        mConversationCallbackBinder = new ConversationCallbackStub(conversationCallback);
    }

    /** Default constructor for serialization. */
    private ConversationCallbackDelegateImpl() {
        mConversationCallbackBinder = null;
    }

    @Override
    public void sendMarkAsRead(@NonNull OnDoneCallback onDoneCallback) {
        try {
            requireNonNull(mConversationCallbackBinder)
                    .onMarkAsRead(RemoteUtils.createOnDoneCallbackStub(onDoneCallback));
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendTextReply(@NonNull String replyText, @NonNull OnDoneCallback onDoneCallback) {
        try {
            requireNonNull(mConversationCallbackBinder).onTextReply(
                    RemoteUtils.createOnDoneCallbackStub(onDoneCallback),
                    replyText
            );
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    @KeepFields
    private static class ConversationCallbackStub extends IConversationCallback.Stub {

        @NonNull
        private final ConversationCallback mConversationCallback;

        ConversationCallbackStub(@NonNull ConversationCallback conversationCallback) {
            mConversationCallback = conversationCallback;
        }

        @Override
        public void onMarkAsRead(@NonNull IOnDoneCallback onDoneCallback) {
            RemoteUtils.dispatchCallFromHost(
                    onDoneCallback,
                    "onMarkAsRead", () -> {
                        mConversationCallback.onMarkAsRead();
                        return null;
                    }
            );
        }

        @Override
        public void onTextReply(
                @NonNull IOnDoneCallback onDoneCallback,
                @NonNull String replyText
        ) {
            RemoteUtils.dispatchCallFromHost(
                    onDoneCallback,
                    "onReply", () -> {
                        mConversationCallback.onTextReply(replyText);
                        return null;
                    }
            );
        }
    }
}
