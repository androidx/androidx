/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class RecycledViewPoolTest {
    static class MockViewHolder extends RecyclerView.ViewHolder {
        public MockViewHolder(Context context) {
            super(new View(context));
        }
    }

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    @Test
    public void construct() {
        RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
        assertEquals(0, pool.getRecycledViewCount(0));
        assertEquals(0, pool.size());
    }

    private RecyclerView.ViewHolder makeHolder(int viewType) {
        RecyclerView.ViewHolder holder = new MockViewHolder(getContext());
        holder.mItemViewType = viewType;
        return holder;
    }

    @Test
    public void put() {
        RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
        pool.putRecycledView(makeHolder(0));
        pool.putRecycledView(makeHolder(1));
        pool.putRecycledView(makeHolder(2));
        pool.putRecycledView(makeHolder(2));

        assertEquals(1, pool.getRecycledViewCount(0));
        assertEquals(1, pool.getRecycledViewCount(1));
        assertEquals(2, pool.getRecycledViewCount(2));
        assertEquals(0, pool.getRecycledViewCount(3));
        assertEquals(4, pool.size());
    }

    @Test
    public void putAndGet() {
        RecyclerView.RecycledViewPool pool = new RecyclerView.RecycledViewPool();
        pool.putRecycledView(makeHolder(3));
        pool.putRecycledView(makeHolder(3));

        assertEquals(2, pool.size());
        assertEquals(2, pool.getRecycledViewCount(3));

        RecyclerView.ViewHolder a = pool.getRecycledView(3);

        assertNotNull(a);
        assertEquals(1, pool.size());
        assertEquals(1, pool.getRecycledViewCount(3));

        RecyclerView.ViewHolder b = pool.getRecycledView(3);

        assertNotNull(b);
        assertNotEquals(a, b);
        assertEquals(0, pool.size());
        assertEquals(0, pool.getRecycledViewCount(3));
    }
}