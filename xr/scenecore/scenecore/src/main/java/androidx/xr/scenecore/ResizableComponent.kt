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

@file:Suppress("BanConcurrentHashMap")

package androidx.xr.scenecore

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * A [Component] which when attached to a [PanelEntity] provides a user-resize affordance.
 *
 * Note: This Component is currently unsupported on GltfModelEntity.
 */
public class ResizableComponent
private constructor(
    private val platformAdapter: JxrPlatformAdapter,
    minimumSize: Dimensions,
    maximumSize: Dimensions,
) : Component {
    private val resizeListenerMap =
        ConcurrentHashMap<ResizeListener, JxrPlatformAdapter.ResizeEventListener>()
    /**
     * The current size of the entity, in meters. This property is automatically updated after
     * resize events to match the resize affordance to the newly suggested size of the content. The
     * apps can still override it. The default value is set to 1 meter, updated to the size of the
     * entity when attached.
     */
    public var size: Dimensions = kDimensionsOneMeter
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setSize(value.toRtDimensions())
            }
        }

    /**
     * A lower bound for the User's resize actions, in meters. This value constrains how small the
     * user can resize the bounding box of the entity. The size of the content inside that bounding
     * box is fully controlled by the application.
     */
    public var minimumSize: Dimensions = minimumSize
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setMinimumSize(value.toRtDimensions())
            }
        }

    /**
     * An upper bound for the User's resize actions, in meters. This value constrains large the user
     * can resize the bounding box of the entity. The size of the content inside that bounding box
     * is fully controlled by the application.
     */
    public var maximumSize: Dimensions = maximumSize
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setMaximumSize(value.toRtDimensions())
            }
        }

    /**
     * The aspect ratio of the entity during resizing. The aspect ratio is determined by taking the
     * entity's width over its height. A value of 0.0f (or negative) means there are no preferences.
     *
     * This method does not immediately resize the entity. The new aspect ratio will be applied the
     * next time the user resizes the entity through the reform UI. During this resize operation,
     * the entity's current area will be preserved.
     *
     * If a different resizing behavior is desired, such as fixing the width and adjusting the
     * height, the client can manually resize the entity to the preferred dimensions before calling
     * this method. No automatic resizing will occur when using the reform UI then.
     */
    public var fixedAspectRatio: Float = 0.0f
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setFixedAspectRatio(value)
            }
        }

    /**
     * Whether the content of the entity (and all child entities) should be automatically hidden
     * while it is being resized.
     */
    @get:Suppress("GetterSetterNames")
    public var autoHideContent: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setAutoHideContent(value)
            }
        }

    /**
     * Whether the size of the ResizableComponent should be automatically updated to match during an
     * ongoing resize (to match the proposed size as resize events are received).
     */
    @get:Suppress("GetterSetterNames")
    public var autoUpdateSize: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setAutoUpdateSize(value)
            }
        }

    /**
     * Whether the resize overlay should be shown even if the entity is not being resized.
     *
     * This is useful for resizing multiple panels at once.
     */
    @get:Suppress("GetterSetterNames")
    public var forceShowResizeOverlay: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.setForceShowResizeOverlay(value)
            }
        }

    private val rtResizableComponent by lazy {
        platformAdapter.createResizableComponent(
            minimumSize.toRtDimensions(),
            maximumSize.toRtDimensions(),
        )
    }

    private var entity: Entity? = null

    /**
     * Attaches this component to the given entity.
     *
     * @param entity The entity to attach this component to.
     * @return `true` if the component was successfully attached, `false` otherwise.
     */
    override fun onAttach(entity: Entity): Boolean {
        if (this.entity != null) {
            Log.e("MovableComponent", "Already attached to entity ${this.entity}")
            return false
        }
        this.entity = entity
        return (entity as BaseEntity<*>).rtEntity.addComponent(rtResizableComponent)
    }

    /**
     * Detaches this component from the entity it is attached to.
     *
     * @param entity The entity to detach this component from.
     */
    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity.removeComponent(rtResizableComponent)
        this.entity = null
    }

    /**
     * Adds the listener to the set of listeners that are invoked through the resize operation, such
     * as start, ongoing and end.
     *
     * The listener is invoked on the provided executor. If the app intends to modify the UI
     * elements/views during the callback, the app should provide the thread executor that is
     * appropriate for the UI operations. For example, if the app is using the main thread to render
     * the UI, the app should provide the main thread (Looper.getMainLooper()) executor. If the app
     * is using a separate thread to render the UI, the app should provide the executor for that
     * thread.
     *
     * @param executor The executor to use for the listener callback.
     * @param resizeListener The listener to be invoked when a resize event occurs.
     */
    public fun addResizeListener(executor: Executor, resizeListener: ResizeListener) {
        val rtResizeEventListener =
            JxrPlatformAdapter.ResizeEventListener { rtResizeEvent ->
                run {
                    val resizeEvent = rtResizeEvent.toResizeEvent()
                    when (resizeEvent.resizeState) {
                        ResizeEvent.RESIZE_STATE_ONGOING ->
                            entity?.let { resizeListener.onResizeUpdate(it, resizeEvent.newSize) }
                        ResizeEvent.RESIZE_STATE_END ->
                            entity?.let { resizeListener.onResizeEnd(it, resizeEvent.newSize) }
                        ResizeEvent.RESIZE_STATE_START ->
                            entity?.let { resizeListener.onResizeStart(it, size) }
                    }
                }
            }
        rtResizableComponent.addResizeEventListener(executor, rtResizeEventListener)
        resizeListenerMap[resizeListener] = rtResizeEventListener
    }

    /**
     * Adds the listener to the set of listeners that are invoked through the resize operation, such
     * as start, ongoing and end.
     *
     * The listener is invoked on the main thread.
     *
     * @param resizeListener The listener to be invoked when a resize event occurs.
     */
    public fun addResizeListener(resizeListener: ResizeListener) {
        addResizeListener(HandlerExecutor.mainThreadExecutor, resizeListener)
    }

    /**
     * Removes a listener from the set listening to resize events.
     *
     * @param resizeListener The listener to be removed.
     */
    public fun removeResizeListener(resizeListener: ResizeListener) {
        if (resizeListenerMap.containsKey(resizeListener)) {
            rtResizableComponent.removeResizeEventListener(resizeListenerMap[resizeListener]!!)
            resizeListenerMap.remove(resizeListener)
        }
    }

    public companion object {
        private val kDimensionsOneMeter = Dimensions(1f, 1f, 1f)
        /** Defaults min and max sizes in meters. */
        internal val kMinimumSize: Dimensions = Dimensions(0f, 0f, 0f)
        internal val kMaximumSize: Dimensions = Dimensions(10f, 10f, 10f)

        /** Factory function for creating [ResizableComponent] instance. */
        internal fun create(
            platformAdapter: JxrPlatformAdapter,
            minimumSize: Dimensions = kMinimumSize,
            maximumSize: Dimensions = kMaximumSize,
        ): ResizableComponent {
            return ResizableComponent(platformAdapter, minimumSize, maximumSize)
        }

        /**
         * Public factory function for creating a ResizableComponent. This component can be attached
         * to a single instance of any non-Anchor Entity.
         *
         * When attached, this Component will enable the user to resize the Entity by dragging along
         * the boundaries of the interaction highlight.
         *
         * @param session The Session to create the ResizableComponent in.
         * @param minimumSize A lower bound for the User's resize actions, in meters. This value is
         *   used to set constraints on how small the user can resize the bounding box of the entity
         *   down to. The size of the content inside that bounding box is fully controlled by the
         *   application. The default value for this param is 0 meters.
         * @param maximumSize An upper bound for the User's resize actions, in meters. This value is
         *   used to set constraints on how large the user can resize the bounding box of the entity
         *   up to. The size of the content inside that bounding box is fully controlled by the
         *   application. The default value for this param is 10 meters.
         * @return [ResizableComponent] instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            minimumSize: Dimensions = ResizableComponent.kMinimumSize,
            maximumSize: Dimensions = ResizableComponent.kMaximumSize,
        ): ResizableComponent =
            ResizableComponent.create(session.platformAdapter, minimumSize, maximumSize)
    }
}
