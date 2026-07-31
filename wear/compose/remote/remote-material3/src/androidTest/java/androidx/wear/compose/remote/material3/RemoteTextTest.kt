/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontFamily
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.DownloadableTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.FallbackCreateTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.R
import androidx.compose.remote.player.compose.test.utils.RemappingTypefaceResolver
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.compose.test.utils.createMockContextWithFont
import androidx.compose.remote.player.core.platform.FontInstance
import androidx.compose.remote.player.core.platform.TypefaceResolver
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontVariation.Setting
import androidx.compose.ui.text.font.FontVariation.Settings
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import java.text.DecimalFormat
import kotlin.test.Test
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteTextTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun remoteText_customRemoteFontFamily() {
        val width = 400
        val height = 120
        val mockContext =
            createMockContextWithFont(
                baseContext = context,
                fontInputStream = context.resources.openRawResource(R.font.inconsolata_regular),
            )
        val resolver =
            DownloadableTypefaceResolver(
                context = mockContext,
                next = FallbackCreateTypefaceResolver(),
                isBlocking = true,
            )

        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width,
                    height,
                    context.resources.displayMetrics.densityDpi,
                    context.resources.configuration.fontScale,
                ),
            playComposableWrapper = ComposableWrappers.blackBackground,
            typefaceResolver = resolver,
        ) {
            RemoteColumn(modifier = RemoteModifier.size(width.rdp, height.rdp)) {
                RemoteText(
                    text = "Hello Default!".rs,
                    color = Color.White.rc,
                    fontFamily = RemoteFontFamily.Default,
                    fontSize = 14.rsp,
                )
                RemoteText(
                    text = "Hello Inconsolata!".rs,
                    color = Color.White.rc,
                    fontFamily = RemoteFontFamily.Named("google:inconsolata"),
                    fontSize = 14.rsp,
                )
            }
        }
    }

    @Test
    fun text_withDefaultColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withDefaultColor".rs
            RemoteText(text, fontSize = 32.rsp)
        }
    }

    @Test
    fun text_withColorAndTextAlign_rtl() {
        remoteComposeTestRule.runScreenshotTestCustomProfile(
            layoutDirection = LayoutDirection.Rtl
        ) {
            val left = "LEFT".rs
            val center = "CENTER".rs
            val right = "RIGHT".rs
            val start = "START".rs
            val end = "END".rs
            val color = rememberNamedRemoteColor("TestColor5_rtl", Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = left,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Left,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = right,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Right,
                )
                RemoteText(
                    text = start,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Start,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = end,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.End,
                )
            }
        }
    }

    @Test
    fun text_withStyle() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "textWithStyle".rs
            RemoteText(
                text,
                style =
                    LocalRemoteTextStyle.current.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 32.rsp,
                    ),
            )
        }
    }

    @Test
    fun text_withColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withColor".rs
            val color = rememberNamedRemoteColor("TestColor2", Color.Green)
            RemoteText(text, color = color, fontSize = 32.rsp)
        }
    }

    @Test
    fun text_withOverridingColor() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withOverridingColor".rs
            val color = rememberNamedRemoteColor("TestColor3", Color.Green)

            RemoteText(
                text,
                color = color, // text color should be green
                fontSize = 32.rsp,
                style =
                    LocalRemoteTextStyle.current.copy(
                        color = Color.Red.rc
                    ), // style color should be ignored
            )
        }
    }

    @Test
    fun text_withParamAndStyle_paramIsPreserved() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = "text_withParamAndStyle".rs
            val color = rememberNamedRemoteColor("TestColor4", Color.Green)

            RemoteText(
                text,
                color = color,
                fontStyle = FontStyle.Italic,
                style = LocalRemoteTextStyle.current.copy(fontSize = 32.rsp),
            )
        }
    }

    @Test
    fun text_withColorAndTextAlign() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val left = "LEFT".rs
            val center = "CENTER".rs
            val right = "RIGHT".rs
            val color = rememberNamedRemoteColor("TestColor5", Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = left,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Left,
                )
                RemoteText(
                    text = center,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Center,
                )
                RemoteText(
                    text = right,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    color = color,
                    textAlign = TextAlign.Right,
                )
            }
        }
    }

    @Test
    fun text_withWeight() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.weight(100))
                VariantText(FontVariation.weight(500))
                VariantText(FontVariation.weight(900))
            }
        }
    }

    @Test
    fun text_withWidth() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.width(10f))
                VariantText(FontVariation.width(50f))
                VariantText(FontVariation.width(90f))
            }
        }
    }

    @Test
    fun text_withGrade() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.grade(0))
                VariantText(FontVariation.grade(100))
                VariantText(FontVariation.grade(200))
            }
        }
    }

    @Test
    fun text_withTnum() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val robotoFont = RemoteFontFamily.Named("google:Roboto Flex")
            val googleSansFont = RemoteFontFamily.Named("google:Google Sans Flex")
            val sampleText = "11111 vs 88888"
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = RemoteString("Roboto Default: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = robotoFont,
                )
                RemoteText(
                    text = RemoteString("Roboto pnum: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = robotoFont,
                    fontVariationSettings = Settings(Setting("pnum", 1f)),
                )
                RemoteText(
                    text = RemoteString("Roboto tnum: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = robotoFont,
                    fontVariationSettings = Settings(Setting("tnum", 1f)),
                )
                RemoteText(
                    text = RemoteString("GSans Default: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = googleSansFont,
                )
                RemoteText(
                    text = RemoteString("GSans pnum: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = googleSansFont,
                    fontVariationSettings = Settings(Setting("pnum", 1f)),
                )
                RemoteText(
                    text = RemoteString("GSans tnum: $sampleText"),
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 14.rsp,
                    fontFamily = googleSansFont,
                    fontVariationSettings = Settings(Setting("tnum", 1f)),
                )
            }
        }
    }

    @Test
    fun text_withSlant() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.slant(-15f))
                VariantText(FontVariation.slant(-7.5f))
                VariantText(FontVariation.slant(0f))
                VariantText(FontVariation.slant(15f))
            }
        }
    }

    @Test
    fun text_withRoundness() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(Setting("RNDS", 0f))
                VariantText(Setting("RNDS", 50f))
                VariantText(Setting("RNDS", 100f))
            }
        }
    }

    @Test
    fun text_withRobotoFlex_minMax() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val robotoFlex = RemoteFontFamily.Named("google:Roboto Flex")
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                VariantText(FontVariation.weight(100), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.weight(1000), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.width(25f), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.width(151f), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.grade(-200), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.grade(150), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.slant(0f), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(FontVariation.slant(-10f), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(Setting("opsz", 8f), fontSize = 14.rsp, fontFamily = robotoFlex)
                VariantText(Setting("opsz", 144f), fontSize = 14.rsp, fontFamily = robotoFlex)
            }
        }
    }

    @Test
    fun text_withLobsterTwo() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val fonts =
                listOf(
                    "google:Lobster Two",
                    "google:Pacifico",
                    "google:Caveat",
                    "google:Dancing Script",
                    "google:Cinzel",
                    "google:Oswald",
                    "google:Comfortaa",
                    "google:Press Start 2P",
                    "google:Bebas Neue",
                    "google:Playfair Display",
                )
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                for (font in fonts) {
                    RemoteText(
                        text = RemoteString(font.removePrefix("google:")),
                        modifier = RemoteModifier.fillMaxWidth(),
                        fontSize = 16.rsp,
                        fontFamily = RemoteFontFamily.Named(font),
                    )
                }
            }
        }
    }

    @Test
    fun text_withDecoration() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = "None".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                )
                RemoteText(
                    text = "Underline".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style =
                        LocalRemoteTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                )
                RemoteText(
                    text = "LineThrough".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style =
                        LocalRemoteTextStyle.current.copy(
                            textDecoration = TextDecoration.LineThrough
                        ),
                )
            }
        }
    }

    @Test
    fun text_withSpacing() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            RemoteColumn(RemoteModifier.fillMaxSize()) {
                RemoteText(
                    text = "Standard\nParagraph".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                )
                RemoteText(
                    text = "Double Line Height\nParagraph\nAnd one more".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style = LocalRemoteTextStyle.current.copy(lineHeight = 64.rsp),
                )
                RemoteText(
                    text = "Letter Spacing\nParagraph".rs,
                    modifier = RemoteModifier.fillMaxWidth(),
                    fontSize = 32.rsp,
                    style = LocalRemoteTextStyle.current.copy(letterSpacing = 64.rsp),
                )
            }
        }
    }

    @Composable
    private fun VariantText(
        setting: Setting,
        fontSize: RemoteTextUnit = 32.rsp,
        fontFamily: RemoteFontFamily = RemoteFontFamily.Named("google:Roboto Flex"),
    ) {
        RemoteText(
            text =
                RemoteString(setting.axisName) +
                    RemoteString(" = ") +
                    setting.toVariationValue(null).rf.toRemoteString(DecimalFormat("0")),
            modifier = RemoteModifier.fillMaxWidth(),
            fontSize = fontSize,
            fontFamily = fontFamily,
            fontVariationSettings = Settings(setting),
        )
    }

    @Test
    fun longText_overflow() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text =
                "a piece of writing in which the expression of feelings and ideas is given intensity by particular attention to diction (sometimes involving rhyme), rhythm, and imagery."
                    .rs
            val color = RemoteColor(Color.Green)

            RemoteColumn(RemoteModifier.fillMaxSize()) {
                // Default
                RemoteText(text = text, fontSize = 18.rsp, color = color)
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Clip,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                )
                RemoteText(
                    text = text,
                    fontSize = 18.rsp,
                    color = color,
                    overflow = TextOverflow.StartEllipsis,
                    maxLines = 1,
                )
            }
        }
    }

    @Test
    fun text_withLineBreakAndHyphens() {
        remoteComposeTestRule.runScreenshotTestCustomProfile {
            val text = SAMPLE_LONG_TEXT.rs
            val dividerColor = Color.Gray.rc
            val textColor = Color.White.rc

            RemoteColumn(modifier = RemoteModifier.fillMaxWidth(0.7f.rf).fillMaxHeight()) {
                // Row 1: Header Row
                RemoteRow(
                    modifier =
                        RemoteModifier.fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(bottom = 2.rdp)
                ) {
                    RemoteText(
                        "".rs,
                        color = textColor,
                        fontSize = 8.rsp,
                        modifier = RemoteModifier.weight(0.7f),
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
                    )
                    RemoteText(
                        "LineBreak.Simple".rs,
                        color = textColor,
                        fontSize = 8.rsp,
                        textAlign = TextAlign.Center,
                        modifier = RemoteModifier.weight(1f),
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
                    )
                    RemoteText(
                        "LineBreak.Heading".rs,
                        color = textColor,
                        fontSize = 8.rsp,
                        textAlign = TextAlign.Center,
                        modifier = RemoteModifier.weight(1f),
                    )
                    RemoteBox(
                        modifier =
                            RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
                    )
                    RemoteText(
                        "LineBreak.Paragraph".rs,
                        color = textColor,
                        fontSize = 8.rsp,
                        textAlign = TextAlign.Center,
                        modifier = RemoteModifier.weight(1f),
                    )
                }

                // Horizontal Divider 1
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(1.rdp).background(dividerColor)
                )

                // Row 2: Hyphens.None
                LineBreakRow(
                    label = "Hyphens.None",
                    hyphens = Hyphens.None,
                    text = text,
                    textColor = textColor,
                    dividerColor = dividerColor,
                )

                // Horizontal Divider 2
                RemoteBox(
                    modifier = RemoteModifier.fillMaxWidth().height(1.rdp).background(dividerColor)
                )

                // Row 3: Hyphens.Auto
                LineBreakRow(
                    label = "Hyphens.Auto",
                    hyphens = Hyphens.Auto,
                    text = text,
                    textColor = textColor,
                    dividerColor = dividerColor,
                    modifier = RemoteModifier.padding(top = 2.rdp),
                )
            }
        }
    }

    @Composable
    private fun LineBreakRow(
        label: String,
        hyphens: Hyphens,
        text: RemoteString,
        textColor: RemoteColor,
        dividerColor: RemoteColor,
        modifier: RemoteModifier = RemoteModifier,
    ) {
        RemoteRow(
            modifier = RemoteModifier.fillMaxWidth().height(IntrinsicSize.Min).then(modifier)
        ) {
            RemoteColumn(modifier = RemoteModifier.weight(0.7f).padding(top = 6.rdp)) {
                RemoteText(label.rs, color = textColor, fontSize = 8.rsp)
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
            )
            RemoteColumn(modifier = RemoteModifier.weight(1f).padding(4.rdp)) {
                RemoteText(
                    text = text,
                    color = textColor,
                    fontSize = 8.rsp,
                    style = RemoteTextStyle(lineBreak = LineBreak.Simple, hyphens = hyphens),
                )
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
            )
            RemoteColumn(modifier = RemoteModifier.weight(1f).padding(4.rdp)) {
                RemoteText(
                    text = text,
                    color = textColor,
                    fontSize = 8.rsp,
                    style = RemoteTextStyle(lineBreak = LineBreak.Heading, hyphens = hyphens),
                )
            }
            RemoteBox(
                modifier = RemoteModifier.fillMaxHeight().width(1.rdp).background(dividerColor)
            )
            RemoteColumn(modifier = RemoteModifier.weight(1f).padding(4.rdp)) {
                RemoteText(
                    text = text,
                    color = textColor,
                    fontSize = 8.rsp,
                    style = RemoteTextStyle(lineBreak = LineBreak.Paragraph, hyphens = hyphens),
                )
            }
        }
    }

    private fun RemoteScreenshotTestRule.runScreenshotTestCustomProfile(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        composable: @Composable @RemoteComposable () -> Unit,
    ) {
        val mockContext =
            createMockContextWithFont(
                baseContext = context,
                fontInputStream = context.resources.openRawResource(R.font.inconsolata_regular),
            )
        val current = FallbackCreateTypefaceResolver()
        val remappingResolver =
            RemappingTypefaceResolver(current).apply {
                remapName("RobotoFlex", "google:Roboto Flex")
                remapType(0, "google:Roboto Flex")
                remapName("Fraunces", "google:Fraunces")
            }
        val resolver =
            DownloadableTypefaceResolver(
                context = context,
                next = remappingResolver,
                isBlocking = true,
            )
        // Trigger a pre-fetch of fonts so they are loaded and cached in
        // DownloadableTypefaceResolver before screenshot capture in CI.
        resolver.prefetchFonts(
            listOf(
                "google:Fraunces",
                "google:Roboto Flex",
                "google:Google Sans Flex",
                "google:Lobster Two",
                "google:Pacifico",
                "google:Caveat",
                "google:Dancing Script",
                "google:Cinzel",
                "google:Oswald",
                "google:Comfortaa",
                "google:Press Start 2P",
                "google:Bebas Neue",
                "google:Playfair Display",
            )
        )

        var resolveCalled = false
        val trackingResolver =
            object : TypefaceResolver {
                override fun resolve(
                    fontType: Int,
                    weight: Int,
                    italic: Boolean,
                    fallbackTypeface: Typeface?,
                    fallbackWeight: Int,
                    fallbackItalic: Boolean,
                ): FontInstance {
                    resolveCalled = true
                    return resolver.resolve(
                        fontType,
                        weight,
                        italic,
                        fallbackTypeface,
                        fallbackWeight,
                        fallbackItalic,
                    )
                }

                override fun resolve(
                    fontName: String,
                    weight: Int,
                    italic: Boolean,
                    fallbackTypeface: Typeface?,
                    fallbackWeight: Int,
                    fallbackItalic: Boolean,
                ): FontInstance {
                    resolveCalled = true
                    return resolver.resolve(
                        fontName,
                        weight,
                        italic,
                        fallbackTypeface,
                        fallbackWeight,
                        fallbackItalic,
                    )
                }
            }

        this.runScreenshotTest(
            profile = TestProfiles.androidXWithCoreText,
            creationComposableWrapper = ComposableWrappers.layoutDirection(layoutDirection),
            playComposableWrapper = ComposableWrappers.blackBackground,
            typefaceResolver = trackingResolver,
            composable = composable,
        )
        assertTrue("Expected TypefaceResolver.resolve to be called during test", resolveCalled)
    }

    private companion object {
        const val SAMPLE_LONG_TEXT =
            "Remote Compose is a technology that enables apps to define their UI and business logic in a format that can be rendered and executed remotely by a host application, called a player. This player handles the display and processing of the application content."
    }
}
