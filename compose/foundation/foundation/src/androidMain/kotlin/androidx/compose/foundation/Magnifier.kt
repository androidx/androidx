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

package androidx.compose.foundation

import android.os.Build
import android.view.View
import android.widget.Magnifier
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

/**
 * A function on elements that are magnified with a [magnifier] modifier that returns the position
 * of the center of the magnified content in the coordinate space of the root composable.
 */
internal val MagnifierPositionInRoot =
    SemanticsPropertyKey<() -> Offset>("MagnifierPositionInRoot")

/**
 * Shows a [Magnifier] widget that shows an enlarged version of the content at [sourceCenter]
 * relative to the current layout node.
 *
 * This function returns a no-op modifier on API levels below P (28), since the framework does not
 * support the [Magnifier] widget on those levels. However, even on higher API levels, not all
 * magnifier features are supported on all platforms. Please refer to parameter explanations below
 * to learn more about supported features on different platform versions.
 *
 * This function does not allow configuration of [source bounds][Magnifier.Builder.setSourceBounds]
 * since the magnifier widget does not support constraining to the bounds of composables.
 *
 * @sample androidx.compose.foundation.samples.MagnifierSample
 *
 * @param sourceCenter The offset of the center of the magnified content. Measured in pixels from
 * the top-left of the layout node this modifier is applied to. This offset is passed to
 * [Magnifier.show].
 * @param magnifierCenter The offset of the magnifier widget itself, where the magnified content is
 * rendered over the original content. Measured in density-independent pixels from the top-left of
 * the layout node this modifier is applied to. If [unspecified][DpOffset.Unspecified], the
 * magnifier widget will be placed at a default offset relative to [sourceCenter]. The value of that
 * offset is specified by the system.
 * @param onSizeChanged An optional callback that will be invoked when the magnifier widget is
 * initialized to report on its actual size. This can be useful when [size] parameter is left
 * unspecified.
 * @param zoom See [Magnifier.setZoom]. Only supported on API 29+.
 * @param size See [Magnifier.Builder.setSize]. Only supported on API 29+.
 * @param cornerRadius See [Magnifier.Builder.setCornerRadius]. Only supported on API 29+.
 * @param elevation See [Magnifier.Builder.setElevation]. Only supported on API 29+.
 * @param clippingEnabled See [Magnifier.Builder.setClippingEnabled]. Only supported on API 29+.
 */
fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    onSizeChanged: ((DpSize) -> Unit)? = null,
    zoom: Float = Float.NaN,
    size: DpSize = DpSize.Unspecified,
    cornerRadius: Dp = Dp.Unspecified,
    elevation: Dp = Dp.Unspecified,
    clippingEnabled: Boolean = true
): Modifier {
    return magnifier(
        sourceCenter = sourceCenter,
        magnifierCenter = magnifierCenter,
        onSizeChanged = onSizeChanged,
        zoom = zoom,
        useTextDefault = false,
        size = size,
        cornerRadius = cornerRadius,
        elevation = elevation,
        clippingEnabled = clippingEnabled
    )
}

/**
 * For testing and internal Text usage purposes.
 *
 * TextField and SelectionManager uses this internal API to pass `useTextDefault` as true.
 */
internal fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    onSizeChanged: ((DpSize) -> Unit)? = null,
    zoom: Float = Float.NaN,
    useTextDefault: Boolean = false,
    size: DpSize = DpSize.Unspecified,
    cornerRadius: Dp = Dp.Unspecified,
    elevation: Dp = Dp.Unspecified,
    clippingEnabled: Boolean = true,
    platformMagnifierFactory: PlatformMagnifierFactory? = null
): Modifier {
    return if (isPlatformMagnifierSupported()) {
        then(
            MagnifierElement(
                sourceCenter = sourceCenter,
                magnifierCenter = magnifierCenter,
                onSizeChanged = onSizeChanged,
                useTextDefault = useTextDefault,
                zoom = zoom,
                size = size,
                cornerRadius = cornerRadius,
                elevation = elevation,
                clippingEnabled = clippingEnabled,
                platformMagnifierFactory = platformMagnifierFactory
                    ?: PlatformMagnifierFactory.getForCurrentPlatform() // this doesn't do an alloc
            )
        )
    } else {
        // Magnifier is only supported in >=28. So avoid doing all the work to manage the magnifier
        // state if it's not needed.
        // TODO(b/202739980) Investigate supporting Magnifier on earlier versions.
        inspectable(
            // Publish inspector info even if magnification isn't supported.
            inspectorInfo = debugInspectorInfo {
                name = "magnifier (not supported)"
                properties["sourceCenter"] = sourceCenter
                properties["magnifierCenter"] = magnifierCenter
                properties["zoom"] = zoom
                properties["size"] = size
                properties["cornerRadius"] = cornerRadius
                properties["elevation"] = elevation
                properties["clippingEnabled"] = clippingEnabled
            }
        ) { this }
    }
}

