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

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.HORIZONTAL_ALIGN_END
import androidx.wear.protolayout.expression.VersionBuilders.VersionInfo
import androidx.wear.protolayout.material3.AppCardStyle.Companion.largeAppCardStyle
import androidx.wear.protolayout.material3.AvatarButtonStyle.Companion.largeAvatarButtonStyle
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledVariantButtonColors
import androidx.wear.protolayout.material3.ButtonGroupDefaults.DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledTonalProgressIndicatorColors
import androidx.wear.protolayout.material3.CircularProgressIndicatorDefaults.filledVariantProgressIndicatorColors
import androidx.wear.protolayout.material3.DataCardStyle.Companion.smallCompactDataCardStyle
import androidx.wear.protolayout.material3.GraphicDataCardDefaults.constructGraphic
import androidx.wear.protolayout.material3.IconButtonStyle.Companion.largeIconButtonStyle
import androidx.wear.protolayout.material3.MaterialGoldenTest.Companion.pxToDp
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MAX_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.MIN_PRIMARY_LAYOUT_MARGIN
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.extraLargeTextButtonStyle
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.largeTextButtonStyle
import androidx.wear.protolayout.material3.TextButtonStyle.Companion.smallTextButtonStyle
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.clip
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.layoutString
import com.google.common.collect.ImmutableMap

private const val CONTENT_DESCRIPTION_PLACEHOLDER = "Description"

object TestCasesGenerator {
    private const val ICON_ID: String = "icon"
    private const val IMAGE_ID: String = "avatar_image"
    private const val NORMAL_SCALE_SUFFIX: String = ""

    /**
     * This function will append goldenSuffix on the name of the golden images that should be
     * different for different user font sizes. Note that some of the golden will have the same name
     * as it should point on the same size independent image. These test cases are meant to be
     * tested in RTL and LTR modes.
     */
    fun generateTestCases(
        goldenSuffix: String = NORMAL_SCALE_SUFFIX
    ): ImmutableMap<String, LayoutElementBuilders.Layout> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val displayMetrics = context.resources.displayMetrics
        val scale = displayMetrics.density

        val deviceParameters =
            DeviceParameters.Builder()
                .setScreenWidthDp(pxToDp(RunnerUtils.SCREEN_SIZE_SMALL, scale))
                .setScreenHeightDp(pxToDp(RunnerUtils.SCREEN_SIZE_SMALL, scale))
                .setScreenDensity(displayMetrics.density)
                .setFontScale(1f)
                .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                // testing with the latest renderer version
                .setRendererSchemaVersion(VersionInfo.Builder().setMajor(99).setMinor(999).build())
                .build()
        val clickable = clickable(id = "action_id")
        val testCases: HashMap<String, LayoutElementBuilders.LayoutElement> = HashMap()

