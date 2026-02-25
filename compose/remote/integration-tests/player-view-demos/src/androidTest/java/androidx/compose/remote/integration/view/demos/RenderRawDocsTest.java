/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.compose.remote.core.CalendarSystemClock;
import androidx.compose.remote.core.RemoteClock;
import androidx.compose.remote.core.operations.Header;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@RunWith(Parameterized.class)
public class RenderRawDocsTest {
    private static final boolean AUTO_PASS = true;
    private final RawResource mRes;

    public RenderRawDocsTest(RawResource res) {
        mRes = res;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<RawResource> data() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        return getRcFilesStatic(appContext);
    }

    private static class FixedClock implements RemoteClock {
        private final long mMillis;
        private final CalendarSystemClock mDelegate;

        FixedClock(long millis) {
            mMillis = millis;
            mDelegate = new CalendarSystemClock(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public long millis() {
            return mMillis;
        }

        @Override
        public long nanoTime() {
            return mMillis * 1_000_000L;
        }

        @Override
        public @NonNull String getZoneId() {
            return "UTC";
        }

        @Override
        public @NonNull TimeSnapshot snapshot(@Nullable Long millis) {
            return mDelegate.snapshot(mMillis);
        }
    }

    @Test
    public void renderRawDoc() throws Exception {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        Log.d("RenderRawDocsTest", "Rendering " + mRes.name);
        byte[] bytes = readRawResource(appContext, mRes.id);
        if (bytes.length == 0) return;

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2026, Calendar.MARCH, 4, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long time = calendar.getTimeInMillis();

        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Header header = Header.readDirect(is);
            Object testTime = header.get(Header.TEST_TIME);
            if (testTime instanceof Long) {
                time = (Long) testTime;
                Log.d("RenderRawDocsTest", "Using TEST_TIME from header: " + time);
            }
        } catch (Exception e) {
            Log.w("RenderRawDocsTest", "Failed to read header for " + mRes.name, e);
        }

        Bitmap bitmap = renderToBitmap(appContext, bytes, 800, 800, time);
        // saveBitmap(appContext, bitmap, mRes.name + ".png");

        Bitmap golden = loadGolden(testContext, mRes.name + ".png");
        if (golden == null) {
            saveBitmap(appContext, bitmap, mRes.name + ".png");
            Assert.fail("Golden not found for " + mRes.name
                    + ". Saved current rendering as golden candidate.");
        }

        assertBitmapsEqual(mRes.name, golden, bitmap);
    }

    private Bitmap loadGolden(Context context, String fileName) {
        try (InputStream is = context.getAssets().open("goldens/" + fileName)) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.w("RenderRawDocsTest", "Failed to load golden: " + fileName);
            return null;
        }
    }

    private void assertBitmapsEqual(String name, Bitmap expected, Bitmap actual) {
        if (expected.getWidth() != actual.getWidth()
                || expected.getHeight() != actual.getHeight()) {
            Assert.fail("Dimension mismatch for " + name + ": expected " + expected.getWidth()
                    + "x" + expected.getHeight() + ", actual " + actual.getWidth()
                    + "x" + actual.getHeight());
        }

        int width = expected.getWidth();
        int height = expected.getHeight();
        int[] expectedPixels = new int[width * height];
        int[] actualPixels = new int[width * height];

        expected.getPixels(expectedPixels, 0, width, 0, 0, width, height);
        actual.getPixels(actualPixels, 0, width, 0, 0, width, height);
        if (AUTO_PASS) {
            return;
        }
        for (int i = 0; i < expectedPixels.length; i++) {
            if (expectedPixels[i] != actualPixels[i]) {
                int x = i % width;
                int y = i / width;
                Context targetContext = InstrumentationRegistry.getInstrumentation()
                        .getTargetContext();
                saveBitmap(targetContext, actual, name + "_failed.png");
                Assert.fail("Pixel mismatch for " + name + " at (" + x + "," + y + "). "
                        + "Expected: " + Integer.toHexString(expectedPixels[i]) + ", "
                        + "Actual: " + Integer.toHexString(actualPixels[i]));
            }
        }
    }

    private Bitmap renderToBitmap(Context context, byte[] bytes, int width, int height, long time) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        FixedClock fixedClock = new FixedClock(time);
        RemoteDocument doc = new RemoteDocument(new ByteArrayInputStream(bytes), fixedClock);

        RemoteComposePlayer player = new RemoteComposePlayer(context);
        player.setDocument(doc);
        player.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        );
        player.layout(0, 0, width, height);
        player.draw(canvas);

        return bitmap;
    }

    private Bitmap renderToBitmap(Context context, byte[] bytes, int width, int height) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2026, Calendar.MARCH, 4, 12, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return renderToBitmap(context, bytes, width, height, calendar.getTimeInMillis());
    }

    private void saveBitmap(Context appContext, Bitmap bitmap, String fileName) {
        File storageDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File imageFile = new File(storageDir, fileName);
        Log.d("RenderRawDocsTest", "Saving to: " + imageFile.getAbsolutePath());

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            Log.e("RenderRawDocsTest", "Failed to save bitmap", e);
        }
    }

    private byte[] readRawResource(Context context, int resId) {
        try (InputStream inputStream = context.getResources().openRawResource(resId)) {
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            int bytesRead = 0;
            while (bytesRead < size) {
                int read = inputStream.read(buffer, bytesRead, size - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
            return buffer;
        } catch (IOException e) {
            Log.e("RenderRawDocsTest", "Failed to read resource " + resId, e);
            return new byte[0];
        }
    }

    public static class RawResource {
        public final int id;
        public final String name;

        public RawResource(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static List<RawResource> getRcFilesStatic(Context context) {
        ArrayList<RawResource> result = new ArrayList<>();
        try {
            Class<?> rawClass = Class.forName(context.getPackageName() + ".R$raw");
            Field[] fields = rawClass.getFields();
            for (Field field : fields) {
                String name = field.getName();
                int id = field.getInt(null);
                result.add(new RawResource(id, name));
            }
        } catch (Exception e) {
            Log.e("RenderRawDocsTest", "Failed to discover RC files", e);
        }
        return result;
    }
}
