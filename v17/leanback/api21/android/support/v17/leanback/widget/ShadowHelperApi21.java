/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.support.v17.leanback.R;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.view.View;
import android.view.ViewOutlineProvider;

class ShadowHelperApi21 {

    static int sNormalZ = Integer.MIN_VALUE;
    static int sFocusedZ;
    static final ViewOutlineProvider sOutlineProvider = new ViewOutlineProvider() {
        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, view.getWidth(), view.getHeight());
            outline.setAlpha(1.0f);
        }
    };

    private static void initializeResources(Resources res) {
        if (sNormalZ == Integer.MIN_VALUE) {
            sNormalZ = (int) res.getDimension(R.dimen.lb_material_shadow_normal_z);
            sFocusedZ = (int) res.getDimension(R.dimen.lb_material_shadow_focused_z);
        }
    }

    /* add shadows and return a implementation detail object */
    public static Object addShadow(ViewGroup shadowContainer, boolean roundedCorners) {
        initializeResources(shadowContainer.getResources());
        if (roundedCorners) {
            RoundedRectHelperApi21.setRoundedRectBackground(shadowContainer,
                    Color.TRANSPARENT);
        } else {
            shadowContainer.setOutlineProvider(sOutlineProvider);
        }
        shadowContainer.setZ(sNormalZ);
        return shadowContainer;
    }

    /* set shadow focus level 0 for unfocused 1 for fully focused */
    public static void setShadowFocusLevel(Object impl, float level) {
        ViewGroup shadowContainer = (ViewGroup) impl;
        shadowContainer.setZ(sNormalZ + level * (sFocusedZ - sNormalZ));
    }

    public static void setZ(View view, float z) {
        view.setZ(z);
    }
}
