/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import android.view.View;
import android.view.ViewTreeObserver;

import java.util.concurrent.CountDownLatch;

class CountOnPreDraw implements ViewTreeObserver.OnPreDrawListener {
    final CountDownLatch mLatch;
    final View mView;

    CountOnPreDraw(CountDownLatch latch, View view) {
        this.mLatch = latch;
        this.mView = view;
    }

    @Override
    public boolean onPreDraw() {
        mView.post(new Runnable() {
            @Override
            public void run() {
                mView.getViewTreeObserver().removeOnPreDrawListener(CountOnPreDraw.this);
                mLatch.countDown();
            }
        });
        return true;
    }
}
