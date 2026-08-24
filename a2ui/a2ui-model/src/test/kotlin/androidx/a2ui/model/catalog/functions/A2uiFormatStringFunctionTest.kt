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

package androidx.a2ui.model.catalog.functions

import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiExecutionContext
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class A2uiFormatStringFunctionTest {

    @Test
    fun execute_withBasicString_returnsUnchanged() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "hello world"),
                createSpyContext(capturedPayloads),
            )
        assertThat(result).isEqualTo("hello world")
        assertThat(capturedPayloads).isEmpty()
    }

    @Test
    fun execute_withSimpleBinding_returnsResolvedBinding() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{foo}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to "foo") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("resolved_binding")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "foo"))
    }

    @Test
    fun execute_withMultipleBindings_returnsResolvedBindings() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "hello ${"$"}{foo} and ${"$"}{bar}!"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf("path" to "foo") to "resolved_binding_1",
                        mapOf("path" to "bar") to "resolved_binding_2",
                    ),
                ),
            )
        assertThat(result).isEqualTo("hello resolved_binding_1 and resolved_binding_2!")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "foo"), mapOf("path" to "bar"))
    }

    @Test
    fun execute_withEscapedBindingStart_returnsUnescapedLiteral() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """\${"$"}{foo}"""),
                createSpyContext(capturedPayloads),
            )
        assertThat(result).isEqualTo("${"$"}{foo}")
        assertThat(capturedPayloads).isEmpty()
    }

    @Test
    fun execute_withDoubleEscapedBindingStart_returnsResolvedBindingWithBackslash() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """\\${"$"}{foo}"""),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to "foo") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("""\resolved_binding""")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "foo"))
    }

    @Test
    fun execute_withTripleEscapedBindingStart_returnsUnescapedLiteralWithBackslash() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """\\\${"$"}{foo}"""),
                createSpyContext(capturedPayloads),
            )
        assertThat(result).isEqualTo("""\${"$"}{foo}""")
        assertThat(capturedPayloads).isEmpty()
    }

    @Test
    fun execute_withQuadrupleEscapedBindingStart_returnsResolvedBindingWithDoubleBackslash() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """\\\\${"$"}{foo}"""),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to "foo") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("""\\resolved_binding""")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "foo"))
    }

    @Test
    fun execute_withEscapedClosingBrace_returnsResolvedBindings() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """${"$"}{foo\}bar}"""),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to "foo}bar") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("resolved_binding")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "foo}bar"))
    }

    @Test
    fun execute_withDoubleEscapedClosingBrace_treatsAsLiteralBackslashAndActiveBrace() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """${"$"}{foo\\}"""),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to """foo\\""") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("resolved_binding")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to """foo\\"""))
    }

    @Test
    fun execute_withNestedBindings_returnsResolvedBindings() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{formatDate(value:${"$"}{/currentDate}, format:'MM-dd')}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf(
                            "call" to "formatDate",
                            "args" to
                                mapOf(
                                    "value" to mapOf("path" to "/currentDate"),
                                    "format" to "MM-dd",
                                ),
                        ) to "resolved_binding"
                    ),
                ),
            )
        assertThat(result).isEqualTo("resolved_binding")
        assertThat(capturedPayloads)
            .containsExactly(
                mapOf(
                    "call" to "formatDate",
                    "args" to mapOf("value" to mapOf("path" to "/currentDate"), "format" to "MM-dd"),
                )
            )
    }

    @Test
    fun execute_withUnfinishedBinding_returnsUnchanged() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "hello ${"$"}{world"),
                createSpyContext(capturedPayloads),
            )
        assertThat(result).isEqualTo("hello ${"$"}{world")
        assertThat(capturedPayloads).isEmpty()
    }

    @Test
    fun execute_withUnfinishedBindingWithEscapedEnd_returnsOriginal() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """hello ${"$"}{world\}"""),
                createSpyContext(capturedPayloads),
            )
        assertThat(result).isEqualTo("""hello ${"$"}{world\}""")
        assertThat(capturedPayloads).isEmpty()
    }

    @Test
    fun execute_withUnicodeIdentifier_returnsResolvedBindings() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{συνάρτηση()}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("call" to "συνάρτηση") to "resolved_unicode"),
                ),
            )
        assertThat(result).isEqualTo("resolved_unicode")
        assertThat(capturedPayloads).containsExactly(mapOf("call" to "συνάρτηση"))
    }

    @Test
    fun execute_withEscapedStartInsideBinding_resolvesCorrectly() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to """${"$"}{a\${"$"}{b\}}"""),
                createSpyContext(
                    capturedPayloads,
                    mapOf(mapOf("path" to "a\${b}") to "resolved_binding"),
                ),
            )
        assertThat(result).isEqualTo("resolved_binding")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "a\${b}"))
    }

    @Test
    fun execute_withNullLiteralInFunction_parsesNullAsNull() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{someFunc(arg:null)}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf("call" to "someFunc", "args" to mapOf("arg" to null)) to
                            "resolved_null_arg"
                    ),
                ),
            )
        assertThat(result).isEqualTo("resolved_null_arg")
        assertThat(capturedPayloads)
            .containsExactly(mapOf("call" to "someFunc", "args" to mapOf("arg" to null)))
    }

    @Test
    fun execute_withBracesInStringLiteral_balancesCorrectly() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{formatDate(format:'yyyy-MM-dd}')}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf("call" to "formatDate", "args" to mapOf("format" to "yyyy-MM-dd}")) to
                            "resolved_date"
                    ),
                ),
            )
        assertThat(result).isEqualTo("resolved_date")
    }

    @Test
    fun execute_withDoubleQuotes_parsesCorrectly() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{formatDate(format:\"yyyy-MM-dd\")}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf("call" to "formatDate", "args" to mapOf("format" to "yyyy-MM-dd")) to
                            "resolved_double_quotes"
                    ),
                ),
            )
        assertThat(result).isEqualTo("resolved_double_quotes")
    }

    @Test
    fun execute_withEscapesInStringLiteral_unescapesCorrectly() {
        val capturedPayloads = mutableListOf<Any?>()
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{print(text:'line1\\nline2\\ttab\\\'quote')}"),
                createSpyContext(
                    capturedPayloads,
                    mapOf(
                        mapOf(
                            "call" to "print",
                            "args" to mapOf("text" to "line1\nline2\ttab'quote"),
                        ) to "printed"
                    ),
                ),
            )
        assertThat(result).isEqualTo("printed")
    }

    @Test
    fun execute_withMalformedFunction_throwsRuntimeException() {
        val capturedPayloads = mutableListOf<Any?>()
        try {
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{someFunc(arg1, arg2:123)}"),
                createSpyContext(capturedPayloads),
            )
            org.junit.Assert.fail("Expected A2uiException.A2uiRuntimeException")
        } catch (e: androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException) {
            assertThat(e.message).contains("Malformed argument")
        }
    }

    @Test
    fun execute_withMissingClosingParenthesis_throwsRuntimeException() {
        val capturedPayloads = mutableListOf<Any?>()
        try {
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{someFunc(arg:123}"),
                createSpyContext(capturedPayloads),
            )
            org.junit.Assert.fail("Expected A2uiException.A2uiRuntimeException")
        } catch (e: androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException) {
            assertThat(e.message).contains("missing closing parenthesis")
        }
    }

    @Test
    fun execute_withRelativePathBinding_passesCorrectPayloadToEvaluatePayload() {
        val capturedPayloads = mutableListOf<Any?>()
        val context =
            createSpyContext(
                capturedPayloads = capturedPayloads,
                resolveMap = mapOf(mapOf("path" to "relative/path") to "resolved_relative"),
                dataPath = A2uiDataPath("/base/path"),
            )
        val result =
            A2uiFormatStringFunction.INSTANCE.execute(
                mapOf("value" to "${"$"}{relative/path}"),
                context,
            )
        assertThat(result).isEqualTo("resolved_relative")
        assertThat(capturedPayloads).containsExactly(mapOf("path" to "relative/path"))
    }

    private fun createSpyContext(
        capturedPayloads: MutableList<Any?>,
        resolveMap: Map<Any?, Any?> = emptyMap(),
        dataPath: A2uiDataPath = A2uiDataPath(""),
    ): A2uiExecutionContext {
        return object : A2uiExecutionContext {
            override val dataPath: A2uiDataPath = dataPath

            override fun evaluatePayload(payload: Any?): Any? {
                capturedPayloads.add(payload)
                return resolveMap[payload]
            }

            override fun executeFunction(name: String, args: Map<String, Any>): Any? = null

            override fun resolveValue(path: A2uiDataPath): Any? = null

            override fun <T : Any> getOrCreateFunctionScopedCache(
                functionDefinition: A2uiFunctionDefinition,
                factory: () -> T,
            ): T {
                return factory()
            }
        }
    }
}
