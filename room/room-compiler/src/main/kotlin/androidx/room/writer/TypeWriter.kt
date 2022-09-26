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
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.apply
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.writeTo
import androidx.room.ext.S
import androidx.room.solver.CodeGenScope
import com.squareup.kotlinpoet.javapoet.JAnnotationSpec
import com.squareup.kotlinpoet.javapoet.KAnnotationSpec
import com.squareup.kotlinpoet.javapoet.toKClassName
import kotlin.reflect.KClass

/**
 * Base class for all writers that can produce a class.
 */
abstract class TypeWriter(val codeLanguage: CodeLanguage) {
    // TODO(danysantiago): Migrate to XPoet
    private val sharedFieldSpecs = mutableMapOf<String, com.squareup.javapoet.FieldSpec>()
    private val sharedMethodSpecs = mutableMapOf<String, com.squareup.javapoet.MethodSpec>()
    private val sharedFieldNames = mutableSetOf<String>()
    private val sharedMethodNames = mutableSetOf<String>()

    private val metadata = mutableMapOf<KClass<*>, Any>()

    abstract fun createTypeSpecBuilder(): XTypeSpec.Builder

    /**
     * Read additional metadata that can be put by sub code generators.
     *
     * @see set for more details.
     */
    operator fun <T> get(key: KClass<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return metadata[key] as? T
    }

    /**
     * Add additional metadata to the TypeWriter that can be read back later.
     * This is useful for additional functionality where a sub code generator needs to bubble up
     * information to the main TypeWriter without copying it in every intermediate step.
     *
     * @see get
     */
    operator fun set(key: KClass<*>, value: Any) {
        metadata[key] = value
    }

    fun write(processingEnv: XProcessingEnv) {
        val builder = createTypeSpecBuilder()
        builder.apply(
            javaTypeBuilder = {
                sharedFieldSpecs.values.forEach { addField(it) }
                sharedMethodSpecs.values.forEach { addMethod(it) }
            },
            kotlinTypeBuilder = { }
        )
        addGeneratedAnnotationIfAvailable(builder, processingEnv)
        addSuppressWarnings(builder)
        builder.build().writeTo(processingEnv.filer)
    }

    private fun addSuppressWarnings(builder: XTypeSpec.Builder) {
        val suppressionKeys = arrayOf("unchecked", "deprecation")
        builder.apply(
            javaTypeBuilder = {
                addAnnotation(
                    com.squareup.javapoet.AnnotationSpec.builder(SuppressWarnings::class.java)
                        .addMember("value", "{$S, $S}", *suppressionKeys)
                        .build()
                )
            },
            kotlinTypeBuilder = {
                addAnnotation(
                    com.squareup.kotlinpoet.AnnotationSpec.builder(Suppress::class)
                        .addMember("names = [%S, %S]", *suppressionKeys)
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
            val memberValue = RoomProcessor::class.java.canonicalName
            adapterTypeSpecBuilder.apply(
                javaTypeBuilder = {
                    addAnnotation(
                        JAnnotationSpec.builder(it.className)
                            .addMember("value", "$S", memberValue)
                            .build()
                    )
                },
                kotlinTypeBuilder = {
                    addAnnotation(
                        KAnnotationSpec.builder(it.className.toKClassName())
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

    fun getOrCreateField(sharedField: SharedFieldSpec): com.squareup.javapoet.FieldSpec {
        return sharedFieldSpecs.getOrPut(sharedField.getUniqueKey()) {
            sharedField.build(this, makeUnique(sharedFieldNames, sharedField.baseName))
        }
    }

    fun getOrCreateMethod(sharedMethod: SharedMethodSpec): com.squareup.javapoet.MethodSpec {
        return sharedMethodSpecs.getOrPut(sharedMethod.getUniqueKey()) {
            sharedMethod.build(this, makeUnique(sharedMethodNames, sharedMethod.baseName))
        }
    }

    abstract class SharedFieldSpec(val baseName: String, val type: com.squareup.javapoet.TypeName) {

        abstract fun getUniqueKey(): String

        abstract fun prepare(writer: TypeWriter, builder: com.squareup.javapoet.FieldSpec.Builder)

        fun build(classWriter: TypeWriter, name: String): com.squareup.javapoet.FieldSpec {
            val builder = com.squareup.javapoet.FieldSpec.builder(type, name)
            prepare(classWriter, builder)
            return builder.build()
        }
    }

    abstract class SharedMethodSpec(val baseName: String) {

        abstract fun getUniqueKey(): String

        abstract fun prepare(
            methodName: String,
            writer: TypeWriter,
            builder: com.squareup.javapoet.MethodSpec.Builder
        )

        fun build(writer: TypeWriter, name: String): com.squareup.javapoet.MethodSpec {
            val builder = com.squareup.javapoet.MethodSpec.methodBuilder(name)
            prepare(name, writer, builder)
            return builder.build()
        }
    }
}
