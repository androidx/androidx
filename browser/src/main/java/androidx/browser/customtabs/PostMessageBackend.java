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

package androidx.browser.customtabs;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

/**
 * Abstracts a receiver of postMessage events. For example, this could be a service connection like
 * {@link PostMessageServiceConnection} or it could be a local client.
 *
 * <p>This will always be backed by a class on the provider side rather than the client side.
 * However, in the case of {@link PostMessageServiceConnection}, it will defer to the client by
 * making remote calls.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface PostMessageBackend {

    /**
     * Posts a message to the client.
     * @param message The String message to post.
     * @param extras Unused.
     * @return Whether the postMessage was sent successfully.
     */
    boolean onPostMessage(String message, Bundle extras);

    /**
     * Notifies the client that the postMessage channel is ready to be used.
     * @param extras Unused.
     * @return Whether the notification was sent successfully.
     */
    boolean onNotifyMessageChannelReady(Bundle extras);

    /**
     * Notifies the client that the channel has been disconnected.
     * @param appContext The application context.
     */
    void onDisconnectChannel(Context appContext);
}
