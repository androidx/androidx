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

package android.support.test.vectordrawable;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;

public class TestAVDActivity extends Activity implements View.OnClickListener{
    private static final String LOG_TAG = "TestActivity";

    private static final String LOGCAT = "VectorDrawable1";
    protected int[] icon = {
        R.drawable.animation_vector_drawable_grouping_1,
        R.drawable.animation_vector_progress_bar,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ObjectAnimator oa = new ObjectAnimator();
        super.onCreate(savedInstanceState);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        scrollView.addView(container);
        container.setOrientation(LinearLayout.VERTICAL);
        Resources res = this.getResources();
        container.setBackgroundColor(0xFF888888);
        AnimatedVectorDrawableCompat []d = new AnimatedVectorDrawableCompat[icon.length];
        long time =  android.os.SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < icon.length; i++) {
             d[i] = AnimatedVectorDrawableCompat.create(this, icon[i]);
        }
        time =  android.os.SystemClock.currentThreadTimeMillis()-time;
        TextView t = new TextView(this);
        DecimalFormat df = new DecimalFormat("#.##");
        t.setText("avgL=" + df.format(time / (icon.length)) + " ms");
        container.addView(t);

        addDrawableButtons(container, d);

        // Now test constant state and mutate a bit.
        AnimatedVectorDrawableCompat []copies = new AnimatedVectorDrawableCompat[3];
        copies[0] = (AnimatedVectorDrawableCompat) d[0].getConstantState().newDrawable();
        copies[1] = (AnimatedVectorDrawableCompat) d[0].getConstantState().newDrawable();
        copies[2] = (AnimatedVectorDrawableCompat) d[0].getConstantState().newDrawable();
        copies[0].setAlpha(128);

        // Expect to see the copies[0, 1] are showing alpha 128, and [2] are showing 255.
        copies[2].mutate();
        copies[2].setAlpha(255);

        addDrawableButtons(container, copies);

        setContentView(scrollView);
    }

    private void addDrawableButtons(LinearLayout container, AnimatedVectorDrawableCompat[] d) {
        for (int i = 0; i < d.length; i++) {
            Button button = new Button(this);
            button.setWidth(200);
            button.setHeight(200);
            button.setBackgroundDrawable(d[i]);
            container.addView(button);
            button.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        AnimatedVectorDrawableCompat d = (AnimatedVectorDrawableCompat) v.getBackground();
        d.start();
    }
}
