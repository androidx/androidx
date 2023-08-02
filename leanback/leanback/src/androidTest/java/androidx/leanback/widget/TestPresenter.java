/*
 * Copyright (C) 2023 The Android Open Source Project
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
package androidx.leanback.widget;

import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

final class TestPresenter extends Presenter {

    static final class Item {
        String mId;
        String mDetail;

        Item(String id, String detail) {
            mId = id;
            mDetail = detail;
        }

        @Override
        public String toString() {
            return mId + "- " + mDetail;
        }
    }

    private static Random sRandom = new Random();

    private static int sIdSeed = 100;

    static Item generateItem(int id) {
        Item item = new Item("" + id, "origin");
        return item;
    }

    static List<Item> generateItems(int startId, int endId) {
        List<Item> list = new ArrayList<>();
        for (int i = startId; i < endId; i++) {
            Item item = generateItem(i);
            list.add(item);
        }
        return list;
    }

    static List<Item> generateItems(int[] ids) {
        List<Item> list = new ArrayList<>();
        for (int i : ids) {
            Item item = new Item("" + i, "origin");
            list.add(item);
        }
        return list;
    }

    static void setupHorizontalGridView(BaseGridView gridView) {
        ViewGroup.LayoutParams layoutParams = gridView.getLayoutParams();
        layoutParams.width = 1920;
        layoutParams.height = 500;
        gridView.setLayoutParams(layoutParams);
        gridView.setPadding(116, 16, 116, 16);
        gridView.setHorizontalSpacing(40);
        gridView.setItemAlignmentOffsetPercent(0f);
        gridView.setItemAlignmentOffset(0);
        gridView.setWindowAlignmentOffsetPercent(0);
        gridView.setWindowAlignmentOffset(116);
    }

    static boolean randomBoolean() {
        return sRandom.nextInt(2) == 0;
    }

    static List<Item> randomChange(List<Item> items) {
        List<Item> newList = new ArrayList<>();
        // 70% old items are copied cover
        for (int i = 0; i < items.size(); i++) {
            if (sRandom.nextInt(10) < 7) {
                newList.add(items.get(i));
            }
        }
        // Randomly shift position <= 1/3 of the old items
        int shiftCount = sRandom.nextInt(newList.size() / 3);
        for (int i = 0; i < shiftCount; i++) {
            int pos1 = sRandom.nextInt(newList.size());
            Item item1 = newList.get(pos1);
            int pos2 = sRandom.nextInt(newList.size());
            Item item2 = newList.get(pos2);
            newList.set(pos1, item2);
            newList.set(pos2, item1);
        }
        // Insert new items into random positions
        int newItemsCount = items.size() - newList.size();
        for (int i = 0; i < newItemsCount; i++) {
            Item item = generateItem(sIdSeed++);
            newList.add(sRandom.nextInt(newList.size()), item);
        }
        return newList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        TextView tv = new TextView(parent.getContext());
        tv.setFocusable(true);
        tv.setFocusableInTouchMode(true);
        tv.setLayoutParams(new ViewGroup.LayoutParams(393, 221));
        tv.setBackgroundColor(Color.LTGRAY);
        tv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    v.setBackgroundColor(Color.YELLOW);
                } else {
                    v.setBackgroundColor(Color.LTGRAY);
                }
            }
        });
        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, @Nullable Object item) {
        ((TextView) viewHolder.view).setText(item.toString());
    }

    @Override
    public void onUnbindViewHolder(@NonNull ViewHolder viewHolder) {
    }

    static final DiffCallback<Item> DIFF_CALLBACK =
            new DiffCallback<Item>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Item oldItem, @NonNull Item newItem) {
                    return Objects.equals(oldItem.mId, newItem.mId);
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Item oldItem,
                        @NonNull Item newItem) {
                    return Objects.equals(oldItem.mDetail, newItem.mDetail);
                }
            };
}
