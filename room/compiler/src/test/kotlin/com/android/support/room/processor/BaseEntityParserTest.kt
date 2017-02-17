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

package com.android.support.room.processor

import com.android.support.room.testing.TestInvocation
import com.android.support.room.testing.TestProcessor
import com.android.support.room.vo.Entity
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory

abstract class BaseEntityParserTest {
    companion object {
        const val ENTITY_PREFIX = """
            package foo.bar;
            import com.android.support.room.*;
            @Entity%s
            public class MyEntity {
            """
        const val ENTITY_SUFFIX = "}"
    }

    fun singleEntity(input: String, attributes: Map<String, String> = mapOf(),
                     handler: (Entity, TestInvocation) -> Unit): CompileTester {
        val attributesReplacement : String
        if (attributes.isEmpty()) {
            attributesReplacement = ""
        } else {
            attributesReplacement = "(" +
                    attributes.entries.map { "${it.key} = ${it.value}" }.joinToString(",") +
                    ")".trimIndent()
        }
        return Truth.assertAbout(JavaSourceSubjectFactory.javaSource())
                .that(JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX.format(attributesReplacement) + input + ENTITY_SUFFIX
                ))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(com.android.support.room.Entity::class,
                                com.android.support.room.PrimaryKey::class,
                                com.android.support.room.Ignore::class,
                                com.android.support.room.Decompose::class)
                        .nextRunHandler { invocation ->
                            val entity = invocation.roundEnv
                                    .getElementsAnnotatedWith(
                                            com.android.support.room.Entity::class.java)
                                    .first()
                            val parser = EntityProcessor(invocation.context,
                                    MoreElements.asType(entity))
                            val parsedQuery = parser.process()
                            handler(parsedQuery, invocation)
                            true
                        }
                        .build())
    }
}
