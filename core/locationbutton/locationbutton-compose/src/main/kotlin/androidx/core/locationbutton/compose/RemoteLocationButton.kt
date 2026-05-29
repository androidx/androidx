/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.locationbutton.compose

import android.app.permissionui.LocationButtonClient
import android.app.permissionui.LocationButtonProvider
import android.app.permissionui.LocationButtonProviderFactory
import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView

/** CompositionLocal to provide a [LocationButtonProvider]. */
@get:RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
public val LocalLocationButtonProvider: ProvidableCompositionLocal<LocationButtonProvider?> =
    staticCompositionLocalOf {
        null
    }

/**
 * A Composable that renders the remote location button.
 *
 * This component handles the connection to the remote rendering service via
 * [LocationButtonProvider] and displays the button inside a [SurfaceView]. It manages the session
 * lifecycle and updates the remote button when properties change.
 */
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
@Composable
internal fun RemoteLocationButton(
    modifier: Modifier,
    backgroundColor: Color,
    strokeColor: Color,
    strokeWidth: Dp,
    cornerRadius: Dp,
    pressedCornerRadius: Dp,
    iconTint: Color,
    textType: Int,
    textColor: Color,
    clickablePadding: PaddingValues,
    compositionOrder: Int,
    onError: (Throwable) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val activity = LocalActivity.current
    val configuration = LocalConfiguration.current
    var provider = LocalLocationButtonProvider.current
    if (provider == null) {
        provider =
            remember(context) { LocationButtonProviderFactory.create(context.applicationContext) }
    }

    val layoutDirection = LocalLayoutDirection.current
    val safePaddings =
        remember(density, clickablePadding, layoutDirection) {
            with(density) {
                val minPaddingPx = MinPadding.roundToPx()
                val maxPaddingPx = MaxPadding.roundToPx()
                SafePaddings(
                    left =
                        clickablePadding
                            .calculateStartPadding(layoutDirection)
                            .roundToPx()
                            .coerceIn(minPaddingPx, maxPaddingPx),
                    right =
                        clickablePadding
                            .calculateEndPadding(layoutDirection)
                            .roundToPx()
                            .coerceIn(minPaddingPx, maxPaddingPx),
                    top =
                        clickablePadding
                            .calculateTopPadding()
                            .roundToPx()
                            .coerceIn(minPaddingPx, maxPaddingPx),
                    bottom =
                        clickablePadding
                            .calculateBottomPadding()
                            .roundToPx()
                            .coerceIn(minPaddingPx, maxPaddingPx),
                )
            }
        }

    var surfaceHostToken by remember { mutableStateOf<IBinder?>(null) }
    var openedSession by remember { mutableStateOf<LocationButtonSession?>(null) }
    var isSessionOpening by remember { mutableStateOf(false) }
    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    // Holder for last applied state to avoid multiple recompositions triggering redundant calls
    val sessionState = remember { SessionState() }

    val currentOnPermissionResult by rememberUpdatedState(onPermissionResult)
    val currentOnError by rememberUpdatedState(onError)

    val clientCallback = remember {
        object : LocationButtonClient {
            override fun onPermissionResult(granted: Boolean) {
                currentOnPermissionResult(granted)
            }

            override fun onSessionError(throwable: Throwable) {
                openedSession?.close()
                openedSession = null
                isSessionOpening = false
                currentOnError(throwable)
            }

            override fun onSessionOpened(session: LocationButtonSession) {
                openedSession = session
                isSessionOpening = false

                surfaceViewRef?.let { view ->
                    view.setChildSurfacePackage(session.surfacePackage)
                    view.setCompositionOrder(compositionOrder)
                    view.invalidate()
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx =
            with(density) {
                if (maxWidth != Dp.Infinity) maxWidth.roundToPx()
                else ButtonDefaults.MinWidth.roundToPx()
            }
        val heightPx =
            with(density) {
                if (maxHeight != Dp.Infinity) maxHeight.roundToPx()
                else ButtonDefaults.MinHeight.roundToPx()
            }

        LaunchedEffect(surfaceHostToken) {
            val token = surfaceHostToken ?: return@LaunchedEffect

            // If surface token changes, we must close old session and open a new one
            openedSession?.close()
            openedSession = null

            if (isSessionOpening) {
                return@LaunchedEffect
            }

            isSessionOpening = true

            val activity =
                activity
                    ?: throw IllegalStateException(
                        "LocationButton must be hosted within an Activity context"
                    )
            val displayId = getDisplayId(context)

            val requestBuilder =
                LocationButtonRequest.Builder(widthPx, heightPx, configuration)
                    .setPaddingLeft(safePaddings.left)
                    .setPaddingTop(safePaddings.top)
                    .setPaddingRight(safePaddings.right)
                    .setPaddingBottom(safePaddings.bottom)
                    .setBackgroundColor(backgroundColor.toArgb())
                    .setIconTint(iconTint.toArgb())
                    .setTextType(textType)
                    .setTextColor(textColor.toArgb())

            if (strokeWidth.isSpecified) {
                val strokePx = with(density) { strokeWidth.roundToPx() }
                requestBuilder.setStrokeWidth(strokePx)
            }
            if (strokeColor.isSpecified) {
                requestBuilder.setStrokeColor(strokeColor.toArgb())
            }

            if (cornerRadius.isSpecified) {
                val cornerPx = with(density) { cornerRadius.toPx() }
                requestBuilder.setCornerRadius(cornerPx)
            }
            if (pressedCornerRadius.isSpecified) {
                val pressedPx = with(density) { pressedCornerRadius.toPx() }
                requestBuilder.setPressedCornerRadius(pressedPx)
            }

            val request = requestBuilder.build()

            sessionState.initialize(request)

            provider.openSession(
                activity,
                token,
                displayId,
                request,
                context.mainExecutor,
                clientCallback,
            )
        }

        DisposableEffect(Unit) {
            onDispose {
                openedSession?.close()
                openedSession = null
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                SurfaceView(viewContext).apply {
                    holder.setFormat(PixelFormat.TRANSPARENT)
                    setZOrderOnTop(true)
                    surfaceViewRef = this

                    addOnAttachStateChangeListener(
                        object : OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {
                                @Suppress("DEPRECATION") val token = (v as SurfaceView).hostToken
                                surfaceHostToken = token
                            }

                            override fun onViewDetachedFromWindow(v: View) {
                                surfaceHostToken = null
                            }
                        }
                    )
                }
            },
            update = { view ->
                openedSession?.let { session ->
                    if (widthPx != sessionState.width || heightPx != sessionState.height) {
                        session.resize(widthPx, heightPx)
                        sessionState.width = widthPx
                        sessionState.height = heightPx
                    }

                    if (
                        safePaddings.left != sessionState.paddingLeft ||
                            safePaddings.top != sessionState.paddingTop ||
                            safePaddings.right != sessionState.paddingRight ||
                            safePaddings.bottom != sessionState.paddingBottom
                    ) {
                        session.setPadding(
                            safePaddings.left,
                            safePaddings.top,
                            safePaddings.right,
                            safePaddings.bottom,
                        )
                        sessionState.paddingLeft = safePaddings.left
                        sessionState.paddingTop = safePaddings.top
                        sessionState.paddingRight = safePaddings.right
                        sessionState.paddingBottom = safePaddings.bottom
                    }

                    if (backgroundColor != sessionState.backgroundColor) {
                        session.setBackgroundColor(backgroundColor.toArgb())
                        sessionState.backgroundColor = backgroundColor
                    }
                    if (strokeColor.isSpecified && strokeColor != sessionState.strokeColor) {
                        session.setStrokeColor(strokeColor.toArgb())
                        sessionState.strokeColor = strokeColor
                    }
                    if (strokeWidth.isSpecified) {
                        val strokePx = with(density) { strokeWidth.roundToPx() }
                        if (strokePx != sessionState.strokeWidth) {
                            session.setStrokeWidth(strokePx)
                            sessionState.strokeWidth = strokePx
                        }
                    }
                    if (cornerRadius.isSpecified) {
                        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
                        if (cornerRadiusPx != sessionState.cornerRadius) {
                            session.setCornerRadius(cornerRadiusPx)
                            sessionState.cornerRadius = cornerRadiusPx
                        }
                    }
                    if (pressedCornerRadius.isSpecified) {
                        val pressedCornerRadiusPx = with(density) { pressedCornerRadius.toPx() }
                        if (pressedCornerRadiusPx != sessionState.pressedCornerRadius) {
                            session.setPressedCornerRadius(pressedCornerRadiusPx)
                            sessionState.pressedCornerRadius = pressedCornerRadiusPx
                        }
                    }
                    if (iconTint != sessionState.iconTint) {
                        session.setIconTint(iconTint.toArgb())
                        sessionState.iconTint = iconTint
                    }
                    if (textType != sessionState.textType) {
                        session.setTextType(textType)
                        sessionState.textType = textType
                    }
                    if (textColor != sessionState.textColor) {
                        session.setTextColor(textColor.toArgb())
                        sessionState.textColor = textColor
                    }
                }
            },
        )
    }
}

/**
 * Holds the state of the remote location button. Values are in pixels, except colors and textType.
 */
@RequiresApi(37)
private class SessionState(
    var width: Int = -1,
    var height: Int = -1,
    var paddingLeft: Int = -1,
    var paddingTop: Int = -1,
    var paddingRight: Int = -1,
    var paddingBottom: Int = -1,
    var backgroundColor: Color = Color.Unspecified,
    var strokeColor: Color = Color.Unspecified,
    var strokeWidth: Int = -1,
    var cornerRadius: Float = -1f,
    var pressedCornerRadius: Float = -1f,
    var iconTint: Color = Color.Unspecified,
    var textType: Int = -1,
    var textColor: Color = Color.Unspecified,
) {
    fun initialize(request: LocationButtonRequest) {
        this.width = request.width
        this.height = request.height
        this.paddingLeft = request.paddingLeft
        this.paddingTop = request.paddingTop
        this.paddingRight = request.paddingRight
        this.paddingBottom = request.paddingBottom
        this.backgroundColor = Color(request.backgroundColor)
        this.strokeColor = Color(request.strokeColor)
        this.strokeWidth = request.strokeWidth
        this.cornerRadius = request.cornerRadius
        this.pressedCornerRadius = request.pressedCornerRadius
        this.iconTint = Color(request.iconTint)
        this.textType = request.textType
        this.textColor = Color(request.textColor)
    }
}

private fun getDisplayId(context: android.content.Context): Int {
    val displayManager = context.getSystemService(DisplayManager::class.java)
    return displayManager?.displays?.firstOrNull()?.displayId ?: 0
}

private data class SafePaddings(val left: Int, val top: Int, val right: Int, val bottom: Int)
