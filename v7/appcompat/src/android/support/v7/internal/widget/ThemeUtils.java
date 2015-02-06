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

package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;

class ThemeUtils {

    private static final ThreadLocal<TypedValue> TL_TYPED_VALUE = new ThreadLocal<>();

    private static final int[] DISABLED_STATE_SET = new int[]{-android.R.attr.state_enabled};
    private static final int[] EMPTY_STATE_SET = new int[0];

    static ColorStateList createDisabledStateList(Context context, int textColor) {
        final TypedValue tv = getTypedValue();

        if (context.getTheme().resolveAttribute(android.R.attr.disabledAlpha, tv, true)) {
            // Alter the original text color's alpha to disabledAlpha
            final int disabledColor = (textColor & 0x00ffffff) |
                    (Math.round(Color.alpha(textColor) * tv.getFloat()) << 24);

            // Now create a new ColorStateList with the default color, and the new disabled
            // color
            final int[][] states = new int[2][];
            final int[] colors = new int[2];
            int i = 0;

            // Disabled state
            states[i] = DISABLED_STATE_SET;
            colors[i] = disabledColor;
            i++;

            // Default state
            states[i] = EMPTY_STATE_SET;
            colors[i] = textColor;
            i++;

            return new ColorStateList(states, colors);
        }
        // Else, we'll just create a default state list
        return ColorStateList.valueOf(textColor);
    }

    private static TypedValue getTypedValue() {
        TypedValue typedValue = TL_TYPED_VALUE.get();
        if (typedValue == null) {
            typedValue = new TypedValue();
            TL_TYPED_VALUE.set(typedValue);
        }
        return typedValue;
    }

}
