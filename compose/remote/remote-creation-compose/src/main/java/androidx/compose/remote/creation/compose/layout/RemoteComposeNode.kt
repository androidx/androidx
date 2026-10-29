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
import androidx.compose.remote.creation.compose.capture.LocalRemoteDensity
import androidx.compose.remote.creation.compose.capture.LocalRemoteLayoutDirection
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.modifier.DrawWithContentModifier
import androidx.compose.remote.creation.compose.modifier.PaddingModifier
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.resolveBottom
import androidx.compose.remote.creation.compose.modifier.resolveLeft
import androidx.compose.remote.creation.compose.modifier.resolveRight
import androidx.compose.remote.creation.compose.modifier.resolveTop
import androidx.compose.remote.creation.compose.state.PlatformRemoteStateScope
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.fastForEach
import androidx.compose.remote.creation.compose.util.fastForEachReversed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Updater

internal abstract class RemoteComposeNode {
    val children = mutableListOf<RemoteComposeNode>()
    var modifier: RemoteModifier = RemoteModifier
    var remoteDensity: RemoteDensity = RemoteDensity.Host

    fun overriddenScope(creationState: RemoteComposeCreationState): RemoteStateScope {
        return OverriddenScope(creationState, remoteDensity, creationState.remoteLayoutDirection)
    }

    abstract fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas)

    private class DrawWithContentEntry(
        val modifier: DrawWithContentModifier,
        val paddingLeft: RemoteFloat?,
        val paddingTop: RemoteFloat?,
        val paddingRight: RemoteFloat?,
        val paddingBottom: RemoteFloat?,
        val stepPaddingLeft: RemoteFloat?,
        val stepPaddingTop: RemoteFloat?,
    )

    fun renderChildren(
        creationState: RemoteComposeCreationState,
        remoteCanvas: RemoteCanvas,
        reversed: Boolean = false,
    ) {
        val scope = overriddenScope(creationState)
        val drawEntries = mutableListOf<DrawWithContentEntry>()
        var paddingLeft: RemoteFloat? = null
        var paddingTop: RemoteFloat? = null
        var paddingRight: RemoteFloat? = null
        var paddingBottom: RemoteFloat? = null
        var stepPaddingLeft: RemoteFloat? = null
        var stepPaddingTop: RemoteFloat? = null

        modifier.foldIn<Unit>(Unit) { _, element ->
            if (element is DrawWithContentModifier) {
                drawEntries.add(
                    DrawWithContentEntry(
                        modifier = element,
                        paddingLeft = paddingLeft,
                        paddingTop = paddingTop,
                        paddingRight = paddingRight,
                        paddingBottom = paddingBottom,
                        stepPaddingLeft = stepPaddingLeft,
                        stepPaddingTop = stepPaddingTop,
                    )
                )
                stepPaddingLeft = null
                stepPaddingTop = null
            } else if (element is PaddingModifier) {
                val left = element.resolveLeft(scope)
                val top = element.resolveTop(scope)
                val right = element.resolveRight(scope)
                val bottom = element.resolveBottom(scope)

                if (left.constantValueOrNull != 0f) {
                    paddingLeft = paddingLeft?.let { it + left } ?: left
                    stepPaddingLeft = stepPaddingLeft?.let { it + left } ?: left
                }
                if (top.constantValueOrNull != 0f) {
                    paddingTop = paddingTop?.let { it + top } ?: top
                    stepPaddingTop = stepPaddingTop?.let { it + top } ?: top
                }
                if (right.constantValueOrNull != 0f) {
                    paddingRight = paddingRight?.let { it + right } ?: right
                }
                if (bottom.constantValueOrNull != 0f) {
                    paddingBottom = paddingBottom?.let { it + bottom } ?: bottom
                }
            }
        }

        if (drawEntries.isNotEmpty()) {
            fun contentWidthProvider(
                left: RemoteFloat?,
                right: RemoteFloat?,
            ): (() -> RemoteFloat)? =
                if (left != null || right != null) {
                    {
                        val totalHorizPadding =
                            if (left != null && right != null) {
                                left + right
                            } else {
                                left ?: right!!
                            }
                        remoteCanvas.remote.component.width - totalHorizPadding
                    }
                } else {
                    null
                }

            fun contentHeightProvider(
                top: RemoteFloat?,
                bottom: RemoteFloat?,
            ): (() -> RemoteFloat)? =
                if (top != null || bottom != null) {
                    {
                        val totalVertPadding =
                            if (top != null && bottom != null) {
                                top + bottom
                            } else {
                                top ?: bottom!!
                            }
                        remoteCanvas.remote.component.height - totalVertPadding
                    }
                } else {
                    null
                }

            val lastEntry = drawEntries.last()
            var currentScope =
                RemoteContentDrawScope(
                    remoteCanvas = remoteCanvas,
                    contentWidth =
                        contentWidthProvider(lastEntry.paddingLeft, lastEntry.paddingRight),
                    contentHeight =
                        contentHeightProvider(lastEntry.paddingTop, lastEntry.paddingBottom),
                    paddingLeft = lastEntry.paddingLeft,
                    paddingTop = lastEntry.paddingTop,
                )

            for (i in drawEntries.lastIndex - 1 downTo 0) {
                val entry = drawEntries[i]
                val nextEntry = drawEntries[i + 1]
                val nextScope = currentScope
                val deltaLeft = nextEntry.stepPaddingLeft
                val deltaTop = nextEntry.stepPaddingTop
                val hasDelta = deltaLeft != null || deltaTop != null
                currentScope =
                    RemoteContentDrawScope(
                        remoteCanvas = remoteCanvas,
                        content = {
                            if (hasDelta) {
                                val dx = deltaLeft ?: 0f.rf
                                val dy = deltaTop ?: 0f.rf
                                remoteCanvas.translate(dx, dy)
                                nextEntry.modifier.onDraw(nextScope)
                                remoteCanvas.translate(-dx, -dy)
                            } else {
                                nextEntry.modifier.onDraw(nextScope)
                            }
                        },
                        contentWidth = contentWidthProvider(entry.paddingLeft, entry.paddingRight),
                        contentHeight =
                            contentHeightProvider(entry.paddingTop, entry.paddingBottom),
                    )
            }

            val firstEntry = drawEntries[0]
            val hasOffset = firstEntry.paddingLeft != null || firstEntry.paddingTop != null

            creationState.document.startCanvasOperations()
            if (hasOffset) {
                remoteCanvas.save()
                remoteCanvas.translate(
                    firstEntry.paddingLeft ?: 0f.rf,
                    firstEntry.paddingTop ?: 0f.rf,
                )
            }
            firstEntry.modifier.onDraw(currentScope)
            if (hasOffset) {
                remoteCanvas.restore()
            }
            remoteCanvas.flush()
            creationState.document.endCanvasOperations()
        }

        if (!reversed) {
            children.fastForEach { it.render(creationState, remoteCanvas) }
        } else {
            children.fastForEachReversed { it.render(creationState, remoteCanvas) }
        }
    }
}

