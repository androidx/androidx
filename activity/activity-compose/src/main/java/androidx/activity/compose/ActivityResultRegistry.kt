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

package androidx.activity.compose

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityOptionsCompat
import java.util.UUID

/**
 * Provides a [ActivityResultRegistryOwner] that can be used by Composables hosted in a
 * [androidx.activity.ComponentActivity].
 */
public object LocalActivityResultRegistryOwner {
    private val LocalComposition = compositionLocalOf<ActivityResultRegistryOwner?> { null }

    /**
     * Returns current composition local value for the owner.
     */
    public val current: ActivityResultRegistryOwner
        @Composable
        get() = LocalComposition.current
            ?: findOwner<ActivityResultRegistryOwner>(LocalContext.current)
            ?: error("No ActivityResultRegisterOwner has been provided")

    /**
     * Associates a [LocalActivityResultRegistryOwner] key to a value in a call to
     * [CompositionLocalProvider].
     */
    public infix fun provides(registryOwner: ActivityResultRegistryOwner):
        ProvidedValue<ActivityResultRegistryOwner?> {
            return LocalComposition.provides(registryOwner)
        }
}

/**
 * Register a request to [Activity#startActivityForResult][start an activity for result],
 * designated by the given [ActivityResultContract][contract].
 *
 * This creates a record in the [ActivityResultRegistry][registry] associated with this
 * caller, managing request code, as well as conversions to/from [Intent] under the hood.
 *
 * This *must* be called unconditionally, as part of initialization path.
 *
 * @sample androidx.activity.compose.samples.RegisterForActivityResult
 *
 * @param contract the contract, specifying conversions to/from [Intent]s
 * @param onResult the callback to be called on the main thread when activity result
 *                 is available
 *
 * @return the launcher that can be used to start the activity or unregister the callback.
 */
@Composable
public fun <I, O> registerForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ActivityResultLauncher<I> {
    // Keep track of the current onResult listener
    val currentOnResult = rememberUpdatedState(onResult)

    // It doesn't really matter what the key is, just that it is unique
    // and consistent across configuration changes
    val key = rememberSaveable { UUID.randomUUID().toString() }

    val activityResultRegistry = LocalActivityResultRegistryOwner.current.activityResultRegistry
    val realLauncher = remember(contract) { ActivityResultLauncherHolder<I>() }
    val returnedLauncher = remember(contract) {
        object : ActivityResultLauncher<I>() {
            override fun launch(input: I, options: ActivityOptionsCompat?) {
                realLauncher.launch(input, options)
            }

            override fun unregister() {
                realLauncher.unregister()
            }

            @Suppress("UNCHECKED_CAST")
            override fun getContract() = contract as ActivityResultContract<I, *>
        }
    }

    // DisposableEffect ensures that we only register once
    // and that we unregister when the composable is disposed
    DisposableEffect(activityResultRegistry, key, contract) {
        realLauncher.launcher = activityResultRegistry.register(key, contract) {
            currentOnResult.value(it)
        }
        onDispose {
            returnedLauncher.unregister()
        }
    }
    return returnedLauncher
}

private class ActivityResultLauncherHolder<I> {
    var launcher: ActivityResultLauncher<I>? = null

    fun launch(input: I?, options: ActivityOptionsCompat?) {
        launcher?.launch(input, options) ?: error("Launcher has not been initialized")
    }

    fun unregister() {
        launcher?.unregister() ?: error("Launcher has not been initialized")
    }
}
