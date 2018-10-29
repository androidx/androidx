package androidx.ui.compositing

import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.flow.layers.LayerBuilder
import androidx.ui.painting.BlendMode
import androidx.ui.painting.Color
import androidx.ui.painting.Path
import androidx.ui.painting.Picture
import androidx.ui.painting.Shader
import androidx.ui.skia.SkMatrix
import androidx.ui.vectormath64.Matrix4

/**
 * Builds a [Scene] containing the given visuals.
 *
 * A [Scene] can then be rendered using [Window.render].
 *
 * To draw graphical operations onto a [Scene], first create a
 * [Picture] using a [PictureRecorder] and a [Canvas], and then add
 * it to the scene using [addPicture]
 */
class SceneBuilder {

    private val layer_builder_ = LayerBuilder.Create()

    /**
     * Pushes a transform operation onto the operation stack.
     *
     * The objects are transformed by the given matrix before rasterization.
     *
     * See [pop] for details about the operation stack.
     */
    // TODO(Migration/Andrey): Changed type from Float64List to Matrix4
    fun pushTransform(matrix4: Matrix4) {
        // TODO(Migration/Andrey): it's Matrix4, so don't need this check
//        if (matrix4.length != 16)
//            throw new ArgumentError('"matrix4" must have 16 entries.');
        _pushTransform(matrix4)
    }

    // TODO(Migration/Andrey): Changed type from Float64List to Matrix4
    private fun _pushTransform(matrix4: Matrix4) {
        layer_builder_.PushTransform(SkMatrix(matrix4))
    }

    /**
     * Pushes a rectangular clip operation onto the operation stack.
     *
     * Rasterization outside the given rectangle is discarded.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushClipRect(rect: Rect) {
        _pushClipRect(rect.left, rect.right, rect.top, rect.bottom)
    }

    private fun _pushClipRect(
        left: Double,
        right: Double,
        top: Double,
        bottom: Double
    ) {
        TODO()
//        layer_builder_->PushClipRect(SkRect::MakeLTRB(left, top, right, bottom),
//        static_cast<flow::Clip>(clipBehavior));
    }

    /**
     * Pushes a rounded-rectangular clip operation onto the operation stack.
     *
     * Rasterization outside the given rounded rectangle is discarded.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushClipRRect(rrect: RRect) = _pushClipRRect(rrect)

    // TODO(Migration/Andrey): Changed type from Float32List to RRect
    private fun _pushClipRRect(rrect: RRect) {
        TODO()
//        layer_builder_ -> PushClipRoundedRect(rrect.sk_rrect,
//                    static_cast < ???({ flow.Clip() }) > (clipBehavior))
    }

    /**
     * Pushes a path clip operation onto the operation stack.
     *
     * Rasterization outside the given path is discarded.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushClipPath(path: Path) {
        TODO()
//        layer_builder_->PushClipPath(path->path(),
//        static_cast<flow::Clip>(clipBehavior));
    }

    /**
     * Pushes an opacity operation onto the operation stack.
     *
     * The given alpha value is blended into the alpha value of the objects'
     * rasterization. An alpha value of 0 makes the objects entirely invisible.
     * An alpha value of 255 has no effect (i.e., the objects retain the current
     * opacity).
     *
     * See [pop] for details about the operation stack.
     */
    fun pushOpacity(alpha: Int) {
        TODO()
//        layer_builder_->PushOpacity(alpha);
    }

    /**
     * Pushes a color filter operation onto the operation stack.
     *
     * The given color is applied to the objects' rasterization using the given
     * blend mode.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushColorFilter(color: Color, blendMode: BlendMode) {
        _pushColorFilter(color.value, blendMode.ordinal)
    }

    private fun _pushColorFilter(color: Int, blendMode: Int) {
        TODO()
//        layer_builder_ -> PushColorFilter(static_cast<SkColor>(color),
//                    static_cast<SkBlendMode>(blendMode))
    }

    /**
     * Pushes a backdrop filter operation onto the operation stack.
     *
     * The given filter is applied to the current contents of the scene prior to
     * rasterizing the given objects.
     *
     * See [pop] for details about the operation stack.
     */
    // TODO(Migration/Andrey) needs ImageFilter
//    fun pushBackdropFilter(filter: ImageFilter) {
//        layer_builder_->PushBackdropFilter(filter->filter());
//    }

