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
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.Process
import android.telecom.CallControl
import android.telecom.CallControlCallback
import android.telecom.CallEventCallback
import android.telecom.CallException
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.core.telecom.CallAttributesCompat.Companion.CALL_TYPE_VIDEO_CALL
import androidx.core.telecom.extensions.CallsManagerExtensions
import androidx.core.telecom.extensions.ExtensionInitializationScope
import androidx.core.telecom.extensions.ExtensionInitializationScopeImpl
import androidx.core.telecom.internal.AddCallResult
import androidx.core.telecom.internal.CallChannels
import androidx.core.telecom.internal.CallSession
import androidx.core.telecom.internal.CallSessionLegacy
import androidx.core.telecom.internal.JetpackConnectionService
import androidx.core.telecom.internal.PreCallEndpoints
import androidx.core.telecom.internal.utils.AudioManagerUtil.Companion.getAvailableAudioDevices
import androidx.core.telecom.internal.utils.EndpointUtils.Companion.getEndpointsFromAudioDeviceInfo
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.internal.utils.Utils.Companion.remapJetpackCapsToPlatformCaps
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * CallsManager allows VoIP applications to add their calls to the Android system service Telecom.
 * By doing this, other services are aware of your VoIP application calls which leads to a more
 * stable environment. For example, a wearable may be able to answer an incoming call from your
 * application if the call is added to the Telecom system. VoIP applications that manage calls and
 * do not inform the Telecom system may experience issues with resources (ex. microphone access).
 *
 * Note that access to some telecom information is permission-protected. Your app cannot access the
 * protected information or gain access to protected functionality unless it has the appropriate
 * permissions declared in its manifest file. Where permissions apply, they are noted in the method
 * descriptions.
 */
@RequiresApi(VERSION_CODES.O)
public class CallsManager(context: Context) : CallsManagerExtensions {
    private val mContext: Context = context
    private var mPhoneAccount: PhoneAccount? = null
    private val mTelecomManager: TelecomManager =
        mContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
    internal val mConnectionService: JetpackConnectionService = JetpackConnectionService()

    // A single declared constant for a direct [Executor], since the coroutines primitives we invoke
    // from the associated callbacks will perform their own dispatch as needed.
    private val mDirectExecutor = Executor { it.run() }
    // This list is modified in [getAvailableStartingCallEndpoints] and used to store the
    // mappings of jetpack call endpoint UUIDs
    private var mPreCallEndpointsList: MutableList<PreCallEndpoints> = mutableListOf()

