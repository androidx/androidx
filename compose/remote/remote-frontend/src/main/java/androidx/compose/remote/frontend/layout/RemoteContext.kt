/*
 * Copyright (C) 2024 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.frontend.modifier.RemoteModifier
import androidx.compose.runtime.Composable

val createIds = CreateIds()

class CreateIds {
    var IdIndices = 0

    operator fun component1(): Int {
        return IdIndices++
    }

    operator fun component2(): Int {
        return IdIndices++
    }

    operator fun component3(): Int {
        return IdIndices++
    }

    operator fun component4(): Int {
        return IdIndices++
    }

    operator fun component5(): Int {
        return IdIndices++
    }

    operator fun component6(): Int {
        return IdIndices++
    }

    operator fun component7(): Int {
        return IdIndices++
    }

    operator fun component8(): Int {
        return IdIndices++
    }
}

/**
 * Utility class to be able to call Remote layout functions with the same name as the foundation
 * ones, even if the current file has import of the foundation ones.
 */
class RemoteContext {

    val Modifier: RemoteModifier
        get() = RemoteModifier

    @RemoteComposable
    @Composable
    fun Row(
        modifier: RemoteModifier = RemoteModifier,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
        verticalAlignment: Alignment.Vertical = Alignment.Top,
        content: @Composable RemoteRowScope.() -> Unit,
    ) {
        RemoteRow(modifier, horizontalArrangement, verticalAlignment, content)
    }

    @RemoteComposable
    @Composable
    fun Column(
        modifier: RemoteModifier = RemoteModifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: @Composable RemoteColumnScope.() -> Unit,
    ) {
        RemoteColumn(modifier, verticalArrangement, horizontalAlignment, content)
    }

    @RemoteComposable
    @Composable
    fun Box(
        modifier: RemoteModifier = RemoteModifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: @Composable () -> Unit,
    ) {
        RemoteBox(modifier, horizontalAlignment, verticalArrangement, content)
    }

    @RemoteComposable
    @Composable
    fun Box(modifier: RemoteModifier = RemoteModifier) {
        RemoteBox(modifier)
    }

    @RemoteComposable
    @Composable
    fun Canvas(
        modifier: RemoteModifier = RemoteModifier,
        content: RemoteCanvasDrawScope.() -> Unit,
    ) {
        RemoteCanvas(modifier, content)
    }
}

/**
 * Utility function to create a RemoteContext scope within which you can call
 * Row/Column/Box/Canvas/etc.
 */
@Composable
fun RemoteContext(content: @Composable RemoteContext.() -> Unit) {
    RemoteContext().content()
}
