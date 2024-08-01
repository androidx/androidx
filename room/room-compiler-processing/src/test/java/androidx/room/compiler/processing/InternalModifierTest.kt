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

import androidx.kruth.assertThat
import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.KOTLINC_LANGUAGE_1_9_ARGS
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compileFiles
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runKspTest
import org.junit.Test

class InternalModifierTest {
    @Test
    fun testInternalsAndInlines_excludeInlines() {
        val signatures =
            buildSignatures(
                XProcessingEnvConfig.DEFAULT.copy(
                    excludeMethodsWithInvalidJvmSourceNames = true,
                )
            )
        assertThat(signatures.ksp).containsExactlyElementsIn(signatures.kapt)
    }

    @Test
    fun testInternalsAndInlines_includeInlines() {
        val signatures =
            buildSignatures(
                XProcessingEnvConfig.DEFAULT.copy(excludeMethodsWithInvalidJvmSourceNames = false)
            )
        // KAPT will always remove methods with invalid java source names when generating stubs
        // so we need to assert them manually.
        val nonJvmSourceSignatures =
            signatures.ksp.filter { it.startsWith("main.") && it.contains("-") }
        assertThat(signatures.ksp - nonJvmSourceSignatures)
            .containsExactlyElementsIn(signatures.kapt)
        assertThat(nonJvmSourceSignatures.map { it.substringBefore("-") })
            .containsExactly(
                "main.Subject.getInlineProp",
                "main.Subject.getInternalInlineProp",
                "main.Subject.inlineReceivingFun",
                "main.Subject.inlineReturningFun",
                "main.Subject.internalInlineReceivingFun",
                "main.Subject.internalInlineReturningFun",
                "main.Subject.setInlineProp",
                "main.Subject.setInternalInlineProp"
            )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun buildSignatures(config: XProcessingEnvConfig): Signatures {
        CompilationTestCapabilities.assumeKspIsEnabled()
        /** parse same file w/ kapt and KSP and ensure results are the same. */
        fun buildSource(pkg: String) =
            Source.kotlin(
                "Subject.kt",
                """
            package $pkg
            internal class InternalClass(val value: String)
            inline class InlineClass(val value:String)
            abstract class Subject {
                var normalProp: String = ""
                var inlineProp: InlineClass = InlineClass("")
                internal abstract var internalAbstractProp: String
                internal var internalProp: String = ""
                internal var internalInlineProp: InlineClass = InlineClass("")
                private var internalTypeProp : InternalClass = InternalClass("")
                @get:JvmName("explicitGetterName")
                @set:JvmName("explicitSetterName")
                var jvmNameProp:String = ""
                fun normalFun() {}
                @JvmName("explicitJvmName")
                fun hasJvmName() {}
                fun inlineReceivingFun(value: InlineClass) {}
                fun inlineReturningFun(): InlineClass = InlineClass("")
                internal fun internalInlineReceivingFun(value: InlineClass) {}
                internal fun internalInlineReturningFun(): InlineClass = InlineClass("")
                inline fun inlineFun() {
                }
            }
            """
                    .trimIndent()
            )

        fun XType.toSignature() = this.asTypeName().java.toString()

        fun XMemberContainer.toSignature() = asClassName().java.toString()

        fun XFieldElement.toSignature() =
            "${closestMemberContainer.toSignature()}.$name : ${type.toSignature()}"

        fun XMethodElement.toSignature() = buildString {
            append(closestMemberContainer.toSignature())
            append(".")
            append(jvmName)
            append("(")
            parameters.forEach { append(it.type.toSignature()) }
            append(")")
            append(":")
            append(returnType.toSignature())
        }

        fun traverse(env: XProcessingEnv) =
            buildList {
                    listOf("main.Subject", "lib.Subject").flatMap {
                        val subject = env.requireTypeElement(it)
                        add(subject.name)
                        add(subject.qualifiedName)
                        subject.getDeclaredMethods().forEach { add(it.toSignature()) }
                        subject.getAllFieldsIncludingPrivateSupers().map { add(it.toSignature()) }
                    }
                }
                .sorted()

        var kaptResult: List<String>? = null
        var kspResult: List<String>? = null
        val source = buildSource("main")
        val classpath = compileFiles(sources = listOf(buildSource("lib")))
        runKaptTest(
            sources = listOf(source),
            classpath = classpath,
            config = config,
        ) { invocation ->
            kaptResult = traverse(invocation.processingEnv)
        }

        runKspTest(
            sources = listOf(source),
            classpath = classpath,
            kotlincArguments = KOTLINC_LANGUAGE_1_9_ARGS,
            config = config,
        ) { invocation ->
            kspResult = traverse(invocation.processingEnv)
        }
        return Signatures(ksp = checkNotNull(kspResult), kapt = checkNotNull(kaptResult))
    }

    private data class Signatures(val ksp: List<String>, val kapt: List<String>)
}
