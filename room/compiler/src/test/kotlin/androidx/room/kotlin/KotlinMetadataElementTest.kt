/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.room.kotlin

import androidx.room.processor.Context
import androidx.room.testing.TestInvocation
import com.google.auto.common.MoreElements
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import javax.lang.model.util.ElementFilter

@RunWith(JUnit4::class)
class KotlinMetadataElementTest {

    @Test
    fun getParameterNames() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(invocation)
            assertThat(ElementFilter.methodsIn(testClassElement.enclosedElements)
                .first { it.simpleName.toString() == "functionWithParams" }
                .let { metadataElement.getParameterNames(MoreElements.asExecutable(it)) }
            ).isEqualTo(
                listOf("param1", "yesOrNo", "number")
            )
        }
    }

    @Test
    fun findPrimaryConstructorSignature() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(invocation)
            assertThat(
                ElementFilter.constructorsIn(testClassElement.enclosedElements).map {
                    val desc = MoreElements.asExecutable(it).descriptor(invocation.typeUtils)
                    desc to (desc == metadataElement.findPrimaryConstructorSignature())
                }.toSet()
            ).isEqualTo(
                setOf(
                    "TestData(Ljava/lang/String;)Landroidx/room/kotlin/" +
                            "KotlinMetadataElementTest\$TestData" to true,
                    "TestData(Landroidx/room/kotlin/KotlinMetadataElementTest\$TestData" to false
                )
            )
        }
    }

    @Test
    fun isSuspendFunction() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(invocation)
            assertThat(ElementFilter.constructorsIn(testClassElement.enclosedElements).map {
                val executableElement = MoreElements.asExecutable(it)
                executableElement.simpleName.toString() to metadataElement.isSuspendFunction(
                    executableElement
                )
            }.toSet()).isEqualTo(
                setOf(
                    "emptyFunction" to false,
                    "suspendFunction" to true,
                    "functionWithParams" to false
                )
            )
        }
    }

    private fun getMetadataElement(invocation: TestInvocation) =
        invocation.typeElement(TestData::class.java.canonicalName).let {
            it to KotlinMetadataElement.createFor(Context(invocation.processingEnv), it)!!
        }

    private class TestData(val constructorParam: String) {

        constructor() : this("anything")

        fun emptyFunction() {}

        suspend fun suspendFunction() {}

        fun functionWithParams(param1: String, yesOrNo: Boolean, number: Int) {}
    }
}