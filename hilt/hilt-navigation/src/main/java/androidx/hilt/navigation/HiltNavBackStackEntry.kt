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

@file:JvmName("HiltViewModelFactory")

package androidx.hilt.navigation

import android.content.Context
import androidx.hilt.lifecycle.viewmodel.HiltViewModelFactory as createHiltViewModelFactory
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry

/**
 * Creates a [ViewModelProvider.Factory] to get
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated `ViewModel` from a [NavBackStackEntry].
 *
 * @param context the activity context.
 * @param navBackStackEntry the navigation back stack entry.
 * @return the factory.
 * @throws IllegalStateException if the context given is not an activity.
 */
@JvmName("create")
public fun HiltViewModelFactory(
    context: Context,
    navBackStackEntry: NavBackStackEntry,
): ViewModelProvider.Factory =
    createHiltViewModelFactory(context, navBackStackEntry.defaultViewModelProviderFactory)

/**
 * Creates a [ViewModelProvider.Factory] to get
 * [HiltViewModel](https://dagger.dev/api/latest/dagger/hilt/android/lifecycle/HiltViewModel)
 * -annotated `ViewModel` from a [ViewModelProvider.Factory].
 *
 * @param context the activity context.
 * @param delegateFactory the delegated factory.
 * @return the factory.
 * @throws IllegalStateException if the context given is not an activity.
 */
@Deprecated(
    "Moved to package: androidx.hilt.lifecycle.viewmodel",
    replaceWith =
        ReplaceWith(
            expression = "HiltViewModelFactory(context, delegateFactory)",
            imports = ["androidx.hilt.lifecycle.viewmodel.HiltViewModelFactory"],
        ),
)
@JvmName("create")
public fun HiltViewModelFactory(
    context: Context,
    delegateFactory: ViewModelProvider.Factory,
): ViewModelProvider.Factory = createHiltViewModelFactory(context, delegateFactory)