    public companion object {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
        @IntDef(
            CAPABILITY_BASELINE,
            CAPABILITY_SUPPORTS_VIDEO_CALLING,
            CAPABILITY_SUPPORTS_CALL_STREAMING,
            flag = true
        )
        @Retention(AnnotationRetention.SOURCE)
        public annotation class Capability

        /**
         * Set on Jetpack Connections that are emulating the transactional APIs using
         * ConnectionService.
         */
        internal const val EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED =
            "android.telecom.extra.VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED"

        /**
         * Event sent from the call producer application to the external call surfaces to notify
         * them that the call has been successfully setup and is ready to be used.
         */
        internal const val EVENT_CALL_READY = "androidx.core.telecom.EVENT_CALL_READY"

        /**
         * The connection is using transactional call APIs.
         *
         * The underlying connection was added as a transactional call via the
         * [TelecomManager.addCall] API.
         */
        internal const val PROPERTY_IS_TRANSACTIONAL = 0x00008000

        /**
         * If your VoIP application does not want support any of the capabilities below, then your
         * application can register with [CAPABILITY_BASELINE].
         *
         * Note: Calls can still be added and to the Telecom system but if other services request to
         * perform a capability that is not supported by your application, Telecom will notify the
         * service of the inability to perform the action instead of hitting an error.
         */
        public const val CAPABILITY_BASELINE: Int = 1 shl 0

        /**
         * Flag indicating that your VoIP application supports video calling. This is not an
         * indication that your application is currently able to make a video call, but rather that
         * it has the ability to make video calls (but not necessarily at this time).
         *
         * Whether a call can make a video call is ultimately controlled by
         * [androidx.core.telecom.CallAttributesCompat]s capability
         * [androidx.core.telecom.CallAttributesCompat.CallType]#[CALL_TYPE_VIDEO_CALL], which
         * indicates that particular call is currently capable of making a video call.
         */
        public const val CAPABILITY_SUPPORTS_VIDEO_CALLING: Int = 1 shl 1

        /**
         * Flag indicating that this VoIP application supports call streaming. Call streaming means
         * a call can be streamed from a root device to another device to continue the call without
         * completely transferring it. The call continues to take place on the source device,
         * however media and control are streamed to another device.
         * [androidx.core.telecom.CallAttributesCompat.CallType]#[CAPABILITY_SUPPORTS_CALL_STREAMING]
         * must also be set on per call basis in the event an application wants to gate this
         * capability on a stricter basis.
         */
        public const val CAPABILITY_SUPPORTS_CALL_STREAMING: Int = 1 shl 2

        // identifiers that indicate the call was established with core-telecom
        internal const val PACKAGE_HANDLE_ID: String = "Jetpack"
        internal const val PACKAGE_LABEL: String = "Telecom-Jetpack"
        internal const val CONNECTION_SERVICE_CLASS =
            "androidx.core.telecom.internal.JetpackConnectionService"
        internal const val PLACEHOLDER_VALUE_ACCOUNT_BUNDLE = "isCoreTelecomAccount"

        // fail messages specific to addCall
        internal const val CALL_CREATION_FAILURE_MSG = "The call failed to be added."
        internal const val ADD_CALL_TIMEOUT = 5000L
        internal const val SWITCH_TO_SPEAKER_TIMEOUT = 1000L
        private val TAG: String = CallsManager::class.java.simpleName.toString()
    }

