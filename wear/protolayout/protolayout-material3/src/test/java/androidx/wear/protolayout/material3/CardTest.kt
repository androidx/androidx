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

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.CENTER_ICON_SIZE_RATIO_IN_GRAPHIC
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.constructGraphic
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.background
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.containsTag
import androidx.wear.protolayout.testing.hasClickable
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasContentDescription
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasImage
import androidx.wear.protolayout.testing.hasTag
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.testing.hasWidth
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.dp
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class CardTest {
    @Test
    fun containerCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun titleCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun appCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun dataCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_DATA_CARD)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun graphicDataCard_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_GRAPHIC_DATA_CARD)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasContentDescription() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasContentDescription(CONTENT_DESCRIPTION))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasClickable() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onRoot()
            .assert(hasClickable(id = CLICKABLE.id))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun titleCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun appCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun dataCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_DATA_CARD)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun graphicDataCard_hasTitle_asText() {
        LayoutElementAssertionsProvider(DEFAULT_GRAPHIC_DATA_CARD)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun titleCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun appCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun dataCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_DATA_CARD)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun graphicDataCard_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_GRAPHIC_DATA_CARD)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun titleCard_hasTime_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TITLE_CARD_WITH_TEXT)
            .onElement(hasText(TEXT3))
            .assertExists()
    }

    @Test
    fun appCard_hasTime_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT3))
            .assertExists()
    }

    @Test
    fun appCard_hasLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasText(TEXT4))
            .assertExists()
    }

    @Test
    fun appCard_hasAvatar() {
        LayoutElementAssertionsProvider(DEFAULT_APP_CARD_WITH_TEXT)
            .onElement(hasImage(AVATAR_ID))
            .assertExists()
    }

    @Test
    fun dataCard_hasIcon() {
        LayoutElementAssertionsProvider(DEFAULT_DATA_CARD_WITH_ICON)
            .onElement(hasImage(AVATAR_ID))
            .assertExists()
    }

    @Test
    fun dataCard_hasSecondaryText() {
        LayoutElementAssertionsProvider(DEFAULT_DATA_CARD_WITH_SECONDARY_TEXT)
            .onElement(hasText(TEXT4))
            .assertExists()
    }

    @Test
    fun graphicDataCard_hasGraphic() {
        LayoutElementAssertionsProvider(DEFAULT_GRAPHIC_DATA_CARD)
            .onElement(containsTag(CircularProgressIndicatorDefaults.METADATA_TAG))
            .assertExists()
    }

    @Test
    fun containerCard_hasBackgroundImage() {
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    backgroundContent = { backgroundImage(IMAGE_ID) }
                ) {
                    text(TEXT.layoutString)
                }
            }

        LayoutElementAssertionsProvider(card).onElement(hasImage(IMAGE_ID)).assertExists()
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun containerCard_hasBackgroundColor() {
        val color = Color.YELLOW
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier =
                        LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                            .background(color.argb)
                            .clickable(id = "id")
                ) {
                    text(TEXT.layoutString)
                }
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(color))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun titleCard_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val timeColor = Color.CYAN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                titleCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = backgroundColor.argb,
                            titleColor = titleColor.argb,
                            contentColor = contentColor.argb,
                            timeColor = timeColor.argb
                        ),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    time = { text(TEXT3.layoutString) },
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT3)).assert(hasColor(timeColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun appCard_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val timeColor = Color.CYAN
        val labelColor = Color.GREEN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                appCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = backgroundColor.argb,
                            titleColor = titleColor.argb,
                            contentColor = contentColor.argb,
                            timeColor = timeColor.argb,
                            labelColor = labelColor.argb
                        ),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    time = { text(TEXT3.layoutString) },
                    label = { text(TEXT4.layoutString) },
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT3)).assert(hasColor(timeColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT4)).assert(hasColor(labelColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun dataCard_withIcon_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val iconColor = Color.CYAN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                iconDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = backgroundColor.argb,
                            titleColor = titleColor.argb,
                            contentColor = contentColor.argb,
                            secondaryIconColor = iconColor.argb
                        ),
                    title = { this.text(TEXT.layoutString) },
                    content = { this.text(TEXT2.layoutString) },
                    secondaryIcon = { icon(AVATAR_ID) }
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasImage(AVATAR_ID))
            .assert(hasColor(iconColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun dataCard_withSecondaryText_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val secondaryLabelColor = Color.CYAN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                textDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = backgroundColor.argb,
                            titleColor = titleColor.argb,
                            contentColor = contentColor.argb,
                            secondaryTextColor = secondaryLabelColor.argb
                        ),
                    title = { this.text(TEXT.layoutString) },
                    content = { this.text(TEXT2.layoutString) },
                    secondaryText = { this.text(TEXT4.layoutString) }
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT4))
            .assert(hasColor(secondaryLabelColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun graphicDataCard_withSecondaryLabel_hasColors() {
        val titleColor = Color.YELLOW
        val contentColor = Color.MAGENTA
        val graphicColor = Color.CYAN
        val backgroundColor = Color.BLUE
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                graphicDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = backgroundColor.argb,
                            titleColor = titleColor.argb,
                            contentColor = contentColor.argb,
                        ),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    graphic = { text(TEXT_GRAPHIC.layoutString, color = graphicColor.argb) }
                )
            }

        LayoutElementAssertionsProvider(card).onRoot().assert(hasColor(backgroundColor))
        LayoutElementAssertionsProvider(card).onElement(hasText(TEXT)).assert(hasColor(titleColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT2))
            .assert(hasColor(contentColor))
        LayoutElementAssertionsProvider(card)
            .onElement(hasText(TEXT_GRAPHIC))
            .assert(hasColor(graphicColor))
        LayoutElementAssertionsProvider(card).onRoot().assert(hasTag(CardDefaults.METADATA_TAG))
    }

    @Test
    fun graphicDataCard_useConstructGraphic_expandSize_inflates() {
        val graphicIconColor = Color.MAGENTA
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                graphicDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = Color.RED.argb,
                            titleColor = Color.GREEN.argb,
                            contentColor = Color.BLUE.argb,
                            graphicIconColor = graphicIconColor.argb,
                        ),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    graphic = {
                        constructGraphic(
                            mainContent = { circularProgressIndicator() },
                            iconContent = { icon(AVATAR_ID) }
                        )
                    }
                )
            }

        LayoutElementAssertionsProvider(card)
            .onElement(hasImage(AVATAR_ID))
            .assert(hasColor(graphicIconColor))
            .assert(hasWidth(expand()))
            .assert(
                hasHeight(
                    DimensionBuilders.ProportionalDimensionProp.Builder()
                        .setAspectRatioWidth(1)
                        .setAspectRatioHeight(1)
                        .build()
                )
            )
    }

    @Test
    fun graphicDataCard_useConstructGraphic_dpSize_inflates() {
        val graphicIconColor = Color.MAGENTA
        val size = 50F.dp
        val expectedIconSize = (size.value * CENTER_ICON_SIZE_RATIO_IN_GRAPHIC).dp
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                graphicDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    colors =
                        CardColors(
                            backgroundColor = Color.RED.argb,
                            titleColor = Color.GREEN.argb,
                            contentColor = Color.BLUE.argb,
                            graphicIconColor = graphicIconColor.argb,
                        ),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    graphic = {
                        constructGraphic(
                            mainContent = { circularProgressIndicator(size = size) },
                            iconContent = { icon(AVATAR_ID) }
                        )
                    }
                )
            }

        LayoutElementAssertionsProvider(card)
            .onElement(hasImage(AVATAR_ID))
            .assert(hasColor(graphicIconColor))
            .assert(hasWidth(expectedIconSize))
            .assert(hasHeight(expectedIconSize))
    }

    // TODO: b/381518061 - Add test for corner shape.

    @Test
    fun containerCard_hasGivenSize() {
        val height = 12
        val card =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    width = expand(),
                    height = height.toDp()
                ) {
                    text(TEXT.layoutString)
                }
            }

        LayoutElementAssertionsProvider(card)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight((height.toDp())))
            .assert(hasTag(CardDefaults.METADATA_TAG))
    }

    companion object {
        private val CONTEXT = getApplicationContext() as Context

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .setRendererSchemaVersion(VersionInfo.Builder().setMajor(99).setMinor(999).build())
                .build()

        private const val CONTENT_DESCRIPTION = "This is a card"

        private const val IMAGE_ID = "image"

        private const val TEXT = "Container card"
        private const val TEXT2 = "Description"
        private const val TEXT3 = "Now"
        private const val TEXT4 = "Label"
        private const val TEXT_GRAPHIC = "Graphic"
        private const val AVATAR_ID = "id"

        private val DEFAULT_CONTAINER_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                card(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                ) {
                    text(TEXT.layoutString)
                }
            }

        private val DEFAULT_TITLE_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                titleCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    time = { text(TEXT3.layoutString) },
                )
            }

        private val DEFAULT_APP_CARD_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                appCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    time = { text(TEXT3.layoutString) },
                    avatar = { avatarImage(AVATAR_ID) },
                    label = { text(TEXT4.layoutString) }
                )
            }

        private val DEFAULT_DATA_CARD_WITH_ICON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                iconDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { this.text(TEXT.layoutString) },
                    content = { this.text(TEXT2.layoutString) },
                    secondaryIcon = { avatarImage(AVATAR_ID) }
                )
            }

        private val DEFAULT_DATA_CARD_WITH_SECONDARY_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                textDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { this.text(TEXT.layoutString) },
                    content = { this.text(TEXT2.layoutString) },
                    secondaryText = { this.text(TEXT4.layoutString) }
                )
            }

        private val DEFAULT_COMPACT_DATA_CARD =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                iconDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { this.text(TEXT.layoutString) },
                    content = { this.text(TEXT2.layoutString) },
                )
            }

        private val DEFAULT_GRAPHIC_DATA_CARD =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                graphicDataCard(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    title = { text(TEXT.layoutString) },
                    content = { text(TEXT2.layoutString) },
                    graphic = { circularProgressIndicator() }
                )
            }
    }
}
