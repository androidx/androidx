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

import android.app.Activity;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class VisibleBoundsTestActivity extends Activity {

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
            ((TextView) view).setText(visibleRegion.toString());
        }
    }

    static class OnRegionLongClick implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View view) {
            Rect visibleRegion = new Rect();
            view.getGlobalVisibleRect(visibleRegion);
            Point visibleRegionCenter = new Point(visibleRegion.centerX(), visibleRegion.centerY());
            ((TextView) view).setText(visibleRegionCenter.toString());
            return true;
        }
    }
}
