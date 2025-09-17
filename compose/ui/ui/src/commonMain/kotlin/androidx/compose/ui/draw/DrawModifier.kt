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

import androidx.collection.MutableObjectList
import androidx.collection.mutableObjectListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.shadow.ShadowContext
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.checkPreconditionNotNull
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireCoordinator
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.node.requireLayoutDirection
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntSize
import androidx.compose.ui.unit.toSize

/** A [Modifier.Element] that draws into the space of the layout. */
@JvmDefaultWithCompatibility
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects to be referenced across
 * draw calls
 */
@JvmDefaultWithCompatibility
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls. This is useful to
     * conditionally recreate objects only if the size of the drawing environment changes, or if
     * state parameters that are inputs to objects change. This method is guaranteed to be called
     * before [DrawModifier.draw].
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
    /** The current size of the drawing environment */
    val size: Size

    /** The current layout direction. */
    val layoutDirection: LayoutDirection

    /** The current screen density to provide the ability to convert between */
    val density: Density
}

/** Draw into a [Canvas] behind the modified content. */
fun Modifier.drawBehind(onDraw: DrawScope.() -> Unit) = this then DrawBehindElement(onDraw)

private class DrawBehindElement(val onDraw: DrawScope.() -> Unit) :
    ModifierNodeElement<DrawBackgroundModifier>() {
    override fun create() = DrawBackgroundModifier(onDraw)

    override fun update(node: DrawBackgroundModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBehind"
        properties["onDraw"] = onDraw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawBehindElement) return false

        if (onDraw !== other.onDraw) return false

        return true
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
    }
}

internal class DrawBackgroundModifier(var onDraw: DrawScope.() -> Unit) :
    Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
        drawContent()
    }
}

/**
 * Draw into a [DrawScope] with content that is persisted across draw calls as long as the size of
 * the drawing area is the same or any state objects that are read have not changed. In the event
 * that the drawing area changes, or the underlying state values that are being read change, this
 * method is invoked again to recreate objects to be used during drawing
 *
 * For example, a [androidx.compose.ui.graphics.LinearGradient] that is to occupy the full bounds of
 * the drawing area can be created once the size has been defined and referenced for subsequent draw
 * calls without having to re-allocate.
 *
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierSample
 * @sample androidx.compose.ui.samples.DrawWithCacheModifierStateParameterSample
 * @sample androidx.compose.ui.samples.DrawWithCacheContentSample
 */
fun Modifier.drawWithCache(onBuildDrawCache: CacheDrawScope.() -> DrawResult) =
    this then DrawWithCacheElement(onBuildDrawCache)

private class DrawWithCacheElement(val onBuildDrawCache: CacheDrawScope.() -> DrawResult) :
    ModifierNodeElement<CacheDrawModifierNodeImpl>() {
    override fun create(): CacheDrawModifierNodeImpl {
        return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
    }

    override fun update(node: CacheDrawModifierNodeImpl) {
        node.block = onBuildDrawCache
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithCache"
        properties["onBuildDrawCache"] = onBuildDrawCache
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawWithCacheElement) return false

        if (onBuildDrawCache !== other.onBuildDrawCache) return false

        return true
    }

    override fun hashCode(): Int {
        return onBuildDrawCache.hashCode()
    }
}

fun CacheDrawModifierNode(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
): CacheDrawModifierNode {
    return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
}

/**
 * Expands on the [androidx.compose.ui.node.DrawModifierNode] by adding the ability to invalidate
 * the draw cache for changes in things like shapes and bitmaps (see Modifier.border for a usage
 * examples).
 */
sealed interface CacheDrawModifierNode : DrawModifierNode {
    fun invalidateDrawCache()
}

/**
 * Wrapper [GraphicsContext] implementation that maintains a list of the [GraphicsLayer] instances
 * that were created through this instance so it can release only those [GraphicsLayer]s when it is
 * disposed of within the corresponding Modifier is disposed
 */
private class ScopedGraphicsContext : GraphicsContext {

    private var allocatedGraphicsLayers: MutableObjectList<GraphicsLayer>? = null

    var graphicsContext: GraphicsContext? = null
        set(value) {
            releaseGraphicsLayers()
            field = value
        }

    override fun createGraphicsLayer(): GraphicsLayer {
        val gContext = graphicsContext
        checkPrecondition(gContext != null) { "GraphicsContext not provided" }
        val layer = gContext.createGraphicsLayer()
        val layers = allocatedGraphicsLayers
        if (layers == null) {
            mutableObjectListOf(layer).also { allocatedGraphicsLayers = it }
        } else {
            layers.add(layer)
        }

        return layer
    }

    override fun releaseGraphicsLayer(layer: GraphicsLayer) {
        graphicsContext?.releaseGraphicsLayer(layer)
    }

    override val shadowContext: ShadowContext
        get() {
            val gContext = graphicsContext
            checkPrecondition(gContext != null) { "GraphicsContext not provided" }
            return gContext.shadowContext
        }

    fun releaseGraphicsLayers() {
        allocatedGraphicsLayers?.let { layers ->
            layers.forEach { layer -> releaseGraphicsLayer(layer) }
            layers.clear()
        }
    }
}

