/*
 * Copyright (C) 2013 The Android Open Source Project
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
package android.support.v7.app;

import android.app.Activity;
import android.graphics.drawable.Drawable;

public class ActionBarImplJBMR2 extends ActionBarImplJB {

    public ActionBarImplJBMR2(Activity activity, Callback callback) {
        super(activity, callback);
    }

    @Override
    public void setHomeAsUpIndicator(Drawable indicator) {
        mActionBar.setHomeAsUpIndicator(indicator);
    }

    @Override
    public void setHomeAsUpIndicator(int resId) {
        mActionBar.setHomeAsUpIndicator(resId);
    }

    @Override
    public void setHomeActionContentDescription(CharSequence description) {
        mActionBar.setHomeActionContentDescription(description);
    }

    @Override
    public void setHomeActionContentDescription(int resId) {
        mActionBar.setHomeActionContentDescription(resId);
    }
}
