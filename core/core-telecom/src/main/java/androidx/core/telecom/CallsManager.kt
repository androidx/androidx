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
import android.os.OutcomeReceiver
import android.os.Process
import android.telecom.CallControl
import android.telecom.CallException
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.internal.JetpackConnectionService
import androidx.core.telecom.internal.utils.Utils
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.job

/**
 * CallsManager allows VoIP applications to add their calls to the Android system service Telecom.
 * By doing this, other services are aware of your VoIP application calls which leads to a more
 * stable environment. For example, a wearable may be able to answer an incoming call from your
 * application if the call is added to the Telecom system.  VoIP applications that manage calls and
 * do not inform the Telecom system may experience issues with resources (ex. microphone access).
 *
 * Note that access to some telecom information is permission-protected. Your app cannot access the
 * protected information or gain access to protected functionality unless it has the appropriate
 * permissions declared in its manifest file. Where permissions apply, they are noted in the method
 * descriptions.
 */
@RequiresApi(VERSION_CODES.O)
class CallsManager constructor(context: Context) {
    private val mContext: Context = context
    private var mPhoneAccount: PhoneAccount? = null
    private val mTelecomManager: TelecomManager =
        mContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    internal val mConnectionService: JetpackConnectionService = JetpackConnectionService()

    // A single declared constant for a direct [Executor], since the coroutines primitives we invoke
    // from the associated callbacks will perform their own dispatch as needed.
    private val mDirectExecutor = Executor { it.run() }

    companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
         *
         * Note: Calls can still be added and to the Telecom system but if other services request to
         * perform a capability that is not supported by your application, Telecom will notify the
         * service of the inability to perform the action instead of hitting an error.
         */
        const val CAPABILITY_BASELINE = 1 shl 0

        /**
         * Flag indicating that your VoIP application supports video calling.
         * This is not an indication that your application is currently able to make a video
         * call, but rather that it has the ability to make video calls (but not necessarily at this
         * time).
         *
         * Whether a call can make a video call is ultimately controlled by
         * [androidx.core.telecom.CallAttributesCompat]s capability
         * [androidx.core.telecom.CallAttributesCompat.CallType]#[CALL_TYPE_VIDEO_CALL],
         * which indicates that particular call is currently capable of making a video call.
         */
        const val CAPABILITY_SUPPORTS_VIDEO_CALLING = 1 shl 1

        /**
         * Flag indicating that this VoIP application supports call streaming. Call streaming means
         * a call can be streamed from a root device to another device to continue the call
         * without completely transferring it. The call continues to take place on the source
         * device, however media and control are streamed to another device.
         * [androidx.core.telecom.CallAttributesCompat.CallType]#[CAPABILITY_SUPPORTS_CALL_STREAMING]
         * must also be set on per call basis in the event an application wants to gate this
         * capability on a stricter basis.
         */
        const val CAPABILITY_SUPPORTS_CALL_STREAMING = 1 shl 2

