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

package androidx.a2ui.compose.ui.catalog

import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions
import androidx.a2ui.model.catalog.functions.A2uiAndFunction
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiNotFunction
import androidx.a2ui.model.catalog.functions.A2uiOpenUrlFunction
import androidx.a2ui.model.catalog.functions.A2uiOrFunction
import androidx.a2ui.model.catalog.functions.A2uiPluralizeFunction
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiException.A2uiValidationException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider
import java.util.Locale
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class A2uiBasicCatalogV1FunctionsTest {

    @Test
    fun toA2uiFunctions_returnsAllSpecificationFunctionsInOrder() {
        assertThat(OpenUrlOnlyFunctions.toA2uiFunctions().map { it.definition.name })
            .containsExactly(
                "required",
                "regex",
                "length",
                "numeric",
                "email",
                "formatString",
                "formatNumber",
                "formatCurrency",
                "formatDate",
                "pluralize",
                "openUrl",
                "and",
                "or",
                "not",
            )
            .inOrder()
    }

    @Test
    fun toA2uiFunctions_usesModelLogicalFunctions() {
        assertThat(OpenUrlOnlyFunctions.function("and")).isSameInstanceAs(A2uiAndFunction.INSTANCE)
        assertThat(OpenUrlOnlyFunctions.function("or")).isSameInstanceAs(A2uiOrFunction.INSTANCE)
        assertThat(OpenUrlOnlyFunctions.function("not")).isSameInstanceAs(A2uiNotFunction.INSTANCE)
    }

    @Test
    fun toA2uiFunctions_usesModelDefinition(
        @TestParameter(valuesProvider = FunctionNames::class) name: String
    ) {
        val modelFunction = createModelFunctions(A2uiLocaleProvider.Default).getValue(name)

        assertThat(OpenUrlOnlyFunctions.function(name).definition)
            .isSameInstanceAs(modelFunction.definition)
    }

    @Test
    fun toA2uiFunctions_withEqualFunctions_returnsEqualFunctions() {
        val urlOpener = A2uiUrlOpener {}
        val localeProvider = A2uiLocaleProvider { Locale.US }

        val functions = DefaultFunctions(urlOpener, localeProvider).toA2uiFunctions()
        val equalFunctions = DefaultFunctions(urlOpener, localeProvider).toA2uiFunctions()

        assertThat(functions).isEqualTo(equalFunctions)
        assertThat(functions.hashCode()).isEqualTo(equalFunctions.hashCode())
        assertThat(functions).isNotEqualTo(OpenUrlOnlyFunctions.toA2uiFunctions())
    }

    @Test
    fun execute_passesExecutionContext(
        @TestParameter(valuesProvider = FunctionNames::class) name: String
    ) {
        val functions = RecordingFunctions()

        functions.function(name).execute(ValidArgs.getValue(name), TestExecutionContext)

        val call = functions.calls.single()
        assertThat(call.name).isEqualTo(name)
        assertThat(call.executionContext).isSameInstanceAs(TestExecutionContext)
    }

    @Test
    fun execute_withMissingArgument_throwsValidationExceptionWithoutCalling(
        // Every function except "required", which accepts a missing value, has required arguments.
        @TestParameter(
            "regex",
            "length",
            "numeric",
            "email",
            "formatString",
            "formatNumber",
            "formatCurrency",
            "formatDate",
            "pluralize",
            "openUrl",
        )
        name: String
    ) {
        val functions = RecordingFunctions()

        assertFailsWith<A2uiValidationException> {
            functions.function(name).execute(emptyMap(), TestExecutionContext)
        }

        assertThat(functions.calls).isEmpty()
    }

    @Test
    fun execute_withThrowingImplementation_propagatesException(
        @TestParameter(valuesProvider = FunctionNames::class) name: String
    ) {
        val exception = A2uiRuntimeException("$name failed")
        val functions = RecordingFunctions(exception)

        val thrownException =
            assertFailsWith<A2uiRuntimeException> {
                functions.function(name).execute(ValidArgs.getValue(name), TestExecutionContext)
            }

        assertThat(thrownException).isSameInstanceAs(exception)
    }

    @Test
    fun required_execute_passesNullForMissingValue() {
        val functions = RecordingFunctions()

        val result = functions.function("required").executeWith("value" to "text")
        functions.function("required").executeWith()

        assertThat(result).isEqualTo(true)
        assertThat(functions.argsOf("required"))
            .containsExactly(listOf("text"), listOf(null))
            .inOrder()
    }

    @Test
    fun regex_execute_passesValueAndPattern() {
        val functions = RecordingFunctions()

        val result =
            functions.function("regex").executeWith("value" to "abc", "pattern" to "[a-z]+")

        assertThat(result).isEqualTo(true)
        assertThat(functions.argsOf("regex")).containsExactly(listOf("abc", "[a-z]+"))
    }

    @Test
    fun length_execute_parsesBoundsAndPassesNoLimitsForMissingBounds() {
        val functions = RecordingFunctions()

        functions.function("length").executeWith("value" to "abc")
        functions.function("length").executeWith("value" to "abc", "min" to 1.0, "max" to "5")

        assertThat(functions.argsOf("length"))
            .containsExactly(listOf("abc", 0, Int.MAX_VALUE), listOf("abc", 1, 5))
            .inOrder()
    }

    @Test
    fun numeric_execute_parsesNumbersAndPassesInfiniteBoundsForMissingBounds() {
        val functions = RecordingFunctions()

        functions.function("numeric").executeWith("value" to 5)
        functions.function("numeric").executeWith("value" to "5", "min" to 1, "max" to 9.5)

        assertThat(functions.argsOf("numeric"))
            .containsExactly(
                listOf(5.0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY),
                listOf(5.0, 1.0, 9.5),
            )
            .inOrder()
    }

    @Test
    fun email_execute_passesValue() {
        val functions = RecordingFunctions()

        val result = functions.function("email").executeWith("value" to "user@example.com")

        assertThat(result).isEqualTo(true)
        assertThat(functions.argsOf("email")).containsExactly(listOf("user@example.com"))
    }

    @Test
    fun formatString_execute_passesValue() {
        val functions = RecordingFunctions()

        val result = functions.function("formatString").executeWith("value" to "\${/name}")

        assertThat(result).isEqualTo("formatted")
        assertThat(functions.argsOf("formatString")).containsExactly(listOf("\${/name}"))
    }

    @Test
    fun formatNumber_execute_appliesSpecificationDefaults() {
        val functions = RecordingFunctions()

        val result = functions.function("formatNumber").executeWith("value" to 1234)
        functions
            .function("formatNumber")
            .executeWith("value" to "1.5", "decimals" to 2.0, "grouping" to "false")

        assertThat(result).isEqualTo("formatted")
        assertThat(functions.argsOf("formatNumber"))
            .containsExactly(listOf(1234.0, null, true), listOf(1.5, 2, false))
            .inOrder()
    }

    @Test
    fun formatCurrency_execute_appliesSpecificationDefaults() {
        val functions = RecordingFunctions()

        functions.function("formatCurrency").executeWith("value" to 10, "currency" to "USD")
        functions
            .function("formatCurrency")
            .executeWith("value" to 10, "currency" to "EUR", "decimals" to 0.0, "grouping" to false)
        // Truncates fractional decimals, as A2uiFormatCurrencyFunction does.
        functions
            .function("formatCurrency")
            .executeWith("value" to 10, "currency" to "EUR", "decimals" to 2.9)

        assertThat(functions.argsOf("formatCurrency"))
            .containsExactly(
                listOf(10.0, "USD", null, true),
                listOf(10.0, "EUR", 0, false),
                listOf(10.0, "EUR", 2, true),
            )
            .inOrder()
    }

    @Test
    fun formatDate_execute_parsesValueAsEpochMillis() {
        val functions = RecordingFunctions()

        functions
            .function("formatDate")
            .executeWith("value" to "1970-01-02T00:00:00Z", "format" to "ISO")
        functions.function("formatDate").executeWith("value" to 86_400, "format" to "ISO")
        functions
            .function("formatDate")
            .executeWith("value" to 1_774_365_900_000L, "format" to "ISO")

        assertThat(functions.argsOf("formatDate"))
            .containsExactly(
                listOf(86_400_000L, "ISO"),
                listOf(86_400_000L, "ISO"),
                listOf(1_774_365_900_000L, "ISO"),
            )
            .inOrder()
    }

    @Test
    fun pluralize_execute_passesNullForMissingForms() {
        val functions = RecordingFunctions()

        val result =
            functions
                .function("pluralize")
                .executeWith("value" to 1, "one" to "item", "other" to "items")
        functions
            .function("pluralize")
            .executeWith(
                "value" to 2,
                "zero" to "none",
                "one" to "one",
                "two" to "two",
                "few" to "few",
                "many" to "many",
                "other" to "other",
            )

        assertThat(result).isEqualTo("items")
        assertThat(functions.argsOf("pluralize"))
            .containsExactly(
                listOf(1.0, null, "item", null, null, null, "items"),
                listOf(2.0, "none", "one", "two", "few", "many", "other"),
            )
            .inOrder()
    }

    @Test
    fun openUrl_execute_passesUrlAndReturnsUnit() {
        val functions = RecordingFunctions()

        val result = functions.function("openUrl").executeWith("url" to "https://example.com")

        assertThat(result).isEqualTo(Unit)
        assertThat(functions.argsOf("openUrl")).containsExactly(listOf("https://example.com"))
    }

    @Test
    fun openUrl_execute_withEmptyUrl_throwsRuntimeExceptionWithoutCalling() {
        val functions = RecordingFunctions()

        assertFailsWith<A2uiRuntimeException> {
            functions.function("openUrl").executeWith("url" to "")
        }

        assertThat(functions.calls).isEmpty()
    }

    @Test
    fun interfaceDefaults_matchModelFunction(
        @TestParameter(valuesProvider = ModelParityCalls::class) call: FunctionCall
    ) {
        // The interface defaults and the model functions both use the default locale.
        val modelFunction = createModelFunctions(A2uiLocaleProvider.Default).getValue(call.name)

        assertThat(outcomeOf(OpenUrlOnlyFunctions.function(call.name), call))
            .isEqualTo(outcomeOf(modelFunction, call))
    }

    @Test
    fun defaultFunctions_matchModelFunction(
        @TestParameter(valuesProvider = ModelParityCalls::class) call: FunctionCall
    ) {
        val localeProvider = A2uiLocaleProvider { Locale.GERMANY }
        val functions = DefaultFunctions(urlOpener = {}, localeProvider = localeProvider)
        val modelFunction = createModelFunctions(localeProvider).getValue(call.name)

        assertThat(outcomeOf(functions.function(call.name), call))
            .isEqualTo(outcomeOf(modelFunction, call))
    }

    @Test
    fun pluralize_matchesModelFunction(
        @TestParameter(valuesProvider = PluralizeCalls::class) call: FunctionCall
    ) {
        // ICU isn't available in JVM tests, so both sides use the same fake message formatter. The
        // results match only if both sides parse the same arguments.
        val messageFormatter = A2uiMessageFormatter { pattern, locale, arguments ->
            "$pattern $locale $arguments"
        }
        val modelFunction = A2uiPluralizeFunction(messageFormatter) { Locale.GERMANY }
        val functions =
            object : Functions by OpenUrlOnlyFunctions {
                override fun pluralize(
                    value: Double,
                    zero: String?,
                    one: String?,
                    two: String?,
                    few: String?,
                    many: String?,
                    other: String,
                    executionContext: A2uiExecutionContext,
                ): String = modelFunction.typedExecute(value, zero, one, two, few, many, other)
            }

        assertThat(outcomeOf(functions.function("pluralize"), call))
            .isEqualTo(outcomeOf(modelFunction, call))
    }

    @Test
    fun openUrl_matchesModelFunction(
        @TestParameter(valuesProvider = OpenUrlCalls::class) call: FunctionCall
    ) {
        val openedUrls = mutableListOf<String>()
        val modelOpenedUrls = mutableListOf<String>()
        val functions =
            DefaultFunctions(
                urlOpener = { openedUrls += it },
                localeProvider = A2uiLocaleProvider.Default,
            )
        val modelFunction = A2uiOpenUrlFunction { modelOpenedUrls += it }

        assertThat(outcomeOf(functions.function("openUrl"), call))
            .isEqualTo(outcomeOf(modelFunction, call))
        assertThat(openedUrls).isEqualTo(modelOpenedUrls)
    }

    @Test
    fun required_default_treatsNullAndEmptyValuesAsMissing() {
        val functions = OpenUrlOnlyFunctions

        assertThat(functions.required(null, TestExecutionContext)).isFalse()
        assertThat(functions.required("", TestExecutionContext)).isFalse()
        assertThat(functions.required(emptyMap<String, Any>(), TestExecutionContext)).isFalse()
        assertThat(functions.required(intArrayOf(), TestExecutionContext)).isFalse()
        assertThat(functions.required("text", TestExecutionContext)).isTrue()
        assertThat(functions.required(0, TestExecutionContext)).isTrue()
    }

    @Test
    fun regex_default_matchesPattern() {
        val functions = OpenUrlOnlyFunctions

        assertThat(functions.regex("abc123", "[a-z]+\\d+", TestExecutionContext)).isTrue()
        assertThat(functions.regex("abc", "\\d+", TestExecutionContext)).isFalse()
    }

    @Test
    fun length_default_checksBoundsInclusively() {
        val functions = OpenUrlOnlyFunctions

        assertThat(functions.length("", 0, Int.MAX_VALUE, TestExecutionContext)).isTrue()
        assertThat(functions.length("abc", 2, 3, TestExecutionContext)).isTrue()
        assertThat(functions.length("abc", 0, 2, TestExecutionContext)).isFalse()
        assertThat(functions.length("abc", 4, Int.MAX_VALUE, TestExecutionContext)).isFalse()
    }

    @Test
    fun numeric_default_checksBounds() {
        val functions = OpenUrlOnlyFunctions

        val noMin = Double.NEGATIVE_INFINITY
        val noMax = Double.POSITIVE_INFINITY

        assertThat(functions.numeric(5.0, noMin, noMax, TestExecutionContext)).isTrue()
        assertThat(functions.numeric(5.0, 1.5, 5.0, TestExecutionContext)).isTrue()
        assertThat(functions.numeric(5.0, noMin, 4.5, TestExecutionContext)).isFalse()
    }

    @Test
    fun email_default_validatesEmailAddress() {
        val functions = OpenUrlOnlyFunctions

        assertThat(functions.email("user@example.com", TestExecutionContext)).isTrue()
        assertThat(functions.email("user@", TestExecutionContext)).isFalse()
    }

    @Test
    fun formatString_default_interpolatesExpressions() {
        assertThat(OpenUrlOnlyFunctions.formatString("Hello, \${/name}!", TestExecutionContext))
            .isEqualTo("Hello, A2UI!")
    }

    @Test
    fun formatNumber_default_usesDefaultLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)

            assertThat(OpenUrlOnlyFunctions.formatNumber(1234.5, 2, true, TestExecutionContext))
                .isEqualTo("1.234,50")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun formatDate_default_formatsEpochMillisInUtc() {
        val functions = DefaultFunctions(urlOpener = {}, localeProvider = { Locale.US })

        // Formats small timestamps as milliseconds, unlike the "value" argument parser, which
        // interprets timestamps below 10^10 as seconds.
        assertThat(functions.formatDate(86_400_000L, "yyyy-MM-dd HH:mm", TestExecutionContext))
            .isEqualTo("1970-01-02 00:00")
        assertThat(functions.formatDate(-1L, "ISO", TestExecutionContext))
            .isEqualTo("1969-12-31T23:59:59Z")
        assertThat(functions.formatDate(1_774_365_900_000L, "ISO", TestExecutionContext))
            .isEqualTo("2026-03-24T15:25:00Z")
    }

    @Test
    fun formatDate_default_withInvalidPattern_throwsRuntimeException() {
        assertFailsWith<A2uiRuntimeException> {
            OpenUrlOnlyFunctions.formatDate(0L, "qqq", TestExecutionContext)
        }
    }

    @Test
    fun defaultFunctions_opensUrlsWithUrlOpener() {
        val openedUrls = mutableListOf<String>()
        val functions =
            DefaultFunctions(
                urlOpener = { openedUrls += it },
                localeProvider = A2uiLocaleProvider.Default,
            )

        functions.openUrl("https://example.com", TestExecutionContext)

        assertThat(openedUrls).containsExactly("https://example.com")
    }

    @Test
    fun defaultFunctions_formatWithLocaleProvider() {
        val us = DefaultFunctions(urlOpener = {}, localeProvider = { Locale.US })
        val germany = DefaultFunctions(urlOpener = {}, localeProvider = { Locale.GERMANY })

        assertThat(us.formatNumber(1234.5, 2, true, TestExecutionContext)).isEqualTo("1,234.50")
        assertThat(germany.formatNumber(1234.5, 2, true, TestExecutionContext))
            .isEqualTo("1.234,50")
        assertThat(us.formatCurrency(1234.5, "USD", null, true, TestExecutionContext))
            .isEqualTo("\$1,234.50")
        assertThat(us.formatCurrency(1234.5, "USD", 0, false, TestExecutionContext))
            .isEqualTo("\$1234")
        assertThat(germany.formatDate(0L, "MMMM", TestExecutionContext)).isEqualTo("Januar")
    }

    @Test
    fun defaultFunctions_withEqualDependencies_areEqual() {
        val urlOpener = A2uiUrlOpener {}
        val localeProvider = A2uiLocaleProvider { Locale.US }

        assertThat(DefaultFunctions(urlOpener, localeProvider))
            .isEqualTo(DefaultFunctions(urlOpener, localeProvider))
        assertThat(DefaultFunctions(urlOpener, localeProvider).hashCode())
            .isEqualTo(DefaultFunctions(urlOpener, localeProvider).hashCode())
        assertThat(DefaultFunctions(urlOpener, localeProvider))
            .isNotEqualTo(DefaultFunctions(urlOpener, A2uiLocaleProvider.Default))
        assertThat(DefaultFunctions(urlOpener, localeProvider))
            .isNotEqualTo(DefaultFunctions({}, localeProvider))
    }

    @Test
    fun delegation_replacesOnlyOverriddenFunctions() {
        val openedUrls = mutableListOf<String>()
        val defaults =
            DefaultFunctions(urlOpener = { openedUrls += it }, localeProvider = { Locale.US })
        val functions =
            object : Functions by defaults {
                override fun formatDate(
                    epochMillis: Long,
                    format: String,
                    executionContext: A2uiExecutionContext,
                ): String = "custom"
            }

        assertThat(functions.function("formatDate").executeWith("value" to 0, "format" to "ISO"))
            .isEqualTo("custom")
        assertThat(functions.function("formatNumber").executeWith("value" to 1234.5))
            .isEqualTo("1,234.5")
        functions.function("openUrl").executeWith("url" to "https://example.com")
        assertThat(openedUrls).containsExactly("https://example.com")
    }

    @Test
    fun override_callsDefaultImplementationWithSuper() {
        val functions =
            object : Functions {
                override fun openUrl(url: String, executionContext: A2uiExecutionContext) {}

                override fun email(value: String, executionContext: A2uiExecutionContext): Boolean =
                    value.endsWith("@example.com") && super.email(value, executionContext)
            }

        assertThat(functions.function("email").executeWith("value" to "user@example.com"))
            .isEqualTo(true)
        assertThat(functions.function("email").executeWith("value" to "user@other.com"))
            .isEqualTo(false)
    }

    private fun createModelFunctions(
        localeProvider: A2uiLocaleProvider
    ): Map<String, A2uiFunction> =
        createBasicCatalogFunctions(
                urlOpener = {},
                messageFormatter = { pattern, _, _ -> pattern },
                localeProvider = localeProvider,
            )
            .associateBy { it.definition.name }

    /**
     * Returns the result of executing [function] with the arguments of [call], or the type and
     * message of the exception that it throws.
     */
    private fun outcomeOf(function: A2uiFunction, call: FunctionCall): Any? =
        try {
            function.execute(call.args, TestExecutionContext)
        } catch (e: Exception) {
            "${e.javaClass.name}: ${e.message}"
        }

    private fun Functions.function(name: String): A2uiFunction =
        toA2uiFunctions().single { it.definition.name == name }

    private fun A2uiFunction.executeWith(vararg args: Pair<String, Any>): Any? =
        execute(mapOf(*args), TestExecutionContext)

    /** Implements only [openUrl], so all other functions use the default implementations. */
    private object OpenUrlOnlyFunctions : Functions {
        override fun openUrl(url: String, executionContext: A2uiExecutionContext) {}
    }

    /**
     * Records the parsed arguments of each call, and then throws [exception] if it isn't `null`.
     */
    private class RecordingFunctions(private val exception: Exception? = null) : Functions {
        val calls = mutableListOf<Call>()

        fun argsOf(name: String): List<List<Any?>> =
            calls.filter { it.name == name }.map { it.args }

        private fun <T> record(
            name: String,
            result: T,
            executionContext: A2uiExecutionContext,
            vararg args: Any?,
        ): T {
            calls += Call(name, args.toList(), executionContext)
            if (exception != null) throw exception
            return result
        }

        override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
            record("openUrl", Unit, executionContext, url)
        }

        override fun required(value: Any?, executionContext: A2uiExecutionContext): Boolean =
            record("required", true, executionContext, value)

        override fun regex(
            value: String,
            pattern: String,
            executionContext: A2uiExecutionContext,
        ): Boolean = record("regex", true, executionContext, value, pattern)

        override fun length(
            value: String,
            min: Int,
            max: Int,
            executionContext: A2uiExecutionContext,
        ): Boolean = record("length", true, executionContext, value, min, max)

        override fun numeric(
            value: Double,
            min: Double,
            max: Double,
            executionContext: A2uiExecutionContext,
        ): Boolean = record("numeric", true, executionContext, value, min, max)

        override fun email(value: String, executionContext: A2uiExecutionContext): Boolean =
            record("email", true, executionContext, value)

        override fun formatString(value: String, executionContext: A2uiExecutionContext): String =
            record("formatString", "formatted", executionContext, value)

        override fun formatNumber(
            value: Double,
            decimals: Int?,
            grouping: Boolean,
            executionContext: A2uiExecutionContext,
        ): String = record("formatNumber", "formatted", executionContext, value, decimals, grouping)

        override fun formatCurrency(
            value: Double,
            currency: String,
            decimals: Int?,
            grouping: Boolean,
            executionContext: A2uiExecutionContext,
        ): String =
            record(
                "formatCurrency",
                "formatted",
                executionContext,
                value,
                currency,
                decimals,
                grouping,
            )

        override fun formatDate(
            epochMillis: Long,
            format: String,
            executionContext: A2uiExecutionContext,
        ): String = record("formatDate", "formatted", executionContext, epochMillis, format)

        override fun pluralize(
            value: Double,
            zero: String?,
            one: String?,
            two: String?,
            few: String?,
            many: String?,
            other: String,
            executionContext: A2uiExecutionContext,
        ): String =
            record("pluralize", other, executionContext, value, zero, one, two, few, many, other)
    }

    /** A call to a [RecordingFunctions] method with its parsed arguments. */
    private class Call(
        val name: String,
        val args: List<Any?>,
        val executionContext: A2uiExecutionContext,
    )

    /** An [A2uiExecutionContext] that resolves `/name` to `"A2UI"`. */
    private object TestExecutionContext : A2uiExecutionContext {
        override val dataPath: A2uiDataPath = A2uiDataPath("/items/0")

        override fun evaluatePayload(payload: Any?): Any? =
            when (payload) {
                mapOf("path" to "/name") -> "A2UI"
                else -> throw UnsupportedOperationException("Unexpected payload: $payload")
            }

        override fun executeFunction(name: String, args: Map<String, Any>): Any? =
            throw UnsupportedOperationException()

        override fun resolveValue(path: A2uiDataPath): Any? = throw UnsupportedOperationException()

        override fun <T : Any> getOrCreateFunctionScopedCache(
            functionDefinition: A2uiFunctionDefinition,
            factory: () -> T,
        ): T = factory()
    }

    /** A call to the function named [name] with the arguments [args]. */
    class FunctionCall(val name: String, val args: Map<String, Any>) {
        override fun toString(): String = "$name($args)"
    }

    /** Provides the names of the methods of [Functions], in the order of the specification. */
    class FunctionNames : TestParameterValuesProvider() {
        override fun provideValues(context: TestParameterValuesProvider.Context): List<String> =
            ArgsMatrix.keys.toList()
    }

    /**
     * Provides the calls in [ArgsMatrix] to all methods except `pluralize`, which needs ICU, and
     * `openUrl`, which needs a URL opener; see [PluralizeCalls] and [OpenUrlCalls].
     */
    class ModelParityCalls : TestParameterValuesProvider() {
        override fun provideValues(
            context: TestParameterValuesProvider.Context
        ): List<FunctionCall> = callsTo(ArgsMatrix.keys - setOf("pluralize", "openUrl"))
    }

    /** Provides the calls in [ArgsMatrix] to `pluralize`. */
    class PluralizeCalls : TestParameterValuesProvider() {
        override fun provideValues(
            context: TestParameterValuesProvider.Context
        ): List<FunctionCall> = callsTo(setOf("pluralize"))
    }

    /** Provides the calls in [ArgsMatrix] to `openUrl`. */
    class OpenUrlCalls : TestParameterValuesProvider() {
        override fun provideValues(
            context: TestParameterValuesProvider.Context
        ): List<FunctionCall> = callsTo(setOf("openUrl"))
    }

    private companion object {
        /**
         * Argument maps for every method of [Functions], including spec defaults, type coercions,
         * and invalid or missing arguments. The first argument map of each method is valid.
         */
        val ArgsMatrix: Map<String, List<Map<String, Any>>> =
            linkedMapOf(
                "required" to
                    listOf(
                        mapOf("value" to "text"),
                        mapOf("value" to ""),
                        mapOf("value" to emptyList<Any>()),
                        mapOf("value" to listOf(1)),
                        mapOf("value" to emptyMap<String, Any>()),
                        mapOf("value" to intArrayOf()),
                        mapOf("value" to 0),
                        emptyMap(),
                    ),
                "regex" to
                    listOf(
                        mapOf("value" to "abc123", "pattern" to "[a-z]+\\d+"),
                        mapOf("value" to "abc", "pattern" to "\\d+"),
                        mapOf("value" to 123, "pattern" to "\\d+"),
                        mapOf("value" to "abc", "pattern" to "["),
                        mapOf("value" to "abc"),
                        emptyMap(),
                    ),
                "length" to
                    listOf(
                        mapOf("value" to "abc", "min" to 2, "max" to 3),
                        mapOf("value" to "abc", "max" to 2),
                        mapOf("value" to "abc", "min" to "1", "max" to 5.0),
                        mapOf("value" to "abc", "min" to 1.7),
                        mapOf("value" to "abc", "min" to "x"),
                        mapOf("value" to ""),
                        emptyMap(),
                    ),
                "numeric" to
                    listOf(
                        mapOf("value" to 5, "min" to 1.5),
                        mapOf("value" to 5, "max" to 4.5),
                        mapOf("value" to "5", "min" to "1", "max" to "9.5"),
                        mapOf("value" to "abc"),
                        emptyMap(),
                    ),
                "email" to
                    listOf(
                        mapOf("value" to "user@example.com"),
                        mapOf("value" to "user@"),
                        mapOf("value" to ""),
                        emptyMap(),
                    ),
                "formatString" to
                    listOf(
                        mapOf("value" to "plain text"),
                        mapOf("value" to "Hello, \${/name}!"),
                        mapOf("value" to "\\\${/name}"),
                        emptyMap(),
                    ),
                "formatNumber" to
                    listOf(
                        mapOf("value" to 1234567.891),
                        mapOf("value" to 1234567.891, "decimals" to 1, "grouping" to false),
                        mapOf("value" to "1.5", "decimals" to "2", "grouping" to "false"),
                        mapOf("value" to 1.5, "decimals" to 1.7),
                        mapOf("value" to "abc"),
                        emptyMap(),
                    ),
                "formatCurrency" to
                    listOf(
                        mapOf("value" to 1234.5, "currency" to "EUR"),
                        mapOf(
                            "value" to 1234.5,
                            "currency" to "usd",
                            "decimals" to 0,
                            "grouping" to false,
                        ),
                        mapOf("value" to 1234.5, "currency" to "USD", "decimals" to 1.7),
                        mapOf("value" to 1, "currency" to "XYZ1"),
                        mapOf("value" to 1),
                        emptyMap(),
                    ),
                "formatDate" to
                    listOf(
                        mapOf("value" to "2026-03-24T15:25:00Z", "format" to "ISO"),
                        mapOf("value" to 1_774_365_900, "format" to "EEEE, d. MMMM yyyy"),
                        mapOf("value" to 1_774_365_900_000L, "format" to "yyyy-MM-dd HH:mm"),
                        mapOf("value" to "2026-03-24", "format" to "ISO"),
                        mapOf("value" to "not a date", "format" to "ISO"),
                        mapOf("value" to 0, "format" to "qqq"),
                        mapOf("value" to 0),
                        emptyMap(),
                    ),
                "pluralize" to
                    listOf(
                        mapOf("value" to 1, "one" to "item", "other" to "items"),
                        mapOf(
                            "value" to 2,
                            "zero" to "none",
                            "one" to "one",
                            "two" to "two",
                            "few" to "few",
                            "many" to "many",
                            "other" to "other",
                        ),
                        mapOf("value" to "3", "other" to "items"),
                        mapOf("value" to 1),
                        emptyMap(),
                    ),
                "openUrl" to
                    listOf(mapOf("url" to "https://example.com"), mapOf("url" to ""), emptyMap()),
            )

        /**
         * A valid argument map for every method of [Functions], in the order of the specification.
         */
        val ValidArgs: Map<String, Map<String, Any>> = ArgsMatrix.mapValues { it.value.first() }

        /**
         * Returns a [FunctionCall] for each argument map in [ArgsMatrix] of the functions [names].
         */
        fun callsTo(names: Set<String>): List<FunctionCall> = names.flatMap { name ->
            ArgsMatrix.getValue(name).map { FunctionCall(name, it) }
        }
    }
}
