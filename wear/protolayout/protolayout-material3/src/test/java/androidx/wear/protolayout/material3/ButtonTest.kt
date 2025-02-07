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
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.material3.CompactButtonStyle.COMPACT_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.backgroundColor
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasClickable
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasContentDescription
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasImage
import androidx.wear.protolayout.testing.hasTag
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.testing.hasWidth
import androidx.wear.protolayout.types.argb
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class ButtonTest {
    @Test
    fun containerButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_BUTTON_WITH_TEXT)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun iconButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_ICON_BUTTON)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun textButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_TEXT_BUTTON)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun pillButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_BUTTON)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun avatarButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_AVATAR_BUTTON)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight(wrapWithMinTapTargetDimension()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun compactButton_size_default() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_BUTTON)
            .onRoot()
            .assert(hasWidth(wrapWithMinTapTargetDimension()))
            // Invisible box
            .assert(hasHeight(MINIMUM_TAP_TARGET_SIZE))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))

        // Assert the visible height
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_BUTTON)
            .onElement(hasColor(ColorScheme().primary.staticArgb))
            .assert(hasHeight(dp(COMPACT_BUTTON_HEIGHT_DP)))
    }

    @Test
    fun imageButton_size_default() {
        LayoutElementAssertionsProvider(
                materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                    imageButton(
                        onClick = CLICKABLE,
                        modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                        backgroundContent = { backgroundImage(IMAGE_ID) }
                    )
                }
            )
            .onRoot()
            .assert(hasWidth(ButtonDefaults.IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()))
            .assert(hasHeight(ButtonDefaults.IMAGE_BUTTON_DEFAULT_SIZE_DP.toDp()))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    fun containerButton_hasContentDescription() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_BUTTON_WITH_TEXT)
            .onRoot()
            .assert(hasContentDescription(CONTENT_DESCRIPTION))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun containerButton_hasClickable() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_BUTTON_WITH_TEXT)
            .onRoot()
            .assert(hasClickable(id = CLICKABLE.id))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun containerButton_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_CONTAINER_BUTTON_WITH_TEXT)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun iconButton_hasContent_asIcon() {
        LayoutElementAssertionsProvider(DEFAULT_ICON_BUTTON)
            .onElement(hasImage(ICON_ID))
            .assertExists()
    }

    @Test
    fun textButton_hasContent_asText() {
        LayoutElementAssertionsProvider(DEFAULT_TEXT_BUTTON).onElement(hasText(TEXT)).assertExists()
    }

    @Test
    fun pillButton_hasLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_BUTTON).onElement(hasText(TEXT)).assertExists()
    }

    @Test
    fun pillButton_hasSecondaryLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_BUTTON).onElement(hasText(TEXT2)).assertExists()
    }

    @Test
    fun pillButton_hasIcon_asIcon() {
        LayoutElementAssertionsProvider(DEFAULT_BUTTON).onElement(hasImage(ICON_ID)).assertExists()
    }

    @Test
    fun avatarButton_hasLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_AVATAR_BUTTON)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun avatarButton_hasSecondaryLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_AVATAR_BUTTON)
            .onElement(hasText(TEXT2))
            .assertExists()
    }

    @Test
    fun avatarButton_hasAvatar_asImage() {
        LayoutElementAssertionsProvider(DEFAULT_AVATAR_BUTTON)
            .onElement(hasImage(IMAGE_ID))
            .assertExists()
    }

    @Test
    fun compactButton_hasLabel_asText() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_BUTTON)
            .onElement(hasText(TEXT))
            .assertExists()
    }

    @Test
    fun compactButton_hasIcon_asIcon() {
        LayoutElementAssertionsProvider(DEFAULT_COMPACT_BUTTON)
            .onElement(hasImage(ICON_ID))
            .assertExists()
    }

    @Test
    fun imageButton_hasBackgroundImage() {
        val button =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                imageButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    backgroundContent = { backgroundImage(IMAGE_ID) }
                )
            }

        LayoutElementAssertionsProvider(button).onElement(hasImage(IMAGE_ID)).assertExists()
        LayoutElementAssertionsProvider(button)
            .onRoot()
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    @Test
    fun containerButton_hasBackgroundColor() {
        val color = Color.YELLOW
        val button =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                buttonContainer(
                    onClick = CLICKABLE,
                    modifier =
                        LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                            .backgroundColor(color.argb),
                    content = { text(TEXT.layoutString) }
                )
            }

        LayoutElementAssertionsProvider(button).onRoot().assert(hasColor(color))
        LayoutElementAssertionsProvider(button)
            .onRoot()
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    // TODO: b/381518061 - Add test for corner shape.

    @Test
    fun containerButton_hasGivenSize() {
        val height = 12
        val button =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                buttonContainer(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    width = expand(),
                    height = height.toDp(),
                    content = { text(TEXT.layoutString) }
                )
            }

        LayoutElementAssertionsProvider(button)
            .onRoot()
            .assert(hasWidth(expand()))
            .assert(hasHeight((height.toDp())))
            .assert(hasTag(ButtonDefaults.METADATA_TAG_BUTTON))
    }

    companion object {
        private val CONTEXT = getApplicationContext() as Context

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .build()

        private const val CONTENT_DESCRIPTION = "This is a button"

        private const val IMAGE_ID = "image"

        private const val ICON_ID = "id"

        private const val TEXT = "Container button"
        private const val TEXT2 = "Secondary label"

        private val DEFAULT_CONTAINER_BUTTON_WITH_TEXT =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                buttonContainer(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    content = { text(TEXT.layoutString) }
                )
            }

        private val DEFAULT_ICON_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                iconButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    iconContent = { icon(ICON_ID) }
                )
            }

        private val DEFAULT_TEXT_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                textButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    labelContent = { text(TEXT.layoutString) }
                )
            }

        private val DEFAULT_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                button(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    labelContent = { text(TEXT.layoutString) },
                    secondaryLabelContent = { text(TEXT2.layoutString) },
                    iconContent = { icon(ICON_ID) }
                )
            }

        private val DEFAULT_AVATAR_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                avatarButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    labelContent = { text(TEXT.layoutString) },
                    secondaryLabelContent = { text(TEXT2.layoutString) },
                    avatarContent = { icon(IMAGE_ID) }
                )
            }

        private val DEFAULT_COMPACT_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION) {
                compactButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                    labelContent = { text(TEXT.layoutString) },
                    iconContent = { icon(ICON_ID) }
                )
            }
    }
}
