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

package androidx.core.telecom.internal

import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ParcelUuid
import android.telecom.CallEndpoint
import android.telecom.CallException
import android.telecom.DisconnectCause
import androidx.annotation.RequiresApi
import androidx.core.telecom.CallControlCallback
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * @hide
 */
@Suppress("ClassVerificationFailure")
class CallSession(coroutineContext: CoroutineContext) {
    private val mCoroutineContext = coroutineContext
    private var mPlatformInterface: android.telecom.CallControl? = null
    private var mClientInterface: CallControlCallback? = null

    /**
     * CallControl is set by CallsManager#addCall when the CallControl object is returned by the
     * platform
     */
    fun setCallControl(control: android.telecom.CallControl) {
        mPlatformInterface = control
    }

    /**
     * pass in the clients callback implementation for CallControlCallback that is set in the
     * CallsManager#addCall scope.
     */
    fun setCallControlCallback(clientCallbackImpl: CallControlCallback) {
        mClientInterface = clientCallbackImpl
    }

    fun hasClientSetCallbacks(): Boolean {
        return mClientInterface != null
    }

    /**
     * Custom OutcomeReceiver that handles the Platform responses to a CallControl API call
     */
    @RequiresApi(VERSION_CODES.UPSIDE_DOWN_CAKE)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    inner class CallControlReceiver(deferred: CompletableDeferred<Boolean>) :
        OutcomeReceiver<Void, CallException> {
        private val mResultDeferred: CompletableDeferred<Boolean> = deferred

        override fun onResult(r: Void?) {
            mResultDeferred.complete(true)
        }

        override fun onError(error: CallException) {
            mResultDeferred.complete(false)
        }
    }

    fun getCallId(): ParcelUuid {
        if (Utils.hasPlatformV2Apis()) {
            return mPlatformInterface!!.callId
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
    }

    fun sendEvent(event: String, extras: Bundle) {
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.sendEvent(event, extras)
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun setActive(): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.setActive(Runnable::run, CallControlReceiver(result))
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
        result.await()
        return result.getCompleted()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun setInactive(): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.setInactive(Runnable::run, CallControlReceiver(result))
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
        result.await()
        return result.getCompleted()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun answer(videoState: Int): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.answer(videoState, Runnable::run, CallControlReceiver(result))
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
        result.await()
        return result.getCompleted()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun requestEndpointChange(endpoint: CallEndpoint): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.requestCallEndpointChange(
                endpoint,
                Runnable::run, CallControlReceiver(result)
            )
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
        result.await()
        return result.getCompleted()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
        val result: CompletableDeferred<Boolean> = CompletableDeferred()
        if (Utils.hasPlatformV2Apis()) {
            mPlatformInterface?.disconnect(
                disconnectCause,
                Runnable::run,
                CallControlReceiver(result)
            )
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
        result.await()
        return result.getCompleted()
    }

    /**
     * CallControlCallback
     */
    fun onSetActive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            if (Utils.hasPlatformV2Apis()) {
                val clientResponse: Boolean = mClientInterface!!.onSetActive()
                wasCompleted.accept(clientResponse)
            }
        }
    }

    fun onSetInactive(wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            if (Utils.hasPlatformV2Apis()) {
                val clientResponse: Boolean = mClientInterface!!.onSetInactive()
                wasCompleted.accept(clientResponse)
            }
        }
    }

    fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            if (Utils.hasPlatformV2Apis()) {
                val clientResponse: Boolean = mClientInterface!!.onAnswer(videoState)
                wasCompleted.accept(clientResponse)
            }
        }
    }

    fun onDisconnect(cause: DisconnectCause, wasCompleted: Consumer<Boolean>) {
        CoroutineScope(mCoroutineContext).launch {
            if (Utils.hasPlatformV2Apis()) {
                val clientResponse: Boolean = mClientInterface!!.onDisconnect(cause)
                wasCompleted.accept(clientResponse)
            }
        }
    }
}