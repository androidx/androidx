/*
 * Copyright (C) 2016 The Android Open Source Project
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
 * limitations under the License
 */

package com.google.android.leanbackjank.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.net.Uri;
import android.util.Log;

import com.google.android.leanbackjank.model.VideoInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Generates fake data for populating the video cards.
 */
public final class VideoProvider {

    private static final String TAG = "JankVideoProvider";
    private static final int STUDIO_COUNT = 13;

    private static final List<Integer> COLORS = Arrays.asList(
            Color.parseColor("#4285F4"),
            Color.parseColor("#EA4335"),
            Color.parseColor("#FABB05"),
            Color.parseColor("#34A853")
    );

    private VideoProvider() {
    }

    public static HashMap<String, List<VideoInfo>> buildMedia(int categoryCount, int entriesPerCat,
            int width, int height, Context context, boolean useSingleBitmap) {
        HashMap<String, List<VideoInfo>> ret = new HashMap<>();

        int count = 0;
        String rootPath = String.format(Locale.US, "%s/%d_%d/", context.getFilesDir(), width,
                height);
        File rootDirectory = new File(rootPath);
        rootDirectory.mkdirs();

        for (int i = 0; i < categoryCount; i++) {
            List<VideoInfo> list = new ArrayList<>();
            String category = "Category " + Integer.toString(i);
            ret.put(category, list);
            for (int j = 0; j < entriesPerCat; j++) {
                String description = String.format(Locale.US,
                        "The gripping yet whimsical description of videoInfo %d in category %d", j,
                        i);
                String title = String.format(Locale.US, "Video %d-%d", i, j);
                String studio = String.format(Locale.US, "Studio %d", count % STUDIO_COUNT);

                VideoInfo videoInfo = new VideoInfo();
                videoInfo.setId(Integer.toString(count));
                videoInfo.setTitle(title);
                videoInfo.setDescription(description);
                videoInfo.setStudio(studio);
                videoInfo.setCategory(category);

                int videoNumber = useSingleBitmap ? 0 : count;
                File file = new File(rootPath + videoNumber + ".jpg");
                if (!file.exists()) {
                    makeIcon(width, height, "Jank", file);
                }
                videoInfo.setImageUri(Uri.fromFile(file));

                count++;

                list.add(videoInfo);
            }
        }

        return ret;
    }

    public static void makeIcon(int width, int height, String string, File file) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Collections.shuffle(COLORS);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        // Draw background gradient.
        Shader shader = new LinearGradient(0, 0, width - 1, height - 1, COLORS.get(0),
                COLORS.get(1), TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawRect(0, 0, width - 1, height - 1, paint);

        paint.setTextSize(height * 0.5f);
        Rect rect = new Rect();
        paint.getTextBounds(string, 0, string.length(), rect);

        int hOffset = (height - rect.height()) / 2;
        int wOffset = (width - rect.width()) / 2;
        shader = new LinearGradient(wOffset, height - hOffset, width - wOffset, hOffset,
                COLORS.get(2), COLORS.get(3), TileMode.CLAMP);
        paint.setShader(shader);

        canvas.drawText(string, width / 2, (height + rect.height()) / 2, paint);

        try {
            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 90, outputStream);
        } catch (IOException e) {
            Log.e(TAG, "Cannot write image to file: " + file, e);
        }
    }
}
