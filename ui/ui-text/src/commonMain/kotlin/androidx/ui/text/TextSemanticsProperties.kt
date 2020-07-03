/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text

import androidx.ui.input.ImeAction
import androidx.ui.semantics.SemanticsPropertyKey
import androidx.ui.semantics.SemanticsPropertyReceiver

/**
 * Semantics properties that apply to the text / text input elements.
 */
object TextSemanticsProperties {
    /**
     * Contains the IME action provided by the node.
     *
     *  @see SemanticsPropertyReceiver.imeAction
     */
    val ImeAction = SemanticsPropertyKey<ImeAction>("ImeAction")

    /**
     * Return whether the node supports input methods.
     *
     * Supporting input methods means that the node provides a connection to IME (keyboard) and
     * is able to accept input from it. This is typically a text field for instance.
     *
     *  @see SemanticsPropertyReceiver.supportsInputMethods
     */
    val SupportsInputMethods = SemanticsPropertyKey<Boolean>("SupportsInputMethods")
}

/**
 * Contains the IME action provided by the node.
 *
 *  @see TextSemanticsProperties.ImeAction
 */
var SemanticsPropertyReceiver.imeAction by TextSemanticsProperties.ImeAction

/**
 * Return whether the component supports input methods.
 *
 * Supporting input methods means that the component provides a connection to IME (keyboard) and
 * is able to accept input from it. This is typically a text field for instance.
 *
 *  @see TextSemanticsProperties.SupportsInputMethods
 */
var SemanticsPropertyReceiver.supportsInputMethods by TextSemanticsProperties.SupportsInputMethods