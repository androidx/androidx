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
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.ui.unit.dp

interface Arrangement {

    interface Horizontal {
        fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal

        fun toRemoteCompose(): Int
    }

    interface Vertical {
        fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical

        fun toRemoteCompose(): Int
    }

    interface HorizontalOrVertical : Horizontal, Vertical {
        override fun toComposeUi():
            androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical

        override fun toRemoteCompose(): Int
    }

    companion object {
        val Top: Arrangement.Vertical = VerticalArrangement(0)
        val Center: Arrangement.Vertical = VerticalArrangement(1)
        val Bottom: Arrangement.Vertical = VerticalArrangement(2)
        val Start: Arrangement.Horizontal = HorizontalArrangement(3)
        val CenterHorizontally: Arrangement.Horizontal = HorizontalArrangement(4)
        val End: Arrangement.Horizontal = HorizontalArrangement(5)
        val SpaceBetween: Arrangement.HorizontalOrVertical = HorizontalOrVerticalArrangement(6)
        val SpaceEvenly: Arrangement.HorizontalOrVertical = HorizontalOrVerticalArrangement(7)
        val SpaceAround: Arrangement.HorizontalOrVertical = HorizontalOrVerticalArrangement(8)
    }
}

data class VerticalArrangement(var type: Int) : Arrangement.Vertical {
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Vertical {
        when (type) {
            0 -> return androidx.compose.foundation.layout.Arrangement.Top
            1 -> return androidx.compose.foundation.layout.Arrangement.Center
            2 -> return androidx.compose.foundation.layout.Arrangement.Bottom
        }
        return androidx.compose.foundation.layout.Arrangement.Top
    }

    override fun toRemoteCompose(): Int {
        when (type) {
            0 -> return ColumnLayout.TOP
            1 -> return ColumnLayout.CENTER
            2 -> return ColumnLayout.BOTTOM
        }
        return ColumnLayout.TOP
    }
}

data class HorizontalOrVerticalArrangement(var type: Int) : Arrangement.HorizontalOrVertical {
    override fun toComposeUi():
        androidx.compose.foundation.layout.Arrangement.HorizontalOrVertical {
        when (type) {
            6 -> return androidx.compose.foundation.layout.Arrangement.SpaceBetween
            7 -> return androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            8 -> return androidx.compose.foundation.layout.Arrangement.SpaceAround
        }
        return androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp)
    }

    override fun toRemoteCompose(): Int {
        when (type) {
            6 -> return ColumnLayout.SPACE_BETWEEN
            7 -> return ColumnLayout.SPACE_EVENLY
            8 -> return ColumnLayout.SPACE_AROUND
        }
        return ColumnLayout.START
    }
}

data class HorizontalArrangement(var type: Int) : Arrangement.Horizontal {
    override fun toComposeUi(): androidx.compose.foundation.layout.Arrangement.Horizontal {
        when (type) {
            3 -> return androidx.compose.foundation.layout.Arrangement.Start
            4 -> return androidx.compose.foundation.layout.Arrangement.Center
            5 -> return androidx.compose.foundation.layout.Arrangement.End
            6 -> return androidx.compose.foundation.layout.Arrangement.SpaceBetween
            7 -> return androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            8 -> return androidx.compose.foundation.layout.Arrangement.SpaceAround
        }
        return androidx.compose.foundation.layout.Arrangement.Start
    }

    override fun toRemoteCompose(): Int {
        when (type) {
            3 -> return ColumnLayout.START
            4 -> return ColumnLayout.CENTER
            5 -> return ColumnLayout.END
            6 -> return ColumnLayout.SPACE_BETWEEN
            7 -> return ColumnLayout.SPACE_EVENLY
            8 -> return ColumnLayout.SPACE_AROUND
        }
        return ColumnLayout.START
    }
}
