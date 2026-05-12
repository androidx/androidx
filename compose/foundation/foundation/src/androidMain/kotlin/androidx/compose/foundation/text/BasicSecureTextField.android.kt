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
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.System.TEXT_SHOW_PASSWORD
import android.text.ShowSecretsSetting
import android.util.Log
import androidx.annotation.RequiresApi
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

/**
 * Interface abstracting the access to system password visibility settings. Resolves differences
 * between platform versions and provides independent control for touch and physical input sources
 * where supported.
 */
internal interface PasswordVisibilitySetting {
    fun shouldShowTouchInput(): Boolean

    fun shouldShowPhysicalInput(): Boolean

    /**
     * Registers an observer to be notified when the system password visibility settings change.
     *
     * @param onChange Callback invoked when the settings change.
     * @return A [Runnable] that, when executed, unregisters the observer.
     */
    fun registerObserver(onChange: () -> Unit): Runnable
}

/** Android implementation that reads settings from [Settings.System]. */
private open class PlatformPasswordVisibilitySettingImpl(protected val context: Context) :
    PasswordVisibilitySetting {
    override fun shouldShowTouchInput(): Boolean = getSystemShowPasswordSetting()

    override fun shouldShowPhysicalInput(): Boolean = getSystemShowPasswordSetting()

    /** Fallback for SDK < 37 to read the system show password setting. */
    private fun getSystemShowPasswordSetting(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, TEXT_SHOW_PASSWORD) > 0
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch show password setting, using value: true", e)
            true
        }
    }

    override fun registerObserver(onChange: () -> Unit): Runnable {
        val uri = Settings.System.getUriFor(TEXT_SHOW_PASSWORD)
        val observer =
            object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    onChange()
                }
            }
        context.contentResolver.registerContentObserver(uri, false, observer)
        return Runnable { context.contentResolver.unregisterContentObserver(observer) }
    }
}

/** Android implementation that reads settings from `ShowSecretsSetting` on API 37+. */
@RequiresApi(37)
private class PlatformPasswordVisibilitySettingApi37(context: Context) :
    PlatformPasswordVisibilitySettingImpl(context) {
    override fun shouldShowTouchInput(): Boolean {
        return ShowSecretsSetting.shouldShowTouchInput(context)
    }

    override fun shouldShowPhysicalInput(): Boolean {
        return ShowSecretsSetting.shouldShowPhysicalInput(context)
    }

    override fun registerObserver(onChange: () -> Unit): Runnable {
        val runnable = Runnable { onChange() }
        return ShowSecretsSetting.registerCallback(context, runnable)
    }
}

/**
 * Factory for creating [PasswordVisibilitySetting] instances. Visible for testing to allow mocking
 * platform settings.
 */
@VisibleForTesting
internal var passwordVisibilitySettingFactory: (Context) -> PasswordVisibilitySetting = { context ->
    if (Build.VERSION.SDK_INT >= 37) {
        PlatformPasswordVisibilitySettingApi37(context)
    } else {
        PlatformPasswordVisibilitySettingImpl(context)
    }
}

/**
 * Resets the [passwordVisibilitySettingFactory] to the default implementation. Visible for testing
 * to clean up after tests that modify the factory.
 */
@VisibleForTesting
internal fun resetPasswordVisibilitySettingFactory() {
    passwordVisibilitySettingFactory = { context ->
        if (Build.VERSION.SDK_INT >= 37) {
            PlatformPasswordVisibilitySettingApi37(context)
        } else {
            PlatformPasswordVisibilitySettingImpl(context)
        }
    }
}

@Composable
internal actual fun rememberPlatformPasswordVisibilitySettingsState(): SplitVisibilitySettings {
    val context = LocalContext.current
    val executor = LocalTextFieldContentObserverRegistrationExecutor.current
    val provider = remember(context) { passwordVisibilitySettingFactory(context) }
    var splitSettings by
        remember(provider) {
            mutableStateOf(
                SplitVisibilitySettings(
                    touch = provider.shouldShowTouchInput(),
                    physical = provider.shouldShowPhysicalInput(),
                )
            )
        }

    // we are not passing the [executor] as a key here because once the registration is
    // completed it doesn't make sense to re-register the observer on a new background thread.
    val registrationToken = remember(provider) { RegistrationToken(executor) }

    DisposableEffect(registrationToken) {
        registrationToken.register(provider) {
            splitSettings =
                SplitVisibilitySettings(
                    touch = provider.shouldShowTouchInput(),
                    physical = provider.shouldShowPhysicalInput(),
                )
        }
        onDispose { registrationToken.dispose() }
    }
    return splitSettings
}

private class RegistrationToken(private val executor: Executor?) {
    private var unregister: Runnable? = null
    private var disposed = false

    fun register(provider: PasswordVisibilitySetting, onChange: () -> Unit) {
        if (executor != null) {
            executor.tryExecute {
                val unregisterRunnable = provider.registerObserver(onChange)
                var runImmediately = false
                // We synchronize only to safely read/write the shared `disposed` and `unregister`
                // state. Do not run the foreign `unregisterRunnable.run()` inside the lock to
                // prevent potential deadlocks or long lock holds since it makes an IPC binder call
                // internally.
                synchronized(this) {
                    if (disposed) {
                        runImmediately = true
                    } else {
                        unregister = unregisterRunnable
                    }
                }
                if (runImmediately) {
                    unregisterRunnable.run()
                }
            }
        } else {
            unregister = provider.registerObserver(onChange)
        }
    }

    fun dispose() {
        if (executor != null) {
            executor.tryExecute {
                var toRun: Runnable? = null
                // We synchronize only to safely update the shared `disposed` and `unregister`
                // state. To prevent deadlocks and lock contention, we capture the unregister
                // Runnable and execute it outside the lock block.
                synchronized(this) {
                    disposed = true
                    toRun = unregister
                    unregister = null
                }
                toRun?.run()
            }
        } else {
            disposed = true
            unregister?.run()
            unregister = null
        }
    }
}

private inline fun Executor.tryExecute(crossinline block: () -> Unit) {
    try {
        execute { block() }
    } catch (_: RejectedExecutionException) {
        block()
    }
}
