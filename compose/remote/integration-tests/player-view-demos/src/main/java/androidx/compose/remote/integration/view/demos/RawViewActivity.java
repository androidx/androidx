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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
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
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;


import androidx.compose.remote.integration.view.demos.notifications.RemoteNotification;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@SuppressLint("RestrictedApiAndroidX")
public class RawViewActivity extends Activity {
    static byte[] sDoc = new byte[0];
    RcDocs[] mFoundDocs;
    private static final boolean USE_PLAYER = true;

    /**
     * Get the current document.
     * @return the current document
     */
    public static byte @NonNull [] getCurrentDoc() {
        return sDoc;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            sDoc = readRawResource(this, R.raw.digital_clock1);
        } catch (Exception e) {
            Log.e("RawViewActivity", "Failed to read initial resource", e);
        }
        getRcFiles();

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        float[] dim = new float[2];
        getDisplayMetricsDimensions(dim);
        int orientation = RecyclerView.HORIZONTAL;

        recyclerView.setLayoutManager(new LinearLayoutManager(this, orientation, false));
        recyclerView.setAdapter(new DocAdapter());

        setContentView(recyclerView);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private class DocAdapter extends RecyclerView.Adapter<DocAdapter.ViewHolder> {
        @NonNull
        @Override
        @SuppressLint("RestrictedApiAndroidX")
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout itemLayout = new LinearLayout(parent.getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            itemLayout.setPadding(20, 20, 20, 20);

            FrameLayout rcViewContainer = new FrameLayout(parent.getContext());
            rcViewContainer.setLayoutParams(new ViewGroup.LayoutParams(800, 800));

            RemoteComposePlayer player = null;
            if (USE_PLAYER) {
                player = new RemoteComposePlayer(parent.getContext());
                rcViewContainer.addView(player);
            }

            LinearLayout controlsRow = new LinearLayout(parent.getContext());
            controlsRow.setOrientation(LinearLayout.HORIZONTAL);
            controlsRow.setGravity(Gravity.CENTER_VERTICAL);
            controlsRow.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView nameLabel = new TextView(parent.getContext());
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            nameLabel.setLayoutParams(labelParams);

            Button launcherButton = new Button(parent.getContext());
            launcherButton.setText("Launcher");
            launcherButton.setAllCaps(false);

            Button notificationButton = new Button(parent.getContext());
            notificationButton.setText("Notification");
            notificationButton.setAllCaps(false);

            controlsRow.addView(nameLabel);
            controlsRow.addView(launcherButton);
            controlsRow.addView(notificationButton);

            itemLayout.addView(rcViewContainer);
            itemLayout.addView(controlsRow);

            return new ViewHolder(itemLayout, rcViewContainer, player, nameLabel, launcherButton,
                    notificationButton);
        }

        @Override
        @SuppressLint("RestrictedApiAndroidX")
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RcDocs docItem = mFoundDocs[position];
            holder.mNameLabel.setText(docItem.name);

            holder.mLauncherButton.setOnClickListener((v) -> {
                try {
                    click(position);
                } catch (Exception e) {
                    Log.e("RawViewActivity", "Error on Launcher click for " + docItem.name, e);
                }
            });

            holder.mNotificationButton.setOnClickListener((v) -> {
                try {
                    sendToNotify(position);
                } catch (Exception e) {
                    Log.e("RawViewActivity", "Error on Notification click for " + docItem.name, e);
                }
            });

            try {
                byte[] bytes = readRawResource(RawViewActivity.this, docItem.id);

                if (USE_PLAYER) {
                    if (holder.mPlayer != null) {
                        holder.mPlayer.setDocument(bytes);
                    }
                } else {
                    holder.mRcViewContainer.removeAllViews();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        RemoteViews.DrawInstructions.Builder r =
                                new RemoteViews.DrawInstructions.Builder(List.of(bytes));

                        RemoteViews remoteViews = new RemoteViews(r.build());
                        View previewView = remoteViews.apply(RawViewActivity.this,
                                holder.mRcViewContainer);
                        holder.mRcViewContainer.addView(previewView);
                    } else {
                        TextView errorView = new TextView(RawViewActivity.this);
                        errorView.setText("RemoteViews not supported");
                        holder.mRcViewContainer.addView(errorView);
                    }
                }
            } catch (Exception e) {
                Log.e("RawViewActivity", "Error binding view holder for " + docItem.name, e);
                if (!USE_PLAYER) {
                    holder.mRcViewContainer.removeAllViews();
                }
                TextView errorView = new TextView(RawViewActivity.this);
                errorView.setText("Error loading " + docItem.name + "\n" + e.getMessage());
                holder.mRcViewContainer.addView(errorView);
            }
        }

        @Override
        public int getItemCount() {
            return mFoundDocs == null ? 0 : mFoundDocs.length;
        }

        @SuppressLint("RestrictedApiAndroidX")
        class ViewHolder extends RecyclerView.ViewHolder {
            FrameLayout mRcViewContainer;
            RemoteComposePlayer mPlayer;
            TextView mNameLabel;
            Button mLauncherButton;
            Button mNotificationButton;

            @SuppressLint("RestrictedApiAndroidX")
            ViewHolder(View itemView, FrameLayout rcViewContainer, RemoteComposePlayer player,
                       TextView nameLabel, Button launcherButton, Button notificationButton) {
                super(itemView);
                this.mRcViewContainer = rcViewContainer;
                this.mPlayer = player;
                this.mNameLabel = nameLabel;
                this.mLauncherButton = launcherButton;
                this.mNotificationButton = notificationButton;
            }
        }
    }

    /**
     * Handle click on launcher button.
     * @param i position of the document
     */
    @SuppressLint("RestrictedApiAndroidX")
    public void click(int i) {
        try {
            sDoc = readRawResource(this, mFoundDocs[i].id);
            Log.v("MAIN", ">>>>>> loading " + mFoundDocs[i].name);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                RemoteViews.DrawInstructions.Builder r =
                        new RemoteViews.DrawInstructions.Builder(List.of(sDoc));
                RemoteViews remoteViews = new RemoteViews(r.build());

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(
                        getApplicationContext());
                ComponentName myProvider = new ComponentName(this, ExperimentWidgetProvider.class);

                int[] ids = appWidgetManager.getAppWidgetIds(myProvider);
                if (ids != null && ids.length > 0) {
                    appWidgetManager.updateAppWidget(ids, remoteViews);
                } else if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                    Bundle extras = new Bundle();
                    extras.putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, remoteViews);
                    appWidgetManager.requestPinAppWidget(myProvider, extras, null);
                }
            }
        } catch (Exception e) {
            Log.e("RawViewActivity", "Click failed for " + mFoundDocs[i].name, e);
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private void sendToNotify(int position) {
        try {
            RcDocs docItem = mFoundDocs[position];
            byte[] bytes = readRawResource(this, docItem.id);
            new RemoteNotification(this)
                    .doc(bytes)
                    .title("Remote Compose Demo")
                    .text("Rendering: " + docItem.name)
                    .send();
        } catch (Exception e) {
            Log.e("RawViewActivity", "Failed to send notification", e);
        }
    }

    @SuppressLint("RestrictedApiAndroidX")
    private void getDisplayMetricsDimensions(float[] dimensions) {
        try {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            int height = displayMetrics.heightPixels;
            int width = displayMetrics.widthPixels;

            dimensions[0] = width;
            dimensions[1] = height;
        } catch (Exception e) {
            Log.e("RawViewActivity", "Failed to get display metrics", e);
            dimensions[0] = 1080;
            dimensions[1] = 1920;
        }
    }

    /**
     * Read a raw resource as a byte array.
     * @param context context to use
     * @param resId resource id
     * @return the byte array
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static byte @NonNull[] readRawResource(@NonNull Context context, int resId) {
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
            Log.e("RawViewActivity", "Failed to read resource " + resId, e);
            return new byte[0];
        }
    }

    public static class RcDocs {
        public final int id;
        public final String name;

        public RcDocs(int id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }
    }

    /**
     * Get the list of RC files from the raw resources.
     */
    @SuppressLint("RestrictedApiAndroidX")
    public void getRcFiles() {
        ArrayList<RcDocs> result = new ArrayList<>();

        try {
            Class<?> rawClass = R.raw.class;
            Field[] fields = rawClass.getFields();
            for (Field field : fields) {
                String name = field.getName();
                int id = field.getInt(null);
                result.add(new RcDocs(id, name));
            }

            mFoundDocs = result.toArray(new RcDocs[0]);
            Arrays.sort(mFoundDocs, Comparator.comparing(o -> o.name));
        } catch (Exception e) {
            Log.e("RawViewActivity", "Failed to discover RC files", e);
        }
    }
}
