/*
 * Copyright 2022 The Android Open Source Project
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

import com.squareup.javapoet.TypeVariableName

interface XTypeParameterElement : XElement {
    /**
     * Returns the generic class, interface, or method that is parameterized by this type parameter.
     */
    override val enclosingElement: XElement

    /**
     *  Returns the bounds of this type parameter.
     *
     *  Note: If there are no explicit bounds, then this list contains a single type representing
     *  `java.lang.Object` in Javac or `kotlin.Any?` in KSP.
     */
    val bounds: List<XType>

    // TODO(b/259091615): Migrate to XTypeName
    /** Returns the [TypeVariableName] for this type parameter) */
    val typeVariableName: TypeVariableName
}
