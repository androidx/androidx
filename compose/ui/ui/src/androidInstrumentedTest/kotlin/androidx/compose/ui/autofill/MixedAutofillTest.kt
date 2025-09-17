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

import android.graphics.Rect as AndroidRect
import android.util.SparseArray
import android.view.View
import android.view.View.AUTOFILL_TYPE_TEXT
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import androidx.autofill.HintConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.onAutofillText
import androidx.compose.ui.semantics.onFillData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.accessibility.AccessibilityNodeProviderCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 26)
class MixedAutofillTest {
    @get:Rule val rule = createComposeRule()
    private val height = 200.dp
    private val width = 200.dp
    private val previousFlagValue =
        @OptIn(ExperimentalComposeUiApi::class) ComposeUiFlags.isSemanticAutofillEnabled

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

    @Test
    fun populateViewStructure_empty() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent { view = LocalView.current }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    fun autofill_empty() {
        // Arrange.
        lateinit var view: View
        val viewStructure: ViewStructure = FakeViewStructure()
        rule.setContent { view = LocalView.current }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.autofill(
                SparseArray<AutofillValue>(2).apply {
                    append(1, AutofillValue.forText("any"))
                    append(2, AutofillValue.forText("any"))
                }
            )
        }

        // Assert.
        assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    fun populateViewStructure_new_old_sameLayoutNode() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        val viewStructure: ViewStructure = FakeViewStructure()
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(onFill = {}, autofillTypes = listOf(AutofillType.Password))
            }
            Box(
                Modifier.semantics {
                        testTag = "newApi"
                        contentType = ContentType.Username
                    }
                    .size(height, width)
                    .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
            ) {
                DisposableEffect(autofillNode) {
                    autofillTree.children[autofillNode.id] = autofillNode
                    onDispose { autofillTree.children.remove(autofillNode.id) }
                }
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    packageName = view.context.applicationInfo.packageName
                    bounds = AndroidRect(0, 0, width.dpToPx(), height.dpToPx())
                    autofillId = view.autofillId
                    isEnabled = true
                    dataIsSensitive = true
                    children.add(
                        FakeViewStructure().apply {
                            virtualId = rule.onNodeWithTag("newApi").semanticsId()
                            packageName = view.context.applicationInfo.packageName
                            bounds = AndroidRect(0, 0, width.dpToPx(), height.dpToPx())
                            autofillId = view.autofillId
                            isEnabled = true
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            visibility = View.VISIBLE
                            isLongClickable = false
                            isFocusable = false
                            isFocused = false
                            isEnabled = true
                            dataIsSensitive = true
                        }
                    )
                    children.add(
                        FakeViewStructure().apply {
                            virtualId = autofillNode.id
                            packageName = view.context.applicationInfo.packageName
                            bounds = AndroidRect(0, 0, width.dpToPx(), height.dpToPx())
                            autofillId = view.autofillId
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_PASSWORD)
                            visibility = View.VISIBLE
                            isLongClickable = false
                            isFocusable = false
                            isFocused = false
                        }
                    )
                }
            )
    }

    @Test
    fun populateViewStructure_new_old_differentLayoutNodes() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        val viewStructure: ViewStructure = FakeViewStructure()
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(onFill = {}, autofillTypes = listOf(AutofillType.Password))
            }
            Column {
                Box(
                    Modifier.semantics { contentType = ContentType.Username }
                        .size(height, width)
                        .testTag("newApi")
                )
                Box(
                    Modifier.size(height, width).onGloballyPositioned {
                        autofillNode.boundingBox = it.boundsInWindow()
                    }
                ) {
                    DisposableEffect(autofillNode) {
                        autofillTree.children[autofillNode.id] = autofillNode
                        onDispose { autofillTree.children.remove(autofillNode.id) }
                    }
                }
            }
        }

        // Act.
        rule.runOnIdle {
            // Compose does not use the Autofill flags parameter, passing in 0 as a placeholder flag
            view.onProvideAutofillVirtualStructure(viewStructure, 0)
        }

        // Assert.
        assertThat(viewStructure)
            .isEqualTo(
                FakeViewStructure().apply {
                    virtualId = AccessibilityNodeProviderCompat.HOST_VIEW_ID
                    packageName = view.context.applicationInfo.packageName
                    bounds = AndroidRect(0, 0, width.dpToPx(), 2 * height.dpToPx())
                    autofillId = view.autofillId
                    isEnabled = true
                    dataIsSensitive = true
                    children.add(
                        FakeViewStructure().apply {
                            virtualId = rule.onNodeWithTag("newApi").semanticsId()
                            packageName = view.context.applicationInfo.packageName
                            bounds = AndroidRect(0, 0, width.dpToPx(), height.dpToPx())
                            autofillId = view.autofillId
                            isEnabled = true
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_USERNAME)
                            visibility = View.VISIBLE
                            isLongClickable = false
                            isFocusable = false
                            isFocused = false
                            isEnabled = true
                            dataIsSensitive = true
                        }
                    )
                    children.add(
                        FakeViewStructure().apply {
                            virtualId = autofillNode.id
                            packageName = view.context.applicationInfo.packageName
                            bounds =
                                AndroidRect(0, height.dpToPx(), width.dpToPx(), 2 * height.dpToPx())
                            autofillId = view.autofillId
                            autofillType = AUTOFILL_TYPE_TEXT
                            autofillHints = mutableListOf(HintConstants.AUTOFILL_HINT_PASSWORD)
                            visibility = View.VISIBLE
                            isLongClickable = false
                            isFocusable = false
                            isFocused = false
                        }
                    )
                }
            )
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofill_new_old_sameLayoutNode() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        lateinit var autoFilledValueNewApi: String
        lateinit var autoFilledValueOldApi: String
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(
                    onFill = { autoFilledValueOldApi = it },
                    autofillTypes = listOf(AutofillType.Password),
                )
            }
            Box(
                Modifier.semantics {
                        testTag = "newApi"
                        contentType = ContentType.Username
                        onFillData {
                            autoFilledValueNewApi = it.textValue as String
                            true
                        }
                    }
                    .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
                    .size(height, width)
            ) {
                DisposableEffect(autofillNode) {
                    autofillTree.children[autofillNode.id] = autofillNode
                    onDispose { autofillTree.children.remove(autofillNode.id) }
                }
            }
        }

        // Act.
        val newApiSemanticsId = rule.onNodeWithTag("newApi").semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>(2).apply {
                    append(newApiSemanticsId, AutofillValue.forText("TestUsername"))
                    append(autofillNode.id, AutofillValue.forText("TestPassword"))
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(autoFilledValueNewApi).isEqualTo("TestUsername")
            assertThat(autoFilledValueOldApi).isEqualTo("TestPassword")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofill_new_old_sameLayoutNode_onAutofillText() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        lateinit var autoFilledValueNewApi: String
        lateinit var autoFilledValueOldApi: String
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(
                    onFill = { autoFilledValueOldApi = it },
                    autofillTypes = listOf(AutofillType.Password),
                )
            }
            Box(
                Modifier.semantics {
                        testTag = "newApi"
                        contentType = ContentType.Username
                        onAutofillText {
                            autoFilledValueNewApi = it.toString()
                            true
                        }
                    }
                    .onGloballyPositioned { autofillNode.boundingBox = it.boundsInWindow() }
                    .size(height, width)
            ) {
                DisposableEffect(autofillNode) {
                    autofillTree.children[autofillNode.id] = autofillNode
                    onDispose { autofillTree.children.remove(autofillNode.id) }
                }
            }
        }

        // Act.
        val newApiSemanticsId = rule.onNodeWithTag("newApi").semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>(2).apply {
                    append(newApiSemanticsId, AutofillValue.forText("TestUsername"))
                    append(autofillNode.id, AutofillValue.forText("TestPassword"))
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(autoFilledValueNewApi).isEqualTo("TestUsername")
            assertThat(autoFilledValueOldApi).isEqualTo("TestPassword")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofill_new_old_differentLayoutNodes() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        lateinit var autoFilledValueNewApi: String
        lateinit var autoFilledValueOldApi: String
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(
                    onFill = { autoFilledValueOldApi = it },
                    autofillTypes = listOf(AutofillType.Password),
                )
            }
            Column {
                Box(
                    Modifier.semantics {
                            contentType = ContentType.Username
                            onFillData {
                                autoFilledValueNewApi = it.textValue as String
                                true
                            }
                        }
                        .size(height, width)
                        .testTag("newApi")
                )
                Box(
                    Modifier.size(height, width).onGloballyPositioned {
                        autofillNode.boundingBox = it.boundsInWindow()
                    }
                ) {
                    DisposableEffect(autofillNode) {
                        autofillTree.children[autofillNode.id] = autofillNode
                        onDispose { autofillTree.children.remove(autofillNode.id) }
                    }
                }
            }
        }

        // Act.
        val newApiSemanticsId = rule.onNodeWithTag("newApi").semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>(2).apply {
                    append(newApiSemanticsId, AutofillValue.forText("TestUsername"))
                    append(autofillNode.id, AutofillValue.forText("TestPassword"))
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(autoFilledValueNewApi).isEqualTo("TestUsername")
            assertThat(autoFilledValueOldApi).isEqualTo("TestPassword")
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun autofill_new_old_differentLayoutNodes_onAutofillText() {
        // Arrange.
        lateinit var view: View
        lateinit var autofillTree: @Suppress("DEPRECATION") AutofillTree
        lateinit var autofillNode: @Suppress("DEPRECATION") AutofillNode
        lateinit var autoFilledValueNewApi: String
        lateinit var autoFilledValueOldApi: String
        rule.setContent {
            view = LocalView.current
            autofillTree = @Suppress("DEPRECATION") LocalAutofillTree.current
            autofillNode = remember {
                @Suppress("DEPRECATION")
                AutofillNode(
                    onFill = { autoFilledValueOldApi = it },
                    autofillTypes = listOf(AutofillType.Password),
                )
            }
            Column {
                Box(
                    Modifier.semantics {
                            contentType = ContentType.Username
                            onAutofillText {
                                autoFilledValueNewApi = it.toString()
                                true
                            }
                        }
                        .size(height, width)
                        .testTag("newApi")
                )
                Box(
                    Modifier.size(height, width).onGloballyPositioned {
                        autofillNode.boundingBox = it.boundsInWindow()
                    }
                ) {
                    DisposableEffect(autofillNode) {
                        autofillTree.children[autofillNode.id] = autofillNode
                        onDispose { autofillTree.children.remove(autofillNode.id) }
                    }
                }
            }
        }

        // Act.
        val newApiSemanticsId = rule.onNodeWithTag("newApi").semanticsId()
        rule.runOnIdle {
            view.autofill(
                SparseArray<AutofillValue>(2).apply {
                    append(newApiSemanticsId, AutofillValue.forText("TestUsername"))
                    append(autofillNode.id, AutofillValue.forText("TestPassword"))
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(autoFilledValueNewApi).isEqualTo("TestUsername")
            assertThat(autoFilledValueOldApi).isEqualTo("TestPassword")
        }
    }

    private fun Dp.dpToPx() = with(rule.density) { this@dpToPx.roundToPx() }
}
