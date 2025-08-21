/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.util.VelocityTrackerAddPointsFix
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import kotlin.coroutines.resume
import kotlin.math.absoluteValue
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class VelocityTrackingListParityTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private var layoutManager: LinearLayoutManager? = null
    private var latestComposeVelocity = 0f
    private var latestRVState = -1

    @OptIn(ExperimentalComposeUiApi::class)
    @Before
    fun setUp() {
        layoutManager = null
        latestComposeVelocity = 0f
        VelocityTrackerAddPointsFix = true
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_smallVeryFast() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        smallGestureVeryFast(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_smallFast() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        smallGestureFast(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_smallSlow() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        smallGestureSlow(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_largeFast() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        largeGestureFast(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_largeVeryFast() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        largeGestureVeryFast(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_orthogonal() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        orthogonalGesture(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_regularGestureOne() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        regularGestureOne(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    @Test
    fun equalLists_withEqualFlings_shouldFinishAtTheSameItem_regularGestureTwo() = runBlocking {
        val state = LazyListState()

        // starting with view
        createActivity(state)
        checkVisibility(composeView(), View.GONE)
        checkVisibility(recyclerView(), View.VISIBLE)

        regularGestureTwo(R.id.view_list)
        rule.waitForIdle()
        recyclerView().awaitScrollIdle()

        val childAtTheTopOfView = layoutManager?.findFirstVisibleItemPosition() ?: 0

        // switch visibilities
        rule.runOnUiThread {
            rule.activity.findViewById<RecyclerView>(R.id.view_list).visibility = View.GONE
            rule.activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.VISIBLE
        }

        checkVisibility(composeView(), View.VISIBLE)
        checkVisibility(recyclerView(), View.GONE)

        assertTrue { isValidGesture(recyclerView().motionEvents.filterNotNull()) }

        // Inject the same events in compose view
        rule.runOnUiThread {
            for (event in recyclerView().motionEvents) {
                composeView().dispatchTouchEvent(event)
            }
        }

        rule.runOnIdle {
            val currentTopInCompose = state.firstVisibleItemIndex
            val diff = (currentTopInCompose - childAtTheTopOfView).absoluteValue
            val message =
                "Compose=$currentTopInCompose View=$childAtTheTopOfView " + "Difference was=$diff"
            assertTrue(message) { diff <= ItemDifferenceThreshold }
        }
    }

    private fun createActivity(state: LazyListState) {
        rule.activityRule.scenario.createActivityWithComposeContent(
            R.layout.android_compose_lists_fling
        ) {
            TestComposeList(state)
        }
    }

    private fun ActivityScenario<*>.createActivityWithComposeContent(
        @LayoutRes layout: Int,
        content: @Composable () -> Unit,
    ) {
        onActivity { activity ->
            activity.setTheme(R.style.Theme_MaterialComponents_Light)
            activity.setContentView(layout)
            with(activity.findViewById<ComposeView>(R.id.compose_view)) { setContent(content) }

            activity.findViewById<RecyclerView>(R.id.view_list)?.let {
                it.adapter = ListAdapter()
                it.layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false).also {
                        this@VelocityTrackingListParityTest.layoutManager = it
                    }
                it.addOnScrollListener(
                    object : OnScrollListener() {
                        override fun onScrollStateChanged(
                            recyclerView: RecyclerView,
                            newState: Int,
                        ) {
                            latestRVState = newState
                        }
                    }
                )
            }

            activity.findViewById<ComposeView>(R.id.compose_view).visibility = View.GONE
        }
        moveToState(Lifecycle.State.RESUMED)
    }

    private fun recyclerView(): RecyclerViewWithMotionEvents =
        rule.activity.findViewById(R.id.view_list)

    private fun composeView(): ComposeView = rule.activity.findViewById(R.id.compose_view)

    private fun checkVisibility(view: View, visibility: Int) {
        assertTrue { view.visibility == visibility }
    }
}

@Composable
fun TestComposeList(state: LazyListState) {
    LazyColumn(Modifier.fillMaxSize(), state = state) {
        items(2000) {
            Box(modifier = Modifier.fillMaxWidth().height(64.dp).background(Color.Black)) {
                Text(text = it.toString(), color = Color.White)
            }
        }
    }
}

private class ListAdapter : RecyclerView.Adapter<ListViewHolder>() {
    val items = (0 until 2000).map { it.toString() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        return ListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.android_compose_lists_fling_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}

private class ListViewHolder(val view: View) : ViewHolder(view) {
    fun bind(position: String) {
        view.findViewById<TextView>(R.id.textView).text = position
    }
}

class RecyclerViewWithMotionEvents(context: Context, attributeSet: AttributeSet) :
    RecyclerView(context, attributeSet) {

    val motionEvents = mutableListOf<MotionEvent?>()

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        motionEvents.add(MotionEvent.obtain(event))
        return super.onTouchEvent(event)
    }
}

private suspend fun RecyclerView.awaitScrollIdle() {
    val rv = this
    withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val listener =
                object : OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            continuation.resume(Unit)
                        }
                    }
                }

            rv.addOnScrollListener(listener)

            continuation.invokeOnCancellation { rv.removeOnScrollListener(listener) }

            if (rv.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                continuation.resume(Unit)
            }
        }
    }
}

private const val ItemDifferenceThreshold = 1
