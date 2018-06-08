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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base class for animation related tests.
 */
public class BaseRecyclerViewAnimationsTest extends BaseRecyclerViewInstrumentationTest {

    protected static final boolean DEBUG = false;

    protected static final String TAG = "RecyclerViewAnimationsTest";

    AnimationLayoutManager mLayoutManager;

    TestAdapter mTestAdapter;

    public BaseRecyclerViewAnimationsTest() {
        super(DEBUG);
    }

    RecyclerView setupBasic(int itemCount) throws Throwable {
        return setupBasic(itemCount, 0, itemCount);
    }

    RecyclerView setupBasic(int itemCount, int firstLayoutStartIndex, int firstLayoutItemCount)
            throws Throwable {
        return setupBasic(itemCount, firstLayoutStartIndex, firstLayoutItemCount, null);
    }

    RecyclerView setupBasic(int itemCount, int firstLayoutStartIndex, int firstLayoutItemCount,
            TestAdapter testAdapter)
            throws Throwable {
        final TestRecyclerView recyclerView = new TestRecyclerView(getActivity());
        recyclerView.setHasFixedSize(true);
        if (testAdapter == null) {
            mTestAdapter = new TestAdapter(itemCount);
        } else {
            mTestAdapter = testAdapter;
        }
        recyclerView.setAdapter(mTestAdapter);
        recyclerView.setItemAnimator(createItemAnimator());
        mLayoutManager = new AnimationLayoutManager();
        recyclerView.setLayoutManager(mLayoutManager);
        mLayoutManager.mOnLayoutCallbacks.mLayoutMin = firstLayoutStartIndex;
        mLayoutManager.mOnLayoutCallbacks.mLayoutItemCount = firstLayoutItemCount;

        mLayoutManager.expectLayouts(1);
        recyclerView.expectDraw(1);
        setRecyclerView(recyclerView);
        mLayoutManager.waitForLayout(2);
        recyclerView.waitForDraw(1);
        mLayoutManager.mOnLayoutCallbacks.reset();
        getInstrumentation().waitForIdleSync();
        checkForMainThreadException();
        assertEquals("extra layouts should not happen", 1, mLayoutManager.getTotalLayoutCount());
        assertEquals("all expected children should be laid out", firstLayoutItemCount,
                mLayoutManager.getChildCount());
        return recyclerView;
    }

    protected RecyclerView.ItemAnimator createItemAnimator() {
        return new DefaultItemAnimator();
    }

    public TestRecyclerView getTestRecyclerView() {
        return (TestRecyclerView) mRecyclerView;
    }

    class AnimationLayoutManager extends TestLayoutManager {

        protected int mTotalLayoutCount = 0;
        private String log;

        OnLayoutCallbacks mOnLayoutCallbacks = new OnLayoutCallbacks() {
        };



        @Override
        public boolean supportsPredictiveItemAnimations() {
            return true;
        }

        public String getLog() {
            return log;
        }

        private String prepareLog(RecyclerView.Recycler recycler, RecyclerView.State state, boolean done) {
            StringBuilder builder = new StringBuilder();
            builder.append("is pre layout:").append(state.isPreLayout()).append(", done:").append(done);
            builder.append("\nViewHolders:\n");
            for (RecyclerView.ViewHolder vh : ((TestRecyclerView)mRecyclerView).collectViewHolders()) {
                builder.append(vh).append("\n");
            }
            builder.append("scrap:\n");
            for (RecyclerView.ViewHolder vh : recycler.getScrapList()) {
                builder.append(vh).append("\n");
            }

            if (state.isPreLayout() && !done) {
                log = "\n" + builder.toString();
            } else {
                log += "\n" + builder.toString();
            }
            return log;
        }

        @Override
        public void expectLayouts(int count) {
            super.expectLayouts(count);
            mOnLayoutCallbacks.mLayoutCount = 0;
        }

        public void setOnLayoutCallbacks(OnLayoutCallbacks onLayoutCallbacks) {
            mOnLayoutCallbacks = onLayoutCallbacks;
        }

        @Override
        public final void onLayoutChildren(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            try {
                mTotalLayoutCount++;
                prepareLog(recycler, state, false);
                if (state.isPreLayout()) {
                    validateOldPositions(recycler, state);
                } else {
                    validateClearedOldPositions(recycler, state);
                }
                mOnLayoutCallbacks.onLayoutChildren(recycler, this, state);
                prepareLog(recycler, state, true);
            } finally {
                layoutLatch.countDown();
            }
        }