        // identifiers that indicate the call was established with core-telecom
        internal const val PACKAGE_HANDLE_ID: String = "Jetpack"
        internal const val PACKAGE_LABEL: String = "Telecom-Jetpack"
        internal const val CONNECTION_SERVICE_CLASS =
            "androidx.core.telecom.internal.JetpackConnectionService"
        // fail messages specific to addCall
        internal const val CALL_CREATION_FAILURE_MSG =
            "The call failed to be added."
    }

    /**
     * VoIP applications should look at each [Capability] annotated above and call this API in
     * order to start adding calls via [addCall].
     *
     * Note: Registering capabilities must be done before calling [addCall] or an exception will
     * be thrown by [addCall].
     *
     * @Throws UnsupportedOperationException if the device is on an invalid build
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    fun registerAppWithTelecom(@Capability capabilities: Int) {
        // verify the build version supports this API and throw an exception if not
        Utils.verifyBuildVersion()
        // start to build the PhoneAccount that will be registered via the platform API
        var platformCapabilities: Int = PhoneAccount.CAPABILITY_SELF_MANAGED
        val phoneAccountBuilder = PhoneAccount.builder(
            getPhoneAccountHandleForPackage(),
            PACKAGE_LABEL
        )
        // append additional capabilities if the device is on a U build or above
        if (Utils.hasPlatformV2Apis()) {
            platformCapabilities = PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS or
                Utils.remapJetpackCapabilitiesToPlatformCapabilities(capabilities)
        }
        // remap and set capabilities
        phoneAccountBuilder.setCapabilities(platformCapabilities)
        // build and register the PhoneAccount via the Platform API
        mPhoneAccount = phoneAccountBuilder.build()
        mTelecomManager.registerPhoneAccount(mPhoneAccount)
    }

    /**
     * Adds a new call with the specified [CallAttributesCompat] to the telecom service. This method
     * can be used to add both incoming and outgoing calls.
     *
     * @param callAttributes     attributes of the new call (incoming or outgoing, address, etc. )
     * @param block              DSL interface block that will run when the call is ready
     *
     * @Throws UnsupportedOperationException if the device is on an invalid build
     * @Throws CancellationException if the call failed to be added
     * @Throws CallException if [CallControlScope.setCallback] is not called first within the block
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Suppress("ClassVerificationFailure")
    suspend fun addCall(
        callAttributes: CallAttributesCompat,
        block: CallControlScope.() -> Unit
    ) {
        // This API is not supported for device running anything below Android O (26)
        Utils.verifyBuildVersion()
        // Setup channels for the CallEventCallbacks that only provide info updates
        val callChannels = CallChannels()
        callAttributes.mHandle = getPhoneAccountHandleForPackage()

        // create a call session based off the build version
        @RequiresApi(34)
        if (Utils.hasPlatformV2Apis()) {
            // CompletableDeferred pauses the execution of this method until the CallControl is
            // returned by the Platform.
            val openResult = CompletableDeferred<CallSession>(parent = coroutineContext.job)
            // CallSession is responsible for handling both CallControl responses from the Platform
            // and propagates CallControlCallbacks that originate in the Platform out to the client.
            val callSession = CallSession(coroutineContext)

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
                        callChannels.closeAllChannels()
                        // fail if we were still waiting for a CallControl
                        openResult.cancel(CancellationException(CALL_CREATION_FAILURE_MSG))
                    }
                }

            // leverage the platform API
            mTelecomManager.addCall(
                callAttributes.toCallAttributes(getPhoneAccountHandleForPackage()),
                mDirectExecutor,
                callControlOutcomeReceiver,
                CallSession.CallControlCallbackImpl(callSession),
                CallSession.CallEventCallbackImpl(callChannels)
            )

            openResult.await() /* wait for the platform to provide a CallControl object */
            /* at this point in time we have CallControl object */
            val scope =
                CallSession.CallControlScopeImpl(openResult.getCompleted(), callChannels)

            // Run the clients code with the session active and exposed via the CallControlScope
            // interface implementation declared above.
            scope.block()
        } else {
            // CompletableDeferred pauses the execution of this method until the Connection
            // is created in JetpackConnectionService
            val openResult =
                CompletableDeferred<CallSessionLegacy>(parent = coroutineContext.job)

            mConnectionService.createConnectionRequest(
                mTelecomManager,
                JetpackConnectionService.PendingConnectionRequest(
                    callAttributes, callChannels, coroutineContext, openResult
                )
            )

            openResult.await()

            val scope =
                CallSessionLegacy.CallControlScopeImpl(openResult.getCompleted(), callChannels)

            // Run the clients code with the session active and exposed via the
            // CallControlScope interface implementation declared above.
            scope.block()
        }
    }

    internal fun getPhoneAccountHandleForPackage(): PhoneAccountHandle {
        // This API is not supported for device running anything below Android O (26)
        Utils.verifyBuildVersion()

        val className = if (Utils.hasPlatformV2Apis()) {
            mContext.packageName
        } else {
            CONNECTION_SERVICE_CLASS
        }
        return PhoneAccountHandle(
            ComponentName(mContext.packageName, className),
            PACKAGE_HANDLE_ID,
            Process.myUserHandle()
        )
    }

    internal fun getBuiltPhoneAccount(): PhoneAccount? {
        return mPhoneAccount
    }
}