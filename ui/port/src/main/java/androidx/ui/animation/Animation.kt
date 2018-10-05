/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.animation

import androidx.ui.VoidCallback
import androidx.ui.foundation.change_notifier.Listenable
import androidx.ui.foundation.change_notifier.ValueListenable
import androidx.ui.foundation.diagnostics.describeIdentity

/** The status of an animation */
enum class AnimationStatus {
    /** The animation is stopped at the beginning */
    DISMISSED,

    /** The animation is running from beginning to end */
    FORWARD,

    /** The animation is running backwards, from end to beginning */
    REVERSE,

    /** The animation is stopped at the end */
    COMPLETED
}

/** Signature for listeners attached using [Animation.addStatusListener]. */
typealias AnimationStatusListener = (status: AnimationStatus) -> Unit

/**
 * An animation with a value of type `T`.
 *
 * An animation consists of a value (of type `T`) together with a status. The
 * status indicates whether the animation is conceptually running from
 * beginning to end or from the end back to the beginning, although the actual
 * value of the animation might not change monotonically (e.g., if the
 * animation uses a curve that bounces).
 *
 * Animations also let other objects listen for changes to either their value
 * or their status. These callbacks are called during the "animation" phase of
 * the pipeline, just prior to rebuilding widgets.
 *
 * To create a new animation that you can run forward and backward, consider
 * using [AnimationController].
 */
abstract class Animation<T> : Listenable, ValueListenable<T> {

    // keep these next five dartdocs in sync with the dartdocs in AnimationWithParentMixin<T>

    /**
     * Calls the listener every time the value of the animation changes.
     *
     * Listeners can be removed with [removeListener].
     */
    abstract override fun addListener(listener: VoidCallback)

    /**
     * Stop calling the listener every time the value of the animation changes.
     *
     * Listeners can be added with [addListener].
     */
    abstract override fun removeListener(listener: VoidCallback)

    /**
     * Calls listener every time the status of the animation changes.
     *
     * Listeners can be removed with [removeStatusListener].
     */
    abstract fun addStatusListener(listener: AnimationStatusListener)

    /**
     * Stops calling the listener every time the status of the animation changes.
     *
     * Listeners can be added with [addStatusListener].
     */
    abstract fun removeStatusListener(listener: AnimationStatusListener)

    /** The current status of this animation. */
    abstract val status: AnimationStatus

    /** The current value of the animation. */
    abstract override val value: T

    override fun toString() = "${describeIdentity(this)}(${this.toStringDetails()})"

    /**
     * Provides a string describing the status of this object, but not including
     * information about the object itself.
     *
     * This function is used by [Animation.toString] so that [Animation]
     * subclasses can provide additional details while ensuring all [Animation]
     * subclasses have a consistent [toString] style.
     *
     * The result of this function includes an icon describing the status of this
     * [Animation] object:
     *
     * * "&#x25B6;": [AnimationStatus.FORWARD] ([value] increasing)
     * * "&#x25C0;": [AnimationStatus.REVERSE] ([value] decreasing)
     * * "&#x23ED;": [AnimationStatus.COMPLETED] ([value] == 1.0)
     * * "&#x23EE;": [AnimationStatus.DISMISSED] ([value] == 0.0)
     */
    open fun toStringDetails() = when (status) {
        AnimationStatus.FORWARD -> "▶"
        AnimationStatus.REVERSE -> "◀"
        AnimationStatus.COMPLETED -> "⏭"
        AnimationStatus.DISMISSED -> "⏮"
    }
}

/** Whether this animation is stopped at the beginning. */
val <T> Animation<T>.isDismissed get() = (status == AnimationStatus.DISMISSED)

/** Whether this animation is stopped at the end. */
val <T> Animation<T>.isCompleted get() = (status == AnimationStatus.COMPLETED)