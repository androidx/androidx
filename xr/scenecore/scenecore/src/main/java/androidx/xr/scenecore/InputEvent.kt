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

package androidx.xr.scenecore

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Matrix4
import androidx.xr.runtime.math.Vector3
import kotlin.annotation.Retention

/**
 * Listener for [InputEvent]s. This listener can be registered with an [Entity] via
 * [InteractableComponent] to receive raw input events when the user interacts with subgraph of that
 * entity.
 */
public fun interface InputEventListener {
    /**
     * Called when an input event is received.
     *
     * @param inputEvent The input event that was received.
     */
    public fun onInputEvent(inputEvent: InputEvent)
}

/**
 * Defines input events for SceneCore.
 *
 * @param source Type of source (e.g. hands, controller, head) that generated this event.
 * @param pointerType Type of the individual pointer (e.g. left, right or default) for this event.
 * @param timestamp Timestamp from
 *   [SystemClock.uptimeMillis](https://developer.android.com/reference/android/os/SystemClock#uptimeMillis())
 *   time base.
 * @param origin The origin of the ray in the receiver's activity space.
 * @param direction A point indicating the direction the ray is pointing, in the receiver's activity
 *   space.
 * @param action Actions similar to Android's
 *   [MotionEvent](https://developer.android.com/reference/android/view/MotionEvent) for keeping
 *   track of a sequence of events on the same target, e.g., HOVER_ENTER -> HOVER_MOVE ->
 *   HOVER_EXIT, DOWN -> MOVE -> UP.
 * @param hitInfoList List of [HitInfo] for the scene entities from the same task that were hit by
 *   the input ray, if any. The list is sorted from closest to farthest from the ray origin. Note
 *   that this first hit entity remains the same during an ongoing DOWN -> MOVE -> UP action, even
 *   if the pointer stops hitting the entity during the action.
 */
