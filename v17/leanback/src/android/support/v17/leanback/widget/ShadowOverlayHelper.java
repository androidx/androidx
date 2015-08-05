/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v17.leanback.R;
import android.support.v17.leanback.system.Settings;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.View;


/**
 * ShadowOverlayHelper is a helper class for shadow, overlay and rounded corner.
 * Initialize it with all the options and it decides the best strategy.
 * <li> For shadow:  it may use 9-patch with opticalBounds or Z-value based shadow for
 *                   API >= 21.  When 9-patch is used, it requires a ShadowOverlayContainer
 *                   created by ItemBridgeAdapter.Wrapper to include 9-patch views.
 * <li> For overlay: it may use ShadowOverlayContainer which overrides draw() or it may
 *                   use setForeground(new ColorDrawable()) for API>=23.  The foreground support
 *                   might be disabled if rounded corner is applied due to performance reason.
 * <li> For rounded-corner:  it uses a ViewOutlineProvider for API>=21.
 * @hide
 */
public final class ShadowOverlayHelper {

    /**
     * No shadow.
     */
    public static final int SHADOW_NONE = 1;

    /**
     * Shadows are fixed.
     */
    public static final int SHADOW_STATIC = 2;

    /**
     * Shadows depend on the size, shape, and position of the view.
     */
    public static final int SHADOW_DYNAMIC = 3;

    int mShadowType = SHADOW_NONE;
    boolean mNeedsOverlay;
    boolean mNeedsRoundedCorner;
    boolean mNeedsShadow;
    boolean mNeedsWrapper;
    private ItemBridgeAdapter.Wrapper mCardWrapper;

    float mUnfocusedZ;
    float mFocusedZ;

    /**
     * Return true if the platform sdk supports shadow.
     */
    public static boolean supportsShadow() {
        return StaticShadowHelper.getInstance().supportsShadow();
    }

    /**
     * Returns true if the platform sdk supports dynamic shadows.
     */
    public static boolean supportsDynamicShadow() {
        return ShadowHelper.getInstance().supportsDynamicShadow();
    }

    /**
     * Returns true if the platform sdk supports rounded corner through outline.
     */
    public static boolean supportsRoundedCorner() {
        return RoundedRectHelper.supportsRoundedCorner();
    }

    /**
     * Returns true if view.setForeground() is supported.
     */
    public static boolean supportsForeground() {
        return ForegroundHelper.supportsForeground();
    }

