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

package androidx.paging.integration.testapp;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.paging.PositionalDataSource;

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

    @Override
    public void loadInitial(@NonNull LoadInitialParams params,
            @NonNull LoadInitialCallback<Item> callback) {
        int position = computeInitialLoadPosition(params, COUNT);
        int loadSize = computeInitialLoadSize(params, position, COUNT);
        List<Item> data = loadRangeInternal(position, loadSize);
        callback.onResult(data, position, COUNT);
    }

    @Override
    public void loadRange(@NonNull LoadRangeParams params,
            @NonNull LoadRangeCallback<Item> callback) {
        List<Item> data = loadRangeInternal(params.startPosition, params.loadSize);
        callback.onResult(data);
    }
}
