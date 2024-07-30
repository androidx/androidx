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

package androidx.room.processor

import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.runProcessorTestWithK1
import androidx.room.testing.context
import androidx.room.vo.Entity
import java.io.File

abstract class BaseEntityParserTest {
    companion object {
        const val ENTITY_PREFIX =
            """
            package foo.bar;
            import androidx.room.*;
            import androidx.annotation.NonNull;
            import java.util.*;
            @Entity%s
            public class MyEntity %s {
            """
        const val ENTITY_SUFFIX = "}"
    }

    fun singleEntity(
        input: String,
        attributes: Map<String, String> = mapOf(),
        baseClass: String = "",
        sources: List<Source> = emptyList(),
        classpathFiles: List<File> = emptyList(),
        handler: (Entity, XTestInvocation) -> Unit
    ) {
        val attributesReplacement: String
        if (attributes.isEmpty()) {
            attributesReplacement = ""
        } else {
            attributesReplacement =
                "(" +
                    attributes.entries.joinToString(",") { "${it.key} = ${it.value}" } +
                    ")".trimIndent()
        }
        val baseClassReplacement: String
        if (baseClass == "") {
            baseClassReplacement = ""
        } else {
            baseClassReplacement = " extends $baseClass"
        }
        runProcessorTestWithK1(
            sources =
                sources +
                    Source.java(
                        qName = "foo.bar.MyEntity",
                        code =
                            ENTITY_PREFIX.format(attributesReplacement, baseClassReplacement) +
                                input +
                                ENTITY_SUFFIX
                    ),
            options = mapOf(Context.BooleanProcessorOptions.GENERATE_KOTLIN.argName to "false"),
            classpath = classpathFiles
        ) { invocation ->
            val entity =
                invocation.roundEnv
                    .getElementsAnnotatedWith(androidx.room.Entity::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .first { it.qualifiedName == "foo.bar.MyEntity" }
            val parser = TableEntityProcessor(invocation.context, entity)
            val parsedQuery = parser.process()
            handler(parsedQuery, invocation)
        }
    }
}
