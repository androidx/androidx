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
import androidx.room3.compiler.processing.util.runProcessorTest
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
        runProcessorTest(sources = listOf(src), classpath = lib) {
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
        runProcessorTest(sources = listOf(src), classpath = lib) {
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

    @Test
    fun propertyAndFieldUseSiteAnnotationsTest() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                annotation class NoTargetAnno
                @Target(AnnotationTarget.FIELD)
                annotation class FieldTargetAnno
                @Target(AnnotationTarget.PROPERTY)
                annotation class PropTargetAnno

                annotation class FieldSiteAnno
                annotation class GetSiteAnno
                annotation class SetSiteAnno
                annotation class PropSiteAnno

                class Subject {
                    @NoTargetAnno
                    @FieldTargetAnno
                    @PropTargetAnno
                    @field:FieldSiteAnno
                    @get:GetSiteAnno
                    @set:SetSiteAnno
                    @property:PropSiteAnno
                    var myProp: Int = 0
                }
                """
                    .trimIndent(),
            )
        runProcessorTest(sources = listOf(src)) { invocation ->
            fun XAnnotated.hasAnno(name: String): Boolean {
                return getAllAnnotations().any { it.name == name }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val prop = subject.getDeclaredProperties().single { it.name == "myProp" }
            val field = prop.backingField

            assertThat(prop.hasAnno("PropSiteAnno")).isTrue()
            assertThat(prop.hasAnno("NoTargetAnno")).isTrue()
            assertThat(prop.hasAnno("PropTargetAnno")).isTrue()
            assertThat(prop.hasAnno("FieldSiteAnno")).isFalse()
            assertThat(prop.hasAnno("GetSiteAnno")).isFalse()
            assertThat(prop.hasAnno("SetSiteAnno")).isFalse()

            assertThat(field).isNotNull()
            assertThat(field!!.hasAnno("FieldSiteAnno")).isTrue()
            assertThat(field.hasAnno("FieldTargetAnno")).isTrue()
            assertThat(field.hasAnno("NoTargetAnno")).isFalse()
            assertThat(field.hasAnno("GetSiteAnno")).isFalse()
            assertThat(field.hasAnno("SetSiteAnno")).isFalse()
            assertThat(field.hasAnno("PropSiteAnno")).isFalse()

            val getter = prop.getter
            assertThat(getter).isNotNull()
            assertThat(getter!!.hasAnno("GetSiteAnno")).isTrue()
            assertThat(getter.hasAnno("FieldSiteAnno")).isFalse()
            assertThat(getter.hasAnno("SetSiteAnno")).isFalse()
            assertThat(getter.hasAnno("PropSiteAnno")).isFalse()

            val setter = prop.setter
            assertThat(setter).isNotNull()
            assertThat(setter!!.hasAnno("SetSiteAnno")).isTrue()
            assertThat(setter.hasAnno("FieldSiteAnno")).isFalse()
            assertThat(setter.hasAnno("GetSiteAnno")).isFalse()
            assertThat(setter.hasAnno("PropSiteAnno")).isFalse()
        }
    }

    @Test
    fun propertyAndFieldUseSiteAnnotationsOnJvmFieldTest() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                annotation class NoTargetAnno
                @Target(AnnotationTarget.FIELD)
                annotation class FieldTargetAnno
                @Target(AnnotationTarget.PROPERTY)
                annotation class PropTargetAnno

                annotation class FieldSiteAnno
                annotation class GetSiteAnno
                annotation class SetSiteAnno
                annotation class PropSiteAnno

                class Subject {
                    @JvmField
                    @NoTargetAnno
                    @FieldTargetAnno
                    @PropTargetAnno
                    @field:FieldSiteAnno
                    @get:GetSiteAnno
                    @set:SetSiteAnno
                    @property:PropSiteAnno
                    var myProp: Int = 0
                }
                """
                    .trimIndent(),
            )
        runProcessorTest(sources = listOf(src)) { invocation ->
            fun XAnnotated.hasAnno(name: String): Boolean {
                return getAllAnnotations().any { it.name == name }
            }

            val subject = invocation.processingEnv.requireTypeElement("Subject")
            val prop = subject.getDeclaredProperties().single { it.name == "myProp" }
            val field = prop.backingField

            assertThat(prop.hasAnno("PropSiteAnno")).isTrue()
            assertThat(prop.hasAnno("NoTargetAnno")).isTrue()
            assertThat(prop.hasAnno("PropTargetAnno")).isTrue()
            assertThat(prop.hasAnno("FieldSiteAnno")).isFalse()
            assertThat(prop.hasAnno("GetSiteAnno")).isFalse()
            assertThat(prop.hasAnno("SetSiteAnno")).isFalse()

            assertThat(field).isNotNull()
            assertThat(field!!.hasAnno("FieldSiteAnno")).isTrue()
            assertThat(field.hasAnno("FieldTargetAnno")).isTrue()
            assertThat(field.hasAnno("NoTargetAnno")).isFalse()
            assertThat(field.hasAnno("GetSiteAnno")).isFalse()
            assertThat(field.hasAnno("SetSiteAnno")).isFalse()
            assertThat(field.hasAnno("PropSiteAnno")).isFalse()
        }
    }

    @Test
    fun propertyAndFieldVisibilityTest() {
        val src =
            Source.kotlin(
                "Subject.kt",
                """
                class Subject {
                    var publicProp: String = "hello"

                    @JvmField
                    var jvmFieldProp: String = "hello"

                    lateinit var lateinitProp: String
                }
                """
                    .trimIndent(),
            )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val subject = invocation.processingEnv.requireTypeElement("Subject")

            // public property (backing field is private, property is public)
            val publicProp = subject.getDeclaredProperties().single { it.name == "publicProp" }
            assertThat(publicProp.isPublic()).isTrue()
            assertThat(publicProp.isPrivate()).isFalse()

            val publicField = publicProp.backingField
            assertThat(publicField).isNotNull()
            assertThat(publicField!!.isPrivate()).isTrue()
            assertThat(publicField.isPublic()).isFalse()

            // @JvmField property (backing field is public, property is public)
            val jvmFieldProp = subject.getDeclaredProperties().single { it.name == "jvmFieldProp" }
            assertThat(jvmFieldProp.isPublic()).isTrue()

            val jvmField = jvmFieldProp.backingField
            assertThat(jvmField).isNotNull()
            assertThat(jvmField!!.isPublic()).isTrue()
            assertThat(jvmField.isPrivate()).isFalse()

            // lateinit Property (backing field has setter visibility - public here)
            val lateinitProp = subject.getDeclaredProperties().single { it.name == "lateinitProp" }
            assertThat(lateinitProp.isPublic()).isTrue()

            val lateinitField = lateinitProp.backingField
            assertThat(lateinitField).isNotNull()
            assertThat(lateinitField!!.isPublic()).isTrue()
            assertThat(lateinitField.isPrivate()).isFalse()
        }
    }
}
