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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This supports multiple ways to load & view a remote compose document.
 * call
 * <pre>
 * void sendToPlayerViaIntent(byte[]data, String name) {
 *         Intent intent = new Intent(Intent.ACTION_VIEW);
 *         intent.putExtra("RC_DOC_NAME", name);
 *         intent.putExtra("RC_DOC_DATA", data);
 *         intent.setType("application/remote-compose-doc");
 *         startActivity(Intent.createChooser(intent, "Open with…"));
 *     }
 *     </pre>
 * <h>adb</h>
 * <br>
 * adb shell am start -a android.intent.action.VIEW
 * -d file:///storage/self/primary/Download/colorList.rc
 * -t application/remote-compose-doc
 */


@SuppressLint("RestrictedApiAndroidX")
public class DocPlayerActivity extends Activity {
    static final boolean USE_EMBEDDED_PLAYER = false;
    private static final String CHANNEL_ID = "custom_notification_channel";
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static int sNotificationId = 1;
    static byte[] sDoc = new byte[0];
    static String sName = "internal";
    static String sDocFile = null;
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
        Intent intent = getIntent();
        Uri fileUri = intent.getData();
        if (fileUri != null) {
            sDocFile = fileUri.getPath();
            sName = fileUri.getLastPathSegment();
            sName = sName.substring(sName.lastIndexOf("/") + 1);

        } else {
            sDocFile = getIntent().getStringExtra("RC_File");
            if (sDocFile != null) {
                sName = sDocFile.substring(sDocFile.lastIndexOf("/") + 1);
            }
        }
        if (sDocFile != null) {
            if (fileUri != null) {
                try {
                    sDoc = readBytesFromUri(fileUri);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                sDoc = readFile(sDocFile);
            }
        } else if (intent.hasExtra("RC_DOC_DATA")) {

            sDoc = getIntent().getByteArrayExtra("RC_DOC_DATA");
            boolean zip = getIntent().getBooleanExtra("RC_DOC_ZIP", true);
            try {
                if (zip) {
                    sDoc = unzipSingleFile(sDoc);
                    sName = "zipped intent";
                } else {
                    sName = "intent";
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

        mFoundDocs[0] = new RcDocs(sName, () -> sDoc);
        mPreview = new FrameLayout[mFoundDocs.length];
        // list
        ViewGroup scrollView;
        LinearLayout verticalLayout = new LinearLayout(this);
        float[] dim = new float[2];
        getDisplayMetricsDimensions(dim);
        System.out.println("dim: " + dim[0] + "x" + dim[1]);
        verticalLayout.setLayoutParams(new ViewGroup.LayoutParams((int) dim[0], (int) dim[1]));
        if (dim[0] > dim[1]) {
//            scrollView = new HorizontalScrollView(this);
            scrollView = new LinearLayout(this);

            verticalLayout.setOrientation(LinearLayout.HORIZONTAL);
            verticalLayout.setGravity(Gravity.CENTER_VERTICAL);
        } else {
//            scrollView = new ScrollView(this);
            scrollView = new LinearLayout(this);

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
            LinearLayout control = new LinearLayout(this);
            control.setOrientation(LinearLayout.HORIZONTAL);
            control.setBackgroundColor(Color.LTGRAY);
            control.setGravity(Gravity.CENTER_HORIZONTAL);
            TextView name = new TextView(this);
            name.setText(mFoundDocs[i].mName);

            Button launcherButton = new Button(this);
            launcherButton.setText("Launcher");
            launcherButton.setAllCaps(false);
            FrameLayout rcView = new FrameLayout(this);
            mPreview[i] = rcView;
            rcView.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            (int) (dim[1] - 340)));
            int finalI = i;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                launcherButton.setOnClickListener((a) -> toLaunch(finalI));
            }
            rcView.setPadding(0, 0, 0, 10);
            rowLayout.addView(rcView);
            rowLayout.addView(control);

            Button notifyButton = new Button(this);
            notifyButton.setOnClickListener((a) -> notify(finalI));
            notifyButton.setText("Notify");
            notifyButton.setAllCaps(false);

            Button loadButton = new Button(this);
            loadButton.setOnClickListener((a) -> getFile());
            loadButton.setText("Load...");
            loadButton.setAllCaps(false);

            control.addView(name);
            control.addView(notifyButton);
            control.addView(launcherButton);
            control.addView(loadButton);

            if (dim[0] > dim[1]) {
                rowLayout.setPadding(10, 0, 0, 0);
                rowLayout.setLayoutParams(
                        new ViewGroup.LayoutParams((int) dim[0] - 200, (int) dim[1] - 20));

            } else {
                rowLayout.setPadding(0, 30, 0, 0);
                rowLayout.setLayoutParams(
                        new ViewGroup.LayoutParams((int) dim[0] - 100, (int) dim[1] - 30));

            }

            verticalLayout.addView(rowLayout);

        }
        scrollView.addView(verticalLayout);

        setContentView(scrollView);
    }

