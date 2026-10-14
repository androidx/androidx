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

import android.content.Context
import android.content.Intent
import android.icu.text.MessageFormat
import android.icu.text.MessagePattern
import android.net.Uri
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1.Functions
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.catalog.functions.A2uiAndFunction
import androidx.a2ui.model.catalog.functions.A2uiEmailFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatCurrencyFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatDateFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatNumberFunction
import androidx.a2ui.model.catalog.functions.A2uiFormatStringFunction
import androidx.a2ui.model.catalog.functions.A2uiFunctionArgParser
import androidx.a2ui.model.catalog.functions.A2uiLengthFunction
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiNotFunction
import androidx.a2ui.model.catalog.functions.A2uiNumericFunction
import androidx.a2ui.model.catalog.functions.A2uiOpenUrlFunction
import androidx.a2ui.model.catalog.functions.A2uiOrFunction
import androidx.a2ui.model.catalog.functions.A2uiPluralizeFunction
import androidx.a2ui.model.catalog.functions.A2uiRegexFunction
import androidx.a2ui.model.catalog.functions.A2uiRequiredFunction
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.a2ui.model.protocol.A2uiExecutionContext
import java.util.Locale

/**
 * The default implementations of [Functions], which the
 * [A2uiBasicCatalogV1.Functions(context)][A2uiBasicCatalogV1.Companion.Functions] factory function
 * creates.
 *
 * Opens URLs with [urlOpener], and formats numbers, currencies, dates, and plurals in the locale of
 * [localeProvider]. The other functions use the default implementations of [Functions].
 *
 * @param urlOpener [A2uiUrlOpener] that opens URLs
 * @param localeProvider [A2uiLocaleProvider] of the locale used to format numbers, currencies,
 *   dates, and plurals
 */
internal class DefaultFunctions(
    private val urlOpener: A2uiUrlOpener,
    private val localeProvider: A2uiLocaleProvider,
) : Functions {
    private val formatNumberFunction = A2uiFormatNumberFunction(localeProvider)
    private val formatCurrencyFunction = A2uiFormatCurrencyFunction(localeProvider)
    private val formatDateFunction = A2uiFormatDateFunction(localeProvider)
    private val pluralizeFunction = A2uiPluralizeFunction(IcuMessageFormatter, localeProvider)

    override fun openUrl(url: String, executionContext: A2uiExecutionContext) {
        urlOpener.openUrl(url)
    }

    override fun formatNumber(
        value: Double,
        decimals: Int?,
        grouping: Boolean,
        executionContext: A2uiExecutionContext,
    ): String = formatNumberFunction.typedExecute(value, decimals, grouping)

    override fun formatCurrency(
        value: Double,
        currency: String,
        decimals: Int?,
        grouping: Boolean,
        executionContext: A2uiExecutionContext,
    ): String = formatCurrencyFunction.typedExecute(value, currency, decimals, grouping)

    override fun formatDate(
        epochMillis: Long,
        format: String,
        executionContext: A2uiExecutionContext,
    ): String = formatDateFunction.typedExecute(epochMillis, format)

    override fun pluralize(
        value: Double,
        zero: String?,
        one: String?,
        two: String?,
        few: String?,
        many: String?,
        other: String,
        executionContext: A2uiExecutionContext,
    ): String = pluralizeFunction.typedExecute(value, zero, one, two, few, many, other)

    override fun equals(other: Any?): Boolean =
        other is DefaultFunctions &&
            urlOpener == other.urlOpener &&
            localeProvider == other.localeProvider

    override fun hashCode(): Int = 31 * urlOpener.hashCode() + localeProvider.hashCode()

    override fun toString(): String =
        "DefaultFunctions(urlOpener=$urlOpener, localeProvider=$localeProvider)"
}

/**
 * The `"pluralize"` function from the [androidx.a2ui.model.catalog.functions] package that the
 * default implementation of [Functions.pluralize] uses, with the ICU plural rules of the default
 * locale.
 */
internal val DefaultPluralizeFunction: A2uiPluralizeFunction =
    A2uiPluralizeFunction(IcuMessageFormatter)

/**
 * Opens URLs with an [Intent.ACTION_VIEW] intent.
 *
 * Only resolves activities in the [Intent.CATEGORY_BROWSABLE] category, which have opted in to
 * being launched from untrusted links.
 *
 * @param context [Context] whose application context starts activities
 */
internal class IntentUrlOpener(context: Context) : A2uiUrlOpener {

    // Holds the application context so that catalogs kept in long-lived objects (e.g., a ViewModel)
    // don't leak activities.
    private val applicationContext: Context = context.applicationContext

