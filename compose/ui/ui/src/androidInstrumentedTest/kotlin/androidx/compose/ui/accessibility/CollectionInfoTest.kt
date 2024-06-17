/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.accessibility

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CollectionInfoTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    private lateinit var composeView: AndroidComposeView
    private val tag = "TestTag"

    // Collection Info tests
    @Test
    fun testCollectionInfo_withSelectableGroup() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.testTag(tag).selectableGroup()) {
                Box(Modifier.size(50.dp).selectable(selected = true, onClick = {}))
                Box(Modifier.size(50.dp).selectable(selected = false, onClick = {}))
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionInfo) {
                assertThat(rowCount).isEqualTo(2)
                assertThat(columnCount).isEqualTo(1)
                assertThat(isHierarchical).isFalse()
            }
        }
    }

    @Test
    fun testDefaultCollectionInfo_lazyList() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(Modifier.testTag(tag)) { items(2) { BasicText("Text") } }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionInfo) {
                assertThat(rowCount).isEqualTo(-1)
                assertThat(columnCount).isEqualTo(1)
                assertThat(isHierarchical).isFalse()
            }
        }
    }

    @Test
    fun testCollectionInfo_lazyList() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(Modifier.testTag(tag).semantics { collectionInfo = CollectionInfo(2, 1) }) {
                items(2) { BasicText("Text") }
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionInfo) {
                assertThat(rowCount).isEqualTo(2)
                assertThat(columnCount).isEqualTo(1)
                assertThat(isHierarchical).isFalse()
            }
        }
    }

    @Test
    fun testCollectionInfo_withSelectableGroup_andDefaultLazyListSemantics() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(Modifier.testTag(tag).selectableGroup()) { items(2) { BasicText("Text") } }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionInfo) {
                assertThat(rowCount).isEqualTo(-1)
                assertThat(columnCount).isEqualTo(1)
                assertThat(isHierarchical).isFalse()
            }
        }
    }

    @Test
    fun testCollectionInfo_withSelectableGroup_andLazyListSemantics() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(
                Modifier.testTag(tag).selectableGroup().semantics {
                    collectionInfo = CollectionInfo(2, 1)
                }
            ) {
                items(2) { BasicText("Text") }
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionInfo) {
                assertThat(rowCount).isEqualTo(2)
                assertThat(columnCount).isEqualTo(1)
                assertThat(isHierarchical).isFalse()
            }
        }
    }

    // Collection Item Info tests
    @Test
    fun testCollectionItemInfo_withSelectableGroup() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.selectableGroup()) {
                Box(Modifier.size(50.dp).testTag(tag).selectable(selected = true, onClick = {}))
                Box(Modifier.size(50.dp).selectable(selected = false, onClick = {}))
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionItemInfo) {
                assertThat(rowIndex).isEqualTo(0)
                assertThat(rowSpan).isEqualTo(1)
                assertThat(columnIndex).isEqualTo(0)
                assertThat(columnSpan).isEqualTo(1)
                assertThat(isSelected).isTrue()
            }
        }
    }

    @Test
    fun testNoCollectionItemInfo_lazyList() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn {
                itemsIndexed(listOf("Text", "Text")) { index, item -> BasicText(item + index) }
            }
        }
        val virtualId = rule.onNodeWithText("Text0").semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { assertThat(info.collectionItemInfo).isNull() }
    }

    @Test
    fun testCollectionItemInfo_defaultLazyListSemantics() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }
        val virtualId = rule.onNodeWithText("Text0").semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionItemInfo) {
                assertThat(rowIndex).isEqualTo(0)
                assertThat(rowSpan).isEqualTo(1)
                assertThat(columnIndex).isEqualTo(0)
                assertThat(columnSpan).isEqualTo(1)
                assertThat(isSelected).isFalse()
            }
        }
    }

    @Test
    fun testCollectionItemInfo_lazyList() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(Modifier.semantics { collectionInfo = CollectionInfo(2, 1) }) {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }
        val virtualId = rule.onNodeWithText("Text0").semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionItemInfo) {
                assertThat(rowIndex).isEqualTo(0)
                assertThat(rowSpan).isEqualTo(1)
                assertThat(columnIndex).isEqualTo(0)
                assertThat(columnSpan).isEqualTo(1)
                assertThat(isSelected).isFalse()
            }
        }
    }

    @Test
    fun testCollectionItemInfo_withSelectableGroup_andDefaultLazyListSemantics() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            LazyColumn(Modifier.selectableGroup()) {
                itemsIndexed(listOf("Text", "Text")) { index, item ->
                    BasicText(
                        item + index,
                        Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(index, 1, 0, 1)
                        }
                    )
                }
            }
        }
        val virtualId = rule.onNodeWithText("Text0").semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            with(info.collectionItemInfo) {
                assertThat(rowIndex).isEqualTo(0)
                assertThat(rowSpan).isEqualTo(1)
                assertThat(columnIndex).isEqualTo(0)
                assertThat(columnSpan).isEqualTo(1)
                assertThat(isSelected).isFalse()
            }
        }
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_whenProvidedDirectly() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(
                Modifier.size(10.dp).testTag(tag).semantics {
                    collectionInfo = CollectionInfo(1, 1)
                }
            ) {
                // items
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert
        rule.runOnIdle {
            assertThat(info.collectionInfo.columnCount).isEqualTo(1)
            assertThat(info.collectionInfo.rowCount).isEqualTo(1)
        }
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_whenProvidedViaSelectableGroup() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.size(10.dp).testTag(tag).selectableGroup()) {
                // items
                Box(Modifier.size(10.dp).selectable(selected = true, onClick = {}))
                Box(Modifier.size(10.dp).selectable(selected = false, onClick = {}))
                Box(Modifier.size(10.dp).selectable(selected = false, onClick = {}))
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle {
            assertThat(info.collectionInfo.columnCount).isEqualTo(1)
            assertThat(info.collectionInfo.rowCount).isEqualTo(3)
        }
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_whenProvidedViaEmptySelectableGroup() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.size(10.dp).testTag(tag).selectableGroup()) {
                // items
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { assertThat(info.collectionInfo).isNull() }
    }

    @Test
    fun testSemanticsNodeHasCollectionInfo_falseWhenNotProvided() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.size(10.dp).testTag(tag)) {
                // items
            }
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        assertThat(info.collectionInfo).isNull()
    }

    @Test
    fun testCollectionInfo_withSelectableGroup_zOrder() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Column(Modifier.selectableGroup()) {
                Box(
                    Modifier.size(50.dp)
                        .selectable(selected = true, onClick = {})
                        .zIndex(3f)
                        .testTag("item0")
                )
                Box(
                    Modifier.size(50.dp)
                        .selectable(selected = false, onClick = {})
                        .zIndex(2f)
                        .testTag("item1")
                )
                Box(
                    Modifier.size(50.dp)
                        .selectable(selected = false, onClick = {})
                        .zIndex(1f)
                        .testTag("item2")
                )
            }
        }
        val virtualId0 = rule.onNodeWithTag("item0").semanticsId
        val virtualId1 = rule.onNodeWithTag("item1").semanticsId
        val virtualId2 = rule.onNodeWithTag("item2").semanticsId

        // Act.
        rule.waitForIdle()
        val info0 = composeView.createAccessibilityNodeInfo(virtualId0)
        val info1 = composeView.createAccessibilityNodeInfo(virtualId1)
        val info2 = composeView.createAccessibilityNodeInfo(virtualId2)

        // Assert.
        rule.runOnIdle {
            assertThat(info0.collectionItemInfo.rowIndex).isEqualTo(0)
            assertThat(info1.collectionItemInfo.rowIndex).isEqualTo(1)
            assertThat(info2.collectionItemInfo.rowIndex).isEqualTo(2)
        }
    }

    @Test
    fun horizontalPager() {
        // Arrange.
        val pageCount = 20
        rule.setContentWithAccessibilityEnabled {
            HorizontalPager(rememberPagerState { pageCount }, Modifier.size(10.dp).testTag(tag)) {}
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { assertThat(info.collectionInfo.columnCount).isEqualTo(pageCount) }
    }

    @Test
    fun verticalPager() {
        // Arrange.
        val pageCount = 20
        rule.setContentWithAccessibilityEnabled {
            VerticalPager(rememberPagerState { pageCount }, Modifier.size(10.dp).testTag(tag)) {}
        }
        val virtualId = rule.onNodeWithTag(tag).semanticsId

        // Act.
        val info = rule.runOnIdle { composeView.createAccessibilityNodeInfo(virtualId) }

        // Assert.
        rule.runOnIdle { assertThat(info.collectionInfo.rowCount).isEqualTo(pageCount) }
    }

    // TODO(b/272068594): Add api to fetch the semantics id from SemanticsNodeInteraction directly.
    private val SemanticsNodeInteraction.semanticsId: Int
        get() = fetchSemanticsNode().id

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            composeView = LocalView.current as AndroidComposeView
            val accessibilityDelegate = ViewCompat.getAccessibilityDelegate(composeView)
            with(accessibilityDelegate as AndroidComposeViewAccessibilityDelegateCompat) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = { false }
            }
            content()
        }
    }

    private fun AndroidComposeView.createAccessibilityNodeInfo(
        semanticsId: Int
    ): AccessibilityNodeInfoCompat {
        val accNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(semanticsId)
        checkNotNull(accNodeInfo) { "Could not find semantics node with id = $semanticsId" }
        return AccessibilityNodeInfoCompat.wrap(accNodeInfo)
    }
}
