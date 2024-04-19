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

package androidx.window.extensions;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;

public class TestActivity extends Activity implements View.OnLayoutChangeListener {

    private int mRootViewId;
    private CountDownLatch mLayoutLatch = new CountDownLatch(1);
    private static CountDownLatch sResumeLatch = new CountDownLatch(1);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final View contentView = new View(this);
        mRootViewId = View.generateViewId();
        contentView.setId(mRootViewId);
        setContentView(contentView);

        getWindow().getDecorView().addOnLayoutChangeListener(this);
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft,
            int oldTop, int oldRight, int oldBottom) {
        mLayoutLatch.countDown();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sResumeLatch.countDown();
    }
}
