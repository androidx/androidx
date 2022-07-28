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

import androidx.constraintlayout.core.motion.key.MotionKeyPosition;
import androidx.constraintlayout.core.motion.utils.ArcCurveFit;
import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.TypedValues;
import androidx.constraintlayout.core.motion.utils.Utils;

import org.junit.Test;

public class MotionKeyPositionTest {

    private static final boolean DEBUG = false;

    @Test
    public void testBasic() {
        assertEquals(2, 1 + 1);

    }

    @Test
    public void keyPosition1() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(400, 400, 430, 440);
        // mw1.motion.mPathMotionArc = MotionWidget.A
        Motion motion = new Motion(mw1);
        motion.setPathMotionArc(ArcCurveFit.ARC_START_VERTICAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);

        motion.setup(1000, 1000, 1, 1000000);
        if (DEBUG) {
            for (float p = 0; p <= 1; p += 0.1) {
                motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
                System.out.println(res);
            }
        }
        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);
        int left = (int) (0.5 + 400 * (1 - Math.sqrt(0.5)));
        int top = (int) (0.5 + 400 * (Math.sqrt(0.5)));
        assertEquals(left, res.getLeft());
        assertEquals(147, res.getRight());
        assertEquals(top, res.getTop(), 0.01);
        assertEquals(top + 40, res.getBottom());

    }

    @Test
    public void keyPosition2() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(400, 400, 430, 440);
        // mw1.motion.mPathMotionArc = MotionWidget.A
        Motion motion = new Motion(mw1);
        motion.setPathMotionArc(ArcCurveFit.ARC_START_HORIZONTAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.setup(1000, 1000, 2, 1000000);
        motion.interpolate(res, 0.5f, 1000000 + (int) (0.5 * 100), cache);
        System.out.println("0.5 " + res);
        if (DEBUG) {
            for (float p = 0; p <= 1; p += 0.01) {
                motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
                System.out.println(res + " ,     " + p);
            }
        }
        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);

        assertEquals(283, res.getLeft());
        assertEquals(313, res.getRight());
        assertEquals(117, res.getTop());
        assertEquals(157, res.getBottom());
    }

    @Test
    public void keyPosition3() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(400, 400, 460, 480);
        MotionKeyPosition keyPosition = new MotionKeyPosition();
        keyPosition.setFramePosition(30);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_X, 0.3f);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_Y, 0.3f);

        MotionKeyPosition keyPosition2 = new MotionKeyPosition();
        keyPosition2.setFramePosition(88);
        keyPosition2.setValue(TypedValues.PositionType.TYPE_PERCENT_X, .9f);
        keyPosition2.setValue(TypedValues.PositionType.TYPE_PERCENT_Y, 0.5f);

        // mw1.motion.mPathMotionArc = MotionWidget.A
        Motion motion = new Motion(mw1);
        //  motion.setPathMotionArc(ArcCurveFit.ARC_START_HORIZONTAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.addKey(keyPosition);
        motion.addKey(keyPosition2);
        motion.setup(1000, 1000, 2, 1000000);
        motion.interpolate(res, 0.5f, 1000000 + (int) (0.5 * 100), cache);
        System.out.println("0.5 " + res);
        if (DEBUG) {

            String str = "";
            for (float p = 0; p <= 1; p += 0.01) {
                motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
                str += res + "\n";
            }
            Utils.socketSend(str);
        }
        motion.interpolate(res, 0f, 1000000 + 1000, cache);
        assertEquals("0, 0, 30, 40", res.toString());
        motion.interpolate(res, 0.2f, 1000000 + 1000, cache);
        assertEquals("80, 86, 116, 134", res.toString());
        motion.interpolate(res, 0.3f, 1000000 + 1000, cache);
        assertEquals("120, 120, 159, 172", res.toString());
        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);
        assertEquals("204, 120, 249, 180", res.toString());
        motion.interpolate(res, 0.7f, 1000000 + 1000, cache);
        assertEquals("289, 106, 339, 174", res.toString());
        motion.interpolate(res, 0.9f, 1000000 + 1000, cache);
        assertEquals("367, 215, 424, 291", res.toString());
        motion.interpolate(res, 1f, 1000000 + 1000, cache);
        assertEquals("400, 400, 460, 480", res.toString());
    }

    @Test
    public void keyPosition4() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        MotionKeyPosition keyPosition = new MotionKeyPosition();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(400, 400, 460, 480);
        keyPosition.setFramePosition(20);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_X, 1f);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_Y, 0.5f);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_HEIGHT, 0.2f);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_WIDTH, 1f);
        // mw1.motion.mPathMotionArc = MotionWidget.A
        Motion motion = new Motion(mw1);
        //  motion.setPathMotionArc(ArcCurveFit.ARC_START_HORIZONTAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.addKey(keyPosition);
        motion.setup(1000, 1000, 2, 1000000);
        motion.interpolate(res, 0.5f, 1000000 + (int) (0.5 * 100), cache);
        System.out.println("0.5 " + res);
        if (DEBUG) {
            for (float p = 0; p <= 1; p += 0.01) {
                motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
                System.out.println(res + " ,     " + p);
            }
        }
        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);

        assertEquals("400, 325, 460, 385", res.toString());
    }

    class Scene {
        MotionWidget mMW1 = new MotionWidget();
        MotionWidget mMW2 = new MotionWidget();
        MotionWidget mRes = new MotionWidget();
        KeyCache mCache = new KeyCache();
        Motion mMotion;
        float mProgress;

        Scene() {
            mMotion = new Motion(mMW1);
            mMW1.setBounds(0, 0, 30, 40);
            mMW2.setBounds(400, 400, 430, 440);
        }

        public void setup() {
            mMotion.setStart(mMW1);
            mMotion.setEnd(mMW2);
            mMotion.setup(1000, 1000, 1, 1000000);
        }

        void sample(Runnable r) {
            for (int p = 0; p <= 10; p++) {
                mProgress = p * 0.1f;
                mMotion.interpolate(mRes, mProgress, 1000000 + (int) (p * 100), mCache);
                r.run();
            }
        }
    }

    @Test
    public void keyPosition3x() {
        Scene s = new Scene();
        KeyCache cache = new KeyCache();
        int[] frames = {25, 50, 75};

        float[] percentX = {0.1f, 0.8f, 0.1f};
        float[] percentY = {0.4f, 0.8f, 0.0f};
        for (int i = 0; i < frames.length; i++) {
            MotionKeyPosition keyPosition = new MotionKeyPosition();
            keyPosition.setFramePosition(frames[i]);
            keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_X, percentX[i]);
            keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_Y, percentY[i]);

            s.mMotion.addKey(keyPosition);
        }

        s.setup();
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + (int) (0.5 * 100), cache);
        System.out.println("0.5 " + s.mRes);
        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mProgress + " ,     " + s.mRes);
            });
        }

        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + (int) (0.5 * 100), cache);
        System.out.println("0.5 " + s.mRes);

        assertEquals("320, 320, 350, 360", s.mRes.toString());
    }


}
