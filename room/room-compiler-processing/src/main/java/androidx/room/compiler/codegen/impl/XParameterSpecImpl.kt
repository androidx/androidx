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

package androidx.room.compiler.codegen.impl

import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XParameterSpec
import androidx.room.compiler.codegen.XSpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.java.JavaParameterSpec
import androidx.room.compiler.codegen.kotlin.KotlinParameterSpec

internal class XParameterSpecImpl(
    override val name: String,
    override val type: XTypeName,
    override val java: JavaParameterSpec,
    override val kotlin: KotlinParameterSpec,
) : ImplSpec<JavaParameterSpec, KotlinParameterSpec>(), XParameterSpec {
    override fun toBuilder() = Builder(name, type, java.toBuilder(), kotlin.toBuilder())

    internal class Builder(
        private val name: String,
        private val type: XTypeName,
        internal val java: JavaParameterSpec.Builder,
        internal val kotlin: KotlinParameterSpec.Builder,
    ) : XSpec.Builder(), XParameterSpec.Builder {
        private val delegates: List<XParameterSpec.Builder> = listOf(java, kotlin)

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            delegates.forEach { it.addAnnotation(annotation) }
        }

        override fun build() = XParameterSpecImpl(name, type, java.build(), kotlin.build())
    }
}
