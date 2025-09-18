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
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface Alignment {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Horizontal {
        public fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal

        public fun toRemoteCompose(): Int
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public interface Vertical {
        public fun toComposeUi(): androidx.compose.ui.Alignment.Vertical

        public fun toRemoteCompose(): Int
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public companion object {
        public val Start: Alignment.Horizontal = HorizontalAlignment(0)
        public val CenterHorizontally: Alignment.Horizontal = HorizontalAlignment(1)
        public val End: Alignment.Horizontal = HorizontalAlignment(2)
        public val Top: Alignment.Vertical = VerticalAlignment(3)
        public val CenterVertically: Alignment.Vertical = VerticalAlignment(4)
        public val Bottom: Alignment.Vertical = VerticalAlignment(5)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class HorizontalAlignment(var type: Int) : Alignment.Horizontal {
    override fun toComposeUi(): androidx.compose.ui.Alignment.Horizontal {
        when (type) {
            0 -> return androidx.compose.ui.Alignment.Start
            1 -> return androidx.compose.ui.Alignment.CenterHorizontally
            2 -> return androidx.compose.ui.Alignment.End
        }
        return androidx.compose.ui.Alignment.Start
    }

    override fun toRemoteCompose(): Int {
        when (type) {
            0 -> return ColumnLayout.START
            1 -> return ColumnLayout.CENTER
            2 -> return ColumnLayout.END
        }
        return ColumnLayout.START
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class VerticalAlignment(var type: Int) : Alignment.Vertical {
    override fun toComposeUi(): androidx.compose.ui.Alignment.Vertical {
        when (type) {
            3 -> return androidx.compose.ui.Alignment.Top
            4 -> return androidx.compose.ui.Alignment.CenterVertically
            5 -> return androidx.compose.ui.Alignment.Bottom
        }
        return androidx.compose.ui.Alignment.Top
    }

    override fun toRemoteCompose(): Int {
        when (type) {
            3 -> return ColumnLayout.TOP
            4 -> return ColumnLayout.CENTER
            5 -> return ColumnLayout.BOTTOM
        }
        return ColumnLayout.TOP
    }
}
