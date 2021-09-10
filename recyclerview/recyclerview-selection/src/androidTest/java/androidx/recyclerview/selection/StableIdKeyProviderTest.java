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

package androidx.recyclerview.selection;

import static androidx.recyclerview.selection.StableIdKeyProvider.ViewHost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.testing.TestAdapter;
import androidx.recyclerview.selection.testing.TestHolder;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class StableIdKeyProviderTest {

    private Context mContext;
    private StableIdTestAdapter mAdapter;
    private TestViewHost mHost;
    private StableIdKeyProvider mKeyProvider;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mAdapter = new StableIdTestAdapter();
        mHost = new TestViewHost(mContext, mAdapter);
        mKeyProvider = new StableIdKeyProvider(mHost);
    }

    @Test
    public void testNotAttached() {
        assertEquals(RecyclerView.NO_POSITION, mKeyProvider.getPosition(1L));
        assertNull(mKeyProvider.getKey(1));
    }

    @Test
    public void testAttachedItems() {
        mHost.addViewHolder(1);
        mHost.addViewHolder(2);
        assertEquals(Long.valueOf(1), mKeyProvider.getKey(1));
        assertEquals(Long.valueOf(2), mKeyProvider.getKey(2));
    }

    @Test
    public void testRecycledItems() {
        mHost.addViewHolder(1);
        mHost.addViewHolder(2);
        mHost.removeViewHolder(1);
        mHost.addViewHolder(3);
        assertEquals(RecyclerView.NO_POSITION, mKeyProvider.getPosition(1L));
        assertNull(mKeyProvider.getKey(1));
        // Ensure item "2" remains accessible, and 3 is present.
        assertEquals(Long.valueOf(2), mKeyProvider.getKey(2));
        assertEquals(Long.valueOf(3), mKeyProvider.getKey(3));
    }

    private static class TestViewHost implements ViewHost, ViewHost.LifecycleListener {

        Map<View, ViewHolder> mViews = new HashMap<>();
        private @Nullable LifecycleListener mListener;
        private final Context mContext;
        private final StableIdTestAdapter mAdapter;

        TestViewHost(Context context, StableIdTestAdapter adapter) {
            mContext = context;
            mAdapter = adapter;
        }

        void addViewHolder(int position) {
            // This is usually implemented by findContainingViewHolder.
            // Here we use the adapter, since it is providing holders in our test.
            LinearLayout view = new LinearLayout(mContext);
            TestHolder holder = mAdapter.getHolder(view, position);
            mViews.put(view, holder);
            onAttached(view);
        }

        void removeViewHolder(int position) {
            View toRemove = null;
            for (Map.Entry<View, ViewHolder> entry : mViews.entrySet()) {
                if (entry.getValue().getLayoutPosition() == position) {
                    toRemove = entry.getKey();
                    break;
                }
            }

            if (toRemove != null) {
                onRecycled(toRemove);
                // Defer removing until after listeners get notified.
                mViews.remove(toRemove);
            }

        }

        @Override
        public void registerLifecycleListener(@NonNull LifecycleListener listener) {
            mListener = listener;
        }

        @Override
        public @Nullable ViewHolder findViewHolder(@NonNull View view) {
            // This is usually implemented by findContainingViewHolder.
            // Here we use the adapter, since it is providing holders in our test.
            return mViews.get(view);
        }

        @Override
        public int getPosition(@NonNull ViewHolder holder) {
            // Note, we use getLayoutPosition rather than getAbsoluteAdapterPosition
            // because the latter requires an owning RecyclerView.
            // In lieu of that, we use getLayoutPosition which is set
            // and stored as a field on the holder at the time the holder is bound.
            // This is a small contrivance necessary to support testability.
            return holder.getLayoutPosition();
        }

        @Override
        public void onAttached(@NonNull View view) {
            mListener.onAttached(view);
        }

        @Override
        public void onRecycled(@NonNull View view) {
            mListener.onRecycled(view);
        }
    }

    private static final class StableIdTestAdapter extends TestAdapter<Long> {
        @SuppressWarnings("unchecked")
        StableIdTestAdapter() {
            super(Collections.EMPTY_LIST, true);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        TestHolder getHolder(ViewGroup view, int position) {
            TestHolder holder = new TestHolder(view);
            bindViewHolder(holder, position);
            return holder;
        }
    }
}