    /**
     * Create ShadowHelper that includes all options.
     *
     * @param context               Context that required to query options
     * @param needsOverlay          true if overlay (dim) is needed
     * @param needsShadow           true if shadow is needed
     * @param needsRoundedCorner    true if roundedCorner is needed.
     * @param preferZOrder          true if prefer dynamic shadow otherwise static shadow is used.
     */
    public ShadowOverlayHelper(Context context,
            boolean needsOverlay, boolean needsShadow, boolean needsRoundedCorner,
            boolean preferZOrder) {
        mNeedsOverlay = needsOverlay;
        mNeedsRoundedCorner = needsRoundedCorner && supportsRoundedCorner();
        mNeedsShadow = needsShadow && supportsShadow();

        // Force to use wrapper to avoid rebuild rounded corner outline on animating foreground
        // drawable.  See b/22724385
        final boolean forceWrapperForOverlay = mNeedsRoundedCorner;

        // figure out shadow type and if we need use wrapper:
        if (mNeedsShadow) {
            // if static shadow is prefered or dynamic shadow is not supported,
            // use static shadow,  otherwise use dynamic shadow.
            if (!preferZOrder || !supportsDynamicShadow()) {
                mShadowType = SHADOW_STATIC;
                // static shadow requires ShadowOverlayContainer to support crossfading
                // of two shadow views.
                mNeedsWrapper = true;
            } else {
                useDynamicShadow(context);
                mNeedsWrapper = ((!supportsForeground() || forceWrapperForOverlay) && mNeedsOverlay);
            }
        } else {
            mShadowType = SHADOW_NONE;
            mNeedsWrapper = ((!supportsForeground() || forceWrapperForOverlay) && mNeedsOverlay);
        }

        if (mNeedsWrapper) {
            mCardWrapper = new ItemBridgeAdapter.Wrapper() {
                @Override
                public View createWrapper(View root) {
                    Context context = root.getContext();
                    ShadowOverlayContainer wrapper = createShadowOverlayContainer(context);
                    wrapper.setLayoutParams(
                            new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
                    return wrapper;
                }
                @Override
                public void wrap(View wrapper, View wrapped) {
                    ((ShadowOverlayContainer) wrapper).wrap(wrapped);
                }
            };
        }
    }

    /**
     * {@link #prepareParentForShadow(ViewGroup)} must be called on parent of container
     * before using shadow.  Depending on Shadow type, optical bounds might be applied.
     */
    public void prepareParentForShadow(ViewGroup parent) {
        if (mShadowType == SHADOW_STATIC) {
            StaticShadowHelper.getInstance().prepareParent(parent);
        }
    }

    void useDynamicShadow(Context context) {
        Resources res = context.getResources();
        useDynamicShadow(res.getDimension(R.dimen.lb_material_shadow_normal_z),
                res.getDimension(R.dimen.lb_material_shadow_focused_z));
    }

    void useDynamicShadow(float unfocusedZ, float focusedZ) {
        mShadowType = SHADOW_DYNAMIC;
        mUnfocusedZ = unfocusedZ;
        mFocusedZ = focusedZ;
    }

    public int getShadowType() {
        return mShadowType;
    }

    public boolean needsOverlay() {
        return mNeedsOverlay;
    }

    public boolean needsRoundedCorner() {
        return mNeedsRoundedCorner;
    }

    ShadowOverlayContainer createShadowOverlayContainer(Context context) {
        return new ShadowOverlayContainer(context, mShadowType, mNeedsOverlay,
                mNeedsRoundedCorner);
    }

    public ItemBridgeAdapter.Wrapper getWrapper() {
        return mCardWrapper;
    }

    /**
     * Set foreground color for view other than ShadowOverlayContainer.
     */
    public static void setForegroundColor(View view, int color) {
        Drawable d = ForegroundHelper.getInstance().getForeground(view);
        if (d instanceof ColorDrawable) {
            ((ColorDrawable) d).setColor(color);
        } else {
            ForegroundHelper.getInstance().setForeground(view, new ColorDrawable(color));
        }
    }

    /**
     * Called on view is created.
     * @param view
     */
    public void onViewCreated(View view) {
        if (!mNeedsWrapper) {
            if (!mNeedsShadow) {
                if (mNeedsRoundedCorner) {
                    RoundedRectHelper.getInstance().setClipToRoundedOutline(view, true);
                }
            } else {
                if (mShadowType == SHADOW_DYNAMIC) {
                    Object tag = ShadowHelper.getInstance().addDynamicShadow(
                            view, mUnfocusedZ, mFocusedZ, mNeedsRoundedCorner);
                    view.setTag(R.id.lb_shadow_impl, tag);
                }
            }
        }
    }

    public static Object getNoneWrapperDyamicShadowImpl(View view) {
        return view.getTag(R.id.lb_shadow_impl);
    }

    /**
     * Set shadow focus level (0 to 1). 0 for unfocused, 1f for fully focused.
     * This is for view other than ShadowOverlayContainer.
     */
    public static void setShadowFocusLevel(View view, float level) {
        setShadowFocusLevel(getNoneWrapperDyamicShadowImpl(view), SHADOW_DYNAMIC, level);
    }

    static void setShadowFocusLevel(Object impl, int shadowType, float level) {
        if (impl != null) {
            if (level < 0f) {
                level = 0f;
            } else if (level > 1f) {
                level = 1f;
            }
            switch (shadowType) {
                case SHADOW_DYNAMIC:
                    ShadowHelper.getInstance().setShadowFocusLevel(impl, level);
                    break;
                case SHADOW_STATIC:
                    StaticShadowHelper.getInstance().setShadowFocusLevel(impl, level);
                    break;
            }
        }
    }
}
