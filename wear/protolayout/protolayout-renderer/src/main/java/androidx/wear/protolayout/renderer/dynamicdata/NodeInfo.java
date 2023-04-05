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

package androidx.wear.protolayout.renderer.dynamicdata;

import android.graphics.drawable.Animatable2.AnimationCallback;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.pipeline.BoundDynamicType;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest;
import androidx.wear.protolayout.expression.pipeline.QuotaManager;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.proto.ModifiersProto.AnimatedVisibility;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger.InnerCase;
import androidx.wear.protolayout.renderer.dynamicdata.PositionIdTree.TreeNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Information about a layout node that has multiple dynamic types or animators to it.
 *
 * <p>Note: this class is not thread-safe.
 */
class NodeInfo implements TreeNode {

    /** List of active bound dynamic types in the pipeline. */
    @NonNull private final List<BoundDynamicType> mActiveBoundTypes = new ArrayList<>();

    /** List of bound dynamic types that need to be evaluated. */
    @NonNull private List<BoundDynamicType> mPendingBoundTypes = Collections.emptyList();

    /** List of binding requests that failed to bind. */
    @NonNull
    private final List<DynamicTypeBindingRequest> mFailedBindingRequests = new ArrayList<>();

    @NonNull private final QuotaManager mAnimationQuotaManager;

    /** Set of animated image resources after they are resolved during inflation. */
    @NonNull private Set<ResolvedAvd> mResolvedAvds = Collections.emptySet();

    @NonNull private Set<ResolvedSeekableAvd> mResolvedSeekableAvds = Collections.emptySet();

    @Nullable private AnimatedVisibility mAnimatedVisibility = null;

    @NonNull private final String mPosId;

    NodeInfo(@NonNull String posId, @NonNull QuotaManager animationQuotaManager) {
        this.mPosId = posId;
        this.mAnimationQuotaManager = animationQuotaManager;
    }

    /**
     * Adds bound dynamic type returned by {@link
     * androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator#bind} to active and
     * pending types. It will stop being pending when {@link #initPendingBoundTypes()} is called.
     */
    void addBoundType(@NonNull BoundDynamicType boundType) {
        mActiveBoundTypes.add(boundType);
        addPendingEvaluationBoundType(boundType);
    }

    private void addPendingEvaluationBoundType(@NonNull BoundDynamicType boundTYpe) {
        if (mPendingBoundTypes.isEmpty()) {
            mPendingBoundTypes = new ArrayList<>();
        }
        mPendingBoundTypes.add(boundTYpe);
    }

    /**
     * Adds {@link DynamicTypeBindingRequest} that {@link
     * androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator} failed to bind. Failed
     * requests will be removed once a binding retry initiated by {@link
     * ProtoLayoutDynamicDataPipeline} succeed.
     */
    void addFailedBindingRequest(@NonNull DynamicTypeBindingRequest request) {
        mFailedBindingRequests.add(request);
    }

    /**
     * Initializes evaluation on all pending bound types, i.e. those added after the last {@link
     * #initPendingBoundTypes} call.
     */
    @UiThread
    void initPendingBoundTypes() {
        mPendingBoundTypes.forEach(BoundDynamicType::startEvaluation);
        mPendingBoundTypes.clear();
    }

    List<DynamicTypeBindingRequest> getFailedBindingRequest() {
        return mFailedBindingRequests;
    }

    @NonNull
    ResolvedAvd addResolvedAvd(@NonNull AnimatedVectorDrawable drawable, @NonNull Trigger trigger) {
        if (mResolvedAvds.isEmpty()) {
            mResolvedAvds = new ArraySet<>();
        }
        ResolvedAvd avd =
                new NodeInfo.ResolvedAvd(
                        drawable,
                        trigger,
                        new QuotaReleasingAnimationCallback(mAnimationQuotaManager));
        mResolvedAvds.add(avd);

        return avd;
    }

    void addResolvedSeekableAvd(@NonNull ResolvedSeekableAvd seekableAvd) {
        if (mResolvedSeekableAvds.isEmpty()) {
            mResolvedSeekableAvds = new ArraySet<>();
        }
        mResolvedSeekableAvds.add(seekableAvd);
    }

    @UiThread
    @Override
    public void destroy() {
        mActiveBoundTypes.forEach(BoundDynamicType::close);
        mResolvedAvds.forEach(ResolvedAvd::unregisterCallback);
    }

    /** Returns the number of active bound dynamic types. */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @SuppressWarnings("RestrictTo")
    int size() {
        return mActiveBoundTypes.stream().mapToInt(BoundDynamicType::getDynamicNodeCount).sum();
    }

    /** Play the animation with the given trigger type */
    @UiThread
    void playAvdAnimations(@NonNull InnerCase triggerCase) {
        for (ResolvedAvd entry : mResolvedAvds) {
            if (entry.mTrigger.getInnerCase() != triggerCase
                    || entry.mDrawable == null
                    || entry.mDrawable.isRunning()) {
                continue;
            }
            if ((triggerCase == InnerCase.ON_VISIBLE_ONCE_TRIGGER
                            || triggerCase == InnerCase.ON_LOAD_TRIGGER)
                    && entry.mPlayedAtLeastOnce) {
                continue;
            }
            if (!mAnimationQuotaManager.tryAcquireQuota(1)) {
                continue;
            }
            entry.startAnimation();
        }
    }

