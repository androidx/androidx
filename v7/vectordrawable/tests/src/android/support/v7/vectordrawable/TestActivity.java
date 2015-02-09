/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v7.vectordrawable;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v7.graphics.drawable.VectorDrawableCompat;
import android.view.View;

import java.io.InputStream;

/**
 * @hide from javadoc
 */
public class TestActivity extends Activity {
    private static final String LOG_TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new SampleView(this));
    }

    private static class SampleView extends View {
        private Bitmap mBitmap;
        private VectorDrawableCompat mDrawable;

        private static void drawIntoBitmap(Bitmap bm) {
            float x = bm.getWidth();
            float y = bm.getHeight();
            Canvas c = new Canvas(bm);
            Paint p = new Paint();
            p.setAntiAlias(true);

            p.setAlpha(0x80);
            c.drawCircle(x/2, y/2, x/2, p);

            p.setAlpha(0x30);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            p.setTextSize(60);
            p.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = p.getFontMetrics();
            c.drawText("Alpha", x/2, (y-fm.ascent)/2, p);
        }

        public SampleView(Context context) {
            super(context);
            setFocusable(true);

            InputStream is = context.getResources().openRawResource(R.drawable.app_sample_code);
            mBitmap = BitmapFactory.decodeStream(is);

            // TODO: use R.drawable.vector_drawable01 when the aapt is able to
            // support.
            mDrawable = VectorDrawableCompat.createFromResource(context.getResources(), R.raw.vector_drawable01);
        }

        @Override protected void onDraw(Canvas canvas) {
            canvas.drawColor(Color.WHITE);

            mDrawable.draw(canvas);

            Paint p = new Paint();
            float y = 10;

            p.setColor(Color.RED);
            canvas.drawBitmap(mBitmap, 10, y, p);
            y += mBitmap.getHeight() + 10;

        }
    }

}
