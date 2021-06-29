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

package androidx.room.compiler.processing.ksp

import androidx.room.compiler.processing.XFieldElement
import androidx.room.compiler.processing.ksp.KspFieldElementTest.TestModifier.PRIVATE
import androidx.room.compiler.processing.ksp.KspFieldElementTest.TestModifier.PROTECTED
import androidx.room.compiler.processing.ksp.KspFieldElementTest.TestModifier.PUBLIC
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.XTestInvocation
import androidx.room.compiler.processing.util.className
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.getField
import androidx.room.compiler.processing.util.runProcessorTest
import androidx.room.compiler.processing.util.typeName
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeVariableName
import org.junit.Test

class KspFieldElementTest {

    @Test
    fun kotlinSrcModifiers() {
        runModifierTest(
            ModifierTestInput(
                qName = "Foo",
                source = Source.kotlin(
                    "Foo.kt",
                    """
                    class Foo {
                        val intField: Int = 0
                        @JvmField
                        val jvmField: Int = 0
                        protected val protectedField: Int = 0
                        @JvmField
                        protected val protectedJvmField: Int = 0
                        // Cannot add @JvmField to private property, compiler does not allow it.
                        // @JvmField
                        // private val privateJvmField: Int = 0
                    }
                    """.trimIndent()
                ),
                expected = mapOf(
                    "intField" to PRIVATE,
                    "jvmField" to PUBLIC,
                    "protectedField" to PRIVATE,
                    "protectedJvmField" to PROTECTED
                )
            )
        )
    }

    @Test
    fun javaSrcModifiers() {
        runModifierTest(
            ModifierTestInput(
                qName = "JavaClassWithFields",
                source = Source.java(
                    "JavaClassWithFields",
                    """
                    public class JavaClassWithFields {
                        public Long javaPublic;
                        protected Long javaProtected;
                        Long javaPackage;
                        private Long javaPrivate;
                    }
                    """.trimIndent()
                ),
                expected = mapOf(
                    "javaPublic" to PUBLIC,
                    "javaProtected" to PROTECTED,
                    "javaPackage" to null,
                    "javaPrivate" to PRIVATE
                )
            )
        )
    }

    @Test
    fun javaSrcModifiers_withAccessors() {
        runModifierTest(
            ModifierTestInput(
                qName = "JavaClassWithAccessors",
                source = Source.java(
                    "JavaClassWithAccessors",
                    """
                    public class JavaClassWithAccessors {
                        public int javaPublic;
                        protected int javaProtected;
                        int javaPackage;
                        private int javaPrivate;

                        public Long getJavaPublic() {
                            return 1L;
                        }
                        public void setJavaPublic(int value) {
                        }
                        public Long getJavaProtected() {
                            return 1L;
                        }
                        public void setJavaProtected(int value) {
                        }
                        public Long getJavaPackage() {
                            return 1L;
                        }
                        public void setJavaPackage(int value) {
                        }
                        public Long getJavaPrivate() {
                            return 1L;
                        }
                        public void setJavaPrivate(int value) {
                        }
                    }
                    """.trimIndent()
                ),
                expected = mapOf(
                    "javaPublic" to PUBLIC,
                    "javaProtected" to PROTECTED,
                    "javaPackage" to null,
                    "javaPrivate" to PRIVATE
                )
            )
        )
    }

    @Test
    fun asMemberOf() {
        val src = Source.kotlin(
            "Foo.kt",
            """
            open class Base<T, R> {
                val t : T = TODO()
                val listOfR : List<R> = TODO()
            }
            class Sub1 : Base<Int, String>()
            """.trimIndent()
        )
        runProcessorTest(sources = listOf(src)) { invocation ->
            val sub = invocation.processingEnv.requireTypeElement("Sub1")
            val base = invocation.processingEnv.requireTypeElement("Base")
            val t = base.getField("t")
            val listOfR = base.getField("listOfR")
            if (invocation.isKsp) {
                // KSP replaces unspecified type parameters with Any? while javac keeps them as is.
                // This might be an issue when detecting errors but besides that it should be OK.
                // It other words, it shouldn't be an issue in proper code. It will only have an
                // impact when Room wants to report `unbound generics` errors as we won't
                // recognize them and instead assume they were set as Object.
                // see: UNBOUND_GENERICS errors in ProcessorErrors)
                assertThat(t.type.typeName).isEqualTo(TypeName.OBJECT)
            } else {
                assertThat(t.type.typeName).isEqualTo(TypeVariableName.get("T"))
            }
            val typeVariableName = if (invocation.isKsp) {
                "E" // ksp reads from class declaration
            } else {
                "R" // javac reads from variable declaration
            }
            assertThat(listOfR.type.typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        TypeVariableName.get(typeVariableName)
                    )
                )

            assertThat(t.enclosingElement).isEqualTo(base)
            assertThat(listOfR.enclosingElement).isEqualTo(base)
            assertThat(t.asMemberOf(sub.type).typeName).isEqualTo(TypeName.INT.box())
            assertThat(listOfR.asMemberOf(sub.type).typeName)
                .isEqualTo(
                    ParameterizedTypeName.get(
                        List::class.className(),
                        String::class.typeName()
                    )
                )
        }
    }

    private fun runModifierTest(vararg inputs: ModifierTestInput) {
        // we'll run the test twice. once it is in source and once it is coming from a dependency.
        val sources = inputs.map(ModifierTestInput::source)
        runProcessorTest(
            sources = sources
        ) { invocation ->
            assertModifiers(invocation, inputs)
        }
        val classpath = compileFiles(sources)
        runProcessorTest(
            sources = emptyList(),
            classpath = classpath
        ) { invocation ->
            if (invocation.isKsp) {
                // TODO: https://github.com/google/ksp/issues/159
                // KSP has a bug in java package private modifier from .class files
                // for now, revert them to make the test happy
                assertModifiers(
                    invocation = invocation,
                    inputs = inputs.map {
                        it.copy(
                            expected = it.expected.mapValues { (_, modifier) ->
                                modifier ?: PUBLIC
                            }
                        )
                    }.toTypedArray()
                )
            } else {
                assertModifiers(invocation, inputs)
            }
        }
    }

    private fun assertModifiers(invocation: XTestInvocation, inputs: Array<out ModifierTestInput>) {
        inputs.forEach { input ->
            val element = invocation.processingEnv.requireTypeElement(input.qName)
            input.expected.forEach { (name, modifier) ->
                val field = element.getField(name)
                assertWithMessage("${input.qName}:$name")
                    .that(field.modifiers)
                    .containsExactlyElementsIn(
                        listOfNotNull(modifier)
                    )
                assertThat(field.enclosingElement).isEqualTo(element)
            }
        }
    }

    /**
     * To avoid any confusion while reading the code, we use another class instead of java's or
     * kotlin's modifiers (XProcessing does not have a modifier class).
     */
    private enum class TestModifier {
        PUBLIC,
        PRIVATE,
        PROTECTED,
    }

    private val XFieldElement.modifiers
        get() = sequence {
            if (isPrivate()) yield(PRIVATE)
            if (isProtected()) yield(PROTECTED)
            if (isPublic()) yield(PUBLIC)
        }.toList()

    private data class ModifierTestInput(
        val qName: String,
        val source: Source,
        val expected: Map<String, TestModifier?>
    )
}
