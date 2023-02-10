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

package androidx.core.telecom

import android.content.ComponentName
import android.content.Context
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ParcelUuid
import android.os.Process
import android.telecom.CallControl
import android.telecom.CallEndpoint
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.telecom.CallAttributes.Companion.VIDEO_CALL
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.internal.Utils
import java.util.concurrent.Executor
import kotlinx.coroutines.flow.Flow
import java.util.function.Consumer
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.job

/**
 * CallsManager allows VoIP applications to add their calls to the Android system service Telecom.
 * By doing this, other services are aware of your VoIP application calls which leads to a more
 * stable environment. For example, a wearable may be able to answer an incoming call from your
 * application if the call is added to the Telecom system.  VoIP applications that manage calls and
 * do not inform the Telecom system may experience issues with resources (ex. microphone access).
 *
 * <p>
 * Note that access to some telecom information is permission-protected. Your app cannot access the
 * protected information or gain access to protected functionality unless it has the appropriate
 * permissions declared in its manifest file. Where permissions apply, they are noted in the method
 * descriptions.
 */
@RequiresApi(VERSION_CODES.O)
class CallsManager constructor(context: Context) {
    private val mContext: Context = context
    private val mTelecomManager: TelecomManager =
        mContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    // A single declared constant for a direct [Executor], since the coroutines primitives we invoke
    // from the associated callbacks will perform their own dispatch as needed.
    private val mDirectExecutor = Executor { it.run() }

    companion object {
        /** @hide */
        @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
        @IntDef(
            CAPABILITY_BASELINE,
            CAPABILITY_SUPPORTS_VIDEO_CALLING,
            CAPABILITY_SUPPORTS_CALL_STREAMING,
            flag = true
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class Capability

        /**
         * If your VoIP application does not want support any of the capabilities below, then your
         * application can register with [CAPABILITY_BASELINE].
         * <p>
         * Note: Calls can still be added and to the Telecom system but if other services request to
         * perform a capability that is not supported by your application, Telecom will notify the
         * service of the inability to perform the action instead of hitting an error.
         */
        const val CAPABILITY_BASELINE = 0

        /**
         * Flag indicating that your VoIP application supports video calling.
         * This is not an indication that your application is currently able to make a video
         * call, but rather that it has the ability to make video calls (but not necessarily at this
         * time).
         * <p>
         * Whether a call can make a video call is ultimately controlled by
         * [androidx.core.telecom.CallAttributes]s capability
         * [androidx.core.telecom.CallAttributes.CallType]#[VIDEO_CALL],
         * which indicates that particular call is currently capable of making a video call.
         * <p>
         */
        const val CAPABILITY_SUPPORTS_VIDEO_CALLING = 1 shl 1

        /**
         * Flag indicating that this VoIP application supports the call streaming
         * session to stream call audio to another remote device via streaming app.
         */
        const val CAPABILITY_SUPPORTS_CALL_STREAMING = 1 shl 2

        /**
         * @hide
         */
        private const val PACKAGE_HANDLE_ID: String = "Jetpack"

        /**
         * @hide
         */
        private const val PACKAGE_LABEL: String = "Telecom-Jetpack"

        /**
         * @hide
         */
        private const val ERROR_CALLBACKS: String = "Error, when using the [CallControlScope]," +
            " you must first set the [androidx.core.telecom.CallControlCallback]s via " +
            "[CallControlScope]#[setCallback]"
    }

    /**
     * VoIP applications should look at each [Capability] annotated above and call this API in
     * order to start adding calls via [addCall].
     * <p>
     * Note: Registering capabilities must be done before calling [addCall] or an exception will
     * be thrown by [addCall].
     * @throws Exception
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    fun registerAppWithTelecom(@Capability capabilities: Int) {
        var requiredPlatformCapabilities: Int = PhoneAccount.CAPABILITY_SELF_MANAGED

        val phoneAccountBuilder = PhoneAccount.builder(
            getPhoneAccountHandleForPackage(),
            PACKAGE_LABEL
        )

        if (Utils.hasPlatformV2Apis()) {
            requiredPlatformCapabilities = requiredPlatformCapabilities or
                PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }

        // remap and set capabilities
        phoneAccountBuilder.setCapabilities(
            requiredPlatformCapabilities
                or Utils.remapJetpackCapabilitiesToPlatformCapabilities(capabilities)
        )

        // build and register the PhoneAccount via the Platform API
        mTelecomManager.registerPhoneAccount(phoneAccountBuilder.build())
    }

    /**
     * Adds a new call with the specified [CallAttributes] to the telecom service. This method
     * can be used to add both incoming and outgoing calls.
     *
     * @param callAttributes     attributes of the new call (incoming or outgoing, address, etc. )
     * @param block              DSL interface block that will run when the call is ready
     *
     * @throws Exception    if any [CallControlScope] API is called before
     * [CallControlScope.setCallback] or if this module does not support the device build.
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Suppress("ClassVerificationFailure")
    suspend fun addCall(
        callAttributes: CallAttributes,
        block: CallControlScope.() -> Unit
    ) {
        if (Utils.hasPlatformV2Apis()) {
            // CompletableDeferred pauses the execution of this method until the CallControl is
            // returned by the Platform.
            val openResult = CompletableDeferred<CallSession>(parent = coroutineContext.job)
            // CallSession is responsible for handling both CallControl responses from the Platform
            // and propagates CallControlCallbacks that originate in the Platform out to the client.
            val callSession = CallSession(coroutineContext)
            // Setup channels for the CallEventCallbacks that only provide info updates
            val currentEndpointChannel = Channel<CallEndpoint>(Channel.UNLIMITED)
            val availableEndpointChannel = Channel<List<CallEndpoint>>(Channel.UNLIMITED)
            val isMutedChannel = Channel<Boolean>(Channel.UNLIMITED)

            /**
             * The Platform [android.telecom.TelecomManager.addCall] requires a
             * [OutcomeReceiver]#<[CallControl], [CallException]> that will receive the async
             * response of whether the call can be added.
             */
            val callControlOutcomeReceiver =
                object : OutcomeReceiver<CallControl, CallException> {
                    override fun onResult(control: CallControl) {
                        callSession.setCallControl(control)
                        openResult.complete(callSession)
                    }

                    override fun onError(reason: CallException) {
                        // close all channels
                        currentEndpointChannel.close()
                        availableEndpointChannel.close()
                        isMutedChannel.close()
                        // fail if we were still waiting for a CallControl
                        openResult.completeExceptionally(reason)
                    }
                }

            // leverage the platform API
            mTelecomManager.addCall(
                callAttributes.toTelecomCallAttributes(getPhoneAccountHandleForPackage()),
                mDirectExecutor,
                callControlOutcomeReceiver,
                object : android.telecom.CallControlCallback {
                    override fun onSetActive(wasCompleted: Consumer<Boolean>) {
                        callSession.onSetActive(wasCompleted)
                    }

                    override fun onSetInactive(wasCompleted: Consumer<Boolean>) {
                        callSession.onSetInactive(wasCompleted)
                    }

                    override fun onAnswer(videoState: Int, wasCompleted: Consumer<Boolean>) {
                        callSession.onAnswer(videoState, wasCompleted)
                    }

                    override fun onDisconnect(
                        disconnectCause: DisconnectCause,
                        wasCompleted: Consumer<Boolean>
                    ) {
                        callSession.onDisconnect(disconnectCause, wasCompleted)
                    }

                    override fun onCallStreamingStarted(wasCompleted: Consumer<Boolean>) {
                        TODO("Implement with the CallStreaming code")
                    }
                },
                object : android.telecom.CallEventCallback {
                    override fun onCallEndpointChanged(endpoint: CallEndpoint) {
                        currentEndpointChannel.trySend(endpoint).getOrThrow()
                    }

                    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
                        availableEndpointChannel.trySend(endpoints).getOrThrow()
                    }

                    override fun onMuteStateChanged(isMuted: Boolean) {
                        isMutedChannel.trySend(isMuted).getOrThrow()
                    }

                    override fun onCallStreamingFailed(reason: Int) {
                        TODO("Implement with the CallStreaming code")
                    }

                    override fun onEvent(event: String, extras: Bundle) {
                        TODO("Implement when events are agreed upon by ICS and package")
                    }
                }
            )