private class CacheDrawModifierNodeImpl(
    private val cacheDrawScope: CacheDrawScope,
    block: CacheDrawScope.() -> DrawResult,
) : Modifier.Node(), CacheDrawModifierNode, ObserverModifierNode, BuildDrawCacheParams {

    private var isCacheValid = false
    private var cachedGraphicsContext: ScopedGraphicsContext? = null

    var block: CacheDrawScope.() -> DrawResult = block
        set(value) {
            field = value
            invalidateDrawCache()
        }

    init {
        cacheDrawScope.cacheParams = this
        cacheDrawScope.graphicsContextProvider = { graphicsContext }
    }

    override val density: Density
        get() = requireDensity()

    override val layoutDirection: LayoutDirection
        get() = requireLayoutDirection()

    override val size: Size
        get() = requireCoordinator(Nodes.Draw).size.toSize()

    val graphicsContext: GraphicsContext
        get() {
            var localGraphicsContext = cachedGraphicsContext
            if (localGraphicsContext == null) {
                localGraphicsContext = ScopedGraphicsContext().also { cachedGraphicsContext = it }
            }
            if (localGraphicsContext.graphicsContext == null) {
                localGraphicsContext.graphicsContext = requireGraphicsContext()
            }
            return localGraphicsContext
        }

    override fun onDetach() {
        super.onDetach()
        cachedGraphicsContext?.releaseGraphicsLayers()
    }

    override fun onReset() {
        super.onReset()
        invalidateDrawCache()
    }

    override fun onMeasureResultChanged() {
        invalidateDrawCache()
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    override fun invalidateDrawCache() {
        // Release all previously allocated graphics layers to the recycling pool
        // if a layer is needed in a subsequent draw, it will be obtained from the pool again and
        // reused
        cachedGraphicsContext?.releaseGraphicsLayers()
        isCacheValid = false
        cacheDrawScope.drawResult = null
        invalidateDraw()
    }

    override fun onDensityChange() {
        invalidateDrawCache()
    }

    override fun onLayoutDirectionChange() {
        invalidateDrawCache()
    }

    private fun getOrBuildCachedDrawBlock(contentDrawScope: ContentDrawScope): DrawResult {
        if (!isCacheValid) {
            cacheDrawScope.apply {
                drawResult = null
                this.contentDrawScope = contentDrawScope
                observeReads { block() }
                checkPreconditionNotNull(drawResult) {
                    "DrawResult not defined, did you forget to call onDraw?"
                }
            }
            isCacheValid = true
        }
        return cacheDrawScope.drawResult!!
    }

    override fun ContentDrawScope.draw() {
        getOrBuildCachedDrawBlock(this).block(this)
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
    internal var contentDrawScope: ContentDrawScope? = null
    internal var graphicsContextProvider: (() -> GraphicsContext)? = null

    /** Provides the dimensions of the current drawing environment */
    val size: Size
        get() = cacheParams.size

    /** Provides the [LayoutDirection]. */
    val layoutDirection: LayoutDirection
        get() = cacheParams.layoutDirection

    /**
     * Returns a managed [GraphicsLayer] instance. This [GraphicsLayer] maybe newly created or
     * return a previously allocated instance. Consumers are not expected to release this instance
     * as it is automatically recycled upon invalidation of the CacheDrawScope and released when the
     * [DrawCacheModifier] is detached.
     */
    fun obtainGraphicsLayer(): GraphicsLayer =
        graphicsContextProvider!!.invoke().createGraphicsLayer()

    /**
     * Returns the [ShadowContext] used to create [InnerShadowPainter] and [DropShadowPainter] to
     * render inner and drop shadows respectively
     */
    fun obtainShadowContext(): ShadowContext = graphicsContextProvider!!.invoke().shadowContext

    /**
     * Record the drawing commands into the [GraphicsLayer] with the [Density], [LayoutDirection]
     * and [Size] are given from the provided [CacheDrawScope]
     */
    fun GraphicsLayer.record(
        density: Density = this@CacheDrawScope,
        layoutDirection: LayoutDirection = this@CacheDrawScope.layoutDirection,
        size: IntSize = this@CacheDrawScope.size.toIntSize(),
        block: ContentDrawScope.() -> Unit,
    ) {
        val scope = contentDrawScope!!
        with(scope) {
            val prevDensity = drawContext.density
            val prevLayoutDirection = drawContext.layoutDirection
            record(size) {
                drawContext.apply {
                    this.density = density
                    this.layoutDirection = layoutDirection
                }
                try {
                    block(scope)
                } finally {
                    drawContext.apply {
                        this.density = prevDensity
                        this.layoutDirection = prevLayoutDirection
                    }
                }
            }
        }
    }

    /** Issue drawing commands to be executed before the layout content is drawn */
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = onDrawWithContent {
        block()
        drawContent()
    }

    /** Issue drawing commands before or after the layout's drawing contents */
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
 * Holder to a callback to be invoked during draw operations. This lambda captures and reuses
 * parameters defined within the CacheDrawScope receiver scope lambda.
 */
class DrawResult internal constructor(internal var block: ContentDrawScope.() -> Unit)

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's contents.
 * It also allows the modifier to adjust the layout's canvas.
 */
fun Modifier.drawWithContent(onDraw: ContentDrawScope.() -> Unit): Modifier =
    this then DrawWithContentElement(onDraw)

private class DrawWithContentElement(val onDraw: ContentDrawScope.() -> Unit) :
    ModifierNodeElement<DrawWithContentModifier>() {
    override fun create() = DrawWithContentModifier(onDraw)

    override fun update(node: DrawWithContentModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithContent"
        properties["onDraw"] = onDraw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawWithContentElement) return false

        if (onDraw !== other.onDraw) return false

        return true
    }

    override fun hashCode(): Int {
        return onDraw.hashCode()
    }
}

private class DrawWithContentModifier(var onDraw: ContentDrawScope.() -> Unit) :
    Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
    }
}
