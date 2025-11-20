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

package androidx.javascriptengine;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Interface for handling messages received from the other end of a {@link MessagePort} channel.
 *
 * A {@link MessagePortClient} implementation must be provided when creating a message channel
 * using {@link JavaScriptIsolate#provideMessagePort(String, Executor, MessagePortClient)}.
 * <p>
 * This interface's methods are invoked on the {@link Executor} that was passed to
 * {@link JavaScriptIsolate#provideMessagePort}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface MessagePortClient {
    /**
     * Called when a {@link Message} is received by the port.
     *
     * The logic executed for messages after arriving on the port.
     *
     * @param message The {@link Message} that has been received by the port.
     */
    void onMessage(@NonNull Message message);
}
