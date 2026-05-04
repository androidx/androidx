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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.awt.toAwtColor
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.SwingInteropViewHolder
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.event.FocusEvent
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy

val NoOpUpdate: Component.() -> Unit = {}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory] block will be called
 * to obtain the [Component] to be composed.
 *
 * By default, the Swing component is placed on top of the Compose layer (that means that Compose
 * content can't overlap or clip it). It might be changed by `compose.interop.blending` system
 * property. See [ComposeFeatureFlags.useInteropBlending].
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be re-executed to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Deprecated(
    "Use overload without `background` parameter. " +
        "Set the background manually in the factory method."
)
@Composable
fun <T : Component> SwingPanel(
    background: Color? = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {
    val interopContainer = LocalInteropContainer.current
    val compositeKeyHashCode = currentCompositeKeyHashCode
    val focusManager = LocalFocusManager.current

    // TODO: entire interop context must be inside SwingInteropViewHolder in order to
    //  expose a version of this API with `onReset` callback and integrated with ReusableComposeNode
    //  https://youtrack.jetbrains.com/issue/CMP-5897/Desktop-self-contained-InteropViewHolder

    val group = remember {
        SwingInteropViewGroup(
            key = compositeKeyHashCode,
            focusComponent = interopContainer.root
        )
    }

    // TODO(https://youtrack.jetbrains.com/issue/CMP-7557/SwingInteropViewHolder.-Commonization-of-focus-logic-with-different-targets)
    //  we probably can commonize this logic across different targets (including Android)
    val focusSwitcher = remember { InteropFocusSwitcher(group, focusManager) }

    val interopViewHolder = remember {
        SwingInteropViewHolder(
            factory = factory,
            container = interopContainer,
            group = group,
            focusSwitcher = focusSwitcher,
            compositeKeyHashCode = compositeKeyHashCode,
            measurePolicy = AwtContentMeasurePolicy(group)
        )
    }

    InteropView(
        factory = { interopViewHolder },
        modifier = modifier.then(focusSwitcher.modifier),
        update = {
            if (background != null) {
                it.background = background.toAwtColor()
            }
            update(it)
        }
    )
}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory] block will be called
 * to obtain the [Component] to be composed.
 *
 * By default, the Swing component is placed on top of the Compose layer (that means that Compose
 * content can't overlap or clip it). It might be changed by `compose.interop.blending` system
 * property. See [ComposeFeatureFlags.useInteropBlending].
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be re-executed to set the new properties.
 *
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
fun <T : Component> SwingPanel(
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {
    @Suppress("DEPRECATION")
    SwingPanel(
        background = null,
        factory = factory,
        modifier = modifier,
        update = update
    )
}

/**
 * Returns whether the event is handled by SwingPanel.
 *
 * The focus can be switched from the child component inside SwingPanel.
 * In that case, SwingPanel will take care of it.
 *
 * The alternative that needs more investigation is to
 * not use ComposePanel as next/previous focus element for SwingPanel children
 * (see [SwingInteropViewGroup.focusComponent])
 */
internal fun FocusEvent.isFocusGainedHandledBySwingPanel(container: Container) =
    container.isParentOf(oppositeComponent)

/**
 * A container for [SwingPanel]'s component. Takes care about focus and clipping.
 *
 * @param key The unique identifier for the panel container.
 * @param focusComponent The component that should receive focus.
 */
internal class SwingInteropViewGroup(
    key: CompositeKeyHashCode,
    private val focusComponent: Component,
) : JPanel() {

    internal var onInvalidate: (() -> Unit)? = null

    init {
        name = "SwingPanel #${key.toString(MaxSupportedRadix)}"
        layout = null
        focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
            override fun getComponentAfter(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                return if (aComponent == getLastComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentAfter(aContainer, aComponent)
                }
            }

            override fun getComponentBefore(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                return if (aComponent == getFirstComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentBefore(aContainer, aComponent)
                }
            }
        }
        isFocusCycleRoot = true
    }

    override fun getPreferredSize(): Dimension {
        return components[0].preferredSize
    }

    override fun getMinimumSize(): Dimension {
        return components[0].minimumSize
    }

    override fun getMaximumSize(): Dimension {
        return components[0].maximumSize
    }

    override fun invalidate() {
        super.invalidate()
        onInvalidate?.invoke()
    }
}

/**
 * The maximum radix available for conversion to and from strings.
 */
private const val MaxSupportedRadix = 36

private class AwtContentMeasurePolicy(
    val component: Component,
) : MeasurePolicy {

    private fun Density.awtToPx(awtPx: Int): Int = awtPx.dp.roundToPx()

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val prefSize = component.preferredSize
        val width = awtToPx(prefSize.width).coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = awtToPx(prefSize.height).coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {}
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return awtToPx(component.minimumSize.width)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return awtToPx(component.minimumSize.height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int
    ): Int {
        return awtToPx(component.maximumSize.width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int
    ): Int {
        return awtToPx(component.maximumSize.height)
    }
}