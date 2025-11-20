/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.view.insets;

import static androidx.core.view.WindowInsetsCompat.Side.BOTTOM;
import static androidx.core.view.WindowInsetsCompat.Side.LEFT;
import static androidx.core.view.WindowInsetsCompat.Side.RIGHT;
import static androidx.core.view.WindowInsetsCompat.Side.TOP;

import android.graphics.RectF;

import androidx.core.graphics.Insets;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A group of {@link Protection}s.
 *
 * <p>The protections in this group are ordered by z-order. The protection at the end of the list
 * has the highest z-order.
 * <p>If there are adjacent protections in the same group, up to only one protection can occupy the
 * sharing corner.
 *
 * @see Protection#occupiesCorners()
 */
class ProtectionGroup implements SystemBarStateMonitor.Callback {

    private final ArrayList<Protection> mProtections = new ArrayList<>();
    private final SystemBarStateMonitor mMonitor;
    private Insets mInsets = Insets.NONE;
    private Insets mInsetsIgnoringVisibility = Insets.NONE;
    private int mAnimationCount;
    private boolean mDisposed;

    ProtectionGroup(SystemBarStateMonitor monitor, List<Protection> protections) {
        addProtections(protections, false /* occupiesCorners */);
        addProtections(protections, true /* occupiesCorners */);
        monitor.addCallback(this);
        mMonitor = monitor;
    }

    private void addProtections(List<Protection> protections, boolean occupiesCorners) {
        final int size = protections.size();
        for (int i = 0; i < size; i++) {
            final Protection protection = protections.get(i);
            if (protection.occupiesCorners() != occupiesCorners) {
                continue;
            }
            final Object controller = protection.getController();
            if (controller == null) {
                protection.setController(this);
                mProtections.add(protection);
            } else {
                throw new IllegalStateException(
                        protection + " (" + (i + 1) + "/" + size + ") is already controlled by "
                                + controller + " but is still added to " + this);
            }
        }
    }

    private void updateInsets() {
        Insets consumed = Insets.NONE;
        for (int i = mProtections.size() - 1; i >= 0; i--) {
            final Protection protection = mProtections.get(i);
            consumed = Insets.max(consumed, protection.dispatchInsets(
                    mInsets, mInsetsIgnoringVisibility, consumed));
        }
    }

    @Override
    public void onInsetsChanged(Insets insets, Insets insetsIgnoringVisibility) {
        mInsets = insets;
        mInsetsIgnoringVisibility = insetsIgnoringVisibility;
        updateInsets();
    }

    @Override
    public void onColorHintChanged(int color) {
        for (int i = mProtections.size() - 1; i >= 0; i--) {
            mProtections.get(i).dispatchColorHint(color);
        }
    }

    @Override
    public void onAnimationStart() {
        mAnimationCount++;
    }

    @Override
    public void onAnimationProgress(int sides, Insets insets, RectF alpha) {
        final Insets insetsStable = mInsetsIgnoringVisibility;
        for (int i = mProtections.size() - 1; i >= 0; i--) {
            final Protection protection = mProtections.get(i);
            final int side = protection.getSide();
            if ((side & sides) == 0) {
                continue;
            }
            protection.setSystemVisible(true);
            switch (side) {
                case LEFT:
                    if (insetsStable.left > 0) {
                        protection.setSystemInsetAmount(
                                insets.left / (float) insetsStable.left);
                    }
                    protection.setSystemAlpha(alpha.left);
                    break;
                case TOP:
                    if (insetsStable.top > 0) {
                        protection.setSystemInsetAmount(
                                insets.top / (float) insetsStable.top);
                    }
                    protection.setSystemAlpha(alpha.top);
                    break;
                case RIGHT:
                    if (insetsStable.right > 0) {
                        protection.setSystemInsetAmount(
                                insets.right / (float) insetsStable.right);
                    }
                    protection.setSystemAlpha(alpha.right);
                    break;
                case BOTTOM:
                    if (insetsStable.bottom > 0) {
                        protection.setSystemInsetAmount(
                                insets.bottom / (float) insetsStable.bottom);
                    }
                    protection.setSystemAlpha(alpha.bottom);
                    break;
            }
        }
    }

    @Override
    public void onAnimationEnd() {
        final boolean wasAnimating = mAnimationCount > 0;
        mAnimationCount--;
        if (wasAnimating && mAnimationCount == 0) {
            // The animating protections were forced to be visible. The call here
            // makes them update the visibility.
            updateInsets();
        }
    }

    /**
     * Returns the number of protections in this group.
     *
     * @return the number of protections in this group.
     */
    int size() {
        return mProtections.size();
    }

    /**
     * Returns the protection at the specified position in this group.
     *
     * @param index the index of the protection to return.
     * @return the protection at the specified position in this group.
     */
    @NonNull
    Protection getProtection(int index) {
        return mProtections.get(index);
    }

    /**
     * Disconnects from the given {@link SystemBarStateMonitor} and the {@link Protection}s.
     */
    void dispose() {
        if (mDisposed) {
            return;
        }
        mDisposed = true;
        mMonitor.removeCallback(this);
        for (int i = mProtections.size() - 1; i >= 0; i--) {
            mProtections.get(i).setController(null);
        }
        mProtections.clear();
    }
}
