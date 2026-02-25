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
package androidx.xr.scenecore.spatial.core

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.math.Vector3.Companion.abs
import androidx.xr.scenecore.runtime.SystemSpaceEntity
import com.android.extensions.xr.XrExtensions
import com.android.extensions.xr.node.Node
import com.android.extensions.xr.node.NodeTransform
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference

/**
 * A parentless system-controlled JXRCore Entity that defines its own coordinate space.
 *
 * It is expected to be the soft root of its own parent-child entity hierarchy.
 */
@RestrictTo(
    RestrictTo.Scope.LIBRARY_GROUP
) // TODO(b/452961674): Review RestrictTo annotations in SceneCore.
public abstract class SystemSpaceEntityImpl
internal constructor(
    context: Context,
    node: Node,
    extensions: XrExtensions,
    entityManager: EntityManager,
    executor: ScheduledExecutorService,
) : AndroidXrEntity(context, node, extensions, entityManager, executor), SystemSpaceEntity {
    // Transform for this space's origin in OpenXR reference space.
    internal val openXrReferenceSpaceTransform = AtomicReference<Matrix4?>(null)
    @VisibleForTesting internal var _worldSpaceScale: Vector3 = Vector3(1f, 1f, 1f)
    // Visible for testing.
    public lateinit var nodeTransformCloseable: Closeable
    private var originChangedListener: Runnable? = null
    private var originChangedExecutor: Executor = mExecutor

    init {
        // The underlying CPM node is always expected to be updated in response to changes to
        // the coordinate space represented by a SystemSpaceEntityImpl so we subscribe at
        // construction.
        subscribeToNodeTransform(node, executor)
    }

    /** Called when the underlying space's origin has changed. */
    public fun onOriginChanged() {
        originChangedListener?.let { originChangedExecutor.execute(it) }
    }

    /** Registers the SDK layer / application's listener for space origin updates. */
    override fun setOnOriginChangedListener(listener: Runnable?, executor: Executor?) {
        originChangedListener = listener
        originChangedExecutor = executor ?: mExecutor
    }

    public val poseInOpenXrReferenceSpace: Pose?
        /**
         * Returns the pose relative to an OpenXR reference space.
         *
         * The OpenXR reference space is the space returned by
         * [XrExtensions.getOpenXrWorldReferenceSpaceType]
         */
        get() = openXrReferenceSpaceTransform.get()?.unscaled()?.pose

    /**
     * Sets the pose and scale of the entity in an OpenXR reference space and should call the
     * onOriginChanged() callback to signal a change in the underlying space.
     *
     * @param openXrReferenceSpaceTransform 4x4 transformation matrix of the entity in an OpenXR
     *   reference space. The OpenXR reference space is of the type defined by the
     *   [XrExtensions.getOpenXrWorldReferenceSpaceType] method.
     */
    public fun setOpenXrReferenceSpaceTransform(openXrReferenceSpaceTransform: Matrix4) {
        if (openXrReferenceSpaceTransform == Matrix4.Zero) {
            return
        }
        this.openXrReferenceSpaceTransform.set(openXrReferenceSpaceTransform)
        // TODO: b/353511649 - Make SystemSpaceEntityImpl thread safe.
        // Matrix4.scale returns either a positive or negative scale based on the rotation
        // matrix determinant, but we keep it positive for now to avoid any unexpected issues.
        // SpaceFlinger might apply a scale to the task node, for example if the user caused the
        // main panel to scale in Homespace mode.
        val actualScale = openXrReferenceSpaceTransform.scale
        // TODO: b/367780918 - Use the original scale, when the new matrix decomposition is tested
        // thoroughly.
        _worldSpaceScale = abs(actualScale)
        this.setScaleInternal(_worldSpaceScale.copy())
        onOriginChanged()
    }

    /**
     * Subscribes to the node's transform update events and caches the pose by calling
     * setOpenXrReferenceSpacePose().
     *
     * @param node The node to subscribe to.
     * @param executor The executor to run the callback on.
     */
    private fun subscribeToNodeTransform(node: Node, executor: ScheduledExecutorService) {
        nodeTransformCloseable =
            node.subscribeToTransform(executor) { transform: NodeTransform ->
                setOpenXrReferenceSpaceTransform(RuntimeUtils.getMatrix(transform.transform))
            }
    }

    override val worldSpaceScale: Vector3
        get() = _worldSpaceScale

    /** Unsubscribes from the node's transform update events. */
    private fun unsubscribeFromNodeTransform() {
        try {
            nodeTransformCloseable.close()
        } catch (e: Exception) {
            throw RuntimeException(
                "Could not close node transform subscription with error: " + e.message
            )
        }
    }

    override fun dispose() {
        unsubscribeFromNodeTransform()
        super.dispose()
    }
}