public class InputEvent(
    @SourceValue public val source: Int,
    @PointerType public val pointerType: Int,
    public val timestamp: Long,
    public val origin: Vector3,
    public val direction: Vector3,
    @ActionValue public val action: Int,
    public val hitInfoList: List<HitInfo> = emptyList(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InputEvent) return false

        if (source != other.source) return false
        if (pointerType != other.pointerType) return false
        if (timestamp != other.timestamp) return false
        if (origin != other.origin) return false
        if (direction != other.direction) return false
        if (action != other.action) return false
        if (hitInfoList == other.hitInfoList) return false
        return true
    }

    override fun hashCode(): Int {
        var result = source.hashCode()
        result = 31 * result + pointerType.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + direction.hashCode()
        result = 31 * result + action.hashCode()
        result = 31 * result + hitInfoList.hashCode()
        return result
    }

    /*
     * There's a possibility of ABI mismatch here when the concrete platformAdapter starts receiving
     * input events with an updated field, such as if a newer source or pointer type has been added
     * to the underlying platform OS. We need to perform a version check when the platformAdapter is
     * constructed to ensure that the application doesn't receive anything it wasn't compiled
     * against.
     */
    // TODO: b/343468347 - Implement version check for xr extensions when loading runtime impl

    /** The type of the source (e.g. hands, controller, head) that generated this event. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Source.SOURCE_UNKNOWN,
        Source.SOURCE_HEAD,
        Source.SOURCE_CONTROLLER,
        Source.SOURCE_HANDS,
        Source.SOURCE_MOUSE,
        Source.SOURCE_GAZE_AND_GESTURE,
    )
    internal annotation class SourceValue

    /** Specifies the source (e.g. hands, controller, head) of the input event. */
    public object Source {
        /** Unknown source. */
        public const val SOURCE_UNKNOWN: Int = 0
        /**
         * Event is based on the user's head. Ray origin is at average between eyes, pushed out to
         * the near clipping plane for both eyes and points in direction head is facing. Action
         * state is based on volume up button being depressed.
         *
         * Events from this source are considered sensitive and hover events are never sent.
         */
        public const val SOURCE_HEAD: Int = 1
        /**
         * Event is based on (one of) the user's controller(s). Ray origin and direction are for a
         * controller aim pose. Action state is based on the primary button on the controller,
         * usually the bottom-most face button.
         */
        public const val SOURCE_CONTROLLER: Int = 2
        /**
         * Event is based on one of the user's hands. Ray is a hand aim pose, with origin between
         * thumb and forefinger and points in direction based on hand orientation. Action state is
         * based on a pinch gesture.
         */
        public const val SOURCE_HANDS: Int = 3
        /**
         * Event is based on a 2D mouse pointing device. Ray origin behaves the same as for
         * [Source.SOURCE_HEAD] and points in direction based on mouse movement. During a drag, the
         * ray origin moves approximating hand motion. The scroll wheel moves the ray away from /
         * towards the user. Action state is based on the primary mouse button.
         */
        public const val SOURCE_MOUSE: Int = 4
        /**
         * Event is based on a mix of the head, eyes, and hands. Ray origin is at average between
         * eyes and points in direction based on a mix of eye gaze direction and hand motion. During
         * a two-handed zoom/rotate gesture, left/right pointer events will be issued; otherwise,
         * default events are issued based on the gaze ray. Action state is based on if the user has
         * done a pinch gesture or not.
         *
         * Events from this source are considered sensitive and hover events are never sent.
         */
        public const val SOURCE_GAZE_AND_GESTURE: Int = 5
    }

    /** The type of the individual pointer (e.g. left, right or default). */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(Pointer.POINTER_TYPE_DEFAULT, Pointer.POINTER_TYPE_LEFT, Pointer.POINTER_TYPE_RIGHT)
    internal annotation class PointerType

    /** Specifies the pointer type (e.g. left, right or default) of the input event. */
    public object Pointer {
        /**
         * Default pointer type for the source (no handedness). Occurs for [Source.SOURCE_UNKNOWN],
         * [Source.SOURCE_HEAD], [Source.SOURCE_MOUSE], and [Source.SOURCE_GAZE_AND_GESTURE].
         */
        public const val POINTER_TYPE_DEFAULT: Int = 0
        /**
         * Left hand / controller pointer. Occurs for [Source.SOURCE_CONTROLLER],
         * [Source.SOURCE_HANDS], and [Source.SOURCE_GAZE_AND_GESTURE].
         */
        public const val POINTER_TYPE_LEFT: Int = 1
        /**
         * Right hand / controller pointer. Occurs for [Source.SOURCE_CONTROLLER],
         * [Source.SOURCE_HANDS], and [Source.SOURCE_GAZE_AND_GESTURE].
         */
        public const val POINTER_TYPE_RIGHT: Int = 2
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        Action.ACTION_DOWN,
        Action.ACTION_UP,
        Action.ACTION_MOVE,
        Action.ACTION_CANCEL,
        Action.ACTION_HOVER_MOVE,
        Action.ACTION_HOVER_ENTER,
        Action.ACTION_HOVER_EXIT,
    )
    internal annotation class ActionValue

    /** Specifies the action (e.g. down, up, move, etc.) of the input event. */
    public object Action {
        /** The primary action button or gesture was just pressed / started. */
        public const val ACTION_DOWN: Int = 0
        /**
         * The primary action button or gesture was just released / stopped. The hit info represents
         * the node that was originally hit (ie, as provided in the [Action.ACTION_DOWN] event).
         */
        public const val ACTION_UP: Int = 1
        /**
         * The primary action button or gesture was pressed/active in the previous event, and is
         * still pressed/active. The hit info represents the node that was originally hit (ie, as
         * provided in the [Action.ACTION_DOWN] event). The hit position may be null if the pointer
         * is no longer hitting that node.
         */
        public const val ACTION_MOVE: Int = 2
        /**
         * While the primary action button or gesture was held, the pointer was disabled. This
         * happens if you are using controllers and the battery runs out, or if you are using a
         * source that transitions to a new pointer type, eg [Source.SOURCE_GAZE_AND_GESTURE].
         */
        public const val ACTION_CANCEL: Int = 3
        /**
         * The primary action button or gesture is not pressed, and the pointer ray continued to hit
         * the same node. The hit info represents the node that was hit (may be null if pointer
         * capture is enabled).
         *
         * Hover input events are never provided for sensitive source types.
         */
        public const val ACTION_HOVER_MOVE: Int = 4
        /**
         * The primary action button or gesture is not pressed, and the pointer ray started to hit a
         * new node. The hit info represents the node that is being hit (may be null if pointer
         * capture is enabled).
         *
         * Hover input events are never provided for sensitive source types.
         */
        public const val ACTION_HOVER_ENTER: Int = 5
        /**
         * The primary action button or gesture is not pressed, and the pointer ray stopped hitting
         * the node that it was previously hitting. The hit info represents the node that was being
         * hit (may be null if pointer capture is enabled).
         *
         * Hover input events are never provided for sensitive source types.
         */
        public const val ACTION_HOVER_EXIT: Int = 6
    }

    /**
     * Information about the hit result of the input ray, originating from one of the
     * [InputEvent.Source], and intersecting with some [Entity] on the scene.
     *
     * @param inputEntity The [Entity] that was hit by the input ray. [Action.ACTION_MOVE],
     *   [Action.ACTION_UP] and [Action.ACTION_CANCEL] events will report the same node as was hit
     *   during the initial [Action.ACTION_DOWN].
     * @param hitPosition The position of the hit in the receiver's activity space. All events may
     *   report the current ray's hit position. This can be null if there no longer is a collision
     *   between the ray and the input node (e.g. during a drag event).
     * @param transform The matrix transforming activity space coordinates into the hit entity's
     *   local coordinate space.
     */
    public class HitInfo(
        public val inputEntity: Entity,
        public val hitPosition: Vector3?,
        // TODO: b/428779736 - Remove transform by providing alternative apis for its current usage.
        public val transform: Matrix4,
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HitInfo) return false
            if (inputEntity != other.inputEntity) return false
            if (hitPosition != other.hitPosition) return false
            if (transform != other.transform) return false
            return true
        }

        override fun hashCode(): Int {
            var result = inputEntity.hashCode()
            result = 31 * result + inputEntity.hashCode()
            result = 31 * result + (hitPosition?.hashCode() ?: 0)
            result = 31 * result + transform.hashCode()
            return result
        }
    }
}
