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

package androidx.core.telecom.reference.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.telecom.CallAttributesCompat
import androidx.core.telecom.CallEndpointCompat
import androidx.core.telecom.CallsManager
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.model.DialerUiState
import androidx.core.telecom.reference.view.loadPhoneNumberPrefix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Dialer screen.
 *
 * This ViewModel manages the state of the dialer UI, including the display name, phone number, call
 * type (audio/video), and call capabilities (e.g., hold). It interacts with the [CallRepository] to
 * initiate outgoing calls based on the current UI state.
 *
 * @param context The application context, used for accessing resources like phone number prefixes.
 * @param callRepository The repository responsible for handling call-related operations.
 */
class DialerViewModel(
    private val context: Context,
    private val callRepository: CallRepository = CallRepository(),
) : ViewModel() {
    // Internal mutable state flow to hold the Dialer UI state.
    private val _uiState = MutableStateFlow(DialerUiState())
    // Publicly exposed immutable state flow for observing UI state changes.
    val uiState: StateFlow<DialerUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "DialerViewModel"
    }

    private var endpointFetchJob: Job? = null

    init {
        // Start fetching endpoints when the ViewModel is created
        fetchAvailableEndpoints()
    }

    /**
     * Fetches the available pre-call audio endpoints and updates the UI state. Cancels any previous
     * fetch job before starting a new one.
     */
    fun fetchAvailableEndpoints() {
        endpointFetchJob?.cancel() // Cancel previous job if running
        Log.d(TAG, "Starting to fetch available endpoints.")
        _uiState.update {
            it.copy(
                isFetchingEndpoints = true,
                availableEndpoints = emptyList(),
                selectedEndpoint = null,
            )
        } // Reset state

        val callsManager = CallsManager(context)
        endpointFetchJob =
            viewModelScope.launch {
                try {
                    callsManager
                        .getAvailableStartingCallEndpoints()
                        // Catch potential errors during flow emission
                        .catch { e ->
                            Log.e(TAG, "Error collecting endpoints", e)
                            _uiState.update {
                                it.copy(
                                    isFetchingEndpoints = false,
                                    availableEndpoints = emptyList(),
                                )
                            }
                        }
                        // Collect the emitted lists of endpoints
                        .collect { endpoints ->
                            Log.i(
                                TAG,
                                "Received endpoints: ${endpoints.joinToString { it.name.toString() }}",
                            )
                            _uiState.update {
                                it.copy(
                                    availableEndpoints = endpoints,
                                    isFetchingEndpoints = false,
                                    // Optionally auto-select the first one or the active one if
                                    // needed
                                    // selectedEndpoint = endpoints.firstOrNull()
                                )
                            }
                        }
                } catch (e: Exception) {
                    // Catch potential errors when *getting* the flow itself
                    Log.e(TAG, "Error getting endpoints flow", e)
                    _uiState.update {
                        it.copy(isFetchingEndpoints = false, availableEndpoints = emptyList())
                    }
                } finally {
                    // Ensure loading state is turned off if flow completes or job is cancelled
                    if (_uiState.value.isFetchingEndpoints) {
                        _uiState.update { it.copy(isFetchingEndpoints = false) }
                    }
                    Log.d(TAG, "Endpoint fetching finished or cancelled.")
                }
            }
    }

    /**
     * Updates the selected endpoint in the UI state.
     *
     * @param endpoint The endpoint selected by the user, or null to clear selection.
     */
    fun selectEndpoint(endpoint: CallEndpointCompat?) {
        Log.i(TAG, "Endpoint selected: ${endpoint?.name}")
        _uiState.update { it.copy(selectedEndpoint = endpoint) }
    }

    /**
     * Updates the display name in the UI state.
     *
     * @param name The new display name to set.
     */
    fun updateDisplayName(name: String) {
        _uiState.update { it.copy(displayName = name) }
    }

    /**
     * Updates the phone number in the UI state.
     *
     * @param number The new phone number to set (should be the raw number without prefix).
     */
    fun updatePhoneNumber(number: String) {
        _uiState.update { it.copy(phoneNumber = number) }
    }

    /**
     * Updates whether the call should be a video call in the UI state.
     *
     * @param isVideo `true` if the call should be a video call, `false` otherwise.
     */
    fun updateIsVideoCall(isVideo: Boolean) {
        _uiState.update { it.copy(isVideoCall = isVideo) }
    }

    /**
     * Updates whether the call should support the hold capability in the UI state.
     *
     * @param canHold `true` if the call should support being put on hold, `false` otherwise.
     */
    fun updateCanHold(canHold: Boolean) {
        _uiState.update { it.copy(canHold = canHold) }
    }

    /**
     * Initiates an outgoing call using the current UI state.
     *
     * Constructs [CallAttributesCompat] based on the current display name, phone number (with
     * prefix), call type, and capabilities, then requests the [callRepository] to add the outgoing
     * call.
     */
    fun initiateOutgoingCall() {
        callRepository.addOutgoingCall(
            CallAttributesCompat(
                _uiState.value.displayName,
                Uri.parse(loadPhoneNumberPrefix(context) + _uiState.value.phoneNumber),
                CallAttributesCompat.DIRECTION_OUTGOING,
                callType = getCallType(),
                callCapabilities = getCallCapabilities(),
                preferredStartingCallEndpoint = _uiState.value.selectedEndpoint,
            )
        )
    }

    /**
     * Determines the appropriate call type based on the current UI state.
     *
     * @return [CallAttributesCompat.CALL_TYPE_VIDEO_CALL] if `isVideoCall` is true, otherwise
     *   [CallAttributesCompat.CALL_TYPE_AUDIO_CALL].
     */
    fun getCallType(): Int {
        return if (_uiState.value.isVideoCall) {
            CallAttributesCompat.CALL_TYPE_VIDEO_CALL
        } else {
            CallAttributesCompat.CALL_TYPE_AUDIO_CALL
        }
    }

    /**
     * Determines the call capabilities based on the current UI state.
     *
     * @return [CallAttributesCompat.SUPPORTS_SET_INACTIVE] if `canHold` is true, otherwise 0 (no
     *   specific capabilities).
     */
    fun getCallCapabilities(): Int {
        return if (_uiState.value.canHold) {
            CallAttributesCompat.SUPPORTS_SET_INACTIVE // Capability for supporting hold
        } else {
            0 // No specific capabilities indicated
        }
    }
}
