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

package androidx.compose.remote.integration.demos.document

import androidx.collection.IntList
import androidx.collection.IntSet
import androidx.collection.buildIntList
import androidx.collection.buildIntSet
import androidx.collection.emptyIntList
import androidx.collection.emptyIntSet
import androidx.collection.mutableIntSetOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.serialization.yaml.YAMLSerializer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class DocumentFormat {
    TEXT,
    YAML,
}

internal data class DocumentNode(
    val id: Int,
    val text: String,
    val depth: Int,
    val ancestorIds: IntList,
    val children: List<DocumentNode>,
)

@Stable
@Suppress("RestrictedApiAndroidX")
internal class DocumentViewState(initialDocument: CoreDocument? = null) {
    private var _document by mutableStateOf(initialDocument)
    var document: CoreDocument?
        get() = _document
        set(value) {
            if (_document !== value) {
                _document = value
                if (value == null) {
                    rootNodes = emptyList()
                    collapsedIds = emptyIntSet()
                    activeMatchNodeId = null
                }
            }
        }

    var selectedFormat by mutableStateOf(DocumentFormat.TEXT)
        private set

    var rootNodes: List<DocumentNode> by mutableStateOf(emptyList())
        private set

    private val allNodes: List<DocumentNode> by derivedStateOf { flattenAllNodes(rootNodes) }

    private val allExpandableIds: IntSet by derivedStateOf { collectExpandableIds(rootNodes) }

    var collapsedIds: IntSet by mutableStateOf(emptyIntSet())
        private set

    val visibleNodes: List<DocumentNode> by derivedStateOf {
        flattenVisibleNodes(rootNodes, collapsedIds)
    }

    var isSearching by mutableStateOf(false)
        private set

    val searchQueryState = TextFieldState()

    val searchQuery: CharSequence
        get() = searchQueryState.text

    var activeMatchNodeId by mutableStateOf<Int?>(null)
        private set

    var scrollTrigger by mutableIntStateOf(0)
        private set

    suspend fun parseCurrentDocument() {
        val currentDoc = document
        val format = selectedFormat
        if (currentDoc == null) {
            rootNodes = emptyList()
            return
        }
        val parsedNodes =
            withContext(Dispatchers.Default) {
                val formatted =
                    when (format) {
                        DocumentFormat.TEXT -> currentDoc.toNestedString()
                        DocumentFormat.YAML -> {
                            val serializer = YAMLSerializer()
                            currentDoc.serialize(serializer.serializeMap())
                            serializer.toString()
                        }
                    }
                if (formatted.isEmpty()) {
                    emptyList()
                } else {
                    parseDocumentNodes(formatted, format)
                }
            }
        rootNodes = parsedNodes
    }

    fun showSearch() {
        isSearching = true
    }

    fun hideSearch() {
        isSearching = false
        searchQueryState.clearText()
        activeMatchNodeId = null
    }

    fun clearActiveMatch() {
        activeMatchNodeId = null
    }

    fun findPrevious() {
        findMatch(forward = false)
    }

    fun findNext() {
        findMatch(forward = true)
    }

    private fun findMatch(forward: Boolean) {
        if (searchQuery.isBlank()) return
        val matches = allNodes.filter { it.text.contains(searchQuery, ignoreCase = true) }
        if (matches.isEmpty()) {
            activeMatchNodeId = null
            return
        }
        val currentIndex = matches.indexOfFirst { it.id == activeMatchNodeId }
        val nextIndex =
            if (forward) {
                (currentIndex + 1) % matches.size
            } else {
                if (currentIndex <= 0) matches.lastIndex else currentIndex - 1
            }
        val targetMatch = matches[nextIndex]
        activeMatchNodeId = targetMatch.id
        if (targetMatch.ancestorIds.isNotEmpty()) {
            collapsedIds =
                buildIntSet(collapsedIds.size) {
                    addAll(collapsedIds)
                    targetMatch.ancestorIds.forEach { remove(it) }
                }
        }
        scrollTrigger++
    }

    fun showAsText() {
        if (selectedFormat != DocumentFormat.TEXT) {
            selectedFormat = DocumentFormat.TEXT
            collapsedIds = emptyIntSet()
            activeMatchNodeId = null
        }
    }

    fun showAsYaml() {
        if (selectedFormat != DocumentFormat.YAML) {
            selectedFormat = DocumentFormat.YAML
            collapsedIds = emptyIntSet()
            activeMatchNodeId = null
        }
    }

    fun expandAll() {
        collapsedIds = emptyIntSet()
    }

    fun collapseAll() {
        collapsedIds = allExpandableIds
    }

