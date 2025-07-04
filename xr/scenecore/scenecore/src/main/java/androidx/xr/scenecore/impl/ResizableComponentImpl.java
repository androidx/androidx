/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.impl;

import android.util.Log;

import androidx.xr.runtime.internal.Dimensions;
import androidx.xr.runtime.internal.Entity;
import androidx.xr.runtime.internal.ResizableComponent;
import androidx.xr.runtime.internal.ResizeEvent;
import androidx.xr.runtime.internal.ResizeEventListener;

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

/** Implementation of ResizableComponent. */
@SuppressWarnings({"BanConcurrentHashMap"})
class ResizableComponentImpl implements ResizableComponent {

    private static final String TAG = "ResizableComponentImpl";

    private final XrExtensions mExtensions;
    private final ExecutorService mExecutor;
    private final ConcurrentHashMap<ResizeEventListener, Executor> mResizeEventListenerMap =
            new ConcurrentHashMap<>();
    // Visible for testing.
    Consumer<ReformEvent> mReformEventConsumer;
    private Entity mEntity = null;
    private Dimensions mCurrentSize = null;

    /** Follows scenecore/ResizableComponent.create document, the default minimum size is 0. */
    private final Dimensions mMinimumSize = new Dimensions(0.0f, 0.0f, 0.0f);

    @NonNull private Dimensions mMinSize = mMinimumSize;

    /** Follows scenecore/ResizableComponent.create document, the default maximum size is 10. */
    private final Dimensions mMaximumSize = new Dimensions(10.0f, 10.0f, 10.0f);

    @NonNull private Dimensions mMaxSize = mMaximumSize;
    private float mFixedAspectRatio = 0.0f;
    private boolean mAutoHideContent = true;
    private boolean mAutoUpdateSize = true;
    private boolean mForceShowResizeOverlay = false;
    private final Dimensions mMinimumValidSize = new Dimensions(0.01f, 0.01f, 0.0f);

    /**
     * Follows scenecore/ResizableComponent.size document, the default size is 1. If
     * ResizableComponent is attached to a panel, use the panel size as default size.
     */
    private Dimensions mDefaultSize = new Dimensions(1f, 1f, 1f);

    private Dimensions mLastValidSizeForResize = mDefaultSize;

    ResizableComponentImpl(
            ExecutorService executor,
            XrExtensions extensions,
            @NonNull Dimensions minSize,
            @NonNull Dimensions maxSize) {
        if (isSizeWellFormed(minSize)
                && isSizeWellFormed(maxSize)
                && minSize.width < maxSize.width
                && minSize.height < maxSize.height
                && minSize.depth <= maxSize.depth) { // allows min/max depth to be equal
            mMinSize = minSize;
            mMaxSize = maxSize;
        } // else use default initial value.
        mExtensions = extensions;
        mExecutor = executor;
    }

    @Override
    public boolean onAttach(@NonNull Entity entity) {
        if (mEntity != null) {
            Log.e(TAG, "Already attached to entity " + mEntity);
            return false;
        }
        mEntity = entity;
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        ReformOptions unused =
                reformOptions.setEnabledReform(
                        reformOptions.getEnabledReform() | ReformOptions.ALLOW_RESIZE);

        // Update the current size if it's not set.
        // TODO: b/348037292 - Remove this special case for PanelEntityImpl.
        if (entity instanceof PanelEntityImpl && mCurrentSize == null) {
            mCurrentSize = ((PanelEntityImpl) entity).getSize();
            // If the entity is a panel, use the panel size as default size.
            if (isSizeValid(mCurrentSize)) {
                mDefaultSize = mCurrentSize;
            }
            if (mCurrentSize.width < mMinSize.width
                    || mCurrentSize.width > mMaxSize.width
                    || mCurrentSize.height < mMinSize.height
                    || mCurrentSize.height > mMaxSize.height) {
                Log.e(TAG, "Size of attached panel entity is not within minsize and maxsize.");
                return false;
            }
        }
        if (isSizeWellFormed(mCurrentSize)) { // allows set (0, 0, 0) in onAttach
            unused =
                    reformOptions.setCurrentSize(
                            new Vec3(mCurrentSize.width, mCurrentSize.height, mCurrentSize.depth));
        }
        unused =
                reformOptions
                        .setMinimumSize(new Vec3(mMinSize.width, mMinSize.height, mMinSize.depth))
                        .setMaximumSize(new Vec3(mMaxSize.width, mMaxSize.height, mMaxSize.depth))
                        .setFixedAspectRatio(mFixedAspectRatio)
                        .setForceShowResizeOverlay(mForceShowResizeOverlay);
        ((AndroidXrEntity) entity).updateReformOptions();
        if (mReformEventConsumer != null) {
            ((AndroidXrEntity) entity).addReformEventConsumer(mReformEventConsumer, mExecutor);
        }
        return true;
    }

