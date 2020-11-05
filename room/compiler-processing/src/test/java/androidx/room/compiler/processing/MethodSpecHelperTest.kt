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

package androidx.room.compiler.processing

import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runProcessorTest
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.JavaSourcesSubjectFactory
import com.squareup.javapoet.MethodSpec
import org.junit.Test
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.util.ElementFilter
import javax.lang.model.util.Types

class MethodSpecHelperTest {
    @Test
    fun overrides() {
        // check our override impl matches javapoet
        val source = Source.java(
            "foo.bar.Baz",
            """
            package foo.bar;
            import androidx.room.compiler.processing.testcode.OtherAnnotation;

            public class Baz {
                public void method1() {
                }

                public void method2(int x) {
                }

                public int parameterAnnotation(@OtherAnnotation("x") int y) {
                    return 3;
                }

                @OtherAnnotation("x")
                public int methodAnntation(int y) {
                    return 3;
                }

                public int varargMethod(int... y) {
                    return 3;
                }

                protected <R> R typeArgs(R r) {
                    return r;
                }

                protected void throwsException() throws Exception {
                }
            }
            """.trimIndent()
        )
        // first build golden image with Java processor so we can use JavaPoet's API
        val golden = buildMethodsViaJavaPoet(source)
        runProcessorTest(
            sources = listOf(source)
        ) {
            val element = it.processingEnv.requireTypeElement("foo.bar.Baz")
            element.getDeclaredMethods().forEachIndexed { index, method ->
                val subject = MethodSpecHelper.overridingWithFinalParams(
                    method,
                    element.type
                ).build().toString()
                assertThat(subject).isEqualTo(golden[index])
            }
        }
    }

    private fun buildMethodsViaJavaPoet(source: Source): List<String> {
        lateinit var result: List<String>
        assertAbout(
            JavaSourcesSubjectFactory.javaSources()
        ).that(
            listOf(source.toJFO())
        ).processedWith(
            object : AbstractProcessor() {
                override fun process(
                    annotations: MutableSet<out TypeElement>,
                    roundEnv: RoundEnvironment
                ): Boolean {
                    val element = processingEnv.elementUtils.getTypeElement("foo.bar.Baz")
                    result = ElementFilter.methodsIn(element.enclosedElements)
                        .map {
                            generateFromJavapoet(
                                it,
                                MoreTypes.asDeclared(element.asType()),
                                processingEnv.typeUtils
                            ).build().toString()
                        }
                    return true
                }

                override fun getSupportedSourceVersion(): SourceVersion {
                    return SourceVersion.latestSupported()
                }

                override fun getSupportedAnnotationTypes(): Set<String> {
                    return setOf("*")
                }
            }
        ).compilesWithoutError()
        return result
    }

    private fun generateFromJavapoet(
        method: ExecutableElement,
        owner: DeclaredType,
        typeUtils: Types
    ): MethodSpec.Builder {
        return overrideWithoutAnnotations(
            elm = method,
            owner = owner,
            typeUtils = typeUtils
        )
    }

    /**
     * Copied from DaoWriter for backwards compatibility
     */
    private fun overrideWithoutAnnotations(
        elm: ExecutableElement,
        owner: DeclaredType,
        typeUtils: Types
    ): MethodSpec.Builder {
        val baseSpec = MethodSpec.overriding(elm, owner, typeUtils)
            .build()

        // make all the params final
        val params = baseSpec.parameters.map { it.toBuilder().addModifiers(Modifier.FINAL).build() }

        return MethodSpec.methodBuilder(baseSpec.name).apply {
            addAnnotation(Override::class.java)
            addModifiers(baseSpec.modifiers)
            addParameters(params)
            addTypeVariables(baseSpec.typeVariables)
            addExceptions(baseSpec.exceptions)
            varargs(baseSpec.varargs)
            returns(baseSpec.returnType)
        }
    }
}
