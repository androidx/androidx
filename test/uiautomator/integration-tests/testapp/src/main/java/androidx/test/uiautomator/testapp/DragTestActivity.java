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
import android.content.ClipData;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

@RequiresApi(24)
public class DragTestActivity extends Activity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.drag_test_activity);

        Button dragButton = findViewById(R.id.drag_button);
        TextView dragDestination = findViewById(R.id.drag_destination);

        dragButton.setOnLongClickListener(v -> {
            v.startDragAndDrop(ClipData.newPlainText(null, null),
                    new View.DragShadowBuilder(dragButton),
                    null,
                    0);
            return true;
        });

        dragButton.setOnDragListener((v, e) -> {
            if (e.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                Rect destRegion = new Rect();
                dragDestination.getGlobalVisibleRect(destRegion);
                if (destRegion.contains((int) e.getX(), (int) e.getY())) {
                    dragDestination.setText("drag_received");
                }
            }
            return true;
        });
    }
}
