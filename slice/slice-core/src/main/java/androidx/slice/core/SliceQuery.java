/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.core;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Utilities for finding content within a Slice.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(19)
@Deprecated
public class SliceQuery {

    /**
     */
    public static boolean hasAnyHints(@NonNull SliceItem item, @Nullable String... hints) {
        if (hints == null) return false;
        for (String hint : hints) {
            if (item.hasHint(hint)) {
                return true;
            }
        }
        return false;
    }

    /**
     */
    public static boolean hasHints(@NonNull SliceItem item, @Nullable String... hints) {
        if (hints == null) return true;
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !item.hasHint(hint)) {
                return false;
            }
        }
        return true;
    }

    /**
     */
    @SuppressWarnings("unused")
    public static boolean hasHints(@NonNull Slice item, @Nullable String... hints) {
        if (hints == null) return true;
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !item.hasHint(hint)) {
                return false;
            }
        }
        return true;
    }

    /**
     */
    @SuppressWarnings("unused")
    @Nullable
    public static SliceItem findNotContaining(@Nullable SliceItem container,
            @NonNull List<SliceItem> list) {
        SliceItem ret = null;
        while (ret == null && list.size() != 0) {
            SliceItem remove = list.remove(0);
            if (!contains(container, remove)) {
                ret = remove;
            }
        }
        return ret;
    }

    /**
     */
    private static boolean contains(@Nullable SliceItem container, @Nullable final SliceItem item) {
        if (container == null || item == null) return false;
        return findSliceItem(toQueue(container), s -> s == item) != null;
    }

    /**
     */
    @NonNull
    public static List<SliceItem> findAll(@NonNull SliceItem s, @Nullable String format) {
        return findAll(s, format, (String[]) null, null);
    }

    /**
     */
    @SuppressWarnings("unused")
    @NonNull
    public static List<SliceItem> findAll(@NonNull Slice s, @Nullable String format,
            @Nullable String hints, @Nullable String nonHints) {
        return findAll(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    @NonNull
    public static List<SliceItem> findAll(@NonNull SliceItem s, @Nullable String format,
            @Nullable String hints, @Nullable String nonHints) {
        return findAll(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    @NonNull
    public static List<SliceItem> findAll(@NonNull Slice s, @Nullable final String format,
            @Nullable final String[] hints, @Nullable final String[] nonHints) {
        ArrayList<SliceItem> ret = new ArrayList<>();
        findAll(toQueue(s), item -> checkFormat(item, format)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)), ret);
        return ret;
    }

    /**
     */
    @NonNull
    public static List<SliceItem> findAll(@NonNull SliceItem s, @Nullable final String format,
            @Nullable final String[] hints, @Nullable final String[] nonHints) {
        ArrayList<SliceItem> ret = new ArrayList<>();
        findAll(toQueue(s), item -> checkFormat(item, format)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)), ret);
        return ret;
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable Slice s, @Nullable String format, @Nullable String hints,
            @Nullable String nonHints) {
        return find(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable Slice s, @Nullable String format) {
        return find(s, format, (String[]) null, null);
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable SliceItem s, @Nullable String format) {
        return find(s, format, (String[]) null, null);
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable SliceItem s, @Nullable String format,
            @Nullable String hints, @Nullable String nonHints) {
        return find(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable Slice s, @Nullable final String format,
            @Nullable final String[] hints, @Nullable final String[] nonHints) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), item -> checkFormat(item, format)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)));
    }

    /**
     */
    @Nullable
    public static SliceItem findSubtype(@Nullable Slice s, @Nullable final String format,
            @Nullable final String subtype) {
        if (s == null) return null;
        return findSliceItem(toQueue(s),
                item -> checkFormat(item, format) && checkSubtype(item, subtype));
    }

    /**
     */
    @Nullable
    public static SliceItem findSubtype(@Nullable SliceItem s, @Nullable final String format,
            @Nullable final String subtype) {
        if (s == null) return null;
        return findSliceItem(toQueue(s),
                item -> checkFormat(item, format) && checkSubtype(item, subtype));
    }

    /**
     */
    @Nullable
    public static SliceItem find(@Nullable SliceItem s, @Nullable final String format,
            @Nullable final String[] hints, @Nullable final String[] nonHints) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), item -> checkFormat(item, format)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean checkFormat(@NonNull SliceItem item, @Nullable String format) {
        return format == null || format.equals(item.getFormat());
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean checkSubtype(@NonNull SliceItem item, @Nullable String subtype) {
        return subtype == null || subtype.equals(item.getSubType());
    }

    private static @NonNull Deque<SliceItem> toQueue(@NonNull Slice item) {
        Deque<SliceItem> q = new ArrayDeque<>();
        Collections.addAll(q, item.getItemArray());
        return q;
    }

    private static @NonNull Deque<SliceItem> toQueue(@NonNull SliceItem item) {
        Deque<SliceItem> q = new ArrayDeque<>();
        q.add(item);
        return q;
    }

    @Nullable
    private static SliceItem findSliceItem(@NonNull final Deque<SliceItem> items,
            @NonNull Filter<SliceItem> f) {
        while (!items.isEmpty()) {
            SliceItem item = items.poll();
            if (f.filter(item)) {
                return item;
            }
            if (item != null && (FORMAT_SLICE.equals(item.getFormat())
                    || FORMAT_ACTION.equals(item.getFormat()))) {
                Collections.addAll(items, item.getSlice().getItemArray());
            }
        }
        return null;
    }

    private static void findAll(@NonNull final Deque<SliceItem> items,
            @NonNull Filter<SliceItem> f, @NonNull List<SliceItem> out) {
        while (!items.isEmpty()) {
            SliceItem item = items.poll();
            if (f.filter(item)) {
                out.add(item);
            }
            if (item != null && (FORMAT_SLICE.equals(item.getFormat())
                    || FORMAT_ACTION.equals(item.getFormat()))) {
                Collections.addAll(items, item.getSlice().getItemArray());
            }
        }
    }

    /**
     * Finds an item matching provided params that is a direct child of the slice.
     */
    @Nullable
    public static SliceItem findTopLevelItem(@NonNull Slice s, @Nullable final String format,
            @Nullable final String subtype, @Nullable final String[] hints,
            @Nullable final String[] nonHints) {
        SliceItem[] items = s.getItemArray();
        for (SliceItem item : items) {
            if (checkFormat(item, format)
                    && checkSubtype(item, subtype)
                    && hasHints(item, hints)
                    && !hasAnyHints(item, nonHints)) {
                return item;
            }
        }
        return null;
    }

    /**
     */
    @Nullable
    public static SliceItem findItem(@NonNull Slice s, @NonNull final Uri uri) {
        return findSliceItem(toQueue(s), input -> {
            if (FORMAT_ACTION.equals(input.getFormat()) || FORMAT_SLICE.equals(
                    input.getFormat())) {
                return uri.equals(input.getSlice().getUri());
            }
            return false;
        });
    }

    private interface Filter<T> {
        boolean filter(T input);
    }

    private SliceQuery() {
    }
}