internal class MagnifierElement(
    private val sourceCenter: Density.() -> Offset,
    private val magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    private val onSizeChanged: ((DpSize) -> Unit)? = null,
    private val zoom: Float = Float.NaN,
    private val useTextDefault: Boolean = false,
    private val size: DpSize = DpSize.Unspecified,
    private val cornerRadius: Dp = Dp.Unspecified,
    private val elevation: Dp = Dp.Unspecified,
    private val clippingEnabled: Boolean = true,
    private val platformMagnifierFactory: PlatformMagnifierFactory
) : ModifierNodeElement<MagnifierNode>() {

    override fun create(): MagnifierNode {
        return MagnifierNode(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            zoom = zoom,
            useTextDefault = useTextDefault,
            size = size,
            cornerRadius = cornerRadius,
            elevation = elevation,
            clippingEnabled = clippingEnabled,
            onSizeChanged = onSizeChanged,
            platformMagnifierFactory = platformMagnifierFactory
        )
    }

    override fun update(node: MagnifierNode) {
        node.update(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            zoom = zoom,
            useTextDefault = useTextDefault,
            size = size,
            cornerRadius = cornerRadius,
            elevation = elevation,
            clippingEnabled = clippingEnabled,
            onSizeChanged = onSizeChanged,
            platformMagnifierFactory = platformMagnifierFactory
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MagnifierElement) return false

        if (sourceCenter != other.sourceCenter) return false
        if (magnifierCenter != other.magnifierCenter) return false
        if (zoom != other.zoom) return false
        if (useTextDefault != other.useTextDefault) return false
        if (size != other.size) return false
        if (cornerRadius != other.cornerRadius) return false
        if (elevation != other.elevation) return false
        if (clippingEnabled != other.clippingEnabled) return false
        if (onSizeChanged != other.onSizeChanged) return false
        if (platformMagnifierFactory != other.platformMagnifierFactory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceCenter.hashCode()
        result = 31 * result + magnifierCenter.hashCode()
        result = 31 * result + zoom.hashCode()
        result = 31 * result + useTextDefault.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + elevation.hashCode()
        result = 31 * result + clippingEnabled.hashCode()
        result = 31 * result + (onSizeChanged?.hashCode() ?: 0)
        result = 31 * result + platformMagnifierFactory.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "magnifier"
        properties["sourceCenter"] = sourceCenter
        properties["magnifierCenter"] = magnifierCenter
        properties["zoom"] = zoom
        properties["size"] = size
        properties["cornerRadius"] = cornerRadius
        properties["elevation"] = elevation
        properties["clippingEnabled"] = clippingEnabled
    }
}

internal class MagnifierNode(
    var sourceCenter: Density.() -> Offset,
    var magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    var onSizeChanged: ((DpSize) -> Unit)? = null,
    var zoom: Float = Float.NaN,
    var useTextDefault: Boolean = false,
    var size: DpSize = DpSize.Unspecified,
    var cornerRadius: Dp = Dp.Unspecified,
    var elevation: Dp = Dp.Unspecified,
    var clippingEnabled: Boolean = true,
    var platformMagnifierFactory: PlatformMagnifierFactory =
        PlatformMagnifierFactory.getForCurrentPlatform()
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    DrawModifierNode,
    SemanticsModifierNode,
    ObserverModifierNode {

    /**
     * Android [View] that this modifier node is attached to in Compose hierarchy.
     */
    private var view: View? = null

    /**
     * Current density provided by [LocalDensity]. Used as a receiver to callback functions that
     * are expected return pixel targeted offsets.
     */
    private var density: Density? = null

    /**
     * Current magnifier instance.
     */
    private var magnifier: PlatformMagnifier? = null

    /**
     * Anchor Composable's position in root layout.
     */
    private var anchorPositionInRoot: Offset by mutableStateOf(Offset.Unspecified)

    /**
     * Position where [sourceCenter] is mapped on root layout. This is passed to platform magnifier
     * to precisely target the requested location.
     */
    private var sourceCenterInRoot: Offset = Offset.Unspecified

    /**
     * Last reported size to [onSizeChanged]. This is compared to the current size before calling
     * the lambda again.
     */
    private var previousSize: IntSize? = null

    fun update(
        sourceCenter: Density.() -> Offset,
        magnifierCenter: Density.() -> Offset,
        zoom: Float,
        useTextDefault: Boolean,
        size: DpSize,
        cornerRadius: Dp,
        elevation: Dp,
        clippingEnabled: Boolean,
        onSizeChanged: ((DpSize) -> Unit)?,
        platformMagnifierFactory: PlatformMagnifierFactory
    ) {
        val previousZoom = this.zoom
        val previousSize = this.size
        val previousCornerRadius = this.cornerRadius
        val previousElevation = this.elevation
        val previousClippingEnabled = this.clippingEnabled
        val previousPlatformMagnifierFactory = this.platformMagnifierFactory

        this.sourceCenter = sourceCenter
        this.magnifierCenter = magnifierCenter
        this.zoom = zoom
        this.useTextDefault = useTextDefault
        this.size = size
        this.cornerRadius = cornerRadius
        this.elevation = elevation
        this.clippingEnabled = clippingEnabled
        this.onSizeChanged = onSizeChanged
        this.platformMagnifierFactory = platformMagnifierFactory

        // On platforms >=Q, the zoom level can be updated dynamically on an existing magnifier, so
        // if the zoom changes between recompositions we don't need to recreate the magnifier. On
        // older platforms, the zoom can only be set initially, so we use the zoom itself as a key
        // so the magnifier gets recreated if it changes.
        if (
            magnifier == null ||
            (zoom != previousZoom && !platformMagnifierFactory.canUpdateZoom) ||
            size != previousSize ||
            cornerRadius != previousCornerRadius ||
            elevation != previousElevation ||
            clippingEnabled != previousClippingEnabled ||
            platformMagnifierFactory != previousPlatformMagnifierFactory
        ) {
            recreateMagnifier()
        }
        updateMagnifier()
    }

    override fun onAttach() {
        onObservedReadsChanged()
    }

    override fun onDetach() {
        magnifier?.dismiss()
        magnifier = null
    }

    override fun onObservedReadsChanged() {
        observeReads {
            val previousView = view
            val view = currentValueOf(LocalView).also { this.view = it }
            val previousDensity = density
            val density = currentValueOf(LocalDensity).also { this.density = it }

            if (magnifier == null || view != previousView || density != previousDensity) {
                recreateMagnifier()
            }

            updateMagnifier()
        }
    }

    private fun recreateMagnifier() {
        magnifier?.dismiss()
        val view = view ?: return
        val density = density ?: return
        magnifier = platformMagnifierFactory.create(
            view = view,
            useTextDefault = useTextDefault,
            size = size,
            cornerRadius = cornerRadius,
            elevation = elevation,
            clippingEnabled = clippingEnabled,
            density = density,
            initialZoom = zoom
        )
        updateSizeIfNecessary()
    }

    private fun updateMagnifier() {
        val magnifier = magnifier ?: return
        val density = density ?: return

        val sourceCenterOffset = sourceCenter(density)
        sourceCenterInRoot =
            if (anchorPositionInRoot.isSpecified && sourceCenterOffset.isSpecified) {
                anchorPositionInRoot + sourceCenterOffset
            } else {
                Offset.Unspecified
            }

        // Once the position is set, it's never null again, so we don't need to worry
        // about dismissing the magnifier if this expression changes value.
        if (sourceCenterInRoot.isSpecified) {
            magnifier.update(
                sourceCenter = sourceCenterInRoot,
                magnifierCenter = magnifierCenter(density).let {
                    if (it.isSpecified) {
                        anchorPositionInRoot + it
                    } else {
                        Offset.Unspecified
                    }
                },
                zoom = zoom
            )
            updateSizeIfNecessary()
        } else {
            // Can't place the magnifier at an unspecified location, so just hide it.
            magnifier.dismiss()
        }
    }

    private fun updateSizeIfNecessary() {
        val magnifier = magnifier ?: return
        val density = density ?: return

        if (magnifier.size != previousSize) {
            onSizeChanged?.invoke(with(density) { magnifier.size.toSize().toDpSize() })
            previousSize = magnifier.size
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        // don't update the magnifier immediately, actual frame draw happens right after all draw
        // commands are recorded. Magnifier update should happen in the next frame.
        coroutineScope.launch {
            withFrameMillis { }
            magnifier?.updateContent()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        // The mutable state must store the Offset, not the LocalCoordinates, because the same
        // LocalCoordinates instance may be sent to this callback multiple times, not implement
        // equals, or be stable, and so won't invalidate the snapshotFlow.
        anchorPositionInRoot = coordinates.positionInRoot()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this[MagnifierPositionInRoot] = { sourceCenterInRoot }
    }
}

@ChecksSdkIntAtLeast(api = 28)
internal fun isPlatformMagnifierSupported(sdkVersion: Int = Build.VERSION.SDK_INT) =
    sdkVersion >= 28
