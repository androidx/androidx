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

package androidx.wear.protolayout.renderer.impl;

import static androidx.core.util.Preconditions.checkNotNull;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.wear.protolayout.expression.pipeline.FixedQuotaManagerImpl;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.proto.StateProto.State;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme;
import androidx.wear.protolayout.renderer.ProtoLayoutVisibilityState;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.InflateResult;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewGroupMutation;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutInflater.ViewMutationException;
import androidx.wear.protolayout.renderer.inflater.ProtoLayoutThemeImpl;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers;
import androidx.wear.protolayout.renderer.inflater.StandardResourceResolvers;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * A single attached instance of a ProtoLayout. This class will ensure that a ProtoLayout is
 * inflated on a background thread, the first time it is attached to the carousel. As much of the
 * inflation as possible will be done in the background, with only the final attachment of the
 * generated layout to a parent container done on the UI thread.
 */
@RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
public class ProtoLayoutViewInstance implements AutoCloseable {
    /**
     * Listener for clicks on Clickable objects that have an Action to (re)load the contents of a
     * layout.
     */
    public interface LoadActionListener {

        /**
         * Called when a Clickable that has a LoadAction is clicked.
         *
         * @param nextState The state that the next layout should be in.
         */
        void onClick(@NonNull State nextState);
    }

    private static final int DEFAULT_MAX_CONCURRENT_RUNNING_ANIMATIONS = 4;

    @NonNull private static final String TAG = "ProtoLayoutViewInstance";

    @NonNull private final Context mUiContext;
    @NonNull private final Resources mRendererResources;
    @NonNull private final ResourceResolversProvider mResourceResolversProvider;
    @NonNull private final ProtoLayoutTheme mProtoLayoutTheme;
    @Nullable private final ProtoLayoutDynamicDataPipeline mDataPipeline;
    @NonNull private final LoadActionListener mLoadActionListener;
    @NonNull private final ListeningExecutorService mUiExecutorService;
    @NonNull private final ListeningExecutorService mBgExecutorService;
    @NonNull private final String mClickableIdExtra;

    private final boolean mAnimationEnabled;

    private final boolean mAdaptiveUpdateRatesEnabled;
    private boolean mWasFullyVisibleBefore;

    /** This keeps track of the current inflated parent for the layout. */
    @Nullable private ViewGroup mInflateParent = null;

    /**
     * This is simply a reference to the current parent for this layout instance (i.e. the last
     * thing passed into "attach"). This is used because it is technically possible to attach the
     * layout to a parent container, detach it again, then re-attach it before the render pass is
     * complete. In this case, the listener attached to renderFuture will fire multiple time and try
     * and attach the layout multiple times, leading to a crash.
     *
     * <p>This field is used inside of renderFuture's listener to ensure that the layout is still
     * attached to the same object that it was when the listener was added, and hence the layout
     * should be attached.
     *
     * <p>This field should only ever be accessed from the UI thread.
     */
    @Nullable private ViewGroup mAttachParent = null;

    /**
     * This field is used to avoid unnecessary rendering when dealing with non-interactive layouts.
     * For interactive layouts, the diffing should already handle this.
     */
    @Nullable private Layout mPrevLayout = null;

    /**
     * This is used as the Future for the currently running inflation session. The first time
     * "attach" is called, it should start the renderer. Subsequent attach calls should only ever
     * re-attach "inflateParent".
     *
     * <p>If this is null, then nothing has yet called "attach", and hence "render" should be called
     * on a background thread. If this is non-null but not done, then the inflation is in progress.
     * If this is non-null and done, then the inflation is complete, and inflateParent can be safely
     * accessed from the UI thread.
     *
     * <p>This field should only ever be accessed from the UI thread.
     */
    @VisibleForTesting @Nullable ListenableFuture<RenderResult> mRenderFuture = null;

    private boolean mCanReattachWithoutRendering = false;

