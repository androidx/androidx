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

import android.text.TextUtils;

import androidx.annotation.RestrictTo;
import androidx.slice.Slice;
import androidx.slice.SliceItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utilities for finding content within a Slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SliceQuery {

    /**
     */
    public static boolean hasAnyHints(SliceItem item, String... hints) {
        if (hints == null) return false;
        List<String> itemHints = item.getHints();
        for (String hint : hints) {
            if (itemHints.contains(hint)) {
                return true;
            }
        }
        return false;
    }

    /**
     */
    public static boolean hasHints(SliceItem item, String... hints) {
        if (hints == null) return true;
        List<String> itemHints = item.getHints();
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !itemHints.contains(hint)) {
                return false;
            }
        }
        return true;
    }

    /**
     */
    public static boolean hasHints(Slice item, String... hints) {
        if (hints == null) return true;
        List<String> itemHints = item.getHints();
        for (String hint : hints) {
            if (!TextUtils.isEmpty(hint) && !itemHints.contains(hint)) {
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
        return findFirst(filter(stream(container), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem s) {
                return s == item;
            }
        }), null) != null;
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
        return collect(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }));
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        return collect(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }));
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
        return findFirst(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }), null);
    }

    /**
     */
    public static SliceItem findSubtype(Slice s, final String format, final String subtype) {
        return findFirst(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        }), null);
    }

    /**
     */
    public static SliceItem findSubtype(SliceItem s, final String format, final String subtype) {
        return findFirst(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        }), null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        return findFirst(filter(stream(s), new Filter<SliceItem>() {
            @Override
            public boolean filter(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }), null);
    }

    private static boolean checkFormat(SliceItem item, String format) {
        return format == null || format.equals(item.getFormat());
    }

    private static boolean checkSubtype(SliceItem item, String subtype) {
        return subtype == null || subtype.equals(item.getSubType());
    }

    /**
     */
    public static Iterator<SliceItem> stream(SliceItem slice) {
        ArrayList<SliceItem> items = new ArrayList<>();
        items.add(slice);
        return getSliceItemStream(items);
    }

    /**
     */
    public static Iterator<SliceItem> stream(Slice slice) {
        ArrayList<SliceItem> items = new ArrayList<>();
        items.addAll(slice.getItems());
        return getSliceItemStream(items);
    }

    /**
     */
    private static Iterator<SliceItem> getSliceItemStream(final ArrayList<SliceItem> items) {
        return new Iterator<SliceItem>() {
            @Override
            public boolean hasNext() {
                return items.size() != 0;
            }

            @Override
            public SliceItem next() {
                SliceItem item = items.remove(0);
                if (FORMAT_SLICE.equals(item.getFormat())
                        || FORMAT_ACTION.equals(item.getFormat())) {
                    items.addAll(item.getSlice().getItems());
                }
                return item;
            }
        };
    }

    private static <T> List<T> collect(Iterator<T> iter) {
        List<T> list = new ArrayList<>();
        while (iter.hasNext()) list.add(iter.next());
        return list;
    }

    private static <T> Iterator<T> filter(final Iterator<T> input, final Filter<T> f) {
        return new Iterator<T>() {
            T mNext = findNext();

            private T findNext() {
                while (input.hasNext()) {
                    T i = input.next();
                    if (f.filter(i)) {
                        return i;
                    }
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                return mNext != null;
            }

            @Override
            public T next() {
                T ret = mNext;
                mNext = findNext();
                return ret;
            }
        };
    }

    private static <T> T findFirst(Iterator<T> filter, T def) {
        while (filter.hasNext()) {
            T r = filter.next();
            if (r != null) return r;
        }
        return def;
    }

    private interface Filter<T> {
        boolean filter(T input);
    }

    private SliceQuery() {
    }
}
