/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class BucketTest extends AndroidTestCase {

    ChildHelper.Bucket mBucket;

    ArrayList<Integer> mArr;

    Set<Integer> mSet;

    int max = 0;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mBucket = new ChildHelper.Bucket();
        mArr = new ArrayList<Integer>();
        Collections.addAll(mArr, 0, 1, 2, 3, 4, 5, 6, 10, 12, 13, 21, 22, 122, 14, 44, 29, 205, 19);
        for (int i = 1; i < 4; i++) {
            mArr.add(i * (ChildHelper.Bucket.BITS_PER_WORD - 1));
            mArr.add(i * (ChildHelper.Bucket.BITS_PER_WORD));
            mArr.add(i * (ChildHelper.Bucket.BITS_PER_WORD + 1));
            mArr.add(i * ChildHelper.Bucket.BITS_PER_WORD - 1);
            mArr.add(i * ChildHelper.Bucket.BITS_PER_WORD);
            mArr.add(i * ChildHelper.Bucket.BITS_PER_WORD + 1);
        }

        mSet = new HashSet<Integer>();
        max = 0;
        for (int i = mArr.size() - 1; i >= 0; i--) {
            if (!mSet.add(mArr.get(i))) {
                mArr.remove(i);
            }
            max = Math.max(max, i);
        }
    }


    public void testSetClear() {
        for (int i : mArr) {
            mBucket.set(i);
            assertTrue(mBucket.get(i));
        }
        for (int i = 0; i < max + 100; i++) {
            assertEquals(mBucket.get(i), mSet.contains(i));
        }

        for (int i : mArr) {
            mBucket.clear(i);
            assertFalse(mBucket.get(i));
        }

        for (int i = 0; i < max + 100; i++) {
            assertFalse(mBucket.get(i));
        }
    }


    public void testRemove() {
        for (int i : mArr) {
            mBucket.reset();
            for (int j : mArr) {
                mBucket.set(j);
            }
            mBucket.remove(i);
            for (int j : mArr) {
                if (i == j) {
                    continue;
                }
                if (j < i) {
                    assertTrue(mBucket.get(j));
                } else {
                    assertEquals(mSet.contains(j + 1), mBucket.get(j));
                }
            }
        }
    }

    public void testInsert() {
        for (int i : mArr) {
            for (boolean val : new boolean[]{true, false}) {
                mBucket.reset();
                for (int j : mArr) {
                    mBucket.set(j);
                }
                mBucket.insert(i, val);
                assertEquals(mBucket.get(i), val);
                for (int j : mArr) {
                    if (j < i) {
                        assertTrue(mBucket.get(j));
                    } else if (j == i) {
                        assertEquals(mBucket.get(j), val);
                    } else {
                        assertEquals(mSet.contains(j - 1), mBucket.get(j));
                        assertTrue(mBucket.get(j + 1));
                    }
                }
            }
        }
    }


    public void testCountOnesBefore() {
        assertEquals(mBucket.countOnesBefore(0), 0);
        for (int i : mArr) {
            mBucket.set(i);
            max = Math.max(i, max);
        }
        assertEquals(mBucket.countOnesBefore(0), 0);
        for (int i = 0; i < max + 200; i++) {
            int count = 0;
            for (int j : mArr) {
                if (j < i) {
                    count++;
                }
            }
            assertEquals(count, mBucket.countOnesBefore(i));
        }
    }
}
