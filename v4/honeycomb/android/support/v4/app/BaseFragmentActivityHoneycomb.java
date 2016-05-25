/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.v4.app;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Base class for {@code FragmentActivity} to be able to use v11 APIs.
 *
 * @hide
 */
abstract class BaseFragmentActivityHoneycomb extends BaseFragmentActivityEclair {

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        final View v = dispatchFragmentsOnCreateView(parent, name, context, attrs);
        if (v == null && Build.VERSION.SDK_INT >= 11) {
            // If we're running on HC or above, let the super have a go
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }

}