    /**
     * Pushes a shader mask operation onto the operation stack.
     *
     * The given shader is applied to the object's rasterization in the given
     * rectangle using the given blend mode.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushShaderMask(shader: Shader, maskRect: Rect, blendMode: BlendMode) {
        _pushShaderMask(shader,
                maskRect.left,
                maskRect.right,
                maskRect.top,
                maskRect.bottom,
                blendMode.ordinal)
    }

    private fun _pushShaderMask(
        shader: Shader,
        maskRectLeft: Double,
        maskRectRight: Double,
        maskRectTop: Double,
        maskRectBottom: Double,
        blendMode: Int
    ) {
        TODO()
//        layer_builder_->PushShaderMask(
//        shader->shader(),
//        SkRect::MakeLTRB(maskRectLeft, maskRectTop, maskRectRight,
//                maskRectBottom),
//        static_cast<SkBlendMode>(blendMode));
    }

    /**
     * Pushes a physical layer operation for an arbitrary shape onto the
     * operation stack.
     *
     * Rasterization will be clipped to the given shape defined by [path]. If
     * [elevation] is greater than 0.0, then a shadow is drawn around the layer.
     * [shadowColor] defines the color of the shadow if present and [color] defines the
     * color of the layer background.
     *
     * See [pop] for details about the operation stack.
     */
    fun pushPhysicalShape(
        path: Path,
        elevation: Double = 0.0,
        color: Color,
        shadowColor: Color? = null
    ) {
        _pushPhysicalShape(path,
                elevation,
                color.value,
                shadowColor?.value ?: 0xFF000000.toInt()
        )
    }

    private fun _pushPhysicalShape(path: Path, elevation: Double, color: Int, shadowColor: Int) {
        TODO()
//        layer_builder_->PushPhysicalShape(
//        path->path(),                 //
//        elevation,                    //
//        static_cast<SkColor>(color),  //
//        static_cast<SkColor>(shadow_color),
//        UIDartState::Current()->window()->viewport_metrics().device_pixel_ratio,
//        static_cast<flow::Clip>(clip_behavior));
    }

    /**
     * Ends the effect of the most recently pushed operation.
     *
     * Internally the scene builder maintains a stack of operations. Each of the
     * operations in the stack applies to each of the objects added to the scene.
     * Calling this function removes the most recently added operation from the
     * stack.
     */
    fun pop() {
        layer_builder_.Pop()
    }

    /**
     * Adds an object to the scene that displays performance statistics.
     *
     * Useful during development to assess the performance of the application.
     * The enabledOptions controls which statistics are displayed. The bounds
     * controls where the statistics are displayed.
     *
     * enabledOptions is a bit field with the following bits defined:
     *  - 0x01: displayRasterizerStatistics - show GPU thread frame time
     *  - 0x02: visualizeRasterizerStatistics - graph GPU thread frame times
     *  - 0x04: displayEngineStatistics - show UI thread frame time
     *  - 0x08: visualizeEngineStatistics - graph UI thread frame times
     * Set enabledOptions to 0x0F to enable all the currently defined features.
     *
     * The "UI thread" is the thread that includes all the execution of
     * the main Dart isolate (the isolate that can call
     * [Window.render]). The UI thread frame time is the total time
     * spent executing the [Window.onBeginFrame] callback. The "GPU
     * thread" is the thread (running on the CPU) that subsequently
     * processes the [Scene] provided by the Dart code to turn it into
     * GPU commands and send it to the GPU.
     *
     * See also the [PerformanceOverlayOption] enum in the rendering library.
     * for more details.
     */
    // Values above must match constants in //engine/src/sky/compositor/performance_overlay_layer.h
    fun addPerformanceOverlay(enabledOptions: Int, bounds: Rect) {
        _addPerformanceOverlay(enabledOptions,
                bounds.left,
                bounds.right,
                bounds.top,
                bounds.bottom)
    }

    private fun _addPerformanceOverlay(
        enabledOptions: Int,
        left: Double,
        right: Double,
        top: Double,
        bottom: Double
    ) {
        TODO()
//        layer_builder_->PushPerformanceOverlay(
//        enabledOptions, SkRect::MakeLTRB(left, top, right, bottom));
    }

