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

import static androidx.core.util.Preconditions.checkNotNull;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.icu.util.ULocale;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.AnimationSet;

import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.PlatformDataKey;
import androidx.wear.protolayout.expression.PlatformEventSources;
import androidx.wear.protolayout.expression.pipeline.BoundDynamicType;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeAnimator;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator.EvaluationException;
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver;
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl;
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider;
import androidx.wear.protolayout.expression.pipeline.PlatformTimeUpdateNotifierImpl;
import androidx.wear.protolayout.expression.pipeline.QuotaManager;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.ModifiersProto.AnimatedVisibility;
import androidx.wear.protolayout.proto.ModifiersProto.EnterTransition;
import androidx.wear.protolayout.proto.ModifiersProto.ExitTransition;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TypesProto.BoolProp;
import androidx.wear.protolayout.renderer.dynamicdata.NodeInfo.ResolvedAvd;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Pipeline for dynamic data.
 *
 * <p>Given a dynamic ProtoLayout data source, this builds up a {@link BoundDynamicType}, which can
 * source the required data, and transform it into its final form.
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class ProtoLayoutDynamicDataPipeline {
    private static final @NonNull String TAG = "DynamicDataPipeline";

    private static final @NonNull QuotaManager DISABLED_ANIMATIONS_QUOTA_MANAGER =
            new FixedQuotaManagerImpl(/* quotaCap= */ 0, "disabled animations");

    final @NonNull PositionIdTree<NodeInfo> mPositionIdTree = new PositionIdTree<>();
    final @NonNull List<QuotaAwareAnimationSet> mEnterAnimations = new ArrayList<>();
    final @NonNull List<QuotaAwareAnimationSet> mExitAnimations = new ArrayList<>();
    final boolean mEnableAnimations;
    boolean mFullyVisible;
    final @NonNull QuotaManager mAnimationQuotaManager;
    private final @NonNull DynamicTypeEvaluator mEvaluator;
    private final @NonNull PlatformTimeUpdateNotifierImpl mTimeNotifier;
    private final @NonNull DynamicTypePlatformDataProvider<Boolean, DynamicBuilders.DynamicBool>
            mVisibilityStatusDataProvider;
    private final @NonNull
            DynamicTypePlatformDataProvider<Integer, PlatformEventSources.DynamicLayoutUpdateStatus>
            mLayoutUpdateStatusDataProvider;

    /** Creates a {@link ProtoLayoutDynamicDataPipeline} without animation support. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public ProtoLayoutDynamicDataPipeline(
            @NonNull Map<PlatformDataProvider, Set<PlatformDataKey<?>>> platformDataProviders,
            @NonNull StateStore stateStore) {
        // Build pipeline with quota that doesn't allow any animations.
        this(
                platformDataProviders,
                stateStore,
                /* enableAnimations= */ false,
                DISABLED_ANIMATIONS_QUOTA_MANAGER,
                new FixedQuotaManagerImpl(Integer.MAX_VALUE));
    }

    /**
     * Creates a {@link ProtoLayoutDynamicDataPipeline} with animation support. Maximum number of
     * concurrently running animations is defined in the given {@link QuotaManager}.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public ProtoLayoutDynamicDataPipeline(
            @NonNull Map<PlatformDataProvider, Set<PlatformDataKey<?>>> platformDataProviders,
            @NonNull StateStore stateStore,
            @NonNull QuotaManager animationQuotaManager,
            @NonNull QuotaManager dynamicNodesQuotaManager) {
        this(
                platformDataProviders,
                stateStore,
                /* enableAnimations= */ true,
                animationQuotaManager,
                dynamicNodesQuotaManager);
    }

    /** Creates a {@link ProtoLayoutDynamicDataPipeline}. */
    private ProtoLayoutDynamicDataPipeline(
            @NonNull Map<PlatformDataProvider, Set<PlatformDataKey<?>>> platformDataProviders,
            @NonNull StateStore stateStore,
            boolean enableAnimations,
            @NonNull QuotaManager animationQuotaManager,
            @NonNull QuotaManager dynamicNodeQuotaManager) {
        this.mEnableAnimations = enableAnimations;
        this.mAnimationQuotaManager = animationQuotaManager;
        DynamicTypeEvaluator.Config.Builder evaluatorConfigBuilder =
                new DynamicTypeEvaluator.Config.Builder().setStateStore(stateStore);
        evaluatorConfigBuilder.setDynamicTypesQuotaManager(dynamicNodeQuotaManager);

        // Platform sensor data.
        for (Map.Entry<PlatformDataProvider, Set<PlatformDataKey<?>>> providerEntry :
                platformDataProviders.entrySet()) {
            evaluatorConfigBuilder.addPlatformDataProvider(
                    providerEntry.getKey(), providerEntry.getValue());
        }

        // Platform visibility data.
        // Add additional provider for visibility status. It's not needed to come from external
        // callers, as this pipeline knows visibility status
        mVisibilityStatusDataProvider =
                DynamicTypePlatformDataProvider.forDynamicBool(
                        PlatformEventSources.Keys.LAYOUT_VISIBILITY, mFullyVisible);
        evaluatorConfigBuilder.addPlatformDataProvider(
                mVisibilityStatusDataProvider,
                ImmutableSet.of(PlatformEventSources.Keys.LAYOUT_VISIBILITY));

        // Add an additional provider for platform layout update state.
        mLayoutUpdateStatusDataProvider =
                DynamicTypePlatformDataProvider.forDynamicLayoutUpdateStatus(
                        PlatformEventSources.Keys.LAYOUT_UPDATE_STATUS,
                        PlatformEventSources.LAYOUT_UPDATE_IDLE);
        evaluatorConfigBuilder.addPlatformDataProvider(
                mLayoutUpdateStatusDataProvider,
                ImmutableSet.of(PlatformEventSources.Keys.LAYOUT_UPDATE_STATUS));

        // Time data.
        this.mTimeNotifier = new PlatformTimeUpdateNotifierImpl();

        evaluatorConfigBuilder.setPlatformTimeUpdateNotifier(this.mTimeNotifier);
        mTimeNotifier.setUpdatesEnabled(true);
        mVisibilityStatusDataProvider.setUpdatesEnabled(true);

        if (enableAnimations) {
            evaluatorConfigBuilder.setAnimationQuotaManager(animationQuotaManager);
        }
        this.mEvaluator = new DynamicTypeEvaluator(evaluatorConfigBuilder.build());
    }

    /** Returns the number of active dynamic types in this pipeline. */
    @VisibleForTesting
    public int size() {
        return mPositionIdTree.getAllNodes().stream().mapToInt(NodeInfo::size).sum();
    }

    @UiThread
    void clear() {
        mPositionIdTree.clear();
    }

    /** Removes all nodes that are descendants of {@code posId}. */
    @UiThread
    void removeChildNodesFor(@NonNull String posId) {
        mPositionIdTree.removeChildNodesFor(posId);
    }

    /** Build {@link PipelineMaker}. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @NonNull PipelineMaker newPipelineMaker(
            @NonNull BiFunction<EnterTransition, View, AnimationSet> enterAnimationInflator,
            @NonNull BiFunction<ExitTransition, View, AnimationSet> exitAnimationInflator) {
        return new PipelineMaker(this, enterAnimationInflator, exitAnimationInflator);
    }

    /**
     * Test version of the {@link #newPipelineMaker(BiFunction, BiFunction)} without animation
     * inflators.
     */
    @VisibleForTesting
    public @NonNull PipelineMaker newPipelineMaker() {
        return newPipelineMaker(
                (enterTransition, view) -> new AnimationSet(/* shareInterpolator= */ false),
                (exitTransition, view) -> new AnimationSet(/* shareInterpolator= */ false));
    }

    /**
     * Sets whether this proto layout can perform updates. If the proto layout cannot update, then
     * updates through the data pipeline (e.g. health updates) will be suppressed.
     */
    @UiThread
    @SuppressWarnings("RestrictTo")
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void setUpdatesEnabled(boolean canUpdate) {
        mTimeNotifier.setUpdatesEnabled(canUpdate);
        mVisibilityStatusDataProvider.setUpdatesEnabled(canUpdate);
        mLayoutUpdateStatusDataProvider.setUpdatesEnabled(canUpdate);
    }

    /** Closes existing gateways. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @SuppressWarnings("RestrictTo")
    public void close() {
        mPositionIdTree.clear();
        setUpdatesEnabled(false);
    }

    /**
     * PipelineMaker for a dynamic data pipeline.
     *
     * <p>Given a dynamic ProtoLayout data source, this creates a sequence of {@link
     * BoundDynamicType} instances, which can source the required data, and transform it into its
     * final form.
     *
     * <p>The nodes are accumulated and can be committed to the pipeline.
     *
     * <p>Note that this class is not thread-safe.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static final class PipelineMaker {
        private final @NonNull ProtoLayoutDynamicDataPipeline mPipeline;

        private final @NonNull BiFunction<EnterTransition, View, AnimationSet>
                mEnterAnimationInflator;

        private final @NonNull BiFunction<ExitTransition, View, AnimationSet>
                mExitAnimationInflator;

        // Stores pending nodes that are committed to the pipeline after a successful layout update.
        private final @NonNull Map<String, NodeInfo> mPosIdToNodeInfo = new ArrayMap<>();
        private final @NonNull List<String> mNodesPendingChildrenRemoval = new ArrayList<>();
        private final @NonNull Set<String> mChangedNodes = new ArraySet<>();
        private final @NonNull Set<String> mParentsOfChangedNodes = new ArraySet<>();
        private int mExitAnimationsCounter = 0;

        PipelineMaker(
                @NonNull ProtoLayoutDynamicDataPipeline pipeline,
                @NonNull BiFunction<EnterTransition, View, AnimationSet> enterAnimationInflator,
                @NonNull BiFunction<ExitTransition, View, AnimationSet> exitAnimationInflator) {
            this.mPipeline = pipeline;
            this.mEnterAnimationInflator = enterAnimationInflator;
            this.mExitAnimationInflator = exitAnimationInflator;
        }

        /**
         * Clears the current data in the {@link ProtoLayoutDynamicDataPipeline} instance that was
         * used to create this and then commits any stored changes.
         *
         * @param inflatedParent The renderer-owned parent view for all of the layout elements
         *     associated with the nodes in this pipeline. This will be used for content transition
         *     animations.
         * @param isReattaching if True, this layout is being reattached and will skip content
         *     transition animations.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @UiThread
        public void clearDataPipelineAndCommit(
                @NonNull ViewGroup inflatedParent, boolean isReattaching) {
            this.mPipeline.clear();
            this.commit(inflatedParent, isReattaching);
        }

        /**
         * Plays Exit animations. This method should be called while {@code parentView} still
         * corresponds to the previous layout. Any subsequent change to the layout should be
         * schedule through the {@code onEnd} callback.
         *
         * @param parentView The parent view these nodes are being inflated into. Note that it
         *     should be attached to a window (and has gone through its layout passes).
         * @param isReattaching if True, this layout is being reattached and will skip content
         *     transition animations.
         * @param onEnd the callback to execute after all Exit animations have finished.
         */
        @UiThread
        @RestrictTo(Scope.LIBRARY_GROUP)
        public void playExitAnimations(
                @NonNull ViewGroup parentView, boolean isReattaching, @Nullable Runnable onEnd) {
            mPipeline.cancelContentTransitionAnimations();

            // This is needed because onEnd should be called only once. In case that there are exit
            // animations that can't be played due to no quota, QuotaAwaraAnimationSet will try to
            // play it, fail and call onEnd. However, the counter of number of played animation will
            // stay 0, so the outer condition will also be true and onEnd will be called again.
            AtomicBoolean onEndWasCalled = new AtomicBoolean(false);
            Runnable wrappedOnEnd =
                    onEnd != null
                            ? () -> {
                                if (!onEndWasCalled.getAndSet(true)) {
                                    onEnd.run();
                                }
                            }
                            : null;

            if (!isReattaching && mPipeline.mFullyVisible && mPipeline.mEnableAnimations) {
                Map<String, ExitTransition> animatingNodes = new ArrayMap<>();
                for (String parentOfChangedNodes : mParentsOfChangedNodes) {
                    mPipeline
                            .mPositionIdTree
                            .findChildrenFor(parentOfChangedNodes)
                            .forEach(
                                    node ->
                                            addAffectedExitAnimations(
                                                    node.getPosId(), animatingNodes));
                }
                for (String changedNode : mChangedNodes) {
                    addAffectedExitAnimations(changedNode, animatingNodes);
                }
                mExitAnimationsCounter = 0;
                for (Map.Entry<String, ExitTransition> animatingNode : animatingNodes.entrySet()) {
                    View associatedView = parentView.findViewWithTag(animatingNode.getKey());
                    if (associatedView != null) {
                        AnimationSet animationSet =
                                mExitAnimationInflator.apply(
                                        checkNotNull(animatingNode.getValue()), associatedView);
                        if (animationSet != null && !animationSet.getAnimations().isEmpty()) {
                            QuotaAwareAnimationSet quotaAwareAnimationSet =
                                    new QuotaAwareAnimationSet(
                                            animationSet,
                                            mPipeline.mAnimationQuotaManager,
                                            associatedView,
                                            () -> {
                                                if (wrappedOnEnd != null) {
                                                    mExitAnimationsCounter--;
                                                    if (mExitAnimationsCounter == 0) {
                                                        mPipeline.mExitAnimations.clear();
                                                        wrappedOnEnd.run();
                                                    }
                                                }
                                            });
                            quotaAwareAnimationSet.tryStartAnimation(
                                    () -> {
                                        mExitAnimationsCounter++;
                                        mPipeline.mExitAnimations.add(quotaAwareAnimationSet);
                                    });
                        }
                    }
                }
            }
            if (mPipeline.mExitAnimations.isEmpty() && wrappedOnEnd != null) {
                // No exit animations.
                wrappedOnEnd.run();
            }
        }

        private void addAffectedExitAnimations(
                @NonNull String changedNode, @NonNull Map<String, ExitTransition> animatingNodes) {
            List<NodeInfo> nodesAffectedBy =
                    mPipeline.getNodesAffectedBy(
                            changedNode,
                            node -> {
                                AnimatedVisibility animatedVisibility =
                                        node.getAnimatedVisibility();
                                return animatedVisibility != null
                                        && animatedVisibility.hasExitTransition();
                            });
            for (NodeInfo affectedNode : nodesAffectedBy) {
                animatingNodes.putIfAbsent(
                        affectedNode.getPosId(),
                        checkNotNull(affectedNode.getAnimatedVisibility()).getExitTransition());
            }
        }

        /**
         * Commits any stored changes into the {@link ProtoLayoutDynamicDataPipeline} instance that
         * was used to create this. This replaces any already available node and should be called
         * only once per layout update.
         *
         * @param inflatedParent The parent view these nodes are being inflated into. This will be
         *     used for Enter animations. If this view is not attached to a window, the animations
         *     (and the rest of pipeline init) will be scheduled to run when the view attaches to a
         *     window later
         * @param isReattaching if True, this layout is being reattached and will skip content
         *     transition animations.
         */
        @UiThread
        @RestrictTo(Scope.LIBRARY_GROUP)
        public void commit(@NonNull ViewGroup inflatedParent, boolean isReattaching) {
            for (String nodePosId : mNodesPendingChildrenRemoval) {
                mPipeline.removeChildNodesFor(nodePosId);
            }
            mNodesPendingChildrenRemoval.clear();
            for (Entry<String, NodeInfo> entry : mPosIdToNodeInfo.entrySet()) {
                String key = entry.getKey();
                if (key.isEmpty()) {
                    Log.e(TAG, "Ignoring empty posId.");
                    continue;
                }
                mPipeline.mPositionIdTree.addOrReplace(key, entry.getValue());
            }

            // in the modified levels.
            if (isReattaching || !mPipeline.mFullyVisible) {
                // Skip content transition animations.
                mChangedNodes.clear();
            }

            // Try binding requests
            mPipeline.mPositionIdTree.forEach(
                    nodeInfo ->
                            nodeInfo.getPendingBindingRequests()
                                    .removeIf(
                                            pendingRequest ->
                                                    mPipeline.tryBindRequest(
                                                            nodeInfo,
                                                            pendingRequest.getRequest(),
                                                            pendingRequest.getOnBindFailed())));

            // Capture nodes with EnterTransition animation.
            Map<String, EnterTransition> enterTransitionNodes = new ArrayMap<>();
            boolean hasSlideInAnimation = false;
            if (mPipeline.mEnableAnimations) {
                for (String changedNode : mChangedNodes) {
                    List<NodeInfo> nodesAffectedBy =
                            mPipeline.getNodesAffectedBy(
                                    changedNode,
                                    node -> {
                                        AnimatedVisibility animatedVisibility =
                                                node.getAnimatedVisibility();
                                        return animatedVisibility != null
                                                && animatedVisibility.hasEnterTransition();
                                    });
                    for (NodeInfo affectedNode : nodesAffectedBy) {
                        EnterTransition enterTransition =
                                checkNotNull(affectedNode.getAnimatedVisibility())
                                        .getEnterTransition();
                        enterTransitionNodes.putIfAbsent(affectedNode.getPosId(), enterTransition);
                        hasSlideInAnimation |= enterTransition.hasSlideIn();
                    }
                }
            }

            Runnable initLayoutRunnable =
                    () -> {
                        mPipeline.initNewLayout();
                        playEnterAnimations(inflatedParent, isReattaching, enterTransitionNodes);
                    };

            // Slide animations need to know the new measurements of the view in order to calculate
            // start and end positions, so we force a measure pass.
            if (hasSlideInAnimation) {
                // The GlobalLayoutListener ensures that initLayoutRunnable will run after the
                // measure pass has finished.
                ViewTreeObserver viewTreeObserver = inflatedParent.getViewTreeObserver();
                viewTreeObserver.addOnGlobalLayoutListener(
                        new OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (viewTreeObserver.isAlive()) {
                                    viewTreeObserver.removeOnGlobalLayoutListener(this);
                                    initLayoutRunnable.run();
                                }
                            }
                        });
                inflatedParent.measure(
                        MeasureSpec.makeMeasureSpec(
                                inflatedParent.getMeasuredWidth(), MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(
                                inflatedParent.getMeasuredHeight(), MeasureSpec.EXACTLY));
            } else {
                initLayoutRunnable.run();
            }
        }

        @UiThread
        private void playEnterAnimations(
                @NonNull ViewGroup parentView,
                boolean isReattaching,
                Map<String, EnterTransition> animatingNodes) {
            // Cancel any already running Enter animation.
            mPipeline.mEnterAnimations.forEach(QuotaAwareAnimationSet::cancelAnimations);
            mPipeline.mEnterAnimations.clear();

            if (isReattaching || !mPipeline.mFullyVisible || !mPipeline.mEnableAnimations) {
                return;
            }
            for (Map.Entry<String, EnterTransition> animatingNode : animatingNodes.entrySet()) {
                View associatedView = parentView.findViewWithTag(animatingNode.getKey());
                if (associatedView != null) {
                    AnimationSet animationSet =
                            mEnterAnimationInflator.apply(
                                    checkNotNull(animatingNode.getValue()), associatedView);

                    if (animationSet != null && !animationSet.getAnimations().isEmpty()) {
                        QuotaAwareAnimationSet quotaAwareAnimationSet =
                                new QuotaAwareAnimationSet(
                                        animationSet,
                                        mPipeline.mAnimationQuotaManager,
                                        associatedView);
                        quotaAwareAnimationSet.tryStartAnimation(
                                () -> mPipeline.mEnterAnimations.add(quotaAwareAnimationSet));
                    }
                }
            }
        }

        private @NonNull NodeInfo getNodeInfo(@NonNull String posId) {
            return mPosIdToNodeInfo.computeIfAbsent(
                    posId, k -> new NodeInfo(posId, mPipeline.mAnimationQuotaManager));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicString stringSource,
                @NonNull Locale locale,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<String> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicStringInternal(
                            stringSource, ULocale.forLocale(locale), consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicInt32 int32Source,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Integer> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicInt32Internal(int32Source, consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicString stringSource,
                @NonNull String invalidData,
                @NonNull Locale locale,
                @NonNull String posId,
                @NonNull Consumer<String> consumer) {
            return addPipelineFor(
                    stringSource, locale, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicFloat floatSource,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Float> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicFloatInternal(floatSource, consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicFloat floatSource,
                float invalidData,
                @NonNull String posId,
                @NonNull Consumer<Float> consumer) {
            return addPipelineFor(
                    floatSource, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicColor colorSource,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Integer> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicColorInternal(colorSource, consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicBool boolSource,
                @NonNull String posId,
                @NonNull Runnable triggerAnimationRunnable) {
            DynamicTypeValueReceiver<Boolean> consumer =
                    buildBooleanConditionTriggerCallback(
                            triggerAnimationRunnable, mPipeline.mAnimationQuotaManager);
            return addPipelineFor(boolSource, posId, consumer);
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicBool boolSource,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicBoolInternal(boolSource, consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DynamicBool boolSource,
                boolean invalidData,
                @NonNull String posId,
                @NonNull Consumer<Boolean> consumer) {
            return addPipelineFor(
                    boolSource, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DpProp dpProp,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Float> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicFloatInternal(
                            dpProp.getDynamicValue(), consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DegreesProp degreesProp,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Float> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicFloatInternal(
                            degreesProp.getDynamicValue(), consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull ColorProp colorProp,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Integer> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicColorInternal(
                            colorProp.getDynamicValue(), consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull BoolProp boolProp,
                @NonNull String posId,
                @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
            DynamicTypeBindingRequest bindingRequest =
                    DynamicTypeBindingRequest.forDynamicBoolInternal(
                            boolProp.getDynamicValue(), consumer);
            addToPendingBindingRequest(posId, bindingRequest, consumer::onInvalidated);
            return this;
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DpProp dpProp,
                float invalidData,
                @NonNull String posId,
                @NonNull Consumer<Float> consumer) {
            return addPipelineFor(dpProp, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull DegreesProp degreesProp,
                float invalidData,
                @NonNull String posId,
                @NonNull Consumer<Float> consumer) {
            return addPipelineFor(
                    degreesProp, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull ColorProp colorProp,
                int invalidData,
                @NonNull String posId,
                @NonNull Consumer<Integer> consumer) {
            return addPipelineFor(
                    colorProp, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressWarnings("RestrictTo")
        public @NonNull PipelineMaker addPipelineFor(
                @NonNull BoolProp boolProp,
                boolean invalidData,
                @NonNull String posId,
                @NonNull Consumer<Boolean> consumer) {
            return addPipelineFor(boolProp, posId, buildStateUpdateCallback(invalidData, consumer));
        }

        private void addToPendingBindingRequest(
                String posId, DynamicTypeBindingRequest request, Runnable onFailed) {
            NodeInfo nodeInfo = getNodeInfo(posId);
            nodeInfo.addPendingBindingRequest(request, onFailed);
        }

        /** This store method shall be called during the layout inflation in a background thread. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        @SuppressLint("CheckReturnValue") // (b/247804720)
        public @NonNull PipelineMaker addResolvedAnimatedImage(
                @NonNull AnimatedVectorDrawable drawable,
                @NonNull Trigger trigger,
                @NonNull String posId) {
            if (!this.mPipeline.mEnableAnimations) {
                Log.w(TAG, "Cannot use ResolvedAnimationImage; animations are disabled.");
                return this;
            }

            getNodeInfo(posId).addResolvedAvd(drawable, trigger);
            return this;
        }

        /**
         * This store method shall be called during the layout inflation in a background thread. It
         * adds given {@link DynamicBool} to the pipeline too.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker addResolvedAnimatedImageWithBoolTrigger(
                @NonNull AnimatedVectorDrawable drawable,
                @NonNull Trigger trigger,
                @NonNull String posId,
                @NonNull DynamicBool boolTrigger) {
            if (!this.mPipeline.mEnableAnimations) {
                Log.w(TAG, "Cannot use ResolvedAnimationImage; animations are disabled.");
                return this;
            }

            if (trigger.getInnerCase() != Trigger.InnerCase.ON_CONDITION_MET_TRIGGER) {
                Log.w(TAG, "Wrong trigger type.");
                return this;
            }

            ResolvedAvd avd = getNodeInfo(posId).addResolvedAvd(drawable, trigger);
            addPipelineFor(boolTrigger, posId, avd::startAnimation);
            return this;
        }

        /** This store method shall be called during the layout inflation in a background thread. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker addResolvedSeekableAnimatedImage(
                @NonNull SeekableAnimatedVectorDrawable seekableDrawable,
                @NonNull DynamicFloat boundProgress,
                @NonNull String posId) {
            if (!this.mPipeline.mEnableAnimations) {
                Log.w(TAG, "Cannot use ResolveSeekableAvd; animations are disabled.");
                return this;
            }

            // Register the bound progress to the seekable animated drawable.
            addPipelineFor(
                    boundProgress,
                    0.0f,
                    posId,
                    aFloat -> {
                        float progress = max(0.0f, min(aFloat, 1.0f));
                        seekableDrawable.setCurrentPlayTime(
                                (long) (progress * seekableDrawable.getTotalDuration()));
                    });
            getNodeInfo(posId)
                    .addResolvedSeekableAvd(
                            new NodeInfo.ResolvedSeekableAvd(seekableDrawable, boundProgress));
            return this;
        }

        /** Stores the {@link AnimatedVisibility} associated with the {@code posId}. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker storeAnimatedVisibilityFor(
                @NonNull String posId, @NonNull AnimatedVisibility animatedVisibility) {
            if (!mPipeline.mEnableAnimations) {
                Log.w(TAG, "Can't use AnimatedVisibility; animations are disabled.");
                return this;
            }
            getNodeInfo(posId).setAnimatedVisibility(animatedVisibility);
            return this;
        }

        /**
         * Mark the node {@code posId} as changed. Content transition animations affected by this
         * node will be triggered when the pipeline is committed.
         *
         * @param posId positionId of the node
         * @param includePreviousChildren if True, the previous children of this node will be marked
         *     as changed too. This is used for triggering Exit animations.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker markNodeAsChanged(
                @NonNull String posId, boolean includePreviousChildren) {
            if (mPipeline.mEnableAnimations) {
                mChangedNodes.add(posId);
                mParentsOfChangedNodes.add(posId);
            }
            return this;
        }

        private static @NonNull DynamicTypeValueReceiver<Boolean>
                buildBooleanConditionTriggerCallback(
                        @NonNull Runnable triggerAnimationRunnable,
                        @NonNull QuotaManager quotaManager) {
            return new DynamicTypeValueReceiver<Boolean>() {
                private boolean mCurrent;

                @Override
                public void onData(@NonNull Boolean newData) {

                    if (newData && !mCurrent && quotaManager.tryAcquireQuota(1)) {
                        triggerAnimationRunnable.run();
                    }
                    mCurrent = newData;
                }

                @Override
                public void onInvalidated() {}
            };
        }

        private <T> @NonNull DynamicTypeValueReceiver<T> buildStateUpdateCallback(
                @NonNull T invalidData, @NonNull Consumer<T> consumer) {
            return new DynamicTypeValueReceiver<T>() {
                @Override
                public void onData(@NonNull T newData) {
                    consumer.accept(newData);
                }

                @Override
                public void onInvalidated() {
                    consumer.accept(invalidData);
                }
            };
        }

        /**
         * Add the given source to the pipeline for future evaluation. Evaluation will start when
         * {@link PipelineMaker} is committed with {@link PipelineMaker#commit}.
         */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker markForChildRemoval(@NonNull String nodePosId) {
            mNodesPendingChildrenRemoval.add(nodePosId);
            return this;
        }

        /** Stores a node if doesn't exist. Otherwise does nothing. */
        @RestrictTo(Scope.LIBRARY_GROUP)
        public @NonNull PipelineMaker rememberNode(@NonNull String nodePosId) {
            NodeInfo ignored = getNodeInfo(nodePosId);
            return this;
        }
    }

    /**
     * Initialize the data pipeline without playing content transition animations. Normally this is
     * called automatically when the parent {@link ViewGroup} associated with this pipeline is
     * attached to a {@link View} hierarchy. This is so that the content transition animations can
     * be executed before this (if needed).
     *
     * <p>This method can be called directly in screenshot tests and when the renderer output is
     * never supposed to be attached to a window.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    public void initWithoutContentTransition() {
        initNewLayout();
    }

    /** Initialize the data pipeline after a new layout is pushed. */
    @UiThread
    @SuppressWarnings("RestrictTo")
    void initNewLayout() {
        if (mFullyVisible) {
            playAvdAnimations(Trigger.InnerCase.ON_VISIBLE_TRIGGER);
            playAvdAnimations(Trigger.InnerCase.ON_VISIBLE_ONCE_TRIGGER);
        }
        playAvdAnimations(Trigger.InnerCase.ON_LOAD_TRIGGER);
        setAnimationVisibility(mFullyVisible);

        mPositionIdTree.forEach(NodeInfo::initPendingBoundTypes);
    }

    private boolean tryBindRequest(
            @NonNull NodeInfo nodeInfo,
            @NonNull DynamicTypeBindingRequest request,
            @NonNull Runnable onBindFailed) {
        try {
            BoundDynamicType dynamicType = mEvaluator.bind(request);
            nodeInfo.addBoundType(dynamicType);
            return true;
        } catch (EvaluationException exception) {
            Log.e(TAG, "Failed to bind dynamicType.", exception);
            onBindFailed.run();
            return false;
        }
    }

    /** Play the animation with the given trigger type. */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public void playAvdAnimations(Trigger.@NonNull InnerCase triggerCase) {
        mPositionIdTree.forEach(info -> info.playAvdAnimations(triggerCase));
    }

    /** Sets visibility of animations. Also pauses or resumes any animators. */
    @UiThread
    private void setAnimationVisibility(boolean visible) {
        mPositionIdTree.forEach(info -> info.setVisibility(visible));
    }

    /** Reset the avd animations with the given trigger type. */
    @UiThread
    @VisibleForTesting
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void resetAvdAnimations(Trigger.@NonNull InnerCase triggerCase) {
        mPositionIdTree.forEach(info -> info.resetAvdAnimations(triggerCase));
    }

    /** Stops running avd animations and releases their quota. */
    @UiThread
    @VisibleForTesting
    @RestrictTo(Scope.LIBRARY_GROUP)
    public void stopAvdAnimations(Trigger.@NonNull InnerCase triggerCase) {
        mPositionIdTree.forEach(info -> info.stopAvdAnimations(triggerCase));
    }

    /** Cancel any already running content transition animations. */
    @UiThread
    void cancelContentTransitionAnimations() {
        ImmutableList.copyOf(mExitAnimations).forEach(QuotaAwareAnimationSet::cancelAnimations);
        mExitAnimations.clear();
        mEnterAnimations.forEach(QuotaAwareAnimationSet::cancelAnimations);
        mEnterAnimations.clear();
    }

    /**
     * Sets visibility for resources tracked by the pipeline and plays / stops any affected
     * animations.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    public void setFullyVisible(boolean fullyVisible) {
        if (this.mFullyVisible == fullyVisible) {
            return;
        }

        this.mFullyVisible = fullyVisible;

        // Set visibility to already started INFINITE AVD will pause the animation when the drawable
        // is invisible and resume the animation when becomes visible again.
        setAnimationVisibility(fullyVisible);

        // Send platform data on visibility.
        this.mVisibilityStatusDataProvider.setValue(fullyVisible);

        if (fullyVisible) {
            playAvdAnimations(Trigger.InnerCase.ON_VISIBLE_TRIGGER);
            playAvdAnimations(Trigger.InnerCase.ON_VISIBLE_ONCE_TRIGGER);
        } else {
            cancelContentTransitionAnimations();
            // Stop the AVD animation with ON_VISIBLE_TRIGGER, but not stop AVD animations with
            // ON_VISIBLE_ONCE_TRIGGER and ON_LOAD_TRIGGER. AVD does not provide API to check
            // whether it is infinite, thus it is hard to stop finite animations only. For the AVDs
            // that would not get restarted, animations are not stopped when the layout becomes
            // invisible. Finite animations continue until they reach the end, while infinite
            // animations are paused by setting their visibility to false.
            stopAvdAnimations(Trigger.InnerCase.ON_VISIBLE_TRIGGER);
            resetAvdAnimations(Trigger.InnerCase.ON_VISIBLE_TRIGGER);
        }
    }

    /**
     * Sets the layout's update status.
     *
     * <p>This is used to update {@link PlatformEventSources#layoutUpdateStatus()} platform data
     * binding.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    @UiThread
    public void setLayoutUpdateStatus(
            @PlatformEventSources.LayoutUpdateStatus int layoutUpdateStatus) {
        this.mLayoutUpdateStatusDataProvider.setValue(layoutUpdateStatus);
    }

    /**
     * Returns the total duration in milliseconds of the animated drawable associated with a
     * StateSource with the given key name; or null if no such SourceKey exists.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public @Nullable Long getSeekableAnimationTotalDurationMillis(@NonNull String sourceKey) {
        NodeInfo node =
                mPositionIdTree.findFirst(
                        nodeInfo ->
                                nodeInfo.getSeekableAnimationTotalDurationMillis(sourceKey)
                                        != null);
        if (node != null) {
            return node.getSeekableAnimationTotalDurationMillis(sourceKey);
        }
        return null;
    }

    /**
     * Returns the list of nodes with matching {@code predicate} affected by a change to the node
     * {@code posId}
     */
    @UiThread
    @NonNull List<NodeInfo> getNodesAffectedBy(
            @NonNull String posId, @NonNull Predicate<NodeInfo> predicate) {
        List<NodeInfo> affectedNodes = mPositionIdTree.findAncestorsFor(posId, predicate);
        NodeInfo currentNode = mPositionIdTree.get(posId);
        if (currentNode != null && predicate.test(currentNode)) {
            affectedNodes.add(currentNode);
        }

        return affectedNodes;
    }

    /** Returns how many animations are running. */
    @VisibleForTesting
    public int getRunningAnimationsCount() {
        return mPositionIdTree.getAllNodes().stream()
                        .mapToInt(NodeInfo::getRunningAnimationCount)
                        .sum()
                + mEnterAnimations.stream()
                        .mapToInt(QuotaAwareAnimationSet::getRunningAnimationCount)
                        .sum()
                + mExitAnimations.stream()
                        .mapToInt(QuotaAwareAnimationSet::getRunningAnimationCount)
                        .sum();
    }

    public @NonNull List<DynamicTypeAnimator> getAnimations() {
        return mPositionIdTree.getAllNodes().stream()
                .flatMap(nodeInfo -> nodeInfo.getAnimations().stream())
                .collect(Collectors.toList());
    }

    /** Returns the cost of nodes existing in the pipeline. */
    @VisibleForTesting
    public int getDynamicExpressionsNodesCost() {
        return mPositionIdTree.getAllNodes().stream()
                .mapToInt(NodeInfo::getExpressionDynamicNodesCost)
                .sum();
    }

    /** Returns whether all quota has been released. */
    @VisibleForTesting
    public boolean isAllQuotaReleased() {
        return mAnimationQuotaManager instanceof FixedQuotaManagerImpl
                && ((FixedQuotaManagerImpl) mAnimationQuotaManager).isAllQuotaReleased();
    }
}
