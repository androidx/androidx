/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

/**
 * Base implementation of {@link DrawSpec} which just draws basic {@link Rect}s.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RectDrawSpec extends DrawSpec {

    public RectDrawSpec(@NonNull Paint paint, @NonNull List<Rect> shapes) {
        super(paint, shapes);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        for (Rect rect : mRects) {
            canvas.drawRect(rect, mPaint);
        }
    }
}
