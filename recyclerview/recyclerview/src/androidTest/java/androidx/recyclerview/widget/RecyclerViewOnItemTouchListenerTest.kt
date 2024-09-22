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
package androidx.recyclerview.widget

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

val ActionDown = MotionEventItem(0, ACTION_DOWN, 500f, 500f)
val ActionMove1 = MotionEventItem(100, ACTION_MOVE, 500f, 400f)
val ActionMove2 = MotionEventItem(200, ACTION_MOVE, 500f, 300f)
val ActionMove3 = MotionEventItem(300, ACTION_MOVE, 500f, 200f)
val ActionUp = MotionEventItem(400, ACTION_UP, 500f, 200f)

@RunWith(AndroidJUnit4::class)
@LargeTest
class RecyclerViewOnItemTouchListenerTest {
    private lateinit var parent: MyFrameLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var childView: MyView
    private lateinit var onItemTouchListener: MyOnItemTouchListener

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        childView = MyView(context)
        childView.minimumWidth = 1000
        childView.minimumHeight = 1000

        recyclerView = RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = InternalTestAdapter(childView)

        onItemTouchListener = MyOnItemTouchListener()
        recyclerView.addOnItemTouchListener(onItemTouchListener)

        parent = MyFrameLayout(context)
        parent.addView(recyclerView)

        val measureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY)
        parent.measure(measureSpec, measureSpec)
        parent.layout(0, 0, 1000, 1000)
    }

    @Test
    fun listenerDoesntIntercept_rvChildDoesntClick_correctListenerCalls() {
        listenerDoesntIntercept_correctListenerCalls(false)
    }

    @Test
    fun listenerDoesntIntercept_rvChildClicks_correctListenerCalls() {
        listenerDoesntIntercept_correctListenerCalls(true)
    }

    private fun listenerDoesntIntercept_correctListenerCalls(childClickable: Boolean) {
        childView.isClickable = childClickable

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionMove1)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(onItemTouchListener.motionEventItems)
            .isEqualTo(listOf(ActionDown to true, ActionMove1 to true, ActionUp to true))
    }

    @Test
    fun listenerInterceptsDown_rvChildClicks_correctListenerCalls() {
        listenerInterceptsDown_correctListenerCalls(false)
    }

    @Test
    fun listenerInterceptsDown_rvChildDoesntClick_correctListenerCalls() {
        listenerInterceptsDown_correctListenerCalls(true)
    }

    private fun listenerInterceptsDown_correctListenerCalls(childClickable: Boolean) {
        childView.isClickable = childClickable

        onItemTouchListener.motionEventItemToStartIntecepting = ActionDown

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionMove1)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(onItemTouchListener.motionEventItems)
            .isEqualTo(
                listOf(
                    ActionDown to true,
                    ActionDown to false,
                    ActionMove1 to false,
                    ActionUp to false
                )
            )
    }

    @Test
    fun listenerInterceptsMove_rvChildDoesntClick_correctListenerCalls() {
        listenerInterceptsMove_correctListenerCalls(false)
    }

    @Test
    fun listenerInterceptsMove_rvChildClicks_correctListenerCalls() {
        listenerInterceptsMove_correctListenerCalls(true)
    }

    @Test
    fun listenerInterceptsMove_rvChildClicks_correctListenerCallsAndSendsCancel() {
        val actionCancelFromMove1 = MotionEventItem(100, ACTION_CANCEL, 500f, 400f)
        val secondOnItemTouchListener = MyOnItemTouchListener()
        recyclerView.addOnItemTouchListener(secondOnItemTouchListener)

        listenerInterceptsMove_correctListenerCalls(true)

        assertThat(secondOnItemTouchListener.motionEventItems)
            .isEqualTo(listOf(ActionDown to true, actionCancelFromMove1 to true))
    }

    private fun listenerInterceptsMove_correctListenerCalls(childClickable: Boolean) {
        childView.isClickable = childClickable
        onItemTouchListener.motionEventItemToStartIntecepting = ActionMove1

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionMove1)
        parent.dispatchMotionEventItem(ActionMove2)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(onItemTouchListener.motionEventItems)
            .isEqualTo(
                listOf(
                    ActionDown to true,
                    ActionMove1 to true,
                    ActionMove2 to false,
                    ActionUp to false
                )
            )
    }

    @Test
    fun listenerInterceptsDown_childOnTouchNotCalled() {
        childView.isClickable = true
        onItemTouchListener.motionEventItemToStartIntecepting = ActionDown

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(childView.motionEventItems).isEmpty()
    }

    @Test
    fun listenerInterceptsMove_childOnTouchCalledWithCorrectEvents() {
        childView.isClickable = true
        onItemTouchListener.motionEventItemToStartIntecepting = ActionMove1

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionMove1)

        assertThat(childView.motionEventItems)
            .isEqualTo(listOf(ActionDown, ActionMove1.toCancelledVersion()))
    }

    @Test
    fun listenerInterceptsUp_childOnTouchCalledWithCorrectEvents() {
        childView.isClickable = true
        onItemTouchListener.motionEventItemToStartIntecepting = ActionUp

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(childView.motionEventItems)
            .isEqualTo(listOf(ActionDown, ActionUp.toCancelledVersion()))
    }

    @Test
    fun listenerInterceptsThenParentIntercepts_correctListenerCalls() {
        onItemTouchListener.motionEventItemToStartIntecepting = ActionMove1
        parent.motionEventItemToStartIntecepting = ActionMove3

        parent.dispatchMotionEventItem(ActionDown)
        parent.dispatchMotionEventItem(ActionMove1)
        parent.dispatchMotionEventItem(ActionMove2)
        parent.dispatchMotionEventItem(ActionMove3)
        parent.dispatchMotionEventItem(ActionUp)

        assertThat(onItemTouchListener.motionEventItems)
            .isEqualTo(
                listOf(
                    ActionDown to true,
                    ActionMove1 to true,
                    ActionMove2 to false,
                    ActionMove3.toCancelledVersion() to false
                )
            )
    }
}

