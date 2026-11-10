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

import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.capture.RemoteDocumentScope
import androidx.compose.remote.creation.compose.state.RemoteComponentCacheKey
import androidx.compose.remote.creation.compose.state.RemoteFloat

/** Exposes component-relative [RemoteFloat] bounds for remote float expressions. */
internal class RemoteFloatContext
private constructor(
    internal val scope: RemoteDocumentScope?,
    internal val token: RemoteComponentToken?,
) {
    /**
     * Creates a [RemoteFloatContext] backed by [scope].
     *
     * @param scope scope providing the target [RemoteComposeWriter]
     */
    internal constructor(scope: RemoteDocumentScope) : this(scope = scope, token = null)

    /**
     * Creates a [RemoteFloatContext] bound to [token].
     *
     * @param token identity token of the target layout component
     */
    internal constructor(token: RemoteComponentToken?) : this(scope = null, token = token)

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
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(capturedToken, type),
            arrayProvider = { creationState ->
                floatArrayOf(creationState.getComponentValue(capturedToken, propIndex))
            },
        )
    }

    /** Returns the width of the current component in pixels as a [RemoteFloat]. */
    internal fun componentWidth(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_WIDTH, "width")
        }
        val doc = requireDocument("width")
        val value = doc.addComponentWidthValue()
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "width"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /** Returns the height of the current component in pixels as a [RemoteFloat]. */
    internal fun componentHeight(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_HEIGHT, "height")
        }
        val doc = requireDocument("height")
        val value = doc.addComponentHeightValue()
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "height"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /**
     * Returns the horizontal center (`width / 2`) of the current component in pixels.
     *
     * @return horizontal center offset as a [RemoteFloat]
     */
    internal fun componentCenterX(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            capturedToken.markRequested(RemoteComponentToken.PROP_WIDTH)
            return RemoteFloat(
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
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "centerX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /**
     * Returns the vertical center (`height / 2`) of the current component in pixels.
     *
     * @return vertical center offset as a [RemoteFloat]
     */
    internal fun componentCenterY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            capturedToken.markRequested(RemoteComponentToken.PROP_HEIGHT)
            return RemoteFloat(
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
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "centerY"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /** Returns the parent-relative X coordinate of the current component in pixels. */
    internal fun componentX(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_X, "x")
        }
        val doc = requireDocument("x")
        val value = doc.addComponentXValue()
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "x"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /** Returns the parent-relative Y coordinate of the current component in pixels. */
    internal fun componentY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_Y, "y")
        }
        val doc = requireDocument("y")
        val value = doc.addComponentYValue()
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "y"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /** Returns the root-relative X coordinate of the current component in pixels. */
    internal fun componentRootX(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_ROOT_X, "rootX")
        }
        val doc = requireDocument("rootX")
        val value = doc.addComponentRootXValue()
        return RemoteFloat(
            constantValueOrNull = null,
            cacheKey = RemoteComponentCacheKey(doc.buffer.lastComponentId, "rootX"),
            arrayProvider = { _ -> floatArrayOf(value) },
        )
    }

    /** Returns the root-relative Y coordinate of the current component in pixels. */
    internal fun componentRootY(): RemoteFloat {
        val capturedToken = activeToken()
        if (capturedToken != null) {
            return tokenComponentFloat(capturedToken, RemoteComponentToken.PROP_ROOT_Y, "rootY")
        }
        val doc = requireDocument("rootY")
        val value = doc.addComponentRootYValue()
        return RemoteFloat(
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
