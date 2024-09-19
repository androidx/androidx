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

package androidx.core.telecom.test.ui

import android.content.Intent
import androidx.core.telecom.test.ui.calling.AudioRoutePickerDialog
import androidx.core.telecom.test.ui.calling.CallsScreen
import androidx.core.telecom.test.ui.calling.OngoingCallsViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog

/* Routes defined by the navgraph */
object NavRoute {
    const val ROLE_REQUESTS = "RoleRequests"
    const val CALLS = "Calls"
    const val NOT_SUPPORTED = "NotSupported"
    const val AUDIO_ROUTE_PICKER = "AudioRoutePicker"
    const val SETTINGS = "Settings"
}

/** The screen used for devices that do not support this application */
fun NavGraphBuilder.notSupportedDestination() {
    composable(NavRoute.NOT_SUPPORTED) { UnsupportedDeviceScreen() }
}

/** The screen used for devices that have not set this application to the default dialer yet. */
fun NavGraphBuilder.roleRequestsDestination(
    roleIntent: Intent,
    onGrantedStateChanged: (Boolean) -> Unit
) {
    composable(NavRoute.ROLE_REQUESTS) { RoleRequestScreen(roleIntent, onGrantedStateChanged) }
}

/** The main calling screen, which manages new and ongoing calls. */
fun NavGraphBuilder.callsDestination(
    ongoingCallsViewModel: OngoingCallsViewModel,
    onShowAudioRouting: () -> Unit,
    onMoveToSettings: () -> Unit
) {
    composable(NavRoute.CALLS) {
        CallsScreen(
            ongoingCallsViewModel = ongoingCallsViewModel,
            onShowAudioRouting = onShowAudioRouting,
            onMoveToSettings = onMoveToSettings
        )
    }
}

/**
 * The audio routing dialog, which sits on top of the active screen and allows users to change the
 * active audio route of the active call.
 */
fun NavGraphBuilder.audioRouteDialog(
    ongoingCallsViewModel: OngoingCallsViewModel,
    onDismissDialog: () -> Unit,
    onChangeAudioRoute: suspend (String) -> Unit
) {
    dialog(NavRoute.AUDIO_ROUTE_PICKER) {
        AudioRoutePickerDialog(ongoingCallsViewModel, onDismissDialog, onChangeAudioRoute)
    }
}

/** Defines the screen used to control app settings. */
fun NavGraphBuilder.settingsDestination() {
    composable(NavRoute.SETTINGS) { SettingsScreen() }
}

/** Launch the audio routing dialog for the user. */
fun NavController.launchAudioRouteDialog() {
    navigate(route = NavRoute.AUDIO_ROUTE_PICKER)
}

/** Launch the settings screen. */
fun NavController.moveToSettingsDestination() {
    navigate(route = NavRoute.SETTINGS)
}
