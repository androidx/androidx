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
        restoreEntityContent()
        val reformOptions = (entity as AndroidXrEntity).getReformOptions()
        reformOptions.enabledReform =
            reformOptions.enabledReform and ReformOptions.ALLOW_RESIZE.inv()
        entity.updateReformOptions()
        reformEventConsumer?.let { entity.removeReformEventConsumer(it) }
        this.entity = null
    }

    private fun hideEntityContent() {
        // Return early if the entity content is already hidden.
        if (isContentHidden.get()) {
            return
        }
        xrExtensions.createNodeTransaction().use { transaction ->
            transaction.setAlpha((entity as AndroidXrEntity).getNode(), 0f).apply()
            isContentHidden.set(true)
        }
    }

    private fun restoreEntityContent() {
        // Return early if the entity content is already visible.
        if (!isContentHidden.get()) {
            return
        }
        xrExtensions.createNodeTransaction().use { transaction ->
            transaction.setAlpha((entity as AndroidXrEntity).getNode(), entity!!.getAlpha()).apply()
            isContentHidden.set(false)
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

        val resizeEventListenerAction =
            BiConsumer { listener: ResizeEventListener, listenerExecutor: Executor ->
                listenerExecutor.execute {
                    val reformState = reformEvent.state
                    if (autoHideContent && reformState != ReformEvent.REFORM_STATE_END) {
                        // Set the alpha to 0 when the resize is active before any
                        // app callbacks, and restore when the resize ends after any
                        // app callbacks, to hide the entity content while it's
                        // being resized.
                        hideEntityContent()
                    }
                    listener.onResizeEvent(
                        ResizeEvent(
                            RuntimeUtils.getResizeEventState(reformEvent.state),
                            proposedSize,
                        )
                    )
                    if (autoHideContent && reformState == ReformEvent.REFORM_STATE_END) {
                        // Restore the entity alpha to its original value after the
                        // resize callback. We can't guarantee that the app has
                        // finished resizing when this is called, since the panel
                        // resize itself is asynchronous, or the app can use this
                        // callback to schedule resize call on a different thread.
                        restoreEntityContent()
                    }
                }
            }
        resizeEventListenerMap.forEach(resizeEventListenerAction)
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
