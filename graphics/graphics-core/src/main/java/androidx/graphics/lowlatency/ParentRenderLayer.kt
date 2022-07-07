/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.lowlatency

import androidx.graphics.opengl.GLRenderer
import androidx.graphics.surface.SurfaceControlCompat

/**
 * Interface used to define a parent for rendering front and double buffered layers.
 * This provides the following facilities:
 *
 * 1) Specifying a parent [SurfaceControlCompat] for a front buffered layer
 * 2) Creating a [GLRenderer.RenderTarget] for rendering double buffered layer
 * 3) Providing callbacks for consumers to know when to recreate dependencies based on
 * the size/state of the parent, as well as allowing consumers to provide parameters
 * to implementations of front/double buffered layers
 */
internal interface ParentRenderLayer<T> {
    /**
     * Modify the provided [SurfaceControlCompat.Transaction] to reparent the provided
     * child [SurfaceControlCompat] to a [SurfaceControlCompat] provided by the parent rendering
     * layer
     */
    fun buildReparentTransaction(
        child: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction,
    )

    /**
     * Create a [GLRenderer.RenderTarget] instance for the parent rendering layer given
     * a [GLRenderer] and corresponding [GLRenderer.RenderCallback]
     */
    fun createRenderTarget(
        renderer: GLRenderer,
        renderLayerCallback: GLFrontBufferedRenderer.Callback<T>
    ): GLRenderer.RenderTarget

    /**
     * Configure the callbacks on this [ParentRenderLayer] instance
     * @param callback [Callback] specified on [ParentRenderLayer]. This can be null to remove
     * the previously set [Callback]
     */
    fun setParentLayerCallbacks(callback: Callback<T>?)

    /**
     * Clear the contents of the parent buffer. This triggers a call to
     * [GLFrontBufferedRenderer.Callback.onDoubleBufferedLayerRenderComplete] to update the
     * buffer shown for the dry layer as well as hides the front buffered layer.
     */
    fun clear()

    /**
     * Release all resources associated with this [ParentRenderLayer] instance
     */
    fun release(transaction: SurfaceControlCompat.Transaction)

    /**
     * Callbacks to be implemented by the consumer of [ParentRenderLayer] to be alerted
     * of size changes or if the [ParentRenderLayer] is destroyed as well as providing a mechanism
     * to expose parameters for rendering front/double buffered layers
     */
    interface Callback<T> {
        /**
         * Callback invoked whenever the size of the [ParentRenderLayer] changes.
         * Consumers can leverage this to initialize appropriate buffer sizes and
         * [SurfaceControlCompat] instances
         */
        fun onSizeChanged(width: Int, height: Int)

        /**
         * Callback invoked when the [ParentRenderLayer] is destroyed. This can be in response
         * to the corresponding View backing the [ParentRenderLayer] is being detached/removed
         * from the View hierarchy
         */
        fun onLayerDestroyed()

        /**
         * Callback invoked by the [ParentRenderLayer] to query the parameters since the last
         * render to the dry layer. This includes all parameters to each request to render content
         * to the front buffered layer since the last time the dry layer was re-rendered.
         * This is useful for recreating the entire scene when front buffered layer contents are to
         * be committed, that is the entire scene is re-rendered into the double buffered layer.
         */
        fun obtainDoubleBufferedLayerParams(): MutableCollection<T>

        /**
         * Obtain a handle to the front buffered layer [SurfaceControlCompat] to be used in
         * transactions to atomically update double buffered layer content as well as hiding the
         * visibility of the front buffered layer
         */
        fun getFrontBufferedLayerSurfaceControl(): SurfaceControlCompat?

        /**
         * Obtain a handle to the [RenderBufferPool] to get [RenderBuffer] instances for
         * rendering to front and double buffered layers
         */
        fun getRenderBufferPool(): RenderBufferPool?
    }
}