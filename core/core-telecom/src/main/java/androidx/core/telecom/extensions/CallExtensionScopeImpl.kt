/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.core.telecom.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telecom.Call
import android.telecom.Call.Callback
import android.telecom.InCallService
import android.telecom.PhoneAccount
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.utils.Utils
import androidx.core.telecom.util.ExperimentalAppActions
import java.util.Collections
import kotlin.coroutines.resume
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Encapsulates the [extensionCapability] associated with a call extension and the
 * [onExchangeComplete], which is called when capability exchange has completed and the extension
 * should be initialized.
 */
@OptIn(ExperimentalAppActions::class)
internal data class CallExtensionCreator(
    val extensionCapability: Capability,
    val onExchangeComplete: suspend (Capability?, CapabilityExchangeListenerRemote?) -> Unit
)

/**
 * Represents the result of performing capability exchange with the underlying VOIP application.
 * Contains the capabilities that the VOIP app supports and the remote binder implementation used to
 * communicate with the remote process.
 */
@OptIn(ExperimentalAppActions::class)
private data class CapabilityExchangeResult(
    val voipCapabilities: Set<Capability>,
    val extensionInitializationBinder: CapabilityExchangeListenerRemote
)

/**
 * The scope enclosing an ongoing call that allows the user to set up optional extensions to the
 * call.
 *
 * Once the call is connected to the remote, [onConnected] will be called and the [Call] is ready to
 * be used with extensions:
 * ```
 * connectExtensions(context, call) {
 *   // initialize extensions
 *   onConnected { call ->
 *     // call extensions are ready to be used along with the traditional call APIs
 *   }
 * }
 * ```
 */
