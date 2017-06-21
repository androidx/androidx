/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.widget.ImageView;

class ImageViewCompatBase {
    static ColorStateList getImageTintList(ImageView view) {
        return (view instanceof TintableImageSourceView)
                ? ((TintableImageSourceView) view).getSupportImageTintList()
                : null;
    }

    static void setImageTintList(ImageView view, ColorStateList tintList) {
        if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintList(tintList);
        }
    }

    static PorterDuff.Mode getImageTintMode(ImageView view) {
        return (view instanceof TintableImageSourceView)
                ? ((TintableImageSourceView) view).getSupportImageTintMode()
                : null;
    }

    static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        if (view instanceof TintableImageSourceView) {
            ((TintableImageSourceView) view).setSupportImageTintMode(mode);
        }
    }
}
