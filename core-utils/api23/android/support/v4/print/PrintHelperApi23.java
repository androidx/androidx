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

package android.support.v4.print;

import android.content.Context;
import android.print.PrintAttributes;

/**
 * Api23 specific PrintManager API implementation.
 */
class PrintHelperApi23 extends PrintHelperApi20 {
    @Override
    protected PrintAttributes.Builder copyAttributes(PrintAttributes other) {
        PrintAttributes.Builder b = super.copyAttributes(other);

        if (other.getDuplexMode() != 0) {
            b.setDuplexMode(other.getDuplexMode());
        }

        return b;
    }

    PrintHelperApi23(Context context) {
        super(context);

        mIsMinMarginsHandlingCorrect = false;
    }
}