    /**
     * This is used to provide a {@link ResourceResolvers} object to the {@link
     * ProtoLayoutViewInstance} allowing it to query {@link ResourceProto.Resources} when needed.
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public interface ResourceResolversProvider {

        /** Provide a {@link ResourceResolvers} instance */
        @Nullable
        ResourceResolvers getResourceResolvers(
                @NonNull Context context,
                @NonNull ResourceProto.Resources resources,
                @NonNull ListeningExecutorService listeningExecutorService,
                boolean animationEnabled);
    }

    /** Data about a parent that a layout has been inflated into. */
    static final class InflateParentData {
        @Nullable final InflateResult mInflateResult;

        InflateParentData(@Nullable InflateResult inflateResult) {
            this.mInflateResult = inflateResult;
        }
    }

    /** Base class for result of a {@link #renderOrComputeMutations} call. */
    interface RenderResult {
        /** If this result can be reused when attaching to a parent. */
        boolean canReattachWithoutRendering();

        /**
         * Run any final inflation steps that need to be run on the Ui thread.
         *
         * @param isReattaching if True, this layout is being reattached and will skip content
         *     transition animations.
         */
        @UiThread
        @NonNull
        ListenableFuture<Void> postInflate(
                @NonNull ViewGroup parent,
                @Nullable ViewGroup prevInflateParent,
                boolean isReattaching);
    }

    /** Result of a {@link #renderOrComputeMutations} call when no changes are required. */
    static final class UnchangedRenderResult implements RenderResult {
        @Override
        public boolean canReattachWithoutRendering() {
            return false;
        }

        @NonNull
        @Override
        public ListenableFuture<Void> postInflate(
                @NonNull ViewGroup parent,
                @Nullable ViewGroup prevInflateParent,
                boolean isReattaching) {
            return Futures.immediateVoidFuture();
        }
    }
    /** Result of a {@link #renderOrComputeMutations} call when a failure has happened. */
    static final class FailedRenderResult implements RenderResult {
        @Override
        public boolean canReattachWithoutRendering() {
            return false;
        }

        @NonNull
        @Override
        public ListenableFuture<Void> postInflate(
                @NonNull ViewGroup parent,
                @Nullable ViewGroup prevInflateParent,
                boolean isReattaching) {
            return Futures.immediateVoidFuture();
        }
    }
    /**
     * Result of a {@link #renderOrComputeMutations} call when the layout has been inflated into a
     * new parent.
     */
    static final class InflatedIntoNewParentRenderResult implements RenderResult {
        @NonNull final InflateParentData mNewInflateParentData;

        InflatedIntoNewParentRenderResult(@NonNull InflateParentData newInflateParentData) {
            this.mNewInflateParentData = newInflateParentData;
        }

        @Override
        public boolean canReattachWithoutRendering() {
            return true;
        }

        @NonNull
        @Override
        @UiThread
        public ListenableFuture<Void> postInflate(
                @NonNull ViewGroup parent,
                @Nullable ViewGroup prevInflateParent,
                boolean isReattaching) {
            InflateResult inflateResult =
                    checkNotNull(
                            mNewInflateParentData.mInflateResult,
                            TAG
                                    + " - inflated result was null, but inflating into new parent"
                                    + " requested.");
            inflateResult.updateDynamicDataPipeline(isReattaching);
            parent.removeAllViews();
            parent.addView(
                    inflateResult.inflateParent,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            return Futures.immediateVoidFuture();
        }
    }

    /**
     * Result of a {@link #renderOrComputeMutations} call when the diffs have been computed and
     * needs to be applied to the previous parent.
     */
    static final class ApplyToPrevParentRenderResult implements RenderResult {
        @NonNull final ProtoLayoutInflater mInflater;
        @NonNull final ViewGroupMutation mMutation;

        ApplyToPrevParentRenderResult(
                @NonNull ProtoLayoutInflater inflater, @NonNull ViewGroupMutation mutation) {
            this.mInflater = inflater;
            this.mMutation = mutation;
        }

        @Override
        public boolean canReattachWithoutRendering() {
            return false;
        }

        @NonNull
        @Override
        @UiThread
        public ListenableFuture<Void> postInflate(
                @NonNull ViewGroup parent,
                @Nullable ViewGroup prevInflateParent,
                boolean isReattaching) {
            return mInflater.applyMutation(checkNotNull(prevInflateParent), mMutation);
        }
    }

    /** Config class for {@link ProtoLayoutViewInstance}. */
    @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
    public static final class Config {
        @NonNull private final Context mUiContext;
        @NonNull private final Resources mRendererResources;
        @NonNull private final ResourceResolversProvider mResourceResolversProvider;
        @NonNull private final ProtoLayoutTheme mProtoLayoutTheme;
        @Nullable private final SensorGateway mSensorGateway;
        @Nullable private final StateStore mStateStore;
        @NonNull private final LoadActionListener mLoadActionListener;
        @NonNull private final ListeningExecutorService mUiExecutorService;
        @NonNull private final ListeningExecutorService mBgExecutorService;
        @NonNull private final String mClickableIdExtra;
        private final boolean mAnimationEnabled;
        private final int mRunningAnimationsLimit;

        private final boolean mUpdatesEnabled;
        private final boolean mAdaptiveUpdateRatesEnabled;
        private final boolean mIsViewFullyVisible;

        Config(
                @NonNull Context uiContext,
                @NonNull Resources rendererResources,
                @NonNull ResourceResolversProvider resourceResolversProvider,
                @NonNull ProtoLayoutTheme protoLayoutTheme,
                @Nullable SensorGateway sensorGateway,
                @Nullable StateStore stateStore,
                @NonNull LoadActionListener loadActionListener,
                @NonNull ListeningExecutorService uiExecutorService,
                @NonNull ListeningExecutorService bgExecutorService,
                @NonNull String clickableIdExtra,
                boolean animationEnabled,
                int runningAnimationsLimit,
                boolean updatesEnabled,
                boolean adaptiveUpdateRatesEnabled,
                boolean isViewFullyVisible) {
            this.mUiContext = uiContext;
            this.mRendererResources = rendererResources;
            this.mResourceResolversProvider = resourceResolversProvider;
            this.mProtoLayoutTheme = protoLayoutTheme;
            this.mSensorGateway = sensorGateway;
            this.mStateStore = stateStore;
            this.mLoadActionListener = loadActionListener;
            this.mUiExecutorService = uiExecutorService;
            this.mBgExecutorService = bgExecutorService;
            this.mClickableIdExtra = clickableIdExtra;
            this.mAnimationEnabled = animationEnabled;
            this.mRunningAnimationsLimit = runningAnimationsLimit;
            this.mUpdatesEnabled = updatesEnabled;
            this.mAdaptiveUpdateRatesEnabled = adaptiveUpdateRatesEnabled;
            this.mIsViewFullyVisible = isViewFullyVisible;
        }

        /** Returns UI Context used for interacting with the UI. */
        @NonNull
        public Context getUiContext() {
            return mUiContext;
        }

        /** Returns the Android Resources object for the renderer package. */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public Resources getRendererResources() {
            return mRendererResources;
        }

        /** Returns provider for resolving resources. */
        @RestrictTo(Scope.LIBRARY)
        @NonNull
        public ResourceResolversProvider getResourceResolversProvider() {
            return mResourceResolversProvider;
        }

        /** Returns theme used for this instance. */
        @NonNull
        ProtoLayoutTheme getProtoLayoutTheme() {
            return mProtoLayoutTheme;
        }

        /** Returns gateway for sensor data. */
        @Nullable
        public SensorGateway getSensorGateway() {
            return mSensorGateway;
        }

        /** Returns state store. */
        @Nullable
        public StateStore getStateStore() {
            return mStateStore;
        }

        /** Returns listener for load actions. */
        @NonNull
        public LoadActionListener getLoadActionListener() {
            return mLoadActionListener;
        }

        /** Returns ExecutorService for UI tasks. */
        @NonNull
        public ListeningExecutorService getUiExecutorService() {
            return mUiExecutorService;
        }

        /** Returns ExecutorService for background tasks. */
        @NonNull
        public ListeningExecutorService getBgExecutorService() {
            return mBgExecutorService;
        }

        /** Returns extra used for storing clickable id. */
        @NonNull
        public String getClickableIdExtra() {
            return mClickableIdExtra;
        }

        /** Returns whether animations are enabled. */
        @RestrictTo(Scope.LIBRARY)
        public boolean getAnimationEnabled() {
            return mAnimationEnabled;
        }

        /** Returns how many animations can be concurrently run. */
        @RestrictTo(Scope.LIBRARY)
        public int getRunningAnimationsLimit() {
            return mRunningAnimationsLimit;
        }

        /** Returns whether updates are enabled. */
        @RestrictTo(Scope.LIBRARY)
        public boolean getUpdatesEnabled() {
            return mUpdatesEnabled;
        }

        /** Returns whether adaptive updates are enabled. */
        @RestrictTo(Scope.LIBRARY)
        public boolean getAdaptiveUpdateRatesEnabled() {
            return mAdaptiveUpdateRatesEnabled;
        }

        /** Returns whether view is fully visible. */
        @RestrictTo(Scope.LIBRARY)
        public boolean getIsViewFullyVisible() {
            return mIsViewFullyVisible;
        }

        /** Builder for {@link Config}. */
        @RestrictTo(Scope.LIBRARY_GROUP_PREFIX)
        public static final class Builder {
            @NonNull private final Context mUiContext;
            @Nullable private Resources mRendererResources;
            @Nullable private ResourceResolversProvider mResourceResolversProvider;
            @Nullable private ProtoLayoutTheme mProtoLayoutTheme;
            @Nullable private SensorGateway mSensorGateway;
            @Nullable private StateStore mStateStore;
            @Nullable private LoadActionListener mLoadActionListener;
            @NonNull private final ListeningExecutorService mUiExecutorService;
            @NonNull private final ListeningExecutorService mBgExecutorService;
            @NonNull private final String mClickableIdExtra;
            private boolean mAnimationEnabled = true;
            private int mRunningAnimationsLimit = DEFAULT_MAX_CONCURRENT_RUNNING_ANIMATIONS;

            private boolean mUpdatesEnabled = true;
            private boolean mAdaptiveUpdateRatesEnabled = true;
            private boolean mIsViewFullyVisible = true;

            /**
             * Builder for the {@link Config} class.
             *
             * @param uiContext Context suitable for interacting with UI.
             * @param uiExecutorService Executor for UI related tasks.
             * @param bgExecutorService Executor for background tasks.
             * @param clickableIdExtra String extra for storing clickable id.
             */
            public Builder(
                    @NonNull Context uiContext,
                    @NonNull ListeningExecutorService uiExecutorService,
                    @NonNull ListeningExecutorService bgExecutorService,
                    @NonNull String clickableIdExtra) {
                this.mUiContext = uiContext;
                this.mUiExecutorService = uiExecutorService;
                this.mBgExecutorService = bgExecutorService;
                this.mClickableIdExtra = clickableIdExtra;
            }

            /** Sets provider for resolving resources. */
            @NonNull
            @RestrictTo(Scope.LIBRARY)
            public Builder setResourceResolverProvider(
                    @NonNull ResourceResolversProvider resourceResolversProvider) {
                this.mResourceResolversProvider = resourceResolversProvider;
                return this;
            }

            /**
             * Sets the Android Resources object for the renderer package. This can usually be
             * retrieved with {@link
             * android.content.pm.PackageManager#getResourcesForApplication(String)}. If not
             * specified, this is retrieved from the Ui Context.
             */
            @NonNull
            @RestrictTo(Scope.LIBRARY)
            public Builder setRendererResources(@NonNull Resources rendererResources) {
                this.mRendererResources = rendererResources;
                return this;
            }

            /**
             * Sets the gateway for accessing sensor data. If not set, sensor data won't be
             * accessible.
             */
            @NonNull
            public Builder setSensorGateway(@NonNull SensorGateway sensorGateway) {
                this.mSensorGateway = sensorGateway;
                return this;
            }

            /** Sets the storage for state updates. */
            @NonNull
            public Builder setStateStore(@NonNull StateStore stateStore) {
                this.mStateStore = stateStore;
                return this;
            }

            /**
             * Sets the listener for clicks that will cause contents to be reloaded. Defaults to
             * no-op.
             */
            @NonNull
            public Builder setLoadActionListener(@NonNull LoadActionListener loadActionListener) {
                this.mLoadActionListener = loadActionListener;
                return this;
            }

            /**
             * Sets whether animation are enabled. If disabled, none of the animation will be
             * played.
             */
            @RestrictTo(Scope.LIBRARY)
            @NonNull
            public Builder setAnimationEnabled(boolean animationEnabled) {
                this.mAnimationEnabled = animationEnabled;
                return this;
            }

            /** Sets the limit to how much concurrently running animations are allowed. */
            @RestrictTo(Scope.LIBRARY)
            @NonNull
            public Builder setRunningAnimationsLimit(int runningAnimationsLimit) {
                this.mRunningAnimationsLimit = runningAnimationsLimit;
                return this;
            }

            /** Sets whether sending updates is enabled. */
            @RestrictTo(Scope.LIBRARY)
            @NonNull
            public Builder setUpdatesEnabled(boolean updatesEnabled) {
                this.mUpdatesEnabled = updatesEnabled;
                return this;
            }

            /** Sets whether adaptive updates rates is enabled. */
            @RestrictTo(Scope.LIBRARY)
            @NonNull
            public Builder setAdaptiveUpdateRatesEnabled(boolean adaptiveUpdateRatesEnabled) {
                this.mAdaptiveUpdateRatesEnabled = adaptiveUpdateRatesEnabled;
                return this;
            }

            /** Sets whether the view is fully visible. */
            @RestrictTo(Scope.LIBRARY)
            @NonNull
            public Builder setIsViewFullyVisible(boolean isViewFullyVisible) {
                this.mIsViewFullyVisible = isViewFullyVisible;
                return this;
            }

            /** Builds {@link Config} object. */
            @NonNull
            public Config build() {
                LoadActionListener loadActionListener = mLoadActionListener;
                if (loadActionListener == null) {
                    loadActionListener = p -> {};
                }
                if (mProtoLayoutTheme == null) {
                    mProtoLayoutTheme = ProtoLayoutThemeImpl.defaultTheme(mUiContext);
                }
                if (mResourceResolversProvider == null) {
                    mResourceResolversProvider =
                            (context, resources, listeningExecutorService, animationEnabled) ->
                                    StandardResourceResolvers.forLocalApp(
                                                    resources,
                                                    mUiContext,
                                                    listeningExecutorService,
                                                    mAnimationEnabled)
                                            .build();
                }
                if (mRendererResources == null) {
                    this.mRendererResources = mUiContext.getResources();
                }
                return new Config(
                        mUiContext,
                        mRendererResources,
                        mResourceResolversProvider,
                        mProtoLayoutTheme,
                        mSensorGateway,
                        mStateStore,
                        loadActionListener,
                        mUiExecutorService,
                        mBgExecutorService,
                        mClickableIdExtra,
                        mAnimationEnabled,
                        mRunningAnimationsLimit,
                        mUpdatesEnabled,
                        mAdaptiveUpdateRatesEnabled,
                        mIsViewFullyVisible);
            }
        }
    }

    public ProtoLayoutViewInstance(@NonNull Config config) {
        this.mUiContext = config.getUiContext();
        this.mRendererResources = config.getRendererResources();
        this.mResourceResolversProvider = config.getResourceResolversProvider();
        this.mProtoLayoutTheme = ProtoLayoutThemeImpl.defaultTheme(mUiContext);
        this.mLoadActionListener = config.getLoadActionListener();
        this.mUiExecutorService = config.getUiExecutorService();
        this.mBgExecutorService = config.getBgExecutorService();
        this.mAnimationEnabled = config.getAnimationEnabled();
        this.mClickableIdExtra = config.getClickableIdExtra();
        this.mAdaptiveUpdateRatesEnabled = config.getAdaptiveUpdateRatesEnabled();
        this.mWasFullyVisibleBefore = false;

        StateStore stateStore = config.getStateStore();
        if (stateStore != null) {
            boolean updatesEnabled = config.getUpdatesEnabled();
            mDataPipeline =
                    config.getAnimationEnabled()
                            ? new ProtoLayoutDynamicDataPipeline(
                                    updatesEnabled,
                                    config.getSensorGateway(),
                                    stateStore,
                                    new FixedQuotaManagerImpl(config.getRunningAnimationsLimit()))
                            : new ProtoLayoutDynamicDataPipeline(
                                    updatesEnabled, config.getSensorGateway(), stateStore);
            mDataPipeline.setFullyVisible(config.getIsViewFullyVisible());
        } else {
            mDataPipeline = null;
        }
    }

    @WorkerThread
    @NonNull
    private RenderResult renderOrComputeMutations(
            @NonNull Layout layout,
            @NonNull ResourceProto.Resources resources,
            @Nullable RenderedMetadata prevRenderedMetadata) {

        ResourceResolvers resolvers =
                mResourceResolversProvider.getResourceResolvers(
                        mUiContext, resources, mUiExecutorService, mAnimationEnabled);

        if (resolvers == null) {
            Log.w(TAG, "Resource resolvers cannot be retrieved.");
            return new FailedRenderResult();
        }

        ProtoLayoutInflater.Config.Builder inflaterConfigBuilder =
                new ProtoLayoutInflater.Config.Builder(mUiContext, layout, resolvers)
                        .setLoadActionExecutor(mUiExecutorService)
                        .setLoadActionListener(mLoadActionListener::onClick)
                        .setRendererResources(mRendererResources)
                        .setProtoLayoutTheme(mProtoLayoutTheme)
                        .setAnimationEnabled(mAnimationEnabled)
                        .setClickableIdExtra(mClickableIdExtra)
                        .setAllowLayoutChangingBindsWithoutDefault(true);
        if (mDataPipeline != null) {
            inflaterConfigBuilder.setDynamicDataPipeline(mDataPipeline);
        }

        ProtoLayoutInflater inflater = new ProtoLayoutInflater(inflaterConfigBuilder.build());

        // mark the view and skip doing diff update (to avoid doubling the work each time).
        @Nullable ViewGroupMutation mutation = null;
        if (mAdaptiveUpdateRatesEnabled && prevRenderedMetadata != null) {
            // Compute the mutation here, but if there is a change, apply it in the UI thread.
            try {
                mutation = inflater.computeMutation(prevRenderedMetadata, layout);
            } catch (UnsupportedOperationException ex) {
                Log.w(TAG, "Error computing mutation.", ex);
            }
        }
        if (mutation == null) {
            // Couldn't compute mutation. Inflate from scratch.
            InflateParentData inflateParentData = inflateIntoNewParent(mUiContext, inflater);
            if (inflateParentData.mInflateResult == null) {
                return new FailedRenderResult();
            }
            return new InflatedIntoNewParentRenderResult(inflateParentData);
        } else if (mutation.isNoOp()) {
            // New layout is the same. Nothing to do.
            return new UnchangedRenderResult();
        } else {
            // We have a diff. Ask for it to be applied to the previously inflated parent, but in
            // the UI thread.
            checkNotNull(prevRenderedMetadata);
            return new ApplyToPrevParentRenderResult(inflater, mutation);
        }
    }

    // dereference of possibly-null reference childLp incompatible argument for parameter arg0 of
    // setLayoutParams.
    @SuppressWarnings({"nullness:dereference.of.nullable", "nullness:argument"})
    @WorkerThread
    @NonNull
    private InflateParentData inflateIntoNewParent(
            @NonNull Context uiContext, @NonNull ProtoLayoutInflater inflater) {
        FrameLayout inflateParent;
        int gravity;
        inflateParent = new FrameLayout(uiContext);
        gravity = Gravity.CENTER;

        // Inflate the current timeline entry (passed above) into "inflateParent". This should, at
        // most, add a single element into that container.
        InflateResult result = inflater.inflate(inflateParent);

        // The inflater will only ever add one child to the container. Set correct gravity on it to
        // ensure that the inflated layout is centered within the inflation parent above.
        if (inflateParent.getChildCount() > 0) {
            View firstChild = inflateParent.getChildAt(0);
            FrameLayout.LayoutParams childLp =
                    (FrameLayout.LayoutParams) firstChild.getLayoutParams();
            childLp.gravity = gravity;
            firstChild.setLayoutParams(childLp);
        }
        return new InflateParentData(result);
    }

    /**
     * Render the layout for this layout and attach this layout instance to a parent container. Note
     * that this method may clear all of {@code parent}'s children before attaching the layout, but
     * only if it's not possible to update them in place.
     *
     * <p>If the layout has not yet been inflated, it will not be attached to the parent container
     * immediately (nor will it remove all of {@code parent}'s children); it will instead inflate
     * the layout in the background, then attach it at some point in the future once it has been
     * inflated.
     *
     * <p>Note that it is safe to call {@link ProtoLayoutViewInstance#detach}, and subsequently,
     * attach again while the layout is inflating; it will only attach to the last requested parent
     * (or if detach was the last call, it will not be attached to anything).
     *
     * <p>Note also that this method must be called from the UI thread;
     */
    @UiThread
    @SuppressWarnings({
        "ReferenceEquality",
        "ExecutorTaskName"
    }) // layout == prevLayout is intentional (and enough in this case)
    @NonNull
    public ListenableFuture<Void> renderAndAttach(
            @NonNull Layout layout,
            @NonNull ResourceProto.Resources resources,
            @NonNull ViewGroup parent) {
        if (mAttachParent == null) {
            mAttachParent = parent;
            mAttachParent.removeAllViews();
            // Preload it with the previous layout if we have one.
            if (mInflateParent != null) {
                mAttachParent.addView(mInflateParent);
            }
        } else if (mAttachParent != parent) {
            throw new IllegalStateException("ProtoLayoutViewInstance is already attached!");
        }

        if (layout == mPrevLayout && mInflateParent != null) {
            // Nothing to do.
            return Futures.immediateVoidFuture();
        }

        boolean isReattaching = false;
        if (mRenderFuture != null) {
            if (!mRenderFuture.isDone()) {
                // There is an ongoing rendering operation. We'll skip this request as a missed
                // frame.
                Log.w(TAG, "Skipped layout update: previous layout update hasn't finished yet.");
                return Futures.immediateCancelledFuture();
            } else if (layout == mPrevLayout && mCanReattachWithoutRendering) {
                isReattaching = true;
            } else {
                mRenderFuture = null;
            }
        }

        @Nullable ViewGroup prevInflateParent = getOnlyChildViewGroup(mAttachParent);
        @Nullable
        RenderedMetadata prevRenderedMetadata =
                prevInflateParent != null
                        ? ProtoLayoutInflater.getRenderedMetadata(prevInflateParent)
                        : null;

        if (mRenderFuture == null) {
            mPrevLayout = layout;
            mRenderFuture =
                    mBgExecutorService.submit(() ->
                                            renderOrComputeMutations(
                                                    layout, resources, prevRenderedMetadata));
            mCanReattachWithoutRendering = false;
        }
        SettableFuture<Void> result = SettableFuture.create();
        if (!checkNotNull(mRenderFuture).isDone()) {
            mRenderFuture.addListener(
                    () -> {
                        // Ensure that this inflater is attached to the same parent as when this
                        // listener was created. If not, something has re-attached us in the time it
                        // took for the inflater to execute.
                        if (mAttachParent == parent) {
                            try {
                                result.setFuture(
                                        postInflate(
                                                parent,
                                                prevInflateParent,
                                                checkNotNull(mRenderFuture).get(),
                                                /* isReattaching= */ false,
                                                layout,
                                                resources));
                            } catch (ExecutionException
                                    | InterruptedException
                                    | CancellationException e) {
                                Log.e(TAG, "Failed to render layout", e);
                                result.setException(e);
                            }
                        } else {
                            Log.w(
                                    TAG,
                                    "Layout is rendered, but inflater is no longer attached to the"
                                            + " same parent. Cancelling inflation.");
                            result.cancel(/* mayInterruptIfRunning= */ false);
                        }
                    },
                    mUiExecutorService);
        } else {
            try {
                result.setFuture(
                        postInflate(
                                parent,
                                prevInflateParent,
                                mRenderFuture.get(),
                                isReattaching,
                                layout,
                                resources));
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                Log.e(TAG, "Failed to render layout", e);
                result.setException(e);
            }
        }
        return result;
    }

    @Nullable
    private static ViewGroup getOnlyChildViewGroup(@NonNull ViewGroup parent) {
        if (parent.getChildCount() == 1) {
            View child = parent.getChildAt(0);
            if (child instanceof ViewGroup) {
                return (ViewGroup) child;
            }
        }
        return null;
    }

    @UiThread
    @SuppressWarnings("ExecutorTaskName")
    @NonNull
    private ListenableFuture<Void> postInflate(
            @NonNull ViewGroup parent,
            @Nullable ViewGroup prevInflateParent,
            @NonNull RenderResult renderResult,
            boolean isReattaching,
            @NonNull Layout layout,
            @NonNull ResourceProto.Resources resources) {
        mCanReattachWithoutRendering = renderResult.canReattachWithoutRendering();

        if (renderResult instanceof InflatedIntoNewParentRenderResult) {
            InflateParentData newInflateParentData =
                    ((InflatedIntoNewParentRenderResult) renderResult).mNewInflateParentData;
            mInflateParent =
                    checkNotNull(
                                    newInflateParentData.mInflateResult,
                                    TAG
                                            + " - inflated result was null, but inflating was"
                                            + " requested.")
                            .inflateParent;
        }

        ListenableFuture<Void> postInflateFuture =
                renderResult.postInflate(parent, prevInflateParent, isReattaching);
        SettableFuture<Void> result = SettableFuture.create();
        if (!postInflateFuture.isDone()) {
            postInflateFuture.addListener(
                    () -> {
                        try {
                            result.set(postInflateFuture.get());
                        } catch (ExecutionException
                                | InterruptedException
                                | CancellationException e) {
                            result.setFuture(
                                    handlePostInflateFailure(
                                            e, layout, resources, prevInflateParent, parent));
                        }
                    },
                    mUiExecutorService);
        } else {
            try {
                postInflateFuture.get();
                return Futures.immediateVoidFuture();
            } catch (ExecutionException
                    | InterruptedException
                    | CancellationException
                    | ViewMutationException e) {
                return handlePostInflateFailure(e, layout, resources, prevInflateParent, parent);
            }
        }
        return result;
    }

    @UiThread
    @SuppressWarnings("ReferenceEquality") // layout == prevLayout is intentional
    @NonNull
    private ListenableFuture<Void> handlePostInflateFailure(
            @NonNull Throwable error,
            @NonNull Layout layout,
            @NonNull ResourceProto.Resources resources,
            @Nullable ViewGroup prevInflateParent,
            @NonNull ViewGroup parent) {
        // If a RuntimeError is thrown, it'll be wrapped in an UncheckedExecutionException
        Throwable e = error.getCause();
        if (e instanceof ViewMutationException) {
            Log.w(TAG, "applyMutation failed." + e.getMessage());
            if (mPrevLayout == layout && parent == mAttachParent) {
                Log.w(TAG, "Retrying full inflation.");
                // Clear rendering metadata and prevLayout to force a full reinflation.
                ProtoLayoutInflater.clearRenderedMetadata(checkNotNull(prevInflateParent));
                mPrevLayout = null;
                return renderAndAttach(layout, resources, parent);
            }
        } else {
            Log.e(TAG, "postInflate failed.", error);
        }
        return Futures.immediateFailedFuture(error);
    }

    /**
     * Detach this layout from a parent container. Note that it is safe to call this method while
     * the layout is inflating; see the notes on {@link ProtoLayoutViewInstance#renderAndAttach} for
     * more information.
     */
    @UiThread
    public void detach(@NonNull ViewGroup parent) {
        if (mAttachParent != null && mAttachParent != parent) {
            throw new IllegalStateException("Layout is not attached to parent " + parent);
        }
        detachInternal();
    }

    @UiThread
    private void detachInternal() {
        if (mRenderFuture != null && !mRenderFuture.isDone()) {
            mRenderFuture.cancel(/* mayInterruptIfRunning= */ false);
        }
        setLayoutVisibility(ProtoLayoutVisibilityState.VISIBILITY_STATE_INVISIBLE);

        ViewGroup inflateParent = mInflateParent;
        if (inflateParent != null) {
            ViewGroup parent = (ViewGroup) inflateParent.getParent();
            if (mAttachParent != null && mAttachParent != parent) {
                Log.w(TAG, "inflateParent was attached to the wrong parent.");
            }
            if (parent != null) {
                parent.removeView(inflateParent);
            }
        }
        mAttachParent = null;
    }

    /**
     * Sets whether updates are enabled for this layout. When disabled, updates through the data
     * pipeline (e.g. health updates) will be suppressed.
     */
    @RestrictTo(Scope.LIBRARY)
    @UiThread
    @SuppressWarnings("RestrictTo")
    public void setUpdatesEnabled(boolean updatesEnabled) {
        if (mDataPipeline != null) {
            mDataPipeline.setUpdatesEnabled(updatesEnabled);
        }
    }

    /** Sets the visibility state for this layout. */
    @RestrictTo(Scope.LIBRARY)
    @UiThread
    public void setLayoutVisibility(@ProtoLayoutVisibilityState int visibility) {

        if (mAnimationEnabled && mDataPipeline != null) {
            // Need to check here the previous layout visibility was not FULLY_VISIBLE, so that when
            // the user swipes a little away from the layout, which will emit PARTIALLY_VISIBLE, but
            // then go back to the current layout without entering another one, we do not want to
            // restart the animation when FULLY_VISIBILITY is emitted in this situation.
            if (visibility == ProtoLayoutVisibilityState.VISIBILITY_STATE_FULLY_VISIBLE
                    && !mWasFullyVisibleBefore) {
                mDataPipeline.setFullyVisible(true);
                mWasFullyVisibleBefore = true;
            } else if (visibility == ProtoLayoutVisibilityState.VISIBILITY_STATE_INVISIBLE) {
                mDataPipeline.setFullyVisible(false);
                mWasFullyVisibleBefore = false;
            }
        }
    }

    @Override
    public void close() throws Exception {
        detachInternal();
        mRenderFuture = null;
        mPrevLayout = null;
        if (mDataPipeline != null) {
            mDataPipeline.close();
        }
    }
}
