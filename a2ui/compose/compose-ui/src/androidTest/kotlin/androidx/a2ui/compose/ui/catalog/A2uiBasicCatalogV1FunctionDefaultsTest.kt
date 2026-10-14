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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.a2ui.model.catalog.A2uiFunction
import androidx.a2ui.model.catalog.A2uiFunctionDefinition
import androidx.a2ui.model.protocol.A2uiDataPath
import androidx.a2ui.model.protocol.A2uiExecutionContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.text.NumberFormat
import java.util.Locale
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class A2uiBasicCatalogV1FunctionDefaultsTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun functions_withContext_opensUrlWithBrowsableViewIntent() {
        val recordingContext = RecordingContext(context)
        val functions = A2uiBasicCatalogV1.Functions(recordingContext)

        functions
            .function("openUrl")
            .execute(mapOf("url" to "https://example.com"), TestExecutionContext)

        val intent = recordingContext.startedIntents.single()
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data).isEqualTo(Uri.parse("https://example.com"))
        assertThat(intent.categories).containsExactly(Intent.CATEGORY_BROWSABLE)
    }

    @Test
    fun functions_withSameContextAndLocaleProvider_areEqual() {
        assertThat(A2uiBasicCatalogV1.Functions(context))
            .isEqualTo(A2uiBasicCatalogV1.Functions(context))
        assertThat(A2uiBasicCatalogV1.Functions(context).hashCode())
            .isEqualTo(A2uiBasicCatalogV1.Functions(context).hashCode())
        assertThat(A2uiBasicCatalogV1.Functions(context))
            .isNotEqualTo(A2uiBasicCatalogV1.Functions(context) { Locale.US })
    }

    @Test
    fun functions_withLocaleProvider_formatsWithLocale() {
        val polish = Locale.forLanguageTag("pl-PL")

        val functions = A2uiBasicCatalogV1.Functions(context, localeProvider = { polish })

        // Polish uses the "few" plural category for 3, which needs both ICU plural rules and the
        // provided locale.
        val pluralizeArgs =
            mapOf(
                "value" to 3,
                "one" to "# plik",
                "few" to "# pliki",
                "many" to "# plików",
                "other" to "# pliku",
            )
        assertThat(functions.function("pluralize").execute(pluralizeArgs, TestExecutionContext))
            .isEqualTo("3 pliki")
        assertThat(
                functions.formatNumber(
                    value = 1234.5,
                    decimals = 2,
                    grouping = true,
                    executionContext = TestExecutionContext,
                )
            )
            .isEqualTo(
                NumberFormat.getNumberInstance(polish)
                    .apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                    }
                    .format(1234.5)
            )
    }

    @Test
    fun pluralize_default_usesDefaultLocale() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("pl-PL"))

            val result =
                OpenUrlOnlyFunctions.pluralize(
                    value = 3.0,
                    zero = null,
                    one = "# plik",
                    two = null,
                    few = "# pliki",
                    many = "# plików",
                    other = "# pliku",
                    executionContext = TestExecutionContext,
                )

            assertThat(result).isEqualTo("3 pliki")
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun pluralize_withApostrophe_rendersApostropheLiterally() {
        val functions = DefaultFunctions(urlOpener = {}, localeProvider = { Locale.US })

        val result =
            functions.pluralize(
                value = 1.0,
                zero = null,
                one = "It's # item",
                two = null,
                few = null,
                many = null,
                other = "It's # items",
                executionContext = TestExecutionContext,
            )

        assertThat(result).isEqualTo("It's 1 item")
    }

    @Test
    fun intentUrlOpener_httpsUrl_startsBrowsableViewIntentInNewTask() {
        val recordingContext = RecordingContext(context)
        val urlOpener = IntentUrlOpener(recordingContext)

        urlOpener.openUrl("https://example.com/path?query=1")

        val intent = recordingContext.startedIntents.single()
        assertThat(intent.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(intent.data).isEqualTo(Uri.parse("https://example.com/path?query=1"))
        assertThat(intent.categories).containsExactly(Intent.CATEGORY_BROWSABLE)
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
            .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun intentUrlOpener_uppercaseScheme_normalizesScheme() {
        val recordingContext = RecordingContext(context)
        val urlOpener = IntentUrlOpener(recordingContext)

        urlOpener.openUrl("HTTPS://example.com")

        assertThat(recordingContext.startedIntents.single().data)
            .isEqualTo(Uri.parse("https://example.com"))
    }

    @Test
    fun intentUrlOpener_nonApplicationContext_startsActivityFromApplicationContext() {
        val applicationContext = RecordingContext(context)
        val activityContext = ActivityContext(base = context, application = applicationContext)
        val urlOpener = IntentUrlOpener(activityContext)

        urlOpener.openUrl("https://example.com")

        assertThat(applicationContext.startedIntents).hasSize(1)
    }

    @Test
    fun intentUrlOpener_noMatchingActivity_throwsActivityNotFoundException() {
        val urlOpener = IntentUrlOpener(context)

        assertFailsWith<ActivityNotFoundException> {
            urlOpener.openUrl("a2ui-test-unhandled-scheme://example")
        }
    }

    @Test
    fun intentUrlOpener_sameApplicationContext_isEqual() {
        val applicationContext = RecordingContext(context)
        val activityContext = ActivityContext(base = context, application = applicationContext)

        assertThat(IntentUrlOpener(activityContext)).isEqualTo(IntentUrlOpener(applicationContext))
        assertThat(IntentUrlOpener(activityContext).hashCode())
            .isEqualTo(IntentUrlOpener(applicationContext).hashCode())
        assertThat(IntentUrlOpener(activityContext)).isNotEqualTo(IntentUrlOpener(context))
    }

    @Test
    fun icuMessageFormatter_namedArgument_replacesPlaceholder() {
        val result =
            IcuMessageFormatter.format("Hello, {name}!", Locale.US, mapOf("name" to "A2UI"))

        assertThat(result).isEqualTo("Hello, A2UI!")
    }

    @Test
    fun icuMessageFormatter_pluralPattern_selectsEnglishCategory() {
        val pattern = "{count, plural, one {# item} other {# items}}"

        assertThat(IcuMessageFormatter.format(pattern, Locale.US, mapOf("count" to 1.0)))
            .isEqualTo("1 item")
        assertThat(IcuMessageFormatter.format(pattern, Locale.US, mapOf("count" to 5.0)))
            .isEqualTo("5 items")
    }

    @Test
    fun icuMessageFormatter_pluralPattern_selectsLocaleSpecificCategory() {
        val polish = Locale.forLanguageTag("pl-PL")
        val pattern = "{count, plural, one {# plik} few {# pliki} many {# plików} other {# pliku}}"

        assertThat(IcuMessageFormatter.format(pattern, polish, mapOf("count" to 1.0)))
            .isEqualTo("1 plik")
        assertThat(IcuMessageFormatter.format(pattern, polish, mapOf("count" to 3.0)))
            .isEqualTo("3 pliki")
        assertThat(IcuMessageFormatter.format(pattern, polish, mapOf("count" to 5.0)))
            .isEqualTo("5 plików")
    }

    @Test
    fun icuMessageFormatter_escapedApostrophe_rendersSingleApostrophe() {
        val result =
            IcuMessageFormatter.format(
                "{count, plural, other {It''s # items}}",
                Locale.US,
                mapOf("count" to 3.0),
            )

        assertThat(result).isEqualTo("It's 3 items")
    }

    @Test
    fun icuMessageFormatter_extraArguments_ignoresUnreferencedArguments() {
        val result =
            IcuMessageFormatter.format(
                "Hello, {name}!",
                Locale.US,
                mapOf("name" to "A2UI", "unused" to 1),
            )

        assertThat(result).isEqualTo("Hello, A2UI!")
    }

    @Test
    fun icuMessageFormatter_missingArgument_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IcuMessageFormatter.format("Hello, {name}!", Locale.US, emptyMap())
        }
    }

    @Test
    fun icuMessageFormatter_missingNestedArgument_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IcuMessageFormatter.format(
                "{count, plural, other {{name} has # items}}",
                Locale.US,
                mapOf("count" to 2.0),
            )
        }
    }

    @Test
    fun icuMessageFormatter_malformedPattern_throwsIllegalArgumentException() {
        assertFailsWith<IllegalArgumentException> {
            IcuMessageFormatter.format(
                "{count, plural, other {# items}",
                Locale.US,
                mapOf("count" to 2.0),
            )
        }
    }

    private fun A2uiBasicCatalogV1.Functions.function(name: String): A2uiFunction =
        toA2uiFunctions().single { it.definition.name == name }

    /** Implements only `openUrl`, so all other functions use the default implementations. */
    private object OpenUrlOnlyFunctions : A2uiBasicCatalogV1.Functions {
        override fun openUrl(url: String, executionContext: A2uiExecutionContext) {}
    }

    /** An [A2uiExecutionContext] for functions that don't access the data model or functions. */
    private object TestExecutionContext : A2uiExecutionContext {
        override val dataPath: A2uiDataPath = A2uiDataPath("/")

        override fun evaluatePayload(payload: Any?): Any? = throw UnsupportedOperationException()

        override fun executeFunction(name: String, args: Map<String, Any>): Any? =
            throw UnsupportedOperationException()

        override fun resolveValue(path: A2uiDataPath): Any? = throw UnsupportedOperationException()

        override fun <T : Any> getOrCreateFunctionScopedCache(
            functionDefinition: A2uiFunctionDefinition,
            factory: () -> T,
        ): T = factory()
    }

    /** Records started intents instead of launching activities. */
    private class RecordingContext(base: Context) : ContextWrapper(base) {
        val startedIntents = mutableListOf<Intent>()

        override fun getApplicationContext(): Context = this

        override fun startActivity(intent: Intent) {
            startedIntents += intent
        }
    }

    /** A non-application context, such as an activity, that must not start activities itself. */
    private class ActivityContext(base: Context, private val application: Context) :
        ContextWrapper(base) {
        override fun getApplicationContext(): Context = application

        override fun startActivity(intent: Intent) {
            throw AssertionError("Activities must be started from the application context")
        }
    }
}
