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

package androidx.slice;

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_LIST;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceProvider.SLICE_TYPE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.app.PendingIntent.CanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.core.test.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceTest {

    public static boolean sFlag = false;

    private static final Uri BASE_URI = Uri.parse("content://androidx.slice.core.test/");
    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testProcess() {
        sFlag = false;
        Slice.bindSlice(mContext,
                BASE_URI.buildUpon().appendPath("set_flag").build(),
                Collections.<SliceSpec>emptySet());
        assertFalse(sFlag);
    }

    @Test
    public void testType() {
        assertEquals(SLICE_TYPE, mContext.getContentResolver().getType(BASE_URI));
    }

    @Test
    public void testSliceUri() {
        Slice s = Slice.bindSlice(mContext, BASE_URI, Collections.<SliceSpec>emptySet());
        assertEquals(BASE_URI, s.getUri());
    }

    @Test
    public void testSubSlice() {
        Uri uri = BASE_URI.buildUpon().appendPath("subslice").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_SLICE, item.getFormat());
        assertEquals("subslice", item.getSubType());
        // The item should start with the same Uri as the parent, but be different.
        assertTrue(item.getSlice().getUri().toString().startsWith(uri.toString()));
        assertNotEquals(uri, item.getSlice().getUri());
    }

    @Test
    public void testText() {
        Uri uri = BASE_URI.buildUpon().appendPath("text").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_TEXT, item.getFormat());
        // TODO: Test spannables here.
        assertEquals("Expected text", item.getText());
    }

    @Test
    public void testIcon() {
        Uri uri = BASE_URI.buildUpon().appendPath("icon").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_IMAGE, item.getFormat());
        assertEquivalent(IconCompat.createWithResource(mContext, R.drawable.size_48x48),
                item.getIcon());
    }

    private void assertEquivalent(IconCompat first, IconCompat second) {
        assertEquals(first.getType(), second.getType());
        assertEquals(first.getResId(), second.getResId());
        assertEquals(first.getResPackage(), second.getResPackage());
    }

    @Test
    public void testAction() {
        sFlag = false;
        final CountDownLatch latch = new CountDownLatch(1);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sFlag = true;
                latch.countDown();
            }
        };
        mContext.registerReceiver(receiver,
                new IntentFilter(mContext.getPackageName() + ".action"));
        Uri uri = BASE_URI.buildUpon().appendPath("action").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_ACTION, item.getFormat());
        try {
            item.getAction().send();
        } catch (CanceledException e) {
        }

        try {
            latch.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(sFlag);
        mContext.unregisterReceiver(receiver);
    }

    @Test
    public void testInt() {
        Uri uri = BASE_URI.buildUpon().appendPath("int").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_INT, item.getFormat());
        assertEquals(0xff121212, item.getInt());
    }

    @Test
    public void testTimestamp() {
        Uri uri = BASE_URI.buildUpon().appendPath("timestamp").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());
        assertEquals(1, s.getItems().size());

        SliceItem item = s.getItems().get(0);
        assertEquals(FORMAT_LONG, item.getFormat());
        assertEquals(43, item.getTimestamp());
    }

    @Test
    public void testHints() {
        // Note this tests that hints are propagated through to the client but not that any specific
        // hints have any effects.
        Uri uri = BASE_URI.buildUpon().appendPath("hints").build();
        Slice s = Slice.bindSlice(mContext, uri, Collections.<SliceSpec>emptySet());
        assertEquals(uri, s.getUri());

        assertEquals(Arrays.asList(HINT_LIST), s.getHints());
        assertEquals(Arrays.asList(HINT_TITLE), s.getItems().get(0).getHints());
        assertEquals(Arrays.asList(HINT_NO_TINT, HINT_LARGE),
                s.getItems().get(1).getHints());
    }
}
