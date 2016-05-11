/*
 * Copyright (C) 2012 The Android Open Source Project
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

package android.support.v4.view;

import android.content.Context;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

class PagerTitleStripIcs {
    public static void setSingleLineAllCaps(TextView text) {
        text.setTransformationMethod(new SingleLineAllCapsTransform(text.getContext()));
    }

    private static class SingleLineAllCapsTransform extends SingleLineTransformationMethod {
        private static final String TAG = "SingleLineAllCapsTransform";

        private Locale mLocale;

        public SingleLineAllCapsTransform(Context context) {
            mLocale = context.getResources().getConfiguration().locale;
        }

        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            source = super.getTransformation(source, view);
            return source != null ? source.toString().toUpperCase(mLocale) : null;
        }
    }
}
