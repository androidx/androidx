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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore.spatial.core

import android.os.Handler
import android.os.Looper
import androidx.xr.scenecore.runtime.Dimensions
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.PanelEntity
import androidx.xr.scenecore.runtime.ResizableComponent
import androidx.xr.scenecore.runtime.ResizeEvent
import androidx.xr.scenecore.runtime.ResizeEventListener
import androidx.xr.scenecore.runtime.SurfaceEntity
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.function.Consumer
import com.android.extensions.xr.node.ReformEvent
import com.android.extensions.xr.node.ReformOptions
import com.android.extensions.xr.node.Vec3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.BiConsumer
import kotlin.math.max
import kotlin.math.min

/** Implementation of ResizableComponent. */
internal class ResizableComponentImpl(
    private val executor: ExecutorService,
    private val xrExtensions: XrExtensions,
    minSize: Dimensions,
    maxSize: Dimensions,
) : ResizableComponent {
    private val resizeEventListenerMap = ConcurrentHashMap<ResizeEventListener, Executor>()
    private val isContentHidden = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    // Visible for testing.
    var reformEventConsumer: Consumer<ReformEvent>? = null
    private var entity: Entity? = null

    private var currentMinSize = dimsClampPositive(minSize, DIMS_ZERO)
    override var minimumSize: Dimensions
        get() = currentMinSize
        set(value) {
            val minSize = dimsClampPositive(value, currentMinSize)
            var updateMin = false
            if (currentMinSize != minSize) {
                currentMinSize = minSize
                updateMin = true
            }

            var updateMax = false
            if (updateMin && dimsAnyLessThen(currentMaxSize, currentMinSize)) {
                currentMaxSize = dimsMax(currentMaxSize, currentMinSize)
                updateMax = true
            }

            if (entity == null) {
                return
            }

            if (updateMin) {
                val reformOptions = (entity as AndroidXrEntity).getReformOptions()
                reformOptions.minimumSize = dimsToVec3(currentMinSize)
                if (updateMax) {
                    reformOptions.maximumSize = dimsToVec3(currentMaxSize)
                }
                (entity as AndroidXrEntity).updateReformOptions()
            }
        }

    var currentMaxSize = dimsClampPositive(maxSize, DIMS_INF)
    override var maximumSize: Dimensions
        get() = currentMaxSize
        set(value) {
            val maxSize = dimsClampPositive(value, currentMaxSize)
            var updateMax = false
            if (currentMaxSize != maxSize) {
                currentMaxSize = maxSize
                updateMax = true
            }

            var updateMin = false
            if (updateMax && dimsAnyLessThen(currentMaxSize, currentMinSize)) {
                currentMinSize = dimsMin(currentMaxSize, currentMinSize)
                updateMin = true
            }

            if (entity == null) {
                return
            }

            if (updateMax) {
                val reformOptions = (entity as AndroidXrEntity).getReformOptions()
                reformOptions.maximumSize = dimsToVec3(currentMaxSize)
                if (updateMin) {
                    reformOptions.minimumSize = dimsToVec3(currentMinSize)
                }
                (entity as AndroidXrEntity).updateReformOptions()
            }
        }

    /**
     * The initializer sanitizes the provided minimum and maximum sizes to ensure they are valid.
     * Any negative values in `minSize` and `maxSize` are clamped to 0. Any `Float.NaN` values are
     * replaced with 0 for `minSize` and [Float.POSITIVE_INFINITY] for `maxSize`.
     *
     * Furthermore, it ensures that the maximum size is always greater than or equal to the minimum
     * size for each dimension. If any dimension of the provided `maxSize` is smaller than the
     * corresponding dimension of `minSize` after sanitization, that dimension of the maximum size
     * will be adjusted to be equal to the minimum size's dimension.
     */
    init {
        if (dimsAnyLessThen(currentMinSize, currentMaxSize)) {
            currentMaxSize = dimsMax(currentMinSize, currentMaxSize)
        }
    }

    private var currentSize = DIMS_ONE

    override var size: Dimensions
        get() = currentSize
        set(value) {
            // TODO: b/350821054 - Implement synchronization policy around Entity/Component updates.
            val outOfDate = updateAndSanitizeCurrentSize(value)
            if (!outOfDate || entity == null) {
                return
            }

            val reformOptions = (entity as AndroidXrEntity).getReformOptions()
            reformOptions.currentSize = dimsToVec3(size)
            if (fixedAspectRatio != 0f) {
                reformOptions.fixedAspectRatio = fixedAspectRatio
            }
            (entity as AndroidXrEntity).updateReformOptions()
        }

    private var fixedAspectRatio = 0.0f

    override var isFixedAspectRatioEnabled: Boolean
        get() = fixedAspectRatio != 0f
        set(value) {
            val initialFixedAspectRatio = fixedAspectRatio
            updateFixedAspectRatio(value)
            // Return early if there was no update.
            if (fixedAspectRatio == initialFixedAspectRatio) {
                return
            }
            if (entity == null) {
                return
            }
            val reformOptions = (entity as AndroidXrEntity).getReformOptions()
            reformOptions.fixedAspectRatio = fixedAspectRatio
            (entity as AndroidXrEntity).updateReformOptions()
        }

    override var autoHideContent = true
    override var autoUpdateSize = true
    override var forceShowResizeOverlay = false
        set(value) {
            if (entity == null || field == value) {
                return
            }
            val reformOptions = (entity as AndroidXrEntity).getReformOptions()
            reformOptions.forceShowResizeOverlay = value
            (entity as AndroidXrEntity).updateReformOptions()
            field = value
        }

    /**
     * Sanitizes and sets the internal current size.
     *
     * Any `NaN` dimension in the input is replaced with the corresponding value from the existing
     * component size. Also updates the fixed aspect ratio if enabled.
     *
     * @param newSize The new dimensions to set.
     */
    private fun updateAndSanitizeCurrentSize(newSize: Dimensions): Boolean {
        val updatedSize = dimsClampPositive(newSize, currentSize)

        if (updatedSize == currentSize) {
            return false
        }

        currentSize = updatedSize
        // Update the fixed aspect ratio if it is enabled.
        if (fixedAspectRatio != 0f) {
            updateFixedAspectRatio(true)
        }
        return true
    }

    private fun updateFixedAspectRatio(fixedAspectRatioEnabled: Boolean) {
        var updatedFixedAspectRatio = 0f
        // Update the fixed aspect ratio based on the current size, or the default size if no
        // current size was set.
        if (fixedAspectRatioEnabled) {
            updatedFixedAspectRatio = currentSize.width / currentSize.height
        }
        fixedAspectRatio = updatedFixedAspectRatio
    }

    private val onSetSizeCompleteListener = Runnable {
        if (autoHideContent) {
            restoreEntityContent()
        }
    }

    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            return false
        }

        var entitySize: Dimensions? = null
        if (entity is PanelEntity) {
            entitySize = entity.size
        } else if (entity is SurfaceEntity) {
            val shape = entity.shape
            if (shape is SurfaceEntity.Shape.Quad) {
                entitySize = shape.dimensions
            }
        }

        if (entity is MainPanelEntityImpl) {
            entity.addOnSetSizeCompleteListener(executor, onSetSizeCompleteListener)
        }

        val reformOptions = (entity as AndroidXrEntity).getReformOptions()
        reformOptions.enabledReform = reformOptions.enabledReform or ReformOptions.ALLOW_RESIZE

        if (entitySize != null) {
            updateAndSanitizeCurrentSize(entitySize)
            reformOptions.currentSize = dimsToVec3(size)
        }

        reformOptions
            .setMinimumSize(dimsToVec3(minimumSize))
            .setMaximumSize(dimsToVec3(maximumSize))
            .setFixedAspectRatio(fixedAspectRatio)
            .forceShowResizeOverlay = forceShowResizeOverlay
        entity.updateReformOptions()
        reformEventConsumer?.let { entity.addReformEventConsumer(it, executor) }

        this.entity = entity
        return true
    }

    override fun onDetach(entity: Entity) {
        // Restore the entity's alpha synchronously here rather than calling restoreEntityContent().
        // Since restoreEntityContent() posts to the main thread, the asynchronous task might
        // execute after 'this.entity' has already been set to null below, resulting in a failure to
        // restore the UI.
        if (isContentHidden.compareAndSet(true, false)) {
            xrExtensions.createNodeTransaction().use { transaction ->
                transaction
                    .setAlpha((entity as AndroidXrEntity).getNode(), entity.getAlpha())
                    .apply()
            }
        }

        val reformOptions = (entity as AndroidXrEntity).getReformOptions()
        reformOptions.enabledReform =
            reformOptions.enabledReform and ReformOptions.ALLOW_RESIZE.inv()
        entity.updateReformOptions()
        if (entity is MainPanelEntityImpl) {
            entity.removeOnSetSizeCompleteListener(onSetSizeCompleteListener)
        }
        reformEventConsumer?.let { entity.removeReformEventConsumer(it) }
        this.entity = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun hideEntityContent() {
        if (isContentHidden.compareAndSet(false, true)) {
            mainHandler.post {
                // Double-check the state to ensure we don't hide the content if it was already
                // restored (e.g., by onDetach) while this task was in the queue.
                if (!isContentHidden.get()) return@post

                val currentEntity = entity as? AndroidXrEntity ?: return@post
                xrExtensions.createNodeTransaction().use { transaction ->
                    transaction.setAlpha(currentEntity.getNode(), 0f).apply()
                }
            }
        }
    }

    private fun restoreEntityContent() {
        if (isContentHidden.compareAndSet(true, false)) {
            mainHandler.post {
                // Double-check the state to ensure we don't restore the content if it was
                // hidden again (e.g., by another rapid resize event) while this task was in the
                // queue.
                if (isContentHidden.get()) return@post

                val currentEntity = entity as? AndroidXrEntity ?: return@post
                xrExtensions.createNodeTransaction().use { transaction ->
                    transaction.setAlpha(currentEntity.getNode(), currentEntity.getAlpha()).apply()
                }
            }
        }
    }

    private val localReformEventConsumer: Consumer<ReformEvent> = Consumer { reformEvent ->
        if (reformEvent.type != ReformEvent.REFORM_TYPE_RESIZE) {
            if (isContentHidden.get()) {
                restoreEntityContent()
            }
            return@Consumer
        }
        val proposedSize: Dimensions =
            dimsClamp(vec3ToDims(reformEvent.proposedSize), minimumSize, maximumSize, size)
        if (autoUpdateSize) {
            // Update the resize affordance size.
            size = proposedSize
        }

        val reformState = reformEvent.state
        val currentEntity = entity

        if (autoHideContent && reformState != ReformEvent.REFORM_STATE_END) {
            // Set the alpha to 0 when the resize is active before any
            // app callbacks, and restore when the resize ends after any
            // app callbacks, to hide the entity content while it's
            // being resized.
            hideEntityContent()
        }

        if (resizeEventListenerMap.isEmpty()) {
            restoreContentIfNotWaitingForSize(reformState, entity)
        } else {
            val resizeEventListenerAction =
                BiConsumer { listener: ResizeEventListener, listenerExecutor: Executor ->
                    listenerExecutor.execute {
                        listener.onResizeEvent(
                            ResizeEvent(
                                RuntimeUtils.getResizeEventState(reformEvent.state),
                                proposedSize,
                            )
                        )
                        restoreContentIfNotWaitingForSize(reformState, entity)
                    }
                }
            resizeEventListenerMap.forEach(resizeEventListenerAction)
        }
    }

    /**
     * Handles the content restoration logic at the end of a resize operation.
     *
     * This checks the state of the resize event and, if applicable (e.g. for MainPanelEntity),
     * defers the restoration if an asynchronous size update is currently in-flight.
     */
    private fun restoreContentIfNotWaitingForSize(reformState: Int, currentEntity: Entity?) {
        if (reformState != ReformEvent.REFORM_STATE_END) return
        // For MainPanelEntityImpl, setting the size is an asynchronous operation.
        // We must wait for it to complete before restoring the content's visibility
        // to prevent visual artifacts. If an IPC call is in-flight (regardless of
        // what triggered it), return early and defer restoration to the completion listener.
        //
        // TODO(b/502252493): Fix potential race condition with async size updates.
        // This mechanism assumes that the `size` property of MainPanelEntity is set
        // synchronously within the onResizeEvent callback above, which allows
        // `isWaitingForSetSize()` to return true immediately.
        // If an app developer defers setting the property (e.g., using `post`),
        // this check will fail, and content may be restored prematurely.
        if (currentEntity is MainPanelEntityImpl && currentEntity.isWaitingForSetSize()) {
            return
        }
        if (autoHideContent) {
            restoreEntityContent()
        }
    }

    override fun addResizeEventListener(
        executor: Executor,
        resizeEventListener: ResizeEventListener,
    ) {
        resizeEventListenerMap[resizeEventListener] = executor
        if (reformEventConsumer != null) {
            return
        }
        reformEventConsumer = localReformEventConsumer
        if (entity == null) {
            return
        }
        (entity as AndroidXrEntity).addReformEventConsumer(reformEventConsumer!!, this.executor)
    }

    override fun removeResizeEventListener(resizeEventListener: ResizeEventListener) {
        resizeEventListenerMap.remove(resizeEventListener)
        if (resizeEventListenerMap.isEmpty()) {
            // When the last listener is removed, unregister the consumer from the entity
            // and reset the consumer variable to null to clean up the state.
            reformEventConsumer?.let {
                (entity as AndroidXrEntity).removeReformEventConsumer(it)
                reformEventConsumer = null
            }
        }
    }

    companion object {
        private val DIMS_ZERO = Dimensions(0f, 0f, 0f)
        private val DIMS_ONE = Dimensions(1f, 1f, 1f)
        private val DIMS_INF =
            Dimensions(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)

        private fun dimsClampPositive(dimValue: Dimensions, nanFallback: Dimensions): Dimensions {
            return Dimensions(
                clampPositive(dimValue.width, nanFallback.width),
                clampPositive(dimValue.height, nanFallback.height),
                clampPositive(dimValue.depth, nanFallback.depth),
            )
        }

        private fun dimsClamp(
            dimValue: Dimensions,
            min: Dimensions,
            max: Dimensions,
            nanFallback: Dimensions,
        ): Dimensions {
            return Dimensions(
                clamp(dimValue.width, min.width, max.width, nanFallback.width),
                clamp(dimValue.height, min.height, max.height, nanFallback.height),
                clamp(dimValue.depth, min.depth, max.depth, nanFallback.depth),
            )
        }

        private fun dimsMin(a: Dimensions, b: Dimensions): Dimensions {
            return Dimensions(min(a.width, b.width), min(a.height, b.height), min(a.depth, b.depth))
        }

        private fun dimsMax(a: Dimensions, b: Dimensions): Dimensions {
            return Dimensions(max(a.width, b.width), max(a.height, b.height), max(a.depth, b.depth))
        }

        private fun dimsAnyLessThen(a: Dimensions, b: Dimensions): Boolean {
            return a.width < b.width || a.height < b.height || a.depth < b.depth
        }

        private fun dimsToVec3(value: Dimensions): Vec3 {
            return Vec3(value.width, value.height, value.depth)
        }

        private fun vec3ToDims(value: Vec3?): Dimensions {
            if (value == null) {
                return Dimensions(Float.NaN, Float.NaN, Float.NaN)
            }
            return Dimensions(value.x, value.y, value.z)
        }

        private fun clampPositive(value: Float, nanReplace: Float): Float {
            return clamp(value, 0f, Float.POSITIVE_INFINITY, nanReplace)
        }

        private fun clamp(value: Float, min: Float, max: Float, nanFallback: Float): Float {
            var value = value
            if (value.isNaN()) value = nanFallback
            if (value < min) return min
            return min(value, max)
        }
    }
}
