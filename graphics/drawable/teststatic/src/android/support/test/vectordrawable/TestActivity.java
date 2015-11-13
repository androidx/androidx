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

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DecimalFormat;

public class TestActivity extends Activity {
    private static final String LOG_TAG = "TestActivity";

    private static final String LOGCAT = "VectorDrawable1";
    protected int[] icon = {
            R.drawable.vector_drawable_scale0,
            R.drawable.vector_drawable_scale1,
            R.drawable.vector_drawable_scale2,
            R.drawable.vector_drawable_scale3,
            R.drawable.vector_drawable01,
            R.drawable.vector_drawable02,
            R.drawable.vector_drawable03,
            R.drawable.vector_drawable04,
            R.drawable.vector_drawable05,
            R.drawable.vector_drawable06,
            R.drawable.vector_drawable07,
            R.drawable.vector_drawable08,
            R.drawable.vector_drawable09,
            R.drawable.vector_drawable10,
            R.drawable.vector_drawable11,
            R.drawable.vector_drawable12,
            R.drawable.vector_drawable13,
            R.drawable.vector_drawable14,
            R.drawable.vector_drawable15,
            R.drawable.vector_drawable16,
            R.drawable.vector_drawable17,
            R.drawable.vector_drawable18,
            R.drawable.vector_drawable19,
            R.drawable.vector_drawable20,
            R.drawable.vector_drawable21,
            R.drawable.vector_drawable22,
            R.drawable.vector_drawable23,
            R.drawable.vector_drawable24,
            R.drawable.vector_drawable25,
            R.drawable.vector_drawable26,
            R.drawable.vector_drawable27,
            R.drawable.vector_drawable28,
            R.drawable.vector_drawable29,
            R.drawable.vector_drawable30,
            R.drawable.vector_test01,
            R.drawable.vector_test02
    };

    private static final int EXTRA_TESTS = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        scrollView.addView(container);
        container.setOrientation(LinearLayout.VERTICAL);
        Resources res = this.getResources();
        container.setBackgroundColor(0xFF888888);
        VectorDrawableCompat []d = new VectorDrawableCompat[icon.length];
        long time =  android.os.SystemClock.currentThreadTimeMillis();
        for (int i = 0; i < icon.length; i++) {
             d[i] = VectorDrawableCompat.create(res, icon[i], getTheme());
        }
        time =  android.os.SystemClock.currentThreadTimeMillis()-time;

        // Testing Tint on one particular case.
        if (d.length > 3) {
            d[3].setTint(0x8000FF00);
            d[3].setTintMode(Mode.MULTIPLY);
        }

        // Testing Constant State like operation by creating the first 2 icons
        // from the 3rd one's constant state.
        VectorDrawableCompat []extras = new VectorDrawableCompat[EXTRA_TESTS];
        ConstantState state = d[0].getConstantState();
        extras[0] = (VectorDrawableCompat) state.newDrawable();
        extras[1] = (VectorDrawableCompat) state.newDrawable();

        // This alpha change is expected to affect both extra 0, 1, and d0.
        extras[0].setAlpha(128);

        d[0].mutate();
        d[0].setAlpha(255);

        // Just show the average create time as the first view.
        TextView t = new TextView(this);
        DecimalFormat df = new DecimalFormat("#.##");
        t.setText("avgL=" + df.format(time / (icon.length)) + " ms");
        container.addView(t);

        addDrawableButtons(container, extras);

        addDrawableButtons(container, d);

        setContentView(scrollView);
    }

    private void addDrawableButtons(LinearLayout container, VectorDrawableCompat[] d) {
        // Add the VD into consequent views.
        for (int i = 0; i < d.length; i++) {
            Button button = new Button(this);
            button.setWidth(200);
            // Note that setBackgroundResource() will fail b/c createFromXmlInner() failed
            // to recognize <vector> pre-L.
            button.setBackgroundDrawable(d[i]);
            container.addView(button);
        }
    }
}
