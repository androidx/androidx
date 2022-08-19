/*
 * Copyright 2022 The Android Open Source Project
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
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class WaitTestActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.wait_test_activity);

        TextView text1 = findViewById(R.id.text_1);

        text1.setOnClickListener(view -> {
            // Unlike in `UiObject2`, the {@link UiObject#click()} will block the process until
            // either accessibility event `TYPE_WINDOW_CONTENT_CHANGED` or `TYPE_VIEW_SELECTED`
            // is received (as in {@link InteractionController#clickAndSync(final int x, final
            // int y, long timeout)}).
            // So we have to change the text right after the click to initialize a
            // `TYPE_WINDOW_CONTENT_CHANGED` event.
            // Otherwise, the click action will only block everything until the delayed text change
            // (as the `postDelayed()` method doing), which invalidates this testing mechanism.
            ((TextView) view).setText("text_1_clicked");
            new Handler().postDelayed(() -> ((TextView) view).setText("text_1_changed"),
                    3_000);
        });
    }
}
