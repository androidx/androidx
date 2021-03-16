/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/**
 * Observable Resources class.
 */
@SuppressWarnings("deprecation")
class TestResources extends Resources {
    private boolean mGetDrawableCalled;

    TestResources(Resources res) {
        super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        mGetDrawableCalled = true;
        return super.getDrawable(id);
    }

    public void resetGetDrawableCalled() {
        mGetDrawableCalled = false;
    }

    public boolean wasGetDrawableCalled() {
        return mGetDrawableCalled;
    }
}
