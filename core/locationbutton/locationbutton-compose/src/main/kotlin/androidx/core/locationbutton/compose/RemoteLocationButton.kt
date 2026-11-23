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
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.LocaleList
import android.view.Display
import android.view.SurfaceView
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewTreeObserver
import androidx.activity.compose.LocalActivity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.offset
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

    var surfaceHostToken by remember { mutableStateOf<IBinder?>(null) }
    // The display the surface is shown on, which the session must be opened for. It isn't
    // necessarily the default display or the display of the hosting activity.
    var surfaceDisplayId by remember { mutableIntStateOf(Display.INVALID_DISPLAY) }
    var openedSession by remember { mutableStateOf<LocationButtonSession?>(null) }
    var surfaceViewRef by remember { mutableStateOf<SurfaceView?>(null) }

    // The size resolved by layout, or null until the first layout pass. The session request
    // carries this size, so it must not be built before it is known.
    var layoutSize by remember { mutableStateOf<IntSize?>(null) }

    // Holder for last applied state to avoid multiple recompositions triggering redundant calls
    val sessionState = remember { SessionState() }

    val currentOnPermissionResult by rememberUpdatedState(onPermissionResult)
    val currentOnError by rememberUpdatedState(onError)

    // A border needs both a width and a color, as for the local button. Otherwise the width is the
    // platform's default of 0, which also removes a border whose width or color becomes
    // unspecified.
    val hasStroke = strokeWidth.isSpecified && strokeColor.isSpecified
    val strokeWidthPx =
        if (hasStroke) {
            with(density) { strokeWidth.roundToPx() }
        } else {
            0
        }

    Layout(
        modifier = modifier,
        content = {
            // Hidden measurement twin. This is the same button the system renders, composed
            // locally only so that it can be measured. It is measured but never placed, so it is
            // skipped during layout and draw and stays out of the semantics tree.
            LocationButtonContent(
                onClick = {},
                modifier = Modifier.clearAndSetSemantics {},
                referenceHeight = ButtonDefaults.MinHeight,
                backgroundColor = backgroundColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius,
                pressedCornerRadius = pressedCornerRadius,
                iconTint = iconTint,
                textType = textType,
                textColor = textColor,
            )

            val hostToken = surfaceHostToken
            val displayId = surfaceDisplayId
            val size = layoutSize
            // The session's callbacks update the views, so they run on the views' UI thread.
            val handler = LocalView.current.handler
            if (
                hostToken != null &&
                    displayId != Display.INVALID_DISPLAY &&
                    size != null &&
                    handler != null
            ) {
                // This effect owns the session: it opens one once the surface is attached and laid
                // out, and closes it when the surface, its display or the UI thread handler changes
                // or the button leaves the composition. A session can't go back to the default
                // corner radius, so a radius that becomes specified or unspecified opens a new one.
                // The provider is only used to open the session, so it isn't a key.
                DisposableEffect(
                    hostToken,
                    displayId,
                    handler,
                    cornerRadius.isSpecified,
                    pressedCornerRadius.isSpecified,
                ) {
                    // Set when this effect is disposed. A session that only opens after that is
                    // closed right away, since nothing else would close it.
                    var isDisposed = false
                    val client =
                        object : LocationButtonClient {
                            override fun onPermissionResult(granted: Boolean) {
                                currentOnPermissionResult(granted)
                            }

                            override fun onSessionError(throwable: Throwable) {
                                if (isDisposed) {
                                    return
                                }
                                openedSession?.close()
                                openedSession = null
                                currentOnError(throwable)
                            }

                            override fun onSessionOpened(session: LocationButtonSession) {
                                if (isDisposed) {
                                    session.close()
                                    return
                                }
                                openedSession = session

                                surfaceViewRef?.let { view ->
                                    view.setChildSurfacePackage(session.surfacePackage)
                                    view.setCompositionOrder(compositionOrder)
                                    sessionState.compositionOrder = compositionOrder
                                    view.invalidate()
                                }
                            }
                        }

                    val activity =
                        activity
                            ?: throw IllegalStateException(
                                "LocationButton must be hosted within an Activity context"
                            )
                    val paddings = with(density) { safePaddings(clickablePadding, layoutDirection) }

                    val requestBuilder =
                        LocationButtonRequest.Builder(size.width, size.height, configuration)
                            .setPaddingLeft(paddings.left)
                            .setPaddingTop(paddings.top)
                            .setPaddingRight(paddings.right)
                            .setPaddingBottom(paddings.bottom)
                            .setBackgroundColor(backgroundColor.toArgb())
                            .setIconTint(iconTint.toArgb())
                            .setTextType(textType)
                            .setTextColor(textColor.toArgb())

                    if (hasStroke) {
                        requestBuilder.setStrokeWidth(strokeWidthPx)
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

                    sessionState.initialize(request, configuration)

                    provider.openSession(
                        activity,
                        hostToken,
                        displayId,
                        request,
                        handler::post,
                        client,
                    )

                    onDispose {
                        isDisposed = true
                        openedSession?.close()
                        openedSession = null
                        surfaceViewRef?.clearChildSurfacePackage()
                    }
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    SurfaceView(viewContext).apply {
                        holder.setFormat(PixelFormat.TRANSPARENT)
                        setZOrderOnTop(true)
                        surfaceViewRef = this

                        // A window that moves to another display is laid out again, with or
                        // without a configuration change, so each layout reads the display.
                        val globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
                            // A layout pass still calls a listener that was removed during the
                            // pass, so this can run right after the surface is detached.
                            if (!isAttachedToWindow) {
                                return@OnGlobalLayoutListener
                            }
                            surfaceDisplayId =
                                checkNotNull(display) {
                                        "LocationButton's surface has no display while attached"
                                    }
                                    .displayId
                        }

                        addOnAttachStateChangeListener(
                            object : OnAttachStateChangeListener {
                                override fun onViewAttachedToWindow(v: View) {
                                    @Suppress("DEPRECATION")
                                    val token = (v as SurfaceView).hostToken
                                    surfaceHostToken = token
                                    surfaceDisplayId =
                                        checkNotNull(v.display) {
                                                "LocationButton's surface has no display while attached"
                                            }
                                            .displayId
                                    v.viewTreeObserver.addOnGlobalLayoutListener(
                                        globalLayoutListener
                                    )
                                }

                                override fun onViewDetachedFromWindow(v: View) {
                                    v.viewTreeObserver.removeOnGlobalLayoutListener(
                                        globalLayoutListener
                                    )
                                    surfaceHostToken = null
                                    surfaceDisplayId = Display.INVALID_DISPLAY
                                }
                            }
                        )
                    }
                },
                update = { view ->
                    // Each set method only calls the session when the value changed. This block
                    // reads openedSession and layoutSize, so it also runs again when the session
                    // opens or layout records a new size.
                    openedSession?.let { session ->
                        sessionState.setConfiguration(session, configuration)
                        layoutSize?.let { size -> sessionState.setSize(session, size) }
                        sessionState.setPadding(
                            session,
                            with(density) { safePaddings(clickablePadding, layoutDirection) },
                        )
                        sessionState.setBackgroundColor(session, backgroundColor)
                        sessionState.setStrokeWidth(session, strokeWidthPx)
                        if (hasStroke) {
                            sessionState.setStrokeColor(session, strokeColor)
                        }
                        if (cornerRadius.isSpecified) {
                            sessionState.setCornerRadius(
                                session,
                                with(density) { cornerRadius.toPx() },
                            )
                        }
                        if (pressedCornerRadius.isSpecified) {
                            sessionState.setPressedCornerRadius(
                                session,
                                with(density) { pressedCornerRadius.toPx() },
                            )
                        }
                        sessionState.setIconTint(session, iconTint)
                        sessionState.setTextType(session, textType)
                        sessionState.setTextColor(session, textColor)
                        sessionState.setCompositionOrder(view, compositionOrder)
                    }
                },
            )
        },
    ) { measurables, constraints ->
        // The measure policy only decides the size. Sending it to the system is a side effect,
        // so the update block does it once placement has recorded the result.
        val twinMeasurable = measurables[0]
        val surfaceMeasurable = measurables[1]

        // The twin is the button inside the padding, so it is measured within the constraints minus
        // the padding, as the local button is. Relax the minimums so the twin reports its natural
        // size instead of being stretched to whatever the parent happens to allow.
        val paddings = safePaddings(clickablePadding, layoutDirection)
        val horizontalPaddingPx = paddings.left + paddings.right
        val verticalPaddingPx = paddings.top + paddings.bottom
        val twinPlaceable =
            twinMeasurable.measure(
                constraints
                    .offset(-horizontalPaddingPx, -verticalPaddingPx)
                    .copy(minWidth = 0, minHeight = 0)
            )

        // The system subtracts the padding again on its side, so the requested size includes it.
        val naturalWidth = twinPlaceable.width + horizontalPaddingPx
        val naturalHeight = twinPlaceable.height + verticalPaddingPx

        val minSizePx = MinTouchTargetSize.roundToPx()
        val maxSizePx = MaxButtonHeight.roundToPx()

        // Only a fixed constraint is a size the caller asked for. A bounded constraint is an upper
        // limit, and treating it as required is what made the button fill its parent.
        //
        // When wrapping, the size is ours to choose, so it is clamped to the same bounds the system
        // enforces. Otherwise this layout slot and the surface the system renders into disagree,
        // and the SurfaceView clips the result. An explicit caller size is passed through as is, so
        // that both implementations lay out identically.
        val widthPx =
            if (constraints.hasFixedWidth) {
                constraints.maxWidth
            } else {
                naturalWidth.coerceAtLeast(minSizePx)
            }
        val heightPx =
            if (constraints.hasFixedHeight) {
                constraints.maxHeight
            } else {
                naturalHeight.coerceIn(minSizePx, maxSizePx)
            }

        val placeable = surfaceMeasurable.measure(Constraints.fixed(widthPx, heightPx))
        // The twin is deliberately not placed, so it is never drawn.
        layout(widthPx, heightPx) {
            // Placement doesn't run for intrinsic measurements, so only a real layout pass records
            // the size the session uses.
            layoutSize = IntSize(widthPx, heightPx)
            placeable.place(0, 0)
        }
    }
}

