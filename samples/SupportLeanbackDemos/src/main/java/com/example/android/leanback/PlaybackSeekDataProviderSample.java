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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Sample PlaybackSeekDataProvider render time label as thumb.
 */
class PlaybackSeekDataProviderSample extends PlaybackSeekAsyncDataProvider {

    Paint mPaint;

    PlaybackSeekDataProviderSample(long duration, long interval) {
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
        Bitmap bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.YELLOW);
        canvas.drawText(formatTime(position), 10, 80, mPaint);
        canvas.drawText(Integer.toString(index), 10, 150, mPaint);
        return bmp;
    }

    String formatTime(long ms) {
        long seconds = ms / 1000;
        float seconds2 = (ms - seconds * 1000) / 1000f;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds -= minutes * 60;
        minutes -= hours * 60;

        StringBuilder b = new StringBuilder();
        if (hours > 0) {
            b.append(hours).append(':');
            if (minutes < 10) {
                b.append('0');
            }
        }
        b.append(minutes).append(':');
        if (seconds < 10) {
            b.append('0');
        }
        b.append(String.format("%.2f", ((float) seconds + seconds2)));
        return b.toString();
    }

}
