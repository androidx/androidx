/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.room.compiler.processing.XAnnotation
import androidx.room.compiler.processing.XAnnotationValue
import androidx.room.compiler.processing.XElement
import androidx.room.compiler.processing.XMessager
import androidx.room.compiler.processing.addOriginatingElement
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertThat
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.symbol.KSFile
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.junit.Test
import java.io.File
import java.io.OutputStream
import javax.tools.Diagnostic

class KspFilerTest {

    @Test
    fun originatingFileAddedForTopLevelFunction() {
        runKspTest(sources = listOf(simpleKotlinClass)) { invocation ->
            val sourceElement = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val fileWithTopLevelFun = FileSpec.builder("foo", "Bar.kt").apply {
                addFunction(FunSpec.builder("baz").addOriginatingElement(sourceElement).build())
            }.build()

            val codeGenerator = DependencyTrackingCodeGenerator()
            KspFiler(codeGenerator, TestMessager()).write(fileWithTopLevelFun)
            codeGenerator.fileDependencies[fileWithTopLevelFun.name]
                .containsExactlySimpleKotlinClass()
        }
    }

    @Test
    fun originatingFileAddedForTopLevelProperty() {
        runKspTest(sources = listOf(simpleKotlinClass)) { invocation ->
            val sourceElement = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val fileWithTopLevelProp = FileSpec.builder("foo", "Bar.kt").apply {
                addProperty(
                    PropertySpec.builder("baz", String::class).apply {
                        initializer("%S", "")
                        addOriginatingElement(sourceElement)
                    }.build()
                )
            }.build()

            val codeGenerator = DependencyTrackingCodeGenerator()
            KspFiler(codeGenerator, TestMessager()).write(fileWithTopLevelProp)
            codeGenerator.fileDependencies[fileWithTopLevelProp.name]
                .containsExactlySimpleKotlinClass()
        }
    }

    @Test
    fun originatingFileAddedForTopLevelElement() {
        runKspTest(sources = listOf(simpleKotlinClass)) { invocation ->
            val sourceElement = invocation.processingEnv.requireTypeElement("foo.bar.Baz")
            val fileWithType = FileSpec.builder("foo", "Bar.kt").apply {
                addType(
                    TypeSpec.classBuilder("Bar").apply {
                        addOriginatingElement(sourceElement)
                    }.build()
                )
            }.build()

            val codeGenerator = DependencyTrackingCodeGenerator()
            KspFiler(codeGenerator, TestMessager()).write(fileWithType)
            codeGenerator.fileDependencies[fileWithType.name]
                .containsExactlySimpleKotlinClass()
        }
    }

    private fun Dependencies?.containsExactlySimpleKotlinClass() {
        assertThat(this).isNotNull()
        val originatingFiles = this!!.originatingFiles.map { it.fileName }
        assertThat(originatingFiles).containsExactly("Baz.kt")
    }

    class TestMessager : XMessager() {
        override fun onPrintMessage(
            kind: Diagnostic.Kind,
            msg: String,
            element: XElement?,
            annotation: XAnnotation?,
            annotationValue: XAnnotationValue?
        ) {
            var errorMsg = "${kind.name} element: $element " +
                "annotation: $annotation " +
                "annotationValue: $annotationValue " +
                "msg: $msg"
            if (kind == Diagnostic.Kind.ERROR) {
                error(errorMsg)
            } else {
                println(errorMsg)
            }
        }
    }

    class DependencyTrackingCodeGenerator : CodeGenerator {

        val fileDependencies = mutableMapOf<String, Dependencies>()

        override val generatedFile: Collection<File>
            get() = emptyList()

        override fun associate(
            sources: List<KSFile>,
            packageName: String,
            fileName: String,
            extensionName: String
        ) {
            // no-op for the sake of dependency tracking.
        }

        override fun createNewFile(
            dependencies: Dependencies,
            packageName: String,
            fileName: String,
            extensionName: String
        ): OutputStream {
            fileDependencies[fileName] = dependencies
            return OutputStream.nullOutputStream()
        }
    }

    companion object {
        val simpleKotlinClass = Source.kotlin(
            "Baz.kt",
            """
                package foo.bar;

                class Baz
            """.trimIndent()
        )
    }
}