    /** Sets visibility of the animations. This also pauses or resumes animators. */
    @UiThread
    @SuppressWarnings("RestrictTo")
    void setVisibility(boolean visible) {
        for (ResolvedAvd entry : mResolvedAvds) {
            entry.mDrawable.setVisible(visible, /* restart= */ false);
        }
        for (ResolvedSeekableAvd entry : mResolvedSeekableAvds) {
            entry.mDrawable.setVisible(visible, /* restart= */ false);
        }
        mActiveBoundTypes.forEach(n -> n.setAnimationVisibility(visible));
    }

    /** Reset the avd animations with the given trigger type */
    @UiThread
    void resetAvdAnimations(@NonNull InnerCase triggerCase) {
        for (ResolvedAvd entry : mResolvedAvds) {
            if (entry.mTrigger.getInnerCase() == triggerCase && entry.mDrawable != null) {
                entry.mDrawable.reset();
            }
        }
    }

    /** Reset the avd animations with the given trigger type */
    @UiThread
    void stopAvdAnimations(@NonNull InnerCase triggerCase) {
        for (ResolvedAvd entry : mResolvedAvds) {
            if (entry.mDrawable.isRunning() && entry.mTrigger.getInnerCase() == triggerCase) {
                entry.mDrawable.stop();
                // We need to manually call the callback, as per Javadoc, callback is called later,
                // on a different thread, meaning that quota won't be released in time.
                entry.mCallback.onAnimationEnd(entry.mDrawable);
            }
        }
    }

    /**
     * Returns the total duration in milliseconds of the animated drawable associated with a
     * StateSource with the given key name; or null if no such SourceKey exists.
     */
    @Nullable
    Long getSeekableAnimationTotalDurationMillis(@NonNull String sourceKey) {
        for (ResolvedSeekableAvd resourceEntry : mResolvedSeekableAvds) {
            if (resourceEntry.hasStateSourceKey(sourceKey)) {
                return resourceEntry.mDrawable.getTotalDuration();
            }
        }
        return null;
    }

    /** Returns how many animations are running. */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @SuppressWarnings("RestrictTo")
    int getRunningAnimationCount() {
        return (int)
                (mActiveBoundTypes.stream()
                                .mapToInt(BoundDynamicType::getRunningAnimationCount)
                                .sum()
                        + mResolvedAvds.stream().filter(avd -> avd.mDrawable.isRunning()).count());
    }

    /** Returns how many expression nodes evaluated. */
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public int getExpressionNodesCount() {
        return mActiveBoundTypes.stream().mapToInt(BoundDynamicType::getDynamicNodeCount).sum();
    }

    /** Stores the {@link AnimatedVisibility} associated with this node. */
    void setAnimatedVisibility(@NonNull AnimatedVisibility animatedVisibility) {
        this.mAnimatedVisibility = animatedVisibility;
    }

    /**
     * Returns the {@link AnimatedVisibility} associated with this node. Returns null if no enter
     * animation is associated with this node.
     */
    @Nullable
    AnimatedVisibility getAnimatedVisibility() {
        return mAnimatedVisibility;
    }

    /** Returns the position Id for this node. */
    @NonNull
    String getPosId() {
        return mPosId;
    }

    static class ResolvedAvd {
        @NonNull final AnimatedVectorDrawable mDrawable;
        @NonNull final QuotaReleasingAnimationCallback mCallback;
        @NonNull final Trigger mTrigger;
        boolean mPlayedAtLeastOnce;

        ResolvedAvd(
                @NonNull AnimatedVectorDrawable drawable,
                @NonNull Trigger trigger,
                @NonNull QuotaReleasingAnimationCallback callback) {
            this.mDrawable = drawable;
            this.mCallback = callback;
            this.mTrigger = trigger;
            mPlayedAtLeastOnce = false;
            this.mDrawable.registerAnimationCallback(callback);
        }

        void unregisterCallback() {
            mDrawable.unregisterAnimationCallback(mCallback);
        }

        void startAnimation() {
            this.mDrawable.start();
            this.mCallback.mIsUsingQuota.set(true);
            this.mPlayedAtLeastOnce = true;
        }
    }

    static class ResolvedSeekableAvd {
        @NonNull final SeekableAnimatedVectorDrawable mDrawable;
        @NonNull final DynamicFloat mBoundProgress;

        ResolvedSeekableAvd(
                @NonNull SeekableAnimatedVectorDrawable drawable,
                @NonNull DynamicFloat boundProgress) {
            this.mDrawable = drawable;
            this.mBoundProgress = boundProgress;
        }

        boolean hasStateSourceKey(@NonNull String sourceKey) {
            return mBoundProgress.getStateSource().getSourceKey().equals(sourceKey);
        }
    }

    /** The callback used for AVD animations to release quota when the animation is finished. */
    private static final class QuotaReleasingAnimationCallback extends AnimationCallback {
        @NonNull private final QuotaManager mQuotaManager;

        @NonNull final AtomicBoolean mIsUsingQuota = new AtomicBoolean(false);

        QuotaReleasingAnimationCallback(@NonNull QuotaManager quotaManager) {
            this.mQuotaManager = quotaManager;
        }

        @Override
        public void onAnimationEnd(@NonNull Drawable drawable) {
            if (mIsUsingQuota.compareAndSet(true, false)) {
                mQuotaManager.releaseQuota(1);
            }
        }

        @Override
        public void onAnimationStart(@NonNull Drawable drawable) {}
    }

    @NonNull
    @Override
    public String toString() {
        return mPosId;
    }
}
