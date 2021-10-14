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

package androidx.room.compiler.processing

/**
 * Parameter of a method.
 */
interface XExecutableParameterElement : XVariableElement {

    /**
     * The enclosing [XExecutableElement] this parameter belongs to.
     */
    val enclosingMethodElement: XExecutableElement

    /**
     * `true` if the parameter has a default value, `false` otherwise.
     *
     * Note that when @JvmOverloads is used in a kotlin function with KAPT, only the original
     * function's parameter might have a default value. For the generated overload methods, this
     * will always return `false`.
     */
    val hasDefaultValue: Boolean
}
