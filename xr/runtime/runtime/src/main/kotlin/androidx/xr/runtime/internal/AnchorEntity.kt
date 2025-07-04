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

package androidx.xr.runtime.internal

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Interface for Anchor entity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AnchorEntity : SystemSpaceEntity {
    /** The current state of the anchor. */
    public val state: @State Int

    /** Returns the native pointer of the anchor. */
    // TODO(b/373711152) : Remove this property once the Jetpack XR Runtime API migration is done.
    public val nativePointer: Long

    /** Registers a listener to be called when the state of the anchor changes. */
    @Suppress("ExecutorRegistration")
    public fun setOnStateChangedListener(onStateChangedListener: OnStateChangedListener)

    /** Specifies the current tracking state of the Anchor. */
    @IntDef(State.UNANCHORED, State.ANCHORED, State.TIMED_OUT, State.ERROR)
    @Target(AnnotationTarget.TYPE)
    @Retention(AnnotationRetention.SOURCE)
    @Suppress("PublicTypedef")
    public annotation class State {
        public companion object {
            /**
             * An UNANCHORED state could mean that the perception stack hasn't found an anchor for
             * this Space, or that it has lost tracking.
             */
            public const val UNANCHORED: Int = 0

            /**
             * The ANCHORED state means that this Anchor is being actively tracked and updated by
             * the perception stack. The application should expect children to maintain their
             * relative positioning to the system's best understanding of a pose in the real world.
             */
            public const val ANCHORED: Int = 1

            /**
             * The AnchorEntity timed out while searching for an underlying anchor. It is not
             * possible to recover the AnchorEntity.
             */
            public const val TIMED_OUT: Int = 2

            /**
             * The ERROR state means that something has gone wrong and this AnchorSpace is invalid
             * without the possibility of recovery.
             */
            public const val ERROR: Int = 3
        }
    }

    /** Interface for listening to Anchor state changes. */
    public fun interface OnStateChangedListener {
        /**
         * Called when the state of the anchor changes.
         *
         * @param newState The new state of the anchor.
         */
        public fun onStateChanged(newState: @State Int)
    }
}