    /**
     * VoIP applications should look at each [Capability] annotated above and call this API in order
     * to start adding calls via [addCall]. Registering capabilities must be done before calling
     * [addCall] or an exception will be thrown by [addCall]. The capabilities can be updated by
     * re-registering.
     *
     * Note: There is no need to unregister at any point. Telecom will handle unregistering once the
     * application using core-telecom has been removed from the device.
     *
     * @throws UnsupportedOperationException if the device is on an invalid build
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    public fun registerAppWithTelecom(@Capability capabilities: Int) {
        // verify the build version supports this API and throw an exception if not
        Utils.verifyBuildVersion()

        val phoneAccountBuilder =
            PhoneAccount.builder(getPhoneAccountHandleForPackage(), PACKAGE_LABEL)

        // remap and set capabilities
        phoneAccountBuilder.setCapabilities(remapJetpackCapsToPlatformCaps(capabilities))
        // see b/343674176. Some OEMs expect the PhoneAccount.getExtras() to be non-null
        // see b/352526256. The bundle must contain a placeholder value. otherwise, the bundle
        // empty bundle will be nulled out on reboot.
        val defaultBundle = Bundle()
        defaultBundle.putBoolean(PLACEHOLDER_VALUE_ACCOUNT_BUNDLE, true)
        phoneAccountBuilder.setExtras(defaultBundle)

        // build and register the PhoneAccount via the Platform API
        mPhoneAccount = phoneAccountBuilder.build()
        mTelecomManager.registerPhoneAccount(mPhoneAccount)
    }

    /**
     * Adds a new call with the specified [CallAttributesCompat] to the telecom service. This method
     * can be used to add both incoming and outgoing calls. Once the call is ready to be
     * disconnected, use the [CallControlScope.disconnect].
     *
     * <b>Call Lifecycle</b>: Your app is given foreground execution priority as long as you have an
     * ongoing call and are posting a [android.app.Notification.CallStyle] notification within 5
     * seconds of adding the call via this method. When your application is given foreground
     * execution priority, your app is treated as a foreground service. Foreground execution
     * priority will prevent the [android.app.ActivityManager] from killing your application when it
     * is placed the background. Foreground execution priority is removed from your app when all of
     * your app's calls terminate or your app no longer posts a valid notification.
     * - Other things that should be noted:
     *     - For outgoing calls, your application should either immediately post a
     *       [android.app.Notification.CallStyle] notification or delay adding the call via this
     *       addCall method until the remote side is ready.
     *     - addCall will NOT complete until the call session has ended. Instead, the addCall block,
     *       which is called the [CallControlScope], will run once the call has been added. Do not
     *       put addCall in a function expecting it to return or add logic after the addCall request
     *       that is important for the call session.
     *     - Each lambda function (onAnswer, onDisconnect, onSetActive, onSetInactive) will be
     *       invoked by Telecom whenever the system needs your VoIP application to change the call
     *       state. For example, if there is an ongoing VoIP call in your application and the system
     *       receives a sim call, Telecom will invoke onSetInactive to place your call on
     *       hold/inactive if the user answers the incoming sim call. These events may not occur
     *       during most calls but should still be implemented in the event Telecom needs to
     *       manipulate your applications call state.
     *     - Each lambda function (onAnswer, onDisconnect, onSetActive, onSetInactive) has a timeout
     *       of 5000 milliseconds. Failing to complete the suspend fun before the timeout will
     *       result in a failed transaction.
     *     - Telecom assumes each callback (onAnswer, onDisconnect, onSetActive, onSetInactive) is
     *       handled successfully on the client side. If the callback cannot be completed, an
     *       Exception should be thrown. Telecom will rethrow the Exception and tear down the call
     *       session.
     *
     * @param callAttributes attributes of the new call (incoming or outgoing, address, etc. )
     * @param onAnswer where callType is the audio/video state the call should be answered as.
     *   Telecom is informing your VoIP application to answer an incoming call and set it to active.
     *   Telecom is requesting this on behalf of an system service (e.g. Automotive service) or a
     *   device (e.g. Wearable).
     * @param onDisconnect where disconnectCause represents the cause for disconnecting the call.
     *   Telecom is informing your VoIP application to disconnect the incoming call. Telecom is
     *   requesting this on behalf of an system service (e.g. Automotive service) or a device (e.g.
     *   Wearable).
     * @param onSetActive Telecom is informing your VoIP application to set the call active. Telecom
     *   is requesting this on behalf of an system service (e.g. Automotive service) or a device
     *   (e.g. Wearable).
     * @param onSetInactive Telecom is informing your VoIP application to set the call inactive.
     *   This is the same as holding a call for two endpoints but can be extended to setting a
     *   meeting inactive. Telecom is requesting this on behalf of an system service (e.g.
     *   Automotive service) or a device (e.g.Wearable). Note: Your app must stop using the
     *   microphone and playing incoming media when returning.
     * @param block DSL interface block that will run when the call is ready
     * @throws UnsupportedOperationException if the device is on an invalid build
     * @throws CallException if the platform cannot add the call (e.g. reached max # of calls) or
     *   failed with an exception (e.g. call was already removed)
     * @throws CancellationException if the call failed to be added within 5000 milliseconds
     */
    @RequiresPermission(value = "android.permission.MANAGE_OWN_CALLS")
    public suspend fun addCall(
        callAttributes: CallAttributesCompat,
        onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
        onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        onSetActive: suspend () -> Unit,
        onSetInactive: suspend () -> Unit,
        block: CallControlScope.() -> Unit
    ): Unit = coroutineScope {
        // Provide a default empty handler for onEvent
        addCall(
            callAttributes,
            onAnswer,
            onDisconnect,
            onSetActive,
            onSetInactive,
            onEvent = { _, _ -> },
            block
        )
    }

    /** Represents an event sent from an InCallService to this Call. */
    @RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
    public data class CallEvent(public val event: String, public val extras: Bundle)

