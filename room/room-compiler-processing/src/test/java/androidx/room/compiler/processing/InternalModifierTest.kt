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

import androidx.room.compiler.processing.util.CompilationTestCapabilities
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.runKaptTest
import androidx.room.compiler.processing.util.runKspTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

class InternalModifierTest {
    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testInternalsAndInlines() {
        CompilationTestCapabilities.assumeKspIsEnabled()
        /**
         * parse same file w/ kapt and KSP and ensure results are the same.
         */
        val source = Source.kotlin(
            "Subject.kt",
            """
            package mainPackage;
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
            """.trimIndent()
        )

        fun XType.toSignature() = this.typeName.toString()

        fun XFieldElement.toSignature() = "$name : ${type.toSignature()}"

        fun XMethodElement.toSignature() = buildString {
            append(name)
            append("(")
            parameters.forEach {
                append(it.type.toSignature())
            }
            append(")")
            append(":")
            append(returnType.toSignature())
        }

        fun traverse(env: XProcessingEnv) = buildList<String> {
            val subject = env.requireTypeElement("mainPackage.Subject")
            add(subject.name)
            add(subject.qualifiedName)
            subject.getDeclaredMethods().forEach {
                add(it.toSignature())
            }
            subject.getAllFieldsIncludingPrivateSupers().map {
                add(it.toSignature())
            }
        }.sorted().joinToString("\n")

        var kaptResult: String = "pending"
        var kspResult: String = "pending"
        runKaptTest(
            sources = listOf(source)
        ) { invocation ->
            kaptResult = traverse(invocation.processingEnv)
        }

        runKspTest(
            sources = listOf(source)
        ) { invocation ->
            kspResult = traverse(invocation.processingEnv)
        }
        assertWithMessage("$kspResult\n--\n$kaptResult")
            .that(kspResult).isEqualTo(kaptResult)
    }
}