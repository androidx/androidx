/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.percentlayout.widget;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

abstract class BaseTestActivity extends Activity {
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        final int contentView = getContentViewLayoutResId();
        if (contentView > 0) {
            setContentView(contentView);
        }
        onContentViewSet();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    protected abstract int getContentViewLayoutResId();

    protected void onContentViewSet() {}
}
