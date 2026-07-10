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

package androidx.room3.compiler.codegen.kotlin

import androidx.room3.compiler.codegen.KAnnotationSpecBuilder
import androidx.room3.compiler.codegen.XAnnotationSpec
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XSpec
import androidx.room3.compiler.codegen.impl.XCodeBlockImpl
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.javapoet.KAnnotationSpec

internal class KotlinAnnotationSpec(override val actual: KAnnotationSpec) :
    KotlinSpec<KAnnotationSpec>(), XAnnotationSpec {

    override fun toBuilder() = Builder(actual.toBuilder())

    internal class Builder(internal val actual: KAnnotationSpecBuilder) :
        XSpec.Builder(), XAnnotationSpec.Builder {
        override fun addMember(name: String, code: XCodeBlock) = apply {
            require(code is XCodeBlockImpl)
            actual.addMember(CodeBlock.of("$name = %L", code.kotlin.actual))
        }

        override fun build() = KotlinAnnotationSpec(actual.build())
    }
}
