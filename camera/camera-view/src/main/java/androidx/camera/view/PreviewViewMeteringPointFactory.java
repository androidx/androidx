/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Rational;
import android.util.Size;

import androidx.annotation.AnyThread;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.UiThread;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.impl.utils.Threads;

/**
 * {@link MeteringPointFactory} for {@link PreviewView}.
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PreviewViewMeteringPointFactory extends MeteringPointFactory {

    static final PointF INVALID_POINT = new PointF(2F, 2F);

    @NonNull
    private final PreviewTransformation mPreviewTransformation;
    @GuardedBy("this")
    @Nullable
    private Rect mSensorRect = null;

    @GuardedBy("this")
    @Nullable
    private Matrix mMatrix;

    PreviewViewMeteringPointFactory(@NonNull PreviewTransformation previewTransformation) {
        mPreviewTransformation = previewTransformation;
    }

    @AnyThread
    @NonNull
    @Override
    protected PointF convertPoint(float x, float y) {
        float[] point = new float[]{x, y};
        synchronized (this) {
            if (mMatrix == null) {
                return INVALID_POINT;
            }
            mMatrix.mapPoints(point);
        }
        return new PointF(point[0], point[1]);
    }

    public void setSensorRect(Rect sensorRect) {
        setSurfaceAspectRatio(new Rational(sensorRect.width(), sensorRect.height()));
        synchronized (this) {
            mSensorRect = sensorRect;
        }
    }

    @UiThread
    void recalculate(@NonNull Size previewViewSize, int layoutDirection) {
        Threads.checkMainThread();
        synchronized (this) {
            if (previewViewSize.getWidth() == 0 || previewViewSize.getHeight() == 0
                    || mSensorRect == null) {
                mMatrix = null;
                return;
            }
            mMatrix = mPreviewTransformation.getPreviewViewToNormalizedSensorMatrix(
                    previewViewSize,
                    layoutDirection,
                    mSensorRect);
        }
    }
}
