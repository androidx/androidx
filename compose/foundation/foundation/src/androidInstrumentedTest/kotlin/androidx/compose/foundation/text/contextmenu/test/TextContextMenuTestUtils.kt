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

package androidx.compose.foundation.text.contextmenu.test

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.test.R
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.internal.TextActionModeCallback
import androidx.compose.foundation.text.contextmenu.modifier.ToolbarRequester
import androidx.compose.foundation.text.contextmenu.modifier.collectTextContextMenuData
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.unit.toOffset
import com.google.common.truth.IterableSubject
import com.google.common.truth.Ordered
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

/**
 * Can collect the context menu items wherever its associated
 * [Modifier.testTextContextMenuDataReader][testTextContextMenuDataReader] is positioned.
 */
internal class TestTextContextMenuDataInvoker {
    internal var node: TestTextContextMenuDataReaderNode? = null

    fun invokeTraversal(): TextContextMenuData =
        checkPreconditionNotNull(node).collectTextContextMenuData()
}

/** Wires up the [invoker] to collect the [TextContextMenuData] at this modifier. */
internal fun Modifier.testTextContextMenuDataReader(
    invoker: TestTextContextMenuDataInvoker
): Modifier = this then TestTextContextMenuDataReaderElement(invoker)

private data class TestTextContextMenuDataReaderElement(
    val invoker: TestTextContextMenuDataInvoker
) : ModifierNodeElement<TestTextContextMenuDataReaderNode>() {
    override fun create(): TestTextContextMenuDataReaderNode =
        TestTextContextMenuDataReaderNode(invoker)

    override fun update(node: TestTextContextMenuDataReaderNode) {
        node.update(invoker)
    }
}

internal class TestTextContextMenuDataReaderNode(invoker: TestTextContextMenuDataInvoker) :
    Modifier.Node() {
    var invoker: TestTextContextMenuDataInvoker = invoker
        private set

    override fun onAttach() {
        super.onAttach()
        invoker.node = this
    }

    override fun onDetach() {
        invoker.node = null
        super.onDetach()
    }

    fun update(reader: TestTextContextMenuDataInvoker) {
        this.invoker = reader
    }
}

internal fun TextContextMenuData.assertItems(expectedKeys: List<Any>) {
    assertThat(components.map { it.key }).containsExactlyList(expectedKeys).inOrder()
}

private inline fun <reified T> IterableSubject.containsExactlyList(expected: List<T>): Ordered =
    containsExactly(*expected.toTypedArray())

internal class SpyTextActionModeCallback : TextActionModeCallback {
    lateinit var delegate: TextActionModeCallback
    var actionMode: ActionMode? = null
    var menu: Menu? = null
    var contentRect: Rect? = null

    override fun onGetContentRect(mode: ActionMode, view: View?): Rect {
        this.actionMode = mode
        return delegate.onGetContentRect(mode, view).also { contentRect = it }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        this.actionMode = mode
        this.menu = menu
        return delegate.onPrepareActionMode(mode, menu)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        this.actionMode = mode
        this.menu = menu
        return delegate.onCreateActionMode(mode, menu)
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        this.actionMode = mode
        return delegate.onActionItemClicked(mode, item)
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        this.actionMode = null
        this.menu = null
        delegate.onDestroyActionMode(mode)
    }
}

internal fun SpyTextActionModeCallback.assertShown(shown: Boolean = true) {
    val message = "Text toolbar status should ${if (shown) "" else "not "}be shown."
    val subject = assertWithMessage(message).that(menu)
    if (shown) {
        subject.isNotNull()
    } else {
        subject.isNull()
    }
}

internal fun SemanticsNodeInteractionsProvider.onItemWithNumber(i: Int): SemanticsNodeInteraction =
    onNodeWithText(labelForNumber(i))

internal fun SemanticsNodeInteraction.positionInScreen(): IntOffset =
    fetchSemanticsNode().positionOnScreen.round()

internal fun testDataProvider(vararg itemNumbers: Int): TextContextMenuDataProvider =
    testDataProvider(positioner = { defaultPositioner(it) }, data = { numbersToData(*itemNumbers) })

internal fun numbersToData(vararg itemNumbers: Int): TextContextMenuData =
    TextContextMenuData(itemNumbers.map { testItem(it) })

internal fun testDataProvider(
    positioner: (LayoutCoordinates) -> Offset = { defaultPositioner(it) },
    data: () -> TextContextMenuData,
): TextContextMenuDataProvider =
    object : TextContextMenuDataProvider {
        override fun position(destinationCoordinates: LayoutCoordinates): Offset =
            positioner(destinationCoordinates)

        override fun contentBounds(destinationCoordinates: LayoutCoordinates): Rect =
            position(destinationCoordinates).let { Rect(it, it) }

        override fun data(): TextContextMenuData = data()
    }

internal fun defaultPositioner(destinationCoordinates: LayoutCoordinates): Offset =
    destinationCoordinates.size.toIntRect().center.toOffset()

internal fun testItem(
    i: Int,
    onClick: TextContextMenuSession.() -> Unit = {},
): TextContextMenuItem =
    TextContextMenuItem(
        key = i,
        label = labelForNumber(i),
        leadingIcon = R.drawable.ic_vector_asset_test,
        onClick = onClick,
    )

internal fun labelForNumber(i: Int): String = "Item $i"

internal fun numberForLabel(label: CharSequence): Int = label.split(" ").last().toInt()

internal fun Menu.items(): List<MenuItem> {
    val items = mutableListOf<MenuItem>()
    for (i in 0 until size()) {
        items.add(getItem(i))
    }
    return items
}

internal fun <T : Any> assertNotNull(obj: T?, messageBlock: (() -> String)? = null): T =
    obj.also {
        val subject =
            if (messageBlock != null) {
                assertWithMessage(messageBlock()).that(it)
            } else {
                assertThat(it)
            }
        subject.isNotNull()
    }!!

internal class FakeToolbarRequester : ToolbarRequester() {
    var shown = false

    override fun show() {
        shown = true
    }

    override fun hide() {
        shown = false
    }
}

internal class FakeTextContextMenuProvider(
    private val onShow: suspend (TextContextMenuDataProvider) -> Unit
) : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider) {
        onShow(dataProvider)
    }
}
