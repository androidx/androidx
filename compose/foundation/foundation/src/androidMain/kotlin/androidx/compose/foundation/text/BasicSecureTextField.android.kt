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

package androidx.compose.foundation.text

import android.content.Context
import android.database.ContentObserver
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.System.TEXT_SHOW_PASSWORD
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

private const val TAG = "BasicSecureTextField"

/**
 * [BasicSecureTextField] or any other secure TextField that depends on it needs to register a
 * [ContentObserver] to be able to observe the changes to platform password visibility settings.
 * Registering this observer on the main thread is usually fine but because it requires an IPC call
 * to be made, a rare lock contention can happen that hangs the main thread for a short time.
 *
 * This [androidx.compose.runtime.CompositionLocal] provides a way to choose an [Executor] to run
 * this registration and its corresponding unregistration in, freeing the main thread.
 *
 * The default value `null` indicates that the main thread will be used.
 */
val LocalTextFieldContentObserverRegistrationExecutor = staticCompositionLocalOf<Executor?> { null }

@Composable
internal actual fun platformAllowsRevealLastTyped(): Boolean {
    val context = LocalContext.current
    val resolver =
        remember(context, contentResolverForSecureTextField) {
            contentResolverForSecureTextField(context)
        }
    var state by remember(resolver) { mutableStateOf(resolver.showPassword) }
    val executor = LocalTextFieldContentObserverRegistrationExecutor.current
    val settingsObserver: ContentObserver =
        remember(resolver) {
            object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    state = resolver.showPassword
                }
            }
        }

    // we are not passing the [executor] as a key here because once the registration is
    // completed it doesn't make sense to re-register the observer on a new background thread.
    val registrationToken = remember(resolver) { RegistrationToken(executor) }

    DisposableEffect(registrationToken) {
        registrationToken.register(resolver, settingsObserver)
        onDispose { registrationToken.dispose() }
    }
    return state
}

private class RegistrationToken(private val executor: Executor?) {
    private var unregister: (() -> Unit)? = null
    private var disposed = false

    fun register(resolver: ContentResolverForSecureTextField, observer: ContentObserver) {
        if (executor != null) {
            executor.tryExecute {
                resolver.registerContentObserver(observer)
                val unregisterLambda = { resolver.unregisterContentObserver(observer) }
                var runImmediately = false
                // We synchronize only to safely read/write the shared `disposed` and `unregister`
                // state. Do not run the foreign `unregisterLambda()` inside the lock to
                // prevent potential deadlocks or long lock holds since it makes an IPC binder call
                // internally.
                synchronized(this) {
                    if (disposed) {
                        runImmediately = true
                    } else {
                        unregister = unregisterLambda
                    }
                }
                if (runImmediately) {
                    unregisterLambda()
                }
            }
        } else {
            resolver.registerContentObserver(observer)
            unregister = { resolver.unregisterContentObserver(observer) }
        }
    }

    fun dispose() {
        if (executor != null) {
            executor.tryExecute {
                var toRun: (() -> Unit)? = null
                // We synchronize only to safely update the shared `disposed` and `unregister`
                // state. To prevent deadlocks and lock contention, we capture the unregister
                // action and execute it outside the lock block.
                synchronized(this) {
                    disposed = true
                    toRun = unregister
                    unregister = null
                }
                toRun?.invoke()
            }
        } else {
            disposed = true
            unregister?.invoke()
            unregister = null
        }
    }
}

@VisibleForTesting
internal interface ContentResolverForSecureTextField {
    fun registerContentObserver(observer: ContentObserver)

    fun unregisterContentObserver(observer: ContentObserver)

    val showPassword: Boolean
}

private val DefaultContentResolverForSecureTextField:
    (Context) -> ContentResolverForSecureTextField =
    { context ->
        val contentResolver = context.contentResolver
        object : ContentResolverForSecureTextField {
            override fun registerContentObserver(observer: ContentObserver) =
                contentResolver.registerContentObserver(
                    /* uri = */ Settings.System.getUriFor(TEXT_SHOW_PASSWORD),
                    /* notifyForDescendants = */ false,
                    /* observer = */ observer,
                )

            override fun unregisterContentObserver(observer: ContentObserver) =
                contentResolver.unregisterContentObserver(observer)

            override val showPassword: Boolean
                get() =
                    try {
                        Settings.System.getInt(contentResolver, TEXT_SHOW_PASSWORD) > 0
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch show password setting, using value: true", e)
                        true
                    }
        }
    }

@VisibleForTesting
internal var contentResolverForSecureTextField: (Context) -> ContentResolverForSecureTextField =
    DefaultContentResolverForSecureTextField

@VisibleForTesting
internal fun resetContentResolverForSecureTextField() {
    contentResolverForSecureTextField = DefaultContentResolverForSecureTextField
}

private inline fun Executor.tryExecute(crossinline block: () -> Unit) {
    try {
        execute { block() }
    } catch (_: RejectedExecutionException) {
        block()
    }
}
