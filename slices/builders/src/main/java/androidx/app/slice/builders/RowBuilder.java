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

package androidx.app.slice.builders;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_SELECTED;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import static androidx.app.slice.core.SliceHints.SUBTYPE_TOGGLE;

import android.app.PendingIntent;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.annotation.NonNull;

import java.util.ArrayList;

import androidx.app.slice.Slice;
import androidx.app.slice.SliceItem;
import androidx.app.slice.core.SliceHints;

/**
 * Sub-builder to construct a row of slice content.
 * <p>
 * Row content can have:
 * <ul>
 *     <li>Title item - This is only displayed if this is a list item in a large template, it
 *     will not be shown if this template is being used for small format. The item appears at the
 *     start of the template. There can only be one title item displayed, and it could be a
 *     timestamp, image, or a tappable icon.</li>
 *     <li>Title - Formatted as a title.</li>
 *     <li>Subtitle - Appears below the title (if one exists) and is formatted as normal text.</li>
 *     <li>End item -  Appears at the end of the template, there can be multiple end items but they
 *     are only shown if there's enough space. End items can be a timestamp, image, or a
 *     tappable icon.</li>
 * </ul>
 * </p>
 *
 * @see ListBuilder
 */
public class RowBuilder extends TemplateSliceBuilder {

    private boolean mIsHeader;
    private PendingIntent mContentIntent;
    private SliceItem mTitleItem;
    private SliceItem mSubtitleItem;
    private SliceItem mStartItem;
    private ArrayList<SliceItem> mEndItems = new ArrayList<>();

    public RowBuilder(ListBuilder parent) {
        super(parent.createChildBuilder());
    }

    public RowBuilder(Uri uri) {
        super(new Slice.Builder(uri));
    }

    /**
     * Sets this row to be considered the header of the slice. This means that when the slice is
     * requested to be show in small format, it will display only the contents specified in this
     * row. If a slice has no header specified, the first row item will be used in the small format.
     */
    public RowBuilder setIsHeader(boolean isHeader) {
        mIsHeader = isHeader;
        return this;
    }

    /**
     * Sets the title item to be the provided timestamp.
     * <p>
     * There can only be one title item, this will replace any other title
     * items that may have been set.
     */
    public RowBuilder setTitleItem(long timeStamp) {
        mStartItem = new SliceItem(timeStamp, FORMAT_TIMESTAMP, null, new String[0]);
        return this;
    }

    /**
     * Sets the title item to be the provided icon.
     * <p>
     * There can only be one title item, this will replace any other title
     * items that may have been set.
     */
    public RowBuilder setTitleItem(Icon icon) {
        mStartItem = new SliceItem(icon, FORMAT_IMAGE, null, new String[0]);
        return this;
    }

    /**
     * Sets the title item to be a tappable icon.
     * <p>
     * There can only be one title item, this will replace any other title
     * items that may have been set.
     */
    public RowBuilder setTitleItem(@NonNull PendingIntent action, @NonNull Icon icon) {
        Slice actionSlice = new Slice.Builder(getBuilder()).addIcon(icon, null).build();
        mStartItem = new SliceItem(action, actionSlice, FORMAT_ACTION, null, new String[0]);
        return this;
    }

    /**
     * Sets the action to be invoked if the user taps on the main content of the template.
     */
    public RowBuilder setContentIntent(@NonNull PendingIntent action) {
        mContentIntent = action;
        return this;
    }

    /**
     * Sets the title text.
     */
    public RowBuilder setTitle(CharSequence title) {
        mTitleItem = new SliceItem(title, FORMAT_TEXT, null, new String[] {HINT_TITLE});
        return this;
    }

    /**
     * Sets the subtitle text.
     */
    public RowBuilder setSubtitle(CharSequence subtitle) {
        mSubtitleItem = new SliceItem(subtitle, FORMAT_TEXT, null, new String[0]);
        return this;
    }

    /**
     * Adds a timestamp to be displayed at the end of the row.
     */
    public RowBuilder addEndItem(long timeStamp) {
        // TODO -- should multiple timestamps be allowed at the end of the row?
        mEndItems.add(new SliceItem(timeStamp, FORMAT_TIMESTAMP, null, new String[0]));
        return this;
    }

    /**
     * Adds an icon to be displayed at the end of the row.
     */
    public RowBuilder addEndItem(Icon icon) {
        mEndItems.add(new SliceItem(icon, FORMAT_IMAGE, null,
                new String[] {HINT_NO_TINT, HINT_LARGE}));
        return this;
    }

    /**
     * Adds a tappable icon to be displayed at the end of the row.
     */
    public RowBuilder addEndItem(@NonNull PendingIntent action, @NonNull Icon icon) {
        Slice actionSlice = new Slice.Builder(getBuilder()).addIcon(icon, null).build();
        mEndItems.add(new SliceItem(action, actionSlice, FORMAT_ACTION, null, new String[0]));
        return this;
    }

    /**
     * Adds a toggle action to the template. If there is a toggle to display, any end items
     * that were added will not be shown.
     */
    public RowBuilder addToggle(@NonNull PendingIntent action, boolean isChecked) {
        @Slice.SliceHint String[] hints = isChecked
                ? new String[] {SUBTYPE_TOGGLE, HINT_SELECTED}
                : new String[] {SUBTYPE_TOGGLE};
        Slice s = new Slice.Builder(getBuilder()).addHints(hints).build();
        mEndItems.add(0, new SliceItem(action, s, FORMAT_ACTION, null, hints));
        return this;
    }

    /**
     * Adds a toggle action to the template with custom icons to represent checked and unchecked
     * state. If there is a toggle to display, any end items that were added will not be shown.
     */
    public RowBuilder addToggle(@NonNull PendingIntent action, @NonNull Icon icon,
            boolean isChecked) {
        @Slice.SliceHint String[] hints = isChecked
                ? new String[] {SliceHints.SUBTYPE_TOGGLE, HINT_SELECTED}
                : new String[] {SliceHints.SUBTYPE_TOGGLE};
        Slice actionSlice = new Slice.Builder(getBuilder())
                .addIcon(icon, null)
                .addHints(hints).build();
        mEndItems.add(0, new SliceItem(action, actionSlice, FORMAT_ACTION, null, hints));
        return this;
    }

    @Override
    public void apply(Slice.Builder b) {
        Slice.Builder wrapped = b;
        if (mContentIntent != null) {
            b = new Slice.Builder(wrapped);
        }
        if (mStartItem != null) {
            b.addItem(mStartItem);
        }
        if (mTitleItem != null) {
            b.addItem(mTitleItem);
        }
        if (mSubtitleItem != null) {
            b.addItem(mSubtitleItem);
        }
        for (int i = 0; i < mEndItems.size(); i++) {
            SliceItem item = mEndItems.get(i);
            b.addItem(item);
        }
        if (mContentIntent != null) {
            wrapped.addAction(mContentIntent, b.build(), null);
        }
        wrapped.addHints(mIsHeader ? null : HINT_LIST_ITEM);
    }
}
