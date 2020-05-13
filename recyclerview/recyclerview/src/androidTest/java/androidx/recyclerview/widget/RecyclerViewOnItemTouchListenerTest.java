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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewOnItemTouchListenerTest {

    private FrameLayout mParent;
    private RecyclerView mRecyclerView;
    private MyView mChildView;
    private MyOnItemTouchListener mOnItemTouchListener;
    private MotionEvent mActionDown;
    private MotionEvent mActionMove1;
    private MotionEvent mActionMove2;
    private MotionEvent mActionMove3;
    private MotionEvent mActionUp;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();

        mChildView = spy(new MyView(context));
        mChildView.setMinimumWidth(1000);
        mChildView.setMinimumHeight(1000);

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));

        MyAdapter myAdapter = new MyAdapter(context, mChildView);
        mRecyclerView.setAdapter(myAdapter);

        mOnItemTouchListener = spy(new MyOnItemTouchListener());
        mRecyclerView.addOnItemTouchListener(mOnItemTouchListener);

        mParent = spy(new FrameLayout(context));
        mParent.addView(mRecyclerView);

        int measureSpec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY);
        mParent.measure(measureSpec, measureSpec);
        mParent.layout(0, 0, 1000, 1000);

        mActionDown = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 500f, 500f, 0);
        mActionMove1 = MotionEvent.obtain(0, 100, MotionEvent.ACTION_MOVE, 500f, 400f, 0);
        mActionMove2 = MotionEvent.obtain(0, 200, MotionEvent.ACTION_MOVE, 500f, 300f, 0);
        mActionMove3 = MotionEvent.obtain(0, 300, MotionEvent.ACTION_MOVE, 500f, 200f, 0);
        mActionUp = MotionEvent.obtain(0, 400, MotionEvent.ACTION_UP, 500f, 200f, 0);
    }

    @Test
    public void listenerDoesntIntercept_rvChildDoesntClick_correctListenerCalls() {
        listenerDoesntIntercept_correctListenerCalls(false);
    }

    @Test
    public void listenerDoesntIntercept_rvChildClicks_correctListenerCalls() {
        listenerDoesntIntercept_correctListenerCalls(true);
    }

    private void listenerDoesntIntercept_correctListenerCalls(boolean childClickable) {
        mChildView.setClickable(childClickable);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionMove1);
        mParent.dispatchTouchEvent(mActionUp);

        InOrder inOrder = inOrder(mOnItemTouchListener);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionDown);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionMove1);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionUp);
        verifyNoMoreInteractions(mOnItemTouchListener);
    }

    @Test
    public void listenerInterceptsDown_rvChildClicks_correctListenerCalls() {
        listenerInterceptsDown_correctListenerCalls(false);
    }

    @Test
    public void listenerInterceptsDown_rvChildDoesntClick_correctListenerCalls() {
        listenerInterceptsDown_correctListenerCalls(true);
    }

    private void listenerInterceptsDown_correctListenerCalls(boolean childClickable) {
        mChildView.setClickable(childClickable);
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionDown))
                .thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionMove1);
        mParent.dispatchTouchEvent(mActionUp);

        InOrder inOrder = inOrder(mOnItemTouchListener);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionDown);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionDown);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionMove1);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionUp);
        verifyNoMoreInteractions(mOnItemTouchListener);
    }

    @Test
    public void listenerInterceptsMove_rvChildDoesntClick_correctListenerCalls() {
        listenerInterceptsMove_correctListenerCalls(false);
    }

    @Test
    public void listenerInterceptsMove_rvChildClicks_correctListenerCalls() {
        listenerInterceptsMove_correctListenerCalls(true);
    }

    public void listenerInterceptsMove_correctListenerCalls(boolean childClickable) {
        mChildView.setClickable(childClickable);
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionMove1))
                .thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionMove1);
        mParent.dispatchTouchEvent(mActionMove2);
        mParent.dispatchTouchEvent(mActionUp);

        InOrder inOrder = inOrder(mOnItemTouchListener);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionDown);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionMove1);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionMove2);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionUp);
        verifyNoMoreInteractions(mOnItemTouchListener);
    }

    @Test
    public void listenerInterceptsDown_childOnTouchNotCalled() {
        mChildView.setClickable(true);
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionDown))
                .thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionUp);

        verify(mChildView, never()).onTouchEvent(any(MotionEvent.class));
    }

    @Test
    public void listenerInterceptsMove_childOnTouchCalledWithCorrectEvents() {
        mChildView.setClickable(true);
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionMove1))
                .thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionMove1);

        verify(mChildView).onTouchEvent(mActionDown);
        assertThat(mChildView.mLastAction, is(MotionEvent.ACTION_CANCEL));
    }

    @Test
    public void listenerInterceptsUp_childOnTouchCalledWithCorrectEvents() {
        mChildView.setClickable(true);
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionUp))
                .thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionUp);

        verify(mChildView, times(2)).onTouchEvent(any(MotionEvent.class));
        verify(mChildView).onTouchEvent(mActionDown);
        assertThat(mChildView.mLastAction, is(MotionEvent.ACTION_CANCEL));
    }

    @Test
    public void listenerInterceptsThenParentIntercepts_correctListenerCalls() {
        when(mOnItemTouchListener
                .onInterceptTouchEvent(mRecyclerView, mActionMove1))
                .thenReturn(true);
        when(mParent.onInterceptTouchEvent(mActionMove3)).thenReturn(true);

        mParent.dispatchTouchEvent(mActionDown);
        mParent.dispatchTouchEvent(mActionMove1);
        mParent.dispatchTouchEvent(mActionMove2);
        mParent.dispatchTouchEvent(mActionMove3);
        mParent.dispatchTouchEvent(mActionUp);

        InOrder inOrder = inOrder(mOnItemTouchListener);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionDown);
        inOrder.verify(mOnItemTouchListener).onInterceptTouchEvent(mRecyclerView, mActionMove1);
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionMove2);

        // Mockito thinks mActionMove3 was passed because it is checking by reference while the
        // framework actually mutates the MotionEvent behind the scenes and changes ACTION_MOVE
        // to ACTION_CANCEL and back to ACTION_MOVE.  Hence the extra assertThat and logic.
        inOrder.verify(mOnItemTouchListener).onTouchEvent(mRecyclerView, mActionMove3);
        assertThat(mOnItemTouchListener.mLastAction, is(MotionEvent.ACTION_CANCEL));

        verifyNoMoreInteractions(mOnItemTouchListener);
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {

        Context mContext;
        View mView;

        MyAdapter(Context context, View view) {
            mContext = context;
            mView = view;
        }

        @Override
        @NonNull
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            return new MyViewHolder(mView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder,
                int position) {
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    private static class MyViewHolder extends RecyclerView.ViewHolder {
        MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class MyOnItemTouchListener implements RecyclerView.OnItemTouchListener {

        int mLastAction = -1;

        @Override
        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            return false;
        }

        @Override
        public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
            mLastAction = e.getAction();
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }

    public class MyView extends View {

        public int mLastAction = -1;

        public MyView(Context context) {
            super(context);
        }

        public MyView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public MyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            mLastAction = event.getAction();
            return super.onTouchEvent(event);
        }
    }
}
