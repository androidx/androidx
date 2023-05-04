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

package androidx.privacysandbox.sdkruntime.client.activity

import android.os.Binder
import android.os.IBinder
import androidx.annotation.GuardedBy
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import org.jetbrains.annotations.TestOnly

/**
 * Singleton class to store instances of [SdkSandboxActivityHandlerCompat] registered by locally
 * loaded SDKs.
 */
internal object LocalSdkActivityHandlerRegistry {

    private val mapsLock = Any()

    @GuardedBy("mapsLock")
    private val handlerToToken =
        hashMapOf<SdkSandboxActivityHandlerCompat, IBinder>()

    @GuardedBy("mapsLock")
    private val tokenToHandler =
        hashMapOf<IBinder, SdkSandboxActivityHandlerCompat>()

    fun register(handler: SdkSandboxActivityHandlerCompat): IBinder =
        synchronized(mapsLock) {
            val existingToken = handlerToToken[handler]
            if (existingToken != null) {
                return existingToken
            }

            val token = Binder()
            handlerToToken[handler] = token
            tokenToHandler[token] = handler

            return token
        }

    fun unregister(handler: SdkSandboxActivityHandlerCompat) =
        synchronized(mapsLock) {
            val unregisteredToken = handlerToToken.remove(handler)
            if (unregisteredToken != null) {
                tokenToHandler.remove(unregisteredToken)
            }
        }

    fun isRegistered(token: IBinder): Boolean =
        synchronized(mapsLock) {
            return tokenToHandler.containsKey(token)
        }

    @TestOnly
    fun getHandlerByToken(token: IBinder): SdkSandboxActivityHandlerCompat? =
        synchronized(mapsLock) {
            return tokenToHandler[token]
        }

    fun notifyOnActivityCreation(
        token: IBinder,
        activityHolder: ActivityHolder
    ) = synchronized(mapsLock) {
        val handler = tokenToHandler[token]
            ?: throw IllegalStateException("There is no registered handler to notify")
        handler.onActivityCreated(activityHolder)
    }
}