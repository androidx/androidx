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

package androidx.room3.compiler.codegen.impl

import androidx.room3.compiler.codegen.XAnnotationSpec
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XSpec
import androidx.room3.compiler.codegen.java.JavaAnnotationSpec
import androidx.room3.compiler.codegen.kotlin.KotlinAnnotationSpec

internal class XAnnotationSpecImpl(
    override val java: JavaAnnotationSpec,
    override val kotlin: KotlinAnnotationSpec,
) : ImplSpec<JavaAnnotationSpec, KotlinAnnotationSpec>(), XAnnotationSpec {

    override fun toBuilder() = Builder(java.toBuilder(), kotlin.toBuilder())

    internal class Builder(
        val java: JavaAnnotationSpec.Builder,
        val kotlin: KotlinAnnotationSpec.Builder,
    ) : XSpec.Builder(), XAnnotationSpec.Builder {
        private val delegates: List<XAnnotationSpec.Builder> = listOf(java, kotlin)

        override fun addMember(name: String, code: XCodeBlock) = apply {
            delegates.forEach { it.addMember(name, code) }
        }

        override fun build() = XAnnotationSpecImpl(java.build(), kotlin.build())
    }
}
