/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
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
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.cinterop.useContents
import kotlinx.coroutines.launch
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPointMake
import platform.UIKit.UIView

/**
 * A function on elements that are magnified with a [magnifier] modifier that returns the position
 * of the center of the magnified content in the coordinate space of the window composable.
 */
internal val MagnifierPositionInWindow =
    SemanticsPropertyKey<() -> Offset>("MagnifierPositionInWindow")

internal fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: (Density.() -> Offset)? = null,
    color: Color = Color.Unspecified,
    onSizeChanged: ((DpSize) -> Unit)? = null,
): Modifier {
    return magnifier(
        sourceCenter = sourceCenter,
        magnifierCenter = magnifierCenter,
        onSizeChanged = onSizeChanged,
        color = color,
        platformMagnifierFactory = null
    )
}

internal fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: (Density.() -> Offset)? = null,
    onSizeChanged: ((DpSize) -> Unit)? = null,
    color: Color = Color.Unspecified,
    platformMagnifierFactory: PlatformMagnifierFactory? = null
): Modifier {
    return if (isPlatformMagnifierSupported()) {
        then(
            MagnifierElement(
                sourceCenter = sourceCenter,
                magnifierCenter = magnifierCenter,
                onSizeChanged = onSizeChanged,
                color = color,
                platformMagnifierFactory = platformMagnifierFactory
                    ?: PlatformMagnifierFactory.getForCurrentPlatform() // this doesn't do an alloc
            )
        )
    } else {
        inspectable(
            // Publish inspector info even if magnification isn't supported.
            inspectorInfo = debugInspectorInfo {
                name = "magnifier (not supported)"
                properties["sourceCenter"] = sourceCenter
                properties["magnifierCenter"] = magnifierCenter
                properties["color"] = color
            }
        ) { this }
    }
}

internal class MagnifierElement(
    private val sourceCenter: Density.() -> Offset,
    private val magnifierCenter: (Density.() -> Offset)? = null,
    private val onSizeChanged: ((DpSize) -> Unit)? = null,
    private val color : Color = Color.Unspecified,
    private val platformMagnifierFactory: PlatformMagnifierFactory
) : ModifierNodeElement<MagnifierNode>() {

    override fun create(): MagnifierNode {
        return MagnifierNode(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            onSizeChanged = onSizeChanged,
            platformMagnifierFactory = platformMagnifierFactory,
            color = color
        )
    }

    override fun update(node: MagnifierNode) {
        node.update(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            onSizeChanged = onSizeChanged,
            color = color,
            platformMagnifierFactory = platformMagnifierFactory
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MagnifierElement) return false

        if (sourceCenter != other.sourceCenter) return false
        if (magnifierCenter != other.magnifierCenter) return false
        if (onSizeChanged != other.onSizeChanged) return false
        if (color != other.color) return false
        if (platformMagnifierFactory != other.platformMagnifierFactory) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceCenter.hashCode()
        result = 31 * result + magnifierCenter.hashCode()
        result = 31 * result + (onSizeChanged?.hashCode() ?: 0)
        result = 31 * result + color.hashCode()
        result = 31 * result + platformMagnifierFactory.hashCode()
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "magnifier"
        properties["sourceCenter"] = sourceCenter
        properties["magnifierCenter"] = magnifierCenter
        properties["color"] = color
    }
}

internal class MagnifierNode(
    var sourceCenter: Density.() -> Offset,
    var magnifierCenter: (Density.() -> Offset)? = null,
    var onSizeChanged: ((DpSize) -> Unit)? = null,
    var color : Color = Color.Unspecified,
    var platformMagnifierFactory: PlatformMagnifierFactory =
        PlatformMagnifierFactory.getForCurrentPlatform()
) : Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    GlobalPositionAwareModifierNode,
    DrawModifierNode,
    SemanticsModifierNode,
    ObserverModifierNode {

    /**
     * iOS [UIView] that this modifier node is attached to in Compose hierarchy.
     */
    private var view: UIView? = null

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
     * Anchor Composable's position in window layout.
     */
    private var anchorPositionInWindow: Offset by mutableStateOf(Offset.Unspecified)

    /**
     * Position where [sourceCenter] is mapped on window layout. This is passed to platform
     * magnifier to precisely target the requested location.
     */
    private var sourceCenterInWindow: Offset = Offset.Unspecified

    /**
     * Last reported size to [onSizeChanged]. This is compared to the current size before calling
     * the lambda again.
     */
    private var previousSize: IntSize? = null

    fun update(
        sourceCenter: Density.() -> Offset,
        magnifierCenter: (Density.() -> Offset)?,
        onSizeChanged: ((DpSize) -> Unit)?,
        color: Color,
        platformMagnifierFactory: PlatformMagnifierFactory
    ) {
        val previousPlatformMagnifierFactory = this.platformMagnifierFactory
        val previousColor = this.color

        this.sourceCenter = sourceCenter
        this.magnifierCenter = magnifierCenter
        this.onSizeChanged = onSizeChanged
        this.color = color
        this.platformMagnifierFactory = platformMagnifierFactory

        if (
            magnifier == null ||
            color != previousColor ||
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

            val view = kotlin.runCatching {
                currentValueOf(LocalUIViewController).view.also { this.view = it }
            }.getOrElse {
                // LocalUIViewController is not provided for unit tests - do nothing
                return@observeReads
            }
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
            density = density,
            color = color
        )
        updateSizeIfNecessary()
    }

    private fun updateMagnifier() {
        val magnifier = magnifier ?: return
        val density = density ?: return

        val sourceCenterOffset = sourceCenter(density)
        sourceCenterInWindow =
            if (anchorPositionInWindow.isSpecified && sourceCenterOffset.isSpecified) {
                anchorPositionInWindow + sourceCenterOffset
            } else {
                Offset.Unspecified
            }

        val sourceCenterInView = view?.window?.takeIf {
            sourceCenterInWindow.isSpecified
        }?.let { window ->
            view!!.convertPoint(
                CGPointMake(
                    sourceCenterInWindow.x.toDouble() / density.density,
                    sourceCenterInWindow.y.toDouble() / density.density
                ),
                fromCoordinateSpace = window.coordinateSpace()
            ).useContents {
                // HACK: Applying additional offset to adjust magnifier location
                // when platform layers are disabled.
                val additionalViewOffsetInWindow = view!!.layer.affineTransform().useContents {
                    Offset(tx.toFloat(), ty.toFloat()) * density.density
                }
                Offset(x.toFloat(), y.toFloat()) * density.density + additionalViewOffsetInWindow
            }
        }

        // Once the position is set, it's never null again, so we don't need to worry
        // about dismissing the magnifier if this expression changes value.
        if (sourceCenterInView != null) {
            // Calculate magnifier center if it's provided. Only accept if the returned value is
            // specified. Then add [anchorPositionInWindow] for relative positioning.
            magnifierCenter?.invoke(density)
                ?.takeIf { it.isSpecified }
                ?.let { anchorPositionInWindow + it }

            magnifier.update(sourceCenter = sourceCenterInView)
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
        anchorPositionInWindow = coordinates.positionInWindow()
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        this[MagnifierPositionInWindow] = { sourceCenterInWindow }
    }
}

internal fun isPlatformMagnifierSupported() =
    available(OS.Ios to OSVersion(major = 17))
