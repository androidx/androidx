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
package android.support.v17.leanback.widget;

import android.test.AndroidTestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Base class for testing Grid algorithm
 * @hide
 */
public abstract class GridTest extends AndroidTestCase {

    static class Provider implements Grid.Provider {

        int[] mItems;
        int mCount;
        int[] mEdges;

        Provider(int[] items) {
            mItems = items;
            mCount = items.length;
            mEdges = new int[mCount];
        }

        @Override
        public int getCount() {
            return mCount;
        }

        @Override
        public int createItem(int index, boolean append, Object[] item) {
            return mItems[index];
        }

        @Override
        public void addItem(Object item, int index, int length, int rowIndex, int edge) {
            if (edge == Integer.MAX_VALUE || edge == Integer.MIN_VALUE) {
                // initialize edge for first item added
                edge = 0;
            }
            mEdges[index] = edge;
        }

        @Override
        public void removeItem(int index) {
        }

        @Override
        public int getEdge(int index) {
            return mEdges[index];
        }

        @Override
        public int getSize(int index) {
            return mItems[index];
        }

        void scroll(int distance) {
            for (int i= 0; i < mEdges.length; i++) {
                mEdges[i] -= distance;
            }
        }
    }

    Provider mProvider;

    static String dump(Grid grid) {
        StringWriter w = new StringWriter();
        grid.debugPrint(new PrintWriter(w));
        return w.toString();
    }
}