    /**
     * Adds a call with extensions support, which allows an app to implement optional additional
     * actions that go beyond the scope of a call, such as information about meeting participants
     * and icons.
     *
     * @param callAttributes attributes of the new call (incoming or outgoing, address, etc. )
     * @param onAnswer where callType is the audio/video state the call should be answered as.
     *   Telecom is informing your VoIP application to answer an incoming call and set it to active.
     *   Telecom is requesting this on behalf of an system service (e.g. Automotive service) or a
     *   device (e.g. Wearable).
     * @param onDisconnect where disconnectCause represents the cause for disconnecting the call.
     *   Telecom is informing your VoIP application to disconnect the incoming call. Telecom is
     *   requesting this on behalf of an system service (e.g. Automotive service) or a device (e.g.
     *   Wearable).
     * @param onSetActive Telecom is informing your VoIP application to set the call active. Telecom
     *   is requesting this on behalf of an system service (e.g. Automotive service) or a device
     *   (e.g. Wearable).
     * @param onSetInactive Telecom is informing your VoIP application to set the call inactive.
     *   This is the same as holding a call for two endpoints but can be extended to setting a
     *   meeting inactive. Telecom is requesting this on behalf of an system service (e.g.
     *   Automotive service) or a device (e.g.Wearable). Note: Your app must stop using the
     *   microphone and playing incoming media when returning.
     * @param init The scope used to first initialize Extensions that will be used when the call is
     *   first notified to the platform and UX surfaces. Once the call is set up, the user's
     *   implementation of [ExtensionInitializationScope.onCall] will be called.
     * @see CallsManagerExtensions.addCallWithExtensions
     */
    @ExperimentalAppActions
    override suspend fun addCallWithExtensions(
        callAttributes: CallAttributesCompat,
        onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
        onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        onSetActive: suspend () -> Unit,
        onSetInactive: suspend () -> Unit,
        init: suspend ExtensionInitializationScope.() -> Unit
    ): Unit = coroutineScope {
        Log.v(TAG, "addCall: begin")
        val eventFlow = MutableSharedFlow<CallEvent>()
        val scope = ExtensionInitializationScopeImpl()
        scope.init()
        val extensionJob = launch {
            Log.d(TAG, "addCall: connecting extensions")
            scope.collectEvents(this, eventFlow)
        }
        Log.v(TAG, "addCall: init complete")
        addCall(
            callAttributes,
            onAnswer,
            onDisconnect,
            onSetActive,
            onSetInactive,
            onEvent = { event, extras -> eventFlow.emit(CallEvent(event, extras)) }
        ) {
            Log.d(TAG, "addCall: invoking delegates")
            scope.invokeDelegate(this)
        }
        // Ensure that when the call ends, we also cancel any ongoing coroutines/flows as part of
        // extension work
        Log.d(TAG, "addCall: cancelling extension job")
        extensionJob.cancelAndJoin()
    }