        private void validateClearedOldPositions(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            if (getTestRecyclerView() == null) {
                return;
            }
            for (RecyclerView.ViewHolder viewHolder : getTestRecyclerView().collectViewHolders()) {
                assertEquals("there should NOT be an old position in post layout",
                        RecyclerView.NO_POSITION, viewHolder.mOldPosition);
                assertEquals("there should NOT be a pre layout position in post layout",
                        RecyclerView.NO_POSITION, viewHolder.mPreLayoutPosition);
            }
        }

        private void validateOldPositions(RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            if (getTestRecyclerView() == null) {
                return;
            }
            for (RecyclerView.ViewHolder viewHolder : getTestRecyclerView().collectViewHolders()) {
                if (!viewHolder.isRemoved() && !viewHolder.isInvalid()) {
                    assertTrue("there should be an old position in pre-layout",
                            viewHolder.mOldPosition != RecyclerView.NO_POSITION);
                }
            }
        }

        public int getTotalLayoutCount() {
            return mTotalLayoutCount;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            mOnLayoutCallbacks.onScroll(dy, recycler, state);
            return super.scrollVerticallyBy(dy, recycler, state);
        }

        public void onPostDispatchLayout() {
            mOnLayoutCallbacks.postDispatchLayout();
        }
    }

    abstract class OnLayoutCallbacks {

        int mLayoutMin = Integer.MIN_VALUE;

        int mLayoutItemCount = Integer.MAX_VALUE;

        int expectedPreLayoutItemCount = -1;

        int expectedPostLayoutItemCount = -1;

        int mDeletedViewCount;

        int mLayoutCount = 0;

        void setExpectedItemCounts(int preLayout, int postLayout) {
            expectedPreLayoutItemCount = preLayout;
            expectedPostLayoutItemCount = postLayout;
        }

        void reset() {
            mLayoutMin = Integer.MIN_VALUE;
            mLayoutItemCount = Integer.MAX_VALUE;
            expectedPreLayoutItemCount = -1;
            expectedPostLayoutItemCount = -1;
            mLayoutCount = 0;
        }

        void beforePreLayout(RecyclerView.Recycler recycler,
                AnimationLayoutManager lm, RecyclerView.State state) {
            mDeletedViewCount = 0;
            for (int i = 0; i < lm.getChildCount(); i++) {
                View v = lm.getChildAt(i);
                if (lm.getLp(v).isItemRemoved()) {
                    mDeletedViewCount++;
                }
            }
        }