data class MotionEventItem(val eventTime: Long, val action: Int, val x: Float, val y: Float)

private fun MotionEventItem.toMotionEvent(): MotionEvent =
    MotionEvent.obtain(0, eventTime, action, x, y, 0)

private fun MotionEventItem.toCancelledVersion() = copy(action = ACTION_CANCEL)

private fun MotionEvent.toMotionEventItem() = MotionEventItem(eventTime, actionMasked, x, y)

private fun View.dispatchMotionEventItem(motionEventItem: MotionEventItem) {
    motionEventItem.toMotionEvent().also {
        dispatchTouchEvent(it)
        it.recycle()
    }
}

private class InternalTestAdapter(var view: View) : RecyclerView.Adapter<MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MyViewHolder(view)

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {}

    override fun getItemCount() = 1
}

private class MyViewHolder internal constructor(itemView: View?) :
    RecyclerView.ViewHolder(itemView!!)

private class MyOnItemTouchListener : RecyclerView.OnItemTouchListener {
    var motionEventItemToStartIntecepting: MotionEventItem? = null
    var motionEventItems = mutableListOf<Pair<MotionEventItem, Boolean>>()

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        motionEventItems.add(e.toMotionEventItem() to true)
        return e.toMotionEventItem() == motionEventItemToStartIntecepting
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        motionEventItems.add(e.toMotionEventItem() to false)
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
}

private class MyView : View {
    var motionEventItems = mutableListOf<MotionEventItem>()

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionEventItems.add(event.toMotionEventItem())
        return super.onTouchEvent(event)
    }
}

private class MyFrameLayout(context: Context) : FrameLayout(context) {

    var motionEventItemToStartIntecepting: MotionEventItem? = null

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        super.onInterceptTouchEvent(ev)
        return ev!!.toMotionEventItem() == motionEventItemToStartIntecepting
    }
}
