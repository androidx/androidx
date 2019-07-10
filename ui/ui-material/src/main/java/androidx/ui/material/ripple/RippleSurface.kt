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
import androidx.compose.Ambient
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.Recompose
import androidx.compose.ambient
import androidx.compose.composer
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.animation.transitionsEnabled
import androidx.ui.core.Draw
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnPositioned
import androidx.ui.graphics.Color

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
     * The [effect] will be drawn as part of this [RippleSurface].
     */
    fun addEffect(effect: RippleEffect)

    /**
     * Removes added previously effects. Called by [RippleEffect] in onDispose()
     */
    fun removeEffect(effect: RippleEffect)

    /** Notifies the [RippleSurface] that one of its effects needs to redraw. */
    fun requestRedraw()
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
 *
 * @param color The surface background color.
 */
@Composable
fun RippleSurface(
    color: Color?,
    @Children children: @Composable() () -> Unit
) {
    val owner = +memo { RippleSurfaceOwnerImpl() }
    owner.backgroundColor = color

    OnPositioned(onPositioned = { owner._layoutCoordinates = it })
    Recompose { recompose ->
        owner.recompose = recompose
        Draw { canvas, _ ->
            // TODO(Andrey) Find a better way to disable ripples when transitions are disabled.
            if (owner.effects.isNotEmpty() && transitionsEnabled) {
                owner.effects.forEach { it.draw(canvas) }
            }
        }
    }
    CurrentRippleSurface.Provider(value = owner, children = children)
}

private class RippleSurfaceOwnerImpl : RippleSurfaceOwner {

    override var backgroundColor: Color? = null
    override val layoutCoordinates
        get() = _layoutCoordinates
            ?: throw IllegalStateException("The surface wasn't yet positioned!")
    var recompose: () -> Unit = {}

    var _layoutCoordinates: LayoutCoordinates? = null
    var effects = mutableListOf<RippleEffect>()

    override fun requestRedraw() = recompose()

    override fun addEffect(effect: RippleEffect) {
        require(!effects.contains(effect))
        effects.add(effect)
        requestRedraw()
    }

    override fun removeEffect(effect: RippleEffect) {
        effects.remove(effect)
        requestRedraw()
    }
}
