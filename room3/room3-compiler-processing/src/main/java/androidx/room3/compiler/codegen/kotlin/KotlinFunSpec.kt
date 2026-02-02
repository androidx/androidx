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

import androidx.room3.compiler.codegen.KFunSpec
import androidx.room3.compiler.codegen.KFunSpecBuilder
import androidx.room3.compiler.codegen.VisibilityModifier
import androidx.room3.compiler.codegen.XAnnotationSpec
import androidx.room3.compiler.codegen.XCodeBlock
import androidx.room3.compiler.codegen.XFunSpec
import androidx.room3.compiler.codegen.XName
import androidx.room3.compiler.codegen.XParameterSpec
import androidx.room3.compiler.codegen.XSpec
import androidx.room3.compiler.codegen.XTypeName
import androidx.room3.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room3.compiler.codegen.impl.XCodeBlockImpl
import androidx.room3.compiler.codegen.impl.XParameterSpecImpl
import androidx.room3.compiler.codegen.java.JavaFunSpec.Builder
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.javapoet.KTypeVariableName

internal class KotlinFunSpec(override val actual: KFunSpec) : KotlinSpec<KFunSpec>(), XFunSpec {

    override val name: XName = XName.of(actual.name)

    override fun toBuilder() = Builder(actual.toBuilder())

    internal class Builder(internal val actual: KFunSpecBuilder) :
        XSpec.Builder(), XFunSpec.Builder {

        override fun addAnnotation(annotation: XAnnotationSpec) = apply {
            require(annotation is XAnnotationSpecImpl)
            actual.addAnnotation(annotation.kotlin.actual)
        }

        override fun addTypeVariable(typeVariable: XTypeName) = apply {
            require(typeVariable.kotlin is KTypeVariableName)
            actual.addTypeVariable(typeVariable.kotlin as KTypeVariableName)
        }

        override fun addAbstractModifier() = apply { actual.addModifiers(KModifier.ABSTRACT) }

        override fun addParameter(parameter: XParameterSpec) = apply {
            require(parameter is XParameterSpecImpl)
            actual.addParameter(parameter.kotlin.actual)
        }

        override fun addParameter(name: String, typeName: XTypeName) =
            addParameter(XParameterSpec.builder(name, typeName).build())

        override fun addCode(code: XCodeBlock) = apply {
            require(code is XCodeBlockImpl)
            actual.addCode(code.kotlin.actual)
        }

        override fun callSuperConstructor(vararg args: XCodeBlock) = apply {
            actual.callSuperConstructor(
                args.map {
                    require(it is XCodeBlockImpl)
                    it.kotlin.actual
                }
            )
        }

        override fun returns(typeName: XTypeName) = apply { actual.returns(typeName.kotlin) }

        override fun build() = KotlinFunSpec(actual.build())
    }
}

internal fun VisibilityModifier.toKotlinVisibilityModifier() =
    when (this) {
        VisibilityModifier.PUBLIC -> KModifier.PUBLIC
        VisibilityModifier.PROTECTED -> KModifier.PROTECTED
        VisibilityModifier.INTERNAL -> KModifier.INTERNAL
        VisibilityModifier.PRIVATE -> KModifier.PRIVATE
    }
