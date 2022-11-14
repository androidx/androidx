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

import org.junit.Test;

public class MotionBenchmarkTest {

    private static final boolean DEBUG = false;

    int setUpMotionController() {
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

        Motion motion = new Motion(mw1);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.addKey(keyPosition);
        motion.addKey(keyPosition2);
        motion.setup(1000, 1000, 2, 1000000);
        motion.interpolate(res, 0.1f, 1000000 + (int) (0.5 * 100), cache);
        return res.getLeft();
    }

    int setUpMotionArcController() {
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

        Motion motion = new Motion(mw1);
        motion.setPathMotionArc(ArcCurveFit.ARC_START_HORIZONTAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);
        motion.addKey(keyPosition);
        motion.addKey(keyPosition2);
        motion.setup(1000, 1000, 2, 1000000);
        motion.interpolate(res, 0.1f, 1000000 + (int) (0.5 * 100), cache);
        return res.getLeft();
    }

    @Test
    public void motionController1000xSetup() {
        for (int i = 0; i < 1000; i++) {
            int left = setUpMotionController();
            assertEquals(40, left);
        }
    }

    @Test
    public void motionControllerArc1000xSetup() {
        for (int i = 0; i < 1000; i++) {
            int left = setUpMotionArcController();
            assertEquals(60, left);
        }
    }
}
