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

import android.support.annotation.RequiresApi;
import android.support.v17.leanback.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

@RequiresApi(18)
class ShadowHelperJbmr2 {

    static class ShadowImpl {
        View mNormalShadow;
        View mFocusShadow;
    }

    /* prepare parent for allowing shadows of a child */
    public static void prepareParent(ViewGroup parent) {
        parent.setLayoutMode(ViewGroup.LAYOUT_MODE_OPTICAL_BOUNDS);
    }

    /* add shadows and return a implementation detail object */
    public static Object addShadow(ViewGroup shadowContainer) {
        shadowContainer.setLayoutMode(ViewGroup.LAYOUT_MODE_OPTICAL_BOUNDS);
        LayoutInflater inflater = LayoutInflater.from(shadowContainer.getContext());
        inflater.inflate(R.layout.lb_shadow, shadowContainer, true);
        ShadowImpl impl = new ShadowImpl();
        impl.mNormalShadow = shadowContainer.findViewById(R.id.lb_shadow_normal);
        impl.mFocusShadow = shadowContainer.findViewById(R.id.lb_shadow_focused);
        return impl;
    }

    /* set shadow focus level 0 for unfocused 1 for fully focused */
    public static void setShadowFocusLevel(Object impl, float level) {
        ShadowImpl shadowImpl = (ShadowImpl) impl;
        shadowImpl.mNormalShadow.setAlpha(1 - level);
        shadowImpl.mFocusShadow.setAlpha(level);
    }
}
