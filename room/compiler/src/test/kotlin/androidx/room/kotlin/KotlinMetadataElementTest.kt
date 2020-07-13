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

import androidx.room.ext.asExecutableElement
import androidx.room.ext.getConstructors
import androidx.room.ext.getDeclaredMethods
import androidx.room.ext.name
import androidx.room.ext.requireTypeElement
import androidx.room.processor.Context
import androidx.room.testing.TestInvocation
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import simpleRun
import kotlin.reflect.KClass

@RunWith(JUnit4::class)
class KotlinMetadataElementTest {

    @Test
    fun getParameterNames() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                TestData::class
            )
            assertThat(testClassElement.getDeclaredMethods()
                .first { it.name == "functionWithParams" }
                .let { metadataElement.getParameterNames(it.asExecutableElement()) }
            ).isEqualTo(
                listOf("param1", "yesOrNo", "number")
            )
        }.compilesWithoutError()
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
                    val desc = it.asExecutableElement().descriptor()
                    desc to (desc == metadataElement.findPrimaryConstructorSignature())
                }
            ).containsExactly(
                "<init>(Ljava/lang/String;)V" to true,
                "<init>()V" to false
            )
        }.compilesWithoutError()
    }

    @Test
    fun isSuspendFunction() {
        simpleRun { invocation ->
            val (testClassElement, metadataElement) = getMetadataElement(
                invocation,
                TestData::class
            )
            assertThat(testClassElement.getDeclaredMethods().map {
                val executableElement = it.asExecutableElement()
                executableElement.name to metadataElement.isSuspendFunction(
                    executableElement
                )
            }).containsExactly(
                "emptyFunction" to false,
                "suspendFunction" to true,
                "functionWithParams" to false,
                "getConstructorParam" to false
            )
        }.compilesWithoutError()
    }

    @Test
    fun isObject() {
        simpleRun { invocation ->
            val (_, objectTypeMetadata) = getMetadataElement(invocation, ObjectType::class)
            assertThat(objectTypeMetadata.isObject()).isTrue()
            val (_, testDataMetadata) = getMetadataElement(invocation, TestData::class)
            assertThat(testDataMetadata.isObject()).isFalse()
        }.compilesWithoutError()
    }

    private fun getMetadataElement(invocation: TestInvocation, klass: KClass<*>) =
        invocation.processingEnv.requireTypeElement(klass).let {
            it to KotlinMetadataElement.createFor(Context(invocation.processingEnv), it)!!
        }

    @Suppress("unused")
    private class TestData(val constructorParam: String) {

        constructor() : this("anything")

        fun emptyFunction() {}

        suspend fun suspendFunction() {}

        @Suppress("UNUSED_PARAMETER")
        fun functionWithParams(param1: String, yesOrNo: Boolean, number: Int) {
        }
    }

    object ObjectType {
        val foo: String = ""
    }
}