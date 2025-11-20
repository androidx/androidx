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

package androidx.wear.protolayout.material3

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.expression.mapTo
import androidx.wear.protolayout.material3.EdgeButtonDefaults.CONTAINER_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.EDGE_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.EDGE_BUTTON_HEIGHT_FALLBACK_DP
import androidx.wear.protolayout.material3.EdgeButtonFallbackDefaults.ICON_SIZE_FALLBACK_DP
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.LayoutElementMatcher
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasContentDescription
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasImage
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.testing.hasWidth
import androidx.wear.protolayout.testing.isClickable
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutConstraint
import androidx.wear.protolayout.types.dp
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class EdgeButtonTest {
    @Test
    fun containerSize() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onRoot()
            .assert(hasWidth(DEVICE_CONFIGURATION.screenWidthDp.toDp()))
            .assert(hasHeight(CONTAINER_HEIGHT_DP.dp))
    }

    @Test
    fun visibleHeight() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasHeight(EDGE_BUTTON_HEIGHT_DP.dp))
    }

    @Test
    fun contentDescription() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasContentDescription(CONTENT_DESCRIPTION))
    }

    @Test
    fun defaultBackgroundColor() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasColor(COLOR_SCHEME.primary.staticArgb))
    }

    @Test
    fun icon() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON).onElement(hasImage(RES_ID)).assertExists()
    }

    @Test
    fun iconColor() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(hasImage(RES_ID))
            .assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))
    }

    @Test
    fun customColors() {
        val queryProvider =
            LayoutElementAssertionsProvider(
                materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                    iconEdgeButton(
                        onClick = CLICKABLE,
                        modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                        colors =
                            ButtonColors(
                                containerColor = COLOR_SCHEME.tertiaryContainer,
                                iconColor = COLOR_SCHEME.onTertiary,
                            ),
                    ) {
                        icon(RES_ID)
                    }
                }
            )

        queryProvider
            .onElement(isClickable())
            .assert(hasColor(COLOR_SCHEME.tertiaryContainer.staticArgb))
        queryProvider
            .onElement(
                LayoutElementMatcher("Element type is Image") { element -> element is Image }
            )
            .assert(hasColor(COLOR_SCHEME.onTertiary.staticArgb))
    }

    @Test
    fun staticText() {
        val label = "static text"
        val textEdgeButton =
            materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                textEdgeButton(onClick = CLICKABLE) { text(label.layoutString) }
            }

        val queryProvider = LayoutElementAssertionsProvider(textEdgeButton)
        queryProvider.onElement(hasText(label)).assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))

        queryProvider.onElement(isClickable()).assert(hasContentDescription(label))
    }

    @Test
    fun staticText_withCustomContentDescription() {
        val label = "static text"
        val textEdgeButton =
            materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                textEdgeButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                ) {
                    text(text = label.layoutString)
                }
            }

        val queryProvider = LayoutElementAssertionsProvider(textEdgeButton)
        queryProvider.onElement(isClickable()).assert(hasContentDescription(CONTENT_DESCRIPTION))
    }

    @Test
    fun dynamicText() {
        val label = "test text"
        val stateKey = AppDataKey<DynamicInt32>("testKey")
        val testValue = 12
        val testTimes = 2
        val dynamicLabel =
            LayoutString(
                label,
                DynamicInt32.from(stateKey).times(testTimes).format(),
                label.asLayoutConstraint(),
            )

        val queryProvider =
            LayoutElementAssertionsProvider(
                    materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                        textEdgeButton(onClick = CLICKABLE) { this.text(dynamicLabel) }
                    }
                )
                .withDynamicData(stateKey mapTo testValue)

        val expectedText = (testValue * testTimes).toString()
        queryProvider
            .onElement(hasText(expectedText))
            .assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))

        queryProvider.onElement(isClickable()).assert(hasContentDescription(expectedText))
    }

    @Test
    fun containerFallbackSize() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON_FALLBACK)
            .onRoot()
            .assert(hasWidth(DEVICE_CONFIGURATION.screenWidthDp.toDp()))
            .assert(hasHeight(CONTAINER_HEIGHT_DP.dp))
    }

    @Test
    fun visibleFallbackHeight() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON_FALLBACK)
            .onElement(isClickable())
            .assert(hasHeight(EDGE_BUTTON_HEIGHT_FALLBACK_DP.dp))
    }

    @Test
    fun iconFallbackSize() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON_FALLBACK)
            .onElement(hasImage(RES_ID))
            .assert(hasWidth(ICON_SIZE_FALLBACK_DP.dp))
            .assert(hasHeight(ICON_SIZE_FALLBACK_DP.dp))
    }

    companion object {
        private val CONTEXT = getApplicationContext() as Context
        private val COLOR_SCHEME = ColorScheme()

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .setRendererSchemaVersion(VersionInfo.Builder().setMajor(99).setMinor(999).build())
                .build()

        private val DEVICE_CONFIGURATION_WITH_OLD_RENDERER =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .setRendererSchemaVersion(VersionInfo.Builder().setMajor(1).setMinor(302).build())
                .build()

        private val CLICKABLE =
            clickable(action = launchAction(ComponentName("pkg", "cls")), id = "action_id")

        private const val CONTENT_DESCRIPTION = "it is an edge button"

        private const val RES_ID = "resId"
        private val ICON_EDGE_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                iconEdgeButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                ) {
                    icon(RES_ID)
                }
            }
        private val ICON_EDGE_BUTTON_FALLBACK =
            materialScope(
                CONTEXT,
                DEVICE_CONFIGURATION_WITH_OLD_RENDERER,
                allowDynamicTheme = false,
            ) {
                iconEdgeButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                ) {
                    icon(RES_ID)
                }
            }
    }
}
