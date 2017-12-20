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

package androidx.app.slice;


import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SliceXmlTest {

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForAction() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addAction(null, null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForRemoteInput() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addRemoteInput(null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowForImage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addIcon(null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions());
    }

    @Test
    public void testNoThrowForAction() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addAction(null, null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions().setActionMode(SliceUtils.SerializeOptions.MODE_REMOVE));
    }

    @Test
    public void testNoThrowForRemoteInput() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addRemoteInput(null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions().setActionMode(SliceUtils.SerializeOptions.MODE_REMOVE));
    }

    @Test
    public void testNoThrowForImage() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Slice s = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addIcon(null, null)
                .build();
        SliceUtils.serializeSlice(s, mContext, outputStream, "UTF-8", new SliceUtils
                .SerializeOptions().setImageMode(SliceUtils.SerializeOptions.MODE_REMOVE));
    }

    @Test
    public void testSerialization() throws IOException {
        Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
        new Canvas(b).drawColor(0xffff0000);
        // Create a slice containing all the types in a hierarchy.
        Slice before = new Slice.Builder(Uri.parse("content://pkg/slice"))
                .addSubSlice(new Slice.Builder(Uri.parse("content://pkg/slice/sub"))
                        .addTimestamp(System.currentTimeMillis(), null, "Hint")
                        .build())
                .addIcon(Icon.createWithBitmap(b), null)
                .addText("Some text", null)
                .addAction(null, new Slice.Builder(Uri.parse("content://pkg/slice/sub"))
                        .addText("Action text", null)
                        .build(), null)
                .addInt(0xff00ff00, "subtype")
                .addHints("Hint 1", "Hint 2")
                .build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        SliceUtils.serializeSlice(before, mContext, outputStream, "UTF-8",
                new SliceUtils.SerializeOptions()
                        .setImageMode(SliceUtils.SerializeOptions.MODE_DISABLE)
                        .setActionMode(SliceUtils.SerializeOptions.MODE_DISABLE));

        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        Slice after = SliceUtils.parseSlice(inputStream, "UTF-8");

        assertEquivalent(before, after);
    }

    private void assertEquivalent(Slice desired, Slice actual) {
        assertEquals(desired.getUri(), actual.getUri());
        assertEquals(desired.getHints(), actual.getHints());
        assertEquals(desired.getItems().size(), actual.getItems().size());

        for (int i = 0; i < desired.getItems().size(); i++) {
            assertEquivalent(desired.getItems().get(i), actual.getItems().get(i));
        }
    }

    private void assertEquivalent(SliceItem desired, SliceItem actual) {
        boolean isSliceType = FORMAT_SLICE.equals(desired.getFormat())
                || FORMAT_ACTION.equals(desired.getFormat());
        if (isSliceType) {
            assertTrue(FORMAT_SLICE.equals(actual.getFormat())
                    || FORMAT_ACTION.equals(actual.getFormat()));
        } else {
            assertEquals(desired.getFormat(), actual.getFormat());
            if (FORMAT_TEXT.equals(desired.getFormat())) {
                assertEquals(String.valueOf(desired.getText()), String.valueOf(actual.getText()));
            }
        }
    }
}
