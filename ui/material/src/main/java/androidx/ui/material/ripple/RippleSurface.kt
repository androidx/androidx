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

import androidx.ui.core.adapter.DensityConsumer
import androidx.ui.core.adapter.Draw
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import com.google.r4a.Ambient
import com.google.r4a.Children
import com.google.r4a.Component
import com.google.r4a.Composable
import com.google.r4a.composer


/**
 * An interface for creating [RippleEffect]s on a [RippleSurface].
 */
interface RippleSurfaceOwner {

    /**
     * The surface background color.
     */
    val backgroundColor: Color?

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
 * Use [RippleSurfaceConsumer] to receive the value.
 */
internal val CurrentRippleSurface = Ambient.of<RippleSurfaceOwner> {
    throw IllegalStateException("No RippleSurface ancestor found.")
}

/**
 * Provides a current [RippleSurfaceOwner] from the [RippleSurface] ancestor.
 */
@Composable
fun RippleSurfaceConsumer(@Children children: (surface: RippleSurfaceOwner) -> Unit) {
    <CurrentRippleSurface.Consumer> surface ->
        <children surface />
    </CurrentRippleSurface.Consumer>
}

/**
 * A surface used to draw [RippleEffect]s on top of it.
 */
class RippleSurface(
    /**
     * The surface background backgroundColor.
     */
    val color: Color?,
    @Children var children: () -> Unit
) : Component() {

    // TODO(Andrey: Use state effect and convert to function. b/124500412
    private lateinit var owner: RippleSurfaceOwnerImpl

    override fun compose() {
        if (!::owner.isInitialized) {
            owner = RippleSurfaceOwnerImpl(color, this::recompose)
        }
        <Draw> canvas, size ->
            if (owner.effects.isNotEmpty()) {
                canvas.save()
                canvas.clipRect(Rect(0f, 0f, size.width, size.height))
                owner.effects.forEach { it.draw(canvas) }
                canvas.restore()
            }
        </Draw>
        <CurrentRippleSurface.Provider value=owner>
            <children />
        </CurrentRippleSurface.Provider>
    }

}

private class RippleSurfaceOwnerImpl(
    override val backgroundColor: Color?,
    private val recompose: () -> Unit
) : RippleSurfaceOwner {

    internal var effects = mutableListOf<RippleEffect>()

    override fun markNeedsRedraw() = recompose()

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
