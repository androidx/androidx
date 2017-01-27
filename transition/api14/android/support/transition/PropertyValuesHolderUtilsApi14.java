/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.transition;

import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.support.annotation.RequiresApi;
import android.util.Property;

@RequiresApi(14)
class PropertyValuesHolderUtilsApi14 implements PropertyValuesHolderUtilsImpl {

    @Override
    public PropertyValuesHolder ofPointF(Property<?, PointF> property, Path path) {
        return PropertyValuesHolder.ofObject(property, new PathEvaluator(path));
    }

    private static class PathEvaluator implements TypeEvaluator<PointF> {

        private final PointF mPointF = new PointF();
        private final PathMeasure mPathMeasure;
        private final float mPathLength;
        private final float[] mPosition = new float[2];

        PathEvaluator(Path path) {
            mPathMeasure = new PathMeasure(path, false);
            mPathLength = mPathMeasure.getLength();
        }

        @Override
        public PointF evaluate(float fraction, PointF startValue, PointF endValue) {
            mPathMeasure.getPosTan(mPathLength * fraction, mPosition, null);
            mPointF.set(mPosition[0], mPosition[1]);
            return mPointF;
        }

    }

}
