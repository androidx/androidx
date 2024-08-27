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

package androidx.compose.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import java.awt.Component
import java.awt.Container
import java.awt.Point
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.Test

class DragAndDropTest {

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testDragAndDropTarget() = runApplicationTest {
        lateinit var window: ComposeWindow

        var dragStarted = false
        var dragMoved = false
        var dragEnded = false
        var dropHappened = false

        val dragAndDropTarget = object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                dragStarted = true
            }

            override fun onMoved(event: DragAndDropEvent) {
                dragMoved = true
            }

            override fun onEnded(event: DragAndDropEvent) {
                dragEnded = true
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                dropHappened = true
                return true
            }
        }

        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                undecorated = true,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { true },
                            target = dragAndDropTarget
                        )
                )
            }
        }

        awaitIdle()

        val dropTarget = assertNotNull(window.findDropTarget())
        dropTarget.sendDragEnter()
        awaitIdle()
        assertTrue(dragStarted)

        dropTarget.sendDragOver()
        awaitIdle()
        assertTrue(dragMoved)

        dropTarget.sendDrop()
        awaitIdle()
        assertTrue(dragEnded)
        assertTrue(dropHappened)
    }

    /**
     * Tests that drag-target works in the presence of multiple compositions (ComposeScenes)
     * attached to the same root component.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun dragAndDropTargetWorksAfterShowingPopup() = runApplicationTest {
        lateinit var window: ComposeWindow

        var dropHappened = false

        val dragAndDropTarget = object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                dropHappened = true
                return true
            }
        }

        var showPopup by mutableStateOf(false)
        launchTestApplication {
            Window(
                onCloseRequest = ::exitApplication,
                undecorated = true,
                state = rememberWindowState(width = 200.dp, height = 100.dp)
            ) {
                window = this.window

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .dragAndDropTarget(
                            shouldStartDragAndDrop = { true },
                            target = dragAndDropTarget
                        )
                )

                if (showPopup) {
                    Popup(
                        alignment = Alignment.BottomEnd,
                    ) {
                        Box(Modifier.size(10.dp))
                    }
                }
            }
        }

        awaitIdle()
        showPopup = true
        awaitIdle()

        val dropTarget = assertNotNull(window.findDropTarget())
        dropTarget.sendDragEnter()
        dropTarget.sendDragOver()
        dropTarget.sendDrop()
        awaitIdle()
        assertTrue(dropHappened)
    }

}

private fun Component.findDropTarget(): DropTarget? {
    dropTarget?.let { return it }
    if (this is Container) {
        for (child in components) {
            child.findDropTarget()?.let { return it }
        }
    }
    return null
}

private fun DropTarget.sendDragEnter(
    mouseLocation: Point = Point(10, 10),
    dropAction: Int = DnDConstants.ACTION_COPY,
    srcActions: Int = DnDConstants.ACTION_COPY
) {
    dragEnter(DropTargetDragEvent(dropTargetContext, mouseLocation, dropAction, srcActions))
}

private fun DropTarget.sendDragOver(
    mouseLocation: Point = Point(10, 10),
    dropAction: Int = DnDConstants.ACTION_COPY,
    srcActions: Int = DnDConstants.ACTION_COPY
) {
    dragOver(DropTargetDragEvent(dropTargetContext, mouseLocation, dropAction, srcActions))
}

private fun DropTarget.sendDrop(
    mouseLocation: Point = Point(10, 10),
    dropAction: Int = DnDConstants.ACTION_COPY,
    srcActions: Int = DnDConstants.ACTION_COPY
) {
    drop(DropTargetDropEvent(dropTargetContext, mouseLocation, dropAction, srcActions))
}