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

package androidx.ui.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.DesktopOwner
import androidx.compose.ui.platform.DesktopOwners
import androidx.compose.ui.platform.setContent
import org.jetbrains.skija.Canvas
import org.jetbrains.skija.Surface
import org.jetbrains.skiko.Library
import java.awt.Component

// TODO(demin): replace by androidx.compose.ui.test.TestComposeWindow when it will be
//  in :ui:ui-test module
class TestSkiaWindow(
    val width: Int,
    val height: Int
) {
    val surface: Surface
    val canvas: Canvas
    init {
        surface = Surface.makeRasterN32Premul(width, height)
        canvas = surface.canvas
    }

    companion object {
        init {
            Library.load("/", "skiko")
            // Until https://github.com/Kotlin/kotlinx.coroutines/issues/2039 is resolved
            // we have to set this property manually for coroutines to work.
            System.getProperties().setProperty("kotlinx.coroutines.fast.service.loader", "false")
        }
    }

    fun setContent(content: @Composable () -> Unit) {
        val component = object : Component() {}
        val owners = DesktopOwners(component = component, invalidate = {})
        val owner = DesktopOwner(owners)
        owner.setContent(content)
        owner.setSize(width, height)
        owner.draw(canvas)
    }
}
