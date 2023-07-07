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

package androidx.viewpager2.integration.testapp

import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.core.text.TextUtilsCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.integration.testapp.cards.CardViewAdapter
import androidx.viewpager2.widget.ViewPager2
import java.util.Locale

class FakeDragActivity : FragmentActivity() {

    private lateinit var viewPager: ViewPager2
    private var landscape = false
    private var lastValue: Float = 0f

    private val isRtl = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) ==
        ViewCompat.LAYOUT_DIRECTION_RTL

    private val ViewPager2.isHorizontal: Boolean
        get() {
            return orientation == ViewPager2.ORIENTATION_HORIZONTAL
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fakedrag)
        landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = CardViewAdapter()
        viewPager.isUserInputEnabled = false
        UserInputController(viewPager, findViewById(R.id.disable_user_input_checkbox)).setUp()
        OrientationController(viewPager, findViewById(R.id.orientation_spinner)).setUp()

        findViewById<View>(R.id.touchpad).setOnTouchListener { _, event ->
            handleOnTouchEvent(event)
        }
    }

    private fun mirrorInRtl(f: Float): Float {
        return if (isRtl) -f else f
    }

    private fun getValue(event: MotionEvent): Float {
        return if (landscape) event.y else mirrorInRtl(event.x)
    }

    private fun handleOnTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastValue = getValue(event)
                viewPager.beginFakeDrag()
            }

            MotionEvent.ACTION_MOVE -> {
                val value = getValue(event)
                val delta = value - lastValue
                viewPager.fakeDragBy(if (viewPager.isHorizontal) mirrorInRtl(delta) else delta)
                lastValue = value
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                viewPager.endFakeDrag()
            }
        }
        return true
    }
}
