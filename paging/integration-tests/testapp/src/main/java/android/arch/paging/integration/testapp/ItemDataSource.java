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

package android.arch.paging.integration.testapp;

import android.arch.paging.PositionalDataSource;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample data source with artificial data.
 */
class ItemDataSource extends PositionalDataSource<Item> {
    private static final int COUNT = 500;

    @ColorInt
    private static final int[] COLORS = new int[] {
            Color.RED,
            Color.BLUE,
            Color.BLACK,
    };

    private static int sGenerationId;
    private final int mGenerationId = sGenerationId++;

    private List<Item> loadRangeInternal(int startPosition, int loadCount) {
        List<Item> items = new ArrayList<>();
        int end = Math.min(COUNT, startPosition + loadCount);
        int bgColor = COLORS[mGenerationId % COLORS.length];

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = startPosition; i != end; i++) {
            items.add(new Item(i, "item " + i, bgColor));
        }
        return items;
    }

    // TODO: open up this API in PositionalDataSource?
    private static int computeFirstLoadPosition(int position, int firstLoadSize,
            int pageSize, int size) {
        int roundedPageStart = Math.round(position / pageSize) * pageSize;

        // minimum start position is 0
        roundedPageStart = Math.max(0, roundedPageStart);

        // maximum start pos is that which will encompass end of list
        int maximumLoadPage = ((size - firstLoadSize + pageSize - 1) / pageSize) * pageSize;
        roundedPageStart = Math.min(maximumLoadPage, roundedPageStart);

        return roundedPageStart;
    }

    @Override
    public void loadInitial(int requestedStartPosition, int requestedLoadSize,
            int pageSize, @NonNull InitialLoadCallback<Item> callback) {
        requestedStartPosition = computeFirstLoadPosition(
                requestedStartPosition, requestedLoadSize, pageSize, COUNT);

        requestedLoadSize = Math.min(COUNT - requestedStartPosition, requestedLoadSize);
        List<Item> data = loadRangeInternal(requestedStartPosition, requestedLoadSize);
        callback.onResult(data, requestedStartPosition, COUNT);
    }

    @Override
    public void loadRange(int startPosition, int count, @NonNull LoadCallback<Item> callback) {
        List<Item> data = loadRangeInternal(startPosition, count);
        callback.onResult(data);
    }
}
