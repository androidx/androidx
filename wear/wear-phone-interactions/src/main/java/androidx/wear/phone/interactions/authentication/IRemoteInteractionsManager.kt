/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.phone.interactions.authentication

import com.google.wear.services.remoteinteractions.RemoteInteractionsManager
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Forwards remote auth interaction availabilities to [RemoteInteractionsManager].
 */
internal interface IRemoteInteractionsManager {

    /** Whether the availability status API is supported. */
    val isAvailabilityStatusApiSupported: Boolean

    /** Forwards a call [registerRemoteAuthClientStatusListener] to [RemoteInteractionsManager.registerRemoteAuthClientStatusListener]. */
    fun registerRemoteAuthClientStatusListener(executor: Executor, listener: Consumer<Int>)

    /** Forwards a call [unregisterRemoteAuthClientStatusListener] to [RemoteInteractionsManager.unregisterRemoteAuthClientStatusListener]. */
    fun unregisterRemoteAuthClientStatusListener(listener: Consumer<Int>)
}
