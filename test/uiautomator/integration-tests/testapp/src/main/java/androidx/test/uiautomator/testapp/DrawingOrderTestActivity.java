/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.test.uiautomator.testapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Contains three partially overlapping sibling views for testing drawing order (z-index). */
public class DrawingOrderTestActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_order_test_activity);
    }

    public static class Layout extends RelativeLayout {
        // Default to order in XML (red, green, blue).
        private final List<Integer> mOrder = Arrays.asList(0, 1, 2);

        public Layout(@Nullable Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            setChildrenDrawingOrderEnabled(true);
            setOnClickListener(v -> {
                // Swap the order of green and blue when clicked.
                Collections.swap(mOrder, 1, 2);
                invalidate();
            });
        }

        @Override
        protected int getChildDrawingOrder(int childCount, int drawingPosition) {
            return mOrder.get(drawingPosition);
        }
    }
}
