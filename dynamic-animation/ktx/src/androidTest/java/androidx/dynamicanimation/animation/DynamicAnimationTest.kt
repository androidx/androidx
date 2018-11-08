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

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@SmallTest
class DynamicAnimationTest {

    private val animObject = Any()
    private lateinit var floatPropertyCompat: FloatPropertyCompat<Any>

    /**
     * Setup [FloatPropertyCompat] before test start
     */
    @Before fun setup() {
        floatPropertyCompat = object : FloatPropertyCompat<Any>("") {

            private var value = 0f

            override fun getValue(`object`: Any?): Float {
                return value
            }

            override fun setValue(`object`: Any?, value: Float) {
                this.value = value
            }
        }
    }

    /**
     * Test extension for creating [FlingAnimation]
     */
    @Test fun testCreateFlingAnimation() {
        val flingAnimation = animObject.flingAnimationOf(floatPropertyCompat)
        assertNotNull(flingAnimation)
        assertEquals(flingAnimation.mTarget, animObject)
        assertEquals(flingAnimation.mProperty, floatPropertyCompat)
        assertTrue(flingAnimation.friction == 1f)
        flingAnimation.friction = 1.5f
        assertTrue(flingAnimation.friction == 1.5f)
    }

    /**
     * Test extension for creating [SpringAnimation]
     */
    @Test fun testCreateSpringAnimation() {
        val springAnimationWithoutFinalPosition = animObject.springAnimationOf(floatPropertyCompat)
        assertNotNull(springAnimationWithoutFinalPosition)
        assertEquals(springAnimationWithoutFinalPosition.mTarget, animObject)
        assertEquals(springAnimationWithoutFinalPosition.mProperty, floatPropertyCompat)
        assertNull(springAnimationWithoutFinalPosition.spring)
        val springAnimationWithFinalPosition = animObject.springAnimationOf(floatPropertyCompat,
            100f)
        assertNotNull(springAnimationWithFinalPosition)
        assertEquals(springAnimationWithFinalPosition.mTarget, animObject)
        assertEquals(springAnimationWithFinalPosition.mProperty, floatPropertyCompat)
        assertNotNull(springAnimationWithFinalPosition.spring)
        assertEquals(springAnimationWithFinalPosition.spring.finalPosition, 100f)
    }

    /**
     * Test extension for setting up [SpringForce]
     */
    @Test fun testSpringForce() {
        val springAnimationWithoutFinalPosition = animObject
            .springAnimationOf(floatPropertyCompat)
            .withSpringForceProperties {
                finalPosition = 100f
                dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                stiffness = SpringForce.STIFFNESS_HIGH
            }
        assertNotNull(springAnimationWithoutFinalPosition.spring)
        assertEquals(springAnimationWithoutFinalPosition.spring.finalPosition, 100f)
        assertEquals(springAnimationWithoutFinalPosition.spring.dampingRatio,
            SpringForce.DAMPING_RATIO_HIGH_BOUNCY)
        assertEquals(springAnimationWithoutFinalPosition.spring.stiffness,
            SpringForce.STIFFNESS_HIGH)
        val springAnimationWithFinalPosition = animObject
            .springAnimationOf(floatPropertyCompat, 120f)
            .withSpringForceProperties {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        assertNotNull(springAnimationWithFinalPosition.spring)
        assertEquals(springAnimationWithFinalPosition.spring.finalPosition, 120f)
        assertEquals(springAnimationWithFinalPosition.spring.dampingRatio,
            SpringForce.DAMPING_RATIO_LOW_BOUNCY)
        assertEquals(springAnimationWithFinalPosition.spring.stiffness,
            SpringForce.STIFFNESS_LOW)
    }
}