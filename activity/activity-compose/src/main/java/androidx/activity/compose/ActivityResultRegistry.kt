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
import androidx.compose.runtime.State
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
     * Returns current composition local value for the owner or `null` if one has not
     * been provided nor is one available by looking at the [LocalContext].
     */
    public val current: ActivityResultRegistryOwner?
        @Composable
        get() = LocalComposition.current
            ?: findOwner<ActivityResultRegistryOwner>(LocalContext.current)

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
 * You should *not* call [ActivityResultLauncher.unregister] on the returned
 * [ActivityResultLauncher]. Attempting to do so will result in an [IllegalStateException].
 *
 * @sample androidx.activity.compose.samples.RememberLauncherForActivityResult
 *
 * @param contract the contract, specifying conversions to/from [Intent]s
 * @param onResult the callback to be called on the main thread when activity result
 *                 is available
 *
 * @return the launcher that can be used to start the activity.
 */
@Composable
public fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ManagedActivityResultLauncher<I, O> {
    // Keep track of the current contract and onResult listener
    val currentContract = rememberUpdatedState(contract)
    val currentOnResult = rememberUpdatedState(onResult)

    // It doesn't really matter what the key is, just that it is unique
    // and consistent across configuration changes
    val key = rememberSaveable { UUID.randomUUID().toString() }

    val activityResultRegistry = checkNotNull(LocalActivityResultRegistryOwner.current) {
        "No ActivityResultRegistryOwner was provided via LocalActivityResultRegistryOwner"
    }.activityResultRegistry
    val realLauncher = remember { ActivityResultLauncherHolder<I>() }
    val returnedLauncher = remember {
        ManagedActivityResultLauncher(realLauncher, currentContract)
    }

    // DisposableEffect ensures that we only register once
    // and that we unregister when the composable is disposed
    DisposableEffect(activityResultRegistry, key, contract) {
        realLauncher.launcher = activityResultRegistry.register(key, contract) {
            currentOnResult.value(it)
        }
        onDispose {
            realLauncher.unregister()
        }
    }
    return returnedLauncher
}

/**
 * A launcher for a previously-{@link ActivityResultCaller#registerForActivityResult prepared call}
 * to start the process of executing an {@link ActivityResultContract}.
 *
 * This launcher does not support the [unregister] function. Attempting to use [unregister] will
 * result in an [IllegalStateException].
 *
 * @param I type of the input required to launch
 */
public class ManagedActivityResultLauncher<I, O> internal constructor(
    private val launcher: ActivityResultLauncherHolder<I>,
    private val contract: State<ActivityResultContract<I, O>>
) : ActivityResultLauncher<I>() {
    /**
     * This function should never be called and doing so will result in an
     * [UnsupportedOperationException].
     *
     * @throws UnsupportedOperationException if this function is called.
     */
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Registration is automatically handled by rememberLauncherForActivityResult")
    override fun unregister() {
        throw UnsupportedOperationException(
            "Registration is automatically handled by rememberLauncherForActivityResult"
        )
    }

    override fun launch(input: I, options: ActivityOptionsCompat?) {
        launcher.launch(input, options)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContract(): ActivityResultContract<I, *> = contract.value
}

internal class ActivityResultLauncherHolder<I> {
    var launcher: ActivityResultLauncher<I>? = null

    fun launch(input: I?, options: ActivityOptionsCompat?) {
        launcher?.launch(input, options) ?: error("Launcher has not been initialized")
    }

    fun unregister() {
        launcher?.unregister() ?: error("Launcher has not been initialized")
    }
}
