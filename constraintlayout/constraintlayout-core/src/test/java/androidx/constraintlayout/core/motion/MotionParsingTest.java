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
import androidx.constraintlayout.core.motion.parse.KeyParser;
import androidx.constraintlayout.core.motion.utils.ArcCurveFit;
import androidx.constraintlayout.core.motion.utils.KeyCache;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class MotionParsingTest {

    private static final boolean DEBUG = false;

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

    String mStr = "{"
            + "frame:22,\n"
            + "target:'widget1',\n"
            + "easing:'easeIn',\n"
            + "curveFit:'spline',\n"
            + "progress:0.3,\n"
            + "alpha:0.2,\n"
            + "elevation:0.7,\n"
            + "rotationZ:23,\n"
            + "rotationX:25.0,\n"
            + "rotationY:27.0,\n"
            + "pivotX:15,\n"
            + "pivotY:17,\n"
            + "pivotTarget:'32',\n"
            + "pathRotate:23,\n"
            + "scaleX:0.5,\n"
            + "scaleY:0.7,\n"
            + "translationX:5,\n"
            + "translationY:7,\n"
            + "translationZ:11,\n"
            + "}";

    @Test
    public void parseKeAttributes() {
        MotionKeyAttributes mka = new MotionKeyAttributes();
        KeyParser.parseAttributes(mStr).applyDelta(mka);
        assertEquals(22, mka.mFramePosition);
        HashSet<String> attrs = new HashSet<>();
        mka.getAttributeNames(attrs);
        String[] split = mStr.replace("\n", "").split("[,:\\{\\}]");
        ArrayList<String> expectlist = new ArrayList<>();
        HashSet<String> exclude =
                new HashSet<>(Arrays.asList("curveFit", "easing",
                        "frame", "target", "pivotTarget"));
        for (int i = 1, j = 0; i < split.length; i += 2, j++) {
            System.out.println(i + " " + split[i]);
            if (!exclude.contains(split[i])) {
                expectlist.add(split[i]);
            }
        }
        String[] expect = expectlist.toArray(new String[0]);
        String[] result = attrs.toArray(new String[0]);
        Arrays.sort(result);
        Arrays.sort(expect);

        assertEquals(Arrays.toString(expect), Arrays.toString(result));
    }


}
