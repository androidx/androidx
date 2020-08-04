/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing.javac.kotlin

import androidx.room.compiler.processing.javac.JavacProcessingEnv
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import kotlin.reflect.KClass

@RunWith(JUnit4::class)
class KotlinMetadataElementTest {

    @Test
    fun getParameterNames() {
        simpleRun { processingEnv ->
            val (testClassElement, metadataElement) = getMetadataElement(
                processingEnv,
                TestData::class
            )
            assertThat(testClassElement.getDeclaredMethods()
                .first { it.simpleName.toString() == "functionWithParams" }
                .let { metadataElement.getParameterNames(it) }
            ).isEqualTo(
                listOf("param1", "yesOrNo", "number")
            )
        }
    }

    @Test
    fun findPrimaryConstructorSignature() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                TestData::class
            )
            assertThat(
                testClassElement.getConstructors().map {
                    val desc = it.descriptor()
                    desc to (desc == metadataElement.findPrimaryConstructorSignature())
                }
            ).containsExactly(
                "<init>(Ljava/lang/String;)V" to true,
                "<init>()V" to false
            )
        }
    }

    @Test
    fun isSuspendFunction() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                TestData::class
            )
            assertThat(testClassElement.getDeclaredMethods().map {
                it.simpleName.toString() to metadataElement.isSuspendFunction(it)
            }).containsExactly(
                "emptyFunction" to false,
                "suspendFunction" to true,
                "functionWithParams" to false,
                "getConstructorParam" to false
            )
        }
    }

    @Test
    fun isObject() {
        simpleRun { invocation ->
            val (_, objectTypeMetadata) = getMetadataElement(invocation, ObjectType::class)
            assertThat(objectTypeMetadata.isObject()).isTrue()
            val (_, testDataMetadata) = getMetadataElement(invocation, TestData::class)
            assertThat(testDataMetadata.isObject()).isFalse()
        }
    }

    private fun TypeElement.getDeclaredMethods() = ElementFilter.methodsIn(enclosedElements)

    private fun TypeElement.getConstructors() = ElementFilter.constructorsIn(enclosedElements)

    private fun simpleRun(
        handler: (ProcessingEnvironment) -> Unit
    ) {
        runProcessorTest {
            if (it.processingEnv !is JavacProcessingEnv) {
                throw AssumptionViolatedException("This test only works for java/kapt compilation")
            }
            handler(it.processingEnv.delegate)
        }
    }

    private fun getMetadataElement(processingEnv: ProcessingEnvironment, klass: KClass<*>) =
        processingEnv.elementUtils.getTypeElement(klass.java.canonicalName).let {
            it to KotlinMetadataElement.createFor(it)!!
        }

    @Suppress("unused")
    private class TestData(val constructorParam: String) {

        constructor() : this("anything")

        fun emptyFunction() {}

        @Suppress("RedundantSuspendModifier")
        suspend fun suspendFunction() {
        }

        @Suppress("UNUSED_PARAMETER")
        fun functionWithParams(param1: String, yesOrNo: Boolean, number: Int) {
        }
    }

    object ObjectType {
        val foo: String = ""
    }
}