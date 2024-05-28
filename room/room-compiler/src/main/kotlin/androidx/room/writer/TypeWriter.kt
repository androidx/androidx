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

package androidx.room.writer

import androidx.room.RoomProcessor
import androidx.room.compiler.codegen.CodeLanguage
import androidx.room.compiler.codegen.VisibilityModifier
import androidx.room.compiler.codegen.XFunSpec
import androidx.room.compiler.codegen.XPropertySpec
import androidx.room.compiler.codegen.XTypeName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.apply
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.writeTo
import androidx.room.processor.Context
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.javapoet.JAnnotationSpec
import com.squareup.kotlinpoet.javapoet.JClassName
import com.squareup.kotlinpoet.javapoet.KAnnotationSpec
import com.squareup.kotlinpoet.javapoet.KClassName
import kotlin.reflect.KClass

/** Base class for all writers that can produce a class. */
abstract class TypeWriter(val context: WriterContext) {
    private val sharedFieldSpecs = mutableMapOf<String, XPropertySpec>()
    private val sharedMethodSpecs = mutableMapOf<String, XFunSpec>()
    private val sharedFieldNames = mutableSetOf<String>()
    private val sharedMethodNames = mutableSetOf<String>()

    private val metadata = mutableMapOf<KClass<*>, Any>()

    val codeLanguage: CodeLanguage = context.codeLanguage

    abstract fun createTypeSpecBuilder(): XTypeSpec.Builder

    /**
     * Read additional metadata that can be put by sub code generators.
     *
     * @see set for more details.
     */
    operator fun <T> get(key: KClass<*>): T? {
        @Suppress("UNCHECKED_CAST") return metadata[key] as? T
    }

    /**
     * Add additional metadata to the TypeWriter that can be read back later. This is useful for
     * additional functionality where a sub code generator needs to bubble up information to the
     * main TypeWriter without copying it in every intermediate step.
     *
     * @see get
     */
    operator fun set(key: KClass<*>, value: Any) {
        metadata[key] = value
    }

    fun write(processingEnv: XProcessingEnv) {
        val builder = createTypeSpecBuilder()
        sharedFieldSpecs.values.forEach { builder.addProperty(it) }
        sharedMethodSpecs.values.forEach { builder.addFunction(it) }
        addGeneratedAnnotationIfAvailable(builder, processingEnv)
        addSuppressWarnings(builder)
        builder.build().writeTo(processingEnv.filer)
    }

    private fun addSuppressWarnings(builder: XTypeSpec.Builder) {
        builder.apply(
            javaTypeBuilder = {
                addAnnotation(
                    com.squareup.javapoet.AnnotationSpec.builder(SuppressWarnings::class.java)
                        .addMember(
                            "value",
                            "{\$S, \$S, \$S}",
                            "unchecked",
                            "deprecation",
                            "removal"
                        )
                        .build()
                )
            },
            kotlinTypeBuilder = {
                addAnnotation(
                    com.squareup.kotlinpoet.AnnotationSpec.builder(Suppress::class)
                        .addMember(
                            "names = [%S, %S, %S, %S]",
                            "UNCHECKED_CAST",
                            "DEPRECATION",
                            "REDUNDANT_PROJECTION",
                            "REMOVAL"
                        )
                        .build()
                )
            }
        )
    }

    private fun addGeneratedAnnotationIfAvailable(
        adapterTypeSpecBuilder: XTypeSpec.Builder,
        processingEnv: XProcessingEnv
    ) {
        processingEnv.findGeneratedAnnotation()?.let {
            val annotationName = it.asClassName().canonicalName
            val memberValue = RoomProcessor::class.java.canonicalName
            adapterTypeSpecBuilder.apply(
                javaTypeBuilder = {
                    addAnnotation(
                        JAnnotationSpec.builder(JClassName.bestGuess(annotationName))
                            .addMember("value", "\$S", memberValue)
                            .build()
                    )
                },
                kotlinTypeBuilder = {
                    addAnnotation(
                        KAnnotationSpec.builder(KClassName.bestGuess(annotationName))
                            .addMember("value = [%S]", memberValue)
                            .build()
                    )
                }
            )
        }
    }

    private fun makeUnique(set: MutableSet<String>, value: String): String {
        if (!value.startsWith(CodeGenScope.CLASS_PROPERTY_PREFIX)) {
            return makeUnique(set, "${CodeGenScope.CLASS_PROPERTY_PREFIX}$value")
        }
        if (set.add(value)) {
            return value
        }
        var index = 1
        while (true) {
            if (set.add("${value}_$index")) {
                return "${value}_$index"
            }
            index++
        }
    }

    fun getOrCreateProperty(sharedProperty: SharedPropertySpec): XPropertySpec {
        return sharedFieldSpecs.getOrPut(sharedProperty.getUniqueKey()) {
            sharedProperty.build(this, makeUnique(sharedFieldNames, sharedProperty.baseName))
        }
    }

    fun getOrCreateFunction(sharedFunction: SharedFunctionSpec): XFunSpec {
        return sharedMethodSpecs.getOrPut(sharedFunction.getUniqueKey()) {
            sharedFunction.build(this, makeUnique(sharedMethodNames, sharedFunction.baseName))
        }
    }

    abstract class SharedPropertySpec(val baseName: String, val type: XTypeName) {

        open val isMutable = false

        abstract fun getUniqueKey(): String

        abstract fun prepare(writer: TypeWriter, builder: XPropertySpec.Builder)

        fun build(classWriter: TypeWriter, name: String): XPropertySpec {
            val builder =
                XPropertySpec.builder(
                    language = classWriter.codeLanguage,
                    name = name,
                    typeName = type,
                    visibility = VisibilityModifier.PRIVATE,
                    isMutable = isMutable
                )
            prepare(classWriter, builder)
            return builder.build()
        }
    }

    abstract class SharedFunctionSpec(val baseName: String) {

        abstract fun getUniqueKey(): String

        abstract fun prepare(methodName: String, writer: TypeWriter, builder: XFunSpec.Builder)

        fun build(writer: TypeWriter, name: String): XFunSpec {
            val builder = XFunSpec.builder(writer.codeLanguage, name, VisibilityModifier.PRIVATE)
            prepare(name, writer, builder)
            return builder.build()
        }
    }

    class WriterContext(
        val codeLanguage: CodeLanguage,
        val targetPlatforms: Set<XProcessingEnv.Platform>,
        val javaLambdaSyntaxAvailable: Boolean
    ) {
        companion object {
            fun fromProcessingContext(context: Context) =
                WriterContext(
                    codeLanguage = context.codeLanguage,
                    targetPlatforms = context.processingEnv.targetPlatforms,
                    javaLambdaSyntaxAvailable = context.javaLambdaSyntaxAvailable
                )
        }
    }
}
