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

package androidx.fragment.app

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Canvas
import android.graphics.Insets
import android.os.Bundle
import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.animation.Animation
import androidx.core.app.ActivityCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.waitForExecution
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentContainerViewTest {
    @Suppress("DEPRECATION")
    val activityRule = androidx.test.rule.ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain =
        RuleChain.outerRule(DetectLeaksAfterTestSuccess()).around(activityRule)

    lateinit var context: Context

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.fragment_container_view)
        context = activityRule.activity.applicationContext
    }

    @Test
    fun inflateFragmentContainerNoActivity() {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.fragment_container_view, null)
    }

    @Test(expected = InflateException::class)
    fun inflatedFragmentContainerNoActivityWithName() {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.inflated_fragment_container_view, null)
    }

    @Test(expected = InflateException::class)
    fun inflatedFragmentContainerNoActivityWithClass() {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.inflated_fragment_container_view_with_class, null)
    }

    @Test
    fun inflatedNestedFragmentWithStates() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager
        val fragmentParent =
            StrictViewFragment(
                contentLayoutId = R.layout.inflated_fragment_container_view_strict_view
            )

        fm.beginTransaction().add(R.id.fragment_container_view, fragmentParent).commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        // child frag should inflate properly with correct states
        val childFrag = fragmentParent.childFragmentManager.findFragmentByTag("childFragment")
        assertThat(childFrag).isNotNull()
    }

    @Test
    fun setLayoutTransitionUnsupported() {
        val activity = activityRule.activity
        val layout = FragmentContainerView(activity.applicationContext)

        try {
            layout.layoutTransition = LayoutTransition()
            fail("setLayoutTransition should throw UnsupportedOperationException")
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\"."
                )
        }
    }

    // If view sets animateLayoutChanges to true, throw UnsupportedOperationException
    @Test
    fun animateLayoutChangesTrueUnsupported() {
        try {
            StrictViewFragment(R.layout.fragment_container_view_unsupported_operation)
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\"."
                )
        }
    }

    @Test
    fun createFragmentWithFragmentContainerView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = StrictViewFragment(R.layout.fragment_container_view)
        fm.beginTransaction().add(R.id.fragment_container_view, fragment).commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        assertWithMessage("Fragment View should be a FragmentContainerView")
            .that(fragment.view)
            .isInstanceOf(FragmentContainerView::class.java)
    }

    @Suppress("DEPRECATION") /* systemWindowInsets */
    @SdkSuppress(minSdkVersion = 29) // WindowInsets.Builder requires API 29
    @Test
    fun windowInsetsDispatchToChildren() {
        val parentView = FragmentContainerView(context)
        val childView = FragmentContainerView(context)

        parentView.fitsSystemWindows = true

        val sentInsets = WindowInsets.Builder().setSystemWindowInsets(Insets.of(4, 3, 2, 1)).build()

        var dispatchedToChild = 0
        childView.setOnApplyWindowInsetsListener(
            object : View.OnApplyWindowInsetsListener {
                override fun onApplyWindowInsets(p0: View, insets: WindowInsets): WindowInsets {
                    assertThat(insets.systemWindowInsets).isEqualTo(sentInsets.systemWindowInsets)
                    dispatchedToChild++
                    return insets
                }
            }
        )

        childView.setTag(androidx.fragment.R.id.fragment_container_view_tag, Fragment())

        parentView.addView(childView)
        parentView.dispatchApplyWindowInsets(sentInsets)

        assertThat(dispatchedToChild).isEqualTo(1)
    }

    @Suppress("DEPRECATION") /* systemWindowInsets */
    @SdkSuppress(minSdkVersion = 29) // WindowInsets.Builder requires API 29
    @Test
    fun windowInsetsDispatchToMultipleChildren() {
        val parentView = FragmentContainerView(context)
        val childView = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        parentView.fitsSystemWindows = true

        val sentInsets = WindowInsets.Builder().setSystemWindowInsets(Insets.of(4, 3, 2, 1)).build()

        var dispatchedToChild = 0
        childView.setOnApplyWindowInsetsListener(
            object : View.OnApplyWindowInsetsListener {
                override fun onApplyWindowInsets(p0: View, insets: WindowInsets): WindowInsets {
                    assertThat(insets.systemWindowInsets).isEqualTo(sentInsets.systemWindowInsets)
                    dispatchedToChild++
                    return WindowInsets.Builder()
                        .setSystemWindowInsets(Insets.of(0, 0, 0, 0))
                        .build()
                }
            }
        )

        var dispatchedToChild2 = 0
        childView2.setOnApplyWindowInsetsListener(
            object : View.OnApplyWindowInsetsListener {
                override fun onApplyWindowInsets(p0: View, insets: WindowInsets): WindowInsets {
                    assertThat(insets.systemWindowInsets).isEqualTo(sentInsets.systemWindowInsets)
                    dispatchedToChild2++
                    return insets
                }
            }
        )

        childView.setTag(androidx.fragment.R.id.fragment_container_view_tag, Fragment())
        childView2.setTag(androidx.fragment.R.id.fragment_container_view_tag, Fragment())

        parentView.addView(childView)
        parentView.addView(childView2)
        parentView.dispatchApplyWindowInsets(sentInsets)

        assertThat(dispatchedToChild).isEqualTo(1)
        assertThat(dispatchedToChild2).isEqualTo(1)
    }

    @Suppress("DEPRECATION") /* systemWindowInsets */
    @SdkSuppress(minSdkVersion = 29) // WindowInsets.Builder requires API 29
    @Test
    fun onApplyWindowInsets() {
        val fragmentContainerView = FragmentContainerView(context)
        var calledListener = false
        fragmentContainerView.fitsSystemWindows = true

        val sentInsets = WindowInsets.Builder().setSystemWindowInsets(Insets.of(4, 3, 2, 1)).build()

        ViewCompat.setOnApplyWindowInsetsListener(
            fragmentContainerView,
            object : OnApplyWindowInsetsListener {
                override fun onApplyWindowInsets(
                    v: View,
                    insets: WindowInsetsCompat,
                ): WindowInsetsCompat {
                    calledListener = true
                    return insets
                }
            },
        )

        fragmentContainerView.onApplyWindowInsets(sentInsets)

        assertThat(calledListener).isFalse()
    }

    @Test
    fun addView() {
        val view = View(context)
        val fragment = Fragment()
        fragment.mView = view

        // Mimic what FragmentStateManager.createView() does
        fragment.mView.setTag(androidx.fragment.R.id.fragment_container_view_tag, fragment)

        val fragmentContainerView = FragmentContainerView(context)

        assertWithMessage("FragmentContainerView should have no child views")
            .that(fragmentContainerView.childCount)
            .isEqualTo(0)

        fragmentContainerView.addView(view)

        assertWithMessage("FragmentContainerView should have one child view")
            .that(fragmentContainerView.childCount)
            .isEqualTo(1)
    }

    @Test
    fun addViewNotAssociatedWithFragment() {
        val view = View(context)

        try {
            FragmentContainerView(context).addView(view, 0, null)
            fail("View without a Fragment added to FragmentContainerView should throw an exception")
        } catch (e: IllegalStateException) {
            assertThat(e)
                .hasMessageThat()
                .contains(
                    "Views added to a FragmentContainerView must be associated with a Fragment. " +
                        "View " +
                        view +
                        " is not associated with a Fragment."
                )
        }
    }

    @Test
    fun removeViewAt() {
        val childView2 = FragmentContainerView(context)

        val view = setupRemoveTestsView(FragmentContainerView(context), childView2)

        view.removeViewAt(0)

        assertThat(view.childCount).isEqualTo(1)
        assertThat(view.getChildAt(0)).isEqualTo(childView2)
    }

    @Test
    fun removeViewInLayout() {
        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        val view = setupRemoveTestsView(childView1, childView2)

        view.removeViewInLayout(childView1)

        assertThat(view.childCount).isEqualTo(1)
        assertThat(view.getChildAt(0)).isEqualTo(childView2)
    }

    @Test
    fun removeView() {
        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        val view = setupRemoveTestsView(childView1, childView2)

        view.removeView(childView1)

        assertThat(view.getChildAt(0)).isEqualTo(childView2)
    }

    @Test
    fun removeViews() {
        val view =
            setupRemoveTestsView(FragmentContainerView(context), FragmentContainerView(context))

        view.removeViews(1, 1)

        assertThat(view.childCount).isEqualTo(1)
    }

    @Test
    fun removeViewsInLayout() {
        val view =
            setupRemoveTestsView(FragmentContainerView(context), FragmentContainerView(context))

        view.removeViewsInLayout(1, 1)

        assertThat(view.childCount).isEqualTo(1)
    }

    @Test
    fun removeAllViewsInLayout() {
        val removingView1 = ChildView(context)
        val removingView2 = ChildView(context)

        val view = setupRemoveTestsView(removingView1, removingView2)

        view.removeAllViewsInLayout()

        assertThat(removingView1.getAnimationCount).isEqualTo(1)
        assertThat(removingView2.getAnimationCount).isEqualTo(1)
        assertThat(view.childCount).isEqualTo(0)
    }

    private fun setupRemoveTestsView(childView1: View, childView2: View): FragmentContainerView {
        val view = FragmentContainerView(context)
        val fragment1 = Fragment()
        val fragment2 = Fragment()

        fragment1.mView = childView1
        fragment2.mView = childView2

        childView1.setTag(androidx.fragment.R.id.fragment_container_view_tag, fragment1)
        childView2.setTag(androidx.fragment.R.id.fragment_container_view_tag, fragment2)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(1)).isEqualTo(childView2)
        return view
    }

    // Disappearing child views should be drawn first before other child views.
    @Test
    fun drawDisappearingChildViewsFirst() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ChildViewFragment()
        val fragment2 = ChildViewFragment()

        fm.beginTransaction()
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(R.id.fragment_container_view, fragment1)
            .commit()
        activityRule.waitForExecution()

        val frag1View = fragment1.mView as ChildView
        // wait for the first draw to finish
        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        assertWithMessage("Timed out waiting for onAnimationEnd on Fragment 1")
            .that(frag1View.onAnimationEndLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        // reset the first drawn view for the transaction we care about.
        drawnFirst = null
        drawnFirstCountDownLatch = CountDownLatch(1)

        fm.beginTransaction()
            .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            .replace(R.id.fragment_container_view, fragment2)
            .commit()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        assertThat(drawnFirst!!).isEqualTo(frag1View)

        drawnFirst = null
    }

    // Disappearing child views should be drawn last if transaction is a pop.
    @Test
    fun drawDisappearingChildViewsLast() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ChildViewFragment()
        val fragment2 = ChildViewFragment()

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val frag1View = fragment1.mView as ChildView

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment2, "2")
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val frag2View = fragment2.mView as ChildView
        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 1")
            .that(frag1View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag1View.onDetachFromWindowLatch = CountDownLatch(1)

        // reset the first drawn view for the transaction we care about.
        drawnFirst = null
        drawnFirstCountDownLatch = CountDownLatch(1)

        fm.popBackStack()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 2")
            .that(frag2View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag2View.onDetachFromWindowLatch = CountDownLatch(1)

        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        // The popped Fragment will be drawn last and therefore will be on top
        assertThat(drawnFirst!!).isNotEqualTo(frag2View)

        drawnFirst = null
    }

    @Test
    fun drawDisappearingChildViewsLastAfterPopNoReordering() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ChildViewFragment("child1")
        val fragment2 = ChildViewFragment("child2")

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val frag1View = fragment1.mView as ChildView

        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment2, "2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val frag2View = fragment2.mView as ChildView
        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 1")
            .that(frag1View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag1View.onDetachFromWindowLatch = CountDownLatch(1)

        // reset the first drawn view for the transaction we care about.
        drawnFirst = null
        drawnFirstCountDownLatch = CountDownLatch(1)

        fm.popBackStack()
        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 2")
            .that(frag2View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag2View.onDetachFromWindowLatch = CountDownLatch(1)

        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        assertThat(drawnFirst!!).isNotEqualTo(frag2View)

        drawnFirst = null
    }

    @Test
    fun drawDisappearingChildViewsLastAfterPopReorderingAllowed() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ChildViewFragment("child1")
        val fragment2 = ChildViewFragment("child2")

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val frag1View = fragment1.mView as ChildView

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment2, "2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val frag2View = fragment2.mView as ChildView
        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 1")
            .that(frag1View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag1View.onDetachFromWindowLatch = CountDownLatch(1)

        // reset the first drawn view for the transaction we care about.
        drawnFirst = null
        drawnFirstCountDownLatch = CountDownLatch(1)

        fm.popBackStack()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 2")
            .that(frag2View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag2View.onDetachFromWindowLatch = CountDownLatch(1)

        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        assertThat(drawnFirst!!).isNotEqualTo(frag2View)

        drawnFirst = null
    }

    @Test
    fun drawDisappearingChildViewsLastAfterPopReorderingAllowedAddNewFragment() {
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = ChildViewFragment("child1")
        val fragment2 = ChildViewFragment("child2")

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment1, "1")
            .commit()
        activityRule.waitForExecution()

        val frag1View = fragment1.mView as ChildView

        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .replace(R.id.fragment_container_view, fragment2, "2")
            .setPrimaryNavigationFragment(fragment2)
            .addToBackStack(null)
            .commit()
        activityRule.waitForExecution()

        val frag2View = fragment2.mView as ChildView
        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 1")
            .that(frag1View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag1View.onDetachFromWindowLatch = CountDownLatch(1)

        // reset the first drawn view for the transaction we care about.
        drawnFirst = null
        drawnFirstCountDownLatch = CountDownLatch(1)

        fm.popBackStack()
        fm.beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right,
            )
            .add(R.id.fragment_container_view, ChildViewFragment(), "1")
            .commit()
        activityRule.waitForExecution()

        assertWithMessage("Timed out waiting for onDetachFromWindow on Fragment 2")
            .that(frag2View.onDetachFromWindowLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        frag2View.onDetachFromWindowLatch = CountDownLatch(1)

        assertWithMessage("Timed out waiting for setDrawnFirstView")
            .that(drawnFirstCountDownLatch.await(1, TimeUnit.SECONDS))
            .isTrue()
        // The view that was popped is drawn first which means it is on the bottom.
        assertThat(drawnFirst!!).isEqualTo(frag2View)

        drawnFirst = null
    }

    @Test
    fun getFragmentNoneAdded() {
        val fragmentContainerView =
            ActivityCompat.requireViewById<FragmentContainerView>(
                activityRule.activity,
                R.id.fragment_container_view,
            )

        assertThat(fragmentContainerView.getFragment<StrictViewFragment?>()).isNull()
    }

    @Test
    fun getFragmentTwoAdds() {
        val fragmentContainerView =
            ActivityCompat.requireViewById<FragmentContainerView>(
                activityRule.activity,
                R.id.fragment_container_view,
            )
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment()
        val fragment2 = StrictViewFragment()

        fm.beginTransaction().add(R.id.fragment_container_view, fragment1).commit()
        activityRule.waitForExecution()

        fm.beginTransaction().add(R.id.fragment_container_view, fragment2).commit()
        activityRule.waitForExecution()

        val topFragment = fragmentContainerView.getFragment<StrictViewFragment>()
        assertThat(topFragment).isSameInstanceAs(fragment2)
    }

    @Test
    fun getFragmentAddAndReplace() {
        val fragmentContainerView =
            ActivityCompat.requireViewById<FragmentContainerView>(
                activityRule.activity,
                R.id.fragment_container_view,
            )
        val fm = activityRule.activity.supportFragmentManager

        val fragment1 = StrictViewFragment(R.layout.fragment_container_view)
        val fragment2 = StrictViewFragment(R.layout.fragment_container_view)

        fm.beginTransaction().add(R.id.fragment_container_view, fragment1).commit()
        activityRule.waitForExecution()

        fm.beginTransaction().replace(R.id.fragment_container_view, fragment2).commit()
        activityRule.waitForExecution()

        val topFragment = fragmentContainerView.getFragment<StrictViewFragment>()
        assertThat(topFragment).isSameInstanceAs(fragment2)
    }

    class ChildViewFragment(val viewTag: String? = null) : StrictViewFragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
        ) = ChildView(context).apply { this.tag = viewTag }
    }

    class ChildView(context: Context?) : View(context) {
        var getAnimationCount = 0
        var onDetachFromWindowLatch = CountDownLatch(1)
        var onAnimationEndLatch = CountDownLatch(1)

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            setDrawnFirstView(this)
        }

        override fun getAnimation(): Animation? {
            getAnimationCount++
            return super.getAnimation()
        }

        override fun onDetachedFromWindow() {
            onDetachFromWindowLatch.countDown()
            super.onDetachedFromWindow()
        }

        override fun onAnimationEnd() {
            onAnimationEndLatch.countDown()
            super.onAnimationEnd()
        }
    }

    companion object {
        var drawnFirst: View? = null
        var drawnFirstCountDownLatch = CountDownLatch(1)

        fun setDrawnFirstView(v: View) {
            if (drawnFirst == null) {
                drawnFirst = v
            }
            drawnFirstCountDownLatch.countDown()
        }
    }
}
