/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.expression.util

import android.icu.util.ULocale
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicBool
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicType
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import com.google.common.truth.Truth.assertWithMessage
import java.time.Instant
import java.util.IllegalFormatConversionException
import java.util.MissingFormatArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters

@RunWith(ParameterizedRobolectricTestRunner::class)
class DynamicFormatterFormatTest(private val case: Case) {
    enum class Case(
        val expected: String,
        val format: String,
        val args: List<Any?>? = null,
        val dynamicArgs: List<DynamicType>? = null,
        val equivalentArgs: List<Any?>? = null,
    ) {
        CONSTANT_ONLY("hello world", "hello world", args = listOf()),
        NON_DYNAMIC_ARGS("hello world", "hello %s", args = listOf("world")),
        EXPLICIT_ARG_INDEX("12 34 56 56", "%d %3\$d %d %<d", args = listOf(12, 56, 34)),
        DYNAMIC_START(
            "hello world",
            "%s world",
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
        ),
        DYNAMIC_MIDDLE(
            "hello world",
            "hel%srld",
            dynamicArgs = listOf(DynamicString.constant("lo wo")),
            equivalentArgs = listOf("lo wo"),
        ),
        DYNAMIC_END(
            "hello world",
            "hello %s",
            dynamicArgs = listOf(DynamicString.constant("world")),
            equivalentArgs = listOf("world"),
        ),
        SEPARATED_DYNAMIC(
            "hello world",
            "%s %s",
            dynamicArgs = listOf(DynamicString.constant("hello"), DynamicString.constant("world")),
            equivalentArgs = listOf("hello", "world"),
        ),
        CONNECTED_DYNAMIC(
            "helloworld",
            "%s%s",
            dynamicArgs = listOf(DynamicString.constant("hello"), DynamicString.constant("world")),
            equivalentArgs = listOf("hello", "world"),
        ),
        // %%
        FORMAT_percent_DYNAMIC_TYPE(
            "%",
            "%%",
            // args are ignored
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
        ),
        // %%
        FORMAT_n_DYNAMIC_TYPE(
            "\n",
            "%n",
            // args are ignored
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
        ),
        // %s
        FORMAT_s_DYNAMIC_STRING(
            "ab",
            "%s",
            dynamicArgs = listOf(DynamicString.constant("ab")),
            equivalentArgs = listOf("ab"),
        ),
        FORMAT_s_DYNAMIC_INT32(
            "12",
            "%s",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
        ),
        FORMAT_s_DYNAMIC_FLOAT(
            "12.0",
            "%s",
            dynamicArgs = listOf(DynamicFloat.constant(12f)),
            equivalentArgs = listOf(12f),
        ),
        FORMAT_s_DYNAMIC_BOOL_TRUE(
            "true",
            "%s",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
        ),
        FORMAT_s_DYNAMIC_BOOL_FALSE(
            "false",
            "%s",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
        ),
        // %S
        FORMAT_S_DYNAMIC_INT32(
            "12",
            "%S",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
        ),
        FORMAT_S_DYNAMIC_FLOAT(
            "12.0",
            "%S",
            dynamicArgs = listOf(DynamicFloat.constant(12f)),
            equivalentArgs = listOf(12f),
        ),
        FORMAT_S_DYNAMIC_BOOL_TRUE(
            "TRUE",
            "%S",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
        ),
        FORMAT_S_DYNAMIC_BOOL_FALSE(
            "FALSE",
            "%S",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
        ),
        // %b
        FORMAT_b_DYNAMIC_BOOL_TRUE(
            "true",
            "%b",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
        ),
        FORMAT_b_DYNAMIC_BOOL_FALSE(
            "false",
            "%b",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
        ),
        FORMAT_b_DYNAMIC_TYPE_IS_TRUE(
            "true",
            "%b",
            dynamicArgs = listOf(DYNAMIC_INSTANT),
            equivalentArgs = listOf(INSTANT),
        ),
        // %B
        FORMAT_B_DYNAMIC_BOOL_TRUE(
            "TRUE",
            "%B",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
        ),
        FORMAT_B_DYNAMIC_BOOL_FALSE(
            "FALSE",
            "%B",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
        ),
        FORMAT_B_DYNAMIC_TYPE_IS_TRUE(
            "TRUE",
            "%B",
            dynamicArgs = listOf(DYNAMIC_INSTANT),
            equivalentArgs = listOf(INSTANT),
        ),
        // %d
        FORMAT_d_DYNAMIC_INT32(
            "12",
            "%d",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
        ),
        // %f
        FORMAT_f_DYNAMIC_FLOAT(
            "12.345000 34.57 56.70",
            "%f %.2f %.2f",
            dynamicArgs =
                listOf(
                    DynamicFloat.constant(12.345f), // default fraction digits
                    DynamicFloat.constant(34.567f), // max fraction digits
                    DynamicFloat.constant(56.7f), // min fraction digits
                ),
            equivalentArgs = listOf(12.345f, 34.567f, 56.7f),
        ),
    }