/**
 * Holds the state of the location button configurations. Values are in pixels, except colors and
 * textType.
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
    var compositionOrder: Int = LocationButtonDefaults.defaultCompositionOrder,
    var locales: LocaleList = LocaleList.getEmptyLocaleList(),
) {
    fun initialize(request: LocationButtonRequest, config: Configuration) {
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
        this.locales = config.locales
    }

    // Each set method below only calls the session when the value differs from the last one it
    // got, and then records the new value.

    fun setConfiguration(session: LocationButtonSession, configuration: Configuration) {
        if (configuration.locales != locales) {
            session.changeConfiguration(configuration)
            locales = configuration.locales
        }
    }

    fun setSize(session: LocationButtonSession, size: IntSize) {
        if (size.width != width || size.height != height) {
            session.resize(size.width, size.height)
            width = size.width
            height = size.height
        }
    }

    fun setPadding(session: LocationButtonSession, paddings: SafePaddings) {
        if (
            paddings.left != paddingLeft ||
                paddings.top != paddingTop ||
                paddings.right != paddingRight ||
                paddings.bottom != paddingBottom
        ) {
            session.setPadding(paddings.left, paddings.top, paddings.right, paddings.bottom)
            paddingLeft = paddings.left
            paddingTop = paddings.top
            paddingRight = paddings.right
            paddingBottom = paddings.bottom
        }
    }

    fun setBackgroundColor(session: LocationButtonSession, color: Color) {
        if (color != backgroundColor) {
            session.setBackgroundColor(color.toArgb())
            backgroundColor = color
        }
    }

    fun setStrokeColor(session: LocationButtonSession, color: Color) {
        if (color != strokeColor) {
            session.setStrokeColor(color.toArgb())
            strokeColor = color
        }
    }

    fun setStrokeWidth(session: LocationButtonSession, widthPx: Int) {
        if (widthPx != strokeWidth) {
            session.setStrokeWidth(widthPx)
            strokeWidth = widthPx
        }
    }

    fun setCornerRadius(session: LocationButtonSession, radiusPx: Float) {
        if (radiusPx != cornerRadius) {
            session.setCornerRadius(radiusPx)
            cornerRadius = radiusPx
        }
    }

    fun setPressedCornerRadius(session: LocationButtonSession, radiusPx: Float) {
        if (radiusPx != pressedCornerRadius) {
            session.setPressedCornerRadius(radiusPx)
            pressedCornerRadius = radiusPx
        }
    }

    fun setIconTint(session: LocationButtonSession, color: Color) {
        if (color != iconTint) {
            session.setIconTint(color.toArgb())
            iconTint = color
        }
    }

    fun setTextType(session: LocationButtonSession, textType: Int) {
        if (textType != this.textType) {
            session.setTextType(textType)
            this.textType = textType
        }
    }

    fun setTextColor(session: LocationButtonSession, color: Color) {
        if (color != textColor) {
            session.setTextColor(color.toArgb())
            textColor = color
        }
    }

    fun setCompositionOrder(view: SurfaceView, compositionOrder: Int) {
        if (compositionOrder != this.compositionOrder) {
            view.setCompositionOrder(compositionOrder)
            this.compositionOrder = compositionOrder
        }
    }
}

/**
 * Resolves [padding] to the padding in pixels the system accepts, between [MinPadding] and
 * [MaxPadding] on each side.
 */
private fun Density.safePaddings(
    padding: PaddingValues,
    layoutDirection: LayoutDirection,
): SafePaddings {
    val minPaddingPx = MinPadding.roundToPx()
    val maxPaddingPx = MaxPadding.roundToPx()
    return SafePaddings(
        left =
            padding
                .calculateStartPadding(layoutDirection)
                .roundToPx()
                .coerceIn(minPaddingPx, maxPaddingPx),
        right =
            padding
                .calculateEndPadding(layoutDirection)
                .roundToPx()
                .coerceIn(minPaddingPx, maxPaddingPx),
        top = padding.calculateTopPadding().roundToPx().coerceIn(minPaddingPx, maxPaddingPx),
        bottom = padding.calculateBottomPadding().roundToPx().coerceIn(minPaddingPx, maxPaddingPx),
    )
}

private data class SafePaddings(val left: Int, val top: Int, val right: Int, val bottom: Int)
