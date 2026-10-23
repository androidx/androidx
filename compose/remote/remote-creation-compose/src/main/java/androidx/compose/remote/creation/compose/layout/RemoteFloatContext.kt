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
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateScope

public class RemoteFloatContext internal constructor(internal val state: RemoteStateScope) {
    public fun componentWidth(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentWidthValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "width"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentHeight(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentHeightValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "height"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentCenterX(): RemoteFloat {
        val doc = state.document
        val componentWidthValue = doc.addComponentWidthValue()
        val value = doc.floatExpression(componentWidthValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "centerX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentCenterY(): RemoteFloat {
        val doc = state.document
        val componentHeightValue = doc.addComponentHeightValue()
        val value = doc.floatExpression(componentHeightValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "centerY"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentX(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentXValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "x"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentY(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentYValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "y"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentRootX(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentRootXValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "rootX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentRootY(): RemoteFloat {
        val doc = state.document
        val value = doc.addComponentRootYValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "rootY"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }
}
