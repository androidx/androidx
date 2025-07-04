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

package androidx.room.compiler.codegen.compat

import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.JAnnotationSpecBuilder
import androidx.room.compiler.codegen.JCodeBlock
import androidx.room.compiler.codegen.JCodeBlockBuilder
import androidx.room.compiler.codegen.JFileSpec
import androidx.room.compiler.codegen.JFileSpecBuilder
import androidx.room.compiler.codegen.JFunSpec
import androidx.room.compiler.codegen.JFunSpecBuilder
import androidx.room.compiler.codegen.JParameterSpecBuilder
import androidx.room.compiler.codegen.JPropertySpecBuilder
import androidx.room.compiler.codegen.JTypeSpecBuilder
import androidx.room.compiler.codegen.KAnnotationSpecBuilder
import androidx.room.compiler.codegen.KCodeBlock
import androidx.room.compiler.codegen.KCodeBlockBuilder
import androidx.room.compiler.codegen.KFileSpec
import androidx.room.compiler.codegen.KFileSpecBuilder
import androidx.room.compiler.codegen.KFunSpec
import androidx.room.compiler.codegen.KFunSpecBuilder
import androidx.room.compiler.codegen.KParameterSpecBuilder
import androidx.room.compiler.codegen.KPropertySpecBuilder
import androidx.room.compiler.codegen.KTypeSpecBuilder
import androidx.room.compiler.codegen.XAnnotationSpec
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XCodeBlock
import androidx.room.compiler.codegen.XFileSpec
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XMemberName
import androidx.room.compiler.codegen.XName
import androidx.room.compiler.codegen.XParameterSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.impl.XAnnotationSpecImpl
import androidx.room.compiler.codegen.impl.XCodeBlockImpl
import androidx.room.compiler.codegen.impl.XFileSpecImpl
import androidx.room.compiler.codegen.impl.XFunSpecImpl
import androidx.room.compiler.codegen.impl.XParameterSpecImpl
import androidx.room.compiler.codegen.impl.XPropertySpecImpl
import androidx.room.compiler.codegen.impl.XTypeSpecImpl
import androidx.room.compiler.codegen.java.JavaAnnotationSpec
import androidx.room.compiler.codegen.java.JavaCodeBlock
import androidx.room.compiler.codegen.java.JavaFileSpec
import androidx.room.compiler.codegen.java.JavaFunSpec
import androidx.room.compiler.codegen.java.JavaParameterSpec
import androidx.room.compiler.codegen.java.JavaPropertySpec
import androidx.room.compiler.codegen.java.JavaTypeSpec
import androidx.room.compiler.codegen.kotlin.KotlinAnnotationSpec
import androidx.room.compiler.codegen.kotlin.KotlinCodeBlock
import androidx.room.compiler.codegen.kotlin.KotlinFileSpec
import androidx.room.compiler.codegen.kotlin.KotlinFunSpec
import androidx.room.compiler.codegen.kotlin.KotlinParameterSpec
import androidx.room.compiler.codegen.kotlin.KotlinPropertySpec
import androidx.room.compiler.codegen.kotlin.KotlinTypeSpec
import androidx.room.compiler.processing.XNullability
import com.squareup.kotlinpoet.javapoet.JAnnotationSpec
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.JTypeName
import com.squareup.kotlinpoet.javapoet.JTypeSpec
import com.squareup.kotlinpoet.javapoet.KAnnotationSpec
import com.squareup.kotlinpoet.javapoet.KClassName
import com.squareup.kotlinpoet.javapoet.KTypeName
import com.squareup.kotlinpoet.javapoet.KTypeSpec
import com.squareup.kotlinpoet.javapoet.toJTypeName
import com.squareup.kotlinpoet.javapoet.toKTypeName

object XConverters {
    @JvmStatic fun XName.toJavaPoet() = java

    @JvmStatic fun XMemberName.toJavaPoet() = java

    @JvmStatic fun XTypeName.toJavaPoet() = java

    @JvmStatic fun XClassName.toJavaPoet() = java

