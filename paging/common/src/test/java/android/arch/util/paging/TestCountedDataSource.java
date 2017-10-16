/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.util.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TestCountedDataSource extends CountedDataSource<User> {
    private static final ArrayList<User> sUsers = TestDataSource.sUsers;

    private int mCount = sUsers.size();

    static void verifyRange(LazyList<User> list, int start, int end) {
        assertEquals("size should be same", end - start, list.mItems.size());
        for (int i = 0; i < list.mItems.size(); i++) {
            // NOTE: avoid getter, to avoid signaling
            assertSame(sUsers.get(start + i), list.mItems.get(i));
        }
    }

    public void setCount(int count) {
        assertTrue(count <= sUsers.size());
        mCount = count;
    }

    @Override
    public int loadCount() {
        return mCount;
    }

    private List<User> getClampedRange(int start, int end) {
        start = Math.max(0, start);
        end = Math.min(loadCount(), end);
        return sUsers.subList(start, end);
    }

    @Nullable
    @Override
    public List<User> loadAfterInitial(int position, int pageSize) {
        return getClampedRange(position + 1, position + 1 + pageSize);
    }

    @Nullable
    @Override
    public List<User> loadAfter(int currentEndIndex, @NonNull User currentEndItem, int pageSize) {
        return getClampedRange(currentEndIndex + 1, currentEndIndex + 1 + pageSize);
    }

    @Nullable
    @Override
    public List<User> loadBefore(int currentBeginIndex, @NonNull User currentBeginItem,
            int pageSize) {
        return getClampedRange(currentBeginIndex - 1 - pageSize, currentBeginIndex);
    }
}
