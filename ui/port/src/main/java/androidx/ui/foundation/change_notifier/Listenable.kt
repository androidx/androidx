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

package androidx.ui.foundation.change_notifier

import androidx.ui.VoidCallback

/** An object that maintains a list of listeners. */
interface Listenable {
    companion object {
        /**
         * Return a [Listenable] that triggers when any of the given [Listenable]s themselves
         * trigger.
         *
         * The list must not be changed after this method has been called. Doing so will lead to
         * memory leaks or exceptions.
         *
         * The list may contain nulls; they are ignored.
         */
        fun merge(listenables: List<Listenable>) {
//            =_MergingListenable;
            TODO("not implemented")
        }
    }

    /** Register a closure to be called when the object notifies its listeners. */
    fun addListener(listener: VoidCallback)

    /**
     * Remove a previously registered closure from the list of closures that the object notifies.
     */
    fun removeListener(listener: VoidCallback)
}