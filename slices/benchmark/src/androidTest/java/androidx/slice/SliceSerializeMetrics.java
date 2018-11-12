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


import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;

import androidx.benchmark.BenchmarkRule;
import androidx.benchmark.BenchmarkState;
import androidx.core.graphics.drawable.IconCompat;
import androidx.slice.benchmark.test.R;
import androidx.slice.core.SliceHints;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@MediumTest
@SdkSuppress(minSdkVersion = 19)
public class SliceSerializeMetrics {

    private static final boolean WRITE_SAMPLE_FILE = false;

    @Rule
    public BenchmarkRule mBenchmarkRule = new BenchmarkRule();

    private final Context mContext = InstrumentationRegistry.getContext();

    @Test
    public void testSerialization() throws Exception {
        final BenchmarkState state = mBenchmarkRule.getState();
        // Create a slice containing all the types in a hierarchy.
        Slice before = createSlice(mContext, Uri.parse("context://pkg/slice"), 3, 3, 6);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024 * 1024);

        if (WRITE_SAMPLE_FILE) {
            if (!mContext.getDataDir().exists()) {
                mContext.getDataDir().mkdir();
            }
            FileOutputStream file = new FileOutputStream(mContext.getDataDir() + "/slice.vp");
            SliceUtils.serializeSlice(before, mContext, file,
                    new SliceUtils.SerializeOptions()
                            .setImageMode(SliceUtils.SerializeOptions.MODE_CONVERT)
                            .setActionMode(SliceUtils.SerializeOptions.MODE_CONVERT));
            file.flush();
            file.close();
        }
        while (state.keepRunning()) {
            outputStream = new ByteArrayOutputStream(1024 * 1024);
            SliceUtils.serializeSlice(before, mContext, outputStream,
                    new SliceUtils.SerializeOptions()
                            .setImageMode(SliceUtils.SerializeOptions.MODE_CONVERT)
                            .setActionMode(SliceUtils.SerializeOptions.MODE_CONVERT));
        }

        byte[] resultBytes = outputStream.toByteArray();

        SliceUtils.SliceActionListener listener = new SliceUtils.SliceActionListener() {
            @Override
            public void onSliceAction(Uri actionUri, Context context, Intent intent) {
            }
        };
        Slice after = SliceUtils.parseSlice(mContext, new ByteArrayInputStream(resultBytes),
                "UTF-8", listener);
        assertEquivalentRoot(before, after);
        if (WRITE_SAMPLE_FILE) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(
                    "mv " + mContext.getDataDir() + "/slice.vp /sdcard/");
        }
    }

    @Test
    public void testDeserialization() throws IOException, SliceUtils.SliceParseException {
        final BenchmarkState state = mBenchmarkRule.getState();
        InputStream inputStream = mContext.getResources().openRawResource(R.raw.slice);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024 * 1024);
        copy(inputStream, outputStream);
        byte[] bytes = outputStream.toByteArray();
        inputStream = new ByteArrayInputStream(bytes);
        inputStream.mark(-1);

        SliceUtils.SliceActionListener listener = new SliceUtils.SliceActionListener() {
            @Override
            public void onSliceAction(Uri actionUri, Context context, Intent intent) {
            }
        };
        Slice after = SliceUtils.parseSlice(mContext, inputStream, "UTF-8", listener);
        while (state.keepRunning()) {
            inputStream.reset();
            after = SliceUtils.parseSlice(mContext, inputStream, "UTF-8", listener);
        }

        Slice before = createSlice(mContext, Uri.parse("context://pkg/slice"), 3, 3, 6);
        assertEquivalentRoot(before, after);
    }

    private static final int BUF_SIZE = 0x1000; // 4K

    public static long copy(InputStream from, OutputStream to) throws IOException {
        assertNotNull(from);
        assertNotNull(to);
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    private void assertEquivalentRoot(Slice desired, Slice actual) {
        assertEquals(desired.getUri(), actual.getUri());
        List<String> desiredHints = new ArrayList<>(desired.getHints());
        desiredHints.add(SliceHints.HINT_CACHED);
        assertEquals(desiredHints, actual.getHints());
        assertEquals(desired.getItems().size(), actual.getItems().size());

        for (int i = 0; i < desired.getItems().size(); i++) {
            assertEquivalent(desired.getItems().get(i), actual.getItems().get(i));
        }
    }

    private void assertEquivalent(SliceItem desired, SliceItem actual) {
        assertEquals(desired.getFormat(), actual.getFormat());
        boolean isSliceType = FORMAT_SLICE.equals(desired.getFormat())
                || FORMAT_ACTION.equals(desired.getFormat());
        if (!isSliceType) {
            if (FORMAT_TEXT.equals(desired.getFormat())) {
                assertEquals(String.valueOf(desired.getText()), String.valueOf(actual.getText()));
            }
        }
    }

    public static Slice createSlice(Context context, Uri uri, int width, int depth, int items) {
        Slice.Builder builder = new Slice.Builder(uri);
        if (depth > 0) {
            for (int i = 0; i < width; i++) {
                builder.addSubSlice(createSlice(context, uri.buildUpon()
                        .appendPath(String.valueOf(width))
                        .appendPath(String.valueOf(depth))
                        .appendPath(String.valueOf(items))
                        .appendPath(String.valueOf(i))
                        .build(), width, depth - 1, items));
            }
        }
        if (items > 1) {
            Bitmap b = Bitmap.createBitmap(50, 25, Bitmap.Config.ARGB_8888);
            new Canvas(b).drawColor(0xffff0000);
            builder.addIcon(IconCompat.createWithBitmap(b), null);
        }
        if (items > 2) {
            builder.addText("Some text", null);
        }
        if (items > 3) {
            PendingIntent pi = PendingIntent.getActivity(context, 0, new Intent(), 0);
            builder.addAction(pi,
                    new Slice.Builder(Uri.parse("content://pkg/slice/action"))
                            .addText("Action text", null)
                            .build(), null);
        }
        if (items > 4) {
            builder.addInt(0xff00ff00, "subtype");
        }
        if (items > 5) {
            builder.addIcon(IconCompat.createWithResource(context,
                    R.drawable.abc_slice_see_more_bg), null);
        }
        return builder.addHints("Hint 1", "Hint 2")
                .build();
    }
}
