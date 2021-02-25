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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavBackStackEntry
import dagger.hilt.android.internal.lifecycle.HiltViewModelFactory

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
    navBackStackEntry: NavBackStackEntry
): ViewModelProvider.Factory {
    val activity = context.let {
        var ctx = it
        while (ctx is ContextWrapper) {
            if (ctx is Activity) {
                return@let ctx
            }
            ctx = ctx.baseContext
        }
        throw IllegalStateException(
            "Expected an activity context for creating a HiltViewModelFactory for a " +
                "NavBackStackEntry but instead found: $ctx"
        )
    }
    return HiltViewModelFactory.createInternal(
        activity,
        navBackStackEntry,
        navBackStackEntry.arguments,
        navBackStackEntry.defaultViewModelProviderFactory,
    )
}