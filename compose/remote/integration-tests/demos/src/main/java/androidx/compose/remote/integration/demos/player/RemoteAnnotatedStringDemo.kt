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

@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package androidx.compose.remote.integration.demos.player

import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

@Suppress("RestrictedApiAndroidX")
private val experimentalProfile =
    Profile(
        RcPlatformProfiles.ANDROIDX.apiLevel,
        RcPlatformProfiles.ANDROIDX.operationsProfiles or RcProfiles.PROFILE_EXPERIMENTAL,
        RcPlatformProfiles.ANDROIDX.platform,
        RcPlatformProfiles.ANDROIDX.profileFactory,
    )

/** Demonstrates URL and Clickable [LinkAnnotation]s. */
@Composable
@RemoteComposable
fun LinksSample() {
    val annotatedText = buildRemoteAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            append("1. Link Annotations:\n")
        }
        append("Please review our ")
        withLink(LinkAnnotation.Url("https://example.com/terms")) { append("Terms of Service") }
        append(" and ")
        withLink(LinkAnnotation.Url("https://example.com/privacy")) { append("Privacy Policy") }
        append(", or click ")
        withLink(LinkAnnotation.Clickable("contact_support", linkInteractionListener = null)) {
            append("Contact Support")
        }
        append(" for help.\n")
    }
    RemoteText(text = annotatedText)
}

/** Demonstrates character-level [SpanStyle]s (colors, highlights, styles, sizes). */
@Composable
@RemoteComposable
fun SpanStylesSample() {
    val styledText = buildRemoteAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            append("2. Span Styles:\n")
        }
        withStyle(SpanStyle(color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)) {
            append("Bold Blue ")
        }
        withStyle(SpanStyle(color = Color(0xFFE53935), fontStyle = FontStyle.Italic)) {
            append("Italic Red ")
        }
        withStyle(
            SpanStyle(
                background = Color(0xFFFFF59D),
                color = Color(0xFF333333),
                textDecoration = TextDecoration.Underline,
            )
        ) {
            append("Yellow Highlight Underline ")
        }
        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = Color.Gray)) {
            append("Strikethrough ")
        }
        withStyle(SpanStyle(fontSize = 20.sp, color = Color(0xFF8E24AA))) { append("Big Purple\n") }
    }
    RemoteText(text = styledText)
}

/** Demonstrates [androidx.compose.ui.text.Bullet] lists with indentation. */
@Composable
@RemoteComposable
fun BulletListSample() {
    val bulletText = buildRemoteAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            append("3. Bullet List:\n")
        }
        withBulletList {
            withBulletListItem { append("First remote bullet point\n") }
            withBulletListItem {
                append("Second point with ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF43A047))) {
                    append("bold green styling\n")
                }
            }
            withBulletListItem { append("Third item in the list\n") }
        }
    }
    RemoteText(text = bulletText)
}

/** Demonstrates [ParagraphStyle] alignments (Start, Center, End). */
@Composable
@RemoteComposable
fun ParagraphAlignmentSample() {
    val paragraphText = buildRemoteAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            append("4. Paragraph Alignment:\n")
        }
        withStyle(ParagraphStyle(textAlign = TextAlign.Start)) {
            append("Left / Start aligned paragraph.\n")
        }
        withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
            withStyle(SpanStyle(color = Color(0xFF00897B), fontStyle = FontStyle.Italic)) {
                append("Centered italic paragraph.\n")
            }
        }
        withStyle(ParagraphStyle(textAlign = TextAlign.End)) {
            append("Right / End aligned paragraph.\n")
        }
    }
    RemoteText(text = paragraphText, modifier = RemoteModifier.fillMaxWidth())
}

/**
 * Demonstrates transformations (upper/lower case, capitalize, decapitalize) and `+` concatenation.
 */
@Composable
@RemoteComposable
fun TransformationsAndConcatSample() {
    val header = buildRemoteAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
            append("5. Transformations & Concat (+):\n")
        }
    }

    val upperSample = buildRemoteAnnotatedString {
        append("• toUpperCase: ")
        append(
            buildRemoteAnnotatedString {
                    withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                        append("lowercase to uppercase\n")
                    }
                }
                .toUpperCase()
        )
    }

    val lowerSample = buildRemoteAnnotatedString {
        append("• toLowerCase: ")
        append(
            buildRemoteAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF1E88E5))) {
                        append("UPPERCASE TO LOWERCASE\n")
                    }
                }
                .toLowerCase()
        )
    }

    val capitalizedSample = buildRemoteAnnotatedString {
        append("• capitalize: ")
        append(
            buildRemoteAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF43A047), fontStyle = FontStyle.Italic)) {
                        append("first letter capitalized\n")
                    }
                }
                .capitalize()
        )
    }

    val decapitalizedSample = buildRemoteAnnotatedString {
        append("• decapitalize: ")
        append(
            buildRemoteAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFFD81B60))) {
                        append("FIRST LETTER DECAPITALIZED\n")
                    }
                }
                .decapitalize()
        )
    }

    // Demonstrates Unicode length expansion ('ß' -> 'SS') and ensures subsequent style spans shift
    // correctly
    val unicodeExpandSample = buildRemoteAnnotatedString {
        append("• Unicode expansion: ")
        append(
            buildRemoteAnnotatedString {
                    append("stra")
                    withStyle(SpanStyle(color = Color(0xFFE53935), fontWeight = FontWeight.Bold)) {
                        append("ß") // 'ß' (1 char) expands to "SS" (2 chars)
                    }
                    append("e: ")
                    withStyle(
                        SpanStyle(
                            color = Color(0xFF1E88E5),
                            textDecoration = TextDecoration.Underline,
                        )
                    ) {
                        append("subsequent shifted style\n")
                    }
                }
                .toUpperCase()
        )
    }

    val combined =
        header +
            upperSample +
            lowerSample +
            capitalizedSample +
            decapitalizedSample +
            unicodeExpandSample

    RemoteText(text = combined)
}

/** Comprehensive showcase of [RemoteAnnotatedString] features. */
@Suppress("RestrictedApiAndroidX")
@Composable
fun RemoteAnnotatedStringDemo() {
    val customSupport = remember {
        AndroidCustomContextImpl().apply {
            registerDelegate("SupportSpannableString", SupportSpannableString())
        }
    }
    RemoteDemo(profile = experimentalProfile, customSupport = customSupport) {
        RemoteColumn(modifier = RemoteModifier.padding(16.rdp)) {
            LinksSample()
            SpanStylesSample()
            BulletListSample()
            ParagraphAlignmentSample()
            TransformationsAndConcatSample()
        }
    }
}
