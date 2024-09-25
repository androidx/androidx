/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.tests.endtoend

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.privacysandbox.ui.client.SandboxedUiAdapterFactory
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionState
import androidx.privacysandbox.ui.client.view.SandboxedSdkUiSessionStateChangedListener
import androidx.privacysandbox.ui.client.view.SandboxedSdkView
import androidx.privacysandbox.ui.core.BackwardCompatUtil
import androidx.privacysandbox.ui.tests.util.ScreenshotTestingUtil
import androidx.privacysandbox.ui.tests.util.TestSessionManager
import androidx.privacysandbox.ui.tests.util.TestSessionManager.Companion.SDK_VIEW_COLOR
import androidx.privacysandbox.ui.tests.util.TestSessionManager.Companion.TIMEOUT
import androidx.privacysandbox.ui.tests.util.TestSessionManager.TestSandboxedUiAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@MediumTest
class PoolingContainerTests(private val invokeBackwardsCompatFlow: Boolean) {

    @get:Rule var activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "invokeBackwardsCompatFlow={0}")
        fun data(): Array<Any> =
            arrayOf(
                arrayOf(true),
                arrayOf(false),
            )
    }

    private val context = InstrumentationRegistry.getInstrumentation().context

    private lateinit var recyclerView: RecyclerView
    private lateinit var activity: Activity
    private lateinit var linearLayout: LinearLayout
    private lateinit var mInstrumentation: Instrumentation
    private lateinit var sessionManager: TestSessionManager

    @Before
    fun setup() {
        if (!invokeBackwardsCompatFlow) {
            // Device needs to support remote provider to invoke non-backward-compat flow.
            assumeTrue(BackwardCompatUtil.canProviderBeRemote())
        }
        sessionManager = TestSessionManager(context, invokeBackwardsCompatFlow)

        mInstrumentation = InstrumentationRegistry.getInstrumentation()

        activity = activityScenarioRule.withActivity { this }
        activityScenarioRule.withActivity {
            recyclerView = RecyclerView(context)
            linearLayout = LinearLayout(context)
            linearLayout.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            linearLayout.setBackgroundColor(Color.RED)
            setContentView(linearLayout)
            linearLayout.addView(recyclerView)
            recyclerView.setBackgroundColor(Color.GREEN)
            recyclerView.layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            recyclerView.setLayoutManager(LinearLayoutManager(context))
        }
    }

    @Test
    fun testPoolingContainerListener_AllViewsRemovedFromContainer() {
        val adapter =
            createRecyclerViewTestAdapterAndWaitForChildrenToBeActive(isNestedView = false)

        activityScenarioRule.withActivity { recyclerView.layoutManager!!.removeAllViews() }

        adapter.waitForViewsToBeDetached()
        adapter.ensureChildrenDoNotBecomeIdleFromActive()
    }

    @Test
    fun testPoolingContainerListener_ContainerRemovedFromLayout() {
        val adapter = createRecyclerViewTestAdapterAndWaitForChildrenToBeActive(isNestedView = true)

        activityScenarioRule.withActivity { linearLayout.removeView(recyclerView) }

        adapter.ensureAllChildrenBecomeIdleFromActive()
    }

    @Test
    fun testPoolingContainerListener_ViewWithinAnotherView_AllViewsRemovedFromContainer() {
        val adapter =
            createRecyclerViewTestAdapterAndWaitForChildrenToBeActive(isNestedView = false)

        activityScenarioRule.withActivity { recyclerView.layoutManager!!.removeAllViews() }

        adapter.waitForViewsToBeDetached()
        adapter.ensureChildrenDoNotBecomeIdleFromActive()
    }

    @Test
    fun testPoolingContainerListener_ViewWithinAnotherView_ContainerRemovedFromLayout() {
        val adapter = createRecyclerViewTestAdapterAndWaitForChildrenToBeActive(isNestedView = true)

        activityScenarioRule.withActivity { linearLayout.removeView(recyclerView) }

        adapter.ensureAllChildrenBecomeIdleFromActive()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun testPoolingContainerListener_NotifyFetchUiForSession() {
        // verifyColorOfScreenshot is only available for U+ devices.
        assumeTrue(SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

        val recyclerViewAdapter = RecyclerViewTestAdapterForFetchingUi()

        activityScenarioRule.withActivity { recyclerView.setAdapter(recyclerViewAdapter) }

        recyclerViewAdapter.scrollSmoothlyToPosition(recyclerViewAdapter.itemCount - 1)
        recyclerViewAdapter.ensureAllChildrenBecomeActive()
        recyclerViewAdapter.scrollSmoothlyToPosition(0)
        val displayMetrics = activity.resources.displayMetrics
        // We don't need to check all the pixels since we only care that at least some of
        // them are equal to SDK_VIEW_COLOR. The smaller rectangle that we will be checking
        // of size 10*10. This will make the test run faster.
        val midPixelLocation = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels) / 2
        assertThat(
                ScreenshotTestingUtil.verifyColorOfScreenshot(
                    mInstrumentation,
                    activity.window,
                    midPixelLocation,
                    midPixelLocation,
                    midPixelLocation + 10,
                    midPixelLocation + 10,
                    SDK_VIEW_COLOR
                )
            )
            .isTrue()
    }

    private fun createRecyclerViewTestAdapterAndWaitForChildrenToBeActive(
        isNestedView: Boolean
    ): RecyclerViewTestAdapter {
        val recyclerViewAdapter = RecyclerViewTestAdapter(context, isNestedView)
        activityScenarioRule.withActivity { recyclerView.setAdapter(recyclerViewAdapter) }

        recyclerViewAdapter.waitForViewsToBeAttached()

        for (i in 0 until recyclerView.childCount) {
            lateinit var childView: SandboxedSdkView
            if (isNestedView) {
                childView =
                    (recyclerView.getChildAt(i) as ViewGroup).getChildAt(0) as SandboxedSdkView
            } else {
                childView = recyclerView.getChildAt(i) as SandboxedSdkView
            }
            sessionManager.createAdapterAndWaitToBeActive(true, childView)
        }

        recyclerViewAdapter.ensureAllChildrenBecomeActive()
        return recyclerViewAdapter
    }

    inner class RecyclerViewTestAdapterForFetchingUi :
        RecyclerView.Adapter<RecyclerViewTestAdapterForFetchingUi.ViewHolder>() {

        private val sandboxedSdkViewSet = mutableSetOf<SandboxedSdkView>()
        private val itemCount = 5
        private val activeLatch = CountDownLatch(itemCount)

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val sandboxedSdkView: SandboxedSdkView =
                (view as LinearLayout).getChildAt(0) as SandboxedSdkView
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
            val view = LinearLayout(context)
            val childSandboxedSdkView = SandboxedSdkView(context)
            view.addView(childSandboxedSdkView)
            val layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
            layoutParams.setMargins(20, 20, 20, 20)
            view.layoutParams = layoutParams
            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            val childSandboxedSdkView = viewHolder.sandboxedSdkView

            if (!sandboxedSdkViewSet.contains(childSandboxedSdkView)) {
                val adapter = TestSandboxedUiAdapter()

                childSandboxedSdkView.addStateChangedListener { state ->
                    if (state is SandboxedSdkUiSessionState.Active) {
                        activeLatch.countDown()
                    }
                }

                val adapterFromCoreLibInfo =
                    SandboxedUiAdapterFactory.createFromCoreLibInfo(
                        sessionManager.getCoreLibInfoFromAdapter(adapter)
                    )

                childSandboxedSdkView.setAdapter(adapterFromCoreLibInfo)
                sandboxedSdkViewSet.add(childSandboxedSdkView)
            }
        }

        fun scrollSmoothlyToPosition(position: Int) {
            activityScenarioRule.withActivity { recyclerView.smoothScrollToPosition(position) }
        }

        fun ensureAllChildrenBecomeActive() {
            assertThat(activeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        override fun getItemCount(): Int = itemCount
    }

    class RecyclerViewTestAdapter(
        private val context: Context,
        private val isNestedView: Boolean = false,
    ) : RecyclerView.Adapter<RecyclerViewTestAdapter.ViewHolder>() {
        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

        private var numberOfSandboxedSdkViews = 0
        private val items = 5
        private val activeLatch = CountDownLatch(items)

        // The session will first be idle -> active -> idle in
        // our tests, hence the count is items*2
        private val idleLatch = CountDownLatch(items * 2)
        private val attachedLatch = CountDownLatch(items)
        private val detachedLatch = CountDownLatch(items)
        private val onAttachStateChangeListener =
            object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    attachedLatch.countDown()
                }

                override fun onViewDetachedFromWindow(v: View) {
                    if (attachedLatch.count == 0.toLong()) {
                        detachedLatch.countDown()
                    }
                }
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            if (numberOfSandboxedSdkViews >= items) {
                // We should return without creating a SandboxedSdkView if the
                // number of SandboxedSdkViews is already equal to items. Recycler
                // view will create new ViewHolders once SandboxedSdkViews are
                // removed. We do not want to count latch down at that point of time.
                return ViewHolder(View(context))
            }

            val listener = SandboxedSdkUiSessionStateChangedListener { state ->
                if (state is SandboxedSdkUiSessionState.Active) {
                    activeLatch.countDown()
                } else if (state is SandboxedSdkUiSessionState.Idle) {
                    idleLatch.countDown()
                }
            }

            numberOfSandboxedSdkViews++
            var view: View = SandboxedSdkView(context)
            (view as SandboxedSdkView).addStateChangedListener(listener)
            if (isNestedView) {
                val parentView = LinearLayout(context)
                parentView.addView(view)
                view = parentView
            }
            view.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
            view.addOnAttachStateChangeListener(onAttachStateChangeListener)
            return ViewHolder(view)
        }

        fun waitForViewsToBeAttached() {
            assertThat(attachedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun waitForViewsToBeDetached() {
            assertThat(detachedLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun ensureAllChildrenBecomeActive() {
            assertThat(activeLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun ensureAllChildrenBecomeIdleFromActive() {
            assertThat(idleLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isTrue()
        }

        fun ensureChildrenDoNotBecomeIdleFromActive() {
            assertThat(idleLatch.await(TIMEOUT, TimeUnit.MILLISECONDS)).isFalse()
            assertThat(idleLatch.count).isEqualTo(items)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {}

        override fun getItemCount(): Int = items
    }
}
