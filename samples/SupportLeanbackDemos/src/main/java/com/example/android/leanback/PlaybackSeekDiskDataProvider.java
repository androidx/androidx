/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.example.android.leanback;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.File;

/**
 * Sample PlaybackSeekDataProvider that reads bitmaps stored on disk.
 * e.g. new PlaybackSeekDiskDataProvider(duration, 1000, "/sdcard/frame_%04d.jpg")
 * Expects the seek positions are 1000ms interval, snapshots are stored at
 * /sdcard/frame_0001.jpg, ...
 */
class PlaybackSeekDiskDataProvider extends PlaybackSeekAsyncDataProvider {

    final Paint mPaint;
    final String mPathPattern;
    PlaybackSeekDiskDataProvider(long duration, long interval, String pathPattern) {
        mPathPattern = pathPattern;
        int size = (int) (duration / interval) + 1;
        long[] pos = new long[size];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = i * duration / pos.length;
        }
        setSeekPositions(pos);
        mPaint = new Paint();
        mPaint.setTextSize(16);
        mPaint.setColor(Color.BLUE);
    }

    protected Bitmap doInBackground(Object task, int index, long position) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            // Thread might be interrupted by cancel() call.
        }
        if (isCancelled(task)) {
            return null;
        }
        String path = String.format(mPathPattern, (index + 1));
        if (new File(path).exists()) {
            return BitmapFactory.decodeFile(path);
        } else {
            Bitmap bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(Color.YELLOW);
            canvas.drawText(path, 10, 80, mPaint);
            canvas.drawText(Integer.toString(index), 10, 150, mPaint);
            return bmp;
        }
    }

}
