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

package androidx.webkit;

import android.os.Handler;
import android.webkit.WebMessagePort;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * <p>The Java representation of the
 * <a href="https://html.spec.whatwg.org/multipage/comms.html#messageport">
 * HTML5 message ports.</a>
 *
 * <p>A Message port represents one endpoint of a Message Channel. In Android
 * webview, there is no separate Message Channel object. When a message channel
 * is created, both ports are tangled to each other and started, and then
 * returned in a MessagePort array, see {@link WebViewCompat#createWebMessageChannel}
 * for creating a message channel.
 *
 * <p>When a message port is first created or received via transfer, it does not
 * have a WebMessageCallback to receive web messages. The messages are queued until
 * a WebMessageCallback is set.
 *
 * <p>A message port should be closed when it is not used by the embedder application
 * anymore. A closed port cannot be transferred or cannot be reopened to send
 * messages. Close can be called multiple times.
 *
 * <p>When a port is transferred to JS, it cannot be used to send or receive messages
 * at the Java side anymore. Different from HTML5 Spec, a port cannot be transferred
 * if one of these has ever happened: i. a message callback was set, ii. a message was
 * posted on it. A transferred port cannot be closed by the application, since
 * the ownership is also transferred.
 *
 * <p>It is possible to transfer both ports of a channel to JS, for example for
 * communication between subframes.
 */
public abstract class WebMessagePortCompat {
    /**
     * The listener for handling MessagePort events. The message callback
     * methods are called on the main thread. If the embedder application
     * wants to receive the messages on a different thread, it can do this
     * by passing a Handler in
     *  {@link WebMessagePortCompat#setWebMessageCallback(Handler, WebMessageCallbackCompat)}.
     * In the latter case, the application should be extra careful for thread safety
     * since WebMessagePort methods should be called on main thread.
     */
    public abstract static class WebMessageCallbackCompat {
        /**
         * Message callback for receiving onMessage events.
         *
         * @param port  the WebMessagePort that the message is destined for
         * @param message  the message from the entangled port.
         */
        public void onMessage(@NonNull WebMessagePortCompat port,
                @Nullable WebMessageCompat message) { }
    }

    /**
     * @hide disallow app devs to extend this class.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public WebMessagePortCompat() { }

    /**
     * Post a WebMessage to the entangled port.
     *
     * @param message  the message from Java to JS.
     *
     * @throws IllegalStateException If message port is already transferred or closed.
     */
    public abstract void postMessage(@NonNull WebMessageCompat message);

    /**
     * Close the message port and free any resources associated with it.
     */
    public abstract void close();

    /**
     * Sets a callback to receive message events on the main thread.
     *
     * @param callback  the message callback.
     */
    public abstract void setWebMessageCallback(@NonNull WebMessageCallbackCompat callback);

    /**
     * Sets a callback to receive message events on the handler that is provided
     * by the application. If the handler is null the message events are received on the main
     * thread.
     *
     * @param handler   the handler to receive the message events.
     * @param callback  the message callback.
     */
    public abstract void setWebMessageCallback(@Nullable Handler handler,
            @NonNull WebMessageCallbackCompat callback);

    /**
     * Internal getter returning the private {@link WebMessagePort} implementing this class.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract WebMessagePort getFrameworkPort();
}
