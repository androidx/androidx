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
 * limitations under the License
 */
package android.support.v17.leanback.widget;

import android.os.Build;
import android.view.View;
import android.view.ViewGroup;


/**
 * Helper for static (nine patch) shadows.
 */
final class StaticShadowHelper {

    final static StaticShadowHelper sInstance = new StaticShadowHelper();
    boolean mSupportsShadow;
    ShadowHelperVersionImpl mImpl;

    /**
     * Interface implemented by classes that support Shadow.
     */
    static interface ShadowHelperVersionImpl {
        public void prepareParent(ViewGroup parent);
        public Object addStaticShadow(ViewGroup shadowContainer, boolean roundedCorners);
        public void setShadowFocusLevel(Object impl, float level);
    }

    /**
     * Interface used when we do not support Shadow animations.
     */
    private static final class ShadowHelperStubImpl implements ShadowHelperVersionImpl {
        @Override
        public void prepareParent(ViewGroup parent) {
            // do nothing
        }

        @Override
        public Object addStaticShadow(ViewGroup shadowContainer, boolean roundedCorners) {
            // do nothing
            return null;
        }

        @Override
        public void setShadowFocusLevel(Object impl, float level) {
            // do nothing
        }
    }

    /**
     * Implementation used on JBMR2 (and above).
     */
    private static final class ShadowHelperJbmr2Impl implements ShadowHelperVersionImpl {
        @Override
        public void prepareParent(ViewGroup parent) {
            ShadowHelperJbmr2.prepareParent(parent);
        }

        @Override
        public Object addStaticShadow(ViewGroup shadowContainer, boolean roundedCorners) {
            // Static shadows are always rounded
            return ShadowHelperJbmr2.addShadow(shadowContainer);
        }

        @Override
        public void setShadowFocusLevel(Object impl, float level) {
            ShadowHelperJbmr2.setShadowFocusLevel(impl, level);
        }
    }

    /**
     * Returns the StaticShadowHelper.
     */
    private StaticShadowHelper() {
        if (Build.VERSION.SDK_INT >= 18) {
            mSupportsShadow = true;
            mImpl = new ShadowHelperJbmr2Impl();
        } else {
            mSupportsShadow = false;
            mImpl = new ShadowHelperStubImpl();
        }
    }

    public static StaticShadowHelper getInstance() {
        return sInstance;
    }

    public boolean supportsShadow() {
        return mSupportsShadow;
    }

    public void prepareParent(ViewGroup parent) {
        mImpl.prepareParent(parent);
    }

    public Object addStaticShadow(ViewGroup shadowContainer, boolean roundedCorners) {
        return mImpl.addStaticShadow(shadowContainer, roundedCorners);
    }

    public void setShadowFocusLevel(Object impl, float level) {
        mImpl.setShadowFocusLevel(impl, level);
    }
}