    override fun openUrl(url: String) {
        val intent =
            // Intent filters match schemes case-sensitively, so normalize "HTTPS" to "https".
            Intent(Intent.ACTION_VIEW, Uri.parse(url).normalizeScheme())
                .addCategory(Intent.CATEGORY_BROWSABLE)
                // Required to start an activity from a non-activity context.
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(intent)
    }

    override fun equals(other: Any?): Boolean =
        other is IntentUrlOpener && applicationContext == other.applicationContext

    override fun hashCode(): Int = applicationContext.hashCode()

    override fun toString(): String = "IntentUrlOpener(applicationContext=$applicationContext)"
}

/**
 * Returns the [A2uiFunction]s that the engine calls for these [Functions], in the order of the
 * specification.
 *
 * The [A2uiFunction] of each method of [Functions] has the definition of its function in the
 * specification, parses the arguments of a call exactly as the corresponding function in the
 * [androidx.a2ui.model.catalog.functions] package does, and calls that method. The logical
 * functions `and`, `or`, and `not` are the functions of that package.
 */
internal fun Functions.toA2uiFunctions(): List<A2uiFunction> =
    listOf(
        RequiredAdapter(this),
        RegexAdapter(this),
        LengthAdapter(this),
        NumericAdapter(this),
        EmailAdapter(this),
        FormatStringAdapter(this),
        FormatNumberAdapter(this),
        FormatCurrencyAdapter(this),
        FormatDateAdapter(this),
        PluralizeAdapter(this),
        OpenUrlAdapter(this),
        A2uiAndFunction.INSTANCE,
        A2uiOrFunction.INSTANCE,
        A2uiNotFunction.INSTANCE,
    )

/**
 * An [A2uiFunction] that parses the arguments of a call and calls a method of [functions].
 *
 * Adapters of the same function are equal if their [functions] are equal, so that catalogs created
 * from equal [Functions] have equal functions.
 */
private abstract class FunctionAdapter(
    final override val definition: A2uiFunctionDefinition,
    protected val functions: Functions,
) : A2uiFunction {

    final override fun equals(other: Any?): Boolean =
        other is FunctionAdapter && other.javaClass == javaClass && other.functions == functions

    final override fun hashCode(): Int = 31 * javaClass.hashCode() + functions.hashCode()

    final override fun toString(): String = "${definition.name}($functions)"
}

private class RequiredAdapter(functions: Functions) :
    FunctionAdapter(A2uiRequiredFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.required(value = args[ValueKey], executionContext = executionContext)
}

private class RegexAdapter(functions: Functions) :
    FunctionAdapter(A2uiRegexFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.regex(
            value = A2uiFunctionArgParser.getStringArg(args, ValueKey),
            pattern = A2uiFunctionArgParser.getStringArg(args, PatternKey),
            executionContext = executionContext,
        )
}

private class LengthAdapter(functions: Functions) :
    FunctionAdapter(A2uiLengthFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.length(
            value = A2uiFunctionArgParser.getStringArg(args, ValueKey),
            min =
                if (args.containsKey(MinKey)) A2uiFunctionArgParser.getIntArg(args, MinKey) else 0,
            max =
                if (args.containsKey(MaxKey)) A2uiFunctionArgParser.getIntArg(args, MaxKey)
                else Int.MAX_VALUE,
            executionContext = executionContext,
        )
}

private class NumericAdapter(functions: Functions) :
    FunctionAdapter(A2uiNumericFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.numeric(
            value = A2uiFunctionArgParser.getDoubleArg(args, ValueKey),
            min =
                if (args.containsKey(MinKey)) A2uiFunctionArgParser.getDoubleArg(args, MinKey)
                else Double.NEGATIVE_INFINITY,
            max =
                if (args.containsKey(MaxKey)) A2uiFunctionArgParser.getDoubleArg(args, MaxKey)
                else Double.POSITIVE_INFINITY,
            executionContext = executionContext,
        )
}

private class EmailAdapter(functions: Functions) :
    FunctionAdapter(A2uiEmailFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.email(
            value = A2uiFunctionArgParser.getStringArg(args, ValueKey),
            executionContext = executionContext,
        )
}

private class FormatStringAdapter(functions: Functions) :
    FunctionAdapter(A2uiFormatStringFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.formatString(
            value = A2uiFunctionArgParser.getStringArg(args, ValueKey),
            executionContext = executionContext,
        )
}

private class FormatNumberAdapter(functions: Functions) :
    FunctionAdapter(A2uiFormatNumberFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.formatNumber(
            value = A2uiFunctionArgParser.getDoubleArg(args, ValueKey),
            decimals =
                if (args.containsKey(DecimalsKey)) {
                    A2uiFunctionArgParser.getIntArg(args, DecimalsKey)
                } else {
                    null
                },
            grouping =
                if (args.containsKey(GroupingKey)) {
                    A2uiFunctionArgParser.getBooleanArg(args, GroupingKey)
                } else {
                    true
                },
            executionContext = executionContext,
        )
}

