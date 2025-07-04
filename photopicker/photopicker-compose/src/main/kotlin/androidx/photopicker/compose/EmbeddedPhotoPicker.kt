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

package androidx.photopicker.compose

import android.os.Build
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerProviderFactory
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/** The default [EmbeddedPhotoPickerFeatureInfo] that is used if one is not provided. */
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
internal val DEFAULT_FEATURE_INFO = EmbeddedPhotoPickerFeatureInfo.Builder().build()

/**
 * Compose entry-point into the EmbeddedPhotoPicker. This composable hosts a remote view from the
 * [EmbeddedPhotoPickerProvider] and interacts with the provided [EmbeddedPhotoPickerState] and
 * coordinates the state between the local compose view and remote view.
 *
 * @param state The state object for the EmbeddedPhotoPicker which can be manually implemented, or a
 *   default implementation may be obtained via the [rememberEmbeddedPhotoPickerState] composable.
 * @param modifier An optional [Modifier] for the root view of the PhotoPicker.
 * @param provider An optional [EmbeddedPhotoPickerProvider] that will be used to connect to the
 *   PhotoPicker service. If null, the platform default will be used.
 * @param embeddedPhotoPickerFeatureInfo A set of features that will be provided to the PhotoPicker
 *   when a session is started.
 */
@Composable
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@ExperimentalPhotoPickerComposeApi
public fun EmbeddedPhotoPicker(
    state: EmbeddedPhotoPickerState,
    modifier: Modifier = Modifier,
    provider: EmbeddedPhotoPickerProvider? = null,
    embeddedPhotoPickerFeatureInfo: EmbeddedPhotoPickerFeatureInfo = DEFAULT_FEATURE_INFO,
) {

    // A reference is required to the open session here so that the surfacePackage can
    // be obtained by the AndroidView which hosts the SurfaceView.
    var openedSession by remember { mutableStateOf<EmbeddedPhotoPickerSession?>(null) }

    // There isn't a compose native SurfaceView, so wrap with AndroidView, as SurfaceView is
    // a required component for the EmbeddedPicker architecture.
    AndroidView(
        modifier =
            modifier.onSizeChanged { size ->

                // If there is a running session it needs to be notified of any size changes
                // to the SurfaceView
                state.notifyResized(size)
            },
        factory = { viewContext ->
            SurfaceView(viewContext).apply {

                // The EmbeddedPhotoPicker wants to draw above the window to prevent other UI
                // elements from drawing on top of it.
                setZOrderOnTop(true)

                // Listen for the SurfaceView being attached to the window so that its hostToken
                // can be obtained.
                addOnAttachStateChangeListener(
                    object : OnAttachStateChangeListener {

                        override fun onViewAttachedToWindow(view: View) {
                            @Suppress("DEPRECATION")
                            // When the SurfaceView is attached, provide its host token to the state
                            // object.
                            state.surfaceHostToken = hostToken
                        }

                        override fun onViewDetachedFromWindow(view: View) {
                            state.surfaceHostToken = null
                        }
                    }
                )
            }
        },

        // update will run immediately after the factory, and when any observed state changes.
        // In this case, openedSession won't exist after the factory, so this will get run again
        // once we receive the session from the provider so that the surfacePackage can be attached.
        update = { view -> openedSession?.surfacePackage?.let { view.setChildSurfacePackage(it) } },
    )

    // Either use the (provided) provider or create one that binds to the applicationContext
    // by default. This will allow the underlying bound service to survive any activity
    // recreations and is better than attaching to the activity itself.
    val context = LocalContext.current
    val photopickerProvider =
        remember(provider) {
            provider ?: EmbeddedPhotoPickerProviderFactory.create(context.applicationContext)
        }

    // Wait until the state object is ready before starting the LaunchedEffect
    // val isReady by state.isReady
    if (state.isReady) {
        LaunchedEffect(
            // Restart this launched effect if the provider or state object changes
            state,
            photopickerProvider,
        ) {
            state.runSession(
                provider = photopickerProvider,
                featureInfo = embeddedPhotoPickerFeatureInfo,
                onReceiveSession = { openedSession = it },
            )
        }
    }
}