    @Override
    public void onDetach(@NonNull Entity entity) {
        ReformOptions reformOptions = ((AndroidXrEntity) entity).getReformOptions();
        ReformOptions unused =
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
            Log.e(TAG, "This component isn't attached to a PanelEntity and never called setSize.");
            return mDefaultSize;
        }
        return mCurrentSize;
    }

    @Override
    public void setSize(@NonNull Dimensions size) {
        // TODO: b/350821054 - Implement synchronization policy around Entity/Component updates.
        if (!isSizeValid(size)) {
            size = isSizeValid(mCurrentSize) ? mCurrentSize : mDefaultSize;
            // Even if the provided size is invalid, do not return. Instead, proceed with a
            // valid fallback size. This ensures that ReformOptions are always updated, which
            // is an expectation of the layout system (e.g., Compose) to prevent stale states.
        }
        mCurrentSize = size;
        if (mEntity == null) {
            Log.i(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        ReformOptions unused =
                reformOptions.setCurrentSize(new Vec3(size.width, size.height, size.depth));
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @NonNull
    @Override
    public Dimensions getMinimumSize() {
        return mMinSize;
    }

    @Override
    public void setMinimumSize(@NonNull Dimensions minSize) {
        if (!isMinSizeValid(minSize)) {
            minSize = isMinSizeValid(mMinSize) ? mMinSize : mMinimumSize;
            // Similar logic to setSize
        }
        mMinSize = minSize;
        if (mEntity == null) {
            Log.i(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        ReformOptions unused =
                reformOptions.setMinimumSize(
                        new Vec3(minSize.width, minSize.height, minSize.depth));
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @NonNull
    @Override
    public Dimensions getMaximumSize() {
        return mMaxSize;
    }

    @Override
    public void setMaximumSize(@NonNull Dimensions maxSize) {
        if (!isMaxSizeValid(maxSize)) {
            maxSize = isMaxSizeValid(mMaxSize) ? mMaxSize : mMaximumSize;
            // Similar logic to setSize
        }
        mMaxSize = maxSize;
        if (mEntity == null) {
            Log.i(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        ReformOptions unused =
                reformOptions.setMaximumSize(
                        new Vec3(maxSize.width, maxSize.height, maxSize.depth));
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @Override
    public float getFixedAspectRatio() {
        return mFixedAspectRatio;
    }

    @Override
    public void setFixedAspectRatio(float fixedAspectRatio) {
        mFixedAspectRatio = fixedAspectRatio;
        if (mEntity == null) {
            Log.i(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        ReformOptions unused = reformOptions.setFixedAspectRatio(fixedAspectRatio);
        ((AndroidXrEntity) mEntity).updateReformOptions();
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
            Log.i(TAG, "This component isn't attached to an entity.");
            return;
        }
        ReformOptions reformOptions = ((AndroidXrEntity) mEntity).getReformOptions();
        ReformOptions unused = reformOptions.setForceShowResizeOverlay(show);
        ((AndroidXrEntity) mEntity).updateReformOptions();
    }

    @Override
    public void addResizeEventListener(
            @NonNull Executor resizeExecutor, @NonNull ResizeEventListener resizeEventListener) {
        mResizeEventListenerMap.put(resizeEventListener, resizeExecutor);
        if (mReformEventConsumer != null) {
            return;
        }
        mReformEventConsumer =
                reformEvent -> {
                    if (reformEvent.getType() != ReformEvent.REFORM_TYPE_RESIZE) {
                        return;
                    }
                    Dimensions newSize =
                            getSanitizedSizeForResize(
                                    new Dimensions(
                                            reformEvent.getProposedSize().x,
                                            reformEvent.getProposedSize().y,
                                            reformEvent.getProposedSize().z));
                    if (mAutoUpdateSize) {
                        // Update the resize affordance size.
                        setSize(newSize);
                    }
                    mResizeEventListenerMap.forEach(
                            (listener, listenerExecutor) ->
                                    listenerExecutor.execute(
                                            () -> {
                                                // Set the alpha to 0 when the resize starts before
                                                // any app callbacks, and
                                                // restore when the resize ends after any app
                                                // callbacks, to hide the entity
                                                // content while it's being resized.
                                                int reformState = reformEvent.getState();
                                                if (mAutoHideContent
                                                        && reformState
                                                                == ReformEvent.REFORM_STATE_START) {
                                                    try (NodeTransaction transaction =
                                                            mExtensions.createNodeTransaction()) {
                                                        transaction
                                                                .setAlpha(
                                                                        ((AndroidXrEntity) mEntity)
                                                                                .getNode(),
                                                                        0f)
                                                                .apply();
                                                    }
                                                }
                                                listener.onResizeEvent(
                                                        new ResizeEvent(
                                                                RuntimeUtils.getResizeEventState(
                                                                        reformEvent.getState()),
                                                                newSize));
                                                if (mAutoHideContent
                                                        && reformState
                                                                == ReformEvent.REFORM_STATE_END) {
                                                    // Restore the entity alpha to its original
                                                    // value after the resize
                                                    // callback. We can't guarantee that the app has
                                                    // finished resizing when
                                                    // this is called, since the panel resize itself
                                                    // is asynchronous, or the
                                                    // app can use this callback to schedule resize
                                                    // call on a different
                                                    // thread.
                                                    try (NodeTransaction transaction =
                                                            mExtensions.createNodeTransaction()) {
                                                        transaction
                                                                .setAlpha(
                                                                        ((AndroidXrEntity) mEntity)
                                                                                .getNode(),
                                                                        mEntity.getAlpha())
                                                                .apply();
                                                    }
                                                }
                                            }));
                };
        if (mEntity == null) {
            Log.i(TAG, "This component isn't attached to an entity.");
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

    /**
     * Checks that the Dimensions object is not null, its values are not NaN, and it meets a minimum
     * system-level size.
     */
    private boolean isSizeWellFormed(Dimensions size) {
        return size != null
                && !Float.isNaN(size.width)
                && !Float.isNaN(size.height)
                && !Float.isNaN(size.depth)
                && size.width >= 0
                && size.height >= 0
                && size.depth >= 0;
    }

    /**
     * Checks if the given {@code Dimensions} object is a valid minimum size.
     *
     * <p>A minimum size is considered valid if it is well-formed (not null, not NaN, and
     * non-negative) and its dimensions are smaller than the current maximum size. Specifically, the
     * width and height must be strictly less than the maximum, while the depth can be less than or
     * equal.
     *
     * @param minSize The {@code Dimensions} to validate.
     * @return {@code true} if the provided minimum size is valid, {@code false} otherwise.
     */
    private boolean isMinSizeValid(Dimensions minSize) {
        return isSizeWellFormed(minSize)
                && minSize.width < mMaxSize.width
                && minSize.height < mMaxSize.height
                && minSize.depth <= mMaxSize.depth; // allows min/max depth to be equal
    }

    /**
     * Checks if the given {@code Dimensions} object is a valid maximum size.
     *
     * <p>A maximum size is considered valid if it is well-formed (not null, not NaN, and
     * non-negative) and its dimensions are larger than the current minimum size. Specifically, the
     * width and height must be strictly greater than the minimum, while the depth can be greater
     * than or equal.
     *
     * @param maxSize The {@code Dimensions} to validate.
     * @return {@code true} if the provided maximum size is valid, {@code false} otherwise.
     */
    private boolean isMaxSizeValid(Dimensions maxSize) {
        return isSizeWellFormed(maxSize)
                && maxSize.width > mMinSize.width
                && maxSize.height > mMinSize.height
                && maxSize.depth >= mMinSize.depth; // allows min/max depth to be equal
    }

    /**
     * Checks that the Dimensions object is well-formed and within the user-defined minimum and
     * maximum size bounds.
     */
    private boolean isSizeValid(Dimensions size) {
        return isSizeWellFormed(size)
                && size.width >= mMinimumValidSize.width
                && size.height >= mMinimumValidSize.height
                && size.depth >= mMinimumValidSize.depth
                && size.width >= mMinSize.width
                && size.height >= mMinSize.height
                && size.depth >= mMinSize.depth
                && size.width <= mMaxSize.width
                && size.height <= mMaxSize.height
                && size.depth <= mMaxSize.depth;
    }

    /**
     * Validates the incoming {@code size} and, if valid, caches it as the last known valid size.
     *
     * <p>This method always returns the last known valid size. This provides a stable, valid value
     * for resizing operations, even if the incoming size is invalid (e.g., outside the minimum or
     * maximum bounds).
     *
     * @param size The proposed new size to validate.
     * @return The last known valid size.
     */
    private Dimensions getSanitizedSizeForResize(Dimensions size) {
        if (isSizeValid(size)) {
            mLastValidSizeForResize = size;
        }
        return mLastValidSizeForResize;
    }
}
