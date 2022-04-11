/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.fragment.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentFocusTest {

    @Test
    fun focusedViewRemoved() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val (fragment, firstEditText) = withActivity {
                setContentView(R.layout.simple_container)
                val container = findViewById<View>(R.id.fragmentContainer) as ViewGroup

                val firstEditText = EditText(container.context)
                container.addView(firstEditText)
                firstEditText.requestFocus()

                val fragment = RemoveEditViewFragment()

                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(1, 0)
                    .replace(R.id.fragmentContainer, fragment)
                    .setReorderingAllowed(true)
                    .commitNow()

                fragment to firstEditText
            }

            assertThat(fragment.endAnimationCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            withActivity {
                val editText = fragment.requireView().findViewById<View>(R.id.editText)
                assertThat(editText).isNull()
                assertThat(firstEditText.isFocused).isTrue()
            }
        }
    }

    @Test
    fun focusedViewRootView() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            val fragment = RequestViewFragment()

            withActivity {
                setContentView(R.layout.simple_container)

                supportFragmentManager.beginTransaction()
                    .setCustomAnimations(1, 0)
                    .replace(R.id.fragmentContainer, fragment)
                    .setReorderingAllowed(true)
                    .commitNow()
            }

            assertThat(fragment.endAnimationCountDownLatch.await(1000, TimeUnit.MILLISECONDS))
                .isTrue()

            withActivity {
                val view = fragment.requireView()
                assertThat(view.isFocused).isTrue()
            }
        }
    }

    @Test
    fun inResumefocusedViewRemoved() {
        with(ActivityScenario.launch(FragmentTestActivity::class.java)) {
            withActivity {
                val fragment = StrictViewFragment(R.layout.simple_container)

                supportFragmentManager.beginTransaction()
                    .replace(R.id.content, fragment)
                    .setReorderingAllowed(true)
                    .commitNow()

                val container = findViewById<ViewGroup>(R.id.fragmentContainer)

                val editText = EditText(container.context)

                (fragment.requireView() as ViewGroup).addView(editText)
                editText.requestFocus()

                supportFragmentManager.beginTransaction()
                    .remove(fragment)
                    .setReorderingAllowed(true)
                    .commitNow()

                assertThat(fragment.getFocusedView()).isNull()
            }
        }
    }

    class RemoveEditViewFragment : StrictViewFragment(R.layout.with_edit_text) {
        val endAnimationCountDownLatch = CountDownLatch(1)
        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
            if (nextAnim == 0) {
                return null
            }

            val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    val editText = requireView().findViewById<EditText>(R.id.editText)
                    (view as ViewGroup).removeView(editText)
                    requireActivity().findViewById<ViewGroup>(
                        R.id.fragmentContainer
                    ).addView(editText)
                }

                override fun onAnimationEnd(animation: Animator) {
                    endAnimationCountDownLatch.countDown()
                }
            })
            return animator
        }
    }

    class RequestViewFragment : StrictViewFragment() {
        val endAnimationCountDownLatch = CountDownLatch(1)
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            view.isFocusable = true
            view.isFocusableInTouchMode = true
            view.requestFocus()
        }

        override fun onCreateAnimator(transit: Int, enter: Boolean, nextAnim: Int): Animator? {
            if (nextAnim == 0) {
                return null
            }

            val animator = ValueAnimator.ofFloat(0f, 1f).setDuration(1)
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    endAnimationCountDownLatch.countDown()
                }
            })
            return animator
        }
    }
}