        void doLayout(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {
            if (DEBUG) {
                Log.d(TAG, "item count " + state.getItemCount());
            }
            lm.detachAndScrapAttachedViews(recycler);
            final int start = mLayoutMin == Integer.MIN_VALUE ? 0 : mLayoutMin;
            final int count = mLayoutItemCount
                    == Integer.MAX_VALUE ? state.getItemCount() : mLayoutItemCount;
            lm.layoutRange(recycler, start, start + count);
            assertEquals("correct # of children should be laid out",
                    count, lm.getChildCount());
            lm.assertVisibleItemPositions();
        }

        private void assertNoPreLayoutPosition(RecyclerView.Recycler recycler) {
            for (RecyclerView.ViewHolder vh : recycler.mAttachedScrap) {
                assertPreLayoutPosition(vh);
            }
        }

        private void assertNoPreLayoutPosition(RecyclerView.LayoutManager lm) {
            for (int i = 0; i < lm.getChildCount(); i ++) {
                final RecyclerView.ViewHolder vh = mRecyclerView
                        .getChildViewHolder(lm.getChildAt(i));
                assertPreLayoutPosition(vh);
            }
        }

        private void assertPreLayoutPosition(RecyclerView.ViewHolder vh) {
            assertEquals("in post layout, there should not be a view holder w/ a pre "
                    + "layout position", RecyclerView.NO_POSITION, vh.mPreLayoutPosition);
            assertEquals("in post layout, there should not be a view holder w/ an old "
                    + "layout position", RecyclerView.NO_POSITION, vh.mOldPosition);
        }

        void onLayoutChildren(RecyclerView.Recycler recycler, AnimationLayoutManager lm,
                RecyclerView.State state) {
            if (state.isPreLayout()) {
                if (expectedPreLayoutItemCount != -1) {
                    assertEquals("on pre layout, state should return abstracted adapter size",
                            expectedPreLayoutItemCount, state.getItemCount());
                }
                beforePreLayout(recycler, lm, state);
            } else {
                if (expectedPostLayoutItemCount != -1) {
                    assertEquals("on post layout, state should return real adapter size",
                            expectedPostLayoutItemCount, state.getItemCount());
                }
                beforePostLayout(recycler, lm, state);
            }
            if (!state.isPreLayout()) {
                assertNoPreLayoutPosition(recycler);
            }
            doLayout(recycler, lm, state);
            if (state.isPreLayout()) {
                afterPreLayout(recycler, lm, state);
            } else {
                afterPostLayout(recycler, lm, state);
                assertNoPreLayoutPosition(lm);
            }
            mLayoutCount++;
        }

        void afterPreLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void beforePostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void afterPostLayout(RecyclerView.Recycler recycler, AnimationLayoutManager layoutManager,
                RecyclerView.State state) {
        }

        void postDispatchLayout() {
        }

        public void onScroll(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {

        }
    }

    class TestRecyclerView extends RecyclerView {

        CountDownLatch drawLatch;

        public TestRecyclerView(Context context) {
            super(context);
        }

        public TestRecyclerView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TestRecyclerView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        void initAdapterManager() {
            super.initAdapterManager();
            mAdapterHelper.mOnItemProcessedCallback = new Runnable() {
                @Override
                public void run() {
                    validatePostUpdateOp();
                }
            };
        }

        @Override
        boolean isAccessibilityEnabled() {
            return true;
        }

        public void expectDraw(int count) {
            drawLatch = new CountDownLatch(count);
        }

        public void waitForDraw(long timeout) throws Throwable {
            drawLatch.await(timeout * (DEBUG ? 100 : 1), TimeUnit.SECONDS);
            assertEquals("all expected draws should happen at the expected time frame",
                    0, drawLatch.getCount());
        }

        List<ViewHolder> collectViewHolders() {
            List<ViewHolder> holders = new ArrayList<ViewHolder>();
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                ViewHolder holder = getChildViewHolderInt(getChildAt(i));
                if (holder != null) {
                    holders.add(holder);
                }
            }
            return holders;
        }


        private void validateViewHolderPositions() {
            final Set<Integer> existingOffsets = new HashSet<Integer>();
            int childCount = getChildCount();
            StringBuilder log = new StringBuilder();
            for (int i = 0; i < childCount; i++) {
                ViewHolder vh = getChildViewHolderInt(getChildAt(i));
                TestViewHolder tvh = (TestViewHolder) vh;
                log.append(tvh.mBoundItem).append(vh)
                        .append(" hidden:")
                        .append(mChildHelper.mHiddenViews.contains(vh.itemView))
                        .append("\n");
            }
            for (int i = 0; i < childCount; i++) {
                ViewHolder vh = getChildViewHolderInt(getChildAt(i));
                if (vh.isInvalid()) {
                    continue;
                }
                if (vh.getLayoutPosition() < 0) {
                    LayoutManager lm = getLayoutManager();
                    for (int j = 0; j < lm.getChildCount(); j ++) {
                        assertNotSame("removed view holder should not be in LM's child list",
                                vh.itemView, lm.getChildAt(j));
                    }
                } else if (!mChildHelper.mHiddenViews.contains(vh.itemView)) {
                    if (!existingOffsets.add(vh.getLayoutPosition())) {
                        throw new IllegalStateException("view holder position conflict for "
                                + "existing views " + vh + "\n" + log);
                    }
                }
            }
        }

        void validatePostUpdateOp() {
            try {
                validateViewHolderPositions();
                if (super.mState.isPreLayout()) {
                    validatePreLayoutSequence((AnimationLayoutManager) getLayoutManager());
                }
                validateAdapterPosition((AnimationLayoutManager) getLayoutManager());
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }
        }



        private void validateAdapterPosition(AnimationLayoutManager lm) {
            for (ViewHolder vh : collectViewHolders()) {
                if (!vh.isRemoved() && vh.mPreLayoutPosition >= 0) {
                    assertEquals("adapter position calculations should match view holder "
                                    + "pre layout:" + mState.isPreLayout()
                                    + " positions\n" + vh + "\n" + lm.getLog(),
                            mAdapterHelper.findPositionOffset(vh.mPreLayoutPosition), vh.mPosition);
                }
            }
        }

        // ensures pre layout positions are continuous block. This is not necessarily a case
        // but valid in test RV
        private void validatePreLayoutSequence(AnimationLayoutManager lm) {
            Set<Integer> preLayoutPositions = new HashSet<Integer>();
            for (ViewHolder vh : collectViewHolders()) {
                assertTrue("pre layout positions should be distinct " + lm.getLog(),
                        preLayoutPositions.add(vh.mPreLayoutPosition));
            }
            int minPos = Integer.MAX_VALUE;
            for (Integer pos : preLayoutPositions) {
                if (pos < minPos) {
                    minPos = pos;
                }
            }
            for (int i = 1; i < preLayoutPositions.size(); i++) {
                assertNotNull("next position should exist " + lm.getLog(),
                        preLayoutPositions.contains(minPos + i));
            }
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (drawLatch != null) {
                drawLatch.countDown();
            }
        }

        @Override
        void dispatchLayout() {
            try {
                super.dispatchLayout();
                if (getLayoutManager() instanceof AnimationLayoutManager) {
                    ((AnimationLayoutManager) getLayoutManager()).onPostDispatchLayout();
                }
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            }

        }


    }

