/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion;

import static org.junit.Assert.assertEquals;

import androidx.constraintlayout.core.motion.key.MotionKeyAttributes;
import androidx.constraintlayout.core.motion.utils.ArcCurveFit;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.TypedValues;

import org.junit.Test;

import java.text.DecimalFormat;

public class MotionCustomKeyAttributesTest {
    private static final boolean DEBUG = true;
    DecimalFormat mDF = new DecimalFormat("0.0");

    class Scene {
        MotionWidget mMW1 = new MotionWidget();
        MotionWidget mMW2 = new MotionWidget();
        MotionWidget mRes = new MotionWidget();
        KeyCache mCache = new KeyCache();
        Motion mMotion;
        float mPos;

        Scene() {
            mMotion = new Motion(mMW1);
            mMW1.setBounds(0, 0, 30, 40);
            mMW2.setBounds(400, 400, 430, 440);
            mMotion.setPathMotionArc(ArcCurveFit.ARC_START_VERTICAL);
        }

        public void setup() {
            mMotion.setStart(mMW1);
            mMotion.setEnd(mMW2);
            mMotion.setup(1000, 1000, 1, 1000000);
        }

        void sample(Runnable r) {
            for (int p = 0; p <= 10; p++) {
                mPos = p * 0.1f;
                mMotion.interpolate(mRes, mPos, 1000000 + (int) (p * 100), mCache);
                r.run();
            }
        }
    }

    @Test
    public void customFloat() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("bob", TypedValues.Custom.TYPE_FLOAT, 0f);
        s.mMW2.setCustomAttribute("bob", TypedValues.Custom.TYPE_FLOAT, 1f);
        MotionKeyAttributes mka = new MotionKeyAttributes();
        mka.setFramePosition(50);
        mka.setCustomAttribute("bob", TypedValues.Custom.TYPE_FLOAT, 2f);
        s.mMotion.addKey(mka);
        s.setup();

        if (DEBUG) {
            s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);

            s.sample(() -> {
                System.out.println(mDF.format(s.mPos) + " " + s.mRes.getCustomAttribute("bob"));
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(2, s.mRes.getCustomAttribute("bob").getFloatValue(), 0.001);
    }

    @Test
    public void customColor1() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF000000);
        s.mMW2.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFFFFFFFF);
        MotionKeyAttributes mka = new MotionKeyAttributes();
        mka.setFramePosition(50);
        mka.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF000000);
        s.mMotion.addKey(mka);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(mDF.format(s.mPos) + "\t" + s.mRes.getCustomAttribute("fish"));
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0xFF000000, s.mRes.getCustomAttribute("fish").getColorValue());
    }

    @Test
    public void customColor2() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFFFF0000);
        s.mMW2.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF0000FF);
        MotionKeyAttributes mka = new MotionKeyAttributes();
        mka.setFramePosition(50);
        mka.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF00FF00);
        s.mMotion.addKey(mka);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(mDF.format(s.mPos) + " " + s.mRes.getCustomAttribute("fish"));

            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0xFF00FF00, s.mRes.getCustomAttribute("fish").getColorValue());
    }


}
