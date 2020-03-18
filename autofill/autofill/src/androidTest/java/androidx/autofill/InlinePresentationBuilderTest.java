/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill;

import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static androidx.autofill.InlinePresentationBuilder.HINT_INLINE_ATTRIBUTION;
import static androidx.autofill.InlinePresentationBuilder.HINT_INLINE_END_ICON;
import static androidx.autofill.InlinePresentationBuilder.HINT_INLINE_START_ICON;
import static androidx.autofill.InlinePresentationBuilder.HINT_INLINE_SUBTITLE;
import static androidx.autofill.InlinePresentationBuilder.HINT_INLINE_TITLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.app.PendingIntent;
import android.app.slice.Slice;
import android.app.slice.SliceItem;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;

import androidx.autofill.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class InlinePresentationBuilderTest {
    private Instrumentation mInstrumentation;
    private Context mContext;
    PendingIntent mAttribution;

    @Before
    public void setup() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getContext();
        mAttribution = PendingIntent.getActivity(mContext, 0, new Intent(), 0);
    }

    @Test
    public void buildSlice_title() {
        Slice slice = new InlinePresentationBuilder("title")
                .setAttribution(mAttribution)
                .build();
        List<SliceItem> sliceItems = slice.getItems();
        assertEquals(2, sliceItems.size());
        verifyIsTitle(sliceItems.get(0));
        verifyIsAttribution(sliceItems.get(1), mAttribution);
    }

    @Test
    public void buildSlice_titleAndSubtitle() {
        Slice slice = new InlinePresentationBuilder("title")
                .setSubtitle("subtitle")
                .setAttribution(mAttribution)
                .build();
        List<SliceItem> sliceItems = slice.getItems();
        assertEquals(3, sliceItems.size());
        verifyIsTitle(sliceItems.get(0));
        verifyIsSubtitle(sliceItems.get(1));
        verifyIsAttribution(sliceItems.get(2), mAttribution);
    }

    @Test
    public void buildSlice_startIcon() {
        Icon icon = Icon.createWithResource(mContext, R.drawable.ic_settings);
        Slice slice = new InlinePresentationBuilder()
                .setStartIcon(icon)
                .setAttribution(mAttribution)
                .build();
        List<SliceItem> sliceItems = slice.getItems();
        assertEquals(2, sliceItems.size());
        verifyIsStartIcon(sliceItems.get(0), icon);
        verifyIsAttribution(sliceItems.get(1), mAttribution);
    }

    @Test
    public void buildSlice_startIconAndTitle() {
        Icon icon = Icon.createWithResource(mContext, R.drawable.ic_settings);
        Slice slice = new InlinePresentationBuilder("title")
                .setStartIcon(icon)
                .setAttribution(mAttribution)
                .build();
        List<SliceItem> sliceItems = slice.getItems();
        assertEquals(3, sliceItems.size());
        verifyIsStartIcon(sliceItems.get(0), icon);
        verifyIsTitle(sliceItems.get(1));
        verifyIsAttribution(sliceItems.get(2), mAttribution);
    }

    @Test
    public void buildSlice_titleAndEndIcon() {
        Icon icon = Icon.createWithResource(mContext, R.drawable.ic_settings);
        Slice slice = new InlinePresentationBuilder("title")
                .setEndIcon(icon)
                .setAttribution(mAttribution)
                .build();
        List<SliceItem> sliceItems = slice.getItems();
        assertEquals(3, sliceItems.size());
        verifyIsTitle(sliceItems.get(0));
        verifyIsEndIcon(sliceItems.get(1), icon);
        verifyIsAttribution(sliceItems.get(2), mAttribution);
    }

    @Test
    public void buildSlice_noFieldSet_exception() {
        try {
            new InlinePresentationBuilder()
                    .setAttribution(mAttribution)
                    .build();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void buildSlice_subtitleWithoutTitle_exception() {
        try {
            new InlinePresentationBuilder()
                    .setSubtitle("subtitle")
                    .setAttribution(mAttribution)
                    .build();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    @Test
    public void buildSlice_sliceWithoutAttribution_exception() {
        try {
            new InlinePresentationBuilder("title").build();
        } catch (IllegalStateException e) {
            return;
        }
        fail();
    }

    private static void verifyIsTitle(SliceItem item) {
        assertEquals(FORMAT_TEXT, item.getFormat());
        assertEquals("title", item.getText());
        assertEquals(1, item.getHints().size());
        assertTrue(item.getHints().contains(HINT_INLINE_TITLE));
    }

    private static void verifyIsSubtitle(SliceItem item) {
        assertEquals(FORMAT_TEXT, item.getFormat());
        assertEquals("subtitle", item.getText());
        assertEquals(1, item.getHints().size());
        assertTrue(item.getHints().contains(HINT_INLINE_SUBTITLE));
    }

    private static void verifyIsStartIcon(SliceItem item, Icon icon) {
        assertEquals(FORMAT_IMAGE, item.getFormat());
        assertEquals(icon, item.getIcon());
        assertEquals(1, item.getHints().size());
        assertTrue(item.getHints().contains(HINT_INLINE_START_ICON));
    }

    private static void verifyIsEndIcon(SliceItem item, Icon icon) {
        assertEquals(FORMAT_IMAGE, item.getFormat());
        assertEquals(icon, item.getIcon());
        assertEquals(1, item.getHints().size());
        assertTrue(item.getHints().contains(HINT_INLINE_END_ICON));
    }

    private static void verifyIsAttribution(SliceItem item, PendingIntent action) {
        assertEquals(FORMAT_ACTION, item.getFormat());
        assertEquals(action, item.getAction());
        assertEquals(1, item.getHints().size());
        assertTrue(item.getHints().contains(HINT_INLINE_ATTRIBUTION));
    }
}
