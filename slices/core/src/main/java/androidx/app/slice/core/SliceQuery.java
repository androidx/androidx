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

import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import androidx.app.slice.builders.SliceHints;

/**
 * Utilities for finding content within a Slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SliceQuery {

    /**
     * @return Whether this item is appropriate to be considered a "start" item, i.e. go in the
     *         front slot of a small slice.
     */
    public static boolean isStartType(SliceItem item) {
        final int type = item.getType();
        return !item.hasHint(SliceHints.HINT_TOGGLE)
                && ((type == SliceItem.TYPE_ACTION && (find(item, SliceItem.TYPE_IMAGE) != null))
                || type == SliceItem.TYPE_IMAGE
                || type == SliceItem.TYPE_TIMESTAMP);
    }

    /**
     * @return Finds the first slice that has non-slice children.
     */
    public static SliceItem findFirstSlice(SliceItem slice) {
        if (slice.getType() != SliceItem.TYPE_SLICE) {
            return slice;
        }
        List<SliceItem> items = slice.getSlice().getItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getType() == SliceItem.TYPE_SLICE) {
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
        if (item.getType() == SliceItem.TYPE_ACTION) {
            List<SliceItem> items = item.getSlice().getItems();
            boolean hasImage = false;
            for (int i = 0; i < items.size(); i++) {
                SliceItem child = items.get(i);
                if (child.getType() == SliceItem.TYPE_IMAGE && !hasImage) {
                    hasImage = true;
                } else if (child.getType() == SliceItem.TYPE_COLOR) {
                    continue;
                } else {
                    return false;
                }
            }
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
            if (item.getType() == SliceItem.TYPE_IMAGE) {
                return item;
            }
            if (!(item.getType() == SliceItem.TYPE_SLICE && item.hasHint(Slice.HINT_LIST))
                    && !item.hasHint(Slice.HINT_ACTIONS)
                    && !item.hasHint(Slice.HINT_LIST_ITEM)
                    && (item.getType() != SliceItem.TYPE_ACTION)) {
                SliceItem icon = SliceQuery.find(item, SliceItem.TYPE_IMAGE);
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
    private static boolean contains(SliceItem container, SliceItem item) {
        if (container == null || item == null) return false;
        return stream(container).filter(s -> (s == item)).findAny().isPresent();
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, int type) {
        return findAll(s, type, (String[]) null, null);
    }

    /**
     */
    public static List<SliceItem> findAll(Slice s, int type, String hints, String nonHints) {
        return findAll(s, type, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, int type, String hints, String nonHints) {
        return findAll(s, type, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static List<SliceItem> findAll(Slice s, int type, String[] hints,
            String[] nonHints) {
        return stream(s).filter(item -> (type == -1 || item.getType() == type)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)))
                .collect(Collectors.toList());
    }

    /**
     */
    public static List<SliceItem> findAll(SliceItem s, int type, String[] hints,
            String[] nonHints) {
        return stream(s).filter(item -> (type == -1 || item.getType() == type)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints)))
                .collect(Collectors.toList());
    }

    /**
     */
    public static SliceItem find(Slice s, int type, String hints, String nonHints) {
        return find(s, type, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static SliceItem find(Slice s, int type) {
        return find(s, type, (String[]) null, null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, int type) {
        return find(s, type, (String[]) null, null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, int type, String hints, String nonHints) {
        return find(s, type, new String[]{ hints }, new String[]{ nonHints });
    }

    /**
     */
    public static SliceItem find(Slice s, int type, String[] hints, String[] nonHints) {
        List<String> h = s.getHints();
        return stream(s).filter(item -> (item.getType() == type || type == -1)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints))).findFirst()
                .orElse(null);
    }

    /**
     */
    public static SliceItem find(SliceItem s, int type, String[] hints, String[] nonHints) {
        return stream(s).filter(item -> (item.getType() == type || type == -1)
                && (hasHints(item, hints) && !hasAnyHints(item, nonHints))).findFirst()
                .orElse(null);
    }

    /**
     */
    public static Stream<SliceItem> stream(SliceItem slice) {
        Queue<SliceItem> items = new LinkedList();
        items.add(slice);
        return getSliceItemStream(items);
    }

    /**
     */
    public static Stream<SliceItem> stream(Slice slice) {
        Queue<SliceItem> items = new LinkedList();
        items.addAll(slice.getItems());
        return getSliceItemStream(items);
    }

    /**
     */
    private static Stream<SliceItem> getSliceItemStream(Queue<SliceItem> items) {
        Iterator<SliceItem> iterator = new Iterator<SliceItem>() {
            @Override
            public boolean hasNext() {
                return items.size() != 0;
            }

            @Override
            public SliceItem next() {
                SliceItem item = items.poll();
                if (item.getType() == SliceItem.TYPE_SLICE
                        || item.getType() == SliceItem.TYPE_ACTION) {
                    items.addAll(item.getSlice().getItems());
                }
                return item;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