    /**
     * Fetch the current available call audio endpoints that can be used for a new call session. The
     * callback flow will be continuously updated until the call session is established via
     * [addCall]. Once [addCall] is invoked with a
     * [CallAttributesCompat.preferredStartingCallEndpoint], the callback containing the
     * [CallEndpointCompat] will stop receiving updates. If the flow is canceled before adding the
     * call, the [CallAttributesCompat.preferredStartingCallEndpoint] will be voided. If a call
     * session isn't started, the flow should be cleaned up client-side by calling cancel() from the
     * same [kotlinx.coroutines.CoroutineScope] the [callbackFlow] is collecting in.
     *
     * Note: The endpoints emitted will be sorted by the [CallEndpointCompat.type] . See
     * [CallEndpointCompat.compareTo] for the ordering. The first element in the list will be the
     * recommended call endpoint to default to for the user.
     *
     * @return a flow of [CallEndpointCompat]s that can be used for a new call session
     */
    public fun getAvailableStartingCallEndpoints(): Flow<List<CallEndpointCompat>> = callbackFlow {
        val audioManager = mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // [AudioDeviceInfo] <-- AudioManager / platform
        val initialAudioDevices = getAvailableAudioDevices(audioManager)
        // [AudioDeviceInfo] --> [CallEndpoints]
        val initialEndpoints = getEndpointsFromAudioDeviceInfo(mContext, initialAudioDevices)

        val preCallEndpoints = PreCallEndpoints(initialEndpoints.toMutableList(), this.channel)
        mPreCallEndpointsList.add(preCallEndpoints)

        val audioDeviceCallback =
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    if (addedDevices != null) {
                        preCallEndpoints.endpointsAddedUpdate(
                            getEndpointsFromAudioDeviceInfo(mContext, addedDevices.toList())
                        )
                    }
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    if (removedDevices != null) {
                        preCallEndpoints.endpointsRemovedUpdate(
                            getEndpointsFromAudioDeviceInfo(mContext, removedDevices.toList())
                        )
                    }
                }
            }
        // The following callback is needed in the event the user connects or disconnects
        // and audio device after this API is called.
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null /*handler*/)
        // Send the initial list of pre-call [CallEndpointCompat]s out to the client. They
        // will be emitted and cached in the Flow & only consumed once the client has
        // collected it.
        trySend(initialEndpoints)
        awaitClose {
            Log.i(TAG, "getAvailableStartingCallEndpoints: awaitClose")
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
            mPreCallEndpointsList.remove(preCallEndpoints)
        }
    }

    /**
     * Internal version of addCall, which also allows components in the library to consume generic
     * events generated from the remote InCallServices. This facilitates the creation of Jetpack
     * defined extensions.
     *
     * @param onEvent Incoming {@link CallEvents} from an InCallService implementation
     * @see addCall For more documentation on the operations/parameters of this class
     */
    @Suppress("ClassVerificationFailure")
    @OptIn(ExperimentalCoroutinesApi::class)
    @RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)
    public suspend fun addCall(
        callAttributes: CallAttributesCompat,
        onAnswer: suspend (callType: @CallAttributesCompat.Companion.CallType Int) -> Unit,
        onDisconnect: suspend (disconnectCause: DisconnectCause) -> Unit,
        onSetActive: suspend () -> Unit,
        onSetInactive: suspend () -> Unit,
        onEvent: suspend (event: String, extras: Bundle) -> Unit,
        block: CallControlScope.() -> Unit
    ) {
        // This API is not supported for device running anything below Android O (26)
        Utils.verifyBuildVersion()
        // Setup channels for the CallEventCallbacks that only provide info updates
        val callChannels = CallChannels()
        callAttributes.mHandle = getPhoneAccountHandleForPackage()
        // This variable controls the addCall execution in the calling activity. AddCall will block
        // for the duration of the session.  When the session is terminated via a disconnect or
        // exception, addCall will unblock.
        val blockingSessionExecution = CompletableDeferred<Unit>(parent = coroutineContext.job)

        val preCallEndpoints: PreCallEndpoints? =
            mPreCallEndpointsList.find {
                it.isCallEndpointBeingTracked(callAttributes.preferredStartingCallEndpoint)
            }

        // create a call session based off the build version
        @RequiresApi(34)
        if (Utils.hasPlatformV2Apis()) {
            // CompletableDeferred pauses the execution of this method until the CallControl is
            // returned by the Platform.
            val openResult = CompletableDeferred<AddCallResult>(parent = coroutineContext.job)
            // CallSession is responsible for handling both CallControl responses from the Platform
            // and propagates CallControlCallbacks that originate in the Platform out to the client.
            val callSession =
                CallSession(
                    coroutineContext,
                    callAttributes,
                    onAnswer,
                    onDisconnect,
                    onSetActive,
                    onSetInactive,
                    preCallEndpoints,
                    callChannels,
                    onEvent,
                    blockingSessionExecution
                )

            /**
             * The Platform [android.telecom.TelecomManager.addCall] requires a
             * [OutcomeReceiver]#<[CallControl], [CallException]> that will receive the async
             * response of whether the call can be added.
             */
            val callControlOutcomeReceiver =
                object : OutcomeReceiver<CallControl, CallException> {
                    override fun onResult(control: CallControl) {
                        callSession.setCallControl(control)
                        openResult.complete(AddCallResult.SuccessCallSession())
                    }

                    override fun onError(reason: CallException) {
                        callChannels.closeAllChannels()
                        openResult.complete(AddCallResult.Error(reason.code))
                    }
                }

            // leverage the platform API
            mTelecomManager.addCall(
                callAttributes.toCallAttributes(getPhoneAccountHandleForPackage()),
                mDirectExecutor,
                callControlOutcomeReceiver,
                callSession as CallControlCallback,
                callSession as CallEventCallback
            )

            pauseExecutionUntilCallIsReadyOrTimeout(openResult, blockingSessionExecution)

            /* at this point in time we have CallControl object */
            val scope =
                CallSession.CallControlScopeImpl(
                    callSession,
                    callChannels,
                    blockingSessionExecution,
                    coroutineContext
                )

            callSession.sendEvent(EVENT_CALL_READY)
            callSession.maybeSwitchStartingEndpoint(callAttributes.preferredStartingCallEndpoint)

            // Run the clients code with the session active and exposed via the CallControlScope
            // interface implementation declared above.
            scope.block()
        } else {
            // CompletableDeferred pauses the execution of this method until the Connection
            // is created in JetpackConnectionService
            val openResult = CompletableDeferred<AddCallResult>(parent = coroutineContext.job)

            val request =
                JetpackConnectionService.PendingConnectionRequest(
                    UUID.randomUUID().toString(),
                    callAttributes,
                    callChannels,
                    coroutineContext,
                    openResult,
                    onAnswer,
                    onDisconnect,
                    onSetActive,
                    onSetInactive,
                    onEvent,
                    callAttributes.preferredStartingCallEndpoint,
                    preCallEndpoints,
                    blockingSessionExecution
                )

            mConnectionService.createConnectionRequest(mTelecomManager, request)

            pauseExecutionUntilCallIsReadyOrTimeout(openResult, blockingSessionExecution, request)

            val result = openResult.getCompleted() as AddCallResult.SuccessCallSessionLegacy
            val scope =
                CallSessionLegacy.CallControlScopeImpl(
                    result.callSessionLegacy,
                    callChannels,
                    blockingSessionExecution,
                    coroutineContext
                )

            // Run the clients code with the session active and exposed via the
            // CallControlScope interface implementation declared above.
            scope.block()
        }
        preCallEndpoints?.mSendChannel?.close()
        blockingSessionExecution.await()
    }

    @ExperimentalCoroutinesApi
    @VisibleForTesting
    internal suspend fun pauseExecutionUntilCallIsReadyOrTimeout(
        openResult: CompletableDeferred<AddCallResult>,
        blockingSessionExecution: CompletableDeferred<Unit>? = null,
        request: JetpackConnectionService.PendingConnectionRequest? = null,
    ) {
        try {
            withTimeout(ADD_CALL_TIMEOUT) {
                // This log will print once a request is sent to the platform to add a new call.
                // It is necessary to pause execution so Core-Telecom does not run the clients
                // CallControlScope before the call is returned from the platform.
                Log.i(
                    TAG,
                    "addCall: pausing [$coroutineContext] execution" +
                        " until the CallControl or Connection is ready"
                )
                openResult.await()
            }
        } catch (timeout: TimeoutCancellationException) {
            // If this block is entered, the platform failed to create the call in time and hung.
            Log.i(TAG, "addCall: timeout hit; canceling call in context=[$coroutineContext]")
            if (request != null) {
                JetpackConnectionService.mPendingConnectionRequests.remove(request)
            }
            blockingSessionExecution?.complete(Unit)
            openResult.cancel(CancellationException(CALL_CREATION_FAILURE_MSG))
        }
        // In the event the platform encountered an exception while adding the call request,
        // re-throw the call exception out to the client
        val result = openResult.getCompleted()
        if (result is AddCallResult.Error) {
            blockingSessionExecution?.complete(Unit)
            throw CallException(
                androidx.core.telecom.CallException.fromTelecomCode(result.errorCode)
            )
        }
        // This log will print once the CallControl object or Connection is returned from the
        // the platform. This means the call was added successfully and Core-Telecom is ready to
        // run the clients CallControlScope block.
        Log.i(TAG, "addCall: creating call session and running the clients scope")
    }

    internal fun getPhoneAccountHandleForPackage(): PhoneAccountHandle {
        // This API is not supported for device running anything below Android O (26)
        Utils.verifyBuildVersion()

        val className =
            if (Utils.hasPlatformV2Apis()) {
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