    @JvmStatic
    fun XAnnotationSpec.toJavaPoet() =
        when (this) {
            is XAnnotationSpecImpl -> java.actual
            is JavaAnnotationSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XAnnotationSpec.Builder.toJavaPoet() =
        when (this) {
            is XAnnotationSpecImpl.Builder -> java.actual
            is JavaAnnotationSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XCodeBlock.toJavaPoet() =
        when (this) {
            is XCodeBlockImpl -> java.actual
            is JavaCodeBlock -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XCodeBlock.Builder.toJavaPoet() =
        when (this) {
            is XCodeBlockImpl.Builder -> java.actual
            is JavaCodeBlock.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFunSpec.toJavaPoet() =
        when (this) {
            is XFunSpecImpl -> java.actual
            is JavaFunSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFunSpec.Builder.toJavaPoet() =
        when (this) {
            is XFunSpecImpl.Builder -> java.actual
            is JavaFunSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XPropertySpec.toJavaPoet() =
        when (this) {
            is XPropertySpecImpl -> java.actual
            is JavaPropertySpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XPropertySpec.Builder.toJavaPoet() =
        when (this) {
            is XPropertySpecImpl.Builder -> java.actual
            is JavaPropertySpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XParameterSpec.toJavaPoet() =
        when (this) {
            is XParameterSpecImpl -> java.actual
            is JavaParameterSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XParameterSpec.Builder.toJavaPoet() =
        when (this) {
            is XParameterSpecImpl.Builder -> java.actual
            is JavaParameterSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XTypeSpec.toJavaPoet() =
        when (this) {
            is XTypeSpecImpl -> java.actual
            is JavaTypeSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XTypeSpec.Builder.toJavaPoet() =
        when (this) {
            is XTypeSpecImpl.Builder -> java.actual
            is JavaTypeSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFileSpec.toJavaPoet() =
        when (this) {
            is XFileSpecImpl -> java.actual
            is JavaFileSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFileSpec.Builder.toJavaPoet() =
        when (this) {
            is XFileSpecImpl.Builder -> java.actual
            is JavaFileSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic fun XName.toKotlinPoet() = kotlin

    @JvmStatic fun XMemberName.toKotlinPoet() = kotlin

    @JvmStatic fun XTypeName.toKotlinPoet() = kotlin

    @JvmStatic fun XClassName.toKotlinPoet() = kotlin

    @JvmStatic
    fun XAnnotationSpec.toKotlinPoet() =
        when (this) {
            is XAnnotationSpecImpl -> kotlin.actual
            is KotlinAnnotationSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XAnnotationSpec.Builder.toKotlinPoet() =
        when (this) {
            is XAnnotationSpecImpl.Builder -> kotlin.actual
            is KotlinAnnotationSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XCodeBlock.toKotlinPoet() =
        when (this) {
            is XCodeBlockImpl -> kotlin.actual
            is KotlinCodeBlock -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XCodeBlock.Builder.toKotlinPoet() =
        when (this) {
            is XCodeBlockImpl.Builder -> kotlin.actual
            is KotlinCodeBlock.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFunSpec.toKotlinPoet() =
        when (this) {
            is XFunSpecImpl -> kotlin.actual
            is KotlinFunSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFunSpec.Builder.toKotlinPoet() =
        when (this) {
            is XFunSpecImpl.Builder -> kotlin.actual
            is KotlinFunSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XParameterSpec.toKotlinPoet() =
        when (this) {
            is XParameterSpecImpl -> kotlin.actual
            is KotlinParameterSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XParameterSpec.Builder.toKotlinPoet() =
        when (this) {
            is XParameterSpecImpl.Builder -> kotlin.actual
            is KotlinParameterSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XPropertySpec.toKotlinPoet() =
        when (this) {
            is XPropertySpecImpl -> kotlin.actual
            is KotlinPropertySpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XPropertySpec.Builder.toKotlinPoet() =
        when (this) {
            is XPropertySpecImpl.Builder -> kotlin.actual
            is KotlinPropertySpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XTypeSpec.toKotlinPoet() =
        when (this) {
            is XTypeSpecImpl -> kotlin.actual
            is KotlinTypeSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XTypeSpec.Builder.toKotlinPoet() =
        when (this) {
            is XTypeSpecImpl.Builder -> kotlin.actual
            is KotlinTypeSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFileSpec.toKotlinPoet() =
        when (this) {
            is XFileSpecImpl -> kotlin.actual
            is KotlinFileSpec -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun XFileSpec.Builder.toKotlinPoet() =
        when (this) {
            is XFileSpecImpl.Builder -> kotlin.actual
            is KotlinFileSpec.Builder -> actual
            else -> error("Unsupported type: ${this.javaClass}")
        }

    @JvmStatic
    fun JClassName.toXPoet() =
        XClassName.get(this.packageName(), *this.simpleNames().toTypedArray())

    @JvmStatic
    fun KClassName.toXPoet() = XClassName.get(this.packageName, *this.simpleNames.toTypedArray())

    @JvmStatic
    fun toXPoet(jClassName: JClassName, kClassName: KClassName) =
        XClassName(jClassName, kClassName, kClassName.nullability())

    @JvmStatic fun JTypeName.toXPoet() = toXPoet(this, toKTypeName())

    @JvmStatic fun KTypeName.toXPoet() = toXPoet(this.toJTypeName(), this)

    @JvmStatic
    fun toXPoet(jTypeName: JTypeName, kTypeName: KTypeName) =
        XTypeName(jTypeName, kTypeName, kTypeName.nullability())

    private fun KTypeName.nullability() =
        if (isNullable) {
            XNullability.NULLABLE
        } else {
            XNullability.UNKNOWN
        }

    @JvmStatic
    fun toXPoet(
        jAnnotationSpec: JAnnotationSpec,
        kAnnotationSpec: KAnnotationSpec,
    ): XAnnotationSpec =
        XAnnotationSpecImpl(
            JavaAnnotationSpec(jAnnotationSpec),
            KotlinAnnotationSpec(kAnnotationSpec),
        )

    @JvmStatic
    fun toXPoet(jCodeBlock: JCodeBlock, kCodeBlock: KCodeBlock): XCodeBlock =
        XCodeBlockImpl(JavaCodeBlock(jCodeBlock), KotlinCodeBlock(kCodeBlock))

    @JvmStatic
    fun toXPoet(
        jCodeBlockBuilder: JCodeBlockBuilder,
        kCodeBlockBuilder: KCodeBlockBuilder,
    ): XCodeBlock.Builder =
        XCodeBlockImpl.Builder(
            JavaCodeBlock.Builder(jCodeBlockBuilder),
            KotlinCodeBlock.Builder(kCodeBlockBuilder),
        )

    @JvmStatic
    fun toXPoet(
        addJavaNullabilityAnnotation: Boolean,
        jFunSpec: JFunSpec,
        kFunSpec: KFunSpec,
    ): XFunSpec =
        XFunSpecImpl(JavaFunSpec(addJavaNullabilityAnnotation, jFunSpec), KotlinFunSpec(kFunSpec))

    @JvmStatic
    fun toXPoet(
        addJavaNullabilityAnnotation: Boolean,
        jFunSpecBuilder: JFunSpecBuilder,
        kFunSpecBuilder: KFunSpecBuilder,
    ): XFunSpec.Builder =
        XFunSpecImpl.Builder(
            JavaFunSpec.Builder(addJavaNullabilityAnnotation, jFunSpecBuilder),
            KotlinFunSpec.Builder(kFunSpecBuilder),
        )

    @JvmStatic
    fun toXPoet(jTypeSpec: JTypeSpec, kTypeSpec: KTypeSpec): XTypeSpec =
        XTypeSpecImpl(JavaTypeSpec(jTypeSpec), KotlinTypeSpec(kTypeSpec))

    @JvmStatic
    fun toXPoet(
        jTypeSpecBuilder: JTypeSpecBuilder,
        kTypeSpecBuilder: KTypeSpecBuilder,
    ): XTypeSpec.Builder =
        XTypeSpecImpl.Builder(
            JavaTypeSpec.Builder(jTypeSpecBuilder),
            KotlinTypeSpec.Builder(kTypeSpecBuilder),
        )

    @JvmStatic
    fun toXPoet(jFileSpec: JFileSpec, kFileSpec: KFileSpec): XFileSpec =
        XFileSpecImpl(JavaFileSpec(jFileSpec), KotlinFileSpec(kFileSpec))

    @JvmStatic
    fun toXPoet(
        jFileSpecBuilder: JFileSpecBuilder,
        kFileSpecBuilder: KFileSpecBuilder,
    ): XFileSpec.Builder =
        XFileSpecImpl.Builder(
            JavaFileSpec.Builder(jFileSpecBuilder),
            KotlinFileSpec.Builder(kFileSpecBuilder),
        )

    @JvmStatic
    fun XAnnotationSpec.Builder.applyToJavaPoet(block: JAnnotationSpecBuilder.() -> Unit) = apply {
        if (this is XAnnotationSpecImpl.Builder || this is JavaAnnotationSpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XCodeBlock.Builder.applyToJavaPoet(block: JCodeBlockBuilder.() -> Unit) = apply {
        if (this is XCodeBlockImpl.Builder || this is JavaCodeBlock.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XFunSpec.Builder.applyToJavaPoet(block: JFunSpecBuilder.() -> Unit) = apply {
        if (this is XFunSpecImpl.Builder || this is JavaFunSpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XParameterSpec.Builder.applyToJavaPoet(block: JParameterSpecBuilder.() -> Unit) = apply {
        if (this is XParameterSpecImpl.Builder || this is JavaParameterSpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XPropertySpec.Builder.applyToJavaPoet(block: JPropertySpecBuilder.() -> Unit) = apply {
        if (this is XPropertySpecImpl.Builder || this is JavaPropertySpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XTypeSpec.Builder.applyToJavaPoet(block: JTypeSpecBuilder.() -> Unit) = apply {
        if (this is XTypeSpecImpl.Builder || this is JavaTypeSpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XFileSpec.Builder.applyToJavaPoet(block: JFileSpecBuilder.() -> Unit) = apply {
        if (this is XFileSpecImpl.Builder || this is JavaFileSpec.Builder) {
            toJavaPoet().block()
        }
    }

    @JvmStatic
    fun XAnnotationSpec.Builder.applyToKotlinPoet(block: KAnnotationSpecBuilder.() -> Unit) =
        apply {
            if (this is XAnnotationSpecImpl.Builder || this is KotlinAnnotationSpec.Builder) {
                toKotlinPoet().block()
            }
        }

    @JvmStatic
    fun XCodeBlock.Builder.applyToKotlinPoet(block: KCodeBlockBuilder.() -> Unit) = apply {
        if (this is XCodeBlockImpl.Builder || this is KotlinCodeBlock.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XFunSpec.Builder.applyToKotlinPoet(block: KFunSpecBuilder.() -> Unit) = apply {
        if (this is XFunSpecImpl.Builder || this is KotlinFunSpec.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XParameterSpec.Builder.applyToKotlinPoet(block: KParameterSpecBuilder.() -> Unit) = apply {
        if (this is XParameterSpecImpl.Builder || this is KotlinParameterSpec.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XPropertySpec.Builder.applyToKotlinPoet(block: KPropertySpecBuilder.() -> Unit) = apply {
        if (this is XPropertySpecImpl.Builder || this is KotlinPropertySpec.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XTypeSpec.Builder.applyToKotlinPoet(block: KTypeSpecBuilder.() -> Unit) = apply {
        if (this is XTypeSpecImpl.Builder || this is KotlinTypeSpec.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XFileSpec.Builder.applyToKotlinPoet(block: KFileSpecBuilder.() -> Unit) = apply {
        if (this is XFileSpecImpl.Builder || this is KotlinFileSpec.Builder) {
            toKotlinPoet().block()
        }
    }

    @JvmStatic
    fun XName.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet()
            CodeLanguage.KOTLIN -> toKotlinPoet()
        }

    @JvmStatic
    fun XAnnotationSpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XCodeBlock.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XFunSpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XParameterSpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XPropertySpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XTypeSpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }

    @JvmStatic
    fun XFileSpec.toString(language: CodeLanguage) =
        when (language) {
            CodeLanguage.JAVA -> toJavaPoet().toString()
            CodeLanguage.KOTLIN -> toKotlinPoet().toString()
        }
}
