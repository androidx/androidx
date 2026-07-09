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

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.compose.remote.creation.json.RemoteComposeJsonParser;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;
import androidx.compose.remote.integration.view.demos.customviews.AndroidCustomSupport;
import androidx.compose.remote.player.core.RemoteDocument;
import androidx.compose.remote.player.view.RemoteComposePlayer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Example Activity displaying a horizontal RecyclerView of RemoteCompose
 * documents loaded from JSON asset files using RemoteComposeJsonParser.
 */
@SuppressWarnings("RestrictedApiAndroidX")
public class ExampleJsonRecyclerActivity extends Activity {

    private static class DemoItem {
        final String mName;
        final RemoteDocument mDoc;
        final int mJsonSizeBytes;
        final int mJsonGzipSizeBytes;
        final int mBinarySizeBytes;
        final int mBinaryGzipSizeBytes;
        final int mNumOperations;
        final int mNumComponents;

        DemoItem(@NonNull String name, @Nullable RemoteDocument doc,
                int jsonSizeBytes, int jsonGzipSizeBytes,
                int binarySizeBytes, int binaryGzipSizeBytes,
                int numOperations, int numComponents) {
            this.mName = name;
            this.mDoc = doc;
            this.mJsonSizeBytes = jsonSizeBytes;
            this.mJsonGzipSizeBytes = jsonGzipSizeBytes;
            this.mBinarySizeBytes = binarySizeBytes;
            this.mBinaryGzipSizeBytes = binaryGzipSizeBytes;
            this.mNumOperations = numOperations;
            this.mNumComponents = numComponents;
        }
    }

    private static int getGzippedSize(byte[] data) {
        if (data == null || data.length == 0) return 0;
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.GZIPOutputStream gzos = new java.util.zip.GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
            return baos.toByteArray().length;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int countOperations(
            @NonNull List<androidx.compose.remote.core.Operation> ops) {
        int count = 0;
        for (androidx.compose.remote.core.Operation op : ops) {
            count++;
            if (op instanceof androidx.compose.remote.core.operations.layout.Container) {
                count += countOperations(
                        ((androidx.compose.remote.core.operations.layout.Container) op).getList());
            }
        }
        return count;
    }

    private static int countComponents(
            @NonNull List<androidx.compose.remote.core.Operation> ops) {
        int count = 0;
        for (androidx.compose.remote.core.Operation op : ops) {
            if (op instanceof androidx.compose.remote.core.operations.layout.Component
                    || op instanceof androidx.compose.remote.core.operations.layout
                    .ComponentStart) {
                count++;
            }
            if (op instanceof androidx.compose.remote.core.operations.layout.Container) {
                count += countComponents(
                        ((androidx.compose.remote.core.operations.layout.Container) op).getList());
            }
        }
        return count;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        List<DemoItem> items = new ArrayList<>();
        AndroidxRcPlatformServices platform = new AndroidxRcPlatformServices();

        try {
            String[] files = getAssets().list("");
            if (files != null) {
                java.util.Arrays.sort(files);
                for (String file : files) {
                    if (file.endsWith(".json")) {
                        String jsonString = loadJsonFromAsset(file);
                        if (jsonString != null) {
                            try {
                                byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);
                                int jsonGzipSize = getGzippedSize(jsonBytes);
                                ByteBuffer buffer = RemoteComposeJsonParser.parse(
                                        jsonString, platform);
                                byte[] docBytes = new byte[buffer.remaining()];
                                buffer.duplicate().get(docBytes);
                                int docGzipSize = getGzippedSize(docBytes);
                                RemoteDocument doc = new RemoteDocument(docBytes);
                                List<androidx.compose.remote.core.Operation> ops =
                                        doc.getDocument().getOperations();
                                int numOps = countOperations(ops);
                                int numComponents = countComponents(ops);
                                items.add(new DemoItem(
                                        file, doc, jsonBytes.length, jsonGzipSize,
                                        docBytes.length, docGzipSize, numOps, numComponents));
                            } catch (Exception e) {
                                android.util.Log.e("JsonRecycler", "Error parsing " + file, e);
                                String errStr = e.getMessage() != null
                                        ? e.getMessage() : e.toString();
                                items.add(new DemoItem(
                                        file + "\nError: " + errStr, null,
                                        0, 0, 0, 0, 0, 0));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("JsonRecycler", "Error listing assets", e);
        }

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new JsonDocAdapter(items));

        setContentView(recyclerView);
    }

    private @Nullable String loadJsonFromAsset(@NonNull String fileName) {
        try (InputStream is = getAssets().open(fileName);
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) != -1) {
                baos.write(buffer, 0, length);
            }
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            android.util.Log.e("JsonRecycler", "Error loading asset " + fileName, e);
        }
        return null;
    }

    private static class JsonDocAdapter extends RecyclerView.Adapter<JsonDocViewHolder> {
        private final List<DemoItem> mItems;

        JsonDocAdapter(@NonNull List<DemoItem> items) {
            mItems = items;
        }

        @Override
        public @NonNull JsonDocViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setBackgroundColor(0xFFEEEEEE);

            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    800, 1400);
            lp.setMargins(24, 24, 24, 24);
            container.setLayoutParams(lp);

            TextView title = new TextView(parent.getContext());
            title.setTextSize(20f);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setTextColor(Color.BLACK);
            title.setPadding(16, 16, 16, 16);
            container.addView(title);

            RemoteComposePlayer player = new RemoteComposePlayer(parent.getContext());
            player.setCustomSupport(new AndroidCustomSupport(player));
            player.setShaderControl(shader -> true);
            LinearLayout.LayoutParams playerLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            player.setLayoutParams(playerLp);
            container.addView(player);

            TextView infoView = new TextView(parent.getContext());
            infoView.setTextSize(13f);
            infoView.setTypeface(Typeface.MONOSPACE);
            infoView.setTextColor(0xFF334155);
            infoView.setPadding(16, 12, 16, 16);
            container.addView(infoView);

            return new JsonDocViewHolder(container, title, player, infoView);
        }

        @Override
        public void onBindViewHolder(@NonNull JsonDocViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private static class JsonDocViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;
        private final RemoteComposePlayer mPlayer;
        private final TextView mInfoView;

        JsonDocViewHolder(
                @NonNull LinearLayout itemView,
                @NonNull TextView title,
                @NonNull RemoteComposePlayer player,
                @NonNull TextView infoView) {
            super(itemView);
            mTitle = title;
            mPlayer = player;
            mInfoView = infoView;
        }

        void bind(@NonNull DemoItem item) {
            mTitle.setText(item.mName);
            if (item.mDoc != null) {
                mPlayer.setDocument(item.mDoc);
                mInfoView.setText(String.format(java.util.Locale.US,
                        "JSON Size:  %,d B (gzip: %,d B)\nDoc Size:   %,d B (gzip: %,d B)\n"
                                + "Operations: %,d\nComponents: %,d",
                        item.mJsonSizeBytes, item.mJsonGzipSizeBytes,
                        item.mBinarySizeBytes, item.mBinaryGzipSizeBytes,
                        item.mNumOperations, item.mNumComponents));
            } else {
                mPlayer.setBackgroundColor(Color.RED);
                mInfoView.setText("Error loading document");
            }
        }
    }
}
