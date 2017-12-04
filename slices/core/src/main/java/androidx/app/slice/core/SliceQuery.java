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

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

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
     * @return Whether this item is appropriate to be considered a "start" item, i.e. go in the
     *         front slot of a small slice.
     */
    public static boolean isStartType(SliceItem item) {
        final String type = item.getFormat();
        return (!item.hasHint(SliceHints.SUBTYPE_TOGGLE)
                && (FORMAT_ACTION.equals(type) && (find(item, FORMAT_IMAGE) != null)))
                || FORMAT_IMAGE.equals(type)
                || FORMAT_TIMESTAMP.equals(type);
    }

    /**
     * @return Finds the first slice that has non-slice children.
     */
    public static SliceItem findFirstSlice(SliceItem slice) {
        if (!FORMAT_SLICE.equals(slice.getFormat())) {
            return slice;
        }
        List<SliceItem> items = slice.getSlice().getItems();
        for (int i = 0; i < items.size(); i++) {
            if (FORMAT_SLICE.equals(items.get(i).getFormat())) {
                SliceItem childSlice = items.get(i);
                return findFirstSlice(childSlice);
            } else {
                // Doesn't have slice children so return it
                return slice;
            }
        }
        // Slices all the way down, just return it
        return slice;
    }

    /**
     * @return Whether this item is a simple action, i.e. an action that only has an icon.
     */
    public static boolean isSimpleAction(SliceItem item) {
        if (FORMAT_ACTION.equals(item.getFormat())) {
            List<SliceItem> items = item.getSlice().getItems();
            boolean hasImage = false;
            for (int i = 0; i < items.size(); i++) {
                SliceItem child = items.get(i);
                if (FORMAT_IMAGE.equals(child.getFormat()) && !hasImage) {
                    hasImage = true;
                } else if (FORMAT_COLOR.equals(child.getFormat())) {
                    continue;
                } else {
                    return false;
                }
            }
            return hasImage;
        }
        return false;
    }

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
    public static SliceItem getPrimaryIcon(Slice slice) {
        for (SliceItem item : slice.getItems()) {
            if (FORMAT_IMAGE.equals(item.getFormat())) {
                return item;
            }
            if (!(FORMAT_SLICE.equals(item.getFormat()) && item.hasHint(HINT_LIST))
                    && !item.hasHint(HINT_ACTIONS)
                    && !item.hasHint(HINT_LIST_ITEM)
                    && !FORMAT_ACTION.equals(item.getFormat())) {
                SliceItem icon = SliceQuery.find(item, FORMAT_IMAGE);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
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
