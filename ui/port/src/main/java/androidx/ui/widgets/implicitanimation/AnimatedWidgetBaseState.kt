/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.implicitanimation

import androidx.ui.animation.Animation
import androidx.ui.animation.AnimationController
import androidx.ui.animation.Tween
import androidx.ui.animation.animations.CurvedAnimation
import androidx.ui.widgets.tickerprovider.SingleTickerProviderStateMixin

/**
 * Signature for a [Tween] factory.
 *
 * This is the type of one of the arguments of [TweenVisitor], the signature
 * used by [AnimatedWidgetBaseState.forEachTween].
 */
typealias TweenConstructor<T> = (targetValue: T) -> Tween<T>

/** Signature for callbacks passed to [AnimatedWidgetBaseState.forEachTween]. */
interface TweenVisitor {
    operator fun <T> invoke(
        tween: Tween<T>?,
        targetValue: T?,
        constructor: TweenConstructor<T>
    ): Tween<T>?
}

/**
 * A base class for widgets with implicit animations.
 *
 * Subclasses must implement the [forEachTween] method to help
 * [AnimatedWidgetBaseState] iterate through the subclasses' widget's fields
 * and animate them.
 */
abstract class AnimatedWidgetBaseState<T : ImplicitlyAnimatedWidget>(
    widget: T
) : SingleTickerProviderStateMixin<T>(widget) {

    private var controller: AnimationController? = null

    /** The animation driving this widget's implicit animations. */
    var animation: Animation<Float>? = null
    private set

    override fun initState() {
        super.initState()
        controller = AnimationController(
                duration = widget.duration,
        debugLabel = widget.toStringShort(),
        vsync = this
        ).apply {
            addListener(handleAnimationChanged)
        }
        updateCurve()
        constructTweens()
    }

    override fun didUpdateWidget(oldWidget: T) {
        super.didUpdateWidget(oldWidget)
        if (widget.curve != oldWidget.curve) {
            updateCurve()
        }
        val controller = controller!!
        controller.duration = widget.duration
        if (constructTweens()) {
            forEachTween(object : TweenVisitor {
                override operator fun <T> invoke(
                    tween: Tween<T>?,
                    targetValue: T?,
                    constructor: TweenConstructor<T>
                ): Tween<T>? {
                    updateTween(tween, targetValue)
                    return tween
                }
            })
            controller.apply {
                value = 0.0f
                forward()
            }
        }
    }

    private fun updateCurve() {
        animation = CurvedAnimation(parent = controller!!, curve = widget.curve)
    }

    override fun dispose() {
        controller!!.dispose()
        super.dispose()
    }

    private val handleAnimationChanged = {
        setState {}
    }

    internal fun <T> shouldAnimateTween(tween: Tween<T>, targetValue: T): Boolean {
        return targetValue != (tween.end ?: tween.begin)
    }

    internal fun <T> updateTween(tween: Tween<T>?, targetValue: T?) {
        if (tween == null)
            return
        tween.apply {
            begin = tween.evaluate(animation!!)
            end = targetValue
        }
    }

    private fun constructTweens(): Boolean {
        var shouldStartAnimation = false
        forEachTween(object : TweenVisitor {
            override fun <T> invoke(
                tween: Tween<T>?,
                targetValue: T?,
                constructor: TweenConstructor<T>
            ): Tween<T>? {
                var tween = tween
                if (targetValue != null) {
                    tween = tween ?: constructor(targetValue)
                    if (shouldAnimateTween(tween, targetValue)) {
                        shouldStartAnimation = true
                    }
                } else {
                    tween = null
                }
                return tween
            }
        })
        return shouldStartAnimation
    }

    /**
     * Subclasses must implement this function by running through the following
     * steps for each animatable facet in the class:
     *
     * 1. Call the visitor callback with three arguments, the first argument
     * being the current value of the Tween<T> object that represents the
     * tween (initially null), the second argument, of type T, being the value
     * on the Widget that represents the current target value of the
     * tween, and the third being a callback that takes a value T (which will
     * be the second argument to the visitor callback), and that returns an
     * Tween<T> object for the tween, configured with the given value
     * as the begin value.
     *
     * 2. Take the value returned from the callback, and store it. This is the
     * value to use as the current value the next time that the forEachTween()
     * method is called.
     */
    abstract fun forEachTween(visitor: TweenVisitor)
}