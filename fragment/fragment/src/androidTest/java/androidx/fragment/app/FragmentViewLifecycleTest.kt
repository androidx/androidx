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

package androidx.fragment.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentViewLifecycleTest {

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(FragmentTestActivity::class.java)

    @Test
    @UiThreadTest
    fun testFragmentViewLifecycle() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = StrictViewFragment(R.layout.fragment_a)
        fm.beginTransaction().add(R.id.content, fragment).commitNow()
        assertThat(fragment.viewLifecycleOwner.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
    }

    @Test
    @UiThreadTest
    fun testFragmentViewLifecycleNullView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = Fragment()
        fm.beginTransaction().add(fragment, "fragment").commitNow()
        try {
            fragment.viewLifecycleOwner
            fail("getViewLifecycleOwner should be unavailable if onCreateView returned null")
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat().contains(
                    "Can't access the Fragment View's LifecycleOwner when" +
                        " getView() is null i.e., before onCreateView() or after onDestroyView()"
                )
        }
    }

    @Test
    @UiThreadTest
    fun testObserveInOnCreateViewNullView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = ObserveInOnCreateViewFragment()
        try {
            fm.beginTransaction().add(fragment, "fragment").commitNow()
            fail("Fragments accessing view lifecycle should fail if onCreateView returned null")
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat()
                .contains("Called getViewLifecycleOwner() but onCreateView() returned null")
            // We need to clean up the Fragment to avoid it still being around
            // when the instrumentation test Activity pauses. Real apps would have
            // just crashed right after onCreateView().
            fm.beginTransaction().remove(fragment).commitNow()
        }
    }

    @Test
    fun testFragmentViewLifecycleRunOnCommit() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val countDownLatch = CountDownLatch(1)
        val fragment = StrictViewFragment(R.layout.fragment_a)
        fm.beginTransaction().add(R.id.content, fragment).runOnCommit {
            assertThat(fragment.viewLifecycleOwner.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.RESUMED)
            countDownLatch.countDown()
        }.commit()
        countDownLatch.await(1, TimeUnit.SECONDS)
    }

    @Test
    fun testFragmentViewLifecycleOwnerLiveData() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val countDownLatch = CountDownLatch(2)
        val fragment = StrictViewFragment(R.layout.fragment_a)
        activityRule.runOnUiThread {
            fragment.viewLifecycleOwnerLiveData.observe(
                activity,
                Observer { lifecycleOwner ->
                    if (lifecycleOwner != null) {
                        assertWithMessage(
                            "Fragment View LifecycleOwner should be only be set" +
                                "after onCreateView()"
                        )
                            .that(fragment.onCreateViewCalled)
                            .isTrue()
                        countDownLatch.countDown()
                    } else {
                        assertWithMessage(
                            "Fragment View LifecycleOwner should be set to null" +
                                " after onDestroyView()"
                        )
                            .that(fragment.onDestroyViewCalled)
                            .isTrue()
                        countDownLatch.countDown()
                    }
                }
            )
            fm.beginTransaction().add(R.id.content, fragment).commitNow()
            // Now remove the Fragment to trigger the destruction of the view
            fm.beginTransaction().remove(fragment).commitNow()
        }
        countDownLatch.await(1, TimeUnit.SECONDS)
    }

    @Test
    fun testViewLifecycleInFragmentLifecycle() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = StrictViewFragment(R.layout.fragment_a)
        val lifecycleObserver = mock(LifecycleEventObserver::class.java)
        lateinit var viewLifecycleOwner: LifecycleOwner
        activityRule.runOnUiThread {
            fragment.viewLifecycleOwnerLiveData.observe(
                activity,
                Observer { lifecycleOwner ->
                    if (lifecycleOwner != null) {
                        viewLifecycleOwner = lifecycleOwner
                        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
                    }
                }
            )
            fragment.lifecycle.addObserver(lifecycleObserver)
            fm.beginTransaction().add(R.id.content, fragment).commitNow()
            // Now remove the Fragment to trigger the destruction of the view
            fm.beginTransaction().remove(fragment).commitNow()
        }

        // The Fragment's lifecycle should change first, followed by the fragment's view lifecycle
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_CREATE)
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_CREATE)
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_START)
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_START)
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_RESUME)
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_RESUME)
        // Now the order reverses as things unwind
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_PAUSE)
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_PAUSE)
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_STOP)
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_STOP)
        verify(lifecycleObserver).onStateChanged(viewLifecycleOwner, Lifecycle.Event.ON_DESTROY)
        verify(lifecycleObserver).onStateChanged(fragment, Lifecycle.Event.ON_DESTROY)
        verifyNoMoreInteractions(lifecycleObserver)
    }

    @Test
    @UiThreadTest
    fun testFragmentViewLifecycleDetach() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = ObservingFragment()
        fm.beginTransaction().add(R.id.content, fragment).commitNow()
        val viewLifecycleOwner = fragment.viewLifecycleOwner
        assertThat(viewLifecycleOwner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("LiveData should have active observers when RESUMED")
            .that(fragment.liveData.hasActiveObservers()).isTrue()

        fm.beginTransaction().detach(fragment).commitNow()
        assertThat(viewLifecycleOwner.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("LiveData should not have active observers after detach()")
            .that(fragment.liveData.hasActiveObservers()).isFalse()
        try {
            fragment.viewLifecycleOwner
            fail("getViewLifecycleOwner should be unavailable after onDestroyView")
        } catch (expected: IllegalStateException) {
            assertThat(expected)
                .hasMessageThat().contains(
                    "Can't access the Fragment View's LifecycleOwner when" +
                        " getView() is null i.e., before onCreateView() or after onDestroyView()"
                )
        }
    }

    @Test
    @UiThreadTest
    fun testFragmentViewLifecycleReattach() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = ObservingFragment()
        fm.beginTransaction().add(R.id.content, fragment).commitNow()
        val viewLifecycleOwner = fragment.viewLifecycleOwner
        assertThat(viewLifecycleOwner.lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("LiveData should have active observers when RESUMED")
            .that(fragment.liveData.hasActiveObservers()).isTrue()

        fm.beginTransaction().detach(fragment).commitNow()
        // The existing view lifecycle should be destroyed
        assertThat(viewLifecycleOwner.lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)
        assertWithMessage("LiveData should not have active observers after detach()")
            .that(fragment.liveData.hasActiveObservers()).isFalse()

        fm.beginTransaction().attach(fragment).commitNow()
        assertWithMessage("A new view LifecycleOwner should be returned after reattachment")
            .that(fragment.viewLifecycleOwner).isNotEqualTo(viewLifecycleOwner)
        assertThat(fragment.viewLifecycleOwner.lifecycle.currentState)
            .isEqualTo(Lifecycle.State.RESUMED)
        assertWithMessage("LiveData should have active observers when RESUMED")
            .that(fragment.liveData.hasActiveObservers()).isTrue()
    }

    /**
     * Test that the ViewTree get() methods for a fragment's view work correctly.
     */
    @Test
    fun testFragmentViewTree() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = ViewTreeCheckFragment()

        var observedLifecycleOwner: Any? = "not set"
        var observedTreeLifecycleOwner: Any? = "not set"
        var observedTreeViewModelStoreOwner: Any? = "not set"
        var observedTreeViewSavedStateRegistryOwner: Any? = "not set"

        val latch = CountDownLatch(1)
        activity.runOnUiThread {
            fragment.viewLifecycleOwnerLiveData.observeForever { owner ->
                // Don't check when we're destroying the fragment.
                // We should never get more than one.
                if (owner == null) return@observeForever

                observedLifecycleOwner = owner
                observedTreeLifecycleOwner = fragment.view?.let { ViewTreeLifecycleOwner.get(it) }
                observedTreeViewModelStoreOwner = fragment.view?.let {
                    ViewTreeViewModelStoreOwner.get(it)
                }
                observedTreeViewSavedStateRegistryOwner = fragment.view?.let {
                    it.findViewTreeSavedStateRegistryOwner()
                }
            }

            fm.beginTransaction().add(R.id.content, fragment).commitNow()
            latch.countDown()
        }

        assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue()

        assertWithMessage("ViewTreeLifecycleOwner should match viewLifecycleOwner after commitNow")
            .that(ViewTreeLifecycleOwner.get(fragment.view ?: error("no fragment view created")))
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeViewModelStoreOwner should match viewLifecycleOwner" +
                " after commitNow"
        )
            .that(
                ViewTreeViewModelStoreOwner.get(
                    fragment.view
                        ?: error("no fragment view created")
                )
            )
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeSavedStateRegistryOwner should match viewLifecycleOwner" +
                " after commitNow"
        )
            .that(
                (fragment.view ?: error("no fragment view created"))
                    .findViewTreeSavedStateRegistryOwner()
            )
            .isSameInstanceAs(fragment.viewLifecycleOwner)

        assertWithMessage(
            "ViewTreeLifecycleOwner should match viewLifecycleOwner in " +
                "viewLifecycleOwnerLiveData observer"
        )
            .that(observedTreeLifecycleOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeViewModelStoreOwner should match viewLifecycleOwner in " +
                "viewLifecycleOwnerLiveData observer"
        )
            .that(observedTreeViewModelStoreOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeSavedStateRegistryOwner should match viewLifecycleOwner in " +
                "viewLifecycleOwnerLiveData observer"
        )
            .that(observedTreeViewSavedStateRegistryOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)

        assertWithMessage(
            "ViewTreeLifecycleOwner should match observed LifecycleOwner in " +
                "viewLifecycleOwnerLiveData observer"
        )
            .that(observedTreeLifecycleOwner)
            .isSameInstanceAs(observedLifecycleOwner)

        assertWithMessage(
            "ViewTreeLifecycleOwner should match viewLifecycleOwner in " +
                "onViewCreated"
        )
            .that(fragment.onViewCreatedLifecycleOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeViewModelStoreOwner should match viewLifecycleOwner in " +
                "onViewCreated"
        )
            .that(fragment.onViewCreatedViewModelStoreOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)
        assertWithMessage(
            "ViewTreeSavedStateRegistryOwner should match viewLifecycleOwner in " +
                "onViewCreated"
        )
            .that(fragment.onViewCreatedSavedStateRegistryOwner)
            .isSameInstanceAs(fragment.viewLifecycleOwner)
    }

    @Test
    fun testViewTreeSavedStateRegistryOwnerWithBackStack() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val savedStateFragment = ViewTreeSavedStateFragment()
        fm.beginTransaction().add(R.id.content, savedStateFragment).commit()
        activityRule.executePendingTransactions()

        assertThat(savedStateFragment.stateIsSaved).isFalse()

        fm.beginTransaction()
            .replace(R.id.content, StrictViewFragment())
            .addToBackStack(null)
            .commit()
        activityRule.executePendingTransactions()

        assertThat(savedStateFragment.stateIsSaved).isTrue()

        activityRule.popBackStackImmediate()

        assertThat(savedStateFragment.stateIsRestored).isTrue()
        assertThat(savedStateFragment.restoredState).isEqualTo("test")
    }

    class ViewTreeCheckFragment : Fragment() {
        var onViewCreatedLifecycleOwner: Any? = "not set"
        var onViewCreatedViewModelStoreOwner: Any? = "not set"
        var onViewCreatedSavedStateRegistryOwner: Any? = "not set"

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? = FrameLayout(inflater.context)

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            onViewCreatedLifecycleOwner = ViewTreeLifecycleOwner.get(view)
            onViewCreatedViewModelStoreOwner = ViewTreeViewModelStoreOwner.get(view)
            onViewCreatedSavedStateRegistryOwner = view.findViewTreeSavedStateRegistryOwner()
        }
    }

    class ObserveInOnCreateViewFragment : Fragment() {
        private val liveData = MutableLiveData<Boolean>()
        private val onCreateViewObserver = Observer<Boolean> { }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            liveData.observe(viewLifecycleOwner, onCreateViewObserver)
            assertWithMessage("LiveData should have observers after onCreateView observe")
                .that(liveData.hasObservers()).isTrue()
            // Return null - oops!
            return null
        }
    }

    class ObservingFragment : StrictViewFragment(R.layout.fragment_a) {
        val liveData = MutableLiveData<Boolean>()
        private val onCreateViewObserver = Observer<Boolean> { }
        private val onViewCreatedObserver = Observer<Boolean> { }
        private val onViewStateRestoredObserver = Observer<Boolean> { }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ) = super.onCreateView(inflater, container, savedInstanceState).also {
            liveData.observe(viewLifecycleOwner, onCreateViewObserver)
            assertWithMessage("LiveData should have observers after onCreateView observe")
                .that(liveData.hasObservers()).isTrue()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            liveData.observe(viewLifecycleOwner, onViewCreatedObserver)
            assertWithMessage("LiveData should have observers after onViewCreated observe")
                .that(liveData.hasObservers()).isTrue()
        }

        override fun onViewStateRestored(savedInstanceState: Bundle?) {
            super.onViewStateRestored(savedInstanceState)
            liveData.observe(viewLifecycleOwner, onViewStateRestoredObserver)
            assertWithMessage("LiveData should have observers after onViewStateRestored observe")
                .that(liveData.hasObservers()).isTrue()
        }
    }

    class ViewTreeSavedStateFragment : StrictViewFragment(R.layout.fragment_a) {
        var stateIsSaved = false
        var stateIsRestored = false
        var restoredState: String? = null

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val savedStateRegistryOwner = view.findViewTreeSavedStateRegistryOwner()!!
            val savedStateLifecycle = savedStateRegistryOwner.lifecycle
            val savedStateRegistry = savedStateRegistryOwner.savedStateRegistry
            savedStateLifecycle.addObserver(
                LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_CREATE) {
                        val restoredBundle = savedStateRegistry.consumeRestoredStateForKey(
                            "savedState"
                        )
                        stateIsRestored = restoredBundle != null
                        restoredState = restoredBundle?.getString("state")
                    }
                }
            )
            savedStateRegistry.registerSavedStateProvider("savedState") {
                stateIsSaved = true
                Bundle().apply {
                    putString("state", "test")
                }
            }
        }
    }
}