    fun toggleNode(nodeId: Int) {
        collapsedIds =
            buildIntSet(collapsedIds.size + 1) {
                addAll(collapsedIds)
                if (nodeId in collapsedIds) {
                    remove(nodeId)
                } else {
                    add(nodeId)
                }
            }
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
internal fun DocumentScreen(state: DocumentViewState, modifier: Modifier = Modifier) {
    if (state.document == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    LaunchedEffect(state.document, state.selectedFormat) { state.parseCurrentDocument() }

    val visibleNodes = state.visibleNodes
    val collapsedIds = state.collapsedIds
    val listState = rememberLazyListState()

    LaunchedEffect(state) {
        snapshotFlow { state.searchQueryState.text }.collect { state.clearActiveMatch() }
    }

    LaunchedEffect(state.activeMatchNodeId, state.scrollTrigger) {
        val targetId = state.activeMatchNodeId ?: return@LaunchedEffect
        val index = state.visibleNodes.indexOfFirst { it.id == targetId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        if (state.isSearching) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    state = state.searchQueryState,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search") },
                    lineLimits = TextFieldLineLimits.SingleLine,
                    trailingIcon = {
                        IconButton(onClick = { state.hideSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                )
                OutlinedButton(
                    onClick = { state.findPrevious() },
                    enabled = state.searchQuery.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous match",
                    )
                }
                OutlinedButton(
                    onClick = { state.findNext() },
                    enabled = state.searchQuery.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next match",
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState()),
        ) {
            items(items = visibleNodes, key = { it.id }) { node ->
                val isExpandable = node.children.isNotEmpty()
                val isCollapsed = node.id in collapsedIds
                val isActiveMatch = node.id == state.activeMatchNodeId
                DocumentNodeRow(
                    node = node,
                    isExpandable = isExpandable,
                    isCollapsed = isCollapsed,
                    isActiveMatch = isActiveMatch,
                    onToggleExpand = { state.toggleNode(node.id) },
                )
            }
        }
    }
}

@Composable
private fun DocumentNodeRow(
    node: DocumentNode,
    isExpandable: Boolean,
    isCollapsed: Boolean,
    isActiveMatch: Boolean,
    onToggleExpand: () -> Unit,
) {
    val backgroundColor =
        if (isActiveMatch) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        }
    Row(
        modifier =
            Modifier.then(
                    if (isExpandable) Modifier.clickable(onClick = onToggleExpand) else Modifier
                )
                .padding(start = (node.depth * 16).dp, top = 2.dp, bottom = 2.dp)
                .background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isExpandable) {
            Icon(
                imageVector =
                    if (isCollapsed) {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                modifier = Modifier.size(20.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
        Text(text = if (isCollapsed) "${node.text} (...)" else node.text)
    }
}

private class MutableDocumentNode(
    val id: Int,
    val text: String,
    val effectiveIndent: Int,
    val children: MutableList<MutableDocumentNode> = mutableListOf(),
) {
    fun toDocumentNode(depth: Int, ancestorIds: IntList): DocumentNode {
        val childAncestorIds =
            if (children.isEmpty()) {
                emptyIntList()
            } else {
                buildIntList(ancestorIds.size + 1) {
                    addAll(ancestorIds)
                    add(id)
                }
            }
        return DocumentNode(
            id = id,
            text = text,
            depth = depth,
            ancestorIds = ancestorIds,
            children = children.map { it.toDocumentNode(depth + 1, childAncestorIds) },
        )
    }
}

/**
 * Parses [formattedDocument] into a tree of [DocumentNode]s based on line indentation.
 *
 * Note: This relies on the exact indentation formatting produced by `CoreDocument.toNestedString()`
 * and `YAMLSerializer.toString()`.
 */
private fun parseDocumentNodes(
    formattedDocument: String,
    format: DocumentFormat,
): List<DocumentNode> {
    val roots = mutableListOf<MutableDocumentNode>()
    val stack = ArrayDeque<MutableDocumentNode>()
    var nextId = 0

    for (line in formattedDocument.lineSequence()) {
        if (line.isBlank()) continue
        val leadingSpaces = line.takeWhile { it == ' ' }.length
        val trimmed = line.trim()
        val effectiveIndent =
            if (format == DocumentFormat.YAML && (trimmed.startsWith("- ") || trimmed == "-")) {
                leadingSpaces + 1
            } else {
                leadingSpaces
            }

        while (stack.isNotEmpty() && stack.last().effectiveIndent >= effectiveIndent) {
            stack.removeLast()
        }

        val node =
            MutableDocumentNode(id = nextId++, text = trimmed, effectiveIndent = effectiveIndent)
        if (stack.isEmpty()) {
            roots.add(node)
        } else {
            stack.last().children.add(node)
        }
        stack.addLast(node)
    }

    return roots.map { it.toDocumentNode(depth = 0, ancestorIds = emptyIntList()) }
}

private fun flattenAllNodes(nodes: List<DocumentNode>): List<DocumentNode> {
    val result = mutableListOf<DocumentNode>()
    fun visit(nodeList: List<DocumentNode>) {
        for (node in nodeList) {
            result.add(node)
            if (node.children.isNotEmpty()) {
                visit(node.children)
            }
        }
    }
    visit(nodes)
    return result
}

private fun collectExpandableIds(nodes: List<DocumentNode>): IntSet {
    val ids = mutableIntSetOf()
    fun visit(nodeList: List<DocumentNode>) {
        for (node in nodeList) {
            if (node.children.isNotEmpty()) {
                ids.add(node.id)
                visit(node.children)
            }
        }
    }
    visit(nodes)
    return ids
}

private fun flattenVisibleNodes(
    nodes: List<DocumentNode>,
    collapsedIds: IntSet,
): List<DocumentNode> {
    val result = mutableListOf<DocumentNode>()
    fun visit(nodeList: List<DocumentNode>) {
        for (node in nodeList) {
            result.add(node)
            if (node.children.isNotEmpty() && node.id !in collapsedIds) {
                visit(node.children)
            }
        }
    }
    visit(nodes)
    return result
}

@Suppress("RestrictedApiAndroidX")
@Preview
@Composable
private fun DocumentScreenPreview() {
    DocumentScreen(state = remember { DocumentViewState(initialDocument = null) })
}
