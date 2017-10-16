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

import android.arch.util.paging.DataSource;
import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Sample data source with artificial data.
 */
public class ItemDataSource extends DataSource<Integer, Item> {
    private static final int COUNT = 10000;

    @ColorInt
    private static final int[] COLORS = new int[] {
            Color.RED,
            Color.BLUE,
            Color.BLACK,
    };


    private static int sGenerationId;
    private final int mGenerationId = sGenerationId++;

    @Override
    public Integer getKey(@NonNull Item item) {
        return item.id;
    }

    @Nullable
    @Override
    public List<Item> loadAfterInitial(@Nullable Integer position, int pageSize) {
        if (position == null) {
            position = -1;
        }
        return createItems(position + 1, pageSize, 1);
    }

    @Nullable
    @Override
    public List<Item> loadAfter(@NonNull Item currentEndItem, int pageSize) {
        return createItems(currentEndItem.id + 1, pageSize, 1);
    }

    @Nullable
    @Override
    public List<Item> loadBefore(@NonNull Item currentBeginItem, int pageSize) {
        return createItems(currentBeginItem.id - 1, pageSize, -1);
    }

    private List<Item> createItems(int start, int count, int direction) {
        if (isInvalid()) {
            // abort!
            return null;
        }

        List<Item> items = new ArrayList<>();
        int end = Math.max(-1, Math.min(COUNT, start + direction * count));
        int bgColor = COLORS[mGenerationId % COLORS.length];

        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (int i = start; i != end; i += direction) {
            items.add(new Item(i, "item " + i, bgColor));
        }

        if (isInvalid()) {
            // abort!
            return null;
        }
        return items;
    }
}
