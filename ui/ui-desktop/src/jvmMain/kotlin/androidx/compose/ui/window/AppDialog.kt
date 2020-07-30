/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.onActive
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.ui.desktop.AppFrame
import androidx.ui.desktop.AppManager
import androidx.ui.desktop.AppWindowAmbient
import androidx.ui.desktop.Dialog
import androidx.ui.desktop.setContent
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.WindowConstants

@Composable
fun Dialog(
    title: String = "JetpackDesktopDialog",
    size: IntSize = IntSize(1024, 768),
    position: IntOffset = IntOffset(0, 0),
    isCentered: Boolean = true,
    onDismissEvent: (() -> Unit)? = null,
    content: @Composable () -> Unit = emptyContent()
) {
    val attached = AppWindowAmbient.current?.window
    val dialog = remember {
        AppDialog(
            attached = attached,
            title = title,
            size = size,
            position = position,
            onDismissEvent = onDismissEvent,
            centered = isCentered
        )
    }

    onActive {
        dialog.show {
            content()
        }
    }

    onDispose {
        dialog.close()
    }
}

class AppDialog : AppFrame {

    constructor(
        attached: JFrame? = null,
        title: String = "JetpackDesktopDialog",
        size: IntSize = IntSize(1024, 768),
        position: IntOffset = IntOffset(0, 0),
        onDismissEvent: (() -> Unit)? = null,
        centered: Boolean = true
    ) {
        this.attached = attached
        this.title = title
        this.width = size.width
        this.height = size.height
        this.x = position.x
        this.y = position.y
        if (onDismissEvent != null) {
            onDismissEvents.add(onDismissEvent)
        }
        isCentered = centered

        AppManager.addWindow(this)
    }

    var attached: JFrame? = null
        private set

    override fun setSize(width: Int, height: Int) {
        // better check min/max values of current window size
        var w = width
        if (w <= 0) {
            w = this.width
        }

        var h = height
        if (h <= 0) {
            h = this.height
        }
        this.width = w
        this.height = h
        window?.setSize(w, h)
    }

    override fun setPosition(x: Int, y: Int) {
        this.x = x
        this.y = y
        window?.setLocation(x, y)
    }

    override fun setWindowCentered() {
        val dim: Dimension = Toolkit.getDefaultToolkit().getScreenSize()
        this.x = dim.width / 2 - width / 2
        this.y = dim.height / 2 - height / 2
        window?.setLocation(x, y)
    }

    var window: Dialog? = null
        private set

    private fun onCreate(content: @Composable () -> Unit) {

        window = Dialog(attached, width = width, height = height, parent = this)

        window!!.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE

        window!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(windowevent: WindowEvent) {
                onDismissEvents.forEach { it.invoke() }
            }
        })

        window!!.title = title

        window!!.setContent {
            content()
        }

        if (isCentered)
            setWindowCentered()
        window!!.setVisible(true)
    }

    override fun show(content: @Composable () -> Unit) {
        onCreate {
            content()
        }
    }

    override fun close() {
        window?.dispose()
        AppManager.removeWindow(this)
    }
}
