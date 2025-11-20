/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import org.jspecify.annotations.Nullable;

public class VisibleBoundsTestActivity extends TestActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.visible_bounds_test_activity);

        TextView partlyInvisibleRegion = findViewById(R.id.partly_invisible_region);
        TextView regionInsideScrollable = findViewById(R.id.region_inside_scrollable);

        partlyInvisibleRegion.setOnClickListener(new OnRegionClick());
        partlyInvisibleRegion.setOnLongClickListener(new OnRegionLongClick());
        regionInsideScrollable.setOnClickListener(new OnRegionClick());
    }

    static class OnRegionClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            Rect visibleRegion = new Rect();
            view.getGlobalVisibleRect(visibleRegion);
            int[] windowOffset = getWindowOffset(view);
            visibleRegion.offset(windowOffset[0], windowOffset[1]);
            ((TextView) view).setText(visibleRegion.toString());
        }
    }

    static class OnRegionLongClick implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            Rect visibleRegion = new Rect();
            view.getGlobalVisibleRect(visibleRegion);
            // To get the absolute screen coordinates of this view, it should apply the offset from
            // the host window to screen.
            int[] windowOffset = getWindowOffset(view);
            visibleRegion.offset(windowOffset[0], windowOffset[1]);
            Point visibleRegionCenter = new Point(visibleRegion.centerX(), visibleRegion.centerY());
            ((TextView) view).setText(visibleRegionCenter.toString());
            return true;
        }
    }

    /** Returns the offset from the host window of the view to screen. */
    static int[] getWindowOffset(View view) {
        // Gets top-left in screen and the host window of the given view.
        int[] screenXY = new int[2];
        int[] windowXY = new int[2];
        view.getLocationOnScreen(screenXY);
        view.getLocationInWindow(windowXY);

        // Gets the offset between window and screen coordinates. This will be non-zero when the
        // view's host window is in desktop mode or a popup.
        int offsetX = screenXY[0] - windowXY[0];
        int offsetY = screenXY[1] - windowXY[1];
        return new int[]{offsetX, offsetY};
    }
}
