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

package androidx.xr.scenecore.spatial.core;

import androidx.xr.scenecore.runtime.Dimensions;
import androidx.xr.scenecore.runtime.Entity;
import androidx.xr.scenecore.runtime.PanelEntity;
import androidx.xr.scenecore.runtime.ResizableComponent;
import androidx.xr.scenecore.runtime.ResizeEvent;
import androidx.xr.scenecore.runtime.ResizeEventListener;
import androidx.xr.scenecore.runtime.SurfaceEntity;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.function.Consumer;
import com.android.extensions.xr.node.NodeTransaction;
import com.android.extensions.xr.node.ReformEvent;
import com.android.extensions.xr.node.ReformOptions;
import com.android.extensions.xr.node.Vec3;

import org.jspecify.annotations.NonNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/** Implementation of ResizableComponent. */
@SuppressWarnings({"BanConcurrentHashMap"})
class ResizableComponentImpl implements ResizableComponent {
    private static final @NonNull Dimensions DIMS_ZERO = new Dimensions(0f, 0f, 0f);
    private static final @NonNull Dimensions DIMS_ONE = new Dimensions(1f, 1f, 1f);
    private static final @NonNull Dimensions DIMS_INF =
            new Dimensions(
                    Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
    private final XrExtensions mExtensions;
    private final ExecutorService mExecutor;
    private final ConcurrentHashMap<ResizeEventListener, Executor> mResizeEventListenerMap =
            new ConcurrentHashMap<>();
    private final AtomicBoolean mIsContentHidden = new AtomicBoolean(false);
    // Visible for testing.
    Consumer<ReformEvent> mReformEventConsumer;
    private Entity mEntity = null;
    private Dimensions mCurrentSize = null;
    private @NonNull Dimensions mMinSize;
    private @NonNull Dimensions mMaxSize;
    private float mFixedAspectRatio = 0.0f;
    private boolean mAutoHideContent = true;
    private boolean mAutoUpdateSize = true;
    private boolean mForceShowResizeOverlay = false;

    /**
     * The constructor sanitizes the provided minimum and maximum sizes to ensure they are valid.
     * Any negative values in {@code minSize} and {@code maxSize} are clamped to 0. Any {@code
     * Float.NaN} values are replaced with 0 for {@code minSize} and {@link Float#POSITIVE_INFINITY}
     * for {@code maxSize}.
     *
     * <p>Furthermore, it ensures that the maximum size is always greater than or equal to the
     * minimum size for each dimension. If any dimension of the provided {@code maxSize} is smaller
     * than the corresponding dimension of {@code minSize} after sanitization, that dimension of the
     * maximum size will be adjusted to be equal to the minimum size's dimension.
     */
    ResizableComponentImpl(
            ExecutorService executor,
            XrExtensions extensions,
            @NonNull Dimensions minSize,
            @NonNull Dimensions maxSize) {
        mMinSize = dimsClampPositive(minSize, DIMS_ZERO);
        mMaxSize = dimsClampPositive(maxSize, DIMS_INF);
        if (dimsAnyLessThen(mMaxSize, mMinSize)) {
            mMaxSize = dimsMax(mMinSize, mMaxSize);
        }
        mExtensions = extensions;
        mExecutor = executor;
    }

    private static @NonNull Dimensions dimsClampPositive(
            @NonNull Dimensions dimValue, @NonNull Dimensions nanFallback) {
        return new Dimensions(
                clampPositive(dimValue.width, nanFallback.width),
                clampPositive(dimValue.height, nanFallback.height),
                clampPositive(dimValue.depth, nanFallback.depth));
    }

    private static @NonNull Dimensions dimsClamp(
            @NonNull Dimensions dimValue,
            @NonNull Dimensions min,
            @NonNull Dimensions max,
            @NonNull Dimensions nanFallback) {
        return new Dimensions(
                clamp(dimValue.width, min.width, max.width, nanFallback.width),
                clamp(dimValue.height, min.height, max.height, nanFallback.height),
                clamp(dimValue.depth, min.depth, max.depth, nanFallback.depth));
    }

    private static @NonNull Dimensions dimsMin(@NonNull Dimensions a, @NonNull Dimensions b) {
        return new Dimensions(
                Math.min(a.width, b.width),
                Math.min(a.height, b.height),
                Math.min(a.depth, b.depth));
    }

    private static @NonNull Dimensions dimsMax(@NonNull Dimensions a, @NonNull Dimensions b) {
        return new Dimensions(
                Math.max(a.width, b.width),
                Math.max(a.height, b.height),
                Math.max(a.depth, b.depth));
    }

    private static boolean dimsAnyLessThen(@NonNull Dimensions a, @NonNull Dimensions b) {
        return a.width < b.width || a.height < b.height || a.depth < b.depth;
    }

    private static @NonNull Vec3 dimsToVec3(@NonNull Dimensions value) {
        return new Vec3(value.width, value.height, value.depth);
    }

    private static @NonNull Dimensions vec3ToDims(Vec3 value) {
        if (value == null) {
            return new Dimensions(Float.NaN, Float.NaN, Float.NaN);
        }
        return new Dimensions(value.x, value.y, value.z);
    }

    private static float clampPositive(float value, float nanReplace) {
        return clamp(value, 0f, Float.POSITIVE_INFINITY, nanReplace);
    }

    private static float clamp(float value, float min, float max, float nanFallback) {
        if (Float.isNaN(value)) value = nanFallback;
        if (value < min) return min;
        return Math.min(value, max);
    }

    @Override
    public boolean onAttach(@NonNull Entity entity) {
        if (mEntity != null) {
            return false;
        }

        Dimensions entitySize = null;
        if (entity instanceof PanelEntity) {
            entitySize = ((PanelEntity) entity).getSize();
        } else if (entity instanceof SurfaceEntity) {
            SurfaceEntity.Shape shape = ((SurfaceEntity) entity).getShape();
            if (shape instanceof SurfaceEntity.Shape.Quad) {
                entitySize = shape.getDimensions();
            }
        }

        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() | ReformOptions.ALLOW_RESIZE);

        if (entitySize != null) {
            updateAndSanitizeCurrentSize(entitySize);
            reformOptions.setCurrentSize(dimsToVec3(mCurrentSize));
        }

        reformOptions
                .setMinimumSize(dimsToVec3(mMinSize))
                .setMaximumSize(dimsToVec3(mMaxSize))
                .setFixedAspectRatio(mFixedAspectRatio)
                .setForceShowResizeOverlay(mForceShowResizeOverlay);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) entity).addReformEventConsumer(mReformEventConsumer, mExecutor);
        }

        mEntity = entity;
        return true;
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        restoreEntityContent();
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        reformOptions.setEnabledReform(
                reformOptions.getEnabledReform() & ~ReformOptions.ALLOW_RESIZE);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) entity).removeReformEventConsumer(mReformEventConsumer);
        }
        mEntity = null;
    }

    @NonNull
    @Override
    public Dimensions getSize() {
        if (mCurrentSize == null) {
            return DIMS_ONE;
        }
        return mCurrentSize;
    }

    @Override
    public void setSize(@NonNull Dimensions size) {
        // TODO: b/350821054 - Implement synchronization policy around Entity/Component updates.
        boolean outOfDate = updateAndSanitizeCurrentSize(size);
        if (!outOfDate || mEntity == null) {
            return;
        }

        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        reformOptions.setCurrentSize(dimsToVec3(mCurrentSize));
        if (mFixedAspectRatio != 0) {
            reformOptions.setFixedAspectRatio(mFixedAspectRatio);
        }
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    /**
     * Sanitizes and sets the internal current size.
     *
     * <p>Any {@code NaN} dimension in the input is replaced with the corresponding value from the
     * existing component size. Also updates the fixed aspect ratio if enabled.
     *
     * @param size The new dimensions to set.
     */
    private boolean updateAndSanitizeCurrentSize(@NonNull Dimensions size) {
        size = dimsClampPositive(size, getSize());

        if (size.equals(mCurrentSize)) {
            return false;
        }

        mCurrentSize = size;
        // Update the fixed aspect ratio if it is enabled.
        if (mFixedAspectRatio != 0) {
            updateFixedAspectRatio(true);
        }
        return true;
    }

    @Override
    public @NonNull Dimensions getMinimumSize() {
        return mMinSize;
    }

    @Override
    public void setMinimumSize(@NonNull Dimensions minSize) {
        minSize = dimsClampPositive(minSize, mMinSize);
        boolean updateMin = false;
        if (!mMinSize.equals(minSize)) {
            mMinSize = minSize;
            updateMin = true;
        }

        boolean updateMax = false;
        if (updateMin && dimsAnyLessThen(mMaxSize, mMinSize)) {
            mMaxSize = dimsMax(mMaxSize, mMinSize);
            updateMax = true;
        }

        if (mEntity == null) {
            return;
        }

        if (updateMin) {
            ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
            reformOptions.setMinimumSize(dimsToVec3(mMinSize));
            if (updateMax) {
                reformOptions.setMaximumSize(dimsToVec3(mMaxSize));
            }
            ((AndroidXrEntity) mEntity).updateReformOptions();
        }
    }

    @Override
    public @NonNull Dimensions getMaximumSize() {
        return mMaxSize;
    }

    @Override
    public void setMaximumSize(@NonNull Dimensions maxSize) {
        maxSize = dimsClampPositive(maxSize, mMaxSize);
        boolean updateMax = false;
        if (!mMaxSize.equals(maxSize)) {
            mMaxSize = maxSize;
            updateMax = true;
        }

        boolean updateMin = false;
        if (updateMax && dimsAnyLessThen(mMaxSize, mMinSize)) {
            mMinSize = dimsMin(mMaxSize, mMinSize);
            updateMin = true;
        }

        if (mEntity == null) {
            return;
        }

        if (updateMax) {
            ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
            reformOptions.setMaximumSize(dimsToVec3(mMaxSize));
            if (updateMin) {
                reformOptions.setMinimumSize(dimsToVec3(mMinSize));
            }
            ((AndroidXrEntity) mEntity).updateReformOptions();
        }
    }

    @Override
    public boolean isFixedAspectRatioEnabled() {
        return mFixedAspectRatio != 0;
    }

    @Override
    public void setFixedAspectRatioEnabled(boolean fixedAspectRatioEnabled) {
        float initialFixedAspectRatio = mFixedAspectRatio;
        updateFixedAspectRatio(fixedAspectRatioEnabled);
        // Return early if there was no update.
        if (mFixedAspectRatio == initialFixedAspectRatio) {
            return;
        }
        if (mEntity == null) {
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        reformOptions.setFixedAspectRatio(mFixedAspectRatio);
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    private void updateFixedAspectRatio(boolean fixedAspectRatioEnabled) {
        float updatedFixedAspectRatio = 0f;
        // Update the fixed aspect ratio based on the current size, or the default size if no
        // current size was set.
        if (fixedAspectRatioEnabled) {
            Dimensions size = getSize();
            updatedFixedAspectRatio = size.width / size.height;
        }
        mFixedAspectRatio = updatedFixedAspectRatio;
    }

    @Override
    public boolean getAutoHideContent() {
        return mAutoHideContent;
    }

    @Override
    public void setAutoHideContent(boolean autoHideContent) {
        mAutoHideContent = autoHideContent;
    }

    @Override
    public boolean getAutoUpdateSize() {
        return mAutoUpdateSize;
    }

    @Override
    public void setAutoUpdateSize(boolean autoUpdateSize) {
        mAutoUpdateSize = autoUpdateSize;
    }

    @Override
    public boolean getForceShowResizeOverlay() {
        return mForceShowResizeOverlay;
    }

    @Override
    public void setForceShowResizeOverlay(boolean show) {
        mForceShowResizeOverlay = show;
        if (mEntity == null) {
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        reformOptions.setForceShowResizeOverlay(show);
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    private void hideEntityContent() {
        // Return early if the entity content is already hidden.
        if (mIsContentHidden.get()) {
            return;
        }
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAlpha(((AndroidXrEntity) mEntity).getNode(), 0f).apply();
            mIsContentHidden.set(true);
        }
    }

    private void restoreEntityContent() {
        // Return early if the entity content is already visible.
        if (!mIsContentHidden.get()) {
            return;
        }
        try (NodeTransaction transaction = mExtensions.createNodeTransaction()) {
            transaction.setAlpha(((AndroidXrEntity) mEntity).getNode(), mEntity.getAlpha()).apply();
            mIsContentHidden.set(false);
        }
    }

    private void reformEventConsumer(ReformEvent reformEvent) {
        if (reformEvent.getType() != ReformEvent.REFORM_TYPE_RESIZE) {
            if (mIsContentHidden.get()) {
                restoreEntityContent();
            }
            return;
        }
        Dimensions proposedSize =
                dimsClamp(vec3ToDims(reformEvent.getProposedSize()), mMinSize, mMaxSize, getSize());
        if (mAutoUpdateSize) {
            // Update the resize affordance size.
            setSize(proposedSize);
        }

        BiConsumer<ResizeEventListener, Executor> resizeEventListenerAction =
                (listener, listenerExecutor) ->
                        listenerExecutor.execute(
                                () -> {
                                    int reformState = reformEvent.getState();
                                    if (mAutoHideContent
                                            && reformState != ReformEvent.REFORM_STATE_END) {
                                        // Set the alpha to 0 when the resize is active before any
                                        // app callbacks, and restore when the resize ends after any
                                        // app callbacks, to hide the entity content while it's
                                        // being resized.
                                        hideEntityContent();
                                    }
                                    listener.onResizeEvent(
                                            new ResizeEvent(
                                                    RuntimeUtils.getResizeEventState(
                                                            reformEvent.getState()),
                                                    proposedSize));
                                    if (mAutoHideContent
                                            && reformState == ReformEvent.REFORM_STATE_END) {
                                        // Restore the entity alpha to its original value after the
                                        // resize callback. We can't guarantee that the app has
                                        // finished resizing when this is called, since the panel
                                        // resize itself is asynchronous, or the app can use this
                                        // callback to schedule resize call on a different thread.
                                        restoreEntityContent();
                                    }
                                });
        mResizeEventListenerMap.forEach(resizeEventListenerAction);
    }

    @Override
    public void addResizeEventListener(
            @NonNull Executor resizeExecutor, @NonNull ResizeEventListener resizeEventListener) {
        mResizeEventListenerMap.put(resizeEventListener, resizeExecutor);
        if (mReformEventConsumer != null) {
            return;
        }
        mReformEventConsumer = this::reformEventConsumer;
        if (mEntity == null) {
            return;
        }
        ((AndroidXrEntity) mEntity).addReformEventConsumer(mReformEventConsumer, mExecutor);
    }

    @Override
    public void removeResizeEventListener(@NonNull ResizeEventListener resizeEventListener) {
        mResizeEventListenerMap.remove(resizeEventListener);
        if (mResizeEventListenerMap.isEmpty()) {
            ((AndroidXrEntity) mEntity).removeReformEventConsumer(mReformEventConsumer);
        }
    }
}
