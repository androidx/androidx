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

package androidx.dynamicanimation.animation

/**
 * Creates [FlingAnimation] for a property that can be accessed via the provided setter and getter.
 * For example, the following sample code creates a [FlingAnimation] for the alpha property of a
 * [View] object named `view`:
 * `flingAnimationOf(view::setAlpha, view::getAlpha)`
 *
 * @param setter The function that mutates the property being animated
 * @param getter The function that returns the value of the property
 * @return [FlingAnimation]
 */
public fun flingAnimationOf(setter: (Float) -> Unit, getter: () -> Float): FlingAnimation {
    return FlingAnimation(createFloatValueHolder(setter, getter))
}

/**
 * Creates [SpringAnimation] for a property that can be accessed via the provided setter and getter.
 * If finalPosition is not [Float.NaN] then create [SpringAnimation] with
 * [SpringForce.mFinalPosition].
 *
 * @param setter The function that mutates the property being animated
 * @param getter The function that returns the value of the property
 * @param finalPosition [SpringForce.mFinalPosition] Final position of spring.
 * @return [SpringAnimation]
 */
public fun springAnimationOf(
    setter: (Float) -> Unit,
    getter: () -> Float,
    finalPosition: Float = Float.NaN
): SpringAnimation {
    val valueHolder = createFloatValueHolder(setter, getter)
    return if (finalPosition.isNaN()) {
        SpringAnimation(valueHolder)
    } else {
        SpringAnimation(valueHolder, finalPosition)
    }
}

/**
 * Updates or applies spring force properties like [SpringForce.mDampingRatio],
 * [SpringForce.mFinalPosition] and stiffness on SpringAnimation.
 *
 * If [SpringAnimation.mSpring] is null in case [SpringAnimation] is created without final position
 * it will be created and attached to [SpringAnimation]
 *
 * @param func lambda with receiver on [SpringForce]
 * @return [SpringAnimation]
 */
public inline fun SpringAnimation.withSpringForceProperties(
    func: SpringForce.() -> Unit
): SpringAnimation {
    if (spring == null) {
        spring = SpringForce()
    }
    spring.func()
    return this
}

private fun createFloatValueHolder(setter: (Float) -> Unit, getter: () -> Float): FloatValueHolder {
    return object : FloatValueHolder() {
        override fun getValue(): Float {
            return getter.invoke()
        }

        override fun setValue(value: Float) {
            setter.invoke(value)
        }
    }
}