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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.runtime.ResizeEventListener as RtResizeEventListener
import androidx.xr.scenecore.runtime.SceneRuntime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * This [Component] can be attached to a single instance of an [Entity]. When attached, this
 * Component will enable the user to resize the Entity by selecting and dragging along the
 * boundaries of a user-resize affordance. While resizing an overlay will appear indicating the
 * proposed updated size.
 *
 * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
 * [Entity.addComponent] to an Entity with these types will return false.
 *
 * Note: This Component is currently unsupported on GltfModelEntity.
 */
public class ResizableComponent
private constructor(
    private val sceneRuntime: SceneRuntime,
    minimumSize: FloatSize3d,
    maximumSize: FloatSize3d,
    private val initialListenerExecutor: Executor,
    private val initialListener: Consumer<ResizeEvent>,
) : Component {
    private val resizeListenerMap =
        ConcurrentHashMap<Consumer<ResizeEvent>, RtResizeEventListener>()
    /**
     * The current size of the affordance for the [Entity], in meters. This property is
     * automatically updated after resize events to match the resize affordance to the newly
     * suggested size of the content. The apps can still override it. The default value is set to 1
     * meter. If attached to a [PanelEntity], this is updated to the size of the Entity when
     * attached.
     */
    public var affordanceSize: FloatSize3d = kDimensionsOneMeter
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.size = value.toRtDimensions()
            }
        }

    /**
     * A lower bound for the User's resize actions, in meters. This value constrains how small the
     * user can resize the bounding box of the [Entity]. The size of the content inside that
     * bounding box is fully controlled by the application.
     */
    public var minimumEntitySize: FloatSize3d = minimumSize
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.minimumSize = value.toRtDimensions()
            }
        }

    /**
     * An upper bound for the User's resize actions, in meters. This value constrains large the user
     * can resize the bounding box of the [Entity]. The size of the content inside that bounding box
     * is fully controlled by the application.
     */
    public var maximumEntitySize: FloatSize3d = maximumSize
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.maximumSize = value.toRtDimensions()
            }
        }

    /**
     * Whether the aspect ratio is maintained during resizing.
     *
     * If true the affordance will maintain its current aspect ratio being resized and all suggested
     * sizes will maintain the current aspect ratio. This defaults to false.
     */
    public var isFixedAspectRatioEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.isFixedAspectRatioEnabled = value
            }
        }

    /**
     * Whether the content of the [Entity], and all child Entities, will be automatically hidden
     * while it is being resized.
     */
    public var isAutoHideContentWhileResizingEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.autoHideContent = value
            }
        }

    /**
     * Whether the size of the resize overlay should be automatically updated to match the proposed
     * size as resize events are received.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @get:JvmName("shouldAutoUpdateOverlay")
    public var shouldAutoUpdateOverlay: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.autoUpdateSize = value
            }
        }

    /**
     * Whether a resize overlay will be shown even if the entity is not being resized.
     *
     * This is useful for resizing multiple panels at once.
     */
    public var isAlwaysShowOverlayEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                rtResizableComponent.forceShowResizeOverlay = value
            }
        }

    private val rtResizableComponent by lazy {
        sceneRuntime.createResizableComponent(
            minimumSize.toRtDimensions(),
            maximumSize.toRtDimensions(),
        )
    }

    private var entity: Entity? = null

    override fun onAttach(entity: Entity): Boolean {
        if (entity is AnchorEntity || entity is ActivitySpace) {
            return false
        }
        if (this.entity != null) {
            return false
        }
        this.entity = entity
        val attached = (entity as BaseEntity<*>).rtEntity!!.addComponent(rtResizableComponent)
        if (!attached) {
            return false
        }
        addResizeEventListener(initialListenerExecutor, initialListener)

        return true
    }

    override fun onDetach(entity: Entity) {
        (entity as BaseEntity<*>).rtEntity!!.removeComponent(rtResizableComponent)
        this.entity = null
    }

    /**
     * Adds the listener to the set of listeners that are invoked through the resize operation, such
     * as start, ongoing and end.
     *
     * The listener is invoked on the provided [Executor] if provided.
     *
     * @param executor The Executor to run the listener on. By default listener is invoked on the
     *   main thread.
     * @param resizeEventListener The listener to be invoked when a resize event occurs.
     */
    @JvmOverloads
    public fun addResizeEventListener(
        executor: Executor = HandlerExecutor.mainThreadExecutor,
        resizeEventListener: Consumer<ResizeEvent>,
    ) {
        val rtResizeEventListener = RtResizeEventListener { rtResizeEvent ->
            run { entity?.let { resizeEventListener.accept(rtResizeEvent.toResizeEvent(it)) } }
        }
        rtResizableComponent.addResizeEventListener(executor, rtResizeEventListener)
        resizeListenerMap[resizeEventListener] = rtResizeEventListener
    }

    /**
     * Removes a listener from the set listening to resize events.
     *
     * @param resizeEventListener The listener to be removed.
     */
    public fun removeResizeEventListener(resizeEventListener: Consumer<ResizeEvent>) {
        if (resizeListenerMap.containsKey(resizeEventListener)) {
            rtResizableComponent.removeResizeEventListener(resizeListenerMap[resizeEventListener]!!)
            resizeListenerMap.remove(resizeEventListener)
        }
    }

    public companion object {
        private val kDimensionsOneMeter = FloatSize3d(1f, 1f, 1f)
        /** Defaults min and max sizes in meters. */
        internal val kMinimumSize: FloatSize3d = FloatSize3d(0f, 0f, 0f)
        internal val kMaximumSize: FloatSize3d = FloatSize3d(10f, 10f, 10f)

        /** Factory function for creating [ResizableComponent] instance. */
        internal fun create(
            sceneRuntime: SceneRuntime,
            minimumSize: FloatSize3d,
            maximumSize: FloatSize3d,
            initialListenerExecutor: Executor,
            initialListener: Consumer<ResizeEvent>,
        ): ResizableComponent {
            return ResizableComponent(
                sceneRuntime,
                minimumSize,
                maximumSize,
                initialListenerExecutor,
                initialListener,
            )
        }

        /**
         * Public factory function for creating a ResizableComponent.
         *
         * This [Component] can be attached to a single instance of an [Entity]. When attached, this
         * Component will enable the user to resize the Entity by dragging along the boundaries of a
         * user-resize affordance.
         *
         * This component cannot be attached to an [AnchorEntity] or to the [ActivitySpace]. Calling
         * [Entity.addComponent] to an Entity with these types will return false.
         *
         * @param session The [Session] to create the ResizableComponent in.
         * @param minimumSize A lower bound for the User's resize actions, in meters. This value is
         *   used to set constraints on how small the user can resize the bounding box of the entity
         *   down to. The size of the content inside that bounding box is fully controlled by the
         *   application. The default value is 0 meters.
         * @param maximumSize An upper bound for the User's resize actions, in meters. This value is
         *   used to set constraints on how large the user can resize the bounding box of the entity
         *   up to. The size of the content inside that bounding box is fully controlled by the
         *   application. The default value is 10 meters.
         * @param executor The Executor to run the listener on. By default listener is invoked on
         *   the main thread.
         * @param resizeEventListener A resize event listener for the event. The application should
         *   set the size of a PanelEntity using [PanelEntity.size].
         * @return [ResizableComponent] instance.
         */
        @JvmOverloads
        @JvmStatic
        public fun create(
            session: Session,
            minimumSize: FloatSize3d = kMinimumSize,
            maximumSize: FloatSize3d = kMaximumSize,
            executor: Executor = HandlerExecutor.mainThreadExecutor,
            resizeEventListener: Consumer<ResizeEvent>,
        ): ResizableComponent =
            ResizableComponent.create(
                session.sceneRuntime,
                minimumSize,
                maximumSize,
                executor,
                resizeEventListener,
            )
    }
}
