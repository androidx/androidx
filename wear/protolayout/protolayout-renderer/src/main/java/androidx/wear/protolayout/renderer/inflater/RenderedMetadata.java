/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.inflater;

import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.isDescendantOf;

import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import androidx.wear.protolayout.proto.FingerprintProto.TreeFingerprint;

import java.util.ArrayList;
import java.util.Map;

/** Additional metadata associated with a rendered layout. */
public final class RenderedMetadata {
    @NonNull private final TreeFingerprint mTreeFingerprint;
    @NonNull private final LayoutInfo mLayoutInfo;

    RenderedMetadata(@NonNull TreeFingerprint treeFingerprint, @NonNull LayoutInfo layoutInfo) {
        this.mTreeFingerprint = treeFingerprint;
        this.mLayoutInfo = layoutInfo;
    }

    @NonNull
    public TreeFingerprint getTreeFingerprint() {
        return mTreeFingerprint;
    }

    @NonNull
    LayoutInfo getLayoutInfo() {
        return mLayoutInfo;
    }

    /** This will hold information needed from an attached view while rendering in background. */
    static final class LayoutInfo {
        static final class Builder {
            @NonNull
            private final Map<String, ViewProperties> mPositionIdToViewProperties =
                    new ArrayMap<>();

            @NonNull
            private final ArrayList<String> mSubtreePosIdPendingRemoval = new ArrayList<>();

            @Nullable private final LayoutInfo mPreviousLayoutInfo;

            Builder(@Nullable LayoutInfo previousLayoutInfo) {
                this.mPreviousLayoutInfo = previousLayoutInfo;
            }

            void add(@NonNull String positionId, @NonNull ViewProperties viewProperties) {
                mPositionIdToViewProperties.put(positionId, viewProperties);
            }

            @Nullable
            ViewProperties getViewPropertiesFor(@NonNull String posId) {
                return mPositionIdToViewProperties.get(posId);
            }

            void removeSubtree(@NonNull String positionId) {
                mSubtreePosIdPendingRemoval.add(positionId);
            }

            @SuppressWarnings("RestrictTo")
            LayoutInfo build() {
                LayoutInfo layoutInfo =
                        new LayoutInfo(
                                mPreviousLayoutInfo != null
                                        ? mPreviousLayoutInfo.mPositionIdToLayoutInfo
                                        : new ArrayMap<>());
                for (String subtreePosId : mSubtreePosIdPendingRemoval) {
                    layoutInfo
                            .mPositionIdToLayoutInfo
                            .keySet()
                            .removeIf(s -> isDescendantOf(s, subtreePosId));
                }
                layoutInfo.mPositionIdToLayoutInfo.putAll(mPositionIdToViewProperties);
                return layoutInfo;
            }
        }

        @NonNull final Map<String, ViewProperties> mPositionIdToLayoutInfo;

        LayoutInfo(@NonNull Map<String, ViewProperties> positionIdToLayoutInfo) {
            this.mPositionIdToLayoutInfo = positionIdToLayoutInfo;
        }

        boolean contains(@NonNull String positionId) {
            return mPositionIdToLayoutInfo.containsKey(positionId);
        }

        @Nullable
        ViewProperties getViewPropertiesFor(@NonNull String positionId) {
            return mPositionIdToLayoutInfo.get(positionId);
        }
    }

    /**
     * Additional layout params that should be applied later when the target element is inflated.
     */
    interface PendingLayoutParams {
        /**
         * Apply the additional fields from this class to the {@code layoutParams}. This might lead
         * to creating a new instance of a subclass of {@link LayoutParams}
         */
        @NonNull
        LayoutParams apply(@NonNull LayoutParams layoutParams);
    }

    /** Additional pending layout params for layout elements inside a {@link FrameLayout}. */
    static final class PendingFrameLayoutParams implements PendingLayoutParams {
        private final int mGravity;

        PendingFrameLayoutParams(int gravity) {
            this.mGravity = gravity;
        }

        @Override
        @NonNull
        public LayoutParams apply(@NonNull LayoutParams layoutParams) {
            FrameLayout.LayoutParams frameLayoutParams;
            // Note: These conversions reflect the procedure in FrameLayout#generateLayoutParams
            // which applies to a View when it's attached to a FrameLayout container.
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                frameLayoutParams = (FrameLayout.LayoutParams) layoutParams;
            } else if (layoutParams instanceof MarginLayoutParams) {
                frameLayoutParams = new FrameLayout.LayoutParams((MarginLayoutParams) layoutParams);
            } else {
                frameLayoutParams = new FrameLayout.LayoutParams(layoutParams);
            }
            frameLayoutParams.gravity = mGravity;
            return frameLayoutParams;
        }
    }

    /** Holds properties of any container that are needed for background rendering. */
    static class ViewProperties {
        @NonNull static final ViewProperties EMPTY = new ViewProperties();
        /**
         * Creates a {@link ViewProperties} from any {@link ViewGroup} container. Note that {@code
         * rawLayoutParams} doesn't need to be a container-specific variant. But the {@code
         * childLayoutParams} should be of a container specific instance (if provided).
         */
        @NonNull
        static ViewProperties fromViewGroup(
                @NonNull ViewGroup viewGroup,
                @NonNull LayoutParams rawLayoutParams,
                @NonNull PendingLayoutParams childLayoutParams) {
            if (viewGroup instanceof LinearLayout) {
                return new LinearLayoutProperties(
                        ((LinearLayout) viewGroup).getOrientation(), rawLayoutParams);
            }
            if (viewGroup instanceof FrameLayout) {
                return new FrameLayoutProperties(childLayoutParams);
            }
            return EMPTY;
        }

        @NonNull
        LayoutParams applyPendingChildLayoutParams(@NonNull LayoutParams layoutParams) {
            // Do nothing.
            return layoutParams;
        }
    }

    /**
     * Holds properties of a {@link LinearLayout} container that are needed for background
     * rendering.
     */
    static final class LinearLayoutProperties extends ViewProperties {
        private final int mOrientation;
        @NonNull private final LayoutParams mRawLayoutParams;

        LinearLayoutProperties(int orientation, @NonNull LayoutParams rawLayoutParams) {
            this.mOrientation = orientation;
            this.mRawLayoutParams = rawLayoutParams;
        }

        int getOrientation() {
            return mOrientation;
        }

        /**
         * Returns the non-container specific {@link LayoutParams} for this object. Note that
         * down-casting this to {@link LinearLayout.LayoutParams} can fail.
         */
        @NonNull
        LayoutParams getRawLayoutParams() {
            return mRawLayoutParams;
        }
    }

    /**
     * Holds properties of a {@link FrameLayout} container that are needed for background rendering.
     */
    static final class FrameLayoutProperties extends ViewProperties {
        @NonNull private final PendingLayoutParams mChildLayoutParams;

        FrameLayoutProperties(@NonNull PendingLayoutParams childLayoutParams) {
            this.mChildLayoutParams = childLayoutParams;
        }

        @Override
        @NonNull
        LayoutParams applyPendingChildLayoutParams(@NonNull LayoutParams layoutParams) {
            return mChildLayoutParams.apply(layoutParams);
        }
    }
}
