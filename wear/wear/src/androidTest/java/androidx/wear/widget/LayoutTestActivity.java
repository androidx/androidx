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

package androidx.wear.widget;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class LayoutTestActivity extends Activity {
    public static final String EXTRA_LAYOUT_RESOURCE_ID = "layout_resource_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (!intent.hasExtra(EXTRA_LAYOUT_RESOURCE_ID)) {
            throw new IllegalArgumentException(
                    "Intent extras must contain EXTRA_LAYOUT_RESOURCE_ID");
        }
        int layoutId = intent.getIntExtra(EXTRA_LAYOUT_RESOURCE_ID, -1);
        setContentView(layoutId);
    }
}
