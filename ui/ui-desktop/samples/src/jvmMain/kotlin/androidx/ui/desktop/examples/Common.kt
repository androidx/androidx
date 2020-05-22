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
package androidx.ui.desktop.examples

import androidx.compose.Composable
import androidx.ui.desktop.SkiaWindow
import androidx.ui.desktop.setContent

import javax.swing.WindowConstants

fun mainWith(title: String, app: @Composable () -> Unit) {
    val width = 1024
    val height = 768

    val frame = SkiaWindow(width = width, height = height)

    frame.title = title
    frame.setLocation(400, 400)
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE

    frame.setContent {
        app()
    }

    frame.setVisible(true)
}