internal class RemoteRootNode : RemoteComposeNode() {
    override fun render(creationState: RemoteComposeCreationState, remoteCanvas: RemoteCanvas) {
        creationState.document.root { renderChildren(creationState, remoteCanvas) }
    }
}

internal abstract class RemoteLayoutNode : RemoteComposeNode() {
    var layoutDirection: RemoteLayoutDirection = RemoteLayoutDirection.Ltr
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNode> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
) {
    val remoteDensity = LocalRemoteDensity.current
    ComposeNode<T, RemoteComposeApplier>(factory) {
        update()
        set(remoteDensity) { this.remoteDensity = it }
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
internal inline fun <T : RemoteComposeNode> RemoteComposeNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val remoteDensity = LocalRemoteDensity.current
    ComposeNode<T, RemoteComposeApplier>(
        factory,
        {
            update()
            set(remoteDensity) { this.remoteDensity = it }
        },
        content,
    )
}

@Composable
internal inline fun <T : RemoteLayoutNode> RemoteLayoutNode(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit,
    content: @Composable @RemoteComposable () -> Unit,
) {
    val layoutDirection = LocalRemoteLayoutDirection.current
    RemoteComposeNode(
        factory = factory,
        update = {
            update()
            set(layoutDirection) { this.layoutDirection = it }
        },
        content = content,
    )
}

internal fun shouldReverse(
    horizontalArrangement: RemoteArrangement.Horizontal,
    layoutDirection: RemoteLayoutDirection,
): Boolean =
    if (layoutDirection == RemoteLayoutDirection.Rtl) {
        when (horizontalArrangement) {
            is HorizontalArrangement -> !horizontalArrangement.isAbsolute()

            is RemoteSpacedAbsoluteHorizontalArrangement,
            is RemoteFloatSpacedAbsoluteHorizontalArrangement -> false

            else -> true
        }
    } else {
        false
    }

internal class OverriddenScope(
    override val parentScope: RemoteStateScope,
    override val remoteDensity: RemoteDensity,
    override val remoteLayoutDirection: RemoteLayoutDirection,
) : PlatformRemoteStateScope
