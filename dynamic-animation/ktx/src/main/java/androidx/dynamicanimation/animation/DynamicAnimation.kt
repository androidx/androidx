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
 * Creates [FlingAnimation] for object.
 *
 * @param property object's property to be animated.
 * @return [FlingAnimation]
 */
inline fun <K> K.flingAnimationOf(property: FloatPropertyCompat<K>): FlingAnimation {
    return FlingAnimation(this, property)
}

/**
 * Creates [SpringAnimation] for object.
 * If finalPosition is not [Float.NaN] then create [SpringAnimation] with
 * [SpringForce.mFinalPosition].
 *
 * @param property object's property to be animated.
 * @param finalPosition [SpringForce.mFinalPosition] Final position of spring.
 * @return [SpringAnimation]
 */
inline fun <K> K.springAnimationOf(
    property: FloatPropertyCompat<K>,
    finalPosition: Float = Float.NaN
): SpringAnimation {
    return if (finalPosition.isNaN()) {
        SpringAnimation(this, property)
    } else {
        SpringAnimation(this, property, finalPosition)
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
inline fun SpringAnimation.withSpringForceProperties(
    func: SpringForce.() -> Unit
): SpringAnimation {
    if (spring == null) {
        spring = SpringForce()
    }
    spring.func()
    return this
}