/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.ripple

import androidx.annotation.CheckResult
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.toRect
import androidx.ui.graphics.Color
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.composer
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Draw
import androidx.ui.core.OnPositioned

/**
 * An interface for creating [RippleEffect]s on a [RippleSurface].
 */
interface RippleSurfaceOwner {

    /**
     * The surface background color.
     */
    val backgroundColor: Color?

    /**
     * The surface layout coordinates.
     */
    val layoutCoordinates: LayoutCoordinates

    /**
     * Add an [RippleEffect].
     *
     * The effect will be drawns as part of this [RippleSurface].
     */
    fun addEffect(feature: RippleEffect)

    /**
     * Removes added previously effects. Called by [RippleEffect] in onDispose()
     */
    fun removeEffect(feature: RippleEffect)

    /** Notifies the [RippleSurface] that one of its effects needs to redraw. */
    fun markNeedsRedraw()
}

/**
 * An ambient to provide the current [RippleSurface].
 *
 * Use [ambientRippleSurface] to receive the value.
 */
internal val CurrentRippleSurface = Ambient.of<RippleSurfaceOwner> {
    throw IllegalStateException("No RippleSurface ancestor found.")
}

/**
 * [ambient] to get a current [RippleSurfaceOwner] object from the [RippleSurface] ancestor.
 */
@CheckResult(suggest = "+")
fun ambientRippleSurface() =
    effectOf<RippleSurfaceOwner> { +ambient(CurrentRippleSurface) }

/**
 * A surface used to draw [RippleEffect]s on top of it.
 */
@Composable
fun RippleSurface(
    /**
     * The surface background backgroundColor.
     */
    color: Color?,
    @Children children: @Composable() () -> Unit
) {
    val owner = +memo { RippleSurfaceOwnerImpl() }
    owner.backgroundColor = color

    OnPositioned(onPositioned = { owner._layoutCoordinates = it })
    Draw { canvas, size ->
        // TODO(Andrey) Find a better way to disable ripples when transitions are disabled.
        val transitionsEnabled = transitionsEnabled
        if (owner.effects.isNotEmpty() && transitionsEnabled) {
            canvas.save()
            canvas.clipRect(size.toRect())
            owner.effects.forEach { it.draw(canvas) }
            canvas.restore()
        }
        owner.recomposeModel.registerForRecomposition()
    }
    CurrentRippleSurface.Provider(value = owner, children = children)
}

private class RippleSurfaceOwnerImpl : RippleSurfaceOwner {

    override var backgroundColor: Color? = null
    override val layoutCoordinates
        get() = _layoutCoordinates
            ?: throw IllegalStateException("The surface wasn't yet positioned!")
    internal var recomposeModel = RecomposeModel()

    internal var _layoutCoordinates: LayoutCoordinates? = null
    internal var effects = mutableListOf<RippleEffect>()

    override fun markNeedsRedraw() {
        recomposeModel.recompose()
    }

    override fun addEffect(feature: RippleEffect) {
        assert(!feature.debugDisposed)
        assert(feature.rippleSurface == this)
        assert(!effects.contains(feature))
        effects.add(feature)
        markNeedsRedraw()
    }

    override fun removeEffect(feature: RippleEffect) {
        effects.remove(feature)
        markNeedsRedraw()
    }
}

// TODO(Andrey: Temporary workaround for the ripple invalidation)
@Model
private class RecomposeModel {

    private var ticker = 0

    fun recompose() {
        ticker++
    }

    fun registerForRecomposition() {
        @Suppress("UNUSED_VARIABLE")
        val ticker = ticker
    }
}
