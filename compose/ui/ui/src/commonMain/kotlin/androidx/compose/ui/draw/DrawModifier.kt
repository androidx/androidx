/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.draw

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
@JvmDefaultWithCompatibility
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects
 * to be referenced across draw calls
 */
@JvmDefaultWithCompatibility
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls.
     * This is useful to conditionally recreate objects only if the size of the
     * drawing environment changes, or if state parameters that are inputs
     * to objects change. This method is guaranteed to be called before
     * [DrawModifier.draw].
     *
     * @param params The params to be used to build the cache.
     */
    fun onBuildCache(params: BuildDrawCacheParams)
}

/**
 * The set of parameters which could be used to build the drawing cache.
 *
 * @see DrawCacheModifier.onBuildCache
 */
interface BuildDrawCacheParams {
    /**
     * The current size of the drawing environment
     */
    val size: Size

    /**
     * The current layout direction.
     */
    val layoutDirection: LayoutDirection

    /**
     * The current screen density to provide the ability to convert between
     */
    val density: Density
}

/**
 * Draw into a [Canvas] behind the modified content.
 */
fun Modifier.drawBehind(
    onDraw: DrawScope.() -> Unit
) = this then DrawBehindElement(onDraw)

@OptIn(ExperimentalComposeUiApi::class)
private data class DrawBehindElement(
    val onDraw: DrawScope.() -> Unit
) : ModifierNodeElement<DrawBackgroundModifier>() {
    override fun create() = DrawBackgroundModifier(onDraw)

    override fun update(node: DrawBackgroundModifier) = node.apply {
        onDraw = this@DrawBehindElement.onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBehind"
        properties["onDraw"] = onDraw
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class DrawBackgroundModifier(
    var onDraw: DrawScope.() -> Unit
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
        drawContent()
    }
}

/**
 * Draw into a [DrawScope] with content that is persisted across
 * draw calls as long as the size of the drawing area is the same or
 * any state objects that are read have not changed. In the event that
 * the drawing area changes, or the underlying state values that are being read
 * change, this method is invoked again to recreate objects to be used during drawing
 *
 * For example, a [androidx.compose.ui.graphics.LinearGradient] that is to occupy the full
 * bounds of the drawing area can be created once the size has been defined and referenced
 * for subsequent draw calls without having to re-allocate.
 *
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierSample
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierStateParameterSample
 * @sample androidx.compose.ui.samples.DrawWithCacheContentSample
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.drawWithCache(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
) = this then DrawWithCacheElement(onBuildDrawCache)

private data class DrawWithCacheElement(
    val onBuildDrawCache: CacheDrawScope.() -> DrawResult
) : ModifierNodeElement<CacheDrawNode>() {
    override fun create(): CacheDrawNode {
        return CacheDrawNode(CacheDrawScope(), onBuildDrawCache)
    }

    override fun update(node: CacheDrawNode) = node.apply {
        block = onBuildDrawCache
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithCache"
        properties["onBuildDrawCache"] = onBuildDrawCache
    }
}

private class CacheDrawNode(
    private val cacheDrawScope: CacheDrawScope,
    block: CacheDrawScope.() -> DrawResult
) : Modifier.Node(), DrawModifierNode, ObserverNode, BuildDrawCacheParams {

    private var isCacheValid = false
    var block: CacheDrawScope.() -> DrawResult = block
        set(value) {
            field = value
            invalidateDrawCache()
        }

    init {
        cacheDrawScope.cacheParams = this
    }

    override val density: Density get() = requireDensity()
    override val layoutDirection: LayoutDirection get() = requireLayoutDirection()
    override val size: Size get() = requireCoordinator(Nodes.LayoutAware).size.toSize()

    override fun onMeasureResultChanged() {
        invalidateDrawCache()
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    private fun invalidateDrawCache() {
        isCacheValid = false
        cacheDrawScope.drawResult = null
        invalidateDraw()
    }

    private fun getOrBuildCachedDrawBlock(): DrawResult {
        if (!isCacheValid) {
            cacheDrawScope.apply {
                drawResult = null
                observeReads { block() }
                checkNotNull(drawResult) {
                    "DrawResult not defined, did you forget to call onDraw?"
                }
            }
            isCacheValid = true
        }
        return cacheDrawScope.drawResult!!
    }

    override fun ContentDrawScope.draw() {
        getOrBuildCachedDrawBlock().block(this)
    }
}

/**
 * Handle to a drawing environment that enables caching of content based on the resolved size.
 * Consumers define parameters and refer to them in the captured draw callback provided in
 * [onDrawBehind] or [onDrawWithContent].
 *
 * [onDrawBehind] will draw behind the layout's drawing contents however, [onDrawWithContent] will
 * provide the ability to draw before or after the layout's contents
 */
class CacheDrawScope internal constructor() : Density {
    internal var cacheParams: BuildDrawCacheParams = EmptyBuildDrawCacheParams
    internal var drawResult: DrawResult? = null

    /**
     * Provides the dimensions of the current drawing environment
     */
    val size: Size get() = cacheParams.size

    /**
     * Provides the [LayoutDirection].
     */
    val layoutDirection: LayoutDirection get() = cacheParams.layoutDirection

    /**
     * Issue drawing commands to be executed before the layout content is drawn
     */
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = onDrawWithContent {
        block()
        drawContent()
    }

    /**
     * Issue drawing commands before or after the layout's drawing contents
     */
    fun onDrawWithContent(block: ContentDrawScope.() -> Unit): DrawResult {
        return DrawResult(block).also { drawResult = it }
    }

    override val density: Float
        get() = cacheParams.density.density

    override val fontScale: Float
        get() = cacheParams.density.fontScale
}

private object EmptyBuildDrawCacheParams : BuildDrawCacheParams {
    override val size: Size = Size.Unspecified
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val density: Density = Density(1f, 1f)
}

/**
 * Holder to a callback to be invoked during draw operations. This lambda
 * captures and reuses parameters defined within the CacheDrawScope receiver scope lambda.
 */
class DrawResult internal constructor(internal var block: ContentDrawScope.() -> Unit)

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this then DrawWithContentElement(onDraw)

@OptIn(ExperimentalComposeUiApi::class)
private data class DrawWithContentElement(
    val onDraw: ContentDrawScope.() -> Unit
) : ModifierNodeElement<DrawWithContentModifier>() {
    override fun create() = DrawWithContentModifier(onDraw)

    override fun update(node: DrawWithContentModifier) = node.apply {
        onDraw = this@DrawWithContentElement.onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithContent"
        properties["onDraw"] = onDraw
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DrawWithContentModifier(
    var onDraw: ContentDrawScope.() -> Unit
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
    }
}
