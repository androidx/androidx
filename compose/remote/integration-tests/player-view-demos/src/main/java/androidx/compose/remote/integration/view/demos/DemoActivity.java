/*
 * Copyright (C) 2026 The Android Open Source Project
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

import static android.widget.LinearLayout.VERTICAL;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.FrameMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.compose.remote.core.CoreDocument.ShaderControl;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.integration.view.demos.customviews.AndroidCustomSupport;
import androidx.compose.remote.integration.view.demos.dsl.DslSysVarKt;
import androidx.compose.remote.integration.view.demos.dsl.DslTextDemoKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslClockKt;
import androidx.compose.remote.integration.view.demos.dsl.RcDslTickerKt;
import androidx.compose.remote.integration.view.demos.dsl.games.DslGameFlappyDroidKt;
import androidx.compose.remote.integration.view.demos.dsl.graph2d.demos.Graph2dDistributionDemosKt;
import androidx.compose.remote.integration.view.demos.examples.DemoGraphsKt;
import androidx.compose.remote.integration.view.demos.providers.Demo1;
import androidx.compose.remote.integration.view.demos.providers.Demo2;
import androidx.compose.remote.integration.view.demos.providers.Demo3;
import androidx.compose.remote.integration.view.demos.providers.Demo4;
import androidx.compose.remote.integration.view.demos.providers.Demo5;
import androidx.compose.remote.integration.view.demos.utils.RCDoc;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressLint("RestrictedApiAndroidX")
public class DemoActivity extends Activity {
    static RemoteComposeBuffer sCurrentBuffer = new RemoteComposeBuffer();
    private FrameMetrics mMetrics;
    static final String CHANNEL_ID = "custom_notification_channel";
    static int sNotificationId = 1;
    public static final boolean BACKGROUND = false;
    private static final boolean NO_COMPOSE = false;

    public static @NonNull RemoteComposeBuffer getCurrentDoc() {
        return sCurrentBuffer;
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        paletteChange();
    }

    /**
     * Get the list of demos
     *
     * @return The list of demos
     */
    public static @NonNull List<RCDoc> getDocs() {
        ArrayList<RCDoc> list = new ArrayList<>();
        // 5 Launcher widgets docs (indices 0 to 4) -> Demo1 to Demo5
        list.add(get("Demo1: Ticker", RcDslTickerKt::dslTicker));
        list.add(getp("Demo2: clock", DemoGraphsKt::demoGraphs2));
//        list.add(get("Demo2: Stopwatch",  DslStopwatchKt::dslStopwatchDemo));
        list.add(get("Demo3: Clock Widget", RcDslClockKt::dslClock));
        list.add(get("Demo4: graph2d", Graph2dDistributionDemosKt::graph2dViolin));
        list.add(get("Demo5: FlappyDroid", DslGameFlappyDroidKt::dslGameFlappyDroid));
        // 6th remote compose document displayed in the application (index 5)
        list.add(get("In-App Display Doc (6th)", DslTextDemoKt::dslRcTextDemo));
        // 7th document for notification (index 6)
        list.add(get("Notification Doc (7th)", DslSysVarKt::dslSysVar));
//        list.add(getpc("1/011/particleSphere", ParticleSphereKt::particleSphere));
//        list.add(getp("1/16/DemoGraphs2_og", DemoGraphsKt::demoGraphs2));
//list.add(get("1/44/Clock", RcDslClockKt::dslClock));
        return list;
    }

    /**
     * Get the document bytes
     *
     * @param index the index of the document
     * @return the document bytes
     */
    public static byte @NonNull [] getDocBytes(int index) {
        List<RCDoc> docs = getDocs();
        if (index >= 0 && index < docs.size()) {
            RCDoc doc = docs.get(index);
            return docToBytes(doc);
        }
        return new byte[0];
    }

    Card mCard;
    static int sScrWidth = 1080;
    static int sWidth = 1080;
    static int sHeight = 1080;
    TextView mStatsView;
    private static final boolean DEBUG = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mUpdateTask;
    private RCDoc mDisplayedDoc;
    private int mNextPinIndex = 0;
    private Button mPinButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());

        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(VERTICAL);

        LinearLayout docControl = new LinearLayout(this);
        docControl.setOrientation(LinearLayout.HORIZONTAL);
        mStatsView = new TextView(this);
        mStatsView.setTextSize(24);
        mStatsView.setPadding(20, 20, 20, 20);
        mStatsView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        docControl.addView(mStatsView);
        layout.addView(docControl);

        mCard = new Card(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        layout.addView(mCard, cardParams);

        LinearLayout row = new LinearLayout(this);
        float textSize = 15;

        mPinButton = new Button(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        mPinButton.setLayoutParams(p);
        mPinButton.setTextSize(textSize);
        mPinButton.setText("Pin\nDemo1");
        mPinButton.setOnClickListener(this::pinNextWidget);

        Button toLWidget = new Button(this);
        toLWidget.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toLWidget.setLayoutParams(p);
        toLWidget.setText("5 Widgets");
        toLWidget.setOnClickListener(this::toWidget);
        row.addView(mPinButton);
        row.addView(toLWidget);

        Button toNotification = new Button(this);
        toNotification.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toNotification.setTextColor(Color.RED);
        toNotification.setLayoutParams(p);
        toNotification.setText("Notify 7th");
        toNotification.setOnClickListener(this::sendToNotify);
        row.addView(toNotification);
        createNotificationChannel();

        Button toPlayer = new Button(this);
        toPlayer.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toPlayer.setTextColor(Color.RED);
        toPlayer.setLayoutParams(p);
        toPlayer.setText("Player");
        toPlayer.setOnClickListener(this::setToPlayer);
        row.addView(toPlayer);

        Button saveAll = new Button(this);
        saveAll.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        saveAll.setLayoutParams(p);
        saveAll.setText("Save\nAll");
        saveAll.setOnClickListener(this::saveAll);
        row.addView(saveAll);

        layout.addView(row);
        layout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                Rect r = new Rect();
                v.getWindowVisibleDisplayFrame(r);
                sHeight = r.height();
                sWidth = r.width();
                sScrWidth = Math.min(r.width(), r.height());
                setUpMetrics();
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
            }
        });

        setContentView(layout);

        // Display the single 6th document in the application without any RecyclerView pattern
        startPlayingDisplayedDoc();
    }

    private void startPlayingDisplayedDoc() {
        List<RCDoc> docs = getDocs();
        if (docs.size() > 5) {
            mDisplayedDoc = docs.get(5); // The 6th document
        } else if (!docs.isEmpty()) {
            mDisplayedDoc = docs.get(0);
        }
        if (mDisplayedDoc == null) return;

        long startTime = System.nanoTime();
        mCard.mTitle.setText(mDisplayedDoc.toString());

        byte[] byteData = docToBytes(mDisplayedDoc);
        if (byteData != null && byteData.length > 0) {
            RemoteDocument rDoc = mDisplayedDoc.getDoc();
            if (rDoc != null) {
                sCurrentBuffer = rDoc.getDocument().getBuffer();
            }
            mCard.mPlayer.setDocument(byteData);
        }

        if (mUpdateTask != null) {
            mHandler.removeCallbacks(mUpdateTask);
        }
        mUpdateTask = new Runnable() {
            @Override
            public void run() {
                if (mCard != null && mCard.mPlayer != null) {
                    int ops = mCard.mPlayer.getOpsPerFrame();
                    float evalTime = mCard.mPlayer.getEvalTime();
                    float diffMs = (System.nanoTime() - startTime) * 1E-6f;
                    DecimalFormat df1 = new DecimalFormat("#,###.##");
                    DecimalFormat df2 = new DecimalFormat("#,###");
                    String stats = "  Load: " + df1.format(diffMs) + " ms\n   cmd: " + ops
                            + "\n Frame: " + df2.format(evalTime) + " ms";
                    mCard.mStats.setText(stats);
                }
                mHandler.postDelayed(this, 300);
            }
        };
        mHandler.postDelayed(mUpdateTask, 300);
    }

    void setUpMetrics() {
        Handler handler = new Handler();
        getWindow().addOnFrameMetricsAvailableListener(
                new Window.OnFrameMetricsAvailableListener() {
                    @Override
                    public void onFrameMetricsAvailable(Window window, FrameMetrics frameMetrics,
                            int dropCountSinceLastInvocation) {
                        mMetrics = new FrameMetrics(frameMetrics);
                    }
                }, handler);
        handler.postDelayed(this::printMetrics, 2000);
    }

    float mRenderingDuration = 0;

    /**
     * Print the metrics
     */
    public void printMetrics() {
        FrameMetrics metrics = mMetrics;
        if (metrics == null) {
            return;
        }
        if (DEBUG) {
            Log.d("Metrics", "ANIMATION_DURATION: "
                    + metrics.getMetric(FrameMetrics.ANIMATION_DURATION) * 1E-6f);
            Log.d("Metrics", "COMMAND_ISSUE_DURATION: "
                    + metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) * 1E-6f);
            Log.d("Metrics",
                    "DRAW_DURATION: " + metrics.getMetric(FrameMetrics.DRAW_DURATION) * 1E-6f);
            Log.d("Metrics", "FIRST_DRAW_FRAME: "
                    + metrics.getMetric(FrameMetrics.FIRST_DRAW_FRAME) * 1E-6f);
            Log.d("Metrics", "INPUT_HANDLING_DURATION: "
                    + metrics.getMetric(FrameMetrics.INPUT_HANDLING_DURATION) * 1E-6f);
            Log.d("Metrics", "LAYOUT_MEASURE_DURATION: "
                    + metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) * 1E-6f);
            Log.d("Metrics", "SWAP_BUFFERS_DURATION: "
                    + metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION) * 1E-6f);
            Log.d("Metrics",
                    "SYNC_DURATION: " + metrics.getMetric(FrameMetrics.SYNC_DURATION) * 1E-6f);
            Log.d("Metrics", "TOTAL_DURATION: " + (mRenderingDuration = metrics.getMetric(
                    FrameMetrics.TOTAL_DURATION) * 1E-6f));
            Log.d("Metrics", "UNKNOWN_DELAY_DURATION: "
                    + metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) * 1E-6f);
        } else {
            mRenderingDuration = metrics.getMetric(FrameMetrics.TOTAL_DURATION) * 1E-6f;
        }
        DecimalFormat df = new DecimalFormat("#.##");
        if (mStatsView != null) {
            mStatsView.setText("render: " + df.format(mRenderingDuration) + "ms");
        }
        setUpMetrics();
    }

    private void saveAll(View v) {
        File storageDir = new File("/storage/self/primary/Download/");
        File[] toRemove = storageDir.listFiles();
        if (toRemove != null) {
            for (int i = 0; i < toRemove.length; i++) {
                File file = toRemove[i];
                if (file.getName().endsWith(".rc")) {
                    boolean delete = file.delete();
                    if (delete) {
                        System.out.println("Deleted " + file.getName());
                    }
                }
            }
        }

        for (RCDoc doc : getDocs()) {
            if (NO_COMPOSE && doc.toString().startsWith("Compose")) {
                continue;
            }
            saveDoc(doc.toString(), docToBytes(doc), getApplicationContext(), false);
        }
        Toast.makeText(this, "Saved all documents!", Toast.LENGTH_SHORT).show();
    }

    private void sendDoc(RCDoc doc) {
        sCurrentBuffer = Objects.requireNonNull(doc.getDoc()).getDocument().getBuffer();
        byte[] buffer = Arrays.copyOf(sCurrentBuffer.getBuffer().getBuffer(),
                sCurrentBuffer.getBuffer().getSize());
        int bufferSize = buffer.length;
        byte[] bytes = new byte[bufferSize];
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0,
                bufferSize)) {
            int read = byteArrayInputStream.read(bytes);
            Log.v("MAIN", "save   " + read + " bytes");
            saveDoc(doc.toString(), bytes, getApplicationContext(), false);
        } catch (IOException e) {
            Log.e("MAIN", "Error reading bytes");
        }
    }

    // ========================= Notification (7th document) ====================================
    private void toNotify() {
        Log.v("MAIN", "toNotify 7th document");
        List<RCDoc> docs = getDocs();
        if (docs.size() <= 6) return;
        RCDoc doc = docs.get(6); // 7th document (index 6)

        sCurrentBuffer = Objects.requireNonNull(doc.getDoc()).getDocument().getBuffer();
        byte[] buffer = Arrays.copyOf(sCurrentBuffer.getBuffer().getBuffer(),
                sCurrentBuffer.getBuffer().getSize());
        int bufferSize = buffer.length;
        byte[] bytes = new byte[bufferSize];
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0,
                bufferSize)) {
            int read = byteArrayInputStream.read(bytes);
            System.out.println("read " + read);
            saveDoc(doc.toString(), bytes, getApplicationContext(), false);
        } catch (IOException e) {
            Log.e("MAIN", "Error reading bytes");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions drawInstruction = new RemoteViews.DrawInstructions.Builder(
                    List.of(bytes)).build();
            RemoteViews remoteViews = new RemoteViews(drawInstruction);
            Log.v("MAIN", "created RemoteViews for 7th doc");
            Intent intent = new Intent(this, this.getClass());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    CHANNEL_ID).setSmallIcon(R.drawable.ic_launcher_background)
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
            Toast.makeText(this,
                    "Sent notification of 7th document!", Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("VERSION.SDK_INT < VANILLA_ICE_CREAM");
        }
    }

    private void createNotificationChannel() {
        CharSequence name = "Custom Notifications";
        String description = "Channel for custom notifications";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION",
                        "Notification permission already granted.");
                Toast.makeText(this,
                        "Permission is already granted.", Toast.LENGTH_SHORT).show();
            } else {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NonNull [] permissions,
            int @NonNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Notifications permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Notifications permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setToPlayer(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                toPlayer();
            } else {
                System.out.println("requesting permission");
                askNotificationPermission();
            }
        } else {
            toPlayer();
        }
    }

    void toPlayer() {
        if (mDisplayedDoc == null) return;
        sendDoc(mDisplayedDoc);
        if (DEBUG) {
            sendToPlayerViaIntent(docToBytes(mDisplayedDoc), mDisplayedDoc.toString());
        } else {
            Intent intent = new Intent(this, DocPlayerActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra("RC_DOC_NAME", mDisplayedDoc.toString());
            intent.putExtra("RC_DOC_DATA", docToBytes(mDisplayedDoc));
            intent.setType("application/remote-compose-doc");
            startActivity(intent);
        }
    }

    void sendToPlayerViaIntent(byte[] data, String name) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra("RC_DOC_NAME", name);
        intent.putExtra("RC_DOC_DATA", data);
        intent.setType("application/remote-compose-doc");
        startActivity(Intent.createChooser(intent, "Open with…"));
    }

    static byte[] docToBytes(RCDoc doc) {
        if (doc == null) return new byte[0];
        RemoteDocument rcdoc = doc.getDoc();
        if (rcdoc == null) {
            return new byte[0];
        }
        sCurrentBuffer = rcdoc.getDocument().getBuffer();

        byte[] buffer = Arrays.copyOf(sCurrentBuffer.getBuffer().getBuffer(),
                sCurrentBuffer.getBuffer().getSize());
        int bufferSize = buffer.length;
        byte[] bytes = new byte[bufferSize];
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer, 0,
                bufferSize)) {
            int read = byteArrayInputStream.read(bytes);
            Log.v("MAIN", "read " + read);
        } catch (IOException e) {
            Log.d("MAIN", "Unable to read doc ");
        }
        return bytes;
    }

    private void sendToNotify(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                toNotify();
            } else {
                System.out.println("requesting permission");
                askNotificationPermission();
            }
        } else {
            toNotify();
        }
    }

    // ========================= 5 Launcher Widgets (Demo1 - Demo5)

    /**
     * Update the widget
     *
     * @param context          the context
     * @param appWidgetManager the app widget manager
     * @param appWidgetIds     the app widget ids
     * @param docIndex         the doc index
     * @param widgetClass      the widget class
     */
    public static void updateWidget(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int @Nullable [] appWidgetIds, int docIndex, @NonNull Class<?> widgetClass) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            byte[] bytes = getDocBytes(docIndex);
            if (bytes != null && bytes.length > 0) {
                RemoteViews.DrawInstructions drawInstruction =
                        new RemoteViews.DrawInstructions.Builder(List.of(bytes)).build();
                RemoteViews remoteViews = new RemoteViews(drawInstruction);
                Intent intent = new Intent(context, DemoActivity.class);
                intent.setAction("com.example.ACTION_VIEW_CLICKED");
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                        PendingIntent.FLAG_MUTABLE);
                int id = 567;
                remoteViews.setOnClickPendingIntent(id, pendingIntent);

                if (appWidgetIds != null && appWidgetIds.length > 0) {
                    appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
                } else {
                    appWidgetManager.updateAppWidget(new ComponentName(context, widgetClass),
                            remoteViews);
                }
                System.out.println("Updated launcher widget " + widgetClass.getSimpleName()
                        + " with doc index " + docIndex);
            }
        } else {
            System.out.println("VERSION.SDK_INT < VANILLA_ICE_CREAM");
        }
    }

    private void toWidget(View v) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        Class<?>[] widgetClasses = new Class<?>[]{
                Demo1.class, Demo2.class, Demo3.class, Demo4.class, Demo5.class
        };

        for (int i = 0; i < 5; i++) {
            updateWidget(this, appWidgetManager, null, i, widgetClasses[i]);
            saveDoc("Demo" + (i + 1), getDocBytes(i), getApplicationContext(), false);
        }
        Toast.makeText(this, "Updated Demo1-Demo5 Launcher Widgets simultaneously!",
                Toast.LENGTH_SHORT).show();
    }

    private void pinNextWidget(View v) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        Class<?>[] widgetClasses = new Class<?>[]{
                Demo1.class, Demo2.class, Demo3.class, Demo4.class, Demo5.class
        };

        int idx = mNextPinIndex % 5;
        Class<?> targetClass = widgetClasses[idx];

        updateWidget(this, appWidgetManager, null, idx, targetClass);
        saveDoc("Demo" + (idx + 1), getDocBytes(idx), getApplicationContext(), false);

        if (appWidgetManager.isRequestPinAppWidgetSupported()) {
            ComponentName componentName = new ComponentName(this, targetClass);
            appWidgetManager.requestPinAppWidget(componentName, null, null);
            Toast.makeText(this, "Requesting to pin Demo" + (idx + 1) + " widget!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "Pinning widgets is not supported on this launcher/device.",
                    Toast.LENGTH_SHORT).show();
        }

        mNextPinIndex = (idx + 1) % 5;
        if (mPinButton != null) {
            mPinButton.setText("Pin\nDemo" + (mNextPinIndex + 1));
        }
    }

    /**
     * Palette change
     */
    public void paletteChange() {
        if (mCard != null && mCard.mPlayer != null) {
            mCard.mPlayer.reloadPalette();
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    static class Card extends LinearLayout {
        RemoteComposePlayer mPlayer;
        TextView mTitle;
        TextView mStats;

        @SuppressLint("RestrictedApiAndroidX")
        Card(Context context) {
            super(context);
            setOrientation(VERTICAL);
            mPlayer = new RemoteComposePlayer(context);
            mPlayer.setCustomSupport(new AndroidCustomSupport(mPlayer));
            mTitle = new TextView(context);
            mStats = new TextView(context);
            mPlayer.setShaderControl(new ShaderControl() {
                @Override
                public boolean isShaderValid(@NonNull String shader) {
                    return true;
                }
            });
            mTitle.setTextSize(24);
            mTitle.setTypeface(mTitle.getTypeface(), Typeface.BOLD);
            mTitle.setTextColor(Color.BLACK);
            mTitle.setBackgroundColor(0xFFDDDDDD);
            mStats.setTextSize(24);
            mStats.setBackgroundColor(0xFFDDDDDD);
            mStats.setTypeface(Typeface.MONOSPACE);
            mTitle.setLines(1);
            mStats.setLines(4);
            setBackgroundColor(0xFF444444);
            int size = sScrWidth - 20;
            if (sHeight > sWidth) {
                Log.v("MAIN", "portrait mode");
            } else { // landscape
                Log.v("MAIN", "landscape mode");
                size = sHeight / 2;
            }
            if (size < 200) {
                size = 1200;
            }
            LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            addView(mPlayer, params);
            addView(mTitle);
            addView(mStats);

            setPadding(10, 10, 10, 10);
        }
    }

    static Drawable sDrawable = new Drawable() {
        @Override
        public void draw(@NonNull Canvas canvas) {
            int pixelSize = 20;
            Bitmap bitmap = Bitmap.createBitmap(pixelSize * 2, pixelSize * 2,
                    Bitmap.Config.ARGB_8888);
            int[] color = new int[pixelSize * pixelSize];
            for (int i = 0; i < color.length; i++) {
                color[i] = 0xFFAAAAAA;
            }
            bitmap.eraseColor(0xFFBBBBBB);
            bitmap.setPixels(color, 0, pixelSize, 0, 0, pixelSize, pixelSize);
            bitmap.setPixels(color, 0, pixelSize, pixelSize, pixelSize, pixelSize, pixelSize);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new BitmapShader(bitmap, BitmapShader.TileMode.REPEAT,
                    BitmapShader.TileMode.REPEAT));
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        }

        /**
         * set the alpha
         * @param alpha the alpha to set to
         */
        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        /**
         * get the opacity
         * @return the opacity
         */
        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    };

    /**
     * get the doc
     * @param name        the name of the doc
     * @param gen         the doc supplier
     * @return the doc
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RCDoc getpc(@NonNull String name,
            @NonNull Supplier<RemoteComposeContext> gen) {
        return getp(name, () -> gen.get().mRemoteWriter);
    }

    /**
     * get the doc
     *
     * @param name        the name of the doc
     * @param docSupplier the doc supplier
     * @return the doc
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RCDoc get(@NonNull String name,
            @NonNull Supplier<byte @NonNull []> docSupplier) {
        return new RCDoc() {
            @NonNull
            final Supplier<byte @NonNull []> mSupplier = docSupplier;
            byte @Nullable [] mDoc;
            float mBuildTime = 0;

            @Override
            public float getBuildTime() {
                return mBuildTime;
            }

            public byte @NonNull [] doc() {
                if (mDoc == null) {
                    mDoc = mSupplier.get();
                }
                return mDoc;
            }

            @Override
            public int getColor() {
                return 0;
            }

            @Override
            public void run() {
            }

            @Override
            public int size() {
                return doc().length;
            }

            @Override
            public int zipSize() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(baos);
                    deflaterOutputStream.write(doc(), 0, doc().length);
                    deflaterOutputStream.finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return baos.size();
            }

            @Override
            @NonNull
            public String toString() {
                return name;
            }

            /**
             * get the doc
             * @return the doc
             */
            @Override
            public RemoteDocument getDoc() {
                Log.v("perf", "build doc \"" + name + "\"");
                long time = System.nanoTime();
                RemoteDocument ret = new RemoteDocument(
                        new ByteArrayInputStream(doc(), 0, doc().length));
                mBuildTime = (System.nanoTime() - time) * 1E-6f;
                return ret;
            }
        };
    }

    /**
     * get the doc
     *
     * @param name           the name of the doc
     * @param writerSupplier the writer supplier
     * @return the doc
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RCDoc getp(@NonNull String name,
            @NonNull Supplier<RemoteComposeWriter> writerSupplier) {
        return new RCDoc() {
            private RemoteComposeWriter mWriter;
            float mBuildTime = 0;

            @Override
            public float getBuildTime() {
                return mBuildTime;
            }

            public RemoteComposeWriter writer() {
                if (mWriter == null) {
                    mWriter = writerSupplier.get();
                }
                return mWriter;
            }

            @Override
            public int getColor() {
                return 0;
            }

            @Override
            public void run() {
            }

            @Override
            public int size() {
                return writer().bufferSize();
            }

            @Override
            public int zipSize() {
                RemoteComposeWriter writer = writer();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(baos);
                    deflaterOutputStream.write(writer.buffer(), 0, writer.bufferSize());
                    deflaterOutputStream.finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return baos.size();
            }

            @Override
            @NonNull
            public String toString() {
                return name;
            }

            /**
             * get the doc
             * @return the doc
             */
            @Override
            public RemoteDocument getDoc() {
                Log.v("perf", "build doc \"" + name + "\"");
                long time = System.nanoTime();
                mWriter = writer();
                RemoteDocument ret = new RemoteDocument(
                        new ByteArrayInputStream(writer().buffer(), 0, writer().bufferSize()));
                mBuildTime = (System.nanoTime() - time) * 1E-6f;
                return ret;
            }
        };
    }

    /**
     * get the doc
     *
     * @param name            the name of the doc
     * @param writerSupplier  the writer supplier
     * @param writerSupplier2 the writer supplier
     * @return the doc
     */
    public static @NonNull RCDoc getp(@NonNull String name,
            @NonNull Supplier<@Nullable RemoteComposeWriter> writerSupplier,
            @NonNull Supplier<@Nullable RemoteComposeWriter> writerSupplier2) {
        return new RCDoc() {
            private RemoteComposeWriter mWriter;
            private RemoteComposeWriter mWriter2;
            float mBuildTime = 0;

            @Override
            public float getBuildTime() {
                return mBuildTime;
            }

            public RemoteComposeWriter writer() {
                if (mWriter == null) {
                    mWriter = writerSupplier.get();
                }
                if (mWriter2 == null) {
                    mWriter2 = writerSupplier2.get();
                }
                return mWriter;
            }

            private RemoteComposeWriter writer2() {
                mWriter2 = writerSupplier2.get();
                return mWriter2;
            }

            @Override
            public int getColor() {
                return 0;
            }

            @Override
            public void run() {
            }

            @Override
            public int size() {
                return writer().bufferSize();
            }

            @Override
            public int zipSize() {
                RemoteComposeWriter writer = writer();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(baos);
                    deflaterOutputStream.write(writer.buffer(), 0, writer.bufferSize());
                    deflaterOutputStream.finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return baos.size();
            }

            /**
             * get the name of the doc
             * @return the name
             */
            @Override
            @NonNull
            public String toString() {
                return name;
            }

            /**
             * get the looper
             * @return the looper
             */
            @Override
            public RemoteDocument getLooper() {
                RemoteComposeWriter w = writer2();
                if (w == null) return null;
                return new RemoteDocument(
                        new ByteArrayInputStream(w.buffer(), 0, w.bufferSize()));
            }

            /**
             * get the doc
             * @return the doc
             */
            @Override
            public RemoteDocument getDoc() {
                Log.v("perf", "build doc \"" + name + "\"");
                long time = System.nanoTime();
                RemoteDocument ret = new RemoteDocument(
                        new ByteArrayInputStream(writer().buffer(), 0, writer().bufferSize()));
                mBuildTime = (System.nanoTime() - time) * 1E-6f;
                return ret;
            }
        };
    }

    /**
     * save the doc
     *
     * @param name       the name of the doc
     * @param buff       the doc
     * @param appContext the app context
     * @param compress   whether to compress the doc
     */
    public static void saveDoc(
            @NonNull String name,
            byte @Nullable [] buff,
            @NonNull Context appContext,
            boolean compress) {
        if (buff == null) {
            return;
        }
        int len = buff.length;
        if (name.indexOf('/') >= 0) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        name = name.replaceFirst("^\\d+", "");
        name = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase().replaceAll("\\s+", "_");
        File storageDir = appContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null) {
            storageDir = new File("/storage/self/primary/Download/");
        }
        if (storageDir == null) {
            return;
        }
        if (!storageDir.exists()) {
            boolean mkdirs = storageDir.mkdirs();
            if (!mkdirs) {
                return;
            }
        }
        System.out.println("saving " + name);
        if (DEBUG) {
            String str = Base64.getEncoder().encodeToString(buff);
            StringBuilder output = new StringBuilder("\ndata = ");
            int chunk = 72;
            for (int i = 0; i < str.length(); i += chunk) {
                int endIndex = Math.min(i + chunk, str.length());
                if (i != 0) output.append("+");
                output.append("\"").append(str.substring(i, endIndex)).append("\"\n");
            }
            Log.v("MAIN", "base64String: " + output);
        }

        File imageFile = new File(storageDir, name + ".rc");
        if (imageFile.exists()) {
            if (buff.length != imageFile.length()) {
                imageFile = new File(storageDir, name + "_c.rc");
            }
        }
        if (compress) {
            imageFile = new File(storageDir, name + ".rcz");
        }

        if (compress) {
            try (
                    FileOutputStream fos = new FileOutputStream(imageFile);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    ZipOutputStream zos = new ZipOutputStream(bos)) {
                zos.setLevel(Deflater.DEFAULT_COMPRESSION);
                ZipEntry entry = new ZipEntry("data.rcz");
                zos.putNextEntry(entry);
                zos.write(buff);
                zos.closeEntry();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(buff, 0, len);
                System.out.println("writing " + len + " bytes");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("done.");
    }

    interface RunThenUI {
        void run();

        void runOnUI();
    }
    /*
    private static void runOnThread(final Handler handler, RunThenUI runnable) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        runnable.runOnUI();
                    }
                });
            }
        }).start();
    }

    private static byte[] addUpdateData(byte[] rcData, String update) {
        byte[] strData = update.getBytes(StandardCharsets.UTF_8);
        int len = strData.length;
        byte[] ret = new byte[rcData.length + 5 + len];
        int off = rcData.length;
        ret[off++] = (byte) 195;
        ret[off++] = (byte) ((len >> 24) & 0xFF);
        ret[off++] = (byte) ((len >> 16) & 0xFF);
        ret[off++] = (byte) ((len >> 8) & 0xFF);
        ret[off++] = (byte) (len & 0xFF);

        System.arraycopy(rcData, 0, ret, 0, rcData.length);
        System.arraycopy(strData, 0, ret, rcData.length + 5, strData.length);
        return ret;
    }
     */
}
