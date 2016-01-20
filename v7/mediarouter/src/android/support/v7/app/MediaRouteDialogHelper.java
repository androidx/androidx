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

package android.support.v7.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.mediarouter.R;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class MediaRouteDialogHelper {
    /**
     * The framework should set the dialog width properly, but somehow it doesn't work, hence
     * duplicating a similar logic here to determine the appropriate dialog width.
     */
    public static int getDialogWidth(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        boolean isPortrait = metrics.widthPixels < metrics.heightPixels;

        TypedValue value = new TypedValue();
        context.getResources().getValue(isPortrait ? R.dimen.mr_dialog_fixed_width_minor
                : R.dimen.mr_dialog_fixed_width_major, value, true);
        if (value.type == TypedValue.TYPE_DIMENSION) {
            return (int) value.getDimension(metrics);
        } else if (value.type == TypedValue.TYPE_FRACTION) {
            return (int) value.getFraction(metrics.widthPixels, metrics.widthPixels);
        }
        return ViewGroup.LayoutParams.WRAP_CONTENT;
    }

    /**
     * Compares two lists regardless of order.
     *
     * @param list1 A list
     * @param list2 A list to be compared with {@code list1}
     * @return True if two lists have exactly same items regardless of order, false otherwise.
     */
    public static <E> boolean listUnorderedEquals(List<E> list1, List<E> list2) {
        HashSet<E> set1 = new HashSet<>(list1);
        HashSet<E> set2 = new HashSet<>(list2);
        return set1.equals(set2);
    }

    /**
     * Compares two lists and returns a set of items which exist
     * after-list but before-list, which means newly added items.
     *
     * @param before A list
     * @param after A list to be compared with {@code before}
     * @return A set of items which contains newly added items while
     * comparing {@code after} to {@code before}.
     */
    public static <E> Set<E> getItemsAdded(List<E> before, List<E> after) {
        HashSet<E> set = new HashSet<>(after);
        set.removeAll(before);
        return set;
    }

    /**
     * Compares two lists and returns a set of items which exist
     * before-list but after-list, which means removed items.
     *
     * @param before A list
     * @param after A list to be compared with {@code before}
     * @return A set of items which contains removed items while
     * comparing {@code after} to {@code before}.
     */
    public static <E> Set<E> getItemsRemoved(List<E> before, List<E> after) {
        HashSet<E> set = new HashSet<>(before);
        set.removeAll(after);
        return set;
    }

    /**
     * Generates an item-Rect map which indicates where member
     * items are located in the given ListView.
     *
     * @param listView A list view
     * @param adapter An array adapter which contains an array of items.
     * @return A map of items and bounds of their views located in the given list view.
     */
    public static <E> HashMap<E, Rect> getItemBoundMap(ListView listView,
            ArrayAdapter<E> adapter) {
        HashMap<E, Rect> itemBoundMap = new HashMap<>();
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
            int position = firstVisiblePosition + i;
            E item = adapter.getItem(position);
            View view = listView.getChildAt(i);
            itemBoundMap.put(item,
                    new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom()));
        }
        return itemBoundMap;
    }

    /**
     * Generates an item-BitmapDrawable map which stores snapshots
     * of member items in the given ListView.
     *
     * @param context A context
     * @param listView A list view
     * @param adapter An array adapter which contains an array of items.
     * @return A map of items and snapshots of their views in the given list view.
     */
    public static <E> HashMap<E, BitmapDrawable> getItemBitmapMap(Context context,
            ListView listView, ArrayAdapter<E> adapter) {
        HashMap<E, BitmapDrawable> itemBitmapMap = new HashMap<>();
        int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
            int position = firstVisiblePosition + i;
            E item = adapter.getItem(position);
            View view = listView.getChildAt(i);
            itemBitmapMap.put(item, getViewBitmap(context, view));
        }
        return itemBitmapMap;
    }

    private static BitmapDrawable getViewBitmap(Context context, View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return new BitmapDrawable(context.getResources(), bitmap);
    }
}