private class FormatCurrencyAdapter(functions: Functions) :
    FunctionAdapter(A2uiFormatCurrencyFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.formatCurrency(
            value = A2uiFunctionArgParser.getDoubleArg(args, ValueKey),
            currency = A2uiFunctionArgParser.getStringArg(args, CurrencyKey),
            decimals =
                if (args.containsKey(DecimalsKey)) {
                    A2uiFunctionArgParser.getDoubleArg(args, DecimalsKey).toInt()
                } else {
                    null
                },
            grouping =
                if (args.containsKey(GroupingKey)) {
                    A2uiFunctionArgParser.getBooleanArg(args, GroupingKey)
                } else {
                    true
                },
            executionContext = executionContext,
        )
}

private class FormatDateAdapter(functions: Functions) :
    FunctionAdapter(A2uiFormatDateFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.formatDate(
            epochMillis = A2uiFunctionArgParser.getEpochMillisArg(args, ValueKey),
            format = A2uiFunctionArgParser.getStringArg(args, FormatKey),
            executionContext = executionContext,
        )
}

private class PluralizeAdapter(functions: Functions) :
    FunctionAdapter(A2uiPluralizeFunction.DEFINITION, functions) {
    // Parses the arguments in the same order as A2uiPluralizeFunction, so that calls with several
    // invalid arguments fail on the same argument.
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? =
        functions.pluralize(
            value = A2uiFunctionArgParser.getDoubleArg(args, ValueKey),
            other = A2uiFunctionArgParser.getStringArg(args, OtherKey),
            zero = args.getOptionalStringArg(ZeroKey),
            one = args.getOptionalStringArg(OneKey),
            two = args.getOptionalStringArg(TwoKey),
            few = args.getOptionalStringArg(FewKey),
            many = args.getOptionalStringArg(ManyKey),
            executionContext = executionContext,
        )
}

private class OpenUrlAdapter(functions: Functions) :
    FunctionAdapter(A2uiOpenUrlFunction.DEFINITION, functions) {
    override fun execute(args: Map<String, Any>, executionContext: A2uiExecutionContext): Any? {
        val url = A2uiFunctionArgParser.getStringArg(args, UrlKey)
        // Checks the URL for every implementation, as A2uiOpenUrlFunction does.
        if (url.isEmpty()) {
            throw A2uiRuntimeException("Function ${definition.name} was invoked with an empty URL")
        }
        functions.openUrl(url = url, executionContext = executionContext)
        return Unit
    }
}

private fun Map<String, Any>.getOptionalStringArg(key: String): String? =
    if (containsKey(key)) A2uiFunctionArgParser.getStringArg(this, key) else null

private const val ValueKey = "value"
private const val PatternKey = "pattern"
private const val MinKey = "min"
private const val MaxKey = "max"
private const val DecimalsKey = "decimals"
private const val GroupingKey = "grouping"
private const val CurrencyKey = "currency"
private const val FormatKey = "format"
private const val ZeroKey = "zero"
private const val OneKey = "one"
private const val TwoKey = "two"
private const val FewKey = "few"
private const val ManyKey = "many"
private const val OtherKey = "other"
private const val UrlKey = "url"

/**
 * An [A2uiMessageFormatter] backed by the platform ICU [MessageFormat].
 *
 * Resolves `plural` and `select` arguments with the CLDR rules of the requested [Locale].
 */
internal object IcuMessageFormatter : A2uiMessageFormatter {

    /**
     * Formats [pattern] with [arguments] for [locale].
     *
     * @throws IllegalArgumentException if [pattern] is malformed or references an argument missing
     *   from [arguments]
     */
    override fun format(pattern: String, locale: Locale, arguments: Map<String, Any>): String {
        // ICU renders a missing argument as a literal "{name}" placeholder instead of failing, so
        // enforce the A2uiMessageFormatter contract by checking every referenced argument,
        // including those nested in plural and select sub-messages.
        val missingArguments = MessagePattern(pattern).argumentNames() - arguments.keys
        require(missingArguments.isEmpty()) {
            "Message pattern references missing arguments $missingArguments: $pattern"
        }
        return MessageFormat(pattern, locale).format(arguments)
    }

    /** Returns the names of all arguments referenced at any nesting level of this pattern. */
    private fun MessagePattern.argumentNames(): Set<String> = buildSet {
        for (index in 0 until countParts()) {
            val part = getPart(index)
            if (
                part.type == MessagePattern.Part.Type.ARG_NAME ||
                    part.type == MessagePattern.Part.Type.ARG_NUMBER
            ) {
                add(getSubstring(part))
            }
        }
    }
}
