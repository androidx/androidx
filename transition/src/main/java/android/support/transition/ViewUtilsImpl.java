/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.graphics.Matrix;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;

@RequiresApi(14)
interface ViewUtilsImpl {

    ViewOverlayImpl getOverlay(@NonNull View view);

    WindowIdImpl getWindowId(@NonNull View view);

    void setTransitionAlpha(@NonNull View view, float alpha);

    float getTransitionAlpha(@NonNull View view);

    void saveNonTransitionAlpha(@NonNull View view);

    void clearNonTransitionAlpha(@NonNull View view);

    void transformMatrixToGlobal(@NonNull View view, @NonNull Matrix matrix);

    void transformMatrixToLocal(@NonNull View view, @NonNull Matrix matrix);

    void setAnimationMatrix(@NonNull View view, Matrix matrix);

    void setLeftTopRightBottom(View v, int left, int top, int right, int bottom);

}
