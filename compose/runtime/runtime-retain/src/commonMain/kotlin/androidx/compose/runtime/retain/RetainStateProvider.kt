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

package androidx.compose.runtime.retain

import androidx.compose.runtime.Stable

/**
 * [RetainStateProvider] is an owner of the [isRetainingExitedValues] state used by
 * [RetainedValuesStore]. This interface is extracted to allow the state of a [RetainedValuesStore]
 * to be observed without the presence of the value storage. This is particularly useful as most
 * [RetainedValuesStore]s respect a hierarchy where they begin retaining exited values when either
 * their retain condition becomes true or their parent store begins retaining exited values.
 */
public interface RetainStateProvider {
    /**
     * Returns whether retained values should continue to be held when they are removed from the
     * composition hierarchy. This indicates that content is being destroyed transiently and that
     * the associated retention scenario of this [RetainStateProvider] (e.g. navigation moving a
     * screen to the back stack, activity recreation, hidden UI, etc.) is currently active.
     *
     * When true, associated [RetainedValuesStore]s should continue to retain objects as they are
     * removed from the composition hierarchy for future reuse.
     */
    public val isRetainingExitedValues: Boolean

    /**
     * Registers the given [observer] with this [RetainStateProvider] to be notified when the value
     * of [isRetainingExitedValues] changes. The added observer will receive its first notification
     * the next time [isRetainingExitedValues] is updated.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see removeRetainStateObserver
     */
    public fun addRetainStateObserver(observer: RetainStateObserver)

    /**
     * Removes a previously registered [observer]. It will receive no further updates from this
     * [RetainStateProvider] unless it is registered again in the future. If the observer is not
     * currently registered, this this method does nothing.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see addRetainStateObserver
     */
    public fun removeRetainStateObserver(observer: RetainStateObserver)

    /**
     * Listener interface to observe changes in the value of
     * [RetainStateProvider.isRetainingExitedValues].
     *
     * @see RetainStateProvider.addRetainStateObserver
     * @see RetainStateProvider.removeRetainStateObserver
     */
    @Suppress("CallbackName")
    public interface RetainStateObserver {
        /**
         * Called to indicate that [RetainStateProvider.isRetainingExitedValues] has become `true`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStartRetainingExitedValues()

        /**
         * Called to indicate that [RetainStateProvider.isRetainingExitedValues] has become `false`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStopRetainingExitedValues()
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainedValuesStore] and
     * is always set to retain exited values. This object is stateless and can be used to orphan a
     * nested [RetainedValuesStore] while maintaining it in a state where the store retains all
     * exited values.
     */
    @Stable
    public object AlwaysRetainExitedValues : RetainStateProvider {
        override val isRetainingExitedValues: Boolean
            get() = true

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainedValuesStore] and
     * is never set to retain exited values. This object is stateless and can be used to orphan a
     * nested [RetainedValuesStore] and clear any parent-driven state of [isRetainingExitedValues].
     */
    @Stable
    public object NeverRetainExitedValues : RetainStateProvider {
        override val isRetainingExitedValues: Boolean
            get() = false

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }
}
