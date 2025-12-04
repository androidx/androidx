/*
 * Copyright 2025 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.player.view.RemoteComposePlayer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@SuppressLint("RestrictedApiAndroidX")
public class DocPlayerActivity extends Activity {
    static final boolean USE_EMBEDDED_PLAYER = false;
    static byte[] sDoc = new byte[0];
    public static @Nullable String sUUDoc = // a simple clock document
            "AAAAAAEAAAAAAAAAAAAAA+gAAAPoAAAAAAAAAADI/////oKCgoLK/////f////8AAAACAAAA"
                    + "AhAAAAABP4AAAEMAAAABP4AAADZC5wAAQucAAELnAABC5wAAyf////yCzf////v/////EAAA"
                    + "AAE/gAAAQwAAAAE/gAAANwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAD+AAAA/gAAAAAAAAMn/"
                    + "///6z/////mClgAAAAD////5AAAAKpYAAAAB////+QAAACuCUQAAACwAAAAD/4AABEHwAAD/"
                    + "sQADUQAAAC0AAAAD/4AAKkAAAAD/sQAEUQAAAC4AAAAD/4AAK0AAAAD/sQAEgf+AACz/gAAt"
                    + "/4AALlEAAAAvAAAADf+AACtAAAAA/7EABP+AACpAAAAA/7EABP+AACtAAAAA/7EABP+xAAZA"
                    + "AAAA/7EABP+xAAIoAAAADgAAAAT/zMzMAAAABUKAAAAAAAABQUAAAAABAAcAAAAPAAEACAAA"
                    + "ABAAAAAAAAMAEgAAAAkAAAAAL/+AAC3/gAAu/4AALf+AAC+DglEAAAAwAAAAA/+AAANAwAAA"
                    + "/7EAA4H/gAAw/4AALf+AAC5RAAAAMQAAAA3/gAArQAAAAP+xAAT/gAAqQAAAAP+xAAT/gAAr"
                    + "QAAAAP+xAAT/sQAGP0zMzf+xAAP/sQACKAAAAAQAAAAE/4iIiAAAAAVCAAAAL/+AAC3/gAAu"
                    + "/4AALf+AADGDUQAAADIAAAAQ/4AAKkAAAAD/sQAE/4AAKkAAAAD/sQAE/4AAK0AAAAD/sQAE"
                    + "/7EABv+AAAE91ndQ/7EAA/+xABL/sQAD/7EAAVEAAAAzAAAAEP+AACtAAAAA/7EABP+AACpA"
                    + "AAAA/7EABP+AACtAAAAA/7EABP+xAAb/gAABPdZ3UP+xAAP/sQAT/7EAA/+xAAIoAAAABAAA"
                    + "AAT/////AAAABUAAAAAv/4AALf+AAC7/gAAy/4AAM4PW1taD1taDg4OD1g==";
    FrameLayout[] mPreview;
    RcDocs[] mFoundDocs = new RcDocs[1];

    private static byte[] unzipSingleFile(byte[] zipBytes) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("Empty ZIP archive");
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            zis.closeEntry();

            // Optionally check that there's no second entry
            if (zis.getNextEntry() != null) {
                throw new IOException("ZIP contains more than one file");
            }

            return bos.toByteArray();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().hasExtra("RC_DOC_DATA")) {
            sDoc = getIntent().getByteArrayExtra("RC_DOC_DATA");
            boolean zip = getIntent().getBooleanExtra("RC_DOC_ZIP", true);
            try {
                if (zip) {
                    sDoc = unzipSingleFile(sDoc);
                }

            } catch (IOException e) {
                if (sUUDoc.length() > 10000) {
                    readRawResource(this, 0);
                }
                throw new RuntimeException(e);
            }
        } else {
            sDoc = Base64.getDecoder().decode(sUUDoc);
        }

        if (USE_EMBEDDED_PLAYER) {
            saveBinaryData(sDoc);
        }

        mFoundDocs[0] = new RcDocs("built in", () -> sDoc);
        mPreview = new FrameLayout[mFoundDocs.length];
        // list
        ViewGroup scrollView;
        LinearLayout verticalLayout = new LinearLayout(this);
        float[] dim = new float[2];
        getDisplayMetricsDimensions(dim);
        if (dim[0] > dim[1]) {
            scrollView = new HorizontalScrollView(this);

            verticalLayout.setOrientation(LinearLayout.HORIZONTAL);
            verticalLayout.setGravity(Gravity.CENTER_VERTICAL);
        } else {
            scrollView = new ScrollView(this);
            verticalLayout.setOrientation(LinearLayout.VERTICAL);
            verticalLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        }
        FrameLayout tmp = new FrameLayout(this);
        tmp.setLayoutParams(new ViewGroup.LayoutParams(100, 100));
        verticalLayout.addView(tmp);
        for (int i = 0; i < mFoundDocs.length; i++) {

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.VERTICAL);
            rowLayout.setGravity(Gravity.CENTER_HORIZONTAL);

            Button commandButton = new Button(this);
            commandButton.setText("INSTALL\n" + mFoundDocs[i].mName);
            commandButton.setAllCaps(false);
            FrameLayout rcView = new FrameLayout(this);
            mPreview[i] = rcView;
            rcView.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) (dim[1] - 340)));
            int finalI = i;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                commandButton.setOnClickListener((a) -> click(finalI));
            }
            rowLayout.addView(rcView);
            rowLayout.addView(commandButton);
            if (dim[0] > dim[1]) {
                rowLayout.setPadding(10, 0, 0, 0);
            } else {
                rowLayout.setPadding(0, 30, 0, 0);
            }
            verticalLayout.addView(rowLayout);
        }
        scrollView.addView(verticalLayout);

        setContentView(scrollView);
    }

    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private void click(int i) {
        sDoc = mFoundDocs[i].getDoc();
        Log.v("MAIN", ">>>>>> loading " + mFoundDocs[i].mName);
        RemoteViews.DrawInstructions.Builder r =
                new RemoteViews.DrawInstructions.Builder(List.of(sDoc));

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetManager.updateAppWidget(ExperimentWidgetProvider.sAppWidgetIds,
                new RemoteViews(r.build()));

    }

    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (int i = 0; i < mFoundDocs.length; i++) {
            byte[] bytes = mFoundDocs[i].getDoc();
            if (USE_EMBEDDED_PLAYER) {
                RemoteViews.DrawInstructions.Builder r =
                        new RemoteViews.DrawInstructions.Builder(List.of(bytes));

                RemoteViews remoteViews = new RemoteViews(r.build());
                View previewView = remoteViews.apply(this, mPreview[i]);
                mPreview[i].addView(previewView);
            } else {
                RemoteComposePlayer player = new RemoteComposePlayer(this);
                player.setShaderControl((String str) -> {
                    return true;
                });
                player.setDocument(bytes);
                mPreview[i].addView(player);
            }

        }
    }

    private void getDisplayMetricsDimensions(float[] dimensions) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        dimensions[0] = width;
        dimensions[1] = height;

    }

    private static byte @NonNull [] readRawResource(Context context, int resId) {
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
            throw new RuntimeException(e);
        }
    }

    public interface DocProvider {
        /**
         * Get the document
         *
         * @return bytes representing the document
         */
        byte @NonNull [] getDoc();
    }

    public static class RcDocs {
        public final @NonNull String mName;
        @NonNull DocProvider mProvider;

        public RcDocs(@NonNull String name, @NonNull DocProvider provider) {
            mProvider = provider;
            mName = name;
        }

        public byte @NonNull [] getDoc() {
            return mProvider.getDoc();
        }
    }

    private boolean saveBinaryData(byte @NonNull [] data) {
        try {
            // Get app-specific storage directory
            File storageDir = getFilesDir();

            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date());
            String filename = "qr_binary_" + timestamp + ".bin";

            File outputFile = new File(storageDir, filename);

            // Write binary data to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(data);
                fos.flush();
            }

            Log.d("MAIN", "Binary data saved to: " + outputFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Log.e("MAIN", "Error saving binary data", e);
            return false;
        }
    }

}
