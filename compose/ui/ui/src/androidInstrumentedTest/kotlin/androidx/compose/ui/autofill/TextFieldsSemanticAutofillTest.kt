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

package androidx.compose.ui.autofill

import android.os.Build
import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.LocalAutofillHighlightColor
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
@RequiresApi(Build.VERSION_CODES.O)
class TextFieldsSemanticAutofillTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @OptIn(ExperimentalComposeUiApi::class)
    private val previousFlagValue = ComposeUiFlags.isSemanticAutofillEnabled

    @Before
    fun enableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = true
    }

    @After
    fun disableAutofill() {
        @OptIn(ExperimentalComposeUiApi::class)
        ComposeUiFlags.isSemanticAutofillEnabled = previousFlagValue
    }

    // ============================================================================================
    // Tests to verify legacy TextField populating and filling.
    // ============================================================================================

    @Test
    @SmallTest
    fun performAutofill_credentials_BTF() {
        // Arrange.
        lateinit var view: View
        val usernameTag = "username_tag"
        val passwordTag = "password_tag"
        var usernameInput by mutableStateOf("")
        var passwordInput by mutableStateOf("")

        rule.setContent {
            view = LocalView.current
            Column {
                BasicTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    modifier =
                        Modifier.testTag(usernameTag).semantics {
                            contentType = ContentType.Username
                        }
                )
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier =
                        Modifier.testTag(passwordTag).semantics {
                            contentType = ContentType.Password
                        }
                )
            }
        }

        // Act.
        val usernameId = rule.onNodeWithTag(usernameTag).semanticsId()
        val passwordId = rule.onNodeWithTag(passwordTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(usernameId, AutofillValue.forText("testUsername"))
                    append(passwordId, AutofillValue.forText("testPassword"))
                }
            )
        }

        // Assert
        rule.onNodeWithTag(usernameTag).assertTextEquals("testUsername")
        rule.onNodeWithTag(passwordTag).assertTextEquals("testPassword")
    }

    // ============================================================================================
    // Tests to verify TextField populating and filling.
    // ============================================================================================

    @Test
    @SmallTest
    fun performAutofill_credentials_legacyTF() {
        // Arrange.
        lateinit var view: View
        val usernameTag = "username_tag"
        var usernameInput by mutableStateOf("")
        rule.setContent {
            view = LocalView.current
            Column {
                TextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Enter username here") },
                    modifier =
                        Modifier.testTag(usernameTag).semantics {
                            contentType = ContentType.Username
                        }
                )
            }
        }

        // Act.
        val usernameId = rule.onNodeWithTag(usernameTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(usernameId, AutofillValue.forText("testUsername"))
                }
            )
        }

        // Assert.
        assertEquals(usernameInput, "testUsername")
    }

    @Test
    @SmallTest
    fun performAutofill_credentials_outlinedTF() {
        // Arrange.
        lateinit var view: View
        val usernameTag = "username_tag"
        var usernameInput by mutableStateOf("")
        rule.setContent {
            view = LocalView.current
            Column {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Enter username here") },
                    modifier =
                        Modifier.testTag(usernameTag).semantics {
                            contentType = ContentType.Username
                        }
                )
            }
        }

        // Act.
        val usernameId = rule.onNodeWithTag(usernameTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(usernameId, AutofillValue.forText("testUsername"))
                }
            )
        }

        // Assert.
        assertEquals(usernameInput, "testUsername")
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    fun performAutofill_customHighlight_legacyTF() {
        lateinit var view: View
        val usernameTag = "username_tag"
        var usernameInput by mutableStateOf("")
        val customHighlightColor = Color.Red

        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalAutofillHighlightColor provides customHighlightColor) {
                Column {
                    TextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Enter username here") },
                        modifier =
                            Modifier.testTag(usernameTag).semantics {
                                contentType = ContentType.Username
                            }
                    )
                }
            }
        }

        // Custom autofill highlight color should not appear prior to autofill being performed
        rule
            .onNodeWithTag(usernameTag)
            .captureToImage()
            .assertDoesNotContainColor(customHighlightColor)

        val usernameId = rule.onNodeWithTag(usernameTag).semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>().apply {
                    append(usernameId, AutofillValue.forText("testUsername"))
                }
            )
        }
        rule.waitForIdle()

        // Custom autofill highlight color should now appear
        rule.onNodeWithTag(usernameTag).captureToImage().assertContainsColor(customHighlightColor)
    }
}
