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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GapWorkerTest {

    private Context getContext() {
        return InstrumentationRegistry.getContext();
    }

    @Test
    public void registrySimple() {
        GapWorker.LayoutPrefetchRegistryImpl registry = new GapWorker.LayoutPrefetchRegistryImpl();
        registry.addPosition(0, 0);
        registry.addPosition(2, 0);
        registry.addPosition(3, 0);
        assertTrue(registry.lastPrefetchIncludedPosition(0));
        assertFalse(registry.lastPrefetchIncludedPosition(1));
        assertTrue(registry.lastPrefetchIncludedPosition(2));
        assertTrue(registry.lastPrefetchIncludedPosition(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryNegativeLayout() {
        GapWorker.LayoutPrefetchRegistryImpl registry = new GapWorker.LayoutPrefetchRegistryImpl();
        registry.addPosition(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void registryNegativeDistance() {
        GapWorker.LayoutPrefetchRegistryImpl registry = new GapWorker.LayoutPrefetchRegistryImpl();
        registry.addPosition(0, -1);
    }

    @Test
    public void registryResetCorrectly() {
        GapWorker.LayoutPrefetchRegistryImpl registry = new GapWorker.LayoutPrefetchRegistryImpl();
        registry.addPosition(0, 0);
        assertFalse(registry.lastPrefetchIncludedPosition(-1));
        assertTrue(registry.lastPrefetchIncludedPosition(0));

        registry.clearPrefetchPositions();

        assertFalse(registry.lastPrefetchIncludedPosition(-1));
        assertFalse(registry.lastPrefetchIncludedPosition(0));
    }

    @Test
    public void taskOrderViewPresence() {
        ArrayList<GapWorker.Task> list = new ArrayList<>();
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());

        list.get(0).view = null;
        list.get(1).view = new RecyclerView(getContext());
        list.get(2).view = null;

        Collections.sort(list, GapWorker.sTaskComparator);

        assertNotNull(list.get(0).view);
        assertNull(list.get(1).view);
        assertNull(list.get(2).view);
    }
    @Test
    public void taskOrderImmediate() {
        ArrayList<GapWorker.Task> list = new ArrayList<>();
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());

        list.get(0).immediate = true;
        list.get(1).immediate = false;
        list.get(2).immediate = true;

        Collections.sort(list, GapWorker.sTaskComparator);

        assertTrue(list.get(0).immediate);
        assertTrue(list.get(1).immediate);
        assertFalse(list.get(2).immediate);
    }

    @Test
    public void taskOrderImmediateVelocity() {
        ArrayList<GapWorker.Task> list = new ArrayList<>();
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());

        list.get(0).immediate = true;
        list.get(0).viewVelocity = 10;

        list.get(1).immediate = false;
        list.get(1).viewVelocity = 99;

        list.get(2).immediate = true;
        list.get(2).viewVelocity = 20;

        Collections.sort(list, GapWorker.sTaskComparator);

        assertEquals(20, list.get(0).viewVelocity);
        assertEquals(10, list.get(1).viewVelocity);
        assertEquals(99, list.get(2).viewVelocity);
    }

    @Test
    public void taskOrderImmediateVelocityDistance() {
        ArrayList<GapWorker.Task> list = new ArrayList<>();
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());
        list.add(new GapWorker.Task());

        list.get(0).immediate = true;
        list.get(0).viewVelocity = 400;
        list.get(0).distanceToItem = 300;

        list.get(1).immediate = false;
        list.get(1).viewVelocity = 800;
        list.get(1).distanceToItem = 900;

        list.get(2).immediate = true;
        list.get(2).viewVelocity = 300;
        list.get(2).distanceToItem = 200;

        list.get(3).immediate = true;
        list.get(3).viewVelocity = 300;
        list.get(3).distanceToItem = 100;

        Collections.sort(list, GapWorker.sTaskComparator);

        assertEquals(300, list.get(0).distanceToItem);
        assertEquals(100, list.get(1).distanceToItem);
        assertEquals(200, list.get(2).distanceToItem);
        assertEquals(900, list.get(3).distanceToItem);
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void gapWorkerWithoutLayout() {
        RecyclerView recyclerView = new RecyclerView(getContext());
        try {
            assertFalse(recyclerView.mIsAttached);
            recyclerView.onAttachedToWindow();
            recyclerView.mGapWorker.prefetch(RecyclerView.FOREVER_NS);
        } finally {
            recyclerView.onDetachedFromWindow();
        }
    }
}