    abstract class AdapterOps {

        final public void run(TestAdapter adapter) throws Throwable {
            onRun(adapter);
        }

        abstract void onRun(TestAdapter testAdapter) throws Throwable;
    }

    static class CollectPositionResult {

        // true if found in scrap
        public RecyclerView.ViewHolder scrapResult;

        public RecyclerView.ViewHolder adapterResult;

        static CollectPositionResult fromScrap(RecyclerView.ViewHolder viewHolder) {
            CollectPositionResult cpr = new CollectPositionResult();
            cpr.scrapResult = viewHolder;
            return cpr;
        }

        static CollectPositionResult fromAdapter(RecyclerView.ViewHolder viewHolder) {
            CollectPositionResult cpr = new CollectPositionResult();
            cpr.adapterResult = viewHolder;
            return cpr;
        }

        @Override
        public String toString() {
            return "CollectPositionResult{" +
                    "scrapResult=" + scrapResult +
                    ", adapterResult=" + adapterResult +
                    '}';
        }
    }

    static class PositionConstraint {

        public static enum Type {
            scrap,
            adapter,
            adapterScrap /*first pass adapter, second pass scrap*/
        }

        Type mType;

        int mOldPos; // if VH

        int mPreLayoutPos;

        int mPostLayoutPos;

        int mValidateCount = 0;

        public static PositionConstraint scrap(int oldPos, int preLayoutPos, int postLayoutPos) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.scrap;
            constraint.mOldPos = oldPos;
            constraint.mPreLayoutPos = preLayoutPos;
            constraint.mPostLayoutPos = postLayoutPos;
            return constraint;
        }

        public static PositionConstraint adapterScrap(int preLayoutPos, int position) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.adapterScrap;
            constraint.mOldPos = RecyclerView.NO_POSITION;
            constraint.mPreLayoutPos = preLayoutPos;
            constraint.mPostLayoutPos = position;// adapter pos does not change
            return constraint;
        }

        public static PositionConstraint adapter(int position) {
            PositionConstraint constraint = new PositionConstraint();
            constraint.mType = Type.adapter;
            constraint.mPreLayoutPos = RecyclerView.NO_POSITION;
            constraint.mOldPos = RecyclerView.NO_POSITION;
            constraint.mPostLayoutPos = position;// adapter pos does not change
            return constraint;
        }

        public void assertValidate() {
            int expectedValidate = 0;
            if (mPreLayoutPos >= 0) {
                expectedValidate ++;
            }
            if (mPostLayoutPos >= 0) {
                expectedValidate ++;
            }
            assertEquals("should run all validates", expectedValidate, mValidateCount);
        }

        @Override
        public String toString() {
            return "Cons{" +
                    "t=" + mType.name() +
                    ", old=" + mOldPos +
                    ", pre=" + mPreLayoutPos +
                    ", post=" + mPostLayoutPos +
                    '}';
        }

