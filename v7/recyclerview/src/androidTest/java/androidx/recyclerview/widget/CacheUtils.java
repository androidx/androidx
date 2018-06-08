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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CacheUtils {
    static void verifyPositionsPrefetched(RecyclerView view, int dx, int dy,
            Integer[]... positionData) {
        RecyclerView.LayoutManager.LayoutPrefetchRegistry layoutPrefetchRegistry =
                mock(RecyclerView.LayoutManager.LayoutPrefetchRegistry.class);
        view.mLayout.collectAdjacentPrefetchPositions(
                dx, dy, view.mState, layoutPrefetchRegistry);

        verify(layoutPrefetchRegistry, times(positionData.length)).addPosition(anyInt(), anyInt());
        for (Integer[] aPositionData : positionData) {
            verify(layoutPrefetchRegistry).addPosition(aPositionData[0], aPositionData[1]);
        }
    }

    private static void verifyCacheContainsPosition(RecyclerView view, int position) {
        for (int i = 0; i < view.mRecycler.mCachedViews.size(); i++) {
            if (view.mRecycler.mCachedViews.get(i).mPosition == position) return;
        }
        fail("Cache does not contain position " + position);
    }

    /**
     * Asserts that the positions passed are all resident in the view's cache.
     */
    static void verifyCacheContainsPositions(RecyclerView view, Integer... positions) {
        for (Integer position : positions) {
            verifyCacheContainsPosition(view, position);
        }
    }

    /**
     * Asserts that the position passed is resident in the view's cache, similar to
     * {@link #verifyCacheContainsPositions}, but additionally requires presence in
     * PrefetchRegistry.
     */
    static void verifyCacheContainsPrefetchedPositions(RecyclerView view, Integer... positions) {
        verifyCacheContainsPositions(view, positions);

        for (Integer position : positions) {
            assertTrue(view.mPrefetchRegistry.lastPrefetchIncludedPosition(position));
        }
        assertEquals(positions.length, view.mRecycler.mCachedViews.size());
    }

    /**
     * Asserts that none of the positions passed are resident in the view's cache.
     */
    static void verifyCacheDoesNotContainPositions(RecyclerView view, Integer... positions) {
        for (Integer position : positions) {
            for (int i = 0; i < view.mRecycler.mCachedViews.size(); i++) {
                assertNotEquals("Cache must not contain position " + position,
                        (int) position, view.mRecycler.mCachedViews.get(i).mPosition);
            }
        }
    }

    static RecyclerView.ViewHolder peekAtCachedViewForPosition(RecyclerView view, int position) {
        for (int i = 0; i < view.mRecycler.mCachedViews.size(); i++) {
            RecyclerView.ViewHolder holder = view.mRecycler.mCachedViews.get(i);
            if (holder.mPosition == position) {
                return holder;
            }
        }
        fail("Unable to find view with position " + position);
        return null;
    }
}
