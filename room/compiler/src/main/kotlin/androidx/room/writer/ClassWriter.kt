/*
 * Copyright (C) 2016 The Android Open Source Project
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
import androidx.room.ext.S
import androidx.room.ext.typeName
import androidx.room.solver.CodeGenScope.Companion.CLASS_PROPERTY_PREFIX
import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import javax.annotation.processing.ProcessingEnvironment

/**
 * Base class for all writers that can produce a class.
 */
abstract class ClassWriter(private val className: ClassName) {
    private val sharedFieldSpecs = mutableMapOf<String, FieldSpec>()
    private val sharedMethodSpecs = mutableMapOf<String, MethodSpec>()
    private val sharedFieldNames = mutableSetOf<String>()
    private val sharedMethodNames = mutableSetOf<String>()

    abstract fun createTypeSpecBuilder(): TypeSpec.Builder

    fun write(processingEnv: ProcessingEnvironment) {
        val builder = createTypeSpecBuilder()
        sharedFieldSpecs.values.forEach { builder.addField(it) }
        sharedMethodSpecs.values.forEach { builder.addMethod(it) }
        addGeneratedAnnotationIfAvailable(builder, processingEnv)
        addSuppressUnchecked(builder)
        JavaFile.builder(className.packageName(), builder.build())
                .build()
                .writeTo(processingEnv.filer)
    }

    private fun addSuppressUnchecked(builder: TypeSpec.Builder) {
        val suppressSpec = AnnotationSpec.builder(SuppressWarnings::class.typeName()).addMember(
                "value",
                S,
                "unchecked"
        ).build()
        builder.addAnnotation(suppressSpec)
    }

    private fun addGeneratedAnnotationIfAvailable(adapterTypeSpecBuilder: TypeSpec.Builder,
                                                  processingEnv: ProcessingEnvironment) {
        val generatedAnnotationAvailable = processingEnv
                .elementUtils
                .getTypeElement("$GENERATED_PACKAGE.$GENERATED_NAME") != null
        if (generatedAnnotationAvailable) {
            val className = ClassName.get(GENERATED_PACKAGE, GENERATED_NAME)
            val generatedAnnotationSpec =
                    AnnotationSpec.builder(className).addMember(
                            "value",
                            S,
                            RoomProcessor::class.java.canonicalName).build()
            adapterTypeSpecBuilder.addAnnotation(generatedAnnotationSpec)
        }
    }

    private fun makeUnique(set: MutableSet<String>, value: String): String {
        if (!value.startsWith(CLASS_PROPERTY_PREFIX)) {
            return makeUnique(set, "$CLASS_PROPERTY_PREFIX$value")
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

    fun getOrCreateField(sharedField: SharedFieldSpec): FieldSpec {
        return sharedFieldSpecs.getOrPut(sharedField.getUniqueKey(), {
            sharedField.build(this, makeUnique(sharedFieldNames, sharedField.baseName))
        })
    }

    fun getOrCreateMethod(sharedMethod: SharedMethodSpec): MethodSpec {
        return sharedMethodSpecs.getOrPut(sharedMethod.getUniqueKey(), {
            sharedMethod.build(this, makeUnique(sharedMethodNames, sharedMethod.baseName))
        })
    }

    abstract class SharedFieldSpec(val baseName: String, val type: TypeName) {

        abstract fun getUniqueKey(): String

        abstract fun prepare(writer: ClassWriter, builder: FieldSpec.Builder)

        fun build(classWriter: ClassWriter, name: String): FieldSpec {
            val builder = FieldSpec.builder(type, name)
            prepare(classWriter, builder)
            return builder.build()
        }
    }

    abstract class SharedMethodSpec(val baseName: String) {

        abstract fun getUniqueKey(): String
        abstract fun prepare(methodName: String, writer: ClassWriter, builder: MethodSpec.Builder)

        fun build(writer: ClassWriter, name: String): MethodSpec {
            val builder = MethodSpec.methodBuilder(name)
            prepare(name, writer, builder)
            return builder.build()
        }
    }

    companion object {
        private const val GENERATED_PACKAGE = "javax.annotation"
        private const val GENERATED_NAME = "Generated"
    }
}
