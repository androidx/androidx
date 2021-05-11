/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UNUSED_VARIABLE") // These are sample files.

package androidx.hilt.navigation.compose.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Sampled
@Composable
fun NavComposable() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "ExampleRoute") {
        composable("ExampleRoute") {
            val viewModel = hiltViewModel<ExampleViewModel>()
        }
    }
}

@Sampled
@Composable
fun NestedNavComposable() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "Parent") {
        navigation(startDestination = "InnerRouteA", route = "Parent") {
            composable("InnerRouteA") {
                val viewModel = hiltViewModel<ParentViewModel>(
                    navController.getBackStackEntry("Parent")
                )
            }
            composable("InnerRouteB") {
                val viewModel = hiltViewModel<ParentViewModel>(
                    navController.getBackStackEntry("Parent")
                )
            }
        }
    }
}

@HiltViewModel
class ExampleViewModel @Inject constructor() : ViewModel()

@HiltViewModel
class ParentViewModel @Inject constructor() : ViewModel()