        public void validate(RecyclerView.State state, CollectPositionResult result, String log) {
            mValidateCount ++;
            assertNotNull(this + ": result should not be null\n" + log, result);
            RecyclerView.ViewHolder viewHolder;
            if (mType == Type.scrap || (mType == Type.adapterScrap && !state.isPreLayout())) {
                assertNotNull(this + ": result should come from scrap\n" + log, result.scrapResult);
                viewHolder = result.scrapResult;
            } else {
                assertNotNull(this + ": result should come from adapter\n"  + log,
                        result.adapterResult);
                assertEquals(this + ": old position should be none when it came from adapter\n" + log,
                        RecyclerView.NO_POSITION, result.adapterResult.getOldPosition());
                viewHolder = result.adapterResult;
            }
            if (state.isPreLayout()) {
                assertEquals(this + ": pre-layout position should match\n" + log, mPreLayoutPos,
                        viewHolder.mPreLayoutPosition == -1 ? viewHolder.mPosition :
                                viewHolder.mPreLayoutPosition);
                assertEquals(this + ": pre-layout getPosition should match\n" + log, mPreLayoutPos,
                        viewHolder.getLayoutPosition());
                if (mType == Type.scrap) {
                    assertEquals(this + ": old position should match\n" + log, mOldPos,
                            result.scrapResult.getOldPosition());
                }
            } else if (mType == Type.adapter || mType == Type.adapterScrap || !result.scrapResult
                    .isRemoved()) {
                assertEquals(this + ": post-layout position should match\n" + log + "\n\n"
                        + viewHolder, mPostLayoutPos, viewHolder.getLayoutPosition());
            }
        }
    }

    static class LoggingInfo extends RecyclerView.ItemAnimator.ItemHolderInfo {
        final RecyclerView.ViewHolder viewHolder;
        @RecyclerView.ItemAnimator.AdapterChanges
        final int changeFlags;
        final List<Object> payloads;

        LoggingInfo(RecyclerView.ViewHolder viewHolder, int changeFlags, List<Object> payloads) {
            this.viewHolder = viewHolder;
            this.changeFlags = changeFlags;
            if (payloads != null) {
                this.payloads = new ArrayList<>();
                this.payloads.addAll(payloads);
            } else {
                this.payloads = null;
            }
            setFrom(viewHolder);
        }

        @Override
        public String toString() {
            return "LoggingInfo{" +
                    "changeFlags=" + changeFlags +
                    ", payloads=" + payloads +
                    '}';
        }
    }

    static class AnimateChange extends AnimateLogBase {

        final RecyclerView.ViewHolder newHolder;

        public AnimateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder,
                LoggingInfo pre, LoggingInfo post) {
            super(oldHolder, pre, post);
            this.newHolder = newHolder;
        }
    }

    static class AnimatePersistence extends AnimateLogBase {

        public AnimatePersistence(RecyclerView.ViewHolder viewHolder, LoggingInfo pre,
                LoggingInfo post) {
            super(viewHolder, pre, post);
        }
    }

    static class AnimateAppearance extends AnimateLogBase {
        public AnimateAppearance(RecyclerView.ViewHolder viewHolder, LoggingInfo pre,
                LoggingInfo post) {
            super(viewHolder, pre, post);
        }
    }

    static class AnimateDisappearance extends AnimateLogBase {
        public AnimateDisappearance(RecyclerView.ViewHolder viewHolder, LoggingInfo pre,
                LoggingInfo post) {
            super(viewHolder, pre, post);
        }
    }
    static class AnimateLogBase {

        public final RecyclerView.ViewHolder viewHolder;
        public final LoggingInfo preInfo;
        public final LoggingInfo postInfo;

        public AnimateLogBase(RecyclerView.ViewHolder viewHolder, LoggingInfo pre,
                LoggingInfo postInfo) {
            this.viewHolder = viewHolder;
            this.preInfo = pre;
            this.postInfo = postInfo;
        }

        public String log() {
            return getClass().getSimpleName() + "[" +  log(preInfo) + " - " + log(postInfo) + "]";
        }

        public String log(LoggingInfo info) {
            return info == null ? "null" : info.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AnimateLogBase that = (AnimateLogBase) o;

            if (viewHolder != null ? !viewHolder.equals(that.viewHolder)
                    : that.viewHolder != null) {
                return false;
            }
            if (preInfo != null ? !preInfo.equals(that.preInfo) : that.preInfo != null) {
                return false;
            }
            return !(postInfo != null ? !postInfo.equals(that.postInfo) : that.postInfo != null);

        }

        @Override
        public int hashCode() {
            int result = viewHolder != null ? viewHolder.hashCode() : 0;
            result = 31 * result + (preInfo != null ? preInfo.hashCode() : 0);
            result = 31 * result + (postInfo != null ? postInfo.hashCode() : 0);
            return result;
        }
    }
}
