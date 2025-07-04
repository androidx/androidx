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

package androidx.compose.foundation.text.input

import android.os.Build
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.CoreTextField
import androidx.compose.foundation.text.assertNoStylusHoverIcon
import androidx.compose.foundation.text.assertStylusHandwritingHoverIcon
import androidx.compose.foundation.text.handwriting.HandwritingBoundsVerticalOffset
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.foundation.text.performStylusInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class HandwritingHoverIconTest {
    @get:Rule val rule = createComposeRule()

    private lateinit var ownerView: View

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    fun stylusHandwritingHoverIcon() {
        // Test is only meaningful when stylus handwriting is supported.
        Assume.assumeTrue(isStylusHandwritingSupported)

        val basicTextFieldTag = "basicTextField"
        val coreTextFieldTag = "coreTextField"
        val coreTextFieldUnsupportedTag = "coreTextFieldUnsupported"
        val outsideBoundsSpacer1Tag = "spacer1"
        val withinBoundsSpacer2Tag = "spacer2"
        val withinBoundsSpacer3Tag = "spacer3"
        val withinBoundsSpacer4Tag = "spacer4"
        val outsideBoundsSpacer5Tag = "spacer5"

        rule.setContent {
            ownerView = LocalView.current

            val basicTextFieldState = remember { TextFieldState() }
            var coreTextFieldValue by remember { mutableStateOf(TextFieldValue()) }
            var coreTextFieldUnsupportedValue by remember { mutableStateOf(TextFieldValue()) }

            Column(Modifier.safeContentPadding()) {
                // This spacer is outside the extended handwriting bounds of the text fields
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(outsideBoundsSpacer1Tag)
                )
                // This spacer is within the extended handwriting bounds of basicTextField
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(withinBoundsSpacer2Tag)
                )
                // This text field supports handwriting
                BasicTextField(
                    state = basicTextFieldState,
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(basicTextFieldTag),
                )
                // This spacer is within the extended handwriting bounds of both text fields
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(withinBoundsSpacer3Tag)
                )
                // This text field supports handwriting
                CoreTextField(
                    value = coreTextFieldValue,
                    onValueChange = { coreTextFieldValue = it },
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(coreTextFieldTag),
                )
                // This spacer is within the extended handwriting bounds of coreTextField
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(withinBoundsSpacer4Tag)
                )
                // This spacer is outside the extended handwriting bounds of the text fields
                Spacer(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(outsideBoundsSpacer5Tag)
                )
                // Password text field does not support handwriting
                CoreTextField(
                    value = coreTextFieldUnsupportedValue,
                    onValueChange = { coreTextFieldUnsupportedValue = it },
                    imeOptions = ImeOptions(keyboardType = KeyboardType.Password),
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(HandwritingBoundsVerticalOffset)
                            .testTag(coreTextFieldUnsupportedTag),
                )
            }
        }

        // No stylus icon shown before hover starts
        rule.runOnIdle { assertNoStylusHoverIcon(ownerView) }

        // This spacer is outside the extended handwriting bounds of the text fields, no icon shown
        rule.onNodeWithTag(outsideBoundsSpacer1Tag).performStylusInput { hoverEnter(center) }
        rule.runOnIdle { assertNoStylusHoverIcon(ownerView) }

        // This spacer is within the extended handwriting bounds of basicTextField, so icon is shown
        rule.onNodeWithTag(withinBoundsSpacer2Tag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertStylusHandwritingHoverIcon(ownerView) }

        // This is within basicTextField, so icon is shown
        rule.onNodeWithTag(basicTextFieldTag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertStylusHandwritingHoverIcon(ownerView) }

        // This spacer is within the extended handwriting bounds of basicTextField, so icon is shown
        rule.onNodeWithTag(withinBoundsSpacer4Tag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertStylusHandwritingHoverIcon(ownerView) }

        // This spacer is outside the extended handwriting bounds of the text fields, no icon shown
        rule.onNodeWithTag(outsideBoundsSpacer5Tag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertNoStylusHoverIcon(ownerView) }

        // This is within coreTextField, so icon is shown
        rule.onNodeWithTag(coreTextFieldTag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertStylusHandwritingHoverIcon(ownerView) }

        // This is within coreTextFieldUnsupported, so no icon shown
        rule.onNodeWithTag(coreTextFieldUnsupportedTag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertNoStylusHoverIcon(ownerView) }

        // This spacer is within the extended handwriting bounds of both text fields, icon is shown
        rule.onNodeWithTag(withinBoundsSpacer3Tag).performStylusInput { hoverMoveTo(center) }
        rule.runOnIdle { assertStylusHandwritingHoverIcon(ownerView) }

        // Hover exit, so no icon shown
        rule.onNodeWithTag(withinBoundsSpacer3Tag).performStylusInput { hoverExit(center) }
        rule.runOnIdle { assertNoStylusHoverIcon(ownerView) }
    }
}
