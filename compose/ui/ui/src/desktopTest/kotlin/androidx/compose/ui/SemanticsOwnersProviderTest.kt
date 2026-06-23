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

package androidx.compose.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.WindowTestScope
import androidx.compose.ui.window.runApplicationTest
import java.awt.BorderLayout
import javax.swing.JFrame
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SemanticsOwnersProviderTest {

    private fun semanticsOwnersProvidedBy(provider: SemanticsOwnersTestContext) = provider.runTest(
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(64.dp)
            ) {
                Text("Hello")
                OutlinedTextField(rememberTextFieldState("World"))
            }
        }
    ) {
        val strings = semanticsOwners.collectText().map { it.text }
        assertContentEquals(listOf("Hello", "World"), strings)
    }

    @Test
    fun semanticsOwnersProvidedInComposeWindow() =
        semanticsOwnersProvidedBy(ComposeWindowSemanticOwnersTestContext())

    @Test
    fun semanticsOwnersProvidedInVisibleComposePanel() =
        semanticsOwnersProvidedBy(ComposePanelSemanticOwnersTestContext(visible = true))

    @Test
    fun semanticsOwnersProvidedInInvisibleComposePanel() =
        semanticsOwnersProvidedBy(ComposePanelSemanticOwnersTestContext(visible = false))

    @Test
    fun semanticsOwnersProvidedInImageComposeScene() =
        semanticsOwnersProvidedBy(ImageComposeSceneSemanticOwnersTestContext())

    private fun semanticsOwnersIsSnapshotStateBy(provider: SemanticsOwnersTestContext) {
        var latestSemanticsOwners: Collection<SemanticsOwner> = emptyList()
        var showPopup by mutableStateOf(false)
        provider.runTest(
            content = {
                Text("Hello")
                if (showPopup) {
                    Popup {
                        Text("World")
                    }
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { provider.semanticsOwners }.collect {
                        latestSemanticsOwners = it
                    }
                }
            }
        ) {
            assertEquals(1, latestSemanticsOwners.size)
            showPopup = true
            awaitIdle()
            assertEquals(2, latestSemanticsOwners.size)
        }
    }

    @Test
    fun semanticsOwnersIsSnapshotStateInComposeWindow() =
        semanticsOwnersIsSnapshotStateBy(ComposeWindowSemanticOwnersTestContext())

    @Test
    fun semanticsOwnersIsSnapshotStateInComposePanel() =
        semanticsOwnersIsSnapshotStateBy(ComposePanelSemanticOwnersTestContext())

    @Test
    fun semanticsOwnersIsSnapshotStateInImageComposeScene() =
        semanticsOwnersIsSnapshotStateBy(ImageComposeSceneSemanticOwnersTestContext())
}

private interface SemanticsOwnersTestContext {
    val semanticsOwners: Collection<SemanticsOwner>

    fun runTest(
        content: @Composable () -> Unit,
        test: suspend SemanticsOwnersTestContext.() -> Unit
    )

    suspend fun awaitIdle()
}

private class ImageComposeSceneSemanticOwnersTestContext : SemanticsOwnersTestContext {
    private val scene: ImageComposeScene = ImageComposeScene(800, 600)
    private var time = 0L

    override val semanticsOwners: Collection<SemanticsOwner>
        get() = scene.semanticsOwners

    override fun runTest(
        content: @Composable () -> Unit,
        test: suspend SemanticsOwnersTestContext.() -> Unit
    ) {
        scene.setContent(content)
        scene.render(time)
        runBlocking {
            test()
        }
    }

    override suspend fun awaitIdle() {
        Snapshot.sendApplyNotifications()
        while (scene.hasInvalidations()) {
            time += 16L
            scene.render(time)
            Snapshot.sendApplyNotifications()
        }
    }
}

private class ComposeWindowSemanticOwnersTestContext : SemanticsOwnersTestContext {
    private lateinit var testScope: WindowTestScope

    @OptIn(ComposeToolingApi::class)
    override val semanticsOwners: Collection<SemanticsOwner>
        get() = testScope.window.semanticsOwners

    override fun runTest(
        content: @Composable (() -> Unit),
        test: suspend SemanticsOwnersTestContext.() -> Unit
    ) = runApplicationTest {
        testScope = this
        launchTestWindowApplication {
            content()
        }
        awaitIdle()
        test()
    }

    override suspend fun awaitIdle() = testScope.awaitIdle()
}

private class ComposePanelSemanticOwnersTestContext(
    val visible: Boolean = true
) : SemanticsOwnersTestContext {
    private lateinit var testScope: WindowTestScope
    private lateinit var composePanel: ComposePanel

    @OptIn(ComposeToolingApi::class)
    override val semanticsOwners: Collection<SemanticsOwner>
        get() = composePanel.semanticsOwners

    override fun runTest(
        content: @Composable (() -> Unit),
        test: suspend SemanticsOwnersTestContext.() -> Unit
    ) = runApplicationTest {
        testScope = this
        val window = JFrame()
        try {
            composePanel = ComposePanel()
            composePanel.setContent {
                content()
            }
            composePanel.isVisible = visible

            window.contentPane.add(composePanel, BorderLayout.CENTER)
            window.pack()
            window.isVisible = true

            awaitIdle()
            test()
        } finally {
            window.dispose()
        }
    }

    override suspend fun awaitIdle() = testScope.awaitIdle()
}

private fun Collection<SemanticsOwner>.collectText(): List<AnnotatedString> {
    val result = mutableListOf<AnnotatedString>()
    forEach {
        it.rootSemanticsNode.collectTextRecursive(result)
    }
    return result
}

private fun SemanticsNode.collectTextRecursive(result: MutableList<AnnotatedString>) {
    result.addAll(config.getOrNull(SemanticsProperties.Text) ?: emptyList())
    config.getOrNull(SemanticsProperties.EditableText)?.let {
        result.add(it)
    }
    for (child in children) {
        child.collectTextRecursive(result)
    }
}
