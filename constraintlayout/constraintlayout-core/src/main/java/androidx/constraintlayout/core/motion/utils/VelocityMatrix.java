/*
 * Copyright (C) 2020 The Android Open Source Project
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

package androidx.constraintlayout.core.motion.utils;

/**
 * This is used to calculate the related velocity matrix for a post layout matrix
 *
 *
 */
public class VelocityMatrix {
    float mDScaleX, mDScaleY, mDTranslateX, mDTranslateY, mDRotate;
    float mRotate;
    @SuppressWarnings("unused") private static String sTag = "VelocityMatrix";

    // @TODO: add description
    public void clear() {
        mDScaleX = mDScaleY = mDTranslateX = mDTranslateY = mDRotate = 0;
    }

    // @TODO: add description
    public void setRotationVelocity(SplineSet rot, float position) {
        if (rot != null) {
            mDRotate = rot.getSlope(position);
            mRotate = rot.get(position);
        }
    }

    // @TODO: add description
    public void setTranslationVelocity(SplineSet transX, SplineSet transY, float position) {

        if (transX != null) {
            mDTranslateX = transX.getSlope(position);
        }
        if (transY != null) {
            mDTranslateY = transY.getSlope(position);
        }
    }

    // @TODO: add description
    public void setScaleVelocity(SplineSet scaleX, SplineSet scaleY, float position) {

        if (scaleX != null) {
            mDScaleX = scaleX.getSlope(position);
        }
        if (scaleY != null) {
            mDScaleY = scaleY.getSlope(position);
        }
    }

    // @TODO: add description
    public void setRotationVelocity(KeyCycleOscillator oscR, float position) {
        if (oscR != null) {
            mDRotate = oscR.getSlope(position);
        }
    }

    // @TODO: add description
    public void setTranslationVelocity(KeyCycleOscillator oscX,
            KeyCycleOscillator oscY,
            float position) {

        if (oscX != null) {
            mDTranslateX = oscX.getSlope(position);
        }

        if (oscY != null) {
            mDTranslateY = oscY.getSlope(position);
        }
    }

    // @TODO: add description
    public void setScaleVelocity(KeyCycleOscillator oscSx,
            KeyCycleOscillator oscSy,
            float position) {
        if (oscSx != null) {
            mDScaleX = oscSx.getSlope(position);
        }
        if (oscSy != null) {
            mDScaleY = oscSy.getSlope(position);
        }
    }

    /**
     * Apply the transform a velocity vector
     *
     *
     */
    public void applyTransform(float locationX,
            float locationY,
            int width,
            int height,
            float[] mAnchorDpDt) {
        float dx = mAnchorDpDt[0];
        float dy = mAnchorDpDt[1];
        float offx = 2 * (locationX - 0.5f);
        float offy = 2 * (locationY - 0.5f);
        dx += mDTranslateX;
        dy += mDTranslateY;
        dx += offx * mDScaleX;
        dy += offy * mDScaleY;
        float r = (float) Math.toRadians(mRotate);
        float dr = (float) Math.toRadians(mDRotate);
        dx += dr * (float) (-width * offx * Math.sin(r) - height * offy * Math.cos(r));
        dy += dr * (float) (width * offx * Math.cos(r) - height * offy * Math.sin(r));
        mAnchorDpDt[0] = dx;
        mAnchorDpDt[1] = dy;
    }
}
