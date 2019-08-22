/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection;

import androidx.annotation.NonNull;

/**
 * Implementation of this class are responsible to handle command from frontend and
 * send back events.
 */
public abstract class Inspector {

    /**
     * @param connection a connection object that allows to send events to studio
     */
    public Inspector(@NonNull Connection connection) {
    }

    /**
     * Called when this inspector  is no longer needed.
     * <p>
     * Agent should use this callback to unsubscribe from any events that it is listening to.
     */
    public void onDispose() {
    }

    /**
     * An inspector can implement this to handle incoming commands.
     * <p>
     * Every command should be replied with a {@link CommandCallback#reply(byte[])} call on the
     * given {@code callback} object.
     *
     * @param data a raw byte array of the command sent by studio.
     * @param callback a callback to reply on the given command.
     */
    public abstract void onReceiveCommand(@NonNull byte[] data, @NonNull CommandCallback callback);

    /**
     * Callback to reply on an command from the studio
     */
    public interface CommandCallback {
        /**
         * Sends a response on the previously handled command.
         *
         * @param response a raw byte array of the response to studio command.
         */
        void reply(@NonNull byte[] response);
    }
}
