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

public class MotionKeyAttributesTest {
    private static final boolean DEBUG = false;

    @Test
    public void basic() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void attributes() {
        MotionWidget mw1 = new MotionWidget();
        MotionWidget mw2 = new MotionWidget();
        MotionWidget res = new MotionWidget();
        KeyCache cache = new KeyCache();
        mw1.setBounds(0, 0, 30, 40);
        mw2.setBounds(400, 400, 430, 440);
        mw1.setRotationZ(0);
        mw2.setRotationZ(360);
        // mw1.motion.mPathMotionArc = MotionWidget.A
        Motion motion = new Motion(mw1);
        motion.setPathMotionArc(ArcCurveFit.ARC_START_VERTICAL);
        motion.setStart(mw1);
        motion.setEnd(mw2);

        motion.setup(1000, 1000, 1, 1000000);
        if (DEBUG) {
            for (int p = 0; p <= 10; p++) {
                motion.interpolate(res, p * 0.1f, 1000000 + (int) (p * 100), cache);
                System.out.println(p * 0.1f + " " + res.getRotationZ());
            }
        }
        motion.interpolate(res, 0.5f, 1000000 + 1000, cache);
        assertEquals(180, res.getRotationZ(), 0.001);
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
            for (int p = 0; p <= 10; p++) {
                mMotion.interpolate(mRes, p * 0.1f, 1000000 + (int) (p * 100), mCache);
                r.run();
            }
        }
    }


    public Scene basicRange(int type, float start, float end) {
        Scene s = new Scene();
        s.mMW1.setValue(type, start);
        s.mMW2.setValue(type, end);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);

        return s;
    }


    @Test
    public void checkRotationZ() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_ROTATION_Z, 0, 360);
        assertEquals(180, s.mRes.getRotationZ(), 0.001);
    }


    @Test
    public void checkRotationX() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_ROTATION_X, 0, 100);
        assertEquals(50, s.mRes.getRotationX(), 0.001);
    }

    @Test
    public void checkRotationY() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_ROTATION_Y, 0, 50);
        assertEquals(25, s.mRes.getRotationY(), 0.001);
    }

    @Test
    public void checkTranslateX() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_TRANSLATION_X, 0, 30);
        assertEquals(15, s.mRes.getTranslationX(), 0.001);
    }

    @Test
    public void checkTranslateY() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_TRANSLATION_Y, 0, 40);
        assertEquals(20, s.mRes.getTranslationY(), 0.001);
    }

    @Test
    public void checkTranslateZ() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_TRANSLATION_Z, 0, 18);
        assertEquals(9, s.mRes.getTranslationZ(), 0.001);
    }

    @Test
    public void checkScaleX() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_SCALE_X, 1, 19);
        assertEquals(10, s.mRes.getScaleX(), 0.001);
    }


    @Test
    public void checkScaleY() {
        Scene s = basicRange(TypedValues.AttributesType.TYPE_SCALE_Y, 1, 3);
        assertEquals(2, s.mRes.getScaleY(), 0.001);
    }

    @Test
    public void attributesRotateX() {
        Scene s = new Scene();
        s.mMW1.setRotationX(-10);
        s.mMW2.setRotationX(10);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0, s.mRes.getRotationX(), 0.001);
    }

    @Test
    public void attributesRotateY() {
        Scene s = new Scene();
        s.mMW1.setRotationY(-10);
        s.mMW2.setRotationY(10);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0, s.mRes.getRotationY(), 0.001);
    }

    @Test
    public void attributesRotateZ() {
        Scene s = new Scene();
        s.mMW1.setRotationZ(-10);
        s.mMW2.setRotationZ(10);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(0, s.mRes.getRotationZ(), 0.001);
    }

    @Test
    public void attributesTranslateX() {
        Scene s = new Scene();
        s.mMW1.setTranslationX(-10);
        s.mMW2.setTranslationX(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getTranslationX(), 0.001);
    }

    @Test
    public void attributesTranslateY() {
        Scene s = new Scene();
        s.mMW1.setTranslationY(-10);
        s.mMW2.setTranslationY(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getTranslationY(), 0.001);
    }

    @Test
    public void attributesTranslateZ() {
        Scene s = new Scene();
        s.mMW1.setTranslationZ(-10);
        s.mMW2.setTranslationZ(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getTranslationZ(), 0.001);
    }

    @Test
    public void attributesScaleX() {
        Scene s = new Scene();
        s.mMW1.setScaleX(-10);
        s.mMW2.setScaleX(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getScaleX(), 0.001);
    }

    @Test
    public void attributesScaleY() {
        Scene s = new Scene();
        s.mMW1.setScaleY(-10);
        s.mMW2.setScaleY(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getScaleY(), 0.001);
    }

    @Test
    public void attributesPivotX() {
        Scene s = new Scene();
        s.mMW1.setPivotX(-10);
        s.mMW2.setPivotX(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getPivotX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getPivotX(), 0.001);
    }

    @Test
    public void attributesPivotY() {
        Scene s = new Scene();
        s.mMW1.setPivotY(-10);
        s.mMW2.setPivotY(40);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getPivotY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(15.0, s.mRes.getPivotY(), 0.001);
    }

    @Test
    public void keyFrameRotateX() {
        Scene s = new Scene();
        s.mMW1.setRotationX(-10);
        s.mMW2.setRotationX(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationX(), 0.001);
    }

    @Test
    public void keyFrameRotateY() {
        Scene s = new Scene();
        s.mMW1.setRotationY(-10);
        s.mMW2.setRotationY(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationY(), 0.001);
    }

    @Test
    public void keyFrameRotateZ() {
        Scene s = new Scene();
        s.mMW1.setRotationZ(-10);
        s.mMW2.setRotationZ(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_Z, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationZ(), 0.001);
    }


    @Test
    public void keyFrameTranslationX() {
        Scene s = new Scene();
        s.mMW1.setTranslationX(-10);
        s.mMW2.setTranslationX(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationX(), 0.001);
    }

    @Test
    public void keyFrameTranslationY() {
        Scene s = new Scene();
        s.mMW1.setTranslationY(-10);
        s.mMW2.setTranslationY(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationY(), 0.001);
    }

    @Test
    public void keyFrameTranslationZ() {
        Scene s = new Scene();
        s.mMW1.setTranslationZ(-10);
        s.mMW2.setTranslationZ(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_Z, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationZ(), 0.001);
    }

    @Test
    public void keyFrameScaleX() {
        Scene s = new Scene();
        s.mMW1.setScaleX(-10);
        s.mMW2.setScaleX(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_SCALE_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getScaleX(), 0.001);
    }

    @Test
    public void keyFrameScaleY() {
        Scene s = new Scene();
        s.mMW1.setScaleY(-10);
        s.mMW2.setScaleY(10);
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_SCALE_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getScaleY(), 0.001);
    }

    @Test
    public void keyFrameNoAttrRotateX() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationX(), 0.001);
    }

    @Test
    public void keyFrameNoAttrRotateY() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationY(), 0.001);
    }

    @Test
    public void keyFrameNoAttrRotateZ() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_ROTATION_Z, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getRotationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getRotationZ(), 0.001);
    }


    @Test
    public void keyFrameNoAttrTranslationX() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationX(), 0.001);
    }

    @Test
    public void keyFrameNoAttrTranslationY() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationY(), 0.001);
    }

    @Test
    public void keyFrameNoAttrTranslationZ() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_TRANSLATION_Z, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getTranslationZ());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getTranslationZ(), 0.001);
    }

    @Test
    public void keyFrameNoAttrScaleX() {
        Scene s = new Scene();

        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_SCALE_X, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleX());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getScaleX(), 0.001);
    }

    @Test
    public void keyFrameNoAttrScaleY() {
        Scene s = new Scene();
        MotionKeyAttributes attribute = new MotionKeyAttributes();
        attribute.setValue(TypedValues.AttributesType.TYPE_SCALE_Y, 23f);
        attribute.setFramePosition(50);
        s.mMotion.addKey(attribute);
        s.setup();

        if (DEBUG) {
            s.sample(() -> {
                System.out.println(s.mRes.getScaleY());
            });
        }
        s.mMotion.interpolate(s.mRes, 0.5f, 1000000 + 1000, s.mCache);
        assertEquals(23, s.mRes.getScaleY(), 0.001);
    }

}
