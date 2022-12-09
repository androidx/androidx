/*
 * Copyright 2022 The Android Open Source Project
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

package android.support.wearable.complications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.support.wearable.complications.ComplicationText.plainText
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.complications.data.ComplicationDisplayPolicies
import androidx.wear.watchface.complications.data.ComplicationPersistencePolicies
import androidx.wear.watchface.complications.data.FloatExpression
import androidx.wear.watchface.complications.data.SharedRobolectricTestRunner
import androidx.wear.watchface.complications.data.StringExpression
import com.google.common.truth.Expect
import kotlin.random.Random
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@RunWith(SharedRobolectricTestRunner::class)
class ComplicationDataEqualityTest {
    @get:Rule
    val expect = Expect.create()

    @Before
    fun setup() {
        ShadowLog.setLoggable("ComplicationData", Log.DEBUG)
    }

    /**
     * Testing equality by generating a scenario per field.
     *
     * Each value of the enum contains two setters that describes how to build a [ComplicationData]
     * in two variations that should not equal each other, like this:
     * ```
     * TEST_NAME(
     *   { setField(<variation one>) },
     *   { setField(<variation two>) },
     * )
     * ```
     *
     * It is then verified that two instances built with `<variation one>` are equal, but different
     * from an instance built with `<variation two>`.
     */
    private enum class EqualsScenario(
        val setterOne: ComplicationData.Builder.() -> Unit,
        val setterTwo: ComplicationData.Builder.() -> Unit,
    ) {
        PERSISTENCE_POLICY(
            { setPersistencePolicy(ComplicationPersistencePolicies.CACHING_ALLOWED) },
            { setPersistencePolicy(ComplicationPersistencePolicies.DO_NOT_PERSIST) },
        ),
        DISPLAY_POLICY(
            { setDisplayPolicy(ComplicationDisplayPolicies.ALWAYS_DISPLAY) },
            { setDisplayPolicy(ComplicationDisplayPolicies.DO_NOT_SHOW_WHEN_DEVICE_LOCKED) },
        ),
        START_DATE_TIME_MILLIS(
            { setStartDateTimeMillis(1) },
            { setStartDateTimeMillis(2) },
        ),
        END_DATE_TIME_MILLIS(
            { setEndDateTimeMillis(1) },
            { setEndDateTimeMillis(2) },
        ),
        RANGED_VALUE(
            { setRangedValue(1f) },
            { setRangedValue(2f) },
        ),
        RANGED_VALUE_EXPRESSION(
            {
                setRangedValueExpression(
                    object : FloatExpression() {
                        override fun asByteArray() = byteArrayOf(1, 2)
                    }
                )
            },
            {
                setRangedValueExpression(
                    object : FloatExpression() {
                        override fun asByteArray() = byteArrayOf(3, 4)
                    }
                )
            },
        ),
        RANGED_VALUE_TYPE(
            { setRangedValueType(1) },
            { setRangedValueType(2) },
        ),
        RANGED_MIN_VALUE(
            { setRangedMinValue(1f) },
            { setRangedMinValue(2f) },
        ),
        RANGED_MAX_VALUE(
            { setRangedMaxValue(1f) },
            { setRangedMaxValue(2f) },
        ),
        TARGET_VALUE(
            { setTargetValue(1f) },
            { setTargetValue(2f) },
        ),
        LONG_TITLE(
            { setLongTitle(plainText("1")) },
            { setLongTitle(plainText("2")) },
        ),
        LONG_TEXT(
            { setLongText(plainText("1")) },
            { setLongText(plainText("2")) },
        ),
        SHORT_TITLE(
            { setShortTitle(plainText("1")) },
            { setShortTitle(plainText("2")) },
        ),
        SHORT_TEXT(
            { setShortText(plainText("1")) },
            { setShortText(plainText("2")) },
        ),
        ICON(
            { setIcon(Icon.createWithContentUri("1")) },
            { setIcon(Icon.createWithContentUri("2")) },
        ),
        BURN_IN_PROTECTION_ICON(
            { setBurnInProtectionIcon(Icon.createWithContentUri("1")) },
            { setBurnInProtectionIcon(Icon.createWithContentUri("2")) },
        ),
        SMALL_IMAGE(
            { setSmallImage(Icon.createWithContentUri("1")) },
            { setSmallImage(Icon.createWithContentUri("2")) },
        ),
        BURN_IN_PROTECTION_SMALL_IMAGE(
            { setBurnInProtectionSmallImage(Icon.createWithContentUri("1")) },
            { setBurnInProtectionSmallImage(Icon.createWithContentUri("2")) },
        ),
        SMALL_IMAGE_STYLE(
            { setSmallImageStyle(ComplicationData.IMAGE_STYLE_ICON) },
            { setSmallImageStyle(ComplicationData.IMAGE_STYLE_PHOTO) },
        ),
        LARGE_IMAGE(
            { setLargeImage(Icon.createWithContentUri("1")) },
            { setLargeImage(Icon.createWithContentUri("2")) },
        ),
        LIST_STYLE_HINT(
            { setListStyleHint(1) },
            { setListStyleHint(2) },
        ),
        TAP_ACTION(
            {
                setTapAction(
                    PendingIntent.getBroadcast(
                        ApplicationProvider.getApplicationContext(),
                        0,
                        Intent("1"),
                        0
                    )
                )
            },
            {
                setTapAction(
                    PendingIntent.getBroadcast(
                        ApplicationProvider.getApplicationContext(),
                        0,
                        Intent("2"),
                        0
                    )
                )
            },
        ),
        CONTENT_DESCRIPTION(
            { setContentDescription(plainText("1")) },
            { setContentDescription(plainText("2")) },
        ),
        TAP_ACTION_LOST_DUE_TO_SERIALIZATION(
            { setTapActionLostDueToSerialization(true) },
            { setTapActionLostDueToSerialization(false) },
        ),
        PLACEHOLDER(
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(plainText("1"))
                        .build()
                )
            },
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(plainText("2"))
                        .build()
                )
            },
        ),
        DATA_SOURCE(
            { setDataSource(ComponentName.createRelative("", "1")) },
            { setDataSource(ComponentName.createRelative("", "2")) },
        ),
        AMBIENT_LAYOUT(
            { setAmbientLayout(byteArrayOf(1, 2)) },
            { setAmbientLayout(byteArrayOf(3, 4)) },
        ),
        INTERACTIVE_LAYOUT(
            { setInteractiveLayout(byteArrayOf(1, 2)) },
            { setInteractiveLayout(byteArrayOf(3, 4)) },
        ),
        LAYOUT_RESOURCES(
            { setLayoutResources(byteArrayOf(1, 2)) },
            { setLayoutResources(byteArrayOf(3, 4)) },
        ),
        COLOR_RAMP(
            { setColorRamp(intArrayOf(1, 2)) },
            { setColorRamp(intArrayOf(3, 4)) },
        ),
        COLOR_RAMP_IS_SMOOTH_SHADED(
            { setColorRampIsSmoothShaded(true) },
            { setColorRampIsSmoothShaded(false) },
        ),
        LIST_ENTRY_COLLECTION(
            {
                setListEntryCollection(
                    listOf(
                        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(plainText("1"))
                            .build()
                    )
                )
            },
            {
                setListEntryCollection(
                    listOf(
                        ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                            .setShortText(plainText("2"))
                            .build()
                    )
                )
            },
        ),
        ELEMENT_WEIGHTS(
            { setElementWeights(floatArrayOf(1f, 2f)) },
            { setElementWeights(floatArrayOf(3f, 4f)) },
        ),
        ELEMENT_COLORS(
            { setElementColors(intArrayOf(1, 2)) },
            { setElementColors(intArrayOf(3, 4)) },
        ),
        ELEMENT_BACKGROUND_COLOR(
            { setElementBackgroundColor(1) },
            { setElementBackgroundColor(2) },
        ),
        ;

        /** Builds a [ComplicationData] with the first variation. */
        fun buildOne() =
            ComplicationData.Builder(ComplicationData.TYPE_NO_DATA)
                .apply { setterOne(this) }
                .build()

        /** Builds a [ComplicationData] with the second variation. */
        fun buildTwo() =
            ComplicationData.Builder(ComplicationData.TYPE_NO_DATA)
                .apply { setterTwo(this) }
                .build()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Icon comparison only works since P.
    public fun testEquals() {
        for (scenario in EqualsScenario.values()) {
            expect
                .withMessage("${scenario.name} equals same")
                .that(scenario.buildOne())
                .isEqualTo(scenario.buildOne())
            expect
                .withMessage("${scenario.name} does not equal another")
                .that(scenario.buildOne())
                .isNotEqualTo(scenario.buildTwo())
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Icon hashing only works since P.
    public fun testHashCode() {
        for (scenario in EqualsScenario.values()) {
            expect
                .withMessage("${scenario.name} equals same")
                .that(scenario.buildOne().hashCode())
                .isEqualTo(scenario.buildOne().hashCode())
            expect
                .withMessage("${scenario.name} does not equal another")
                .that(scenario.buildOne().hashCode())
                .isNotEqualTo(scenario.buildTwo().hashCode())
        }
    }

    /**
     * Testing equality by generating a scenario per field.
     *
     * Each value of the enum contains two setters that describes how to build a [ComplicationData]
     * in two variations that should not equal each other, like this:
     * ```
     * TEST_NAME(
     *   { setField(<variation one>) },
     *   { setField(<variation two>) },
     * )
     * ```
     *
     * It is then verified that two instances built with `<variation one>` are equal, but different
     * from an instance built with `<variation two>`.
     */
    private enum class EqualsUnevaluatedScenario(
        val setterOne: ComplicationData.Builder.() -> Unit,
        val setterTwo: ComplicationData.Builder.() -> Unit,
    ) {
        RANGED_VALUE_EXPRESSION(
            {
                setRangedValue(Random.nextFloat()) // Ignored when there's an expression.
                    .setRangedValueExpression(
                        object : FloatExpression() {
                            override fun asByteArray() = byteArrayOf(1, 2)
                        }
                    )
            },
            {
                setRangedValue(Random.nextFloat()) // Ignored when there's an expression.
                    .setRangedValueExpression(
                        object : FloatExpression() {
                            override fun asByteArray() = byteArrayOf(3, 4)
                        }
                    )
            },
        ),

        // Not ignored without an expression.
        RANGED_VALUE_NO_EXPRESSION(
            { setRangedValue(1f) },
            { setRangedValue(2f) },
        ),
        SHORT_TITLE_EXPRESSION(
            {
                setShortTitle(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(1, 2))
                    )
                )
            },
            {
                setShortTitle(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(3, 4))
                    )
                )
            },
        ),

        // Not ignored without an expression.
        SHORT_TITLE_NO_EXPRESSION(
            { setShortTitle(plainText("1")) },
            { setShortTitle(plainText("2")) },
        ),
        SHORT_TEXT_EXPRESSION(
            {
                setShortText(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(1, 2))
                    )
                )
            },
            {
                setShortText(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(3, 4))
                    )
                )
            },
        ),

        // Not ignored without an expression.
        SHORT_TEXT_NO_EXPRESSION(
            { setShortText(plainText("1")) },
            { setShortText(plainText("2")) },
        ),
        LONG_TITLE_EXPRESSION(
            {
                setLongTitle(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(1, 2))
                    )
                )
            },
            {
                setLongTitle(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(3, 4))
                    )
                )
            },
        ),

        // Not ignored without an expression.
        LONG_TITLE_NO_EXPRESSION(
            { setLongTitle(plainText("1")) },
            { setLongTitle(plainText("2")) },
        ),
        LONG_TEXT_EXPRESSION(
            {
                setLongText(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(1, 2))
                    )
                )
            },
            {
                setLongText(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(3, 4))
                    )
                )
            },
        ),

        // Not ignored without an expression.
        LONG_TEXT_NO_EXPRESSION(
            { setLongText(plainText("1")) },
            { setLongText(plainText("2")) },
        ),
        CONTENT_DESCRIPTION_EXPRESSION(
            {
                setContentDescription(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(1, 2))
                    )
                )
            },
            {
                setContentDescription(
                    ComplicationText(
                        Random.nextInt().toString(), // Ignored when there's an expression.
                        /* timeDependentText = */ null,
                        StringExpression(byteArrayOf(3, 4))
                    )
                )
            },
        ),

        // Not ignored without an expression.
        CONTENT_DESCRIPTION_NO_EXPRESSION(
            { setContentDescription(plainText("1")) },
            { setContentDescription(plainText("2")) },
        ),
        PLACEHOLDER_EXPRESSION(
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(
                            ComplicationText(
                                Random.nextInt().toString(), // Ignored when there's an expression.
                                /* timeDependentText = */ null,
                                StringExpression(byteArrayOf(1, 2))
                            )
                        )
                        .build()
                )
            },
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(
                            ComplicationText(
                                Random.nextInt().toString(), // Ignored when there's an expression.
                                /* timeDependentText = */ null,
                                StringExpression(byteArrayOf(3, 4))
                            )
                        )
                        .build()
                )
            },
        ),

        // Not ignored without an expression.
        PLACEHOLDER_NO_EXPRESSION(
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(plainText("1"))
                        .build()
                )
            },
            {
                setPlaceholder(
                    ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                        .setShortText(plainText("2"))
                        .build()
                )
            },
        ),
        ;

        /** Builds a [ComplicationData] with the first variation. */
        fun buildOne() =
            ComplicationData.Builder(ComplicationData.TYPE_NO_DATA)
                .apply { setterOne(this) }
                .build()

        /** Builds a [ComplicationData] with the second variation. */
        fun buildTwo() =
            ComplicationData.Builder(ComplicationData.TYPE_NO_DATA)
                .apply { setterTwo(this) }
                .build()
    }

    @Test
    public fun equalsUnevaluated() {
        for (scenario in EqualsUnevaluatedScenario.values()) {
            expect
                .withMessage("${scenario.name} unevaluated equals same")
                .that(scenario.buildOne().equalsUnevaluated(scenario.buildOne()))
                .isTrue()
            expect
                .withMessage("${scenario.name} does not unevaluated equal another")
                .that(scenario.buildOne().equalsUnevaluated(scenario.buildTwo()))
                .isFalse()
        }
    }
}