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
import androidx.compose.remote.creation.compose.capture.RemoteDocumentScope
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression

/** Receiver scope providing access to dynamic layout measurements of the active component. */
public class RemoteFloatContext
private constructor(
    internal val scope: RemoteDocumentScope?,
    internal val token: RemoteComponentToken?,
) {
    /** Creates a [RemoteFloatContext] backed by [scope]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(scope: RemoteDocumentScope) : this(scope = scope, token = null)

    /** Creates a [RemoteFloatContext] bound to [token]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(token: RemoteComponentToken?) : this(scope = null, token = token)

    internal val document: RemoteComposeWriter
        get() = requireDocument("unknown")

    private fun activeToken(): RemoteComponentToken? = token ?: scope?.resolveComponentToken()

    private fun requireDocument(type: String): RemoteComposeWriter =
        checkNotNull(scope) {
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

    /** Returns the width of the active component in pixels. */
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

    /** Returns the height of the active component in pixels. */
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

    /** Returns the horizontal center (`width / 2`) of the active component in pixels. */
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

    /** Returns the vertical center (`height / 2`) of the active component in pixels. */
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

    /** Returns the horizontal offset of the active component relative to its parent in pixels. */
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

    /** Returns the vertical offset of the active component relative to its parent in pixels. */
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

    /** Returns the horizontal offset of the active component relative to the root in pixels. */
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

    /** Returns the vertical offset of the active component relative to the root in pixels. */
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
        return scope == other.scope && token == other.token
    }

    override fun hashCode(): Int {
        var result = scope?.hashCode() ?: 0
        result = 31 * result + (token?.hashCode() ?: 0)
        return result
    }
}
