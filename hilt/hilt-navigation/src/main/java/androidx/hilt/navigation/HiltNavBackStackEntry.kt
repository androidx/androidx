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
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
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
    return HiltViewModelFactory(context, navBackStackEntry.defaultViewModelProviderFactory)
}

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
@JvmName("create")
public fun HiltViewModelFactory(
    context: Context,
    delegateFactory: ViewModelProvider.Factory
): ViewModelProvider.Factory {
    val activity = context.let {
        var ctx = it
        while (ctx is ContextWrapper) {
            // Hilt can only be used with ComponentActivity
            if (ctx is ComponentActivity) {
                return@let ctx
            }
            ctx = ctx.baseContext
        }
        throw IllegalStateException(
            "Expected an activity context for creating a HiltViewModelFactory " +
                "but instead found: $ctx"
        )
    }
    // TODO(kuanyingchou): The `owner` is actually not used. We pass
    //  `activity` here since it's a NonNull parameter. This can be removed with Dagger 2.45.
    return HiltViewModelFactory.createInternal(
        /* activity = */ activity,
        /* owner = */ activity,
        /* defaultArgs = */ null,
        /* delegateFactory = */ delegateFactory
    )
}
