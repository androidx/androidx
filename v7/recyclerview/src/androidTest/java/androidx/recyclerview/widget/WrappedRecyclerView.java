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

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.util.AttributeSet;

import androidx.core.view.ViewCompat;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * RecyclerView wrapper used in tests. This class can fake behavior like layout direction w/o
 * playing with framework support.
 */
public class WrappedRecyclerView extends RecyclerView {

    Boolean mFakeRTL;
    private long mDrawingTimeOffsetMs;

    public void setFakeRTL(Boolean fakeRTL) {
        mFakeRTL = fakeRTL;
    }

    public void setDrawingTimeOffset(long offsetMs) {
        mDrawingTimeOffsetMs = offsetMs;
    }

    @Override
    public long getDrawingTime() {
        return super.getDrawingTime() + mDrawingTimeOffsetMs;
    }

    public WrappedRecyclerView(Context context) {
        super(context);
        init(context);
    }

    public WrappedRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public WrappedRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        //initializeScrollbars(null);
    }

    public void waitUntilLayout() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        while (isLayoutRequested()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitUntilAnimations() throws InterruptedException {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        final CountDownLatch latch = new CountDownLatch(1);
        if (mItemAnimator == null || !mItemAnimator.isRunning(
                new ItemAnimator.ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        latch.countDown();
                    }
                })) {
            latch.countDown();
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        MatcherAssert.assertThat("waiting too long for animations",
                latch.await(60, TimeUnit.SECONDS), CoreMatchers.is(true));
    }


    @Override
    public int getLayoutDirection() {
        if (mFakeRTL == null) {
            return super.getLayoutDirection();
        }
        //noinspection WrongConstant
        return Boolean.TRUE.equals(mFakeRTL) ? ViewCompat.LAYOUT_DIRECTION_RTL
                : ViewCompat.LAYOUT_DIRECTION_LTR;
    }

    @Override
    public boolean setChildImportantForAccessibilityInternal(ViewHolder viewHolder,
            int importantForAccessibilityBeforeHidden) {
        return super.setChildImportantForAccessibilityInternal(viewHolder,
                importantForAccessibilityBeforeHidden);
    }
}