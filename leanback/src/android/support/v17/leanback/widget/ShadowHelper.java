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

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;


/**
 * Helper for shadow.
 */
final class ShadowHelper {

    final static ShadowHelper sInstance = new ShadowHelper();
    boolean mSupportsDynamicShadow;
    ShadowHelperVersionImpl mImpl;

    /**
     * Interface implemented by classes that support Shadow.
     */
    static interface ShadowHelperVersionImpl {
        public Object addDynamicShadow(
                View shadowContainer, float unfocusedZ, float focusedZ, int roundedCornerRadius);
        public void setZ(View view, float z);
        public void setShadowFocusLevel(Object impl, float level);
    }

    /**
     * Interface used when we do not support Shadow animations.
     */
    private static final class ShadowHelperStubImpl implements ShadowHelperVersionImpl {
        ShadowHelperStubImpl() {
        }

        @Override
        public Object addDynamicShadow(
                View shadowContainer, float focusedZ, float unfocusedZ, int roundedCornerRadius) {
            // do nothing
            return null;
        }

        @Override
        public void setShadowFocusLevel(Object impl, float level) {
            // do nothing
        }

        @Override
        public void setZ(View view, float z) {
            // do nothing
        }
    }

    /**
     * Implementation used on api 21 (and above).
     */
    @RequiresApi(21)
    private static final class ShadowHelperApi21Impl implements ShadowHelperVersionImpl {
        ShadowHelperApi21Impl() {
        }

        @Override
        public Object addDynamicShadow(
                View shadowContainer, float unfocusedZ, float focusedZ, int roundedCornerRadius) {
            return ShadowHelperApi21.addDynamicShadow(
                    shadowContainer, unfocusedZ, focusedZ, roundedCornerRadius);
        }

        @Override
        public void setShadowFocusLevel(Object impl, float level) {
            ShadowHelperApi21.setShadowFocusLevel(impl, level);
        }

        @Override
        public void setZ(View view, float z) {
            ShadowHelperApi21.setZ(view, z);
        }
    }

    /**
     * Returns the ShadowHelper.
     */
    private ShadowHelper() {
        if (Build.VERSION.SDK_INT >= 21) {
            mSupportsDynamicShadow = true;
            mImpl = new ShadowHelperApi21Impl();
        } else {
            mImpl = new ShadowHelperStubImpl();
        }
    }

    public static ShadowHelper getInstance() {
        return sInstance;
    }

    public boolean supportsDynamicShadow() {
        return mSupportsDynamicShadow;
    }

    public Object addDynamicShadow(
            View shadowContainer, float unfocusedZ, float focusedZ, int roundedCornerRadius) {
        return mImpl.addDynamicShadow(shadowContainer, unfocusedZ, focusedZ, roundedCornerRadius);
    }

    public void setShadowFocusLevel(Object impl, float level) {
        mImpl.setShadowFocusLevel(impl, level);
    }

    /**
     * Set the view z coordinate.
     */
    public void setZ(View view, float z) {
        mImpl.setZ(view, z);
    }

}
