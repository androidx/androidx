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

import android.content.Intent
import android.util.Log
import androidx.core.telecom.reference.Constants.ACTION_ANSWER_AND_SHOW_UI
import androidx.core.telecom.reference.Constants.EXTRA_CALL_ID
import androidx.core.telecom.reference.Constants.EXTRA_REMOTE_USER_NAME
import androidx.core.telecom.reference.Constants.EXTRA_SIMULATED_NUMBER
import androidx.core.telecom.reference.view.NavRoutes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DialerActivityAction {
    data class StartServiceToAnswer(val callId: String, val number: String, val name: String) :
        DialerActivityAction()

    object ConnectServiceIfNeeded : DialerActivityAction()
}

/**
 * [DialerActivityViewModel] is a [ViewModel] responsible for handling and processing new [Intent]s
 * directed towards the Dialer activity.
 *
 * It exposes a [newIntentFlow] as a [kotlinx.coroutines.flow.Flow] of [Intent]s, allowing other
 * components to observe and react to new intents received by the activity.
 *
 * The [processNewIntent] function is used to receive incoming [Intent]s and emit them into the
 * [newIntentFlow]. This decouples the source of the intent (e.g., a broadcast receiver, another
 * activity) from the components that need to handle it within the Dialer activity.
 */
class DialerActivityViewModel : ViewModel() {
    companion object {
        private const val TAG = "DialerActivityViewModel"
    }

    // Existing intent flow
    private val _newIntentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val newIntentFlow = _newIntentFlow.asSharedFlow()

    // Existing dialog state
    private val _showSettingsGuidanceDialog = MutableStateFlow(false)
    val showSettingsGuidanceDialog = _showSettingsGuidanceDialog.asStateFlow()

    // --- ADD Event Flow for Activity Actions ---
    private val _activityAction = MutableSharedFlow<DialerActivityAction>()
    val activityAction: SharedFlow<DialerActivityAction> = _activityAction.asSharedFlow()

    // Called from Activity's onCreate/onNewIntent
    fun processIntent(intent: Intent) {
        viewModelScope.launch { // Use viewModelScope
            val action = intent.action
            val callId = intent.getStringExtra(EXTRA_CALL_ID)
            val isAnswerAction = ACTION_ANSWER_AND_SHOW_UI == action
            val isViewCallAction =
                Intent.ACTION_VIEW == action &&
                    intent.data?.toString()?.contains(NavRoutes.IN_CALL) == true

            Log.d(
                TAG,
                "Processing intent: action=$action, callId=$callId," +
                    " isAnswerAction=$isAnswerAction, isViewCallAction=$isViewCallAction",
            )

            // --- Emit events based on intent ---
            if (isAnswerAction || isViewCallAction || callId != null) {
                Log.d(TAG, "Intent relates to a call, emitting ConnectServiceIfNeeded.")
                _activityAction.emit(DialerActivityAction.ConnectServiceIfNeeded)
            }

            if (isAnswerAction && callId != null) {
                val number = intent.getStringExtra(EXTRA_SIMULATED_NUMBER)
                val name = intent.getStringExtra(EXTRA_REMOTE_USER_NAME)
                if (number != null && name != null) {
                    Log.i(TAG, "[$callId] Emitting StartServiceToAnswer.")
                    _activityAction.emit(
                        DialerActivityAction.StartServiceToAnswer(callId, number, name)
                    )
                } else {
                    Log.w(TAG, "[$callId] Received answer without name/number arg!")
                }
            } else if (isViewCallAction || callId != null) {
                Log.d(TAG, "[$callId] Handling regular view intent (emit newIntentFlow for nav).")
                _newIntentFlow.tryEmit(intent) // Let DialerApp handle navigation via deep link
            } else {
                Log.w(TAG, "Received intent unrelated to a specific call action.")
                _newIntentFlow.tryEmit(intent) // Emit for potential other deep links
            }
        }
    }

    // Existing permission dialog logic
    fun onPermissionsPermanentlyDenied() {
        _showSettingsGuidanceDialog.value = true
    }

    fun onSettingsGuidanceDialogDismissed() {
        _showSettingsGuidanceDialog.value = false
    }
}
