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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.util.Pair;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.ScrollFeedbackProviderCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unchecked")
@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecyclerViewBasicTest {

    RecyclerView mRecyclerView;

    ScrollFeedbackProviderCompat mScrollFeedbackProvider;

    @Before
    public void setUp() throws Exception {
        mRecyclerView = new RecyclerView(getContext());
    }

    private Context getContext() {
        return ApplicationProvider.getApplicationContext();
    }

    @Test
    public void measureWithoutLayoutManager() {
        measure();
    }

    private void measure() {
        mRecyclerView.measure(View.MeasureSpec.AT_MOST | 320, View.MeasureSpec.AT_MOST | 240);
    }

    private void layout() {
        mRecyclerView.layout(0, 0, 320, 320);
    }

    private void focusSearch() {
        mRecyclerView.focusSearch(1);
    }

    @Test
    public void layoutWithoutAdapter() throws InterruptedException {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        layout();
        assertEquals("layout manager should not be called if there is no adapter attached",
                0, layoutManager.mLayoutCount);
    }

    @Test
    @Ignore("b/236978861")
    public void setScrollContainer() {
        assertTrue("RecyclerView should announce itself as scroll container for the IME to "
                + "handle it properly", mRecyclerView.isScrollContainer());
    }

    @Test
    public void layoutWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
    }

    @Test
    public void focusWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        focusSearch();
    }

    @Test
    public void scrollWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.scrollBy(10, 10);
    }

    @Test
    public void smoothScrollWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.smoothScrollBy(10, 10);
    }

    @Test
    public void scrollToPositionWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.scrollToPosition(5);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void scrollFeedbackCallbacks() {
        mScrollFeedbackProvider = mock(ScrollFeedbackProviderCompat.class);
        mRecyclerView.mScrollFeedbackProvider = mScrollFeedbackProvider;
        mRecyclerView.setAdapter(new MockAdapter(20));
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        measure();
        layout();

        MotionEvent ev = TouchUtils.createMotionEvent(
                /* inputDeviceId= */ 1,
                InputDeviceCompat.SOURCE_TOUCHSCREEN,
                MotionEvent.ACTION_MOVE,
                List.of(
                    Pair.create(MotionEvent.AXIS_X, 10),
                    Pair.create(MotionEvent.AXIS_Y, -20)));
        layoutManager.mConsumedHorizontalScroll = 3;
        layoutManager.mConsumedVerticalScroll = -20;
        mRecyclerView.scrollByInternal(
                /* x= */ 10,
                /* y= */ -20,
                /* horizontalAxis= */ MotionEvent.AXIS_X,
                /* verticalAxis= */ MotionEvent.AXIS_Y,
                ev,
                ViewCompat.TYPE_TOUCH);

        // Verify onScrollProgress calls equating to the amount of consumed pixels on each axis.
        verify(mScrollFeedbackProvider).onScrollProgress(
                /* inputDeviceId= */ 1, InputDeviceCompat.SOURCE_TOUCHSCREEN, MotionEvent.AXIS_X,
                /* deltaInPixels= */ 3);
        verify(mScrollFeedbackProvider).onScrollProgress(
                /* inputDeviceId= */ 1, InputDeviceCompat.SOURCE_TOUCHSCREEN, MotionEvent.AXIS_Y,
                /* deltaInPixels= */ -20);
        // Part of the X scroll was not consumed, so expect an onScrollLimit call.
        verify(mScrollFeedbackProvider).onScrollLimit(
                /* inputDeviceId= */ 1, InputDeviceCompat.SOURCE_TOUCHSCREEN, MotionEvent.AXIS_X,
                /* isStart= */ false);
        // All of the Y scroll was consumed. So expect no onScrollLimit call.
        verify(mScrollFeedbackProvider, never()).onScrollLimit(
                /* inputDeviceId= */ anyInt(), anyInt(), eq(MotionEvent.AXIS_Y),
                /* isStart= */ eq(false));
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void scrollFeedbackCallbacks_motionEventUnavailable() {
        mScrollFeedbackProvider = mock(ScrollFeedbackProviderCompat.class);
        mRecyclerView.mScrollFeedbackProvider = mScrollFeedbackProvider;
        mRecyclerView.setAdapter(new MockAdapter(20));
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        measure();
        layout();

        mRecyclerView.scrollByInternal(
                /* x= */ 10,
                /* y= */ -20,
                /* horizontalAxis= */ -1,
                /* verticalAxis= */ -1,
                null,
                ViewCompat.TYPE_TOUCH);

        verify(mScrollFeedbackProvider, never()).onScrollProgress(
                anyInt(), anyInt(), anyInt(), anyInt());
        verify(mScrollFeedbackProvider, never()).onScrollLimit(
                anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    public void smoothScrollToPositionWithoutLayoutManager() throws InterruptedException {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        mRecyclerView.smoothScrollToPosition(5);
    }

    @Test
    public void interceptTouchWithoutLayoutManager() {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        assertFalse(mRecyclerView.onInterceptTouchEvent(
                MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 10, 10, 0)));
    }

    @Test
    public void onTouchWithoutLayoutManager() {
        mRecyclerView.setAdapter(new MockAdapter(20));
        measure();
        layout();
        assertFalse(mRecyclerView.onTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 10, 10, 0)));
    }

    @Test
    public void layoutSimple() throws InterruptedException {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new MockAdapter(3));
        layout();
        assertEquals("when both layout manager and activity is set, recycler view should call"
                + " layout manager's layout method", 1, layoutManager.mLayoutCount);
    }

    @Test
    public void observingAdapters() {
        MockAdapter adapterOld = new MockAdapter(1);
        mRecyclerView.setAdapter(adapterOld);
        assertTrue("attached adapter should have observables", adapterOld.hasObservers());

        MockAdapter adapterNew = new MockAdapter(2);
        mRecyclerView.setAdapter(adapterNew);
        assertFalse("detached adapter should lose observable", adapterOld.hasObservers());
        assertTrue("new adapter should have observers", adapterNew.hasObservers());

        mRecyclerView.setAdapter(null);
        assertNull("adapter should be removed successfully", mRecyclerView.getAdapter());
        assertFalse("when adapter is removed, observables should be removed too",
                adapterNew.hasObservers());
    }

    @Test
    public void adapterChangeCallbacks() {
        MockLayoutManager layoutManager = new MockLayoutManager();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapterOld = new MockAdapter(1);
        mRecyclerView.setAdapter(adapterOld);
        layoutManager.assertPrevNextAdapters(null, adapterOld);

        MockAdapter adapterNew = new MockAdapter(2);
        mRecyclerView.setAdapter(adapterNew);
        layoutManager.assertPrevNextAdapters("switching adapters should trigger correct callbacks"
                , adapterOld, adapterNew);

        mRecyclerView.setAdapter(null);
        layoutManager.assertPrevNextAdapters(
                "Setting adapter null should trigger correct callbacks",
                adapterNew, null);
    }

    @Test
    public void recyclerOffsetsOnMove() {
        MockLayoutManager  layoutManager = new MockLayoutManager();
        final List<RecyclerView.ViewHolder> recycledVhs = new ArrayList<>();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapter = new MockAdapter(100) {
            @Override
            public void onViewRecycled(RecyclerView.@NonNull ViewHolder holder) {
                super.onViewRecycled(holder);
                recycledVhs.add(holder);
            }
        };
        MockViewHolder mvh = new MockViewHolder(new TextView(getContext()));
        mRecyclerView.setAdapter(adapter);
        adapter.bindViewHolder(mvh, 20);
        mRecyclerView.mRecycler.mCachedViews.add(mvh);
        mRecyclerView.offsetPositionRecordsForRemove(10, 9, false);

        mRecyclerView.offsetPositionRecordsForRemove(11, 1, false);
        assertEquals(1, recycledVhs.size());
        assertSame(mvh, recycledVhs.get(0));
    }

    @Test
    public void recyclerOffsetsOnAdd() {
        MockLayoutManager  layoutManager = new MockLayoutManager();
        final List<RecyclerView.ViewHolder> recycledVhs = new ArrayList<>();
        mRecyclerView.setLayoutManager(layoutManager);
        MockAdapter adapter = new MockAdapter(100) {
            @Override
            public void onViewRecycled(RecyclerView.@NonNull ViewHolder holder) {
                super.onViewRecycled(holder);
                recycledVhs.add(holder);
            }
        };
        MockViewHolder mvh = new MockViewHolder(new TextView(getContext()));
        mRecyclerView.setAdapter(adapter);
        adapter.bindViewHolder(mvh, 20);
        mRecyclerView.mRecycler.mCachedViews.add(mvh);
        mRecyclerView.offsetPositionRecordsForRemove(10, 9, false);

        mRecyclerView.offsetPositionRecordsForInsert(15, 10);
        assertEquals(11, mvh.mPosition);
    }

    @Test
    public void savedStateWithStatelessLayoutManager() throws InterruptedException {
        mRecyclerView.setLayoutManager(new MockLayoutManager() {
            @Override
            public Parcelable onSaveInstanceState() {
                return null;
            }
        });
        mRecyclerView.setAdapter(new MockAdapter(3));
        Parcel parcel = Parcel.obtain();
        String parcelSuffix = UUID.randomUUID().toString();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);

        // reset position for reading
        parcel.setDataPosition(0);
        RecyclerView restored = new RecyclerView(getContext());
        restored.setLayoutManager(new MockLayoutManager());
        mRecyclerView.setAdapter(new MockAdapter(3));
        // restore
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);
        restored.onRestoreInstanceState(savedState);

        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        assertEquals("When unmarshalling, all of the parcel should be read", 0, parcel.dataAvail());

    }

    @Test
    public void savedState() throws InterruptedException {
        MockLayoutManager mlm = new MockLayoutManager();
        mRecyclerView.setLayoutManager(mlm);
        mRecyclerView.setAdapter(new MockAdapter(3));
        layout();
        Parcelable savedState = mRecyclerView.onSaveInstanceState();
        // we append a suffix to the parcelable to test out of bounds
        String parcelSuffix = UUID.randomUUID().toString();
        Parcel parcel = Parcel.obtain();
        savedState.writeToParcel(parcel, 0);
        parcel.writeString(parcelSuffix);

        // reset for reading
        parcel.setDataPosition(0);
        // re-create
        savedState = RecyclerView.SavedState.CREATOR.createFromParcel(parcel);

        RecyclerView restored = new RecyclerView(getContext());
        mRecyclerView = restored;
        MockLayoutManager mlmRestored = new MockLayoutManager();
        restored.setLayoutManager(mlmRestored);
        restored.setAdapter(new MockAdapter(3));
        restored.onRestoreInstanceState(savedState);
        layout();
        assertEquals("Parcel reading should not go out of bounds", parcelSuffix,
                parcel.readString());
        assertEquals("When unmarshalling, all of the parcel should be read", 0, parcel.dataAvail());
        assertEquals("uuid in layout manager should be preserved properly", mlm.mUuid,
                mlmRestored.mUuid);
    }

    @Test
    public void dontSaveChildrenState() throws InterruptedException {
        MockLayoutManager mlm = new MockLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                super.onLayoutChildren(recycler, state);
                View view = recycler.getViewForPosition(0);
                addView(view);
                measureChildWithMargins(view, 0, 0);
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
            }
        };
        mRecyclerView.setLayoutManager(mlm);
        mRecyclerView.setAdapter(new MockAdapter(3) {
            @Override
            public RecyclerView.@NonNull ViewHolder onCreateViewHolder(
                    @NonNull ViewGroup parent, int viewType) {
                final LoggingView itemView = new LoggingView(parent.getContext());
                //noinspection ResourceType
                itemView.setId(3);
                return new MockViewHolder(itemView);
            }
        });
        measure();
        layout();
        View view = mRecyclerView.getChildAt(0);
        assertNotNull("Assumption check", view);
        LoggingView loggingView = (LoggingView) view;
        SparseArray<Parcelable> container = new SparseArray<Parcelable>();
        mRecyclerView.saveHierarchyState(container);
        assertEquals("children's save state method should not be called", 0,
                loggingView.getOnSavedInstanceCnt());
    }

    @Test
    public void smoothScrollBy_withCustomInterpolator_usesCustomInterpolator() {
        mRecyclerView.setLayoutManager(new MockLayoutManager());
        mRecyclerView.setAdapter(new MockAdapter(20));
        Interpolator interpolator = new LinearInterpolator();
        mRecyclerView.smoothScrollBy(0, 100, interpolator);
        assertSame(interpolator, mRecyclerView.mViewFlinger.mInterpolator);
    }

    @Test
    public void smoothScrollBy_withoutCustomInterpolator_resetsToDefaultInterpolator() {
        mRecyclerView.setLayoutManager(new MockLayoutManager());
        mRecyclerView.setAdapter(new MockAdapter(20));
        Interpolator interpolator = new LinearInterpolator();
        mRecyclerView.smoothScrollBy(0, 100, interpolator);

        mRecyclerView.smoothScrollBy(0, -100);

        assertSame(RecyclerView.sQuinticInterpolator, mRecyclerView.mViewFlinger.mInterpolator);
    }

    @Test
    public void fling_resetsInterpolatorToDefault() {
        mRecyclerView.setLayoutManager(new MockLayoutManager());
        mRecyclerView.setAdapter(new MockAdapter(20));
        Interpolator interpolator = new LinearInterpolator();
        mRecyclerView.smoothScrollBy(0, 100, interpolator);

        mRecyclerView.fling(0, -1000);

        assertSame(RecyclerView.sQuinticInterpolator, mRecyclerView.mViewFlinger.mInterpolator);
    }

    @Test
    public void createAttachedException() {
        mRecyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.@NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_view, parent, true)
                        .findViewById(R.id.item_view); // find child, since parent is returned
                return new RecyclerView.ViewHolder(view) {};
            }

            @Override
            public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
                fail("shouldn't get here, should throw during create");
            }

            @Override
            public int getItemCount() {
                return 1;
            }
        });
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        try {
            measure();
            //layout();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void prefetchChangesCacheSize() {
        mRecyclerView.setAdapter(new MockAdapter(20));
        MockLayoutManager mlm = new MockLayoutManager() {
            @Override
            public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                    RecyclerView.LayoutManager.LayoutPrefetchRegistry prefetchManager) {
                prefetchManager.addPosition(0, 0);
                prefetchManager.addPosition(1, 0);
                prefetchManager.addPosition(2, 0);
            }
        };

        RecyclerView.Recycler recycler = mRecyclerView.mRecycler;
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE, recycler.mViewCacheMax);
        mRecyclerView.setLayoutManager(mlm);
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE, recycler.mViewCacheMax);

        // layout, so prefetches can occur
        mRecyclerView.measure(View.MeasureSpec.EXACTLY | 100, View.MeasureSpec.EXACTLY | 100);
        mRecyclerView.layout(0, 0, 100, 100);

        // prefetch gets 3 items, so expands cache by 3
        mRecyclerView.mPrefetchRegistry.collectPrefetchPositionsFromView(mRecyclerView, false);
        assertEquals(3, mRecyclerView.mPrefetchRegistry.mCount);
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE + 3, recycler.mViewCacheMax);

        // Reset to default by removing layout
        mRecyclerView.setLayoutManager(null);
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE, recycler.mViewCacheMax);

        // And restore by restoring layout
        mRecyclerView.setLayoutManager(mlm);
        assertEquals(RecyclerView.Recycler.DEFAULT_CACHE_SIZE + 3, recycler.mViewCacheMax);
    }

    @Test
    public void getNanoTime() throws InterruptedException {
        // check that it looks vaguely time-ish
        long time = mRecyclerView.getNanoTime();
        assertNotEquals(0, time);

        // Sleep for 1 nano to ensure next call won't have the same measurement.
        Thread.sleep(0, 1);
        assertNotEquals(time, mRecyclerView.getNanoTime());
    }

    @Test
    public void findNestedRecyclerView() {
        RecyclerView recyclerView = new RecyclerView(getContext());
        assertEquals(recyclerView, RecyclerView.findNestedRecyclerView(recyclerView));

        ViewGroup parent = new FrameLayout(getContext());
        assertEquals(null, RecyclerView.findNestedRecyclerView(parent));
        parent.addView(recyclerView);
        assertEquals(recyclerView, RecyclerView.findNestedRecyclerView(parent));

        ViewGroup grandParent = new FrameLayout(getContext());
        assertEquals(null, RecyclerView.findNestedRecyclerView(grandParent));
        grandParent.addView(parent);
        assertEquals(recyclerView, RecyclerView.findNestedRecyclerView(grandParent));
    }

    @Test
    public void clearNestedRecyclerViewIfNotNested() {
        RecyclerView recyclerView = new RecyclerView(getContext());
        ViewGroup parent = new FrameLayout(getContext());
        parent.addView(recyclerView);
        ViewGroup grandParent = new FrameLayout(getContext());
        grandParent.addView(parent);

        // verify trivial noop case
        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(recyclerView) {};
        holder.mNestedRecyclerView = new WeakReference<>(recyclerView);
        RecyclerView.clearNestedRecyclerViewIfNotNested(holder);
        assertEquals(recyclerView, holder.mNestedRecyclerView.get());

        // verify clear case
        holder = new RecyclerView.ViewHolder(new View(getContext())) {};
        holder.mNestedRecyclerView = new WeakReference<>(recyclerView);
        RecyclerView.clearNestedRecyclerViewIfNotNested(holder);
        assertNull(holder.mNestedRecyclerView);

        // verify more deeply nested case
        holder = new RecyclerView.ViewHolder(grandParent) {};
        holder.mNestedRecyclerView = new WeakReference<>(recyclerView);
        RecyclerView.clearNestedRecyclerViewIfNotNested(holder);
        assertEquals(recyclerView, holder.mNestedRecyclerView.get());
    }

    @Test
    public void exceptionContainsClasses() {
        RecyclerView first = new RecyclerView(getContext());
        first.setLayoutManager(new LinearLayoutManager(getContext()));
        first.setAdapter(new MockAdapter(10));

        RecyclerView second = new RecyclerView(getContext());
        try {
            second.setLayoutManager(first.getLayoutManager());
            fail("exception expected");
        } catch (IllegalArgumentException e) {
            // Note: exception contains first RV
            String m = e.getMessage();
            assertTrue("must contain RV class", m.contains(RecyclerView.class.getName()));
            assertTrue("must contain Adapter class", m.contains(MockAdapter.class.getName()));
            assertTrue("must contain LM class", m.contains(LinearLayoutManager.class.getName()));
            assertTrue("must contain ctx class", m.contains(getContext().getClass().getName()));
        }
    }

    @Test
    public void focusOrderTest() {
        FocusOrderAdapter focusAdapter = new FocusOrderAdapter(getContext());
        mRecyclerView.setAdapter(focusAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        measure();
        layout();

        View expected = focusAdapter.mBottomLeft;
        assertEquals(expected, focusAdapter.mTopRight.focusSearch(View.FOCUS_FORWARD));

        expected = focusAdapter.mBottomRight;
        assertSame(expected, focusAdapter.mBottomLeft.focusSearch(View.FOCUS_FORWARD));

        // we don't want looping within RecyclerView
        assertNull(focusAdapter.mBottomRight.focusSearch(View.FOCUS_FORWARD));
        assertNull(focusAdapter.mTopLeft.focusSearch(View.FOCUS_BACKWARD));
    }

    @Test
    public void setAdapter_callsCorrectLmMethods() throws Throwable {
        MockLayoutManager mockLayoutManager = new MockLayoutManager();
        MockAdapter mockAdapter = new MockAdapter(1);
        mRecyclerView.setLayoutManager(mockLayoutManager);

        mRecyclerView.setAdapter(mockAdapter);
        layout();

        assertEquals(1, mockLayoutManager.mAdapterChangedCount);
        assertEquals(0, mockLayoutManager.mItemsChangedCount);
    }

    @Test
    public void swapAdapter_callsCorrectLmMethods() throws Throwable {
        MockLayoutManager mockLayoutManager = new MockLayoutManager();
        MockAdapter mockAdapter = new MockAdapter(1);
        mRecyclerView.setLayoutManager(mockLayoutManager);

        mRecyclerView.swapAdapter(mockAdapter, true);
        layout();

        assertEquals(1, mockLayoutManager.mAdapterChangedCount);
        assertEquals(1, mockLayoutManager.mItemsChangedCount);
    }

    @Test
    public void notifyDataSetChanged_callsCorrectLmMethods() throws Throwable {
        MockLayoutManager mockLayoutManager = new MockLayoutManager();
        MockAdapter mockAdapter = new MockAdapter(1);
        mRecyclerView.setLayoutManager(mockLayoutManager);
        mRecyclerView.setAdapter(mockAdapter);
        mockLayoutManager.mAdapterChangedCount = 0;
        mockLayoutManager.mItemsChangedCount = 0;

        mockAdapter.notifyDataSetChanged();
        layout();

        assertEquals(0, mockLayoutManager.mAdapterChangedCount);
        assertEquals(1, mockLayoutManager.mItemsChangedCount);
    }

    @Test
    public void setAdapter_callsCorrectAdapterListenerMethods() throws Throwable {
        MockOnAdapterChangeListener firstAdapterListener = new MockOnAdapterChangeListener() {
            @Override
            public void onAdapterChanged(RecyclerView.@Nullable Adapter<?> oldAdapter,
                    RecyclerView.@Nullable Adapter<?> newAdapter) {
                super.onAdapterChanged(oldAdapter, newAdapter);
                mRecyclerView.removeOnAdapterChangeListener(this);
            }
        };
        MockOnAdapterChangeListener secondAdapterListener = new MockOnAdapterChangeListener() {
            @Override
            public void onAdapterChanged(RecyclerView.@Nullable Adapter<?> oldAdapter,
                    RecyclerView.@Nullable Adapter<?> newAdapter) {
                super.onAdapterChanged(oldAdapter, newAdapter);
                mRecyclerView.removeOnAdapterChangeListener(this);
            }
        };
        MockAdapter firstAdapter = new MockAdapter(0);
        MockAdapter secondAdapter = new MockAdapter(0);
        mRecyclerView.addOnAdapterChangeListener(firstAdapterListener);
        mRecyclerView.addOnAdapterChangeListener(secondAdapterListener);

        mRecyclerView.setAdapter(firstAdapter);
        mRecyclerView.setAdapter(secondAdapter);

        assertEquals(1, firstAdapterListener.mAdapterChangedCount);
        assertNull(firstAdapterListener.mOldAdapter);
        assertSame(firstAdapter, firstAdapterListener.mNewAdapter);
        assertEquals(1, secondAdapterListener.mAdapterChangedCount);
        assertNull(secondAdapterListener.mOldAdapter);
        assertSame(firstAdapter, secondAdapterListener.mNewAdapter);
    }

    @Test
    public void swapAdapter_callsCorrectAdapterListenerMethods() throws Throwable {
        MockOnAdapterChangeListener firstAdapterListener = new MockOnAdapterChangeListener() {
            @Override
            public void onAdapterChanged(RecyclerView.@Nullable Adapter<?> oldAdapter,
                    RecyclerView.@Nullable Adapter<?> newAdapter) {
                super.onAdapterChanged(oldAdapter, newAdapter);
                mRecyclerView.removeOnAdapterChangeListener(this);
            }
        };
        MockOnAdapterChangeListener secondAdapterListener = new MockOnAdapterChangeListener() {
            @Override
            public void onAdapterChanged(RecyclerView.@Nullable Adapter<?> oldAdapter,
                    RecyclerView.@Nullable Adapter<?> newAdapter) {
                super.onAdapterChanged(oldAdapter, newAdapter);
                mRecyclerView.removeOnAdapterChangeListener(this);
            }
        };
        MockAdapter firstAdapter = new MockAdapter(0);
        MockAdapter secondAdapter = new MockAdapter(0);
        mRecyclerView.addOnAdapterChangeListener(firstAdapterListener);
        mRecyclerView.addOnAdapterChangeListener(secondAdapterListener);

        mRecyclerView.swapAdapter(firstAdapter, true);
        mRecyclerView.swapAdapter(secondAdapter, true);

        assertEquals(1, firstAdapterListener.mAdapterChangedCount);
        assertNull(firstAdapterListener.mOldAdapter);
        assertSame(firstAdapter, firstAdapterListener.mNewAdapter);
        assertEquals(1, secondAdapterListener.mAdapterChangedCount);
        assertNull(secondAdapterListener.mOldAdapter);
        assertSame(firstAdapter, secondAdapterListener.mNewAdapter);
    }

    static class MockLayoutManager extends RecyclerView.LayoutManager {

        int mLayoutCount = 0;

        int mAdapterChangedCount = 0;
        int mItemsChangedCount = 0;

        int mConsumedHorizontalScroll = Integer.MIN_VALUE;
        int mConsumedVerticalScroll = Integer.MIN_VALUE;

        RecyclerView.Adapter mPrevAdapter;

        RecyclerView.Adapter mNextAdapter;

        String mUuid = UUID.randomUUID().toString();

        @Override
        public void onAdapterChanged(RecyclerView.Adapter oldAdapter,
                RecyclerView.Adapter newAdapter) {
            super.onAdapterChanged(oldAdapter, newAdapter);
            mPrevAdapter = oldAdapter;
            mNextAdapter = newAdapter;
            mAdapterChangedCount++;
        }

        @Override
        public void onItemsChanged(RecyclerView recyclerView) {
            mItemsChangedCount++;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            mLayoutCount += 1;
        }

        @Override
        public Parcelable onSaveInstanceState() {
            LayoutManagerSavedState lss = new LayoutManagerSavedState();
            lss.mUuid = mUuid;
            return lss;
        }

        @Override
        public void onRestoreInstanceState(Parcelable state) {
            super.onRestoreInstanceState(state);
            if (state instanceof LayoutManagerSavedState) {
                mUuid = ((LayoutManagerSavedState) state).mUuid;
            }
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        public void assertPrevNextAdapters(String message, RecyclerView.Adapter prevAdapter,
                RecyclerView.Adapter nextAdapter) {
            assertSame(message, prevAdapter, mPrevAdapter);
            assertSame(message, nextAdapter, mNextAdapter);
        }

        public void assertPrevNextAdapters(RecyclerView.Adapter prevAdapter,
                RecyclerView.Adapter nextAdapter) {
            assertPrevNextAdapters("Adapters from onAdapterChanged callback should match",
                    prevAdapter, nextAdapter);
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return mConsumedHorizontalScroll != Integer.MIN_VALUE ? mConsumedHorizontalScroll : dx;
        }

        @Override
        public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
                RecyclerView.State state) {
            return mConsumedVerticalScroll != Integer.MIN_VALUE ? mConsumedVerticalScroll : dy;
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public boolean canScrollVertically() {
            return true;
        }
    }

    @SuppressLint("BanParcelableUsage")
    static class LayoutManagerSavedState implements Parcelable {

        String mUuid;

        public LayoutManagerSavedState(Parcel in) {
            mUuid = in.readString();
        }

        public LayoutManagerSavedState() {

        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mUuid);
        }

        public static final Parcelable.Creator<LayoutManagerSavedState> CREATOR
                = new Parcelable.Creator<LayoutManagerSavedState>() {
            @Override
            public LayoutManagerSavedState createFromParcel(Parcel in) {
                return new LayoutManagerSavedState(in);
            }

            @Override
            public LayoutManagerSavedState[] newArray(int size) {
                return new LayoutManagerSavedState[size];
            }
        };
    }

    static class MockAdapter extends RecyclerView.Adapter {

        private int mCount = 0;


        MockAdapter(int count) {
            this.mCount = count;
        }

        @Override
        public RecyclerView.@NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            return new MockViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mCount;
        }

        void removeItems(int start, int count) {
            mCount -= count;
            notifyItemRangeRemoved(start, count);
        }

        void addItems(int start, int count) {
            mCount += count;
            notifyItemRangeInserted(start, count);
        }
    }

    static class MockViewHolder extends RecyclerView.ViewHolder {
        public Object mItem;
        public MockViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class MockOnAdapterChangeListener implements RecyclerView.OnAdapterChangeListener {

        int mAdapterChangedCount = 0;

        RecyclerView.Adapter<?> mOldAdapter;

        RecyclerView.Adapter<?> mNewAdapter;

        @Override
        public void onAdapterChanged(RecyclerView.@Nullable Adapter<?> oldAdapter,
                RecyclerView.@Nullable Adapter<?> newAdapter) {
            mOldAdapter = oldAdapter;
            mNewAdapter = newAdapter;
            mAdapterChangedCount++;
        }
    }

    static class LoggingView extends TextView {
        private int mOnSavedInstanceCnt = 0;

        public LoggingView(Context context) {
            super(context);
        }

        public LoggingView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public LoggingView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        public Parcelable onSaveInstanceState() {
            mOnSavedInstanceCnt ++;
            return super.onSaveInstanceState();
        }

        public int getOnSavedInstanceCnt() {
            return mOnSavedInstanceCnt;
        }
    }

    static class FocusOrderAdapter extends RecyclerView.Adapter {
        TextView mTopLeft;
        TextView mTopRight;
        TextView mBottomLeft;
        TextView mBottomRight;

        FocusOrderAdapter(Context context) {
            mTopLeft = new TextView(context);
            mTopRight = new TextView(context);
            mBottomLeft = new TextView(context);
            mBottomRight = new TextView(context);
            for (TextView tv : new TextView[]{mTopLeft, mTopRight, mBottomLeft, mBottomRight}) {
                tv.setFocusableInTouchMode(true);
                tv.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            }
            // create a scenario where the "first" focusable is to the right of the last one
            mTopLeft.setFocusable(false);
            mTopRight.getLayoutParams().width = 101;
            mTopLeft.getLayoutParams().width = 101;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout holder = new LinearLayout(parent.getContext());
            holder.setOrientation(LinearLayout.HORIZONTAL);
            return new MockViewHolder(holder);
        }

        @Override
        public void onBindViewHolder(RecyclerView.@NonNull ViewHolder holder, int position) {
            LinearLayout l = (LinearLayout) holder.itemView;
            l.removeAllViews();
            if (position == 0) {
                l.addView(mTopLeft);
                l.addView(mTopRight);
            } else {
                l.addView(mBottomLeft);
                l.addView(mBottomRight);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }

        void removeItems(int start, int count) {
        }

        void addItems(int start, int count) {
        }
    }
}
