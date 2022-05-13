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
package androidx.appcompat.graphics.drawable

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.StateSet
import androidx.appcompat.resources.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.NullObject
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AnimatedStateListDrawableCompatTest {
    private var mContext: Context? = null

    @Before
    fun setup() {
        mContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testStateListDrawable() {
        AnimatedStateListDrawableCompat()

        // Check the values set in the constructor
        Assert.assertNotNull(AnimatedStateListDrawableCompat().constantState)
    }

    @Test
    fun testAddState() {
        val asld = AnimatedStateListDrawableCompat()
        Assert.assertEquals(0, asld.stateCount.toLong())
        try {
            asld.addState(StateSet.WILD_CARD, NullObject.get(), R.id.focused)
            Assert.fail("Expected NullPointerException")
        } catch (e: NullPointerException) {
            // Expected.
        }
        val unfocused: Drawable = MockDrawable()
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.focused)
        Assert.assertEquals(1, asld.stateCount.toLong())
        val focused: Drawable = MockDrawable()
        asld.addState(STATE_FOCUSED, focused, R.id.unfocused)
        Assert.assertEquals(2, asld.stateCount.toLong())
    }

    @Test
    fun testAddTransition() {
        val asld = AnimatedStateListDrawableCompat()
        val focused: Drawable = MockDrawable()
        val unfocused: Drawable = MockDrawable()
        asld.addState(STATE_FOCUSED, focused, R.id.focused)
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused)
        try {
            asld.addTransition(R.id.focused, R.id.focused, NullObject.get(), false)
            Assert.fail("Expected NullPointerException")
        } catch (e: NullPointerException) {
            // Expected.
        }
        val focusedToUnfocused = MockTransition()
        asld.addTransition(R.id.focused, R.id.unfocused, focusedToUnfocused, false)
        Assert.assertEquals(3, asld.stateCount.toLong())
        val unfocusedToFocused = MockTransition()
        asld.addTransition(R.id.unfocused, R.id.focused, unfocusedToFocused, false)
        Assert.assertEquals(4, asld.stateCount.toLong())
        val reversible = MockTransition()
        asld.addTransition(R.id.focused, R.id.unfocused, reversible, true)
        Assert.assertEquals(5, asld.stateCount.toLong())
    }

    @Test
    fun testIsStateful() {
        Assert.assertTrue(AnimatedStateListDrawableCompat().isStateful)
    }

    @Test
    fun testOnStateChange() {
        val asld = AnimatedStateListDrawableCompat()
        val focused: Drawable = MockDrawable()
        val unfocused: Drawable = MockDrawable()
        asld.addState(STATE_FOCUSED, focused, R.id.focused)
        asld.addState(StateSet.WILD_CARD, unfocused, R.id.unfocused)
        val focusedToUnfocused = MockTransition()
        val unfocusedToFocused = MockTransition()
        asld.addTransition(R.id.focused, R.id.unfocused, focusedToUnfocused, false)
        asld.addTransition(R.id.unfocused, R.id.focused, unfocusedToFocused, false)
        asld.state = STATE_EMPTY
        Assert.assertSame(unfocused, asld.current)
        asld.state = STATE_FOCUSED
        Assert.assertSame(unfocusedToFocused, asld.current)
        asld.state = STATE_FOCUSED
        Assert.assertSame(unfocusedToFocused, asld.current)
    }

    @Test
    fun testAnimationDrawableTransition() {
        val asld = AnimatedStateListDrawableCompat.create(
            mContext!!,
            R.drawable.animated_state_list_density, mContext!!.theme
        )
        Assert.assertNotNull(asld)
        Assert.assertTrue(asld!!.isVisible)
        // Missing public API to verify these.
        //assertFalse(asld.isConstantSize());
        //assertNull(asld.getConstantPadding());
        // Check that 4 drawables were parsed
        Assert.assertEquals(4, asld.stateCount.toLong())
    }

    @Test
    fun testAnimatedVectorTransition() {
        val asld = AnimatedStateListDrawableCompat.create(
            mContext!!,
            R.drawable.asl_heart, mContext!!.theme
        )
        // Check that 4 drawables were parsed
        Assert.assertNotNull(asld)
        Assert.assertEquals(4, asld!!.stateCount.toLong())
    }

    @Test
    fun testChildAnimatedVectorTransition() {
        val asld = AnimatedStateListDrawableCompat.create(
            mContext!!,
            R.drawable.animated_state_list_with_avd, mContext!!.theme
        )
        // Check that 6 drawables were parsed
        Assert.assertNotNull(asld)
        Assert.assertEquals(6, asld!!.stateCount.toLong())
    }

    @Test
    fun testChildVectorItem() {
        val asld = AnimatedStateListDrawableCompat.create(
            mContext!!,
            R.drawable.asl_heart_embedded, mContext!!.theme
        )
        // Check that 4 drawables were parsed
        Assert.assertNotNull(asld)
        Assert.assertEquals(4, asld!!.stateCount.toLong())
    }

    @Test
    fun testConstantStateWhenChildHasNullConstantState() {
        // Given an empty ASLD which returns a constant state
        val asld = AnimatedStateListDrawableCompat()
        Assert.assertNotNull(asld.constantState)

        // When a drawable who returns a null constant state is added
        // MockDrawable returns null from getConstantState() - same as Drawable's default impl
        val noConstantStateDrawable: Drawable = MockDrawable()
        asld.addState(StateSet.WILD_CARD, noConstantStateDrawable, R.id.focused)

        // Then the ASLD should also return a null constant state
        Assert.assertNull(asld.constantState)
    }

    internal class MockTransition : MockDrawable(), Animatable,
        Animatable2Compat {
        private val mCallbacks = HashSet<Animatable2Compat.AnimationCallback>()
        override fun start() {}
        override fun stop() {}
        override fun isRunning(): Boolean {
            return false
        }

        override fun registerAnimationCallback(callback: Animatable2Compat.AnimationCallback) {
            mCallbacks.add(callback)
        }

        override fun unregisterAnimationCallback(callback: Animatable2Compat.AnimationCallback): Boolean {
            return mCallbacks.remove(callback)
        }

        override fun clearAnimationCallbacks() {
            mCallbacks.clear()
        }
    }

    internal open class MockDrawable : Drawable() {
        override fun draw(canvas: Canvas) {}

        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Deprecated in Java")
        override fun getOpacity(): Int {
            return 0
        }

        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(cf: ColorFilter?) {}
    }

    companion object {
        private val STATE_EMPTY = intArrayOf()
        private val STATE_FOCUSED = intArrayOf(android.R.attr.state_focused)
    }
}