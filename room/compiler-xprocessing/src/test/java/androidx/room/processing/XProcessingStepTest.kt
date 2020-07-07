/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.room.processing

import androidx.room.processing.testcode.MainAnnotation
import androidx.room.processing.testcode.OtherAnnotation
import com.google.auto.common.BasicAnnotationProcessor
import com.google.common.truth.Truth
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.junit.Test
import kotlin.reflect.KClass

class XProcessingStepTest {
    @Test
    fun xProcessingStep() {
        val annotatedElements = mutableMapOf<KClass<out Annotation>, String>()
        val processingStep = object : XProcessingStep {
            override fun process(
                env: XProcessingEnv,
                elementsByAnnotation: Map<KClass<out Annotation>, List<XTypeElement>>
            ): Set<XTypeElement> {
                elementsByAnnotation[OtherAnnotation::class]?.forEach {
                    annotatedElements[OtherAnnotation::class] = it.qualifiedName
                }
                elementsByAnnotation[MainAnnotation::class]?.forEach {
                    annotatedElements[MainAnnotation::class] = it.qualifiedName
                }
                return emptySet()
            }

            override fun annotations(): Set<KClass<out Annotation>> {
                return setOf(OtherAnnotation::class, MainAnnotation::class)
            }
        }
        val mainProcessor = object : BasicAnnotationProcessor() {
            override fun initSteps(): Iterable<ProcessingStep> {
                return listOf(
                    processingStep.asAutoValueProcessor(
                        XProcessingEnv.create(processingEnv)
                    )
                )
            }
        }
        val main = JavaFileObjects.forSourceString(
            "foo.bar.Main", """
            package foo.bar;
            import androidx.room.processing.testcode.*;
            @MainAnnotation(
                typeList = {},
                singleType = Object.class,
                intMethod = 3,
                singleOtherAnnotation = @OtherAnnotation("y")
            )
            class Main {
            }
        """.trimIndent()
        )
        val other = JavaFileObjects.forSourceString(
            "foo.bar.Other", """
            package foo.bar;
            import androidx.room.processing.testcode.*;
            @OtherAnnotation("x")
            class Other {
            }
        """.trimIndent()
        )
        Truth.assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(main, other)
        ).processedWith(
            mainProcessor
        ).compilesWithoutError()
        Truth.assertThat(annotatedElements).containsExactlyEntriesIn(
            mapOf(
                MainAnnotation::class to "foo.bar.Main",
                OtherAnnotation::class to "foo.bar.Other"
            )
        )
    }
}