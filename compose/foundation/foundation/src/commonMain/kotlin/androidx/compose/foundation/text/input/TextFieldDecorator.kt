/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.Composable

/**
 * Composable interface that allows to add decorations around text field, such as icon,
 * placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field.
 *
 * @sample androidx.compose.foundation.samples.BasicTextField2DecoratorSample
 */
@ExperimentalFoundationApi
fun interface TextFieldDecorator {

    /**
     * To allow you to control the placement of the inner text field relative to your decorations,
     * the text field implementation will pass in a framework-controlled composable parameter
     * [innerTextField] to this method. You must not call [innerTextField] more than once.
     */
    // Composable parameters of Composable functions are normally slots for callers to inject
    // their content but this one is a special inverted-slot API. It's better to be explicit
    // with the naming.
    @Suppress("ComposableLambdaParameterNaming")
    @Composable
    fun Decoration(innerTextField: @Composable () -> Unit)
}
