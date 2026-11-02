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
package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateScope

public class RemoteFloatContext
private constructor(
    internal val state: RemoteStateScope?,
    internal val token: RemoteComponentToken?,
) {
    internal constructor(state: RemoteStateScope) : this(state = state, token = null)

    internal constructor(token: RemoteComponentToken?) : this(state = null, token = token)

    private fun activeToken(): RemoteComponentToken? = token ?: state?.resolveComponentToken()

    private fun requireDocument(type: String): RemoteComposeWriter =
        checkNotNull(state) {
                "Component value '$type' requires an enclosing RemoteCompose layout component " +
                    "or RemoteStateScope."
            }
            .document

    private fun tokenComponentFloat(
        capturedToken: RemoteComponentToken,
        propIndex: Int,
        type: String,
    ): RemoteFloat {
        capturedToken.markRequested(propIndex)
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(capturedToken, type),
            arrayProvider = { creationState ->
                floatArrayOf(creationState.getComponentValue(capturedToken, propIndex))
            },
        )
    }

    public fun componentWidth(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_WIDTH, "width")
        }
        val doc = requireDocument("width")
        val value = doc.addComponentWidthValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "width"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentHeight(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_HEIGHT, "height")
        }
        val doc = requireDocument("height")
        val value = doc.addComponentHeightValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "height"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentCenterX(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            capturedToken.markRequested(RemoteComponentToken.PROP_WIDTH)
            return RemoteFloatExpression(
                constantValueOrNull = null,
                cacheKey = RemoteComponentCacheKey(capturedToken, "centerX"),
                arrayProvider = { creationState ->
                    val componentWidthValue =
                        creationState.getComponentValue(
                            capturedToken,
                            RemoteComponentToken.PROP_WIDTH,
                        )
                    val value =
                        creationState.document.floatExpression(
                            componentWidthValue,
                            2f,
                            AnimatedFloatExpression.DIV,
                        )
                    floatArrayOf(value)
                },
            )
        }
        val doc = requireDocument("centerX")
        val componentWidthValue = doc.addComponentWidthValue()
        val value = doc.floatExpression(componentWidthValue, 2f, AnimatedFloatExpression.DIV)
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "centerX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    public fun componentCenterY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            capturedToken.markRequested(RemoteComponentToken.PROP_HEIGHT)
            return RemoteFloatExpression(
                constantValueOrNull = null,
                cacheKey = RemoteComponentCacheKey(capturedToken, "centerY"),
                arrayProvider = { creationState ->
                    val componentHeightValue =
                        creationState.getComponentValue(
                            capturedToken,
                            RemoteComponentToken.PROP_HEIGHT,
                        )
                    val value =
                        creationState.document.floatExpression(
                            componentHeightValue,
                            2f,
                            AnimatedFloatExpression.DIV,
                        )
                    floatArrayOf(value)
                },
            )
        }
        val doc = requireDocument("centerY")
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
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_X, "x")
        }
        val doc = requireDocument("x")
        val value = doc.addComponentXValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "x"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_Y, "y")
        }
        val doc = requireDocument("y")
        val value = doc.addComponentYValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "y"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentRootX(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_ROOT_X, "rootX")
        }
        val doc = requireDocument("rootX")
        val value = doc.addComponentRootXValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "rootX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun componentRootY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_ROOT_Y, "rootY")
        }
        val doc = requireDocument("rootY")
        val value = doc.addComponentRootYValue()
        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "rootY"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteFloatContext) return false
        return state == other.state && token == other.token
    }

    override fun hashCode(): Int {
        var result = state?.hashCode() ?: 0
        result = 31 * result + (token?.hashCode() ?: 0)
        return result
    }
}
