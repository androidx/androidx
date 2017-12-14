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

package androidx.app.slice.core;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.annotation.TargetApi;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;

/**
 * Utilities for finding content within a Slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO: Not expect 24.
@TargetApi(24)
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
        return stream(container).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem s) {
                return s == item;
            }
        }).findAny().isPresent();
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
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }).collect(Collectors.<SliceItem>toList());
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }).collect(Collectors.<SliceItem>toList());
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
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }).findFirst().orElse(null);
    }

    /**
     */
    public static SliceItem findSubtype(Slice s, final String format, final String subtype) {
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        }).findFirst().orElse(null);
    }

    /**
     */
    public static SliceItem findSubtype(SliceItem s, final String format, final String subtype) {
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format) && checkSubtype(item, subtype);
            }
        }).findFirst().orElse(null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, final String format, final String[] hints,
            final String[] nonHints) {
        return stream(s).filter(new Predicate<SliceItem>() {
            @Override
            public boolean test(SliceItem item) {
                return checkFormat(item, format)
                        && (hasHints(item, hints) && !hasAnyHints(item, nonHints));
            }
        }).findFirst().orElse(null);
    }

    private static boolean checkFormat(SliceItem item, String format) {
        return format == null || format.equals(item.getFormat());
    }

    private static boolean checkSubtype(SliceItem item, String subtype) {
        return subtype == null || subtype.equals(item.getSubType());
    }

    /**
     */
    public static Stream<SliceItem> stream(SliceItem slice) {
        Queue<SliceItem> items = new ArrayDeque<>();
        items.add(slice);
        return getSliceItemStream(items);
    }

    /**
     */
    public static Stream<SliceItem> stream(Slice slice) {
        Queue<SliceItem> items = new ArrayDeque<>();
        items.addAll(slice.getItems());
        return getSliceItemStream(items);
    }

    /**
     */
    private static Stream<SliceItem> getSliceItemStream(final Queue<SliceItem> items) {
        Iterator<SliceItem> iterator = new Iterator<SliceItem>() {
            @Override
            public boolean hasNext() {
                return items.size() != 0;
            }

            @Override
            public SliceItem next() {
                SliceItem item = items.poll();
                if (FORMAT_SLICE.equals(item.getFormat())
                        || FORMAT_ACTION.equals(item.getFormat())) {
                    items.addAll(item.getSlice().getItems());
                }
                return item;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
