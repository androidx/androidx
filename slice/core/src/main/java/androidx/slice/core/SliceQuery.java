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
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@RequiresApi(19)
public class SliceQuery {

    /**
     */
    public static boolean hasAnyHints(SliceItem item, String... hints) {
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
    public static boolean hasHints(SliceItem item, String... hints) {
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
    public static boolean hasHints(Slice item, String... hints) {
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
    public static SliceItem findNotContaining(SliceItem container, List<SliceItem> list) {
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
    private static boolean contains(SliceItem container, final SliceItem item) {
        if (container == null || item == null) return false;
        return findSliceItem(toQueue(container), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem s) {
                return s == item;
            }
        }) != null;
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, String format) {
        return findAll(s, format, (String[]) null, null);
    }

    /**
     */
    public static List<SliceItem> findAll(Slice s, String format, String hints, String nonHints) {
        return findAll(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, String format, String hints,
            String nonHints) {
        return findAll(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static List<SliceItem> findAll(Slice s, final String format, final String[] hints,
            final String[] nonHints) {
        ArrayList<SliceItem> ret = new ArrayList<>();
        findAll(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }, ret);
        return ret;
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        ArrayList<SliceItem> ret = new ArrayList<>();
        findAll(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }, ret);
        return ret;
    }

    /**
     */
    public static SliceItem find(Slice s, String format, String hints, String nonHints) {
        return find(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static SliceItem find(Slice s, String format) {
        return find(s, format, (String[]) null, null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, String format) {
        return find(s, format, (String[]) null, null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, String format, String hints, String nonHints) {
        return find(s, format, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static SliceItem find(Slice s, final String format, final String[] hints,
            final String[] nonHints) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        });
    }

    /**
     */
    public static SliceItem findSubtype(Slice s, final String format, final String subtype) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        });
    }

    /**
     */
    public static SliceItem findSubtype(SliceItem s, final String format, final String subtype) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        });
    }

    /**
     */
    public static SliceItem find(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        if (s == null) return null;
        return findSliceItem(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean checkFormat(SliceItem item, String format) {
        return format == null || format.equals(item.getFormat());
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean checkSubtype(SliceItem item, String subtype) {
        return subtype == null || subtype.equals(item.getSubType());
    }

    private static Deque<SliceItem> toQueue(Slice item) {
        Deque<SliceItem> q = new ArrayDeque<>();
        Collections.addAll(q, item.getItemArray());
        return q;
    }

    private static Deque<SliceItem> toQueue(SliceItem item) {
        Deque<SliceItem> q = new ArrayDeque<>();
        q.add(item);
        return q;
    }

    private static SliceItem findSliceItem(final Deque<SliceItem> items, Filter<SliceItem> f) {
        while (!items.isEmpty()) {
            SliceItem item = items.poll();
            if (f.filter(item)) {
                return item;
            }
            if (FORMAT_SLICE.equals(item.getFormat())
                    || FORMAT_ACTION.equals(item.getFormat())) {
                Collections.addAll(items, item.getSlice().getItemArray());
            }
        }
        return null;
    }

    private static void findAll(final Deque<SliceItem> items, Filter<SliceItem> f,
            List<SliceItem> out) {
        while (!items.isEmpty()) {
            SliceItem item = items.poll();
            if (f.filter(item)) {
                out.add(item);
            }
            if (FORMAT_SLICE.equals(item.getFormat())
                    || FORMAT_ACTION.equals(item.getFormat())) {
                Collections.addAll(items, item.getSlice().getItemArray());
            }
        }
    }

    /**
     * Finds an item matching provided params that is a direct child of the slice.
     */
    public static SliceItem findTopLevelItem(Slice s, final String format, final String subtype,
            final String[] hints, final String[] nonHints) {
        SliceItem[] items = s.getItemArray();
        for (int i = 0; i < items.length; i++) {
            SliceItem item = items[i];
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
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public static SliceItem findItem(Slice s, final Uri uri) {
        return findSliceItem(toQueue(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem input) {
                if (FORMAT_ACTION.equals(input.getFormat()) || FORMAT_SLICE.equals(
                        input.getFormat())) {
                    return uri.equals(input.getSlice().getUri());
                }
                return false;
            }
        });
    }

    private interface Filter<T> {
        boolean filter(T input);
    }

    private SliceQuery() {
    }
}