@OptIn(ExperimentalAppActions::class)
@RequiresApi(Build.VERSION_CODES.O)
internal class CallExtensionScopeImpl(
    private val applicationContext: Context,
    private val callScope: CoroutineScope,
    private val call: Call
) : CallExtensionScope {
    companion object {
        internal const val TAG = "CallExtensions"

        /** Set on Connections that are using ConnectionService+AUTO specific extension layer. */
        internal const val EXTRA_VOIP_API_VERSION = "android.telecom.extra.VOIP_API_VERSION"

        internal const val CAPABILITY_EXCHANGE_VERSION = 1
        internal const val RESOLVE_EXTENSIONS_TYPE_TIMEOUT_MS = 1000L
        internal const val CAPABILITY_EXCHANGE_TIMEOUT_MS = 1000L

        /** Constants used to denote the extension level supported by the VOIP app. */
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(NONE, EXTRAS, CAPABILITY_EXCHANGE, UNKNOWN)
        internal annotation class CapabilityExchangeType

        internal const val NONE = 0
        internal const val EXTRAS = 1
        internal const val CAPABILITY_EXCHANGE = 2
        internal const val UNKNOWN = 3
    }

    private var delegate: (suspend (Call) -> Unit)? = null
    // Creates a Set of creators that will be used to create and  maintain the extension connection
    // with the remote VOIP application.
    // This has to be done this way because actions are set AFTER the extension is registered, so we
    // need to query the Capability after CallExtensionScope initialization has completed.
    private val callExtensionCreators = HashSet<() -> CallExtensionCreator>()

    override fun onConnected(block: suspend (Call) -> Unit) {
        delegate = block
    }

    override fun addParticipantExtension(
        onActiveParticipantChanged: suspend (Participant?) -> Unit,
        onParticipantsUpdated: suspend (Set<Participant>) -> Unit
    ): ParticipantExtensionRemoteImpl {
        val extension =
            ParticipantExtensionRemoteImpl(
                callScope,
                onActiveParticipantChanged,
                onParticipantsUpdated
            )
        registerExtension {
            CallExtensionCreator(
                extensionCapability =
                    Capability().apply {
                        featureId = Extensions.PARTICIPANT
                        featureVersion = ParticipantExtensionImpl.VERSION
                        supportedActions = extension.actions
                    },
                onExchangeComplete = extension::onExchangeComplete
            )
        }
        return extension
    }

    override fun addLocalCallSilenceExtension(
        onIsLocallySilencedUpdated: suspend (Boolean) -> Unit
    ): LocalCallSilenceExtensionRemoteImpl {
        val extension = LocalCallSilenceExtensionRemoteImpl(callScope, onIsLocallySilencedUpdated)
        registerExtension {
            CallExtensionCreator(
                extensionCapability =
                    Capability().apply {
                        featureId = Extensions.LOCAL_CALL_SILENCE
                        featureVersion = LocalCallSilenceExtensionImpl.VERSION
                        supportedActions = extension.actions
                    },
                onExchangeComplete = extension::onExchangeComplete
            )
        }
        return extension
    }

    /**
     * Register an extension with this call, whose capability will be negotiated with the VOIP
     * application.
     *
     * Once capability exchange completes, the shared [Capability.featureId] will be used to map the
     * negotiated capability with this extension and [receiver] will be called with a valid
     * negotiated [Capability] and interface to use to create/manage this extension with the remote.
     *
     * @param receiver The receiver that will be called once capability exchange completes and we
     *   either have a valid negotiated capability or a `null` Capability if the remote side does
     *   not support this capability.
     */
    internal fun registerExtension(receiver: () -> CallExtensionCreator) {
        callExtensionCreators.add(receiver)
    }

    /**
     * Invoke the stored [onConnected] block once capability exchange has completed and the
     * associated extensions have been set up.
     */
    private suspend fun invokeDelegate() {
        Log.i(TAG, "invokeDelegate")
        delegate?.invoke(call)
    }

    /**
     * Internal helper used to help resolve the call extension type. This is invoked before
     * capability exchange between the [InCallService] and VOIP app starts to ensure the necessary
     * features are enabled to support it.
     *
     * If the call is placed using the V1.5 ConnectionService + Extensions Library (Auto Case), the
     * call will have the [EXTRA_VOIP_API_VERSION] defined in the extras. The call extension would
     * be resolved as [EXTRAS].
     *
     * If the call is using the v2 APIs and the phone account associated with the call supports
     * transactional ops (U+) or the call has the [CallsManager.PROPERTY_IS_TRANSACTIONAL] property
     * defined (on V devices), then the extension type is [CAPABILITY_EXCHANGE].
     *
     * If the call is added via [CallsManager.addCall] on pre-U devices and the
     * [CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED] is present in the call extras,
     * the extension type also resolves to [CAPABILITY_EXCHANGE].
     *
     * In the case that none of the cases above apply and the phone account is found not to support
     * transactional ops (assumes that caller has [android.Manifest.permission.READ_PHONE_NUMBERS]
     * permission), then the extension type is [NONE].
     *
     * If the caller does not have the required permission to retrieve the phone account, then the
     * extension type will be [UNKNOWN], until it can be resolved.
     *
     * @return the extension type [CapabilityExchangeType] resolved for the call.
     */
    @VisibleForTesting
    internal suspend fun resolveCallExtensionsType(): Int {
        var details = call.details
        var type = NONE
        if (Utils.hasPlatformV2Apis()) {
            // Android CallsManager V+ check
            if (details.hasProperty(CallsManager.PROPERTY_IS_TRANSACTIONAL)) {
                return CAPABILITY_EXCHANGE
            }
            // Android CallsManager U check
            // Verify read phone numbers permission to see if phone account supports transactional
            // ops.
            if (
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_PHONE_NUMBERS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val telecomManager =
                    applicationContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val phoneAccount = telecomManager.getPhoneAccount(details.accountHandle)
                if (
                    phoneAccount?.hasCapabilities(
                        PhoneAccount.CAPABILITY_SUPPORTS_TRANSACTIONAL_OPERATIONS
                    ) == true
                ) {
                    return CAPABILITY_EXCHANGE
                }
            } else {
                Log.i(TAG, "Unable to resolve call extension type due to lack of permission.")
                type = UNKNOWN
            }
        }
        // The extras may come in after the call is first signalled to InCallService - wait for the
        // details to be populated with extras.
        if (details.extras == null || details.extras.isEmpty()) {
            details =
                withTimeoutOrNull(RESOLVE_EXTENSIONS_TYPE_TIMEOUT_MS) {
                    detailsFlow().first { details ->
                        details.extras != null && !details.extras.isEmpty()
                    }
                    // return initial details if no updates come in before the timeout
                } ?: call.details
        }
        val callExtras = details.extras ?: Bundle()
        // Extras based impl check
        if (callExtras.containsKey(EXTRA_VOIP_API_VERSION)) {
            return EXTRAS
        }
        // CS based impl check
        if (callExtras.containsKey(CallsManager.EXTRA_VOIP_BACKWARDS_COMPATIBILITY_SUPPORTED)) {
            return CAPABILITY_EXCHANGE
        }
        Log.i(TAG, "Unable to resolve call extension type. Returning $type.")
        return type
    }

    /** Perform the operation to connect the extensions to the call. */
    internal suspend fun connectExtensionSession() {
        val type = resolveCallExtensionsType()
        Log.d(TAG, "connectExtensionsSession: type=$type")
        // When we support EXTRAs, extensions should wrap this detail into a generic interface
        val extensions = performExchangeWithRemote()
        try {
            when (type) {
                CAPABILITY_EXCHANGE -> initializeExtensions(extensions)
                else -> {
                    Log.w(
                        TAG,
                        "connectExtensions: unexpected type: $type. Proceeding with " +
                            "no extension support"
                    )
                    initializeExtensions(null)
                }
            }
            invokeDelegate()
            waitForDestroy()
        } finally {
            Log.i(TAG, "setupExtensionSession: scope closing, calling onRemoveExtensions")
            callScope.cancel()
            extensions?.extensionInitializationBinder?.onRemoveExtensions()
        }
    }

    /**
     * Register the extensions interface with the remote application and get the result.
     *
     * If negotiation takes longer than [CAPABILITY_EXCHANGE_TIMEOUT_MS], we will assume the remote
     * does not support extensions at all.
     */
    private suspend fun performExchangeWithRemote(): CapabilityExchangeResult? {
        Log.d(TAG, "requestExtensions: requesting extensions from remote")
        val extensions =
            withTimeoutOrNull(CAPABILITY_EXCHANGE_TIMEOUT_MS) { registerWithRemoteService() }
        if (extensions == null) {
            Log.w(TAG, "startCapabilityExchange: never received response")
        }
        return extensions
    }

    /**
     * Initialize all extensions that were registered with [registerExtension] and provide the
     * negotiated capability or null if the remote doesn't support this extension.
     */
    private suspend fun initializeExtensions(extensions: CapabilityExchangeResult?) {
        Log.i(TAG, "initializeExtensions: Initializing extensions...")
        val delegates = callExtensionCreators.map { it() }
        if (extensions == null) {
            for (initializer in delegates) {
                initializer.onExchangeComplete(null, null)
            }
            return
        }

        for (initializer in delegates) {
            Log.d(TAG, "initializeExtensions: capability=${initializer.extensionCapability}")
            val remoteCap =
                extensions.voipCapabilities.firstOrNull {
                    it.featureId == initializer.extensionCapability.featureId
                }
            if (remoteCap == null) {
                Log.d(TAG, "initializeExtensions: no VOIP capability, skipping...")
                initializer.onExchangeComplete.invoke(null, null)
                continue
            }
            val negotiatedCap =
                calculateNegotiatedCapability(initializer.extensionCapability, remoteCap)
            Log.d(TAG, "initializeExtensions: negotiated cap=$negotiatedCap")
            initializer.onExchangeComplete.invoke(
                negotiatedCap,
                extensions.extensionInitializationBinder
            )
        }
    }

    /**
     * Initiate capability exchange via a call event and wait for the response from the calling
     * application using the Binder passed to the remote service.
     *
     * @return the remote capabilities and Binder interface used to communicate with the remote
     */
    private suspend fun registerWithRemoteService(): CapabilityExchangeResult? =
        suspendCancellableCoroutine { continuation ->
            val binder =
                object : ICapabilityExchange.Stub() {
                    override fun beginExchange(
                        capabilities: MutableList<Capability>?,
                        l: ICapabilityExchangeListener?
                    ) {
                        Log.v(
                            TAG,
                            "registerWithRemoteService: received remote result," +
                                " caps=$capabilities, listener is null=${l == null}"
                        )
                        continuation.resume(
                            l?.let {
                                CapabilityExchangeResult(
                                    capabilities?.toSet() ?: Collections.emptySet(),
                                    CapabilityExchangeListenerRemote(l)
                                )
                            }
                        )
                    }
                }
            Log.v(TAG, "registerWithRemoteService: sending event")
            val extras = setExtras(binder)
            call.sendCallEvent(Extensions.EVENT_JETPACK_CAPABILITY_EXCHANGE, extras)
        }

    /**
     * @return the negotiated capability by finding the highest version and actions supported by
     *   both the local and remote interfaces.
     */
    @ExperimentalAppActions
    private fun calculateNegotiatedCapability(
        localCapability: Capability,
        remoteCapability: Capability
    ): Capability {
        return Capability().apply {
            featureId = localCapability.featureId
            featureVersion = min(localCapability.featureVersion, remoteCapability.featureVersion)
            supportedActions =
                localCapability.supportedActions
                    .intersect(remoteCapability.supportedActions.toSet())
                    .toIntArray()
        }
    }

    /**
     * @return a [Bundle] that contains the binder and version used by the remote to respond to a
     *   capability exchange request.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setExtras(binder: IBinder): Bundle {
        return Bundle().apply {
            putBinder(Extensions.EXTRA_CAPABILITY_EXCHANGE_BINDER, binder)
            putInt(Extensions.EXTRA_CAPABILITY_EXCHANGE_VERSION, CAPABILITY_EXCHANGE_VERSION)
        }
    }

    /** Create a flow that reports changes to [Call.Details] provided by the [Call.Callback]. */
    private fun detailsFlow(): Flow<Call.Details> = callbackFlow {
        val callback =
            object : Callback() {
                override fun onDetailsChanged(call: Call?, details: Call.Details?) {
                    details?.also { trySendBlocking(it) }
                }
            }
        // send the current state first since registering for the callback doesn't deliver the
        // current value.
        trySendBlocking(call.details)
        call.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { call.unregisterCallback(callback) }
    }

    /** Wait for the call to be destroyed. */
    private suspend fun waitForDestroy() = suspendCancellableCoroutine { continuation ->
        val callback =
            object : Callback() {
                override fun onCallDestroyed(targetCall: Call?) {
                    if (targetCall == null || call != targetCall || continuation.isCompleted) return
                    continuation.resume(Unit)
                }
            }
        if (Api26Impl.getCallState(call) != Call.STATE_DISCONNECTED) {
            call.registerCallback(callback, Handler(Looper.getMainLooper()))
            continuation.invokeOnCancellation { call.unregisterCallback(callback) }
        } else {
            continuation.resume(Unit)
        }
    }
}

/** Ensure compatibility for [Call] APIs back to API level 26 */
@RequiresApi(Build.VERSION_CODES.O)
private object Api26Impl {
    @Suppress("DEPRECATION")
    @JvmStatic
    fun getCallState(call: Call): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.getCallState(call)
        } else {
            call.state
        }
    }
}

/** Ensure compatibility for [Call] APIs for API level 31+ */
@RequiresApi(Build.VERSION_CODES.S)
private object Api31Impl {
    @JvmStatic
    fun getCallState(call: Call): Int {
        return call.details.state
    }
}
