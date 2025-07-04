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

package androidx.core.telecom.reference.view

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.core.telecom.reference.CallRepository
import androidx.core.telecom.reference.Constants.ACTION_ANSWER_AND_SHOW_UI
import androidx.core.telecom.reference.Constants.DEEP_LINK_BASE_URI
import androidx.core.telecom.reference.VoipApplication
import androidx.core.telecom.reference.viewModel.DialerActivityViewModel
import androidx.core.telecom.reference.viewModel.DialerViewModel
import androidx.core.telecom.reference.viewModel.InCallViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink

/**
 * import androidx.core.telecom.reference.Constants.ACTION_ANSWER_AND_SHOW_UI
 *
 * The root composable function for the Dialer application UI.
 *
 * This function sets up the navigation controller ([androidx.navigation.compose.NavHost]), creates
 * custom ViewModel factories to provide dependencies ([Context], [CallRepository]) to the
 * ViewModels ([DialerViewModel], [InCallViewModel]), and defines the navigation graph using
 * [NavHost]. It orchestrates the display of different screens ([DialerScreen], [InCallScreen],
 * [SettingsScreen]) based on the current navigation route.
 *
 * @param context The application context, passed down to ViewModels that require it.
 */
@Composable
fun DialerApp(lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current, context: Context) {
    // Create and remember a NavController for managing navigation between screens.
    val navController = rememberNavController()
    val appContext = context.applicationContext
    val callRepository = (appContext as VoipApplication).callRepository
    val activityViewModel: DialerActivityViewModel = viewModel()

    // Remember a custom ViewModelProvider.Factory for DialerViewModel.
    // This allows injecting the Context and CallRepository into DialerViewModel.
    val dialerViewModelFactory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DialerViewModel::class.java)) {
                    // Create DialerViewModel with dependencies
                    @Suppress("UNCHECKED_CAST")
                    return DialerViewModel(appContext, callRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    val inCallViewModelFactory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(InCallViewModel::class.java)) {
                    // Create DialerViewModel with dependencies
                    @Suppress("UNCHECKED_CAST")
                    return InCallViewModel(appContext, callRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }

    // Obtain instances of the ViewModels using their respective factories.
    // These ViewModels will be scoped to the NavHost's lifecycle.
    val dialerViewModel: DialerViewModel = viewModel(factory = dialerViewModelFactory)
    val inCallViewModel: InCallViewModel = viewModel(factory = inCallViewModelFactory)

    DisposableEffect(lifecycleOwner, appContext) {
        callRepository.maybeConnectService(appContext)
        // When the effect leaves the Composition, teardown
        onDispose { callRepository.maybeDisconnectService() }
    }

    // Effect to handle intents passed from the Activity
    LaunchedEffect(Unit) {
        activityViewModel.newIntentFlow.collect { intent ->
            Log.d("DialerApp", "Collected new intent from ViewModel, calling" + " handleDeepLink")
            val navigated = navController.handleDeepLink(intent)
            Log.d("DialerApp", "handleDeepLink result: $navigated")
        }
    }

    // Define the navigation graph for the application.
    NavHost(navController = navController, startDestination = NavRoutes.DIALER) {
        // Define the Dialer screen destination.
        composable(NavRoutes.DIALER) {
            DialerScreen(
                dialerViewModel = dialerViewModel,
                // Navigate to Settings when the settings action is triggered.
                onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                // Navigate to the In-Call screen when a call is started.
                onStartCall = { navController.navigate(NavRoutes.IN_CALL) },
            )
        }
        // Define the In-Call screen destination.
        composable(
            route = NavRoutes.IN_CALL,
            // *** Add Deep Link Information ***
            deepLinks =
                listOf(
                    navDeepLink {
                        uriPattern = "${DEEP_LINK_BASE_URI}/${NavRoutes.IN_CALL}"
                        action = ACTION_ANSWER_AND_SHOW_UI
                    },
                    navDeepLink {
                        uriPattern = "${DEEP_LINK_BASE_URI}/${NavRoutes.IN_CALL}"
                        action = Intent.ACTION_VIEW
                    },
                ),
        ) { _ ->
            InCallScreen(inCallViewModel)
        }
        composable(NavRoutes.SETTINGS) { SettingsScreen() }
    }
}
