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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.glance.layout.Alignment
import androidx.glance.layout.Alignment.Companion.Bottom
import androidx.glance.layout.Alignment.Companion.CenterHorizontally
import androidx.glance.layout.Alignment.Companion.CenterVertically
import androidx.glance.layout.Alignment.Companion.End
import androidx.glance.layout.Alignment.Companion.Start
import androidx.glance.layout.Alignment.Companion.Top

internal fun Alignment.Horizontal.toBoxLayoutEnum(): Int {
    return if (this == Start) {
        BoxLayout.START
    } else if (this == CenterHorizontally) {
        BoxLayout.CENTER
    } else if (this == End) {
        BoxLayout.END
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}

internal fun Alignment.Vertical.toBoxLayoutEnum(): Int {
    return if (this == Top) {
        BoxLayout.TOP
    } else if (this == CenterVertically) {
        BoxLayout.CENTER
    } else if (this == Bottom) {
        BoxLayout.BOTTOM
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}

internal fun Alignment.Horizontal.toRowLayoutEnum(): Int {
    return if (this == Start) {
        RowLayout.START
    } else if (this == CenterHorizontally) {
        RowLayout.CENTER
    } else if (this == End) {
        RowLayout.END
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}

internal fun Alignment.Vertical.toRowLayoutEnum(): Int {
    return if (this == Top) {
        RowLayout.TOP
    } else if (this == CenterVertically) {
        RowLayout.CENTER
    } else if (this == Bottom) {
        RowLayout.BOTTOM
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}

internal fun Alignment.Horizontal.toColumnLayoutEnum(): Int {
    return if (this == Start) {
        ColumnLayout.START
    } else if (this == CenterHorizontally) {
        ColumnLayout.CENTER
    } else if (this == End) {
        ColumnLayout.END
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}

internal fun Alignment.Vertical.toColumnLayoutEnum(): Int {
    return if (this == Top) {
        ColumnLayout.TOP
    } else if (this == CenterVertically) {
        ColumnLayout.CENTER
    } else if (this == Bottom) {
        ColumnLayout.BOTTOM
    } else {
        throw IllegalStateException("Unexpected alignment: $this")
    }
}