            openResult.await() /* wait for the platform to provide a CallControl object */
            /* at this point in time we have CallControl object */
            val session = openResult.getCompleted()

            val scope = object : CallControlScope {
                //  handle actionable/handshake events that originate in the platform
                //  and require a response from the client
                override fun setCallback(callControlCallback: CallControlCallback) {
                    session.setCallControlCallback(callControlCallback)
                }

                // handle requests that originate from the client and propagate into platform
                //  return the platforms response which indicates success of the request.
                override fun getCallId(): ParcelUuid {
                    verifySessionCallbacks()
                    return session.getCallId()
                }

                // TODO:: expose in CallControlScope when events are agreed upon by ICS and package
                fun sendEvent(event: String, extras: Bundle) {
                    verifySessionCallbacks()
                    session.sendEvent(event, extras)
                }

                override suspend fun setActive(): Boolean {
                    verifySessionCallbacks()
                    return session.setActive()
                }

                override suspend fun setInactive(): Boolean {
                    verifySessionCallbacks()
                    return session.setInactive()
                }

                override suspend fun answer(callType: Int): Boolean {
                    verifySessionCallbacks()
                    return session.answer(callType)
                }

                override suspend fun disconnect(disconnectCause: DisconnectCause): Boolean {
                    verifySessionCallbacks()
                    return session.disconnect(disconnectCause)
                }

                override suspend fun requestEndpointChange(endpoint: CallEndpoint): Boolean {
                    verifySessionCallbacks()
                    return session.requestEndpointChange(endpoint)
                }

                // Send these events out to the client to collect
                override val currentCallEndpoint: Flow<CallEndpoint> =
                    currentEndpointChannel.receiveAsFlow()

                override val availableEndpoints: Flow<List<CallEndpoint>> =
                    availableEndpointChannel.receiveAsFlow()

                override val isMuted: Flow<Boolean> =
                    isMutedChannel.receiveAsFlow()

                private fun verifySessionCallbacks() {
                    if (!session.hasClientSetCallbacks()) {
                        throw Exception(ERROR_CALLBACKS)
                    }
                }
            }
            // Run the clients code with the session active and exposed via the CallControlScope
            // interface implementation declared above.
            scope.block()
        } else {
            throw Exception(Utils.ERROR_BUILD_VERSION)
        }
    }

    /**
     * @hide
     */
    private fun getPhoneAccountHandleForPackage(): PhoneAccountHandle {
        return PhoneAccountHandle(
            ComponentName(mContext.packageName, mContext.packageName),
            PACKAGE_HANDLE_ID,
            Process.myUserHandle()
        )
    }
}
