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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateScope

public class RemoteFloatContext internal constructor(internal val state: RemoteStateScope) {
    public fun componentWidth(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentWidthValue()
        return RemoteFloatExpression(null, { _ -> floatArrayOf(value) })
    }

    public fun componentHeight(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentHeightValue()
        return RemoteFloatExpression(null, { _ -> floatArrayOf(value) })
    }

    public fun componentCenterX(): RemoteFloat {
        val doc = state.document
        val componentWidthValue = doc.addComponentWidthValue()
        val value = doc.floatExpression(componentWidthValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(null, { _ -> floatArrayOf(value) })
    }

    public fun componentCenterY(): RemoteFloat {
        val doc = state.document
        val componentHeightValue = doc.addComponentHeightValue()
        val value = doc.floatExpression(componentHeightValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(null, { _ -> floatArrayOf(value) })
    }
}
