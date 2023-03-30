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

import androidx.constraintlayout.core.motion.utils.KeyCache;
import androidx.constraintlayout.core.motion.utils.TypedValues;

import org.junit.Test;

public class MotionCustomAttributesTest {
    private static final boolean DEBUG = true;

    @Test
    public void testBasic() {
        assertEquals(2, 1 + 1);

    }

    class Scene {
        MotionWidget mMW1 = new MotionWidget();
        MotionWidget mMW2 = new MotionWidget();
        MotionWidget mRes = new MotionWidget();
        KeyCache mCache = new KeyCache();
        Motion mMotion;
        float mPos;

        Scene() {
            mMotion = new Motion(mMW1);
//            mw1.setBounds(0, 0, 30, 40);
//            mw2.setBounds(400, 400, 430, 440);
//            motion.setPathMotionArc(ArcCurveFit.ARC_START_VERTICAL);
        }

        public void setup() {
            mMotion.setStart(mMW1);
            mMotion.setEnd(mMW2);
            mMotion.setup(0, 0, 1, 1000000);
        }

        void sample(Runnable r) {
            for (int p = 0; p <= 10; p++) {
                mPos = p;
                mMotion.interpolate(mRes, p * 0.1f, 1000000 + (int) (p * 100), mCache);
                r.run();
            }
        }
    }

    @Test
    public void customFloat() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("bob", TypedValues.Custom.TYPE_FLOAT, 0f);
        s.mMW2.setCustomAttribute("bob", TypedValues.Custom.TYPE_FLOAT, 1f);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getCustomAttribute("bob").getFloatValue());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0.5, s.mRes.getCustomAttribute("bob").getFloatValue(), 0.001);
    }

    @Test
    public void customColor1() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF00FF00);
        s.mMW2.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFFFF00FF);
        s.setup();

        s.sample(() -> {
            System.out.println(s.mPos + " "
                    + Integer.toHexString(
                    s.mRes.getCustomAttribute("fish")
                            .getColorValue()));
        });

        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0xffbababa, s.mRes.getCustomAttribute("fish").getColorValue());
    }

    @Test
    public void customColor2() {
        Scene s = new Scene();
        s.mMW1.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0xFF000000);
        s.mMW2.setCustomAttribute("fish", TypedValues.Custom.TYPE_COLOR, 0x00880088);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mPos + " "
                        + Integer.toHexString(
                        s.mRes.getCustomAttribute("fish")
                                .getColorValue()));
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0x7f630063, s.mRes.getCustomAttribute("fish").getColorValue());
    }


}
