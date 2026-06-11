/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.room3.compiler.processing

import androidx.kruth.assertThat
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.compileFiles
import androidx.room3.compiler.processing.util.runKspTest
import org.junit.Test

class XPropertyElementTest {

    @Test
    fun compiledPropertyTest() {
        val depSrc =
            Source.kotlin(
                "LibClass.kt",
                """
                data class LibClass(
                    val id: Long,
                    val name: String,
                )
                """
                    .trimIndent(),
            )
        val lib = compileFiles(listOf(depSrc))
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                class Subject(val lib: LibClass)
                """
                    .trimIndent(),
            )
        runKspTest(sources = listOf(src), classpath = lib) {
            val nestedTypeElement =
                it.processingEnv
                    .requireTypeElement("Subject")
                    .findPrimaryConstructor()!!
                    .parameters
                    .single()
                    .type
                    .typeElement
            val fieldNames = nestedTypeElement!!.getDeclaredProperties().map { it.name }
            assertThat(fieldNames).containsExactly("id", "name")
        }
    }

    // To validate b/521861812 and https://github.com/google/ksp/issues/2823, issues related to
    // getDeclaredFields() not finding fields in compiled dependencies.
    @Test
    fun compiledNestedPropertyTest() {
        val depSrc =
            Source.kotlin(
                "LibClass.kt",
                """
                class LibClass {
                  data class NestedClass(
                    val id: Long,
                    val name: String,
                  )
                }
                """
                    .trimIndent(),
            )
        val lib = compileFiles(listOf(depSrc))
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                class Subject(val nested: LibClass.NestedClass)
                """
                    .trimIndent(),
            )
        runKspTest(sources = listOf(src), classpath = lib) {
            val nestedTypeElement =
                it.processingEnv
                    .requireTypeElement("Subject")
                    .findPrimaryConstructor()!!
                    .parameters
                    .single()
                    .type
                    .typeElement
            val fieldNames = nestedTypeElement!!.getDeclaredProperties().map { it.name }
            assertThat(fieldNames).containsExactly("id", "name")
        }
    }

    @Test
    fun propertiesWithoutBackingFieldTest() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                interface MyInterface {
                    val interfaceProp: String
                }
                abstract class MyAbstractClass : MyInterface {
                    abstract override val interfaceProp: String
                    val noBackingFieldProp: Int
                        get() = 42
                    var noBackingFieldVarProp: String
                        get() = "hello"
                        set(value) {}
                    val backingFieldProp: Double = 3.14
                }
                """
                    .trimIndent(),
            )
        runKspTest(sources = listOf(src)) { invocation ->
            val myInterface = invocation.processingEnv.requireTypeElement("MyInterface")
            val interfaceProperties = myInterface.getDeclaredProperties()
            assertThat(interfaceProperties.map { it.name }).containsExactly("interfaceProp")
            val interfaceProp = interfaceProperties.single()
            assertThat(interfaceProp.getter).isNotNull()
            assertThat(interfaceProp.getter!!.name).isEqualTo("getInterfaceProp")
            assertThat(interfaceProp.setter).isNull()

            val myAbstractClass = invocation.processingEnv.requireTypeElement("MyAbstractClass")
            val declaredProperties = myAbstractClass.getDeclaredProperties()
            assertThat(declaredProperties.map { it.name })
                .containsExactly(
                    "interfaceProp",
                    "noBackingFieldProp",
                    "noBackingFieldVarProp",
                    "backingFieldProp",
                )

            val noBackingFieldProp = declaredProperties.first { it.name == "noBackingFieldProp" }
            assertThat(noBackingFieldProp.getter).isNotNull()
            assertThat(noBackingFieldProp.getter!!.name).isEqualTo("getNoBackingFieldProp")
            assertThat(noBackingFieldProp.setter).isNull()

            val noBackingFieldVarProp =
                declaredProperties.first { it.name == "noBackingFieldVarProp" }
            assertThat(noBackingFieldVarProp.getter).isNotNull()
            assertThat(noBackingFieldVarProp.getter!!.name).isEqualTo("getNoBackingFieldVarProp")
            assertThat(noBackingFieldVarProp.setter).isNotNull()
            assertThat(noBackingFieldVarProp.setter!!.name).isEqualTo("setNoBackingFieldVarProp")

            val backingFieldProp = declaredProperties.first { it.name == "backingFieldProp" }
            assertThat(backingFieldProp.getter).isNotNull()
            assertThat(backingFieldProp.getter!!.name).isEqualTo("getBackingFieldProp")
            assertThat(backingFieldProp.setter).isNull()
        }
    }

    @Test
    fun compiledPropertiesWithoutBackingFieldTest() {
        val depSrc =
            Source.kotlin(
                "LibClass.kt",
                """
                interface LibInterface {
                    val interfaceProp: String
                }
                abstract class LibClass : LibInterface {
                    abstract override val interfaceProp: String
                    val noBackingFieldProp: Int
                        get() = 42
                    var noBackingFieldVarProp: String
                        get() = "hello"
                        set(value) {}
                    val backingFieldProp: Double = 3.14
                }
                """
                    .trimIndent(),
            )
        val lib = compileFiles(listOf(depSrc))
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                class Subject(val lib: LibClass)
                """
                    .trimIndent(),
            )
        runKspTest(sources = listOf(src), classpath = lib) { invocation ->
            val nestedTypeElement =
                invocation.processingEnv
                    .requireTypeElement("Subject")
                    .findPrimaryConstructor()!!
                    .parameters
                    .single()
                    .type
                    .typeElement
            val declaredProperties = nestedTypeElement!!.getDeclaredProperties()
            assertThat(declaredProperties.map { it.name })
                .containsExactly(
                    "interfaceProp",
                    "noBackingFieldProp",
                    "noBackingFieldVarProp",
                    "backingFieldProp",
                )

            val noBackingFieldProp = declaredProperties.first { it.name == "noBackingFieldProp" }
            assertThat(noBackingFieldProp.getter).isNotNull()
            assertThat(noBackingFieldProp.getter!!.name).isEqualTo("getNoBackingFieldProp")
            assertThat(noBackingFieldProp.setter).isNull()

            val noBackingFieldVarProp =
                declaredProperties.first { it.name == "noBackingFieldVarProp" }
            assertThat(noBackingFieldVarProp.getter).isNotNull()
            assertThat(noBackingFieldVarProp.getter!!.name).isEqualTo("getNoBackingFieldVarProp")
            assertThat(noBackingFieldVarProp.setter).isNotNull()
            assertThat(noBackingFieldVarProp.setter!!.name).isEqualTo("setNoBackingFieldVarProp")

            val backingFieldProp = declaredProperties.first { it.name == "backingFieldProp" }
            assertThat(backingFieldProp.getter).isNotNull()
            assertThat(backingFieldProp.getter!!.name).isEqualTo("getBackingFieldProp")
            assertThat(backingFieldProp.setter).isNull()
        }
    }
}
