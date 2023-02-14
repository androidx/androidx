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

public class MotionControlTest {
    private static final boolean DEBUG = false;

    @Test
    public void testBasic() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void simpleLinear() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(500, 600, 530, 640);


        Motion motion = new Motion(mw1);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.setup(1000, 1000, 1, 1000000);
        System.out.println("-------------------------------------------");
        for (float p = 0; p <= 1; p += 0.1) {
            motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
            System.out.println(res);
        }

        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);
        assertEquals((int) (0.5 + (mw1.getLeft() + mw2.getLeft()) / 2), res.getLeft());
        assertEquals((int) (0.5 + (mw1.getRight() + mw2.getRight()) / 2), res.getRight());
        assertEquals((int) (0.5 + (mw1.getTop() + mw2.getTop()) / 2), res.getTop());
        assertEquals((int) (0.5 + (mw1.getBottom() + mw2.getBottom()) / 2), res.getBottom());

    }

    @Test
    public void archMode1() {
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
    public void archMode2() {
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
    public void archMode3() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        MotionKeyPosition keyPosition = new MotionKeyPosition();
        mw1.setBounds(0, 0, 3, 4);
        mw2.setBounds(400, 400, 460, 480);
        keyPosition.setFramePosition(50);
        keyPosition.setValue(TypedValues.PositionType.TYPE_PERCENT_X, 1.0f);
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
            String str = "";
            for (float p = 0; p <= 1; p += 0.01) {
                motion.interpolate(res, p, 1000000 + (int) (p * 100), cache);
                str += res + "\n";
            }
            Utils.socketSend(str);
        }
        motion.interpolate(res, 0.7f, 1000000 + 1000, cache);
        String str = res.toString();
        System.out.println(str);

        assertEquals("400, 288, 460, 328", str);
    }


}