    /**
     * call select File intent
     */
    void getFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Allow any file type
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select File"), 1001, null);
    }

    /**
     * Select file callback
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            System.out.println(" >>> " + uri.getPath());
            Intent intent = new Intent(getApplicationContext(), this.getClass());

            intent.setType("application/remote-compose-doc");
            intent.setData(uri);
            startActivity(intent);
        }
    }

    private byte[] readBytesFromUri(@NonNull Uri uri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Failed to open input stream");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        inputStream.close();
        return buffer.toByteArray();
    }

    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private void toLaunch(int i) {
        sDoc = mFoundDocs[i].getDoc();
        Log.v("MAIN", ">>>>>> loading " + mFoundDocs[i].mName);
        RemoteViews.DrawInstructions.Builder r =
                new RemoteViews.DrawInstructions.Builder(List.of(sDoc));

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        appWidgetManager.updateAppWidget(ExperimentWidgetProvider.sAppWidgetIds,
                new RemoteViews(r.build()));

    }

    private void notify(int i) {
        sDoc = mFoundDocs[i].getDoc();
        Log.v("MAIN", ">>>>>> notify " + mFoundDocs[i].mName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                toNotify(mFoundDocs[i]);
            } else {
                System.out.println("requesting permission");
                askNotificationPermission();
                //requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            toNotify(mFoundDocs[i]);
        }
    }


    private void toNotify(DocPlayerActivity.RcDocs doc) {
        byte[] buffer = doc.getDoc();
        byte[] bytes = buffer;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions drawInstruction = new RemoteViews.DrawInstructions.Builder(
                    List.of(bytes)).build();
            RemoteViews remoteViews = new RemoteViews(drawInstruction);
            Log.v("MAIN", "created RemoteViews " + doc.mName);
            Intent intent = new Intent(this, this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    CHANNEL_ID).setSmallIcon(R.drawable.viewer)
                    .setStyle(
                            new NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(
                            remoteViews).setPriority(
                            NotificationCompat.PRIORITY_DEFAULT).setContentIntent(
                            pendingIntent).setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("MAIN", "Permission not granted");
                return;
            }
            notificationManager.notify(sNotificationId++, builder.build());
        } else {
            System.out.println("VERSION.SDK_INT < VANILLA_ICE_CREAM");
        }
    }

    private void askNotificationPermission() {
        // This is only necessary for API level 33+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if permission is already granted
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION", "Notification permission already granted.");
                Toast.makeText(this, "Permission is already granted.", Toast.LENGTH_SHORT).show();
            } else {
                // Request the permission
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
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

    private static byte @NonNull [] readFile(String filePath) {
        if (filePath.endsWith("rcz")) {
            try (InputStream inputStream = new InflaterInputStream(
                    Files.newInputStream(new File(filePath).toPath()))) {
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
        } else {
            try (InputStream inputStream = Files.newInputStream(new File(filePath).toPath())) {
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
