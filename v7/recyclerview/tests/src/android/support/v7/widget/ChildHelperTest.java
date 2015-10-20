/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.widget;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ChildHelperTest extends AndroidTestCase {
    LoggingCallback  mLoggingCallback;
    ChildHelper mChildHelper;

    @Before
    public void prepare() throws Exception {
        setUp();
        setContext(InstrumentationRegistry.getContext());
        mLoggingCallback = new LoggingCallback();
        mChildHelper = new ChildHelper(mLoggingCallback);
    }

    private RecyclerView.ViewHolder vh() {
        View view = new View(mContext);
        RecyclerViewBasicTest.MockViewHolder mockViewHolder
                = new RecyclerViewBasicTest.MockViewHolder(view);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(0 , 0);
        lp.mViewHolder = mockViewHolder;
        view.setLayoutParams(lp);
        return mockViewHolder;
    }

    @Test
    public void testAddChild() {
        RecyclerView.ViewHolder vh = vh();
        mChildHelper.addView(vh.itemView, false);
        assertEquals(1, mLoggingCallback.getChildCount());
        assertFalse(mChildHelper.isHidden(vh.itemView));
        assertEquals(0, mLoggingCallback.mOnEnteredHiddenState.size());
    }

    @Test
    public void testAddChildHidden() {
        RecyclerView.ViewHolder vh = vh();
        mChildHelper.addView(vh.itemView, true);
        assertEquals(1, mLoggingCallback.getChildCount());
        assertTrue(mChildHelper.isHidden(vh.itemView));
        assertTrue(mLoggingCallback.mOnEnteredHiddenState.contains(vh.itemView));
    }

    @Test
    public void testAddChildAndHide() {
        RecyclerView.ViewHolder vh = vh();
        mChildHelper.addView(vh.itemView, false);
        mChildHelper.hide(vh.itemView);
        assertTrue(mChildHelper.isHidden(vh.itemView));
        mChildHelper.unhide(vh.itemView);
        assertFalse(mChildHelper.isHidden(vh.itemView));
    }

    @Test
    public void testFindHiddenNonRemoved() {
        RecyclerView.ViewHolder vh = vh();
        vh.mPosition = 12;
        mChildHelper.addView(vh.itemView, true);
        assertSame(vh.itemView,
                mChildHelper.findHiddenNonRemovedView(12, RecyclerView.INVALID_TYPE));
    }

    @Test
    public void testFindHiddenRemoved() {
        RecyclerView.ViewHolder vh = vh();
        vh.mPosition = 12;
        vh.addFlags(RecyclerView.ViewHolder.FLAG_REMOVED);
        mChildHelper.addView(vh.itemView, true);
        assertNull(mChildHelper.findHiddenNonRemovedView(12, RecyclerView.INVALID_TYPE));
    }

    private static class LoggingCallback implements ChildHelper.Callback {
        List<View> mViews = new ArrayList<>();
        List<View> mDetached = new ArrayList<>();
        List<View> mOnEnteredHiddenState = new ArrayList<>();
        List<View> mOnExitedHiddenState = new ArrayList<>();
        @Override
        public int getChildCount() {
            return mViews.size();
        }

        @Override
        public void addView(View child, int index) {
            mViews.add(index, child);
        }

        @Override
        public int indexOfChild(View view) {
            return mViews.indexOf(view);
        }

        private boolean validateIndex(int index) {
            return index < getChildCount() && index >= 0;
        }

        @Override
        public void removeViewAt(int index) {
            if (validateIndex(index)) {
                mViews.remove(index);
            }
        }

        @Override
        public View getChildAt(int offset) {
            if (validateIndex(offset)) {
                return mViews.remove(offset);
            }
            return null;
        }

        @Override
        public void removeAllViews() {
            mViews.clear();
        }

        @Override
        public RecyclerView.ViewHolder getChildViewHolder(View view) {
            return RecyclerView.getChildViewHolderInt(view);
        }

        @Override
        public void attachViewToParent(View child, int index, ViewGroup.LayoutParams layoutParams) {
            assertTrue(mDetached.remove(child));
            addView(child, index);
        }

        @Override
        public void detachViewFromParent(int offset) {
            mDetached.add(getChildAt(offset));
        }

        @Override
        public void onEnteredHiddenState(View child) {
            mOnEnteredHiddenState.add(child);
        }

        @Override
        public void onLeftHiddenState(View child) {
            mOnExitedHiddenState.add(child);
        }

        public void clearHiddenStateLog() {
            mOnExitedHiddenState.clear();
            mOnEnteredHiddenState.clear();
        }
    }
}
