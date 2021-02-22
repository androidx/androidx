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

package androidx.dynamicanimation.tests

import android.view.View

import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.flingAnimationOf
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import androidx.dynamicanimation.ktx.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify

@MediumTest
@RunWith(AndroidJUnit4::class)
class DynamicAnimationTest {

    @Suppress("DEPRECATION")
    @get:Rule val activityTestRule = androidx.test.rule.ActivityTestRule<AnimationActivity>(
        AnimationActivity::class.java
    )

    private lateinit var view: View

    @Before fun setup() {
        view = activityTestRule.getActivity().findViewById(R.id.anim_view)
    }

    /**
     * Test extension for creating [FlingAnimation]
     */
    @Test fun testCreateFlingAnimation() {
        val flingAnimation = flingAnimationOf(view::setAlpha, view::getAlpha)
        assertNotNull(flingAnimation)
        assertTrue(flingAnimation.friction == 1f)
        flingAnimation.friction = 1.5f
        assertTrue(flingAnimation.friction == 1.5f)
    }

    /**
     * Test extension for creating [SpringAnimation]
     */
    @Test fun testCreateSpringAnimation() {
        val springAnimationWithoutFinalPosition =
            springAnimationOf(view::setScaleX, view::getScaleX)
        assertNotNull(springAnimationWithoutFinalPosition)
        assertNull(springAnimationWithoutFinalPosition.spring)
        val springAnimationWithFinalPosition =
            springAnimationOf(view::setScaleX, view::getScaleX, 100f)
        assertNotNull(springAnimationWithFinalPosition)
        assertNotNull(springAnimationWithFinalPosition.spring)
        assertEquals(springAnimationWithFinalPosition.spring.finalPosition, 100f)

        val listener: DynamicAnimation.OnAnimationEndListener = mock(
            DynamicAnimation.OnAnimationEndListener::class.java
        )
        springAnimationWithFinalPosition.addEndListener(listener)
        InstrumentationRegistry.getInstrumentation().runOnMainSync({
            springAnimationWithFinalPosition.setStartValue(0f).start()
        })
        assertTrue(springAnimationWithFinalPosition.isRunning())

        InstrumentationRegistry.getInstrumentation().runOnMainSync({
            springAnimationWithFinalPosition.skipToEnd()
        })

        verify(listener, timeout(2000)).onAnimationEnd(
            springAnimationWithFinalPosition, false,
            100f, 0f
        )

        assertEquals(100f, view.getScaleX())
    }

    /**
     * Test extension for setting up [SpringForce]
     */
    @Test fun testSpringForce() {
        val springAnimationWithoutFinalPosition = springAnimationOf(
            view::setTranslationX, view::getTranslationX
        )
            .withSpringForceProperties {
                finalPosition = 100f
                dampingRatio = SpringForce.DAMPING_RATIO_HIGH_BOUNCY
                stiffness = SpringForce.STIFFNESS_HIGH
            }
        assertNotNull(springAnimationWithoutFinalPosition.spring)
        assertEquals(springAnimationWithoutFinalPosition.spring.finalPosition, 100f)
        assertEquals(
            springAnimationWithoutFinalPosition.spring.dampingRatio,
            SpringForce.DAMPING_RATIO_HIGH_BOUNCY
        )
        assertEquals(
            springAnimationWithoutFinalPosition.spring.stiffness,
            SpringForce.STIFFNESS_HIGH
        )
        val springAnimationWithFinalPosition = springAnimationOf(
            view::setTranslationX, view::getTranslationX, 120f
        )
            .withSpringForceProperties {
                dampingRatio = SpringForce.DAMPING_RATIO_LOW_BOUNCY
                stiffness = SpringForce.STIFFNESS_LOW
            }
        assertNotNull(springAnimationWithFinalPosition.spring)
        assertEquals(springAnimationWithFinalPosition.spring.finalPosition, 120f)
        assertEquals(
            springAnimationWithFinalPosition.spring.dampingRatio,
            SpringForce.DAMPING_RATIO_LOW_BOUNCY
        )
        assertEquals(
            springAnimationWithFinalPosition.spring.stiffness,
            SpringForce.STIFFNESS_LOW
        )
    }
}
