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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

@RequiresApi(21)
class ImageViewCompatLollipop {
    static ColorStateList getImageTintList(ImageView view) {
        return view.getImageTintList();
    }

    static void setImageTintList(ImageView view, ColorStateList tintList) {
        view.setImageTintList(tintList);

        if (Build.VERSION.SDK_INT == 21) {
            // Work around a bug in L that did not update the state of the image source
            // after applying the tint
            Drawable imageViewDrawable = view.getDrawable();
            boolean hasTint = (view.getImageTintList() != null)
                    && (view.getImageTintMode() != null);
            if ((imageViewDrawable != null) && hasTint) {
                if (imageViewDrawable.isStateful()) {
                    imageViewDrawable.setState(view.getDrawableState());
                }
                view.setImageDrawable(imageViewDrawable);
            }
        }
    }

    static PorterDuff.Mode getImageTintMode(ImageView view) {
        return view.getImageTintMode();
    }

    static void setImageTintMode(ImageView view, PorterDuff.Mode mode) {
        view.setImageTintMode(mode);

        if (Build.VERSION.SDK_INT == 21) {
            // Work around a bug in L that did not update the state of the image source
            // after applying the tint
            Drawable imageViewDrawable = view.getDrawable();
            boolean hasTint = (view.getImageTintList() != null)
                    && (view.getImageTintMode() != null);
            if ((imageViewDrawable != null) && hasTint) {
                if (imageViewDrawable.isStateful()) {
                    imageViewDrawable.setState(view.getDrawableState());
                }
                view.setImageDrawable(imageViewDrawable);
            }
        }
    }
}