        testCases["primarylayout_edgebuttonfilled_buttongroup_iconoverride_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                // renderer version 1.302 has no asymmetrical corner support, so edgebutton will use
                // its fallback style
                deviceParameters.copy(VersionInfo.Builder().setMajor(1).setMinor(302).build()),
                allowDynamicTheme = false
            ) {
                primaryLayoutWithOverrideIcon(
                    mainSlot = {
                        text(
                            text = "Overflow main text and fallback edge button".layoutString,
                            color = colorScheme.secondary,
                            maxLines = 3
                        )
                    },
                    bottomSlot = {
                        textEdgeButton(
                            onClick = clickable,
                            labelContent = { text("Action".layoutString) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledButtonColors()
                        )
                    },
                    titleSlot = { text("Title".layoutString) },
                    overrideIcon = true
                )
            }
        testCases["primarylayout_graphcard_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        graphicDataCard(
                            onClick = clickable,
                            modifier =
                                LayoutModifier.contentDescription("Graphic Data Card with CPI"),
                            height = expand(),
                            horizontalAlignment = LayoutElementBuilders.HORIZONTAL_ALIGN_START,
                            title = {
                                text(
                                    "1234".layoutString,
                                )
                            },
                            content = {
                                text(
                                    "steps".layoutString,
                                )
                            },
                            graphic = {
                                constructGraphic(
                                    mainContent = {
                                        circularProgressIndicator(staticProgress = 0.5F)
                                    },
                                    iconContent = { icon(ICON_ID) }
                                )
                            }
                        )
                    },
                    margins = MIN_PRIMARY_LAYOUT_MARGIN
                )
            }
        testCases["primarylayout_graphcard_filledVariant_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        graphicDataCard(
                            onClick = clickable,
                            modifier =
                                LayoutModifier.contentDescription(
                                    "Graphic Data Card with segmented CPI"
                                ),
                            height = expand(),
                            horizontalAlignment = LayoutElementBuilders.HORIZONTAL_ALIGN_START,
                            title = {
                                text(
                                    "1234".layoutString,
                                )
                            },
                            content = {
                                text(
                                    "steps".layoutString,
                                )
                            },
                            graphic = {
                                constructGraphic(
                                    mainContent = {
                                        segmentedCircularProgressIndicator(
                                            segmentCount = 5,
                                            staticProgress = 0.5F,
                                        )
                                    },
                                    iconContent = { icon(ICON_ID) }
                                )
                            },
                            colors = filledVariantCardColors(),
                        )
                    },
                    margins = MIN_PRIMARY_LAYOUT_MARGIN
                )
            }
        testCases["primarylayout_graphcard_avatarbutton_fallback_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters.copy(VersionInfo.Builder().setMajor(1).setMinor(100).build()),
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        Column.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(
                                graphicDataCard(
                                    onClick = clickable,
                                    modifier =
                                        LayoutModifier.contentDescription("Graphic Data Card"),
                                    height = expand(),
                                    horizontalAlignment =
                                        LayoutElementBuilders.HORIZONTAL_ALIGN_START,
                                    title = {
                                        text(
                                            "1234".layoutString,
                                        )
                                    },
                                    graphic = { circularProgressIndicator(staticProgress = 0.5F) }
                                )
                            )
                            .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                            .addContent(
                                avatarButton(
                                    onClick = clickable,
                                    labelContent = { text("Primary label".layoutString) },
                                    avatarContent = { avatarImage(IMAGE_ID) },
                                    height = expand()
                                )
                            )
                            .build()
                    },
                    margins = MIN_PRIMARY_LAYOUT_MARGIN
                )
            }
        testCases["primarylayout_edgebuttonfilledvariant_iconoverride_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayoutWithOverrideIcon(
                    mainSlot = {
                        buttonGroup {
                            buttonGroupItem {
                                iconDataCard(
                                    onClick = clickable,
                                    modifier = LayoutModifier.contentDescription("Data Card"),
                                    title = { text("MM".layoutString) },
                                    content = { text("Min".layoutString) },
                                    secondaryIcon = { icon(ICON_ID) },
                                    shape = shapes.none,
                                    width = expand(),
                                    height = expand()
                                )
                            }
                            buttonGroupItem {
                                textDataCard(
                                    onClick = clickable,
                                    modifier = LayoutModifier.contentDescription("Data Card"),
                                    title = { text("MM".layoutString) },
                                    content = { text("Min".layoutString) },
                                    secondaryText = { text("Label".layoutString) },
                                    colors =
                                        CardColors(
                                            backgroundColor = colorScheme.onSecondary,
                                            titleColor = colorScheme.secondary,
                                            contentColor = colorScheme.secondaryDim
                                        ),
                                    shape = shapes.full
                                )
                            }
                        }
                    },
                    margins = MAX_PRIMARY_LAYOUT_MARGIN,
                    bottomSlot = {
                        textEdgeButton(
                            onClick = clickable,
                            labelContent = { text("Action that overflows".layoutString) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledVariantButtonColors()
                        )
                    },
                    overrideIcon = true
                )
            }
        testCases["primarylayout_edgebuttonfilledtonal_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        card(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            width = expand(),
                            height = expand(),
                            backgroundContent = {
                                backgroundImage(protoLayoutResourceId = IMAGE_ID)
                            }
                        ) {
                            text(
                                "Card with image background".layoutString,
                                color = colorScheme.onBackground
                            )
                        }
                    },
                    bottomSlot = {
                        iconEdgeButton(
                            onClick = clickable,
                            iconContent = { icon(ICON_ID) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledTonalButtonColors()
                        )
                    },
                    titleSlot = {
                        text("Title that overflows".layoutString, color = colorScheme.error)
                    }
                )
            }
        testCases["primarylayout_titlecard_bottomslot_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        titleCard(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            height = expand(),
                            title = {
                                this.text(
                                    "Title Card text that will overflow after 2 max lines of text"
                                        .layoutString
                                )
                            },
                            time = { text("Now".layoutString) },
                            content = { text("Default title card".layoutString) },
                            colors = filledVariantCardColors()
                        )
                    },
                    bottomSlot = { text("Bottom Slot that overflows".layoutString) },
                    titleSlot = { text("TitleCard".layoutString, color = colorScheme.secondaryDim) }
                )
            }
        testCases["primarylayout_bottomslot_withlabel_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        iconDataCard(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Data card labels only"),
                            title = { text("000".layoutString) },
                            content = { text("PM".layoutString) },
                            style = smallCompactDataCardStyle(),
                            colors =
                                CardColors(
                                    backgroundColor = colorScheme.errorContainer,
                                    titleColor = colorScheme.onErrorContainer,
                                    contentColor = colorScheme.onError
                                ),
                            height = expand()
                        )
                    },
                    bottomSlot = { text("Bottom Slot".layoutString) },
                    labelForBottomSlot = { text("Label in bottom slot overflows".layoutString) },
                    titleSlot = {
                        text("Title".layoutString, color = colorScheme.secondaryContainer)
                    }
                )
            }
        testCases["primarylayout_nobottomslot_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        appCard(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            title = {
                                this.text(
                                    "Default App Card text that will overflow after 1 line of text"
                                        .layoutString
                                )
                            },
                            time = { text("Now".layoutString) },
                            content = { text("Default app card".layoutString) },
                            label = { text("Label in card".layoutString) },
                            avatar = { avatarImage(IMAGE_ID) }
                        )
                    },
                    labelForBottomSlot = { text("Ignored Label in bottom slot".layoutString) },
                    titleSlot = {
                        text("Title".layoutString, color = colorScheme.secondaryContainer)
                    }
                )
            }
        testCases["primarylayout_largeappcard_nobottomslot_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        appCard(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            title = {
                                this.text(
                                    "Large App Card text that will overflow after 1 line of text"
                                        .layoutString
                                )
                            },
                            time = { text("Now".layoutString) },
                            content = { text("Large app card".layoutString) },
                            label = { text("Label in card".layoutString) },
                            avatar = { avatarImage(IMAGE_ID) },
                            colors = filledVariantCardColors(),
                            style = largeAppCardStyle()
                        )
                    },
                    labelForBottomSlot = { text("Ignored Label in bottom slot".layoutString) },
                    titleSlot = {
                        text("Title".layoutString, color = colorScheme.secondaryContainer)
                    }
                )
            }
        testCases["primarylayout_nobottomslotnotitle_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        Column.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(
                                button(
                                    onClick = clickable,
                                    labelContent = { text("Primary label".layoutString) },
                                    secondaryLabelContent = {
                                        text("Secondary label".layoutString)
                                    },
                                    iconContent = { icon(ICON_ID) },
                                    width = expand()
                                )
                            )
                            .addContent(
                                buttonGroup {
                                    buttonGroupItem {
                                        compactButton(
                                            onClick = clickable,
                                            labelContent = { text("Label".layoutString) },
                                        )
                                    }
                                    buttonGroupItem {
                                        imageButton(
                                            onClick = clickable,
                                            backgroundContent = { backgroundImage(IMAGE_ID) },
                                            modifier = LayoutModifier.clip(shapes.extraSmall)
                                        )
                                    }
                                }
                            )
                            .build()
                    },
                )
            }
        testCases["primarylayout_nobottomslotnotitle_avatarbuttons_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        Column.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(
                                avatarButton(
                                    onClick = clickable,
                                    labelContent = { text("Primary label".layoutString) },
                                    secondaryLabelContent = {
                                        text("Secondary label".layoutString)
                                    },
                                    avatarContent = { avatarImage(IMAGE_ID) },
                                )
                            )
                            .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                            .addContent(
                                avatarButton(
                                    onClick = clickable,
                                    labelContent = {
                                        text("Primary label overflowing".layoutString)
                                    },
                                    secondaryLabelContent = {
                                        text("Secondary label overflowing".layoutString)
                                    },
                                    avatarContent = { avatarImage(IMAGE_ID) },
                                    height = expand(),
                                    style = largeAvatarButtonStyle(),
                                    horizontalAlignment = HORIZONTAL_ALIGN_END
                                )
                            )
                            .build()
                    },
                )
            }
        testCases["primarylayout_oneslotbuttons_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        Column.Builder()
                            .setWidth(expand())
                            .setHeight(expand())
                            .addContent(
                                buttonGroup {
                                    buttonGroupItem {
                                        iconButton(
                                            onClick = clickable,
                                            iconContent = { icon(ICON_ID) }
                                        )
                                    }
                                    buttonGroupItem {
                                        iconButton(
                                            onClick = clickable,
                                            iconContent = { icon(ICON_ID) },
                                            style = largeIconButtonStyle(),
                                            colors = filledTonalButtonColors(),
                                            width = expand(),
                                            height = expand(),
                                            shape = shapes.large
                                        )
                                    }
                                    buttonGroupItem {
                                        textButton(
                                            onClick = clickable,
                                            labelContent = { text("000".layoutString) },
                                            style = smallTextButtonStyle(),
                                        )
                                    }
                                }
                            )
                            .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
                            .addContent(
                                buttonGroup {
                                    buttonGroupItem {
                                        textButton(
                                            onClick = clickable,
                                            labelContent = { text("1".layoutString) },
                                            width = expand(),
                                            shape = shapes.small
                                        )
                                    }
                                    buttonGroupItem {
                                        textButton(
                                            onClick = clickable,
                                            labelContent = { text("2".layoutString) },
                                            style = largeTextButtonStyle(),
                                            colors = filledTonalButtonColors(),
                                            height = expand()
                                        )
                                    }
                                    buttonGroupItem {
                                        textButton(
                                            onClick = clickable,
                                            labelContent = { text("3".layoutString) },
                                            style = extraLargeTextButtonStyle(),
                                            colors = filledVariantButtonColors(),
                                            width = expand(),
                                            height = expand()
                                        )
                                    }
                                }
                            )
                            .build()
                    },
                )
            }
        testCases["primarylayout_circularprogressindicators_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = { progressIndicatorGroup() },
                    margins = MIN_PRIMARY_LAYOUT_MARGIN
                )
            }

        testCases["primarylayout_circularprogressindicators_fallback__golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                // renderer with version 1.302 has no DashedArcLine or asymmetrical corners support
                deviceConfiguration =
                    deviceParameters.copy(VersionInfo.Builder().setMajor(1).setMinor(302).build()),
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = { progressIndicatorGroup() },
                    margins = MIN_PRIMARY_LAYOUT_MARGIN,
                    bottomSlot = {
                        iconEdgeButton(
                            onClick = clickable,
                            iconContent = { icon(ICON_ID) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledTonalButtonColors()
                        )
                    }
                )
            }

        return collectTestCases(testCases)
    }

    private fun collectTestCases(
        testCases: Map<String, LayoutElementBuilders.LayoutElement>
    ): ImmutableMap<String, LayoutElementBuilders.Layout> {
        return testCases.entries
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    { obj: Map.Entry<String, LayoutElementBuilders.LayoutElement> -> obj.key },
                    { entry: Map.Entry<String, LayoutElementBuilders.LayoutElement> ->
                        LayoutElementBuilders.Layout.fromLayoutElement(entry.value)
                    }
                )
            )
    }

    private fun MaterialScope.progressIndicatorGroup(): Column =
        Column.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                buttonGroup(height = dp(52F)) {
                    buttonGroupItem {
                        circularProgressIndicator(colors = filledTonalProgressIndicatorColors())
                    }
                    buttonGroupItem {
                        circularProgressIndicator(
                            staticProgress = 0.75F,
                            colors = filledVariantProgressIndicatorColors()
                        )
                    }
                    buttonGroupItem {
                        circularProgressIndicator(
                            staticProgress = 1.5F,
                            startAngleDegrees = 200F,
                            endAngleDegrees = 520F,
                            colors = filledVariantProgressIndicatorColors()
                        )
                    }
                }
            )
            .addContent(DEFAULT_SPACER_BETWEEN_BUTTON_GROUPS)
            .addContent(
                buttonGroup(height = dp(52F)) {
                    buttonGroupItem {
                        segmentedCircularProgressIndicator(
                            segmentCount = 1,
                            colors = filledTonalProgressIndicatorColors()
                        )
                    }
                    buttonGroupItem {
                        segmentedCircularProgressIndicator(
                            segmentCount = 5,
                            staticProgress = 0.75F,
                            colors = filledVariantProgressIndicatorColors()
                        )
                    }
                    buttonGroupItem {
                        segmentedCircularProgressIndicator(
                            segmentCount = 5,
                            staticProgress = 1.5F,
                            startAngleDegrees = 200F,
                            endAngleDegrees = 520F,
                            colors = filledVariantProgressIndicatorColors()
                        )
                    }
                }
            )
            .build()

    /**
     * Make a copy of a [DeviceParameters], and update it with the provided renderer version for
     * testing fallback features on older renderer.
     */
    private fun DeviceParameters.copy(rendererVersion: VersionInfo): DeviceParameters =
        DeviceParameters.Builder()
            .setScreenWidthDp(screenWidthDp)
            .setScreenHeightDp(screenHeightDp)
            .setScreenDensity(screenDensity)
            .setFontScale(fontScale)
            .setScreenShape(screenShape)
            .setRendererSchemaVersion(rendererVersion)
            .build()
}
