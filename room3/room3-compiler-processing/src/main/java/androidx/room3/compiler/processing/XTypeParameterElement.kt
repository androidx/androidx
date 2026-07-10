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

package androidx.room3.compiler.processing

import androidx.room3.compiler.codegen.XTypeName
import com.squareup.javapoet.TypeVariableName

interface XTypeParameterElement : XElement {
    /**
     * Returns the generic class, interface, or method that is parameterized by this type parameter.
     */
    override val enclosingElement: XElement

    /**
     * Returns the bounds of this type parameter.
     *
     * Note: If there are no explicit bounds, then this list contains a single type representing
     * `java.lang.Object` in Javac or `kotlin.Any?` in KSP.
     */
    val bounds: List<XType>

    /** Returns the [TypeVariableName] for this type parameter) */
    // TODO(b/247248619): Deprecate when more progress is made, otherwise -werror fails the build.
    // @Deprecated(
    //     message = "Use asTypeVariableName().toJavaPoet() to be clear the name is for JavaPoet.",
    //     replaceWith = ReplaceWith(
    //         expression = "asTypeVariableName().toJavaPoet()",
    //         imports = ["androidx.room3.compiler.codegen.toJavaPoet"]
    //     )
    // )
    val typeVariableName: TypeVariableName

    /** Returns the type variable name for this type parameter as [XTypeName]. */
    fun asTypeVariableName(): XTypeName
}
