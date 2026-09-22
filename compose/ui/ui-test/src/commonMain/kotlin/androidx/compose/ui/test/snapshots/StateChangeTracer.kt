/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.test.snapshots

/**
 * Traces where state objects are written, and where the test is at that moment, so that the error
 * of a failing runOnEachSnapshotApply block shows the test statements behind it.
 *
 * Create it on the thread that runs the test.
 */
internal expect class StateChangeTracer() {
    /**
     * Records that the current thread writes [state]. Call it on the thread that writes, while it
     * writes, so the record shows both the code that writes and the statement that the test runs.
     */
    fun recordWrite(state: Any)

    /** Forgets the recorded writes of all state objects that aren't in [states]. */
    fun retainWrites(states: Set<Any>)

    /**
     * Forgets the recorded writes of [states]. Call it once a snapshot that changed them was
     * applied, so that these write can't explain a later change that isn't recorded.
     */
    fun forgetWrites(states: Set<Any>)

    /**
     * Rewrites the stack trace of [failure], which a block threw, to show the test statements
     * behind it: where the test was when the state that the block read was written, or, if none of
     * it changed, where the block was registered and where the test is now. Adds where that state
     * was written as suppressed [Throwable]s.
     *
     * @param failure the error that the block threw
     * @param changed the state objects that the block read and that changed in the snapshot it
     *   failed on, each with its description as the block saw it
     * @param registeredAt a [Throwable] created where the block was registered
     */
    fun explain(failure: Throwable, changed: Map<Any, String>, registeredAt: Throwable)
}
