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
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat

private const val TAG = "BasicSecureTextField"

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

    DisposableEffect(provider) {
        val unregister =
            provider.registerObserver {
                splitSettings =
                    SplitVisibilitySettings(
                        touch = provider.shouldShowTouchInput(),
                        physical = provider.shouldShowPhysicalInput(),
                    )
            }
        onDispose { unregister.run() }
    }
    return splitSettings
}
