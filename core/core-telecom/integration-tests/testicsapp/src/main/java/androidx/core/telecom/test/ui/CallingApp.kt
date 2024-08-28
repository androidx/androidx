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

import android.app.role.RoleManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.telecom.test.ui.calling.OngoingCallsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/** Compose UI for the application, which handles navigation between screens */
@Composable
fun CallingApp(isSupported: Boolean) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val roleManager = context.getSystemService(RoleManager::class.java)
    var isGranted by remember { mutableStateOf(roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) }
    val roleIntent by remember {
        mutableStateOf(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
    }
    val ongoingCallsViewModel: OngoingCallsViewModel = viewModel()

    val startRoute =
        when (isSupported) {
            true -> {
                when (isGranted) {
                    true -> NavRoute.CALLS
                    false -> NavRoute.ROLE_REQUESTS
                }
            }
            false -> NavRoute.NOT_SUPPORTED
        }
    // Following encapsulation guidelines from
    // https://developer.android.com/guide/navigation/design/encapsulate
    NavHost(navController, startDestination = startRoute) {
        notSupportedDestination()
        roleRequestsDestination(roleIntent) { isGranted = it }
        callsDestination(
            ongoingCallsViewModel,
            onShowAudioRouting = { navController.launchAudioRouteDialog() },
            onMoveToSettings = { navController.moveToSettingsDestination() }
        )
        audioRouteDialog(
            ongoingCallsViewModel,
            onDismissDialog = { navController.popBackStack() },
            onChangeAudioRoute = { ongoingCallsViewModel.onChangeAudioRoute(it) }
        )
        settingsDestination()
    }
}
