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
import com.google.common.truth.Truth.assertThat
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
        val format: String,
        val args: List<Any?>? = null,
        val dynamicArgs: List<DynamicType>? = null,
        val equivalentArgs: List<Any?>? = null,
        val expected: String,
    ) {
        CONSTANT_ONLY(format = "hello world", args = listOf(), expected = "hello world"),
        NON_DYNAMIC_ARGS(format = "hello %s", args = listOf("world"), expected = "hello world"),
        EXPLICIT_ARG_INDEX(
            format = "%d %3\$d %d %<d",
            args = listOf(12, 56, 34),
            expected = "12 34 56 56",
        ),
        DYNAMIC_START(
            format = "%s world",
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
            expected = "hello world",
        ),
        DYNAMIC_MIDDLE(
            format = "hel%srld",
            dynamicArgs = listOf(DynamicString.constant("lo wo")),
            equivalentArgs = listOf("lo wo"),
            expected = "hello world",
        ),
        DYNAMIC_END(
            format = "hello %s",
            dynamicArgs = listOf(DynamicString.constant("world")),
            equivalentArgs = listOf("world"),
            expected = "hello world",
        ),
        SEPARATED_DYNAMIC(
            format = "%s %s",
            dynamicArgs = listOf(DynamicString.constant("hello"), DynamicString.constant("world")),
            equivalentArgs = listOf("hello", "world"),
            expected = "hello world",
        ),
        CONNECTED_DYNAMIC(
            format = "%s%s",
            dynamicArgs = listOf(DynamicString.constant("hello"), DynamicString.constant("world")),
            equivalentArgs = listOf("hello", "world"),
            expected = "helloworld",
        ),
        // %%
        FORMAT_percent_DYNAMIC_TYPE(
            format = "%%",
            // args are ignored
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
            expected = "%",
        ),
        // %%
        FORMAT_n_DYNAMIC_TYPE(
            format = "%n",
            // args are ignored
            dynamicArgs = listOf(DynamicString.constant("hello")),
            equivalentArgs = listOf("hello"),
            expected = "\n",
        ),
        // %s
        FORMAT_s_DYNAMIC_STRING(
            format = "%s",
            dynamicArgs = listOf(DynamicString.constant("ab")),
            equivalentArgs = listOf("ab"),
            expected = "ab",
        ),
        FORMAT_s_DYNAMIC_INT32(
            format = "%s",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
            expected = "12",
        ),
        FORMAT_s_DYNAMIC_FLOAT(
            format = "%s",
            dynamicArgs = listOf(DynamicFloat.constant(12f)),
            equivalentArgs = listOf(12f),
            expected = "12.0",
        ),
        FORMAT_s_DYNAMIC_BOOL_TRUE(
            format = "%s",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
            expected = "true",
        ),
        FORMAT_s_DYNAMIC_BOOL_FALSE(
            format = "%s",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
            expected = "false",
        ),
        // %S
        FORMAT_S_DYNAMIC_INT32(
            format = "%S",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
            expected = "12",
        ),
        FORMAT_S_DYNAMIC_FLOAT(
            format = "%S",
            dynamicArgs = listOf(DynamicFloat.constant(12f)),
            equivalentArgs = listOf(12f),
            expected = "12.0",
        ),
        FORMAT_S_DYNAMIC_BOOL_TRUE(
            format = "%S",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
            expected = "TRUE",
        ),
        FORMAT_S_DYNAMIC_BOOL_FALSE(
            format = "%S",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
            expected = "FALSE",
        ),
        // %b
        FORMAT_b_DYNAMIC_BOOL_TRUE(
            format = "%b",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
            expected = "true",
        ),
        FORMAT_b_DYNAMIC_BOOL_FALSE(
            format = "%b",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
            expected = "false",
        ),
        FORMAT_b_DYNAMIC_TYPE_IS_TRUE(
            format = "%b",
            dynamicArgs = listOf(DYNAMIC_INSTANT),
            equivalentArgs = listOf(INSTANT),
            expected = "true",
        ),
        // %B
        FORMAT_B_DYNAMIC_BOOL_TRUE(
            format = "%B",
            dynamicArgs = listOf(DynamicBool.constant(true)),
            equivalentArgs = listOf(true),
            expected = "TRUE",
        ),
        FORMAT_B_DYNAMIC_BOOL_FALSE(
            format = "%B",
            dynamicArgs = listOf(DynamicBool.constant(false)),
            equivalentArgs = listOf(false),
            expected = "FALSE",
        ),
        FORMAT_B_DYNAMIC_TYPE_IS_TRUE(
            format = "%B",
            dynamicArgs = listOf(DYNAMIC_INSTANT),
            equivalentArgs = listOf(INSTANT),
            expected = "TRUE",
        ),
        // %d
        FORMAT_d_DYNAMIC_INT32(
            format = "%d",
            dynamicArgs = listOf(DynamicInt32.constant(12)),
            equivalentArgs = listOf(12),
            expected = "12",
        ),
        // %f
        FORMAT_f_DYNAMIC_FLOAT(
            format = "%f %.2f %.2f",
            dynamicArgs =
                listOf(
                    DynamicFloat.constant(12.345f), // default fraction digits
                    DynamicFloat.constant(34.567f), // max fraction digits
                    DynamicFloat.constant(56.7f), // min fraction digits
                ),
            equivalentArgs = listOf(12.345f, 34.567f, 56.7f),
            expected = "12.345000 34.57 56.70",
        ),
    }

    @Test
    fun equalsExpected() {
        val args = (case.args ?: case.dynamicArgs)!!.toTypedArray()
        assertThat(DynamicString.format(case.format, *args).evaluate()).isEqualTo(case.expected)
    }

    @Test
    fun equalsDefault() {
        val dynamicArgs = (case.args ?: case.dynamicArgs)!!.toTypedArray()
        val equivalentArgs = (case.args ?: case.equivalentArgs)!!.toTypedArray()
        assertThat(DynamicString.format(case.format, *dynamicArgs).evaluate())
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
    enum class Case(val format: String, val args: List<Any?> = listOf(), val expected: Exception) {
        NOT_ENOUGH_POSITIONAL_ARGS(
            format = "%s %s",
            args = listOf("hello"),
            expected = MissingFormatArgumentException("Format specifier '%s'"),
        ),
        NOT_ENOUGH_EXPLICIT_INDEX_ARGS(
            format = "%s %3\$s",
            args = listOf("hello"),
            expected = MissingFormatArgumentException("Format specifier '%3\$s'"),
        ),
        RELATIVE_INDEX_IS_FIRST(
            format = "%<s",
            args = listOf("hello"),
            expected = MissingFormatArgumentException("Format specifier '%<s'"),
        ),
        DYNAMIC_ARG_NOT_ALLOWED(
            format = "%d",
            args = listOf(DynamicString.constant("hello")),
            expected = IllegalFormatConversionException('d', String::class.java),
        ),
        UNSUPPORTED_DYNAMIC_CONVERSION(
            format = "%h",
            args = listOf(DynamicInt32.constant(12)),
            expected = UnsupportedOperationException("Unsupported conversion for DynamicType: 'h'"),
        ),
        // %s
        FORMAT_s_DYNAMIC_STRING_WITH_UNSUPPORTED_OPTIONS(
            format = "%2s",
            args = listOf(DynamicString.constant("a")),
            expected = UnsupportedOperationException("Unsupported specifier: '%2s'"),
        ),
        FORMAT_s_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            format = "%2s",
            args = listOf(DynamicInt32.constant(12)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2s'"),
        ),
        FORMAT_s_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            format = "%2s",
            args = listOf(DynamicFloat.constant(12f)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2s'"),
        ),
        FORMAT_s_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            format = "%2s",
            args = listOf(DynamicBool.constant(true)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2s'"),
        ),
        FORMAT_s_UNSUPPORTED_DYNAMIC_ARG(
            format = "%s",
            args = listOf(DYNAMIC_INSTANT),
            expected =
                UnsupportedOperationException("$DYNAMIC_INSTANT unsupported for specifier: '%s'"),
        ),
        // %S
        FORMAT_S_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            format = "%2S",
            args = listOf(DynamicInt32.constant(12)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2S'"),
        ),
        FORMAT_S_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            format = "%2S",
            args = listOf(DynamicFloat.constant(12f)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2S'"),
        ),
        FORMAT_S_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            format = "%2S",
            args = listOf(DynamicBool.constant(true)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2S'"),
        ),
        FORMAT_S_UNSUPPORTED_DYNAMIC_ARG(
            format = "%S",
            args = listOf(DYNAMIC_INSTANT),
            expected =
                UnsupportedOperationException("$DYNAMIC_INSTANT unsupported for specifier: '%S'"),
        ),
        // %b
        FORMAT_b_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            format = "%2b",
            args = listOf(DynamicBool.constant(true)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2b'"),
        ),
        // %B
        FORMAT_B_DYNAMIC_BOOL_WITH_UNSUPPORTED_OPTIONS(
            format = "%2B",
            args = listOf(DynamicBool.constant(true)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2B'"),
        ),
        // %d
        FORMAT_d_DYNAMIC_INT32_WITH_UNSUPPORTED_OPTIONS(
            format = "%2d",
            args = listOf(DynamicInt32.constant(12)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2d'"),
        ),
        // %f
        FORMAT_f_DYNAMIC_FLOAT_WITH_UNSUPPORTED_OPTIONS(
            format = "%2f",
            args = listOf(DynamicFloat.constant(12f)),
            expected = UnsupportedOperationException("Unsupported specifier: '%2f'"),
        ),
    }

    @Test
    fun fails() {
        val exception =
            assertFailsWith(case.expected::class) {
                DynamicString.format(case.format, *case.args.toTypedArray())
            }
        assertThat(exception).hasMessageThat().isEqualTo(case.expected.message)
    }

    companion object {
        @Parameters @JvmStatic fun parameters() = Case.values()
    }
}

private val INSTANT = Instant.EPOCH
private val DYNAMIC_INSTANT = DynamicInstant.withSecondsPrecision(INSTANT)