    @Test
    fun equalsExpected() {
        val args = (case.args ?: case.dynamicArgs)!!.toTypedArray()
        assertWithMessage(case.name)
            .that(DynamicString.format(case.format, *args).evaluate())
            .isEqualTo(case.expected)
    }

    @Test
    fun equalsDefault() {
        val dynamicArgs = (case.args ?: case.dynamicArgs)!!.toTypedArray()
        val equivalentArgs = (case.args ?: case.equivalentArgs)!!.toTypedArray()
        assertWithMessage(case.name)
            .that(DynamicString.format(case.format, *dynamicArgs).evaluate())
            .isEqualTo(case.format.format(*equivalentArgs))
    }

    private fun DynamicString.evaluate(): String {
        var result = ""
        DynamicTypeEvaluator(DynamicTypeEvaluator.Config.Builder().build())
            .bind(
                DynamicTypeBindingRequest.forDynamicString(
                    this,
                    ULocale.ENGLISH,
                    { it.run() },
                    object : DynamicTypeValueReceiver<String> {
                        override fun onData(newData: String) {
                            result = newData
                        }

                        override fun onInvalidated() {
                            throw IllegalStateException("onInvalidated")
                        }
                    }
                )
            )
            .startEvaluation()
        return result
    }

    companion object {
        @Parameters @JvmStatic fun parameters() = Case.values()
    }
}

@RunWith(ParameterizedRobolectricTestRunner::class)
class DynamicFormatterFailingTest(private val case: Case) {
    enum class Case(val expected: Exception, val format: String, val args: List<Any?> = listOf()) {
        NOT_ENOUGH_POSITIONAL_ARGS(
            MissingFormatArgumentException("Format specifier '%s'"),
            "%s %s",
            args = listOf("hello")
        ),
        NOT_ENOUGH_EXPLICIT_INDEX_ARGS(
            MissingFormatArgumentException("Format specifier '%3\$s'"),
            "%s %3\$s",
            args = listOf("hello")
        ),
        RELATIVE_INDEX_IS_FIRST(
            MissingFormatArgumentException("Format specifier '%<s'"),
            "%<s",
            args = listOf("hello")
        ),
        DYNAMIC_ARG_NOT_ALLOWED(
            IllegalFormatConversionException('d', String::class.java),
            "%d",
            args = listOf(DynamicString.constant("hello"))
        ),
        UNSUPPORTED_DYNAMIC_CONVERSION(
            UnsupportedOperationException("Unsupported conversion for DynamicType: 'h'"),
            "%h",
            args = listOf(DynamicInt32.constant(12))
        ),
        // %s
        FORMAT_s_DYNAMIC_STRING_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2s'"),
            "%2s",
            args = listOf(DynamicString.constant("a"))
        ),
        FORMAT_s_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2s'"),
            "%2s",
            args = listOf(DynamicInt32.constant(12))
        ),
        FORMAT_s_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2s'"),
            "%2s",
            args = listOf(DynamicFloat.constant(12f))
        ),
        FORMAT_s_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2s'"),
            "%2s",
            args = listOf(DynamicBool.constant(true))
        ),
        FORMAT_s_UNSUPPORTED_DYNAMIC_ARG(
            UnsupportedOperationException("$DYNAMIC_INSTANT unsupported for specifier: '%s'"),
            "%s",
            args = listOf(DYNAMIC_INSTANT),
        ),
        // %S
        FORMAT_S_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2S'"),
            "%2S",
            args = listOf(DynamicInt32.constant(12))
        ),
        FORMAT_S_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2S'"),
            "%2S",
            args = listOf(DynamicFloat.constant(12f))
        ),
        FORMAT_S_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2S'"),
            "%2S",
            args = listOf(DynamicBool.constant(true))
        ),
        FORMAT_S_UNSUPPORTED_DYNAMIC_ARG(
            UnsupportedOperationException("$DYNAMIC_INSTANT unsupported for specifier: '%S'"),
            "%S",
            args = listOf(DYNAMIC_INSTANT),
        ),
        // %b
        FORMAT_b_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2b'"),
            "%2b",
            args = listOf(DynamicBool.constant(true))
        ),
        // %B
        FORMAT_B_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2B'"),
            "%2B",
            args = listOf(DynamicBool.constant(true))
        ),
        // %d
        FORMAT_d_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2d'"),
            "%2d",
            args = listOf(DynamicInt32.constant(12))
        ),
        // %f
        FORMAT_f_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            UnsupportedOperationException("Unsupported specifier: '%2f'"),
            "%2f",
            args = listOf(DynamicFloat.constant(12f))
        ),
    }

    @Test
    fun fails() {
        val exception =
            assertFailsWith(case.expected::class, case.name) {
                DynamicString.format(case.format, *case.args.toTypedArray())
            }
        assertWithMessage(case.name)
            .that(exception)
            .hasMessageThat()
            .isEqualTo(case.expected.message)
    }

    companion object {
        @Parameters @JvmStatic fun parameters() = Case.values()
    }
}

private val INSTANT = Instant.EPOCH
private val DYNAMIC_INSTANT = DynamicInstant.withSecondsPrecision(INSTANT)
