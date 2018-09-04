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

import androidx.annotation.NonNull
import androidx.room.Embedded
import androidx.room.Fts3
import androidx.room.Fts4
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.FtsEntity
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import javax.tools.JavaFileObject

abstract class BaseFtsEntityParserTest {
    companion object {
        const val ENTITY_PREFIX = """
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
        jfos: List<JavaFileObject> = emptyList(),
        classLoader: ClassLoader = javaClass.classLoader,
        handler: (FtsEntity, TestInvocation) -> Unit
    ): CompileTester {
        val ftsVersion = getFtsVersion().toString()
        val entityAttributesReplacement = if (entityAttributes.isEmpty()) {
            ""
        } else {
            "(" + entityAttributes.entries.joinToString(",") { "${it.key} = ${it.value}" } + ")"
        }
        val ftsAttributesReplacement = if (ftsAttributes.isEmpty()) {
            ""
        } else {
            "(" + ftsAttributes.entries.joinToString(",") { "${it.key} = ${it.value}" } + ")"
        }
        val baseClassReplacement = if (baseClass == "") {
            ""
        } else {
            " extends $baseClass"
        }
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(jfos + JavaFileObjects.forSourceString("foo.bar.MyEntity",
                        ENTITY_PREFIX.format(entityAttributesReplacement, ftsVersion,
                                ftsAttributesReplacement, baseClassReplacement) + input +
                                ENTITY_SUFFIX))
                .withClasspathFrom(classLoader)
                .processedWith(TestProcessor.builder()
                        .forAnnotations(androidx.room.Entity::class,
                                Fts3::class,
                                Fts4::class,
                                androidx.room.PrimaryKey::class,
                                androidx.room.Ignore::class,
                                Embedded::class,
                                androidx.room.ColumnInfo::class,
                                NonNull::class)
                        .nextRunHandler { invocation ->
                            val fts3AnnotatedElements = invocation.roundEnv
                                    .getElementsAnnotatedWith(Fts3::class.java)
                            val fts4AnnotatedElements = invocation.roundEnv
                                    .getElementsAnnotatedWith(Fts4::class.java)
                            val entity = (fts3AnnotatedElements + fts4AnnotatedElements).first {
                                it.toString() == "foo.bar.MyEntity"
                            }
                            val processor = FtsTableEntityProcessor(invocation.context,
                                    MoreElements.asType(entity))
                            val processedEntity = processor.process()
                            handler(processedEntity, invocation)
                            true
                        }
                        .build())
    }

    abstract fun getFtsVersion(): Int
}