/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.processor

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.FtsEntity
import java.io.File

abstract class BaseFtsEntityParserTest {
    companion object {
        const val ENTITY_PREFIX =
            """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
            @Entity%s
            @Fts%s%s
            public class MyEntity %s {
            """
        const val ENTITY_SUFFIX = "}"
    }

    fun singleEntity(
        input: String,
        entityAttributes: Map<String, String> = mapOf(),
        ftsAttributes: Map<String, String> = mapOf(),
        baseClass: String = "",
        sources: List<Source> = emptyList(),
        classpath: List<File> = emptyList(),
        handler: (FtsEntity, XTestInvocation) -> Unit
    ) {
        val ftsVersion = getFtsVersion().toString()
        val entityAttributesReplacement =
            if (entityAttributes.isEmpty()) {
                ""
            } else {
                "(" + entityAttributes.entries.joinToString(",") { "${it.key} = ${it.value}" } + ")"
            }
        val ftsAttributesReplacement =
            if (ftsAttributes.isEmpty()) {
                ""
            } else {
                "(" + ftsAttributes.entries.joinToString(",") { "${it.key} = ${it.value}" } + ")"
            }
        val baseClassReplacement =
            if (baseClass == "") {
                ""
            } else {
                " extends $baseClass"
            }
        val entitySource =
            Source.java(
                "foo.bar.MyEntity",
                ENTITY_PREFIX.format(
                    entityAttributesReplacement,
                    ftsVersion,
                    ftsAttributesReplacement,
                    baseClassReplacement
                ) + input + ENTITY_SUFFIX
            )
        runProcessorTestWithK1(sources = sources + entitySource, classpath = classpath) { invocation
            ->
            val entity = invocation.processingEnv.requireTypeElement("foo.bar.MyEntity")
            val processor = FtsTableEntityProcessor(invocation.context, entity)
            val processedEntity = processor.process()
            handler(processedEntity, invocation)
        }
    }

    abstract fun getFtsVersion(): Int
}
