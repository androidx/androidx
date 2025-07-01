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
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.HandlerCompat

private const val TAG = "BasicSecureTextField"

@Composable
internal actual fun platformAllowsRevealLastTyped(): Boolean {
    val context = LocalContext.current
    val resolver =
        remember(context, contentResolverForSecureTextField) {
            contentResolverForSecureTextField(context)
        }
    var state by remember(resolver) { mutableStateOf(resolver.showPassword) }
    val settingsObserver =
        remember(resolver) {
            object : ContentObserver(HandlerCompat.createAsync(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    state = resolver.showPassword
                }
            }
        }
    DisposableEffect(resolver, settingsObserver) {
        resolver.registerContentObserver(
            Settings.System.getUriFor(TEXT_SHOW_PASSWORD),
            false,
            settingsObserver,
        )
        onDispose { resolver.unregisterContentObserver(settingsObserver) }
    }
    return state
}

@VisibleForTesting
internal interface ContentResolverForSecureTextField {
    fun registerContentObserver(uri: Uri, notifyForDescendants: Boolean, observer: ContentObserver)

    fun unregisterContentObserver(observer: ContentObserver)

    val showPassword: Boolean
}

private val DefaultContentResolverForSecureTextField:
    (Context) -> ContentResolverForSecureTextField =
    { context ->
        val contentResolver = context.contentResolver
        object : ContentResolverForSecureTextField {
            override fun registerContentObserver(
                uri: Uri,
                notifyForDescendants: Boolean,
                observer: ContentObserver,
            ) = contentResolver.registerContentObserver(uri, notifyForDescendants, observer)

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
