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

package androidx.core.telecom.extensions

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.telecom.Call
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.telecom.extensions.Extensions.CALL_ICON
import androidx.core.telecom.extensions.Extensions.LOCAL_CALL_SILENCE
import androidx.core.telecom.extensions.Extensions.MEETING_SUMMARY
import androidx.core.telecom.internal.CapabilityExchangeListenerRemote
import androidx.core.telecom.internal.CapabilityExchangeRepository
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Processes extras-based extensions for a Telecom [Call]. This class handles the extraction,
 * processing, and propagation of call-related data (like speaker, participant count, call icon, and
 * local silence state) received via `Bundle` extras. It interacts with the
 * `CapabilityExchangeRepository` to set up extensions and manage communication with a remote
 * endpoint. This class is designed to work within a `CoroutineScope` to handle asynchronous
 * operations and flow updates.
 *
 * @param callScope The [CoroutineScope] in which extension-related operations are performed. This
 *   scope is used for launching coroutines that observe and update state flows.
 * @param call The [Call] instance for which extensions are being processed.
 */
@OptIn(ExperimentalAppActions::class)
@RequiresApi(Build.VERSION_CODES.O)
internal class ExtrasCallExtensionProcessor(
    private val callScope: CoroutineScope,
    private val call: Call,
) {
    companion object {
        private const val TAG = "ECEP"
        /** Set on Connections that are using ConnectionService+AUTO specific extension layer. */
        internal const val EXTRA_VOIP_API_VERSION = "android.telecom.extra.VOIP_API_VERSION"

        /**
         * String value (name of current speaker). Null will not be displayed, empty strings will be
         * indicative of no current speaker but that the app still wishes to display speaker info.
         */
        internal const val EXTRA_CURRENT_SPEAKER = "android.telecom.extra.CURRENT_SPEAKER"

        /**
         * Integer value. Null values will not be displayed, values >= 0 will be shown by supported
         * UI’s.
         */
        internal const val EXTRA_PARTICIPANT_COUNT = "android.telecom.extra.PARTICIPANT_COUNT"

        /**
         * URI value for an image to be displayed to represent the current call (overrides contact
         * image in Auto). Supported URI types will be resource URI’s and content provider URI’s.
         */
        internal const val EXTRA_CALL_IMAGE_URI = "android.telecom.extra.CALL_IMAGE_URI"

        /**
         * Extra to be included to indicate that the app uses local call microphone silence rather
         * than the default global mute.
         */
        internal const val EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY =
            "android.telecom.extra.USE_LOCAL_CALL_SILENCE_CAPABILITY"

        /**
         * Extra to be included to indicate that the call is currently able to have its call silence
         * state modified.
         */
        internal const val EXTRA_CALL_SILENCE_AVAILABILITY =
            "android.telecom.extra.CALL_SILENCE_AVAILABILITY"

        /** Extra associated with the {@code boolean} call microphone silence state extra. */
        internal const val EXTRA_LOCAL_CALL_SILENCE_STATE =
            "android.telecom.extra.LOCAL_CALL_SILENCE_STATE"

        /**
         * Event received when an ICS is requesting a change to the call silence state. Will be
         * packaged with the {@link #EXTRA_LOCAL_CALL_SILENCE_STATE} and a {@code boolean} value.
         */
        internal const val EVENT_LOCAL_CALL_SILENCE_STATE_CHANGED =
            "android.telecom.event.LOCAL_CALL_SILENCE_STATE_CHANGED"
    }

    private val mProcessedKeys = mutableSetOf<String>()
    private val mSpeakerNameFlow = MutableStateFlow("")
    private val mParticipantCountFlow = MutableStateFlow(0)
    private val mUriFlow = MutableStateFlow<Uri>(Uri.EMPTY)

    private var mHasLocalCallSilenceCapability = true // one-time set
    private var mCurrentlyUsingLocalCallSilence = true // dynamic
    private val mLocalCallSilenceFlow = MutableStateFlow(false)

    /**
     * Processes call extensions based on a [Flow] of [Call.Details]. This function sets up the
     * necessary extensions using a [CapabilityExchangeRepository], collects updates from the
     * provided [detailsFlow], and returns a [CapabilityExchangeResult]. It uses a
     * [CompletableDeferred] to ensure all asynchronous operations related to extension setup are
     * completed before returning.
     *
     * @param detailsFlow A [Flow] of [Call.Details], providing updates to the call's state and
     *   extras.
     * @return A [CapabilityExchangeResult] containing the negotiated capabilities, or `null` if no
     *   relevant extras were found.
     */
    internal suspend fun handleExtrasExtensionsFromVoipApp(
        detailsFlow: Flow<Call.Details>
    ): CapabilityExchangeResult? {
        Log.i(TAG, "handleExtrasExtensionsFromVoipApp: consuming extras")
        val callbackRepository = CapabilityExchangeRepository(callScope)
        initMeetingExtension(callbackRepository)
        initCallIconExtension(callbackRepository)
        initLocalSilenceExtension(callbackRepository)
        callScope.launch { detailsFlow.collect { details -> processExtras(details.extras) } }
        return getExtensionsUpdateFromNewExtras(detailsFlow.first(), callbackRepository)
    }

    /**
     * Extracts extension updates from [Call.Details] and returns a [CapabilityExchangeResult]. This
     * method retrieves the extras from the provided [details], determines the VoIP API version, and
     * constructs a set of supported capabilities. It then calls [processExtras] to handle the
     * individual extra values.
     *
     * @param details The current [Call.Details] of the call.
     * @param r The [CapabilityExchangeRepository] instance for capability exchange.
     * @return A [CapabilityExchangeResult] representing the updated capabilities and remote
     *   listener, or `null` if no relevant extras are found.
     */
    private suspend fun getExtensionsUpdateFromNewExtras(
        details: Call.Details,
        r: CapabilityExchangeRepository,
    ): CapabilityExchangeResult? {
        val extras = details.extras?.takeIf { it.size() > 0 } ?: return null

        val apiVersion = extras.getInt(EXTRA_VOIP_API_VERSION, 0)
        if (extras.containsKey(EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY)) {
            mHasLocalCallSilenceCapability =
                extras.getBoolean(EXTRA_USE_LOCAL_CALL_SILENCE_CAPABILITY)
        }
        val voipCapabilities =
            setOf(
                getVoipMeetingSummaryCapability(apiVersion),
                getVoipIconCapability(apiVersion),
                getVoipLocalCallSilenceCapability(apiVersion),
            )

        processExtras(extras)

        return CapabilityExchangeResult(
            voipCapabilities,
            CapabilityExchangeListenerRemote(r.listener),
        )
    }

    /**
     * Processes the provided [extras] [Bundle], extracting values for known keys and emitting them
     * to the corresponding internal state flows. This function handles the logic for dealing with
     * extras that may be present in some updates but absent in others.
     *
     * @param extras The [Bundle] containing the call's extras, or `null` if no extras are present.
     */
    private suspend fun processExtras(extras: Bundle?) {
        val currentKeys = extras?.keySet() ?: emptySet()

        processKey(
            key = EXTRA_CALL_IMAGE_URI,
            extras = extras,
            currentKeys = currentKeys,
            getValue = { b -> b.getParcelableCompat(EXTRA_CALL_IMAGE_URI, Uri::class.java) },
            defaultValue = Uri.EMPTY,
            flow = mUriFlow,
        )

        processKey(
            key = EXTRA_PARTICIPANT_COUNT,
            extras = extras,
            currentKeys = currentKeys,
            getValue = { b -> b.getInt(EXTRA_PARTICIPANT_COUNT) },
            defaultValue = 0,
            flow = mParticipantCountFlow,
        )
        processKey(
            key = EXTRA_CURRENT_SPEAKER,
            extras = extras,
            currentKeys = currentKeys,
            getValue = { b -> b.getString(EXTRA_CURRENT_SPEAKER) },
            defaultValue = "",
            flow = mSpeakerNameFlow,
        )

        if (extras?.containsKey(EXTRA_CALL_SILENCE_AVAILABILITY) == true) {
            mCurrentlyUsingLocalCallSilence = extras.getBoolean(EXTRA_CALL_SILENCE_AVAILABILITY)
        }
        if (mHasLocalCallSilenceCapability && mCurrentlyUsingLocalCallSilence) {
            processKey(
                key = EXTRA_LOCAL_CALL_SILENCE_STATE,
                extras = extras,
                currentKeys = currentKeys,
                getValue = { b -> b.getBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE) },
                defaultValue = false,
                flow = mLocalCallSilenceFlow,
            )
        } else {
            Log.w(TAG, "processExtras: attempted to toggle LCS but global mute is enabled")
        }

        mProcessedKeys.addAll(currentKeys)
    }

    /**
     * Processes a single extra key. If the [key] is present in [currentKeys], the [getValue] lambda
     * is used to extract the value from the extras [Bundle], and the value is emitted to the
     * provided [flow]. If the [key] was previously processed but is now missing, the [defaultValue]
     * is emitted to the [flow].
     *
     * @param key The string key of the extra to process.
     * @param currentKeys A set of strings representing the keys currently present in the extras.
     * @param getValue A lambda that takes a [Bundle] and returns the value associated with the
     *   [key], or `null` if the value is not found or is of the wrong type.
     * @param defaultValue The default value to emit if the key is not present or was previously
     *   present but is now missing.
     * @param flow The [MutableStateFlow] to which the extracted value (or default value) will be
     *   emitted.
     */
    private suspend fun <T> processKey(
        key: String,
        extras: Bundle?,
        currentKeys: Set<String>,
        getValue: (Bundle) -> T?,
        defaultValue: T,
        flow: MutableStateFlow<T>,
    ) {
        if (currentKeys.contains(key)) {
            mProcessedKeys.add(key)
            val value = extras?.let { getValue(it) } ?: defaultValue // Safely get, default if null
            flow.emit(value)
        } else if (mProcessedKeys.contains(key)) { // Key was present, now missing
            flow.emit(defaultValue)
        }
    }

    /**
     * Initializes the meeting summary extension by setting up listeners for speaker name and
     * participant count updates. These updates are propagated to the remote endpoint via the
     * provided [CapabilityExchangeRepository]. `finishSync()` is called on the binder to signal the
     * completion of the initialization.
     *
     * @param r The [CapabilityExchangeRepository] instance used to register the extension and
     *   communicate with the remote endpoint.
     */
    private fun initMeetingExtension(r: CapabilityExchangeRepository) {
        r.onMeetingSummaryExtension = { coroutineScope, binder ->
            mSpeakerNameFlow.onEach { binder.updateCurrentSpeaker(it) }.launchIn(coroutineScope)
            mParticipantCountFlow
                .onEach { binder.updateParticipantCount(it) }
                .launchIn(coroutineScope)
            binder.finishSync()
        }
    }

    /**
     * Initializes the local call silence extension. This sets up a listener for changes to the
     * local call silence state and provides an implementation of [ILocalSilenceActions] to handle
     * remote requests to modify the silence state.
     *
     * @param r The [CapabilityExchangeRepository] used to register the extension.
     */
    private fun initLocalSilenceExtension(r: CapabilityExchangeRepository) {
        r.onCreateLocalCallSilenceExtension = { coroutineScope, _, binder ->
            mLocalCallSilenceFlow
                .onEach { binder.updateIsLocallySilenced(it) }
                .launchIn(coroutineScope)
            val remoteExtensionBinder =
                object : ILocalSilenceActions.Stub() {
                    override fun setIsLocallySilenced(
                        isLocallySilenced: Boolean,
                        cb: IActionsResultCallback?,
                    ) {
                        call.sendCallEvent(
                            EVENT_LOCAL_CALL_SILENCE_STATE_CHANGED,
                            Bundle().apply {
                                putBoolean(EXTRA_LOCAL_CALL_SILENCE_STATE, isLocallySilenced)
                            },
                        )
                        cb?.onSuccess()
                    }
                }
            binder.finishSync(remoteExtensionBinder)
        }
    }

    /**
     * Initializes the call icon extension by setting up a listener for call icon URI updates. These
     * updates are propagated to the remote endpoint via the provided
     * [CapabilityExchangeRepository]. `finishSync()` is called on the binder to signal the
     * completion of the initialization.
     *
     * @param r The [CapabilityExchangeRepository] instance used to register the extension and
     *   communicate with the remote endpoint.
     */
    private fun initCallIconExtension(r: CapabilityExchangeRepository) {
        r.onCreateCallIconExtension = { coroutineScope, _, _, binder ->
            mUriFlow.onEach { binder.updateCallIconUri(it) }.launchIn(coroutineScope)
            binder.finishSync()
        }
    }

    /**
     * Creates a [Capability] object representing the meeting summary extension.
     *
     * @param version The VoIP API version.
     * @return A [Capability] object for the meeting summary extension.
     */
    private fun getVoipMeetingSummaryCapability(version: Int): Capability {
        return Capability().apply {
            featureId = MEETING_SUMMARY
            featureVersion = version
            supportedActions = emptySet<Int>().toIntArray()
        }
    }

    /**
     * Creates a [Capability] object representing the call icon extension.
     *
     * @param version The VoIP API version.
     * @return A [Capability] object for the call icon extension.
     */
    private fun getVoipIconCapability(version: Int): Capability {
        return Capability().apply {
            featureId = CALL_ICON
            featureVersion = version
            supportedActions = emptySet<Int>().toIntArray()
        }
    }

    /**
     * Creates a [Capability] object representing the local call silence extension.
     *
     * @param version The VoIP API version.
     * @return A [Capability] object for the local call silence extension.
     */
    internal fun getVoipLocalCallSilenceCapability(version: Int): Capability {
        return Capability().apply {
            featureId = LOCAL_CALL_SILENCE
            featureVersion = version
            supportedActions = emptySet<Int>().toIntArray()
        }
    }

    private fun <T : Parcelable> Bundle?.getParcelableCompat(key: String, clazz: Class<T>): T? {
        if (this == null) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(key, clazz)
        } else {
            @Suppress("DEPRECATION")
            getParcelable(key) as? T
        }
    }
}
