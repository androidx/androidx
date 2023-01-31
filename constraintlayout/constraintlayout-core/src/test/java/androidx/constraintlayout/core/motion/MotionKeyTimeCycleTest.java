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

import androidx.constraintlayout.core.motion.key.MotionKeyTimeCycle;
import androidx.constraintlayout.core.motion.utils.ArcCurveFit;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.TypedValues;

import org.junit.Test;

public class MotionKeyTimeCycleTest {
    private static final boolean DEBUG = true;
    private static final int SAMPLES = 30;
    private static final boolean DISABLE = true;

    void cycleBuilder(Scene s, int type) {
        float[] amp = {0, 50, 0};
        int[] pos = {0, 50, 100};
        float[] period = {0, 2, 0};
        for (int i = 0; i < amp.length; i++) {
            MotionKeyTimeCycle cycle = new MotionKeyTimeCycle();
            cycle.setValue(type, amp[i]);
            cycle.setValue(TypedValues.CycleType.TYPE_WAVE_PERIOD, period[i]);
            cycle.setFramePosition(pos[i]);
            s.mMotion.addKey(cycle);
        }
    }

    public Scene basicRunThrough(int type) {
        Scene s = new Scene();
        cycleBuilder(s, type);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getValueAttributes(type));
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        return s;
    }

    @Test
    public void disabled() {
        if (DISABLE) {
            System.out.println(" all test in MotionKeyTimeCycle DISABLE!");
        }
        assertEquals(DISABLE, true);
    }

    @Test
    public void keyCycleRotationX() {
        if (DISABLE) {
            return;
        }

        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_ROTATION_X);
        assertEquals(0.0, s.mRes.getRotationX(), 0.0001);
    }

    @Test
    public void keyCycleRotationY() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_ROTATION_Y);
        assertEquals(0.0, s.mRes.getRotationY(), 0.0001);
    }

    @Test
    public void keyCycleRotationZ() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_ROTATION_Z);
        assertEquals(0.0, s.mRes.getRotationZ(), 0.0001);
    }

    @Test
    public void keyCycleTranslationX() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_TRANSLATION_X);
        assertEquals(0.0, s.mRes.getTranslationX(), 0.0001);
    }

    @Test
    public void keyCycleTranslationY() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_TRANSLATION_Y);
        assertEquals(0.0, s.mRes.getTranslationY(), 0.0001);
    }

    @Test
    public void keyCycleTranslationZ() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_TRANSLATION_Z);
        assertEquals(0.0, s.mRes.getTranslationZ(), 0.0001);
    }

    @Test
    public void keyCycleScaleX() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_SCALE_X);
        assertEquals(0.0, s.mRes.getScaleX(), 0.0001);
    }

    @Test
    public void keyCycleScaleY() {
        if (DISABLE) {
            return;
        }
        Scene s = basicRunThrough(TypedValues.CycleType.TYPE_SCALE_Y);
        assertEquals(0.0, s.mRes.getScaleY(), 0.0001);
    }

    class Scene {
        MotionWidget mMW1 = new MotionWidget();
        MotionWidget mMW2 = new MotionWidget();
        MotionWidget mRes = new MotionWidget();
        KeyCache mCache = new KeyCache();
        Motion mMotion;

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
            for (int p = 0; p <= SAMPLES; p++) {
                mMotion.interpolate(mRes, p * 0.1f, 1000000 + (int) (p * 100), mCache);
                r.run();
            }
        }
    }
}