    /**
     * Adds a [Picture] to the scene.
     *
     * The picture is rasterized at the given offset.
     */
    fun addPicture(
        offset: Offset,
        picture: Picture,
        isComplexHint: Boolean = false,
        willChangeHint: Boolean = false
    ) {
        layer_builder_.PushPicture(
                offset,
                picture,
                isComplexHint,
                willChangeHint
        )
    }

    /**
     * Adds a backend texture to the scene.
     *
     * The texture is scaled to the given size and rasterized at the given offset.
     */
    fun addTexture(
        textureId: Int,
        offset: Offset = Offset.zero,
        width: Double = 0.0,
        height: Double = 0.0
    ) {
        _addTexture(offset.dx, offset.dy, width, height, textureId)
    }

    private fun _addTexture(
        dx: Double,
        dy: Double,
        width: Double,
        height: Double,
        textureId: Int
    ) {
        TODO()
//        layer_builder_->PushTexture(SkPoint::Make(dx, dy),
//        SkSize::Make(width, height), textureId, freeze);
    }

    /**
     * (Fuchsia-only) Adds a scene rendered by another application to the scene
     * for this application.
     */
    // TODO(Migration/Andrey) needs SceneHost
//    fun addChildScene(
//            offset: Offset = Offset.zero,
//            width: Double = 0.0,
//            height: Double = 0.0,
//        sceneHost: SceneHost? = null,
//        hitTestable : Boolean = true
//    ) {
//        _addChildScene(offset.dx,
//                offset.dy,
//                width,
//                height,
//                sceneHost,
//                hitTestable)
//    }
//
//    private fun _addChildScene(
//            dx: Double,
//            dy: Double,
//            width: Double,
//            height: Double,
//            sceneHost: SceneHost?,
//            hitTestable : Boolean) {
//        #if defined(OS_FUCHSIA)
//        layer_builder_->PushChildScene(SkPoint::Make(dx, dy),            //
//        SkSize::Make(width, height),      //
//        sceneHost->export_node_holder(),  //
//        hitTestable);
//        #endif  // defined(OS_FUCHSIA)
//    }

    /**
     * Sets a threshold after which additional debugging information should be recorded.
     *
     * Currently this interface is difficult to use by end-developers. If you're
     * interested in using this feature, please contact [flutter-dev](https://groups.google.com/forum/#!forum/flutter-dev).
     * We'll hopefully be able to figure out how to make this feature more useful
     * to you.
     */
    fun setRasterizerTracingThreshold(frameInterval: Int) {
        layer_builder_.rasterizer_tracing_threshold = frameInterval
    }

    /**
     * Sets whether the raster cache should checkerboard cached entries. This is
     * only useful for debugging purposes.
     *
     * The compositor can sometimes decide to cache certain portions of the
     * widget hierarchy. Such portions typically don't change often from frame to
     * frame and are expensive to render. This can speed up overall rendering. However,
     * there is certain upfront cost to constructing these cache entries. And, if
     * the cache entries are not used very often, this cost may not be worth the
     * speedup in rendering of subsequent frames. If the developer wants to be certain
     * that populating the raster cache is not causing stutters, this option can be
     * set. Depending on the observations made, hints can be provided to the compositor
     * that aid it in making better decisions about caching.
     *
     * Currently this interface is difficult to use by end-developers. If you're
     * interested in using this feature, please contact [flutter-dev](https://groups.google.com/forum/#!forum/flutter-dev).
     */
    fun setCheckerboardRasterCacheImages(checkerboard: Boolean) {
        layer_builder_.checkerboard_raster_cache_images = checkerboard
    }

    /**
     * Sets whether the compositor should checkerboard layers that are rendered
     * to offscreen bitmaps.
     *
     * This is only useful for debugging purposes.
     */
    fun setCheckerboardOffscreenLayers(checkerboard: Boolean) {
        layer_builder_.checkerboard_offscreen_layers = checkerboard
    }

    /**
     * Finishes building the scene.
     *
     * Returns a [Scene] containing the objects that have been added to
     * this scene builder. The [Scene] can then be displayed on the
     * screen with [Window.render].
     *
     * After calling this function, the scene builder object is invalid and
     * cannot be used further.
     */
    fun build(): Scene {
        val scene = Scene.create(layer_builder_.TakeLayer(),
                layer_builder_.rasterizer_tracing_threshold,
                layer_builder_.checkerboard_raster_cache_images,
                layer_builder_.checkerboard_offscreen_layers)
//        ClearDartWrapper()
        return scene
    }
}