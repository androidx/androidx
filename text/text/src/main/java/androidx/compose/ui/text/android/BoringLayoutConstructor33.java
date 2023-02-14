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

package androidx.compose.ui.text.android;

import android.text.BoringLayout;
import android.text.Layout;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * Platform BoringLayout constructor has marked TextUtils.TruncateAt as NonNull even though it is
 * nullable, and have to be nullable.
 *
 * This class has the same signature of the BoringLayout constructor with only difference of
 * ellipsize is marked as Nullable.
 *
 * This was the only way to prevent compilation failure for nullability of TruncateAt ellipsize.
 *
 * See b/225695033
 */
@RequiresApi(33)
class BoringLayoutConstructor33 {

    private BoringLayoutConstructor33() {}

    @NonNull
    public static BoringLayout create(
            @NonNull CharSequence text,
            @NonNull TextPaint paint,
            @IntRange(from = 0) int width,
            @NonNull Layout.Alignment alignment,
            float lineSpacingMultiplier,
            float lineSpacingExtra,
            @NonNull BoringLayout.Metrics metrics,
            boolean includePadding,
            @Nullable TextUtils.TruncateAt ellipsize,
            @IntRange(from = 0) int ellipsizedWidth,
            boolean useFallbackLineSpacing
    ) {
        return new BoringLayout(
                text,
                paint,
                width,
                alignment,
                lineSpacingMultiplier,
                lineSpacingExtra,
                metrics,
                includePadding,
                ellipsize,
                ellipsizedWidth,
                useFallbackLineSpacing
        );
    }
}
