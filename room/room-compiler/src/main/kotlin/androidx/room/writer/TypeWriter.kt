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
import androidx.room.compiler.codegen.XClassName
import androidx.room.compiler.codegen.XTypeSpec
import androidx.room.compiler.codegen.XTypeSpec.Builder.Companion.apply
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.writeTo
import androidx.room.ext.S
import com.squareup.kotlinpoet.javapoet.toKClassName
import kotlin.reflect.KClass

/**
 * Base class for all writers that can produce a class.
 */
abstract class TypeWriter(private val className: XClassName) {
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
     * Add additional metadata to the ClassWriter that can be read back later.
     * This is useful for additional functionality where a sub code generator needs to bubble up
     * information to the main ClassWriter without copying it in every intermediate step.
     *
     * @see get
     */
    operator fun set(key: KClass<*>, value: Any) {
        metadata[key] = value
    }

    fun write(processingEnv: XProcessingEnv) {
        val builder = createTypeSpecBuilder()
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
                        com.squareup.javapoet.AnnotationSpec.builder(it.className)
                            .addMember("value", "$S", memberValue)
                            .build()
                    )
                },
                kotlinTypeBuilder = {
                    addAnnotation(
                        com.squareup.kotlinpoet.AnnotationSpec.builder(it.className.toKClassName())
                            .addMember("value = [%S]", memberValue)
                            .build()
                    )
                }
            )
        }
    }
}
