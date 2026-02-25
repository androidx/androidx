/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static androidx.compose.remote.integration.view.demos.DemosComposeKt.getRemoteComposable;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.compose.remote.core.CoreDocument.ShaderControl;
import androidx.compose.remote.core.RemoteComposeBuffer;
import androidx.compose.remote.creation.RemoteComposeContext;
import androidx.compose.remote.creation.RemoteComposeWriter;
import androidx.compose.remote.integration.view.demos.examples.DemoPaths;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressLint("RestrictedApiAndroidX")
public class ExperimentRecyclerActivity extends Activity {
    static Bitmap sPersonImage3;
    static RemoteComposeBuffer sCurrentBuffer = DemoPaths.pathTest().getBuffer();
    private FrameMetrics mMetrics;
    static final String CHANNEL_ID = "custom_notification_channel";
    static int sNotificationId = 1;
    public static final boolean BACKGROUND = false;


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
     * @param context The context
     * @return The list of demos
     */
    public @NonNull List<RCDoc> getDocs(@NonNull Context context) {

        sPersonImage3 = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.mostly_cloudy);
        ArrayList<RCDoc> list = new ArrayList<>(DemosCreation.getDemos(this));
        list.addAll(getRemoteComposable(context));

        return list;
    }

    RecyclerView mRecyclerView;
    LinearLayoutManager mLinearLayoutManager;
    static int sScrWidth = 1080;
    static int sWidth = 1080;
    static int sHeight = 1080;
    List<RCDoc> mDocList;
    TextView mTextView;
    TextView mStatsView;
    private static final boolean DEBUG = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());

        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
        // Optionally, hide the navigation bar as well for a fully immersive experience
        // windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars());
        // Make sure the system bars are re-hidden when the user interacts with the UI
        windowInsetsController.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // Initialize RecyclerView
        mRecyclerView = new RecyclerView(this);
        mLinearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        // Sample data
        mDocList = getDocs(getBaseContext());
        mDocList.sort(new Comparator<RCDoc>() {
            @Override
            public int compare(RCDoc d1, RCDoc d2) {
                return d1.toString().compareTo(d2.toString());
            }
        });
        sCurrentBuffer = Objects.requireNonNull(mDocList.get(0).getDoc()).getDocument().getBuffer();

        // Set adapter to RecyclerView
        mRecyclerView.setAdapter(new MyAdapter(mDocList));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(VERTICAL);
        layout.addView(mRecyclerView);
        //        Switch lock = new Switch(this);
        LinearLayout docControl = new LinearLayout(this);
        docControl.setOrientation(LinearLayout.HORIZONTAL);
        ToggleButton lock = new ToggleButton(this);
        //        lock.setSwitchMinWidth(200);
        lock.setOnCheckedChangeListener(this::lock);
        lock.setText("lock Off");
        lock.setTextOff("lock Off");
        lock.setTextOn("lock On");
        lock.setTextSize(24);
        mStatsView = new TextView(this);
        mStatsView.setTextSize(24);
        mStatsView.setPadding(20, 20, 20, 20);
        mStatsView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mTextView = new TextView(this);
        mTextView.setTextSize(24);
        mTextView.setText("0");
        mTextView.setPadding(20, 20, 20, 20);
        mTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                int off = mLinearLayoutManager.findFirstVisibleItemPosition();

                int left = Objects.requireNonNull(
                        mLinearLayoutManager.findViewByPosition(off)).getLeft();
                if (left < -sScrWidth / 2) off++;
                mTextView.setText("" + off);
            }
        });
        docControl.addView(mTextView);
        lock.setBackgroundColor(0xFFAAFFAA);
        docControl.addView(lock);
        docControl.addView(mStatsView);

        layout.addView(docControl);
        LinearLayout row = new LinearLayout(this);
        float textSize = 15;
        // ==================== ExperimentActivity  launch ==========================

        Button launch = new Button(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;

        launch.setLayoutParams(p);
        launch.setTextSize(textSize);

        launch.setText("Menu");
        launch.setOnClickListener(this::launch);

        // ==================== WIDGET ==========================
        Button toLWidget = new Button(this);
        toLWidget.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toLWidget.setLayoutParams(p);
        toLWidget.setText("Widget");
        toLWidget.setOnClickListener(this::toWidget);
        row.addView(launch);
        row.addView(toLWidget);

        // ==================== NOTIFICATION ==========================
        Button toNotification = new Button(this);
        toNotification.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toNotification.setTextColor(Color.RED);
        toNotification.setLayoutParams(p);
        toNotification.setText("Notify");
        toNotification.setOnClickListener(this::sendToNotify);
        row.addView(toNotification);
        createNotificationChannel();
        // ==================== To Player ==========================
        Button toPlayer = new Button(this);
        toPlayer.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        toPlayer.setTextColor(Color.RED);
        toPlayer.setLayoutParams(p);
        toPlayer.setText("Player");
        toPlayer.setOnClickListener(this::setToPlayer);
        row.addView(toPlayer);
        // ==================== Save All ==========================
        Button saveAll = new Button(this);
        saveAll.setTextSize(textSize);
        p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        p.weight = 1;
        saveAll.setLayoutParams(p);
        saveAll.setText("SaveAll");
        saveAll.setOnClickListener(this::saveAll);
        row.addView(saveAll);
        createNotificationChannel();
        // ================================ ==========================

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

        // Set RecyclerView as the content view
        setContentView(layout);
    }

    void setUpMetrics() {
        Handler handler = new Handler();
        // Log.d("Metrics", "setup");
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
        mStatsView.setText("render: " + df.format(mRenderingDuration) + "ms");
        setUpMetrics();
    }

    private void saveAll(View v) {
        File storageDir = new File("/storage/self/primary/Download/");
        File[] toRemove = storageDir.listFiles();
        for (int i = 0; i < toRemove.length; i++) {
            File file = toRemove[i];
            if (file.getName().endsWith(".rc")) {
                file.delete();
            }
        }

        for (RCDoc doc : mDocList) {
            saveDoc(doc.toString(), docToBytes(doc), getApplicationContext(), false);
        }
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

    // ========================= Notification ====================================
    private void toNotify() {
        Log.v("MAIN", "toNotify");
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();
        int left = Objects.requireNonNull(mLinearLayoutManager.findViewByPosition(off)).getLeft();
        if (left < -sScrWidth / 2) off++;
        View view = mLinearLayoutManager.findViewByPosition(off);
        System.out.println(view);

        RCDoc doc = mDocList.get(off);
//        MyViewHolder holder = (MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(off);

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
            Log.v("MAIN", "created RemoteViews");
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

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    @Override
    public void onRequestPermissionsResult(int requestCode, String @NonNull [] permissions,
            int @NonNull [] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            // Check if the permission was granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show();
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
        RCDoc doc = getDoc();
        sendDoc(doc); // for debugging
        if (DEBUG) {
            sendToPlayerViaIntent(docToBytes(doc), doc.toString());
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
        RemoteDocument rcdoc = doc.getDoc();
        if (rcdoc == null) {
            return null;
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

    RCDoc getDoc() {
        Log.v("MAIN", "toNotify");
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();
        int left = Objects.requireNonNull(mLinearLayoutManager.findViewByPosition(off)).getLeft();
        if (left < -sScrWidth / 2) off++;
        View view = mLinearLayoutManager.findViewByPosition(off);
        System.out.println(view);

        return mDocList.get(off);
    }

    private void sendToNotify(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                toNotify();
            } else {
                System.out.println("requesting permission");
                askNotificationPermission();
                //requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            toNotify();
        }
    }

    // ========================= Widget ====================================
    private void toWidget(View v) {
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();
        int left = Objects.requireNonNull(mLinearLayoutManager.findViewByPosition(off)).getLeft();
        if (left < -sScrWidth / 2) off++;
        View view = mLinearLayoutManager.findViewByPosition(off);
        System.out.println(view);

//        Card ui = (Card) view;
        RCDoc doc = mDocList.get(off);
//        MyViewHolder holder = (MyViewHolder) mRecyclerView.findViewHolderForAdapterPosition(off);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

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
        } catch (Exception e) {
            // Handle the exception
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            RemoteViews.DrawInstructions drawInstruction = new RemoteViews.DrawInstructions.Builder(
                    List.of(bytes)).build();
            RemoteViews remoteViews = new RemoteViews(drawInstruction);
            Intent intent = new Intent(this, ExperimentRecyclerActivity.class);
            intent.setAction("com.example.ACTION_VIEW_CLICKED");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                    PendingIntent.FLAG_MUTABLE);
            int id = 567;
            remoteViews.setOnClickPendingIntent(id, pendingIntent);
            appWidgetManager.updateAppWidget(ExperimentWidgetProvider.sAppWidgetIds, remoteViews);
            System.out.println("update widget ");
        } else {
            System.out.println("VERSION.SDK_INT < VANILLA_ICE_CREAM");
        }
    }

    private void launch(View v) {
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();

        int left = Objects.requireNonNull(mLinearLayoutManager.findViewByPosition(off)).getLeft();
        if (left < -sScrWidth / 2) off++;
        // View view =  mLinearLayoutManager.findViewByPosition(off);
        launch(mDocList.get(off));
    }

    private void logDoc() {
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();

        int left = Objects.requireNonNull(mLinearLayoutManager.findViewByPosition(off)).getLeft();
        if (left < -sScrWidth / 2) off++;
        // View view =  mLinearLayoutManager.findViewByPosition(off);
        try {
            Log.v("MAIN", Objects.requireNonNull(
                    mDocList.get(off).getDoc()).getDocument().toNestedString());
        } catch (Exception e) {
            Log.v("MAIN", "error in printing");
        }
    }

    void launch(RCDoc doc) {
        Log.v("MAIN", " launch $to_run");
        String composeKey = "USE_COMPOSE";
        String showComposeKey = "SHOW_COMPOSE";
        String showOrigamiKey = "SHOW_ORIGAMI";
        String debugComposeKey = "DEBUG_ORIGAMI";
        Intent intent = new Intent(this, ExperimentActivity.class);
        intent.putExtra(composeKey, doc.toString());
        intent.putExtra(showComposeKey, "true");
        intent.putExtra(showOrigamiKey, "true");
        intent.putExtra(debugComposeKey, "false");
        startActivity(intent);
    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     */
    public void paletteChange() {
        int off = mLinearLayoutManager.findFirstVisibleItemPosition();

        Card card = (Card) mLinearLayoutManager.findViewByPosition(off);
        if (card == null) {
            System.err.println("card is null");
            return;
        }

        card.mPlayer.reloadPalette();

    }

    /**
     * Called when the checked state of a compound button has changed.
     *
     * @param buttonView The button view whose state has changed.
     * @param isChecked  The new checked state of buttonView.
     */
    public void lock(@NonNull CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            buttonView.setBackgroundColor(0xFFFFAAAA);

            int off = mLinearLayoutManager.findFirstVisibleItemPosition();

            int left = Objects.requireNonNull(
                    mLinearLayoutManager.findViewByPosition(off)).getLeft();
            if (left < -sScrWidth / 2) off++;
            mRecyclerView.smoothScrollToPosition(off);
            RecyclerView.SimpleOnItemTouchListener listener =
                    new RecyclerView.SimpleOnItemTouchListener() {
                        @Override
                        public boolean onInterceptTouchEvent(@NonNull RecyclerView rv,
                                @NonNull MotionEvent e) {
                            return true;
                        }
                    };
            mRecyclerView.addOnItemTouchListener(listener);
            mRecyclerView.postDelayed(() -> {
                mRecyclerView.removeOnItemTouchListener(listener);
                mRecyclerView.suppressLayout(true);
            }, 100);

        } else {
            buttonView.setBackgroundColor(0xFFAAFFAA);
            logDoc();
            mRecyclerView.suppressLayout(false);
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
            mTitle = new TextView(context);
            mStats = new TextView(context);
            mPlayer.setShaderControl(new ShaderControl() {
                @Override
                public boolean isShaderValid(@NonNull String shader) {
                    return true;
                }
            });
//            int mode =(mPlayer.getResources().getConfiguration().isNightModeActive())?Rc.Theme
//            .DARK:Rc.Theme.LIGHT;
//            mPlayer.setTheme(mode);
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
            LayoutParams params = new LayoutParams(size, size);
            // mPlayer.setBackground(sDrawable);
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

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    };

    // Adapter class
    private static class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final List<RCDoc> mItems;

        MyAdapter(List<RCDoc> items) {
            this.mItems = items;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Create a TextView as item view
            Card layout = new Card(parent.getContext());

            return new MyViewHolder(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    // ViewHolder class
    private static class MyViewHolder extends RecyclerView.ViewHolder {
        private final Card mUi;
        RCDoc mItem;
        String mTimeStr;
        int mDocOps;
        int mSize;
        int mZipSize;
        Handler mHandler = new Handler(Looper.getMainLooper());

        // In your Activity or Fragment
        Runnable mUpdateTask;

        MyViewHolder(@NonNull Card player) {
            super(player);
            mUi = player;
        }

        DecimalFormat mDf1 = new DecimalFormat("#,###.##");
        DecimalFormat mDf2 = new DecimalFormat("#,###");

        @SuppressLint("RestrictedApiAndroidX")
        public void bind(RCDoc item) {
            mItem = item;
            if (mUpdateTask != null) {
                mHandler.removeCallbacks(mUpdateTask);
            }

            long time = System.nanoTime();
            RemoteDocument doc = item.getDoc();
            if (doc == null) {
                try {
                    mUi.mPlayer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            bind(item);
                        }
                    }, 2);
                    //  Thread. sleep(10);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            mTimeStr = mDf1.format((System.nanoTime() - time) * 1E-6f) + " ms";

            mUpdateTask = new Runnable() {
                long mLastTime = System.nanoTime();

                @Override
                @SuppressLint("RestrictedApiAndroidX")
                public void run() {
                    // Example: Update specific item data or randomize
                    updateItem();
                    long time = System.nanoTime();
                    float diff = (time - mLastTime) * 1E-9f;

                    if (diff > 2 && mItem != null) {
                        RemoteDocument loop = mItem.getLooper();
                        if (loop != null) {
                            mLastTime = time;
                            if (BACKGROUND) {
                                runOnThread(mHandler, new RunThenUI() {
                                    RemoteComposePlayer.PreparedDocument mPDoc;

                                    @Override
                                    public void run() {
                                        mPDoc = mUi.mPlayer.prepareDocument(loop);
                                    }

                                    @Override
                                    public void runOnUI() {
                                        mUi.mPlayer.setPreparedDocument(mPDoc);
                                    }
                                });
                            } else {
                                mUi.mPlayer.setDocument(loop);
                            }
                        }
                    }
                    // Schedule the next update
                    mHandler.postDelayed(this, 300); // Update every 2 seconds
                }
            };
            mHandler.postDelayed(mUpdateTask, 300);
            Log.v("perf", "Current document is \"" + item + "\"");
            if (BACKGROUND) {
                runOnThread(mHandler, new RunThenUI() {
                    RemoteComposePlayer.PreparedDocument mPDoc;

                    @Override
                    public void run() {
                        mPDoc = mUi.mPlayer.prepareDocument(item.getDoc());
                    }

                    @Override
                    public void runOnUI() {
                        mUi.mPlayer.setPreparedDocument(mPDoc);
                    }
                });
            } else {
                RemoteDocument docData = item.getDoc();
                byte[] byteData = docToBytes(item);
                String update = "\n"
                        + "data = 0, 1, -1, 0, 0, 0.4, 0.2, 0\n";
                if (byteData.length < 1) { // not used
                    byteData = addUpdateData(byteData, update);
                }
                if (docData != null) {
                    mUi.mPlayer.setDocument(byteData);
                }
            }

            // ui.player.setDebug(1);
            String[] data = doc.getStats();

            mDocOps = Integer.parseInt(data[0].split(":")[1].trim());
            Log.v("perf", "doc has " + mDocOps + " opps");
            // int ops = ui.player.getOpsPerFrame();
            mUi.mTitle.setText(item.toString());
        }

        void populateStats(int ops, float evalTime) {
            if (mSize == 0) {
                mSize = mItem.size();
                mZipSize = mItem.zipSize();
            }
            float build = mItem.getBuildTime();
            String stats = "  Load: " + mTimeStr + " bld: " + mDf2.format(build) + "ms\n   cmd: "
                    + ops + " / " + mDocOps
                    + "\n  Size: " + mDf2.format(mSize) + "/" + mDf2.format(mZipSize)
                    + " B" + "\n Frame: "
                    + mDf2.format(evalTime) + " ms";
            mUi.mStats.setText(stats);
        }

        public void updateItem() {
            int ops = mUi.mPlayer.getOpsPerFrame();
            float time = mUi.mPlayer.getEvalTime();
            populateStats(ops, time);
        }
    }

    /**
     * Creates a document with a name and a writer supplier
     *
     * @param name name of the document
     * @param gen  the writer supplier
     * @return the document
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RCDoc getpc(@NonNull String name,
            @NonNull Supplier<RemoteComposeContext> gen) {
        return getp(name, () -> gen.get().mRemoteWriter);
    }

    /**
     * Creates a document with a name and a writer supplier
     *
     * @param name           name of the document
     * @param writerSupplier the writer supplier
     * @return the document
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
     * Creates a document with a name and a writer supplier
     *
     * @param name            name of the document
     * @param writerSupplier  the writer supplier
     * @param writerSupplier2 the second writer supplier
     * @return the document
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

            @Override
            @NonNull
            public String toString() {
                return name;
            }

            @Override
            public RemoteDocument getLooper() {
                RemoteComposeWriter w = writer2();
                return new RemoteDocument(
                        new ByteArrayInputStream(w.buffer(), 0, w.bufferSize()));
            }

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
     * Saves the document to a file
     *
     * @param name       name of the file
     * @param buff       the document
     * @param appContext the application context
     */
    public static void saveDoc(@NonNull String name, byte @Nullable [] buff, @NonNull Context
            appContext, boolean compress) {
        if (buff == null) {
            return;
        }
        int len = buff.length;
        if (name.indexOf('/') >= 0) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }

        name = name.replaceFirst("^\\d+", "");
        // convert to snake case
        name = name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
        File storageDir = appContext.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES); // Using internal storage
        if (storageDir != null) {
            storageDir = new File("/storage/self/primary/Download/");
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
            System.out.println("WARNING IMAGE EXIST " + name + " len = " + buff.length + "  "
                    + imageFile.length());
            if (buff.length != imageFile.length()) {
                imageFile = new File(storageDir, name + "_c.rc");
            }
        }
        if (compress) {
            imageFile = new File(storageDir, name + ".rcz");
        }
        System.out.println("adb pull " + imageFile.getAbsolutePath() + " /tmp/");

        if (compress) {
            try
                    (FileOutputStream fos = new FileOutputStream(imageFile);
                     BufferedOutputStream bos = new BufferedOutputStream(fos);
                     ZipOutputStream zos = new ZipOutputStream(bos)) {

                // optional: choose compression level [0..9]
                zos.setLevel(Deflater.DEFAULT_COMPRESSION);

                ZipEntry entry = new ZipEntry("data.rcz"); // name inside the zip
                zos.putNextEntry(entry);
                zos.write(buff);
                zos.closeEntry();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                // Compress and write the bitmap to the file
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

    /**
     * Kicks off the computation on a new background thread.
     */
    private static void runOnThread(final Handler handler, RunThenUI runnable) {

        // Create and start a new thread to do the heavy work
        new Thread(new Runnable() {
            @Override
            public void run() {
                runnable.run();

                // After the work is done, update the UI on the main thread
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        runnable.runOnUI();
                    }
                });
            }
        }).start(); // Don't forget to start the thread!
    }

    private static byte[] addUpdateData(byte[] rcData, String update) {
        byte[] strData = update.getBytes(StandardCharsets.UTF_8);
        int len = strData.length;
        byte[] ret = new byte[rcData.length + 5 + len];
        int off = rcData.length;
        ret[off++] = (byte) 195;
        System.out.println("............" + (int) (0xFF & ret[off]) + ".......");
        ret[off++] = (byte) ((len >> 24) & 0xFF);
        ret[off++] = (byte) ((len >> 16) & 0xFF);
        ret[off++] = (byte) ((len >> 8) & 0xFF);
        ret[off++] = (byte) (len & 0xFF);

        System.arraycopy(rcData, 0, ret, 0, rcData.length);
        System.arraycopy(strData, 0, ret, rcData.length + 5, strData.length);
        return ret;
    }

}
