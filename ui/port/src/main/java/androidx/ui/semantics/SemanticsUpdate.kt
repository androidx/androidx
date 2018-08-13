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

package androidx.ui.semantics

/**
 * An opaque object representing a batch of semantics updates.
 *
 * To create a SemanticsUpdate object, use a [SemanticsUpdateBuilder].
 *
 * Semantics updates can be applied to the system's retained semantics tree
 * using the [Window.updateSemantics] method.
 */
// TODO(Migration/Andrey): native code?
class SemanticsUpdate /*extends NativeFieldWrapperClass2*/ {
    /**
     * This class is created by the engine, and should not be instantiated
     * or extended directly.
     *
     * To create a SemanticsUpdate object, use a [SemanticsUpdateBuilder].
     */
    internal constructor()

    /**
     * Releases the resources used by this semantics update.
     *
     * After calling this function, the semantics update is cannot be used
     * further.
     */
    fun dispose() {
        TODO()
//        native 'SemanticsUpdate_dispose';
    }
}
