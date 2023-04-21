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

import static androidx.core.util.Preconditions.checkNotNull;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.FIRST_CHILD_INDEX;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.ROOT_NODE_ID;
import static androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.getParentNodePosId;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateVoidFuture;

import static java.lang.Math.max;
import static java.lang.Math.round;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewOutlineProvider;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.Space;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable;
import androidx.wear.protolayout.expression.pipeline.AnimationsHelper;
import androidx.wear.protolayout.expression.proto.AnimationParameterProto.AnimationSpec;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.proto.ActionProto.Action;
import androidx.wear.protolayout.proto.ActionProto.AndroidActivity;
import androidx.wear.protolayout.proto.ActionProto.AndroidExtra;
import androidx.wear.protolayout.proto.ActionProto.LaunchAction;
import androidx.wear.protolayout.proto.ActionProto.LoadAction;
import androidx.wear.protolayout.proto.AlignmentProto.AngularAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.ArcAnchorType;
import androidx.wear.protolayout.proto.AlignmentProto.HorizontalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.TextAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignment;
import androidx.wear.protolayout.proto.AlignmentProto.VerticalAlignmentProp;
import androidx.wear.protolayout.proto.ColorProto.ColorProp;
import androidx.wear.protolayout.proto.DimensionProto.ArcLineLength;
import androidx.wear.protolayout.proto.DimensionProto.ArcSpacerLength;
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension;
import androidx.wear.protolayout.proto.DimensionProto.ContainerDimension.InnerCase;
import androidx.wear.protolayout.proto.DimensionProto.DegreesProp;
import androidx.wear.protolayout.proto.DimensionProto.DpProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedAngularDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ExpandedDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.ImageDimension;
import androidx.wear.protolayout.proto.DimensionProto.ProportionalDimensionProp;
import androidx.wear.protolayout.proto.DimensionProto.SpProp;
import androidx.wear.protolayout.proto.DimensionProto.SpacerDimension;
import androidx.wear.protolayout.proto.DimensionProto.WrappedDimensionProp;
import androidx.wear.protolayout.proto.FingerprintProto.NodeFingerprint;
import androidx.wear.protolayout.proto.LayoutElementProto.ExtensionLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.Arc;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcLine;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcSpacer;
import androidx.wear.protolayout.proto.LayoutElementProto.ArcText;
import androidx.wear.protolayout.proto.LayoutElementProto.Box;
import androidx.wear.protolayout.proto.LayoutElementProto.Column;
import androidx.wear.protolayout.proto.LayoutElementProto.ContentScaleMode;
import androidx.wear.protolayout.proto.LayoutElementProto.FontStyle;
import androidx.wear.protolayout.proto.LayoutElementProto.Image;
import androidx.wear.protolayout.proto.LayoutElementProto.Layout;
import androidx.wear.protolayout.proto.LayoutElementProto.LayoutElement;
import androidx.wear.protolayout.proto.LayoutElementProto.MarqueeParameters;
import androidx.wear.protolayout.proto.LayoutElementProto.Row;
import androidx.wear.protolayout.proto.LayoutElementProto.Spacer;
import androidx.wear.protolayout.proto.LayoutElementProto.Span;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanImage;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanText;
import androidx.wear.protolayout.proto.LayoutElementProto.SpanVerticalAlignmentProp;
import androidx.wear.protolayout.proto.LayoutElementProto.Spannable;
import androidx.wear.protolayout.proto.LayoutElementProto.Text;
import androidx.wear.protolayout.proto.LayoutElementProto.TextOverflow;
import androidx.wear.protolayout.proto.LayoutElementProto.TextOverflowProp;
import androidx.wear.protolayout.proto.ModifiersProto.ArcModifiers;
import androidx.wear.protolayout.proto.ModifiersProto.Background;
import androidx.wear.protolayout.proto.ModifiersProto.Border;
import androidx.wear.protolayout.proto.ModifiersProto.Clickable;
import androidx.wear.protolayout.proto.ModifiersProto.EnterTransition;
import androidx.wear.protolayout.proto.ModifiersProto.ExitTransition;
import androidx.wear.protolayout.proto.ModifiersProto.FadeInTransition;
import androidx.wear.protolayout.proto.ModifiersProto.FadeOutTransition;
import androidx.wear.protolayout.proto.ModifiersProto.Modifiers;
import androidx.wear.protolayout.proto.ModifiersProto.Padding;
import androidx.wear.protolayout.proto.ModifiersProto.Semantics;
import androidx.wear.protolayout.proto.ModifiersProto.SemanticsRole;
import androidx.wear.protolayout.proto.ModifiersProto.SlideDirection;
import androidx.wear.protolayout.proto.ModifiersProto.SlideInTransition;
import androidx.wear.protolayout.proto.ModifiersProto.SlideOutTransition;
import androidx.wear.protolayout.proto.ModifiersProto.SlideParentSnapOption;
import androidx.wear.protolayout.proto.ModifiersProto.SpanModifiers;
import androidx.wear.protolayout.proto.StateProto.State;
import androidx.wear.protolayout.proto.TriggerProto.OnConditionMetTrigger;
import androidx.wear.protolayout.proto.TriggerProto.OnLoadTrigger;
import androidx.wear.protolayout.proto.TriggerProto.Trigger;
import androidx.wear.protolayout.proto.TypesProto.StringProp;
import androidx.wear.protolayout.renderer.ProtoLayoutExtensionViewProvider;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme;
import androidx.wear.protolayout.renderer.ProtoLayoutTheme.FontSet;
import androidx.wear.protolayout.renderer.R;
import androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer;
import androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.LayoutDiff;
import androidx.wear.protolayout.renderer.common.ProtoLayoutDiffer.TreeNodeWithChange;
import androidx.wear.protolayout.renderer.dynamicdata.ProtoLayoutDynamicDataPipeline;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.LayoutInfo;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.LinearLayoutProperties;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.PendingFrameLayoutParams;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.PendingLayoutParams;
import androidx.wear.protolayout.renderer.inflater.RenderedMetadata.ViewProperties;
import androidx.wear.protolayout.renderer.inflater.ResourceResolvers.ResourceAccessException;
import androidx.wear.widget.ArcLayout;
import androidx.wear.widget.CurvedTextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Renderer for ProtoLayout.
 *
 * <p>This variant uses Android views to represent the contents of the ProtoLayout.
 */
public final class ProtoLayoutInflater {

    private static final String TAG = "ProtoLayoutInflater";
    private static final char ZERO_WIDTH_JOINER = '\u200D';

    // This will help debug potential layout issues that might be leading to full layout updates or
    // poor performance in general.
    private static final boolean DEBUG_DIFF_UPDATE_ENABLED = false;

    /** Target alpha for fade in animation. */
    private static final float FADE_IN_TARGET_ALPHA = 1;

    /** Initial alpha for fade out animation. */
    private static final float FADE_OUT_INITIAL_ALPHA = 1;

    /** The default trigger for animations set to onLoad. */
    private static final Trigger DEFAULT_ANIMATION_TRIGGER =
            Trigger.newBuilder().setOnLoadTrigger(OnLoadTrigger.getDefaultInstance()).build();

    /**
     * Default maximum raw byte size for a bitmap drawable.
     *
     * @see <a
     *     href="https://cs.android.com/android/_/android/platform/frameworks/base/+/d01036ee5893357db577c961119fb85825247f03:graphics/java/android/graphics/RecordingCanvas.java;l=44;bpv=1;bpt=0;drc=00af5271dabd578397176eda0cd7a66c55fac59a">
     *     The framework enforced max size</a>
     */
    private static final int DEFAULT_MAX_BITMAP_RAW_SIZE = 20 * 1024 * 1024;

    private static final int HORIZONTAL_ALIGN_DEFAULT_GRAVITY = Gravity.CENTER_HORIZONTAL;
    private static final int VERTICAL_ALIGN_DEFAULT_GRAVITY = Gravity.CENTER_VERTICAL;
    private static final int TEXT_ALIGN_DEFAULT = Gravity.CENTER_HORIZONTAL;
    private static final ScaleType IMAGE_DEFAULT_SCALE_TYPE = ScaleType.FIT_CENTER;

    @ArcLayout.LayoutParams.VerticalAlignment
    private static final int ARC_VERTICAL_ALIGN_DEFAULT =
            ArcLayout.LayoutParams.VERTICAL_ALIGN_CENTER;

    @SizedArcContainer.LayoutParams.AngularAlignment
    private static final int ANGULAR_ALIGNMENT_DEFAULT =
            SizedArcContainer.LayoutParams.ANGULAR_ALIGNMENT_CENTER;

    private static final int SPAN_VERTICAL_ALIGN_DEFAULT = ImageSpan.ALIGN_BOTTOM;

    // This is pretty badly named; TruncateAt specifies where to place the ellipsis (or whether to
    // marquee). Disabling truncation with null actually disables the _ellipsis_, but text will
    // still be truncated.
    @Nullable private static final TruncateAt TEXT_OVERFLOW_DEFAULT = null;

    private static final int TEXT_COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int TEXT_MAX_LINES_DEFAULT = 1;
    private static final int TEXT_MIN_LINES = 1;

    private static final ContainerDimension CONTAINER_DIMENSION_DEFAULT =
            ContainerDimension.newBuilder()
                    .setWrappedDimension(WrappedDimensionProp.getDefaultInstance())
                    .build();

    @ArcLayout.AnchorType private static final int ARC_ANCHOR_DEFAULT = ArcLayout.ANCHOR_CENTER;

    // White
    private static final int LINE_COLOR_DEFAULT = 0xFFFFFFFF;

    static final PendingLayoutParams NO_OP_PENDING_LAYOUT_PARAMS = layoutParams -> layoutParams;

    final Context mUiContext;

    // Context wrapped with the provided ProtoLayoutTheme. This context should be used for creating
    // any Text Views, as it will apply text appearance attributes.
    private final Context mProtoLayoutThemeContext;

    private final ProtoLayoutTheme mProtoLayoutTheme;
    private final Layout mLayoutProto;
    private final ResourceResolvers mLayoutResourceResolvers;

    private final Optional<ProtoLayoutDynamicDataPipeline> mDataPipeline;

    @Nullable private final ProtoLayoutExtensionViewProvider mExtensionViewProvider;

    private final boolean mAllowLayoutChangingBindsWithoutDefault;
    final String mClickableIdExtra;

    @Nullable final Executor mLoadActionExecutor;
    final LoadActionListener mLoadActionListener;
    final boolean mAnimationEnabled;

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

    /**
     * A one-off class to be returned from {@link ProtoLayoutInflater#inflate} containing top level
     * parent, list of content transition animations to be run and a PipelineMaker with pending
     * changes to the dynamic data pipeline.
     */
    public static final class InflateResult {
        public final ViewGroup inflateParent;
        public final View firstChild;
        private final Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> mPipelineMaker;

        InflateResult(
                ViewGroup inflateParent,
                View firstChild,
                Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
            this.inflateParent = inflateParent;
            this.firstChild = firstChild;
            this.mPipelineMaker = pipelineMaker;
        }

        /**
         * Update the DynamicDataPipeline with new nodes that were stored during the layout update.
         *
         * @param isReattaching if True, this layout is being reattached and will skip content
         *     transition animations.
         */
        @UiThread
        public void updateDynamicDataPipeline(boolean isReattaching) {
            mPipelineMaker.ifPresent(
                    pipe -> pipe.clearDataPipelineAndCommit(inflateParent, isReattaching));
        }
    }

    /** A mutation that can be applied to a {@link ViewGroup}, using {@link #applyMutation}. */
    public static final class ViewGroupMutation {
        final List<InflatedView> mInflatedViews;
        final RenderedMetadata mRenderedMetadataAfterMutation;
        final NodeFingerprint mPreMutationRootNodeFingerprint;
        final Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> mPipelineMaker;

        ViewGroupMutation(
                List<InflatedView> inflatedViews,
                RenderedMetadata renderedMetadataAfterMutation,
                NodeFingerprint preMutationRootNodeFingerprint,
                Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
            this.mInflatedViews = inflatedViews;
            this.mRenderedMetadataAfterMutation = renderedMetadataAfterMutation;
            this.mPreMutationRootNodeFingerprint = preMutationRootNodeFingerprint;
            this.mPipelineMaker = pipelineMaker;
        }

        /** Returns true if this mutation has no effect. */
        public boolean isNoOp() {
            return this.mInflatedViews.isEmpty();
        }
    }

    private static final class InflatedView {
        final View mView;
        final LayoutParams mLayoutParams;
        final PendingLayoutParams mChildLayoutParams;
        private int mNumMissingChildren;

        /**
         * @param view The {@link View} that has been inflated.
         * @param layoutParams The {@link LayoutParams} that must be used when attaching the
         *     inflated view to a parent.
         * @param childLayoutParams The {@link LayoutParams} that must be applied to children
         *     carried over from a previous layout.
         * @param numMissingChildren Non-zero if {@code view} is a {@link ViewGroup} whose children
         *     have not been added. This means that before using this view in a layout, its children
         *     must be copied from the {@link ViewGroup} that represents the previous version of
         *     this layout element.
         */
        InflatedView(
                View view,
                LayoutParams layoutParams,
                PendingLayoutParams childLayoutParams,
                int numMissingChildren) {
            this.mView = view;
            this.mLayoutParams = layoutParams;
            this.mChildLayoutParams = childLayoutParams;
            this.mNumMissingChildren = numMissingChildren;
        }

        InflatedView(View view, LayoutParams layoutParams) {
            this(view, layoutParams, NO_OP_PENDING_LAYOUT_PARAMS, /* numMissingChildren= */ 0);
        }

        boolean addMissingChildrenFrom(View source) {
            if (mNumMissingChildren == 0) {
                // Nothing to do.
                return true;
            }
            Object tagObj = source.getTag();
            String tag = (tagObj == null ? "unknown" : (String) tagObj);
            if (!(mView instanceof ViewGroup)) {
                Log.w(TAG, "Destination is not a group: " + tag);
                return false;
            }
            ViewGroup destinationGroup = (ViewGroup) mView;
            if (destinationGroup.getChildCount() > 0) {
                Log.w(TAG, "Destination already has children: " + tag);
                return false;
            }
            if (!(source instanceof ViewGroup)) {
                Log.w(TAG, "Source is not a group: " + tag);
                return false;
            }
            ViewGroup sourceGroup = (ViewGroup) source;
            if (sourceGroup.getChildCount() != mNumMissingChildren) {
                Log.w(
                        TAG,
                        String.format(
                                "Expected %d children in %s found %d",
                                mNumMissingChildren, tag, sourceGroup.getChildCount()));
                return false;
            }
            List<View> children = new ArrayList<>(sourceGroup.getChildCount());
            for (int i = 0; i < mNumMissingChildren; i++) {
                children.add(sourceGroup.getChildAt(i));
            }
            sourceGroup.removeAllViews();

            for (View child : children) {
                destinationGroup.addView(child);
                child.setLayoutParams(
                        mChildLayoutParams.apply(checkNotNull(child.getLayoutParams())));
            }
            mNumMissingChildren = 0;
            return true;
        }

        @Nullable
        String getTag() {
            return (String) mView.getTag();
        }
    }

    /**
     * A one-of class to pass either a real {@link ViewGroup} or only its needed properties through
     * the renderer.
     */
    private static final class ParentViewWrapper {
        @Nullable private final ViewGroup mParent;
        private final ViewProperties mParentProps;

        ParentViewWrapper(ViewGroup parent, LayoutParams parentLayoutParams) {
            this.mParent = parent;
            this.mParentProps =
                    ViewProperties.fromViewGroup(
                            parent, parentLayoutParams, NO_OP_PENDING_LAYOUT_PARAMS);
        }

        ParentViewWrapper(
                ViewGroup parent,
                LayoutParams parentLayoutParams,
                PendingLayoutParams childLayoutParams) {
            this.mParent = parent;
            this.mParentProps =
                    ViewProperties.fromViewGroup(parent, parentLayoutParams, childLayoutParams);
        }

        ParentViewWrapper(ViewProperties parentProps) {
            this.mParent = null;
            this.mParentProps = parentProps;
        }

        ViewProperties getParentProperties() {
            return mParentProps;
        }

        /** If this class holds a {@link ViewGroup}, add {@code child} to it. */
        void maybeAddView(View child, LayoutParams layoutParams) {
            if (mParent != null) {
                mParent.addView(child, layoutParams);
            }
        }
    }

    /** Exception that will be thrown when applying a mutation to a {@link View} fails. */
    public static class ViewMutationException extends RuntimeException {
        public ViewMutationException(@NonNull String message) {
            super(message);
        }
    }

    /** Config class for ProtoLayoutInflater */
    public static final class Config {
        @NonNull private final Context mUiContext;
        @NonNull private final Layout mLayout;
        @NonNull private final ResourceResolvers mLayoutResourceResolvers;
        @Nullable private final Executor mLoadActionExecutor;
        @NonNull private final LoadActionListener mLoadActionListener;
        @NonNull private final Resources mRendererResources;
        @NonNull private final ProtoLayoutTheme mProtoLayoutTheme;
        @Nullable private final ProtoLayoutDynamicDataPipeline mDataPipeline;
        @NonNull private final String mClickableIdExtra;
        @Nullable private final ProtoLayoutExtensionViewProvider mExtensionViewProvider;
        private final boolean mAnimationEnabled;
        private final boolean mAllowLayoutChangingBindsWithoutDefault;

        Config(
                @NonNull Context uiContext,
                @NonNull Layout layout,
                @NonNull ResourceResolvers layoutResourceResolvers,
                @Nullable Executor loadActionExecutor,
                @NonNull LoadActionListener loadActionListener,
                @NonNull Resources rendererResources,
                @NonNull ProtoLayoutTheme protoLayoutTheme,
                @Nullable ProtoLayoutDynamicDataPipeline dataPipeline,
                @Nullable ProtoLayoutExtensionViewProvider extensionViewProvider,
                @NonNull String clickableIdExtra,
                boolean animationEnabled,
                boolean allowLayoutChangingBindsWithoutDefault) {
            this.mUiContext = uiContext;
            this.mLayout = layout;
            this.mLayoutResourceResolvers = layoutResourceResolvers;
            this.mLoadActionExecutor = loadActionExecutor;
            this.mLoadActionListener = loadActionListener;
            this.mRendererResources = rendererResources;
            this.mProtoLayoutTheme = protoLayoutTheme;
            this.mDataPipeline = dataPipeline;
            this.mAnimationEnabled = animationEnabled;
            this.mAllowLayoutChangingBindsWithoutDefault = allowLayoutChangingBindsWithoutDefault;
            this.mClickableIdExtra = clickableIdExtra;
            this.mExtensionViewProvider = extensionViewProvider;
        }

        /** A {@link Context} suitable for interacting with UI. */
        @NonNull
        public Context getUiContext() {
            return mUiContext;
        }

        /** The layout to be rendered. */
        @NonNull
        public Layout getLayout() {
            return mLayout;
        }

        /** Resolvers for the resources used for rendering this layout. */
        @NonNull
        public ResourceResolvers getLayoutResourceResolvers() {
            return mLayoutResourceResolvers;
        }

        /** Executor to dispatch loadActionListener on. */
        @Nullable
        public Executor getLoadActionExecutor() {
            return mLoadActionExecutor;
        }

        /** Listener for clicks that will cause contents to be reloaded. */
        @NonNull
        public LoadActionListener getLoadActionListener() {
            return mLoadActionListener;
        }

        /**
         * Renderer internal resources. This Resources object can be used to resolve Renderer's
         * resources.
         */
        @NonNull
        public Resources getRendererResources() {
            return mRendererResources;
        }

        /**
         * Theme to use for this ProtoLayoutInflater instance. This can be used to customise things
         * like the default font family.
         */
        @NonNull
        public ProtoLayoutTheme getProtoLayoutTheme() {
            return mProtoLayoutTheme;
        }

        /**
         * Pipeline for dynamic data. If null, the dynamic properties would not be registered for
         * update.
         */
        @Nullable
        public ProtoLayoutDynamicDataPipeline getDynamicDataPipeline() {
            return mDataPipeline;
        }

        /** ID for the Intent extra containing the ID of a Clickable. */
        @NonNull
        public String getClickableIdExtra() {
            return mClickableIdExtra;
        }

        /** View provider for the renderer extension. */
        @Nullable
        public ProtoLayoutExtensionViewProvider getExtensionViewProvider() {
            return mExtensionViewProvider;
        }

        /** Whether animation is enabled, which decides whether to load contentUpdateAnimations. */
        public boolean getAnimationEnabled() {
            return mAnimationEnabled;
        }

        /**
         * Whether a "layout changing" data bind can be applied without the "value_for_layout" field
         * being filled in. This is to support legacy apps which use layout-changing data binds
         * before the full support was built.
         */
        public boolean getAllowLayoutChangingBindsWithoutDefault() {
            return mAllowLayoutChangingBindsWithoutDefault;
        }

        /** Builder for the Config class. */
        public static final class Builder {
            @NonNull private final Context mUiContext;
            @NonNull private final Layout mLayout;
            @NonNull private final ResourceResolvers mLayoutResourceResolvers;
            @Nullable private Executor mLoadActionExecutor;
            @Nullable private LoadActionListener mLoadActionListener;
            @NonNull private Resources mRendererResources;
            @Nullable private ProtoLayoutTheme mProtoLayoutTheme;
            @Nullable private ProtoLayoutDynamicDataPipeline mDataPipeline = null;
            private boolean mAnimationEnabled = true;
            private boolean mAllowLayoutChangingBindsWithoutDefault = false;
            @Nullable private String mClickableIdExtra;

            @Nullable private ProtoLayoutExtensionViewProvider mExtensionViewProvider = null;
            /**
             * @param uiContext A {@link Context} suitable for interacting with UI with.
             * @param layout The layout to be rendered.
             * @param layoutResourceResolvers Resolvers for the resources used for rendering this
             *     layout.
             */
            public Builder(
                    @NonNull Context uiContext,
                    @NonNull Layout layout,
                    @NonNull ResourceResolvers layoutResourceResolvers) {
                this.mUiContext = uiContext;
                this.mRendererResources = uiContext.getResources();
                this.mLayout = layout;
                this.mLayoutResourceResolvers = layoutResourceResolvers;
            }

            /**
             * Sets the Executor to dispatch loadActionListener on. This is required when setting
             * {@link Builder#setLoadActionListener}.
             */
            @NonNull
            public Builder setLoadActionExecutor(@NonNull Executor loadActionExecutor) {
                this.mLoadActionExecutor = loadActionExecutor;
                return this;
            }

            /**
             * Sets the listener for clicks that will cause contents to be reloaded. Defaults to
             * no-op. This is required if the given layout contains a load action. When this is set,
             * it's also required to set an executor with {@link Builder#setLoadActionExecutor}.
             */
            @NonNull
            public Builder setLoadActionListener(@NonNull LoadActionListener loadActionListener) {
                this.mLoadActionListener = loadActionListener;
                return this;
            }

            /**
             * Sets the Renderer internal Resources object. This should be specified when loading
             * the renderer from a separate APK. This can usually be retrieved with {@link
             * android.content.pm.PackageManager#getResourcesForApplication(String)}. If not
             * specified, this is retrieved from the Ui Context.
             */
            @NonNull
            public Builder setRendererResources(@NonNull Resources rendererResources) {
                this.mRendererResources = rendererResources;
                return this;
            }

            /**
             * Sets the theme to use for this ProtoLayoutInflater instance. This can be used to
             * customise things like the default font family. If not set, the default theme is used.
             */
            @NonNull
            public Builder setProtoLayoutTheme(@NonNull ProtoLayoutTheme protoLayoutTheme) {
                this.mProtoLayoutTheme = protoLayoutTheme;
                return this;
            }

            /**
             * Sets the pipeline for dynamic data. If null, the dynamic properties would not be
             * registered for update.
             */
            @NonNull
            public Builder setDynamicDataPipeline(
                    @NonNull ProtoLayoutDynamicDataPipeline dataPipeline) {
                this.mDataPipeline = dataPipeline;
                return this;
            }

            /** Sets the view provider for the renderer extension. */
            @NonNull
            public Builder setExtensionViewProvider(
                    @NonNull ProtoLayoutExtensionViewProvider extensionViewProvider) {
                this.mExtensionViewProvider = extensionViewProvider;
                return this;
            }

            /**
             * Sets whether animation is enabled, which decides whether to load
             * contentUpdateAnimations. Defaults to true.
             */
            @NonNull
            public Builder setAnimationEnabled(boolean animationEnabled) {
                this.mAnimationEnabled = animationEnabled;
                return this;
            }

            /** Sets the ID for the Intent extra containing the ID of a Clickable. */
            @NonNull
            public Builder setClickableIdExtra(@NonNull String clickableIdExtra) {
                this.mClickableIdExtra = clickableIdExtra;
                return this;
            }

            /**
             * Sets whether a "layout changing" data bind can be applied without the
             * "value_for_layout" field being filled in. This is to support legacy apps which use
             * layout-changing data binds before the full support was built. Defaults to false.
             */
            @NonNull
            public Builder setAllowLayoutChangingBindsWithoutDefault(
                    boolean allowLayoutChangingBindsWithoutDefault) {
                this.mAllowLayoutChangingBindsWithoutDefault =
                        allowLayoutChangingBindsWithoutDefault;
                return this;
            }

            /** Builds a Config instance. */
            @NonNull
            public Config build() {
                if (mLoadActionListener != null && mLoadActionExecutor == null) {
                    throw new IllegalArgumentException(
                            "A loadActionExecutor should always be set if setting a"
                                    + " loadActionListener.");
                } else if (mLoadActionListener == null && mLoadActionExecutor != null) {
                    throw new IllegalArgumentException(
                            "A loadActionExecutor has been provided but no loadActionListener was"
                                    + " set.");
                }

                if (mLoadActionListener == null) {
                    mLoadActionListener = p -> {};
                }
                if (mProtoLayoutTheme == null) {
                    this.mProtoLayoutTheme = ProtoLayoutThemeImpl.defaultTheme(mUiContext);
                }

                return new Config(
                        mUiContext,
                        mLayout,
                        mLayoutResourceResolvers,
                        mLoadActionExecutor,
                        checkNotNull(mLoadActionListener),
                        mRendererResources,
                        checkNotNull(mProtoLayoutTheme),
                        mDataPipeline,
                        mExtensionViewProvider,
                        checkNotNull(mClickableIdExtra),
                        mAnimationEnabled,
                        mAllowLayoutChangingBindsWithoutDefault);
            }
        }
    }

    public ProtoLayoutInflater(@NonNull Config config) {
        // Wrap the Ui Context with a Theme from rendererResources, so that any implicit resource
        // reads using the R class from this package are successful.
        Theme rendererTheme = config.getRendererResources().newTheme();
        rendererTheme.setTo(config.getUiContext().getTheme());
        this.mUiContext = new ContextThemeWrapper(config.getUiContext(), rendererTheme);
        this.mProtoLayoutTheme = config.getProtoLayoutTheme();
        this.mProtoLayoutThemeContext =
                new ContextThemeWrapper(mUiContext, mProtoLayoutTheme.getTheme());
        this.mLayoutProto = config.getLayout();
        this.mLayoutResourceResolvers = config.getLayoutResourceResolvers();
        this.mLoadActionExecutor = config.getLoadActionExecutor();
        this.mLoadActionListener = config.getLoadActionListener();
        this.mDataPipeline = Optional.ofNullable(config.getDynamicDataPipeline());
        this.mAnimationEnabled = config.getAnimationEnabled();
        this.mAllowLayoutChangingBindsWithoutDefault =
                config.getAllowLayoutChangingBindsWithoutDefault();
        this.mClickableIdExtra = config.getClickableIdExtra();
        this.mExtensionViewProvider = config.getExtensionViewProvider();
    }

    private int safeDpToPx(float dp) {
        return round(max(0, dp) * mUiContext.getResources().getDisplayMetrics().density);
    }

    private int safeDpToPx(DpProp dpProp) {
        return safeDpToPx(dpProp.getValue());
    }

    @Nullable
    private static Float safeAspectRatioOrNull(
            ProportionalDimensionProp proportionalDimensionProp) {
        final int dividend = proportionalDimensionProp.getAspectRatioWidth();
        final int divisor = proportionalDimensionProp.getAspectRatioHeight();

        if (dividend <= 0 || divisor <= 0) {
            return null;
        }
        return (float) dividend / divisor;
    }

    private static Rect getSourceBounds(View v) {
        final int[] pos = new int[2];
        v.getLocationOnScreen(pos);

        return new Rect(
                /* left= */ pos[0],
                /* top= */ pos[1],
                /* right= */ pos[0] + v.getWidth(),
                /* bottom= */ pos[1] + v.getHeight());
    }

    /**
     * Generates a generic LayoutParameters for use by all components. This just defaults to setting
     * the width/height to WRAP_CONTENT.
     *
     * @return The default layout parameters.
     */
    private static LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    // dereference of possibly-null reference parent.getLayoutParams()
    @SuppressWarnings("nullness:dereference.of.nullable")
    private LayoutParams updateLayoutParamsInLinearLayout(
            LinearLayoutProperties linearLayoutProperties,
            LayoutParams layoutParams,
            ContainerDimension width,
            ContainerDimension height) {
        // This is a little bit fun. ProtoLayout's semantics is that dimension = expand should eat
        // all remaining space in that dimension, but not grow the parent. This is easy for standard
        // containers, but a little trickier in rows and columns on Android.
        //
        // A Row (LinearLayout) supports this with width=0 and weight>0. After doing a layout pass,
        // it will assign all remaining space to elements with width=0 and weight>0, biased by the
        // weight. This causes problems if there are two (or more) "expand" elements in a row, which
        // is itself set to WRAP_CONTENTS, and one of those elements has a measured width (e.g.
        // Text). In that case, the LinearLayout will measure the text, then ensure that all
        // elements with a weight set have their widths set according to the weight. For us, that
        // means that _all_ elements with expand=true will size themselves to the same width as the
        // Text, pushing out the bounds of the parent row. This happens on columns too, but of
        // course regarding height.
        //
        // To get around this, if an element with expand=true is added to a row that is WRAP_CONTENT
        // (e.g. a row with no explicit width, that is not expanded), we ignore the expand=true, and
        // set the inner element's width to WRAP_CONTENT too.

        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(layoutParams);
        LayoutParams parentLayoutParams = linearLayoutProperties.getRawLayoutParams();

        // Handle the width
        if (linearLayoutProperties.getOrientation() == LinearLayout.HORIZONTAL
                && width.getInnerCase() == InnerCase.EXPANDED_DIMENSION) {
            // If the parent container would not normally have "remaining space", ignore the
            // expand=true.
            if (parentLayoutParams.width == LayoutParams.WRAP_CONTENT) {
                linearLayoutParams.width = LayoutParams.WRAP_CONTENT;
            } else {
                linearLayoutParams.width = 0;
                float weight = width.getExpandedDimension().getLayoutWeight().getValue();
                linearLayoutParams.weight = weight != 0.0f ? weight : 1.0f;
            }
        } else {
            linearLayoutParams.width = dimensionToPx(width);
        }

        // And the height
        if (linearLayoutProperties.getOrientation() == LinearLayout.VERTICAL
                && height.getInnerCase() == InnerCase.EXPANDED_DIMENSION) {
            // If the parent container would not normally have "remaining space", ignore the
            // expand=true.
            if (parentLayoutParams.height == LayoutParams.WRAP_CONTENT) {
                linearLayoutParams.height = LayoutParams.WRAP_CONTENT;
            } else {
                linearLayoutParams.height = 0;
                float weight = height.getExpandedDimension().getLayoutWeight().getValue();
                linearLayoutParams.weight = weight != 0.0f ? weight : 1.0f;
            }
        } else {
            linearLayoutParams.height = dimensionToPx(height);
        }

        return linearLayoutParams;
    }

    private LayoutParams updateLayoutParams(
            ViewProperties viewProperties,
            LayoutParams layoutParams,
            ContainerDimension width,
            ContainerDimension height) {
        if (viewProperties instanceof LinearLayoutProperties) {
            // LinearLayouts have a bunch of messy caveats in ProtoLayout when their children can be
            // expanded; factor that case out to keep this clean.
            return updateLayoutParamsInLinearLayout(
                    (LinearLayoutProperties) viewProperties, layoutParams, width, height);
        } else {
            layoutParams.width = dimensionToPx(width);
            layoutParams.height = dimensionToPx(height);
        }

        return layoutParams;
    }

    private void resolveMinimumDimensions(
            View view, ContainerDimension width, ContainerDimension height) {
        if (width.getWrappedDimension().hasMinimumSize()) {
            view.setMinimumWidth(safeDpToPx(width.getWrappedDimension().getMinimumSize()));
        }

        if (height.getWrappedDimension().hasMinimumSize()) {
            view.setMinimumHeight(safeDpToPx(height.getWrappedDimension().getMinimumSize()));
        }
    }

    @VisibleForTesting()
    static int getFrameLayoutGravity(
            HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
        return horizontalAlignmentToGravity(horizontalAlignment)
                | verticalAlignmentToGravity(verticalAlignment);
    }

    @SuppressLint("RtlHardcoded")
    private static int horizontalAlignmentToGravity(HorizontalAlignment alignment) {
        switch (alignment) {
            case HORIZONTAL_ALIGN_START:
                return Gravity.START;
            case HORIZONTAL_ALIGN_CENTER:
                return Gravity.CENTER_HORIZONTAL;
            case HORIZONTAL_ALIGN_END:
                return Gravity.END;
            case HORIZONTAL_ALIGN_LEFT:
                return Gravity.LEFT;
            case HORIZONTAL_ALIGN_RIGHT:
                return Gravity.RIGHT;
            case UNRECOGNIZED:
            case HORIZONTAL_ALIGN_UNDEFINED:
                return HORIZONTAL_ALIGN_DEFAULT_GRAVITY;
        }

        return HORIZONTAL_ALIGN_DEFAULT_GRAVITY;
    }

    private static int verticalAlignmentToGravity(VerticalAlignment alignment) {
        switch (alignment) {
            case VERTICAL_ALIGN_TOP:
                return Gravity.TOP;
            case VERTICAL_ALIGN_CENTER:
                return Gravity.CENTER_VERTICAL;
            case VERTICAL_ALIGN_BOTTOM:
                return Gravity.BOTTOM;
            case UNRECOGNIZED:
            case VERTICAL_ALIGN_UNDEFINED:
                return VERTICAL_ALIGN_DEFAULT_GRAVITY;
        }

        return VERTICAL_ALIGN_DEFAULT_GRAVITY;
    }

    @ArcLayout.LayoutParams.VerticalAlignment
    private static int verticalAlignmentToArcVAlign(VerticalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case VERTICAL_ALIGN_TOP:
                return ArcLayout.LayoutParams.VERTICAL_ALIGN_OUTER;
            case VERTICAL_ALIGN_CENTER:
                return ArcLayout.LayoutParams.VERTICAL_ALIGN_CENTER;
            case VERTICAL_ALIGN_BOTTOM:
                return ArcLayout.LayoutParams.VERTICAL_ALIGN_INNER;
            case UNRECOGNIZED:
            case VERTICAL_ALIGN_UNDEFINED:
                return ARC_VERTICAL_ALIGN_DEFAULT;
        }

        return ARC_VERTICAL_ALIGN_DEFAULT;
    }

    private static ScaleType contentScaleModeToScaleType(ContentScaleMode contentScaleMode) {
        switch (contentScaleMode) {
            case CONTENT_SCALE_MODE_FIT:
                return ScaleType.FIT_CENTER;
            case CONTENT_SCALE_MODE_CROP:
                return ScaleType.CENTER_CROP;
            case CONTENT_SCALE_MODE_FILL_BOUNDS:
                return ScaleType.FIT_XY;
            case CONTENT_SCALE_MODE_UNDEFINED:
            case UNRECOGNIZED:
                return IMAGE_DEFAULT_SCALE_TYPE;
        }

        return IMAGE_DEFAULT_SCALE_TYPE;
    }

    private static int spanVerticalAlignmentToImgSpanAlignment(
            SpanVerticalAlignmentProp alignment) {
        switch (alignment.getValue()) {
            case SPAN_VERTICAL_ALIGN_TEXT_BASELINE:
                return ImageSpan.ALIGN_BASELINE;
            case SPAN_VERTICAL_ALIGN_BOTTOM:
                return ImageSpan.ALIGN_BOTTOM;
            case SPAN_VERTICAL_ALIGN_UNDEFINED:
            case UNRECOGNIZED:
                return SPAN_VERTICAL_ALIGN_DEFAULT;
        }

        return SPAN_VERTICAL_ALIGN_DEFAULT;
    }

    /**
     * Whether a font style is bold or not (has weight > 700). Note that this check is required,
     * even if you are using an explicitly bold font (e.g. Roboto-Bold), as Typeface still needs to
     * bold bit set to render properly.
     */
    private static boolean isBold(FontStyle fontStyle) {
        // Although this method could be a simple equality check against FONT_WEIGHT_BOLD, we list
        // all current cases here so that this will become a compile time error as soon as a new
        // FontWeight value is added to the schema. If this fails to build, then this means that an
        // int typeface style is no longer enough to represent all FontWeight values and a
        // customizable, per-weight text style must be introduced to ProtoLayoutInflater to handle
        // this. See b/176980535
        switch (fontStyle.getWeight().getValue()) {
            case FONT_WEIGHT_BOLD:
                return true;
            case FONT_WEIGHT_NORMAL:
            case FONT_WEIGHT_MEDIUM:
            case FONT_WEIGHT_UNDEFINED:
            case UNRECOGNIZED:
                return false;
        }

        return false;
    }

    private Typeface fontStyleToTypeface(FontStyle fontStyle) {
        FontSet fonts = mProtoLayoutTheme.getFontSet(fontStyle.getVariant().getValue().getNumber());

        switch (fontStyle.getWeight().getValue()) {
            case FONT_WEIGHT_BOLD:
                return fonts.getBoldFont();
            case FONT_WEIGHT_MEDIUM:
                return fonts.getMediumFont();
            case FONT_WEIGHT_NORMAL:
            case FONT_WEIGHT_UNDEFINED:
            case UNRECOGNIZED:
                return fonts.getNormalFont();
        }

        return fonts.getNormalFont();
    }

    private static int fontStyleToTypefaceStyle(FontStyle fontStyle) {
        final boolean isBold = isBold(fontStyle);
        final boolean isItalic = fontStyle.getItalic().getValue();

        if (isBold && isItalic) {
            return Typeface.BOLD_ITALIC;
        } else if (isBold) {
            return Typeface.BOLD;
        } else if (isItalic) {
            return Typeface.ITALIC;
        } else {
            return Typeface.NORMAL;
        }
    }

    @SuppressWarnings("nullness")
    private Typeface createTypeface(FontStyle fontStyle) {
        return Typeface.create(fontStyleToTypeface(fontStyle), fontStyleToTypefaceStyle(fontStyle));
    }

    private static MetricAffectingSpan createTypefaceSpan(FontStyle fontStyle) {
        return new StyleSpan(fontStyleToTypefaceStyle(fontStyle));
    }

    /**
     * Returns whether or not the default style bits in Typeface can be used, or if we need to add
     * bold/italic flags there.
     */
    private static boolean hasDefaultTypefaceStyle(FontStyle fontStyle) {
        return !fontStyle.getItalic().getValue() && !isBold(fontStyle);
    }

    private float toPx(SpProp spField) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                spField.getValue(),
                mUiContext.getResources().getDisplayMetrics());
    }

    private void applyFontStyle(
            FontStyle style,
            TextView textView,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        // Note: Underline must be applied as a Span to work correctly (as opposed to using
        // TextPaint#setTextUnderline). This is applied in the caller instead.

        // Need to supply typefaceStyle when creating the typeface (will select specialist
        // bold/italic typefaces), *and* when setting the typeface (will set synthetic bold/italic
        // flags in Paint if they're not supported by the given typeface).
        textView.setTypeface(createTypeface(style), fontStyleToTypefaceStyle(style));

        if (style.hasSize()) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.getSize().getValue());
        }

        if (style.hasLetterSpacing()) {
            textView.setLetterSpacing(style.getLetterSpacing().getValue());
        }

        if (style.hasColor()) {
            handleProp(style.getColor(), textView::setTextColor, posId, pipelineMaker);
        } else {
            textView.setTextColor(TEXT_COLOR_DEFAULT);
        }
    }

    private void applyFontStyle(FontStyle style, CurvedTextView textView) {
        // Need to supply typefaceStyle when creating the typeface (will select specialist
        // bold/italic typefaces), *and* when setting the typeface (will set synthetic bold/italic
        // flags in Paint if they're not supported by the given typeface).
        textView.setTypeface(createTypeface(style), fontStyleToTypefaceStyle(style));

        // underline. We can implement this later by drawing a line under the text ourselves though.

        if (style.hasSize()) {
            textView.setTextSize(toPx(style.getSize()));
        }
    }

    void dispatchLaunchActionIntent(Intent i) {
        ActivityInfo ai = i.resolveActivityInfo(mUiContext.getPackageManager(), /* flags= */ 0);

        if (ai != null && ai.exported && (ai.permission == null || ai.permission.isEmpty())) {
            mUiContext.startActivity(i);
        }
    }

    private void applyClickable(View view, Clickable clickable) {
        view.setTag(R.id.clickable_id_tag, clickable.getId());

        boolean hasAction = false;
        switch (clickable.getOnClick().getValueCase()) {
            case LAUNCH_ACTION:
                Intent i =
                        buildLaunchActionIntent(
                                clickable.getOnClick().getLaunchAction(),
                                clickable.getId(),
                                mClickableIdExtra);
                if (i != null) {
                    hasAction = true;
                    view.setOnClickListener(
                            v -> {
                                i.setSourceBounds(getSourceBounds(view));
                                dispatchLaunchActionIntent(i);
                            });
                }
                break;
            case LOAD_ACTION:
                hasAction = true;
                if (mLoadActionExecutor == null) {
                    Log.w(TAG, "Ignoring load action since an executor has not been provided.");
                    break;
                }
                view.setOnClickListener(
                        v ->
                                checkNotNull(mLoadActionExecutor)
                                        .execute(
                                                () ->
                                                        mLoadActionListener.onClick(
                                                                buildState(
                                                                        clickable
                                                                                .getOnClick()
                                                                                .getLoadAction(),
                                                                        clickable
                                                                                .getId()))));
                break;
            case VALUE_NOT_SET:
                break;
        }

        if (hasAction) {
            // Apply ripple effect Resolve selectableItemBackground against the mUiContext theme,
            // which provides the drawable. Note that this is not customizable by the
            // ProtoLayoutTheme.
            TypedValue outValue = new TypedValue();
            boolean isValid =
                    mUiContext
                            .getTheme()
                            .resolveAttribute(
                                    android.R.attr.selectableItemBackground,
                                    outValue,
                                    /* resolveRefs= */ true);
            if (isValid) {
                view.setForeground(mUiContext.getDrawable(outValue.resourceId));
            } else {
                Log.e(
                        TAG,
                        "Could not resolve android.R.attr.selectableItemBackground from Ui"
                                + " Context.");
            }
        }
    }

    private void applyPadding(View view, Padding padding) {
        if (padding.getRtlAware().getValue()) {
            view.setPaddingRelative(
                    safeDpToPx(padding.getStart()),
                    safeDpToPx(padding.getTop()),
                    safeDpToPx(padding.getEnd()),
                    safeDpToPx(padding.getBottom()));
        } else {
            view.setPadding(
                    safeDpToPx(padding.getStart()),
                    safeDpToPx(padding.getTop()),
                    safeDpToPx(padding.getEnd()),
                    safeDpToPx(padding.getBottom()));
        }
    }

    private GradientDrawable applyBackground(
            View view,
            Background background,
            @Nullable GradientDrawable drawable,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (drawable == null) {
            drawable = new GradientDrawable();
        }

        if (background.hasColor()) {
            handleProp(background.getColor(), drawable::setColor, posId, pipelineMaker);
        }

        if (background.hasCorner()) {
            final int radiusPx = safeDpToPx(background.getCorner().getRadius());
            if (radiusPx != 0) {
                drawable.setCornerRadius(radiusPx);
                view.setClipToOutline(true);
                view.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            }
        }

        return drawable;
    }

    private GradientDrawable applyBorder(
            Border border,
            @Nullable GradientDrawable drawable,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (drawable == null) {
            drawable = new GradientDrawable();
        }

        GradientDrawable finalDrawable = drawable;
        int width = safeDpToPx(border.getWidth());
        handleProp(
                border.getColor(),
                borderColor -> finalDrawable.setStroke(width, borderColor),
                posId,
                pipelineMaker);

        return drawable;
    }

    private View applyModifiers(
            View view,
            Modifiers modifiers,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (modifiers.hasClickable()) {
            applyClickable(view, modifiers.getClickable());
        }

        if (modifiers.hasSemantics()) {
            applySemantics(view, modifiers.getSemantics(), posId, pipelineMaker);
        }

        if (modifiers.hasPadding()) {
            applyPadding(view, modifiers.getPadding());
        }

        GradientDrawable backgroundDrawable = null;

        if (modifiers.hasBackground()) {
            backgroundDrawable =
                    applyBackground(
                            view,
                            modifiers.getBackground(),
                            backgroundDrawable,
                            posId,
                            pipelineMaker);
        }

        if (modifiers.hasBorder()) {
            backgroundDrawable =
                    applyBorder(modifiers.getBorder(), backgroundDrawable, posId, pipelineMaker);
        }

        if (backgroundDrawable != null) {
            view.setBackground(backgroundDrawable);
        }

        if (mAnimationEnabled && modifiers.hasContentUpdateAnimation()) {
            pipelineMaker.ifPresent(
                    p ->
                            p.storeAnimatedVisibilityFor(
                                    posId, modifiers.getContentUpdateAnimation()));
        }

        return view;
    }

    @SuppressWarnings("RestrictTo")
    static AnimationSet getEnterAnimations(
            @NonNull EnterTransition enterTransition, @NonNull View view) {
        AnimationSet animations = new AnimationSet(/* shareInterpolator= */ false);
        if (enterTransition.hasFadeIn()) {
            FadeInTransition fadeIn = enterTransition.getFadeIn();
            AlphaAnimation alphaAnimation =
                    new AlphaAnimation(fadeIn.getInitialAlpha(), FADE_IN_TARGET_ALPHA);

            // If it doesn't exist, this will be default object.
            AnimationSpec spec = fadeIn.getAnimationSpec();

            AnimationsHelper.applyAnimationSpecToAnimation(alphaAnimation, spec);
            animations.addAnimation(alphaAnimation);
        }

        if (enterTransition.hasSlideIn()) {
            SlideInTransition slideIn = enterTransition.getSlideIn();

            // If it doesn't exist, this will be default object.
            AnimationSpec spec = slideIn.getAnimationSpec();

            float fromXDelta = 0;
            float toXDelta = 0;
            float fromYDelta = 0;
            float toYDelta = 0;

            switch (slideIn.getDirectionValue()) {
                case SlideDirection.SLIDE_DIRECTION_UNDEFINED_VALUE:
                    // Do the same as for horizontal as that is default.
                case SlideDirection.SLIDE_DIRECTION_LEFT_TO_RIGHT_VALUE:
                case SlideDirection.SLIDE_DIRECTION_RIGHT_TO_LEFT_VALUE:
                    fromXDelta = getInitialOffsetOrDefaultX(slideIn, view);
                    break;
                case SlideDirection.SLIDE_DIRECTION_TOP_TO_BOTTOM_VALUE:
                case SlideDirection.SLIDE_DIRECTION_BOTTOM_TO_TOP_VALUE:
                    fromYDelta = getInitialOffsetOrDefaultY(slideIn, view);
                    break;
                default:
                    break;
            }

            TranslateAnimation translateAnimation =
                    new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
            AnimationsHelper.applyAnimationSpecToAnimation(translateAnimation, spec);
            animations.addAnimation(translateAnimation);
        }
        return animations;
    }

    @SuppressWarnings("RestrictTo")
    static AnimationSet getExitAnimations(
            @NonNull ExitTransition exitTransition, @NonNull View view) {
        AnimationSet animations = new AnimationSet(/* shareInterpolator= */ false);
        if (exitTransition.hasFadeOut()) {
            FadeOutTransition fadeOut = exitTransition.getFadeOut();
            AlphaAnimation alphaAnimation =
                    new AlphaAnimation(FADE_OUT_INITIAL_ALPHA, fadeOut.getTargetAlpha());

            // If it doesn't exist, this will be default object.
            AnimationSpec spec = fadeOut.getAnimationSpec();

            // Indefinite Exit animations aren't allowed.
            if (!spec.hasRepeatable() || spec.getRepeatable().getIterations() != 0) {
                AnimationsHelper.applyAnimationSpecToAnimation(alphaAnimation, spec);
                animations.addAnimation(alphaAnimation);
            }
        }

        if (exitTransition.hasSlideOut()) {
            SlideOutTransition slideOut = exitTransition.getSlideOut();

            // If it doesn't exist, this will be default object.
            AnimationSpec spec = slideOut.getAnimationSpec();
            // Indefinite Exit animations aren't allowed.
            if (!spec.hasRepeatable() || spec.getRepeatable().getIterations() != 0) {
                float fromXDelta = 0;
                float toXDelta = 0;
                float fromYDelta = 0;
                float toYDelta = 0;

                switch (slideOut.getDirectionValue()) {
                    case SlideDirection.SLIDE_DIRECTION_UNDEFINED_VALUE:
                        // Do the same as for horizontal as that is default.
                    case SlideDirection.SLIDE_DIRECTION_LEFT_TO_RIGHT_VALUE:
                    case SlideDirection.SLIDE_DIRECTION_RIGHT_TO_LEFT_VALUE:
                        toXDelta = getTargetOffsetOrDefaultX(slideOut, view);
                        break;
                    case SlideDirection.SLIDE_DIRECTION_TOP_TO_BOTTOM_VALUE:
                    case SlideDirection.SLIDE_DIRECTION_BOTTOM_TO_TOP_VALUE:
                        toYDelta = getTargetOffsetOrDefaultY(slideOut, view);
                        break;
                    default:
                        break;
                }

                TranslateAnimation translateAnimation =
                        new TranslateAnimation(fromXDelta, toXDelta, fromYDelta, toYDelta);
                AnimationsHelper.applyAnimationSpecToAnimation(translateAnimation, spec);
                animations.addAnimation(translateAnimation);
            }
        }
        return animations;
    }

    /**
     * Returns offset from SlideInTransition if it's set. Otherwise, returns the default value which
     * * is sliding to the left or right parent edge, depending on the direction.
     */
    private static float getInitialOffsetOrDefaultX(
            @NonNull SlideInTransition slideIn, @NonNull View view) {
        int sign =
                slideIn.getDirectionValue() == SlideDirection.SLIDE_DIRECTION_LEFT_TO_RIGHT_VALUE
                        ? -1
                        : 1;
        if (slideIn.hasInitialSlideBound()) {

            switch (slideIn.getInitialSlideBound().getInnerCase()) {
                case LINEAR_BOUND:
                    return slideIn.getInitialSlideBound().getLinearBound().getOffsetDp() * sign;
                case PARENT_BOUND:
                    if (slideIn.getInitialSlideBound().getParentBound().getSnapTo()
                            == SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_OUTSIDE) {
                        return (sign == -1 ? (view.getLeft() + view.getWidth()) : view.getRight())
                                * sign;
                    }
                    // fall through
                case INNER_NOT_SET:
                    break;
            }
        }
        return (sign == -1 ? view.getLeft() : (view.getRight() - view.getWidth())) * sign;
    }

    /**
     * Returns offset from SlideInTransition if it's set. Otherwise, returns the default value which
     * * is sliding to the left or right parent edge, depending on the direction.
     */
    private static float getInitialOffsetOrDefaultY(
            @NonNull SlideInTransition slideIn, @NonNull View view) {
        int sign =
                slideIn.getDirectionValue() == SlideDirection.SLIDE_DIRECTION_TOP_TO_BOTTOM_VALUE
                        ? -1
                        : 1;
        if (slideIn.hasInitialSlideBound()) {

            switch (slideIn.getInitialSlideBound().getInnerCase()) {
                case LINEAR_BOUND:
                    return slideIn.getInitialSlideBound().getLinearBound().getOffsetDp() * sign;
                case PARENT_BOUND:
                    if (slideIn.getInitialSlideBound().getParentBound().getSnapTo()
                            == SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_OUTSIDE) {
                        return (sign == -1 ? (view.getTop() + view.getHeight()) : view.getBottom())
                                * sign;
                    }
                    // fall through
                case INNER_NOT_SET:
                    break;
            }
        }
        return (sign == -1 ? view.getTop() : (view.getBottom() - view.getHeight())) * sign;
    }

    /**
     * Returns offset from SlideOutTransition if it's set. Otherwise, returns the default value
     * which is sliding to the left or right parent edge, depending on the direction.
     */
    private static float getTargetOffsetOrDefaultX(
            @NonNull SlideOutTransition slideOut, @NonNull View view) {
        int sign =
                slideOut.getDirectionValue() == SlideDirection.SLIDE_DIRECTION_LEFT_TO_RIGHT_VALUE
                        ? 1
                        : -1;
        if (slideOut.hasTargetSlideBound()) {

            switch (slideOut.getTargetSlideBound().getInnerCase()) {
                case LINEAR_BOUND:
                    return slideOut.getTargetSlideBound().getLinearBound().getOffsetDp() * sign;
                case PARENT_BOUND:
                    if (slideOut.getTargetSlideBound().getParentBound().getSnapTo()
                            == SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_OUTSIDE) {
                        return (sign == -1 ? (view.getLeft() + view.getWidth()) : view.getRight())
                                * sign;
                    }
                    // fall through
                case INNER_NOT_SET:
                    break;
            }
        }
        return (sign == 1 ? view.getLeft() : (view.getRight() - view.getWidth())) * sign;
    }

    /**
     * Returns offset from SlideOutTransition if it's set. Otherwise, returns the default value
     * which is sliding to the top or bottom parent edge, depending on the direction.
     */
    private static float getTargetOffsetOrDefaultY(
            @NonNull SlideOutTransition slideOut, @NonNull View view) {
        int sign =
                slideOut.getDirectionValue() == SlideDirection.SLIDE_DIRECTION_TOP_TO_BOTTOM_VALUE
                        ? 1
                        : -1;
        if (slideOut.hasTargetSlideBound()) {

            switch (slideOut.getTargetSlideBound().getInnerCase()) {
                case LINEAR_BOUND:
                    return slideOut.getTargetSlideBound().getLinearBound().getOffsetDp() * sign;
                case PARENT_BOUND:
                    if (slideOut.getTargetSlideBound().getParentBound().getSnapTo()
                            == SlideParentSnapOption.SLIDE_PARENT_SNAP_TO_OUTSIDE) {
                        return (sign == -1 ? (view.getTop() + view.getHeight()) : view.getBottom())
                                * sign;
                    }
                    // fall through
                case INNER_NOT_SET:
                    break;
            }
        }
        return (sign == 1 ? view.getTop() : (view.getBottom() - view.getHeight())) * sign;
    }

    // This is a little nasty; ArcLayout.Widget is just an interface, so we have no guarantee that
    // the instance also extends View (as it should). Instead, just take a View in and rename this,
    // and check that it's an ArcLayout.Widget internally.
    private View applyModifiersToArcLayoutView(
            View view,
            ArcModifiers modifiers,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (!(view instanceof ArcLayout.Widget)) {
            Log.e(
                    TAG,
                    "applyModifiersToArcLayoutView should only be called with an ArcLayout.Widget");
            return view;
        }

        if (modifiers.hasClickable()) {
            applyClickable(view, modifiers.getClickable());
        }

        if (modifiers.hasSemantics()) {
            applySemantics(view, modifiers.getSemantics(), posId, pipelineMaker);
        }

        return view;
    }

    private static int textAlignToAndroidGravity(TextAlignment alignment) {
        switch (alignment) {
            case TEXT_ALIGN_START:
                return Gravity.START;
            case TEXT_ALIGN_CENTER:
                return Gravity.CENTER_HORIZONTAL;
            case TEXT_ALIGN_END:
                return Gravity.END;
            case TEXT_ALIGN_UNDEFINED:
            case UNRECOGNIZED:
                return TEXT_ALIGN_DEFAULT;
        }

        return TEXT_ALIGN_DEFAULT;
    }

    @Nullable
    private static TruncateAt textTruncationToEllipsize(TextOverflowProp type) {
        switch (type.getValue()) {
            case TEXT_OVERFLOW_TRUNCATE:
                // A null TruncateAt disables adding an ellipsis.
                return null;
            case TEXT_OVERFLOW_ELLIPSIZE_END:
                return TruncateAt.END;
            case TEXT_OVERFLOW_MARQUEE:
                return TruncateAt.MARQUEE;
            case TEXT_OVERFLOW_UNDEFINED:
            case UNRECOGNIZED:
                return TEXT_OVERFLOW_DEFAULT;
        }

        return TEXT_OVERFLOW_DEFAULT;
    }

    @ArcLayout.AnchorType
    private static int anchorTypeToAnchorPos(ArcAnchorType type) {
        switch (type) {
            case ARC_ANCHOR_START:
                return ArcLayout.ANCHOR_START;
            case ARC_ANCHOR_CENTER:
                return ArcLayout.ANCHOR_CENTER;
            case ARC_ANCHOR_END:
                return ArcLayout.ANCHOR_END;
            case ARC_ANCHOR_UNDEFINED:
            case UNRECOGNIZED:
                return ARC_ANCHOR_DEFAULT;
        }

        return ARC_ANCHOR_DEFAULT;
    }

    @SizedArcContainer.LayoutParams.AngularAlignment
    private static int angularAlignmentProtoToAngularAlignment(AngularAlignment angularAlignment) {
        switch (angularAlignment) {
            case ANGULAR_ALIGNMENT_START:
                return SizedArcContainer.LayoutParams.ANGULAR_ALIGNMENT_START;
            case ANGULAR_ALIGNMENT_CENTER:
                return SizedArcContainer.LayoutParams.ANGULAR_ALIGNMENT_CENTER;
            case ANGULAR_ALIGNMENT_END:
                return SizedArcContainer.LayoutParams.ANGULAR_ALIGNMENT_END;
            case ANGULAR_ALIGNMENT_UNDEFINED:
            case UNRECOGNIZED:
                return ANGULAR_ALIGNMENT_DEFAULT;
        }

        return ANGULAR_ALIGNMENT_DEFAULT;
    }

    private int dimensionToPx(ContainerDimension containerDimension) {
        switch (containerDimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return safeDpToPx(containerDimension.getLinearDimension());
            case EXPANDED_DIMENSION:
                return LayoutParams.MATCH_PARENT;
            case WRAPPED_DIMENSION:
                return LayoutParams.WRAP_CONTENT;
            case INNER_NOT_SET:
                return dimensionToPx(CONTAINER_DIMENSION_DEFAULT);
        }

        return dimensionToPx(CONTAINER_DIMENSION_DEFAULT);
    }

    private static int extractTextColorArgb(FontStyle fontStyle) {
        if (fontStyle.hasColor()) {
            return fontStyle.getColor().getArgb();
        } else {
            return TEXT_COLOR_DEFAULT;
        }
    }

    /**
     * Returns an Android {@link Intent} that can perform the action defined in the given layout
     * {@link LaunchAction}.
     */
    @Nullable
    public static Intent buildLaunchActionIntent(
            @NonNull LaunchAction launchAction,
            @NonNull String clickableId,
            @NonNull String clickableIdExtra) {
        if (launchAction.hasAndroidActivity()) {
            AndroidActivity activity = launchAction.getAndroidActivity();
            Intent i =
                    new Intent().setClassName(activity.getPackageName(), activity.getClassName());
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (!clickableId.isEmpty() && !clickableIdExtra.isEmpty()) {
                i.putExtra(clickableIdExtra, clickableId);
            }

            for (Map.Entry<String, AndroidExtra> entry : activity.getKeyToExtraMap().entrySet()) {
                if (entry.getValue().hasStringVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getStringVal().getValue());
                } else if (entry.getValue().hasIntVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getIntVal().getValue());
                } else if (entry.getValue().hasLongVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getLongVal().getValue());
                } else if (entry.getValue().hasDoubleVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getDoubleVal().getValue());
                } else if (entry.getValue().hasBooleanVal()) {
                    i.putExtra(entry.getKey(), entry.getValue().getBooleanVal().getValue());
                }
            }

            return i;
        }

        return null;
    }

    static State buildState(LoadAction loadAction, String clickableId) {
        // Get the state specified by the provider and add the last clicked clickable's ID to it.
        return loadAction.getRequestState().toBuilder().setLastClickableId(clickableId).build();
    }

    @Nullable
    private InflatedView inflateColumn(
            ParentViewWrapper parentViewWrapper,
            Column column,
            String columnPosId,
            boolean includeChildren,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        ContainerDimension width =
                column.hasWidth() ? column.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height =
                column.hasHeight() ? column.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, column.getContentsList())) {
            Log.w(TAG, "Column set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        LinearLayout linearLayout = new LinearLayout(mUiContext);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        linearLayout.setGravity(
                horizontalAlignmentToGravity(column.getHorizontalAlignment().getValue()));

        layoutParams =
                updateLayoutParams(
                        parentViewWrapper.getParentProperties(), layoutParams, width, height);
        resolveMinimumDimensions(linearLayout, width, height);

        View wrappedView =
                applyModifiers(linearLayout, column.getModifiers(), columnPosId, pipelineMaker);

        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        if (includeChildren) {
            inflateChildElements(
                    linearLayout,
                    layoutParams,
                    NO_OP_PENDING_LAYOUT_PARAMS,
                    column.getContentsList(),
                    columnPosId,
                    layoutInfoBuilder,
                    pipelineMaker);
            layoutInfoBuilder.removeSubtree(columnPosId);
        }

        int numMissingChildren = includeChildren ? 0 : column.getContentsCount();
        return new InflatedView(
                wrappedView,
                parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(layoutParams),
                NO_OP_PENDING_LAYOUT_PARAMS,
                numMissingChildren);
    }

    @Nullable
    private InflatedView inflateRow(
            ParentViewWrapper parentViewWrapper,
            Row row,
            String rowPosId,
            boolean includeChildren,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        ContainerDimension width = row.hasWidth() ? row.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height = row.hasHeight() ? row.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, row.getContentsList())) {
            Log.w(TAG, "Row set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        LinearLayout linearLayout = new LinearLayout(mUiContext);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        linearLayout.setGravity(verticalAlignmentToGravity(row.getVerticalAlignment().getValue()));

        layoutParams =
                updateLayoutParams(
                        parentViewWrapper.getParentProperties(), layoutParams, width, height);
        resolveMinimumDimensions(linearLayout, width, height);

        View wrappedView =
                applyModifiers(linearLayout, row.getModifiers(), rowPosId, pipelineMaker);

        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        if (includeChildren) {
            inflateChildElements(
                    linearLayout,
                    layoutParams,
                    NO_OP_PENDING_LAYOUT_PARAMS,
                    row.getContentsList(),
                    rowPosId,
                    layoutInfoBuilder,
                    pipelineMaker);
            layoutInfoBuilder.removeSubtree(rowPosId);
        }

        int numMissingChildren = includeChildren ? 0 : row.getContentsCount();
        return new InflatedView(
                wrappedView,
                parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(layoutParams),
                NO_OP_PENDING_LAYOUT_PARAMS,
                numMissingChildren);
    }

    // dereference of possibly-null reference lp
    @SuppressWarnings("nullness:dereference.of.nullable")
    @Nullable
    private InflatedView inflateBox(
            ParentViewWrapper parentViewWrapper,
            Box box,
            String boxPosId,
            boolean includeChildren,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        ContainerDimension width = box.hasWidth() ? box.getWidth() : CONTAINER_DIMENSION_DEFAULT;
        ContainerDimension height = box.hasHeight() ? box.getHeight() : CONTAINER_DIMENSION_DEFAULT;

        if (!canMeasureContainer(width, height, box.getContentsList())) {
            Log.w(TAG, "Box set to wrap but contents are unmeasurable. Ignoring.");
            return null;
        }

        FrameLayout frame = new FrameLayout(mUiContext);

        LayoutParams layoutParams = generateDefaultLayoutParams();

        layoutParams =
                updateLayoutParams(
                        parentViewWrapper.getParentProperties(), layoutParams, width, height);
        resolveMinimumDimensions(frame, width, height);

        int gravity =
                getFrameLayoutGravity(
                        box.getHorizontalAlignment().getValue(),
                        box.getVerticalAlignment().getValue());
        PendingFrameLayoutParams childLayoutParams = new PendingFrameLayoutParams(gravity);

        View wrappedView = applyModifiers(frame, box.getModifiers(), boxPosId, pipelineMaker);

        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        if (includeChildren) {
            inflateChildElements(
                    frame,
                    layoutParams,
                    childLayoutParams,
                    box.getContentsList(),
                    boxPosId,
                    layoutInfoBuilder,
                    pipelineMaker);
            layoutInfoBuilder.removeSubtree(boxPosId);
        }

        // We can't set layout gravity to a FrameLayout ahead of time (and foregroundGravity only
        // sets the gravity of the foreground Drawable). Go and apply gravity to the child.
        try {
            applyGravityToFrameLayoutChildren(frame, gravity);
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Error applying Gravity to FrameLayout children.", ex);
        }

        // HACK: FrameLayout has a bug in it. If we add one WRAP_CONTENT child, and one MATCH_PARENT
        // child, the expected behaviour is that the FrameLayout sizes itself to fit the
        // WRAP_CONTENT child (e.g. a TextView), then the MATCH_PARENT child is forced to the same
        // size as the outer FrameLayout (and hence, the size of the TextView, after accounting for
        // padding etc). Because of a bug though, this doesn't happen; instead, the MATCH_PARENT
        // child will just keep its intrinsic size. This is because FrameLayout only forces
        // MATCH_PARENT children to a given size if there are _more than one_ of them (see the
        // bottom of FrameLayout#onMeasure).
        //
        // To work around this (without copying the whole of FrameLayout just to change a "1" to
        // "0"),
        // we add a Space element in if there is one MATCH_PARENT child. This has a tiny cost to the
        // measure pass, and negligible cost to layout/draw (since it doesn't take part in those
        // passes).
        int numMatchParentChildren = 0;
        for (int i = 0; i < frame.getChildCount(); i++) {
            LayoutParams lp = frame.getChildAt(i).getLayoutParams();
            if (lp.width == LayoutParams.MATCH_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
                numMatchParentChildren++;
            }
        }

        if (numMatchParentChildren == 1) {
            Space hackSpace = new Space(mUiContext);
            LayoutParams hackSpaceLp =
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            frame.addView(hackSpace, hackSpaceLp);
        }

        int numMissingChildren = includeChildren ? 0 : box.getContentsCount();
        return new InflatedView(
                wrappedView,
                parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(layoutParams),
                childLayoutParams,
                numMissingChildren);
    }

    @Nullable
    private InflatedView inflateSpacer(
            ParentViewWrapper parentViewWrapper,
            Spacer spacer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        LayoutParams layoutParams = generateDefaultLayoutParams();

        // Initialize the size wrapper here, if needed. This simplifies the logic below when
        // creating the actual Spacer and adding it to its parent...
        FrameLayout sizeWrapper = null;
        if (needsSizeWrapper(spacer.getWidth()) || needsSizeWrapper(spacer.getHeight())) {
            sizeWrapper = new FrameLayout(mUiContext);
            LayoutParams spaceWrapperLayoutParams = generateDefaultLayoutParams();
            spaceWrapperLayoutParams.width = LayoutParams.WRAP_CONTENT;
            spaceWrapperLayoutParams.height = LayoutParams.WRAP_CONTENT;

            // Technically speaking, this logic isn't 100% accurate. In legacy size-changing mode
            // (before
            // value_for_layout was introduced), apps may not set value_for_layout. That's fine; the
            // needsSizeWrapper checks will catch that. It's possible that one dimension has
            // value_for_layout set though, and the other relies on legacy size changing mode. We
            // don't deal with that case; if value_for_layout is present on one dimension, and both
            // are dynamic, then it must be set on both dimensions.
            if (spacer.getWidth().getLinearDimension().hasDynamicValue()) {
                float widthForLayout = spacer.getWidth().getLinearDimension().getValueForLayout();
                spaceWrapperLayoutParams.width = safeDpToPx(widthForLayout);
            }

            if (spacer.getHeight().getLinearDimension().hasDynamicValue()) {
                float heightForLayout = spacer.getHeight().getLinearDimension().getValueForLayout();
                spaceWrapperLayoutParams.height = safeDpToPx(heightForLayout);
            }

            int gravity =
                    horizontalAlignmentToGravity(
                                    spacer.getWidth()
                                            .getLinearDimension()
                                            .getHorizontalAlignmentForLayout())
                            | verticalAlignmentToGravity(
                                    spacer.getHeight()
                                            .getLinearDimension()
                                            .getVerticalAlignmentForLayout());
            FrameLayout.LayoutParams frameLayoutLayoutParams =
                    new FrameLayout.LayoutParams(layoutParams);
            frameLayoutLayoutParams.gravity = gravity;
            layoutParams = frameLayoutLayoutParams;

            parentViewWrapper.maybeAddView(sizeWrapper, spaceWrapperLayoutParams);

            parentViewWrapper = new ParentViewWrapper(sizeWrapper, spaceWrapperLayoutParams);
        }

        // Modifiers cannot be applied to android's Space, so use a plain View if this Spacer has
        // modifiers.
        View view;
        if (spacer.hasModifiers()) {
            view =
                    applyModifiers(
                            new View(mUiContext), spacer.getModifiers(), posId, pipelineMaker);

            // Currently, a spacer can only have a known size, not wrap or expand. Because of that,
            // we don't need to use updateLayoutParams (it only exists to special-case expand() in a
            // linear layout). Just go and set the LayoutParams directly here. First though, init
            // the layout params to 0 (so we don't get strange behaviour before the first data
            // pipeline update).
            layoutParams.width = 0;
            layoutParams.height = 0;

            // The View needs to be added before any of the *Prop messages are wired up.
            // View#getLayoutParams will return null if the View has not been added to a container
            // yet
            // (since the LayoutParams are technically managed by the parent).
            parentViewWrapper.maybeAddView(view, layoutParams);

            handleProp(
                    spacer.getWidth().getLinearDimension(),
                    width -> {
                        LayoutParams lp = view.getLayoutParams();
                        if (lp == null) {
                            Log.e(TAG, "LayoutParams was null when updating spacer width");
                            return;
                        }

                        lp.width = safeDpToPx(width);
                        view.requestLayout();
                    },
                    posId,
                    pipelineMaker);

            handleProp(
                    spacer.getHeight().getLinearDimension(),
                    height -> {
                        LayoutParams lp = view.getLayoutParams();
                        if (lp == null) {
                            Log.e(TAG, "LayoutParams was null when updating spacer height");
                            return;
                        }

                        lp.height = safeDpToPx(height);
                        view.requestLayout();
                    },
                    posId,
                    pipelineMaker);
        } else {
            view = new Space(mUiContext);
            handleProp(
                    spacer.getWidth().getLinearDimension(),
                    width -> view.setMinimumWidth(safeDpToPx(width)),
                    posId,
                    pipelineMaker);
            handleProp(
                    spacer.getHeight().getLinearDimension(),
                    height -> view.setMinimumHeight(safeDpToPx(height)),
                    posId,
                    pipelineMaker);
            parentViewWrapper.maybeAddView(view, layoutParams);
        }

        if (sizeWrapper != null) {
            return new InflatedView(
                    sizeWrapper,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(layoutParams));
        } else {
            return new InflatedView(
                    view,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(layoutParams));
        }
    }

    @Nullable
    private InflatedView inflateArcSpacer(
            ParentViewWrapper parentViewWrapper,
            ArcSpacer spacer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        float lengthDegrees = 0;
        int thicknessPx = safeDpToPx(spacer.getThickness());
        WearCurvedSpacer space = new WearCurvedSpacer(mUiContext);
        ArcLayout.LayoutParams layoutParams =
                new ArcLayout.LayoutParams(generateDefaultLayoutParams());

        if (spacer.hasAngularLength()) {
            final ArcSpacerLength angularLength = spacer.getAngularLength();
            switch (angularLength.getInnerCase()) {
                case DEGREES:
                    lengthDegrees = max(0, angularLength.getDegrees().getValue());
                    break;

                case EXPANDED_ANGULAR_DIMENSION:
                    {
                        float weight =
                                angularLength
                                        .getExpandedAngularDimension()
                                        .getLayoutWeight()
                                        .getValue();
                        if (weight == 0 && thicknessPx == 0) {
                            return null;
                        }
                        layoutParams.setWeight(weight);

                        space.setThickness(thicknessPx);

                        View wrappedView =
                                applyModifiersToArcLayoutView(
                                        space, spacer.getModifiers(), posId, pipelineMaker);
                        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

                        return new InflatedView(
                                wrappedView,
                                parentViewWrapper
                                        .getParentProperties()
                                        .applyPendingChildLayoutParams(layoutParams));
                    }

                case INNER_NOT_SET:
                    break;
            }
        } else {
            lengthDegrees = max(0, spacer.getLength().getValue());
        }

        if (lengthDegrees == 0 && thicknessPx == 0) {
            return null;
        }
        space.setSweepAngleDegrees(lengthDegrees);
        space.setThickness(thicknessPx);

        View wrappedView =
                applyModifiersToArcLayoutView(space, spacer.getModifiers(), posId, pipelineMaker);
        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        return new InflatedView(
                wrappedView,
                parentViewWrapper
                        .getParentProperties()
                        .applyPendingChildLayoutParams(layoutParams));
    }

    private static void applyTextOverflow(
            TextView textView, TextOverflowProp overflow, MarqueeParameters marqueeParameters) {
        textView.setEllipsize(textTruncationToEllipsize(overflow));
        if (overflow.getValue() == TextOverflow.TEXT_OVERFLOW_MARQUEE
                && textView.getMaxLines() == 1) {
            int marqueeIterations =
                    marqueeParameters.hasIterations()
                            ? marqueeParameters.getIterations()
                            : -1; // Defaults to repeat indefinitely (-1).
            textView.setMarqueeRepeatLimit(marqueeIterations);
            textView.setSelected(true);
            textView.setSingleLine();
            textView.setHorizontalFadingEdgeEnabled(true);
        }
    }

    private InflatedView inflateText(
            ParentViewWrapper parentViewWrapper,
            Text text,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        TextView textView = newThemedTextView();

        LayoutParams layoutParams = generateDefaultLayoutParams();

        boolean needsSizeWrapper = needsSizeWrapper(text.getText());

        handleProp(
                text.getText(),
                t -> {
                    // Underlines are applied using a Spannable here, rather than setting paint bits
                    // (or
                    // using Paint#setTextUnderline). When multiple fonts are mixed on the same line
                    // (especially when mixing anything with NotoSans-CJK), multiple underlines can
                    // appear. Using UnderlineSpan instead though causes the correct behaviour to
                    // happen
                    // (only a
                    // single underline).
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    ssb.append(t);

                    if (text.getFontStyle().getUnderline().getValue()) {
                        ssb.setSpan(new UnderlineSpan(), 0, ssb.length(), Spanned.SPAN_MARK_MARK);
                    }

                    textView.setText(ssb);
                },
                posId,
                pipelineMaker);

        textView.setGravity(textAlignToAndroidGravity(text.getMultilineAlignment().getValue()));

        // Use needsSizeWrapper as a proxy for "has a dynamic size". If there's a dynamic binding
        // for the text element, then it can only have a single line of text.
        if (text.hasMaxLines() && !needsSizeWrapper) {
            textView.setMaxLines(max(TEXT_MIN_LINES, text.getMaxLines().getValue()));
        } else {
            textView.setMaxLines(TEXT_MAX_LINES_DEFAULT);
        }
        applyTextOverflow(textView, text.getOverflow(), text.getMarqueeParameters());

        // Setting colours **must** go after setting the Text Appearance, otherwise it will get
        // immediately overridden.
        if (text.hasFontStyle()) {
            applyFontStyle(text.getFontStyle(), textView, posId, pipelineMaker);
        } else {
            applyFontStyle(FontStyle.getDefaultInstance(), textView, posId, pipelineMaker);
        }

        boolean excludeFontPadding = false;

        if (text.hasAndroidTextStyle()) {
            excludeFontPadding = text.getAndroidTextStyle().getExcludeFontPadding();
        }
        applyExcludeFontPadding(textView, excludeFontPadding);

        if (text.hasLineHeight()) {
            float lineHeightPx = toPx(text.getLineHeight());
            final float fontHeightPx = textView.getPaint().getFontSpacing();
            if (lineHeightPx != fontHeightPx) {
                textView.setLineSpacing(lineHeightPx - fontHeightPx, 1f);
            }
        }

        // We don't want the text to be screen-reader focusable, unless wrapped in a Spannable
        // modifier. This prevents automatically reading out partial text (e.g. text in a row) etc.
        //
        // This **must** be done before applying modifiers; applying a Semantics modifier will set
        // importantForAccessibility, so we don't want to override it after applying modifiers.
        textView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        View wrappedView = applyModifiers(textView, text.getModifiers(), posId, pipelineMaker);

        if (needsSizeWrapper) {
            // If we're here, then it's safe to unconditionally read size_for_layout.
            String valueForLayout = text.getText().getValueForLayout();

            // Now create a "container" element, with that size, to hold the text.
            FrameLayout sizeChangingTextWrapper = new FrameLayout(mUiContext);
            LayoutParams sizeChangingTextWrapperLayoutParams = generateDefaultLayoutParams();
            // Use the actual TextView to measure the text width.
            sizeChangingTextWrapperLayoutParams.width =
                    (int) textView.getPaint().measureText(valueForLayout);
            sizeChangingTextWrapperLayoutParams.height = LayoutParams.WRAP_CONTENT;

            // Set horizontal gravity on the wrapper to reflect alignment.
            int gravity = textAlignToAndroidGravity(text.getText().getTextAlignmentForLayout());
            FrameLayout.LayoutParams frameLayoutLayoutParams =
                    new FrameLayout.LayoutParams(layoutParams);
            frameLayoutLayoutParams.gravity = gravity;
            layoutParams = frameLayoutLayoutParams;

            sizeChangingTextWrapper.addView(wrappedView, layoutParams);
            parentViewWrapper.maybeAddView(
                    sizeChangingTextWrapper, sizeChangingTextWrapperLayoutParams);
            return new InflatedView(
                    sizeChangingTextWrapper,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(sizeChangingTextWrapperLayoutParams));
        } else {
            parentViewWrapper.maybeAddView(wrappedView, layoutParams);
            return new InflatedView(
                    wrappedView,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(layoutParams));
        }
    }

    /**
     * Sets whether the padding is included or not. If font padding is not included, sets the
     * correct padding to the TextView to avoid clipping taller languages.
     */
    private void applyExcludeFontPadding(TextView textView, boolean excludeFontPadding) {
        // Reversed value, since TextView sets padding to be included, while our protos are for
        // excluding it.
        textView.setIncludeFontPadding(!excludeFontPadding);

        // We need to update padding in the TextView if font's padding is not used, to avoid
        // clipping of taller languages.
        if (!excludeFontPadding) {
            return;
        }

        float ascent = textView.getPaint().getFontMetrics().ascent;
        float descent = textView.getPaint().getFontMetrics().descent;
        String text = textView.getText().toString();
        Rect bounds = new Rect();

        textView.getPaint().getTextBounds(text, 0, max(0, text.length() - 1), bounds);

        int topPadding = textView.getPaddingTop();
        int bottomPadding = textView.getPaddingBottom();

        if (ascent > bounds.top) {
            topPadding = (int) (ascent - bounds.top);
        }

        if (descent < bounds.bottom) {
            bottomPadding = (int) (bounds.bottom - descent);
        }

        textView.setPadding(
                textView.getPaddingLeft(), topPadding, textView.getPaddingRight(), bottomPadding);
    }

    private InflatedView inflateArcText(
            ParentViewWrapper parentViewWrapper,
            ArcText text,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        CurvedTextView textView = newThemedCurvedTextView();

        LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        textView.setText(text.getText().getValue());

        if (text.hasFontStyle()) {
            applyFontStyle(text.getFontStyle(), textView);
        }

        textView.setTextColor(extractTextColorArgb(text.getFontStyle()));

        View wrappedView =
                applyModifiersToArcLayoutView(textView, text.getModifiers(), posId, pipelineMaker);
        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        return new InflatedView(
                wrappedView,
                parentViewWrapper
                        .getParentProperties()
                        .applyPendingChildLayoutParams(layoutParams));
    }

    private static boolean isZeroLengthImageDimension(ImageDimension dimension) {
        return dimension.getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION
                && dimension.getLinearDimension().getValue() == 0;
    }

    private static ContainerDimension imageDimensionToContainerDimension(ImageDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return ContainerDimension.newBuilder()
                        .setLinearDimension(dimension.getLinearDimension())
                        .build();
            case EXPANDED_DIMENSION:
                return ContainerDimension.newBuilder()
                        .setExpandedDimension(ExpandedDimensionProp.getDefaultInstance())
                        .build();
            case PROPORTIONAL_DIMENSION:
                // A ratio size should be translated to a WRAP_CONTENT; the RatioViewWrapper will
                // deal with the sizing of that.
                return ContainerDimension.newBuilder()
                        .setWrappedDimension(WrappedDimensionProp.getDefaultInstance())
                        .build();
            case INNER_NOT_SET:
                break;
        }
        // Caller should have already checked for this.
        throw new IllegalArgumentException(
                "ImageDimension has an unknown dimension type: " + dimension.getInnerCase().name());
    }

    @SuppressWarnings("ExecutorTaskName")
    @Nullable
    private InflatedView inflateImage(
            ParentViewWrapper parentViewWrapper,
            Image image,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        String protoResId = image.getResourceId().getValue();

        // If either width or height isn't set, abort.
        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.INNER_NOT_SET
                || image.getHeight().getInnerCase() == ImageDimension.InnerCase.INNER_NOT_SET) {
            Log.w(TAG, "One of width and height not set on image " + protoResId);
            return null;
        }

        // The image must occupy _some_ space.
        if (isZeroLengthImageDimension(image.getWidth())
                || isZeroLengthImageDimension(image.getHeight())) {
            Log.w(TAG, "One of width and height was zero on image " + protoResId);
            return null;
        }

        // Both dimensions can't be ratios.
        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION
                && image.getHeight().getInnerCase()
                        == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            Log.w(TAG, "Both width and height were proportional for image " + protoResId);
            return null;
        }

        // Pull the ratio for the RatioViewWrapper. Was either argument a proportional dimension?
        @Nullable Float ratio = RatioViewWrapper.UNDEFINED_ASPECT_RATIO;

        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            ratio = safeAspectRatioOrNull(image.getWidth().getProportionalDimension());
        }

        if (image.getHeight().getInnerCase() == ImageDimension.InnerCase.PROPORTIONAL_DIMENSION) {
            ratio = safeAspectRatioOrNull(image.getHeight().getProportionalDimension());
        }

        if (ratio == null) {
            Log.w(TAG, "Invalid aspect ratio for image " + protoResId);
            return null;
        }

        ImageViewWithoutIntrinsicSizes imageView = new ImageViewWithoutIntrinsicSizes(mUiContext);

        if (image.hasContentScaleMode()) {
            imageView.setScaleType(
                    contentScaleModeToScaleType(image.getContentScaleMode().getValue()));
        }

        if (image.getWidth().getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION) {
            imageView.setMinimumWidth(safeDpToPx(image.getWidth().getLinearDimension()));
        }

        if (image.getHeight().getInnerCase() == ImageDimension.InnerCase.LINEAR_DIMENSION) {
            imageView.setMinimumHeight(safeDpToPx(image.getHeight().getLinearDimension()));
        }

        // We need to sort out the sizing of the widget now, so we can pass the correct params to
        // RatioViewWrapper. First, translate the ImageSize to a ContainerSize. A ratio size should
        // be translated to a WRAP_CONTENT; the RatioViewWrapper will deal with the sizing of that.
        LayoutParams ratioWrapperLayoutParams = generateDefaultLayoutParams();
        ratioWrapperLayoutParams =
                updateLayoutParams(
                        parentViewWrapper.getParentProperties(),
                        ratioWrapperLayoutParams,
                        imageDimensionToContainerDimension(image.getWidth()),
                        imageDimensionToContainerDimension(image.getHeight()));

        // Apply the modifiers to the ImageView, **not** the RatioViewWrapper.
        //
        // RatioViewWrapper doesn't do any custom drawing, it only exists to force dimensions during
        // the measure/layout passes, so it doesn't matter which element any border/background
        // modifiers get applied to. Applying modifiers to the ImageView is important for Semantics
        // though; screen readers try and pick up on the type of element being read, so in the case
        // of an image would read "image, <description>" (where the location of "image" can move
        // depending on user settings). If we apply the modifiers to RatioViewWrapper though, screen
        // readers will not realise that this is an image, and will read the incorrect description.
        View wrappedImageView =
                applyModifiers(imageView, image.getModifiers(), posId, pipelineMaker);

        RatioViewWrapper ratioViewWrapper = new RatioViewWrapper(mUiContext);
        ratioViewWrapper.setAspectRatio(ratio);
        ratioViewWrapper.addView(wrappedImageView);

        parentViewWrapper.maybeAddView(ratioViewWrapper, ratioWrapperLayoutParams);

        ListenableFuture<Drawable> drawableFuture =
                mLayoutResourceResolvers.getDrawable(protoResId);
        Drawable immediatelySetDrawable = null;
        if (drawableFuture.isDone() && !drawableFuture.isCancelled()) {
            // If the future is done, immediately draw.
            immediatelySetDrawable = setImageDrawable(imageView, drawableFuture, protoResId);
        }

        if (immediatelySetDrawable != null && pipelineMaker.isPresent()) {
            if (immediatelySetDrawable instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable avd = (AnimatedVectorDrawable) immediatelySetDrawable;
                try {
                    Trigger trigger = mLayoutResourceResolvers.getAnimationTrigger(protoResId);

                    if (trigger != null
                            && trigger.getInnerCase()
                                    == Trigger.InnerCase.ON_CONDITION_MET_TRIGGER) {
                        OnConditionMetTrigger conditionTrigger = trigger.getOnConditionMetTrigger();
                        pipelineMaker
                                .get()
                                .addResolvedAnimatedImageWithBoolTrigger(
                                        avd, trigger, posId, conditionTrigger.getTrigger());
                    } else {
                        // Use default trigger if it's not set.
                        if (trigger == null
                                || trigger.getInnerCase() == Trigger.InnerCase.INNER_NOT_SET) {
                            trigger = DEFAULT_ANIMATION_TRIGGER;
                        }
                        pipelineMaker.get().addResolvedAnimatedImage(avd, trigger, posId);
                    }
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Error setting up animation trigger", ex);
                }
            } else if (immediatelySetDrawable instanceof SeekableAnimatedVectorDrawable) {
                SeekableAnimatedVectorDrawable seekableAvd =
                        (SeekableAnimatedVectorDrawable) immediatelySetDrawable;
                try {
                    DynamicFloat progress = mLayoutResourceResolvers.getBoundProgress(protoResId);
                    if (progress != null) {
                        pipelineMaker
                                .get()
                                .addResolvedSeekableAnimatedImage(seekableAvd, progress, posId);
                    }
                } catch (IllegalArgumentException ex) {
                    Log.e(TAG, "Error setting up seekable animated image", ex);
                }
            }
        } else {
            // Is there a placeholder to use in the meantime?
            try {
                if (mLayoutResourceResolvers.hasPlaceholderDrawable(protoResId)) {
                    if (setImageDrawable(
                                    imageView,
                                    mLayoutResourceResolvers.getPlaceholderDrawableOrThrow(
                                            protoResId),
                                    protoResId)
                            == null) {
                        Log.w(TAG, "Failed to set the placeholder for " + protoResId);
                    }
                }
            } catch (ResourceAccessException | IllegalArgumentException ex) {
                Log.e(TAG, "Exception loading placeholder for resource " + protoResId, ex);
            }

            // Otherwise, handle the result on the UI thread.
            drawableFuture.addListener(
                    () -> setImageDrawable(imageView, drawableFuture, protoResId),
                    ContextCompat.getMainExecutor(mUiContext));
        }

        boolean canImageBeTinted = false;

        try {
            canImageBeTinted = mLayoutResourceResolvers.canImageBeTinted(protoResId);
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "Exception tinting image " + protoResId, ex);
        }

        if (image.getColorFilter().hasTint() && canImageBeTinted) {
            // Only allow tinting for Android images.f
            handleProp(
                    image.getColorFilter().getTint(),
                    tintColor -> {
                        ColorStateList tint = ColorStateList.valueOf(tintColor);
                        imageView.setImageTintList(tint);

                        // SRC_IN throws away the colours in the drawable that we're tinting.
                        // Effectively, the drawable being tinted is only a mask to apply the colour
                        // to.
                        imageView.setImageTintMode(Mode.SRC_IN);
                    },
                    posId,
                    pipelineMaker);
        }

        return new InflatedView(
                ratioViewWrapper,
                parentViewWrapper
                        .getParentProperties()
                        .applyPendingChildLayoutParams(ratioWrapperLayoutParams));
    }

    /**
     * Set drawable to the image view.
     *
     * @return Returns the drawable if it is successfully retrieved from the drawable future and set
     *     to the image view; otherwise returns null to indicate the failure of setting drawable.
     */
    @Nullable
    private static Drawable setImageDrawable(
            ImageView imageView, Future<Drawable> drawableFuture, String protoResId) {
        try {
            return setImageDrawable(imageView, drawableFuture.get(), protoResId);
        } catch (ExecutionException | InterruptedException | CancellationException e) {
            Log.w(TAG, "Could not get drawable for image " + protoResId, e);
        }
        return null;
    }

    /**
     * Set drawable to the image view.
     *
     * @return Returns the drawable if it is successfully set to the image view; otherwise returns
     *     null to indicate the failure of setting drawable.
     */
    @Nullable
    private static Drawable setImageDrawable(
            ImageView imageView, Drawable drawable, String protoResId) {
        if (drawable instanceof BitmapDrawable
                && ((BitmapDrawable) drawable).getBitmap().getByteCount()
                        > DEFAULT_MAX_BITMAP_RAW_SIZE) {
            Log.w(TAG, "Ignoring image " + protoResId + " as it's too large.");
            return null;
        }
        imageView.setImageDrawable(drawable);
        return drawable;
    }

    @Nullable
    private InflatedView inflateArcLine(
            ParentViewWrapper parentViewWrapper,
            ArcLine line,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        float lengthDegrees = 0;
        if (line.hasAngularLength()) {
            if (line.getAngularLength().getInnerCase() == ArcLineLength.InnerCase.DEGREES) {
                lengthDegrees = max(0, line.getAngularLength().getDegrees().getValue());
            }
        } else {
            lengthDegrees = max(0, line.getLength().getValue());
        }

        int thicknessPx = safeDpToPx(line.getThickness());

        if (lengthDegrees == 0 && thicknessPx == 0) {
            return null;
        }

        WearCurvedLineView lineView = new WearCurvedLineView(mUiContext);

        // A ArcLineView must always be the same width/height as its parent, so it can draw the line
        // properly inside of those bounds.
        ArcLayout.LayoutParams layoutParams =
                new ArcLayout.LayoutParams(generateDefaultLayoutParams());
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        if (line.hasColor()) {
            handleProp(line.getColor(), lineView::setColor, posId, pipelineMaker);
        } else {
            lineView.setColor(LINE_COLOR_DEFAULT);
        }

        if (line.hasStrokeCap()) {
            switch (line.getStrokeCap().getValue()) {
                case STROKE_CAP_BUTT:
                    lineView.setStrokeCap(Cap.BUTT);
                    break;
                case STROKE_CAP_ROUND:
                    lineView.setStrokeCap(Cap.ROUND);
                    break;
                case STROKE_CAP_SQUARE:
                    lineView.setStrokeCap(Cap.SQUARE);
                    break;
                case UNRECOGNIZED:
                case STROKE_CAP_UNDEFINED:
                    Log.w(TAG, "Undefined StrokeCap value.");
                    break;
            }
        }

        lineView.setThickness(thicknessPx);

        DegreesProp length;
        if (line.hasAngularLength()) {
            final ArcLineLength angularLength = line.getAngularLength();
            switch (angularLength.getInnerCase()) {
                case DEGREES:
                    length = line.getAngularLength().getDegrees();
                    handleProp(length, lineView::setLineSweepAngleDegrees, posId, pipelineMaker);
                    break;

                case EXPANDED_ANGULAR_DIMENSION:
                    {
                        ExpandedAngularDimensionProp expandedAngularDimension =
                                angularLength.getExpandedAngularDimension();
                        layoutParams.setWeight(
                                expandedAngularDimension.hasLayoutWeight()
                                        ? expandedAngularDimension.getLayoutWeight().getValue()
                                        : 1.0f);
                        length = DegreesProp.getDefaultInstance();
                        break;
                    }

                default:
                    length = DegreesProp.getDefaultInstance();
                    break;
            }
        } else {
            length = line.getLength();
            handleProp(length, lineView::setLineSweepAngleDegrees, posId, pipelineMaker);
        }

        float sizeForLayout =
                getSizeForLayout(line.getLength(), WearCurvedLineView.SWEEP_ANGLE_WRAP_LENGTH);

        SizedArcContainer sizeWrapper = null;
        SizedArcContainer.LayoutParams sizedLp =
                new SizedArcContainer.LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        if (needsSizeWrapper(length)) {
            sizeWrapper = new SizedArcContainer(mUiContext);
            sizeWrapper.setSweepAngleDegrees(sizeForLayout);
            sizedLp.setAngularAlignment(
                    angularAlignmentProtoToAngularAlignment(length.getAngularAlignmentForLayout()));

            // Also clamp the line to that angle...
            lineView.setMaxSweepAngleDegrees(sizeForLayout);
        }

        View wrappedView =
                applyModifiersToArcLayoutView(lineView, line.getModifiers(), posId, pipelineMaker);

        if (sizeWrapper != null) {
            sizeWrapper.addView(wrappedView, sizedLp);
            parentViewWrapper.maybeAddView(sizeWrapper, layoutParams);
            return new InflatedView(
                    sizeWrapper,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(layoutParams));
        } else {
            parentViewWrapper.maybeAddView(wrappedView, layoutParams);
            return new InflatedView(
                    wrappedView,
                    parentViewWrapper
                            .getParentProperties()
                            .applyPendingChildLayoutParams(layoutParams));
        }
    }

    // dereference of possibly-null reference childLayoutParams
    @SuppressWarnings("nullness:dereference.of.nullable")
    @Nullable
    private InflatedView inflateArc(
            ParentViewWrapper parentViewWrapper,
            Arc arc,
            String arcPosId,
            boolean includeChildren,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        ArcLayout arcLayout = new ArcLayout(mUiContext);

        LayoutParams layoutParams = generateDefaultLayoutParams();
        layoutParams.width = LayoutParams.MATCH_PARENT;
        layoutParams.height = LayoutParams.MATCH_PARENT;

        handleProp(
                arc.getAnchorAngle(),
                angle -> {
                    arcLayout.setAnchorAngleDegrees(angle);
                    // Invalidating arcLayout isn't enough. AnchorAngleDegrees change should trigger
                    // child requestLayout.
                    arcLayout.requestLayout();
                },
                arcPosId,
                pipelineMaker);
        arcLayout.setAnchorAngleDegrees(arc.getAnchorAngle().getValue());
        arcLayout.setAnchorType(anchorTypeToAnchorPos(arc.getAnchorType().getValue()));

        if (arc.hasMaxAngle()) {
            arcLayout.setMaxAngleDegrees(arc.getMaxAngle().getValue());
        }

        // Add all children.
        if (includeChildren) {
            int index = FIRST_CHILD_INDEX;
            for (ArcLayoutElement child : arc.getContentsList()) {
                String childPosId = ProtoLayoutDiffer.createNodePosId(arcPosId, index++);
                @Nullable
                InflatedView childView =
                        inflateArcLayoutElement(
                                new ParentViewWrapper(arcLayout, layoutParams),
                                child,
                                childPosId,
                                layoutInfoBuilder,
                                pipelineMaker);
                if (childView != null) {
                    ArcLayout.LayoutParams childLayoutParams =
                            (ArcLayout.LayoutParams) childView.mView.getLayoutParams();
                    boolean rotate = false;
                    if (child.hasAdapter()) {
                        rotate = child.getAdapter().getRotateContents().getValue();
                    }

                    // Apply rotation and gravity.
                    childLayoutParams.setRotated(rotate);
                    childLayoutParams.setVerticalAlignment(
                            verticalAlignmentToArcVAlign(arc.getVerticalAlign()));
                }
            }
            layoutInfoBuilder.removeSubtree(arcPosId);
        }

        View wrappedView = applyModifiers(arcLayout, arc.getModifiers(), arcPosId, pipelineMaker);
        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        int numMissingChildren = includeChildren ? 0 : arc.getContentsCount();
        return new InflatedView(
                wrappedView,
                parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(layoutParams),
                NO_OP_PENDING_LAYOUT_PARAMS,
                numMissingChildren);
    }

    private void applyStylesToSpan(
            SpannableStringBuilder builder, int start, int end, FontStyle fontStyle) {
        if (fontStyle.hasSize()) {
            AbsoluteSizeSpan span = new AbsoluteSizeSpan(round(toPx(fontStyle.getSize())));
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.hasWeight() || fontStyle.hasVariant()) {
            CustomTypefaceSpan span = new CustomTypefaceSpan(fontStyleToTypeface(fontStyle));
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (!hasDefaultTypefaceStyle(fontStyle)) {
            MetricAffectingSpan span = createTypefaceSpan(fontStyle);
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.getUnderline().getValue()) {
            UnderlineSpan span = new UnderlineSpan();
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        if (fontStyle.hasLetterSpacing()) {
            LetterSpacingSpan span = new LetterSpacingSpan(fontStyle.getLetterSpacing().getValue());
            builder.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
        }

        ForegroundColorSpan colorSpan = new ForegroundColorSpan(extractTextColorArgb(fontStyle));

        builder.setSpan(colorSpan, start, end, Spanned.SPAN_MARK_MARK);
    }

    private void applyModifiersToSpan(
            SpannableStringBuilder builder, int start, int end, SpanModifiers modifiers) {
        if (modifiers.hasClickable()) {
            ClickableSpan clickableSpan = new ProtoLayoutClickableSpan(modifiers.getClickable());

            builder.setSpan(clickableSpan, start, end, Spanned.SPAN_MARK_MARK);
        }
    }

    private SpannableStringBuilder inflateTextInSpannable(
            SpannableStringBuilder builder, SpanText text) {
        int currentPos = builder.length();
        int lastPos = currentPos + text.getText().getValue().length();

        builder.append(text.getText().getValue());

        applyStylesToSpan(builder, currentPos, lastPos, text.getFontStyle());
        applyModifiersToSpan(builder, currentPos, lastPos, text.getModifiers());

        return builder;
    }

    @SuppressWarnings("ExecutorTaskName")
    private SpannableStringBuilder inflateImageInSpannable(
            SpannableStringBuilder builder, SpanImage protoImage, TextView textView) {
        String protoResId = protoImage.getResourceId().getValue();

        if (protoImage.getWidth().getValue() == 0 || protoImage.getHeight().getValue() == 0) {
            Log.w(TAG, "One of width and height was zero on image " + protoResId);
            return builder;
        }

        ListenableFuture<Drawable> drawableFuture =
                mLayoutResourceResolvers.getDrawable(protoResId);
        if (drawableFuture.isDone()) {
            // If the future is done, immediately add drawable to builder.
            try {
                Drawable drawable = drawableFuture.get();
                appendSpanDrawable(builder, drawable, protoImage);
            } catch (ExecutionException | InterruptedException e) {
                Log.w(
                        TAG,
                        "Could not get drawable for image "
                                + protoImage.getResourceId().getValue());
            }
        } else {
            // If the future is not done, add an empty drawable to builder as a placeholder.
            @Nullable Drawable placeholderDrawable = null;

            try {
                if (mLayoutResourceResolvers.hasPlaceholderDrawable(protoResId)) {
                    placeholderDrawable =
                            mLayoutResourceResolvers.getPlaceholderDrawableOrThrow(protoResId);
                }
            } catch (ResourceAccessException | IllegalArgumentException ex) {
                Log.e(TAG, "Could not get placeholder for image " + protoResId, ex);
            }

            if (placeholderDrawable == null) {
                placeholderDrawable = new ColorDrawable(Color.TRANSPARENT);
            }

            int startInclusive = builder.length();
            FixedImageSpan placeholderDrawableSpan =
                    appendSpanDrawable(builder, placeholderDrawable, protoImage);
            int endExclusive = builder.length();

            // When the future is done, replace the empty drawable with the received one.
            drawableFuture.addListener(
                    () -> {
                        // Remove the placeholder. This should be safe, even with other modifiers
                        // applied. This just removes the single drawable span, and should leave
                        // other spans in place.
                        builder.removeSpan(placeholderDrawableSpan);
                        // Add the new drawable to the same range.
                        setSpanDrawable(
                                builder, drawableFuture, startInclusive, endExclusive, protoImage);
                        // Update the TextView.
                        textView.setText(builder);
                    },
                    ContextCompat.getMainExecutor(mUiContext));
        }

        return builder;
    }

    private FixedImageSpan appendSpanDrawable(
            SpannableStringBuilder builder, Drawable drawable, SpanImage protoImage) {
        drawable.setBounds(
                0, 0, safeDpToPx(protoImage.getWidth()), safeDpToPx(protoImage.getHeight()));
        FixedImageSpan imgSpan =
                new FixedImageSpan(
                        drawable,
                        spanVerticalAlignmentToImgSpanAlignment(protoImage.getAlignment()));

        int startPos = builder.length();

        // Adding NBSP around the space to prevent it from being trimmed.
        builder.append(
                ZERO_WIDTH_JOINER + " " + ZERO_WIDTH_JOINER, imgSpan, Spanned.SPAN_MARK_MARK);
        int endPos = builder.length();

        applyModifiersToSpan(builder, startPos, endPos, protoImage.getModifiers());

        return imgSpan;
    }

    private void setSpanDrawable(
            SpannableStringBuilder builder,
            ListenableFuture<Drawable> drawableFuture,
            int startInclusive,
            int endExclusive,
            SpanImage protoImage) {
        final String protoResourceId = protoImage.getResourceId().getValue();

        try {
            // Add the image span to the same range occupied by the placeholder.
            Drawable drawable = drawableFuture.get();
            drawable.setBounds(
                    0, 0, safeDpToPx(protoImage.getWidth()), safeDpToPx(protoImage.getHeight()));
            FixedImageSpan imgSpan =
                    new FixedImageSpan(
                            drawable,
                            spanVerticalAlignmentToImgSpanAlignment(protoImage.getAlignment()));
            builder.setSpan(
                    imgSpan,
                    startInclusive,
                    endExclusive,
                    android.text.Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        } catch (ExecutionException | InterruptedException | CancellationException e) {
            Log.w(TAG, "Could not get drawable for image " + protoResourceId);
        }
    }

    private InflatedView inflateSpannable(
            ParentViewWrapper parentViewWrapper,
            Spannable spannable,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        TextView tv = newThemedTextView();

        LayoutParams layoutParams = generateDefaultLayoutParams();

        SpannableStringBuilder builder = new SpannableStringBuilder();

        boolean isAnySpanClickable = false;

        boolean excludeFontPadding = false;

        for (Span element : spannable.getSpansList()) {
            switch (element.getInnerCase()) {
                case IMAGE:
                    SpanImage protoImage = element.getImage();
                    builder = inflateImageInSpannable(builder, protoImage, tv);

                    if (protoImage.getModifiers().hasClickable()) {
                        isAnySpanClickable = true;
                    }

                    break;
                case TEXT:
                    SpanText protoText = element.getText();
                    builder = inflateTextInSpannable(builder, protoText);

                    if (protoText.getModifiers().hasClickable()) {
                        isAnySpanClickable = true;
                    }

                    if (protoText.hasAndroidTextStyle()
                            && protoText.getAndroidTextStyle().getExcludeFontPadding()) {
                        excludeFontPadding = true;
                    }
                    break;
                default:
                    Log.w(TAG, "Unknown Span child type.");
                    break;
            }
        }

        tv.setGravity(horizontalAlignmentToGravity(spannable.getMultilineAlignment().getValue()));

        if (spannable.hasMaxLines()) {
            tv.setMaxLines(max(TEXT_MIN_LINES, spannable.getMaxLines().getValue()));
        } else {
            tv.setMaxLines(TEXT_MAX_LINES_DEFAULT);
        }
        applyTextOverflow(tv, spannable.getOverflow(), spannable.getMarqueeParameters());

        if (spannable.hasLineHeight()) {
            // We use a Span here instead of just calling TextViewCompat#setLineHeight.
            // setLineHeight is implemented by taking the difference between the current font height
            // (via the font metrics, not just the size in SP), subtracting that from the desired
            // line height, and setting that as the inter-line spacing. This doesn't work for our
            // Spannables; we don't use a default height, yet TextView still has a default font (and
            // size) that it tries to base the requested line height on, despite that never actually
            // being used. The end result is that the line height never actually drops out as
            // expected.
            //
            // Instead, wrap the whole thing in a LineHeightSpan with the desired line height. This
            // gets calculated properly as the TextView is calculating its per-line font metrics,
            // and will actually work correctly.
            StandardLineHeightSpan span =
                    new StandardLineHeightSpan((int) toPx(spannable.getLineHeight()));
            builder.setSpan(
                    span,
                    /* start= */ 0,
                    /* end= */ builder.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        } else if (spannable.hasLineSpacing()) {
            tv.setLineSpacing(toPx(spannable.getLineSpacing()), 1f);
        }

        tv.setText(builder);

        applyExcludeFontPadding(tv, excludeFontPadding);

        if (isAnySpanClickable) {
            // For any ClickableSpans to work, the MovementMethod must be set to LinkMovementMethod.
            tv.setMovementMethod(LinkMovementMethod.getInstance());

            // Disable the highlight color; if we don't do this, the clicked span will get
            // highlighted, which will be cleared half a second later if using LoadAction as the
            // next layout will be delivered, which recreates the elements and clears the highlight.
            tv.setHighlightColor(Color.TRANSPARENT);

            // Use InhibitingScroller to prevent the text from scrolling when tapped. Setting a
            // MovementMethod on a TextView (e.g. for clickables in a Spannable) then cause the
            // TextView to be scrollable, and to jump to the end when tapped.
            tv.setScroller(new InhibitingScroller(mUiContext));
        }

        View wrappedView = applyModifiers(tv, spannable.getModifiers(), posId, pipelineMaker);
        parentViewWrapper.maybeAddView(wrappedView, layoutParams);

        return new InflatedView(
                wrappedView,
                parentViewWrapper
                        .getParentProperties()
                        .applyPendingChildLayoutParams(layoutParams));
    }

    @Nullable
    private InflatedView inflateArcLayoutElement(
            ParentViewWrapper parentViewWrapper,
            ArcLayoutElement element,
            String nodePosId,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        InflatedView inflatedView = null;

        switch (element.getInnerCase()) {
            case ADAPTER:
                // Fall back to the normal inflater.
                inflatedView =
                        inflateLayoutElement(
                                parentViewWrapper,
                                element.getAdapter().getContent(),
                                nodePosId,
                                /* includeChildren= */ true,
                                layoutInfoBuilder,
                                pipelineMaker);
                break;

            case SPACER:
                inflatedView =
                        inflateArcSpacer(
                                parentViewWrapper, element.getSpacer(), nodePosId, pipelineMaker);
                break;

            case LINE:
                inflatedView =
                        inflateArcLine(
                                parentViewWrapper, element.getLine(), nodePosId, pipelineMaker);
                break;

            case TEXT:
                inflatedView =
                        inflateArcText(
                                parentViewWrapper, element.getText(), nodePosId, pipelineMaker);
                break;

            case INNER_NOT_SET:
                break;
        }

        if (inflatedView == null) {
            // Covers null (returned when the childCase in the proto isn't known). Sadly, ProtoLite
            // doesn't give us a way to access childCase's underlying tag, so we can't give any
            // smarter error message here.
            Log.w(TAG, "Unknown child type");
        } else if (nodePosId.isEmpty()) {
            Log.w(TAG, "No node ID for " + element.getInnerCase().name());
        } else {
            // Set the view's tag to a known, position-based ID so that it can be looked up to apply
            // mutations.
            inflatedView.mView.setTag(nodePosId);
            if (inflatedView.mView instanceof ViewGroup) {
                layoutInfoBuilder.add(
                        nodePosId,
                        ViewProperties.fromViewGroup(
                                (ViewGroup) inflatedView.mView,
                                inflatedView.mLayoutParams,
                                inflatedView.mChildLayoutParams));
            }
            pipelineMaker.ifPresent(pipe -> pipe.rememberNode(nodePosId));
        }
        return inflatedView;
    }

    @Nullable
    private InflatedView inflateLayoutElement(
            ParentViewWrapper parentViewWrapper,
            LayoutElement element,
            String nodePosId,
            boolean includeChildren,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        InflatedView inflatedView = null;
        // What is it?
        switch (element.getInnerCase()) {
            case COLUMN:
                inflatedView =
                        inflateColumn(
                                parentViewWrapper,
                                element.getColumn(),
                                nodePosId,
                                includeChildren,
                                layoutInfoBuilder,
                                pipelineMaker);
                break;
            case ROW:
                inflatedView =
                        inflateRow(
                                parentViewWrapper,
                                element.getRow(),
                                nodePosId,
                                includeChildren,
                                layoutInfoBuilder,
                                pipelineMaker);
                break;
            case BOX:
                inflatedView =
                        inflateBox(
                                parentViewWrapper,
                                element.getBox(),
                                nodePosId,
                                includeChildren,
                                layoutInfoBuilder,
                                pipelineMaker);
                break;
            case SPACER:
                inflatedView =
                        inflateSpacer(
                                parentViewWrapper, element.getSpacer(), nodePosId, pipelineMaker);
                break;
            case TEXT:
                inflatedView =
                        inflateText(parentViewWrapper, element.getText(), nodePosId, pipelineMaker);
                break;
            case IMAGE:
                inflatedView =
                        inflateImage(
                                parentViewWrapper, element.getImage(), nodePosId, pipelineMaker);
                break;
            case ARC:
                inflatedView =
                        inflateArc(
                                parentViewWrapper,
                                element.getArc(),
                                nodePosId,
                                includeChildren,
                                layoutInfoBuilder,
                                pipelineMaker);
                break;
            case SPANNABLE:
                inflatedView =
                        inflateSpannable(
                                parentViewWrapper,
                                element.getSpannable(),
                                nodePosId,
                                pipelineMaker);
                break;
            case EXTENSION:
                try {
                    inflatedView =
                            inflateExtension(
                                    parentViewWrapper, element.getExtension());
                } catch (IllegalStateException ex) {
                    Log.w(TAG, "Error inflating Extension.", ex);
                }
                break;
            case INNER_NOT_SET:
                Log.w(TAG, "Unknown child type: " + element.getInnerCase().name());
                break;
        }

        if (inflatedView == null) {
            Log.w(TAG, "Error inflating " + element.getInnerCase().name());
        } else if (nodePosId.isEmpty()) {
            Log.w(TAG, "No node ID for " + element.getInnerCase().name());
        } else {
            // Set the view's tag to a known, position-based ID so that it can be looked up to apply
            // mutations.
            inflatedView.mView.setTag(nodePosId);
            if (inflatedView.mView instanceof ViewGroup) {
                layoutInfoBuilder.add(
                        nodePosId,
                        ViewProperties.fromViewGroup(
                                (ViewGroup) inflatedView.mView,
                                inflatedView.mLayoutParams,
                                inflatedView.mChildLayoutParams));
            }
            pipelineMaker.ifPresent(pipe -> pipe.rememberNode(nodePosId));
        }
        return inflatedView;
    }

    @Nullable
    private InflatedView inflateExtension(
            ParentViewWrapper parentViewWrapper, ExtensionLayoutElement element) {
        int widthPx = safeDpToPx(element.getWidth().getLinearDimension());
        int heightPx = safeDpToPx(element.getHeight().getLinearDimension());

        if (widthPx == 0 && heightPx == 0) {
            return null;
        }

        if (mExtensionViewProvider == null) {
            Log.e(TAG, "Layout has extension payload, but no extension provider is available.");
            return inflateFailedExtension(parentViewWrapper, element);
        }

        View view =
                mExtensionViewProvider.provideView(
                        element.getPayload().toByteArray(), element.getExtensionId());

        if (view == null) {
            Log.w(TAG, "Extension view provider returned null.");
            // A failed extension should still occupy space.
            return inflateFailedExtension(parentViewWrapper, element);
        }

        if (view.getTag() != null) {
            throw new IllegalStateException("Extension must not set View's default tag");
        }

        LayoutParams lp = new LayoutParams(widthPx, heightPx);
        parentViewWrapper.maybeAddView(view, lp);

        return new InflatedView(
                view, parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(lp));
    }

    private InflatedView inflateFailedExtension(
            ParentViewWrapper parentViewWrapper, ExtensionLayoutElement element) {
        int widthPx = safeDpToPx(element.getWidth().getLinearDimension());
        int heightPx = safeDpToPx(element.getHeight().getLinearDimension());

        Space space = new Space(mUiContext);

        LayoutParams lp = new LayoutParams(widthPx, heightPx);
        parentViewWrapper.maybeAddView(space, lp);

        return new InflatedView(
                space, parentViewWrapper.getParentProperties().applyPendingChildLayoutParams(lp));
    }

    /**
     * Either yield the constant value stored in stringProp, or register for updates if it is
     * dynamic property.
     *
     * <p>If both are set, this routine will yield the constant value if and only if this renderer
     * has a dynamic pipeline (i.e. {code mDataPipeline} is non-null), otherwise it will only
     * subscribe for dynamic updates. If the dynamic pipeline ever yields an invalid value (via
     * {@code onStateInvalid}), then stringProp's static valid will be used instead.
     */
    private void handleProp(
            StringProp stringProp,
            Consumer<String> consumer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (stringProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker
                        .get()
                        .addPipelineFor(
                                stringProp.getDynamicValue(),
                                stringProp.getValue(),
                                mUiContext.getResources().getConfiguration().getLocales().get(0),
                                posId,
                                consumer);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error building pipeline", ex);
                consumer.accept(stringProp.getValue());
            }
        } else {
            consumer.accept(stringProp.getValue());
        }
    }

    private void handleProp(
            DegreesProp degreesProp,
            Consumer<Float> consumer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (degreesProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker
                        .get()
                        .addPipelineFor(degreesProp, degreesProp.getValue(), posId, consumer);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error building pipeline", ex);
                consumer.accept(degreesProp.getValue());
            }
        } else {
            consumer.accept(degreesProp.getValue());
        }
    }

    private void handleProp(
            DpProp dpProp,
            Consumer<Float> consumer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (dpProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker.get().addPipelineFor(dpProp, dpProp.getValue(), posId, consumer);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error building pipeline", ex);
                consumer.accept(dpProp.getValue());
            }
        } else {
            consumer.accept(dpProp.getValue());
        }
    }

    private void handleProp(
            ColorProp colorProp,
            Consumer<Integer> consumer,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        if (colorProp.hasDynamicValue() && pipelineMaker.isPresent()) {
            try {
                pipelineMaker.get().addPipelineFor(colorProp, colorProp.getArgb(), posId, consumer);
            } catch (RuntimeException ex) {
                Log.e(TAG, "Error building pipeline", ex);
                consumer.accept(colorProp.getArgb());
            }
        } else {
            consumer.accept(colorProp.getArgb());
        }
    }

    private boolean needsSizeWrapper(StringProp stringProp) {
        if (stringProp.hasDynamicValue() && mDataPipeline.isPresent()) {
            if (!stringProp.getValueForLayout().isEmpty()) {
                // If value_for_layout is set, then a size wrapper is needed. This covers the case
                // where mAllowLayoutChangingBindsWithoutDefault, but a size has been provided
                // anyway.
                return true;
            } else {
                return !mAllowLayoutChangingBindsWithoutDefault;
            }
        } else {
            // Dynamic data disabled; we won't be using the dynamic value regardless...
            return false;
        }
    }

    private boolean needsSizeWrapper(SpacerDimension spacerDimension) {
        DpProp dimension = spacerDimension.getLinearDimension();
        if (dimension.hasDynamicValue() && mDataPipeline.isPresent()) {
            if (dimension.getValueForLayout() > 0f) {
                return true;
            } else {
                return !mAllowLayoutChangingBindsWithoutDefault;
            }
        } else {
            return false;
        }
    }

    private boolean needsSizeWrapper(DegreesProp degreesProp) {
        if (degreesProp.hasDynamicValue() && mDataPipeline.isPresent()) {
            if (degreesProp.getValueForLayout() > 0f) {
                return true;
            } else {
                return !mAllowLayoutChangingBindsWithoutDefault;
            }
        } else {
            return false;
        }
    }

    private float getSizeForLayout(DegreesProp degreesProp, float otherwise) {
        if (degreesProp.hasDynamicValue() && mDataPipeline.isPresent()) {
            if (degreesProp.getValueForLayout() > 0f) {
                // If value_for_layout is set, always use it
                return degreesProp.getValueForLayout();
            } else if (mAllowLayoutChangingBindsWithoutDefault) {
                // We're in "legacy binds" mode. Allow usage of the bind without needing
                // value_for_layout
                return otherwise;
            } else {
                // Neither set. Error condition (that should not happen without the developer
                // manually building the proto).
                return 0f;
            }
        } else {
            return otherwise;
        }
    }

    private boolean canMeasureContainer(
            ContainerDimension containerWidth,
            ContainerDimension containerHeight,
            List<LayoutElement> elements) {
        // We can't measure a container if it's set to wrap-contents but all of its contents are set
        // to expand-to-parent. Such containers must not be displayed.
        if (containerWidth.hasWrappedDimension()
                && !containsMeasurableWidth(containerHeight, elements)) {
            return false;
        }
        if (containerHeight.hasWrappedDimension()
                && !containsMeasurableHeight(containerWidth, elements)) {
            return false;
        }
        return true;
    }

    private boolean containsMeasurableWidth(
            ContainerDimension containerHeight, List<LayoutElement> elements) {
        for (LayoutElement element : elements) {
            if (isWidthMeasurable(element, containerHeight)) {
                // Enough to find a single element that is measurable.
                return true;
            }
        }
        return false;
    }

    private boolean containsMeasurableHeight(
            ContainerDimension containerWidth, List<LayoutElement> elements) {
        for (LayoutElement element : elements) {
            if (isHeightMeasurable(element, containerWidth)) {
                // Enough to find a single element that is measurable.
                return true;
            }
        }
        return false;
    }

    private boolean isWidthMeasurable(LayoutElement element, ContainerDimension containerHeight) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return isMeasurable(element.getColumn().getWidth());
            case ROW:
                return isMeasurable(element.getRow().getWidth());
            case BOX:
                return isMeasurable(element.getBox().getWidth());
            case SPACER:
                return isMeasurable(element.getSpacer().getWidth());
            case IMAGE:
                // Special-case. If the image width is proportional, then the height must be
                // measurable. This means either a fixed size, or expanded where we know the parent
                // dimension.
                Image img = element.getImage();
                if (img.getWidth().hasProportionalDimension()) {
                    boolean isContainerHeightKnown =
                            (containerHeight.hasExpandedDimension()
                                    || containerHeight.hasLinearDimension());
                    return img.getHeight().hasLinearDimension()
                            || (img.getHeight().hasExpandedDimension() && isContainerHeightKnown);
                } else {
                    return isMeasurable(element.getImage().getWidth());
                }
            case ARC:
            case TEXT:
            case SPANNABLE:
                return true;
            case INNER_NOT_SET:
                return false;
            default: // TODO(b/276703002): Remove default case
                return false;
        }
    }

    private boolean isHeightMeasurable(LayoutElement element, ContainerDimension containerWidth) {
        switch (element.getInnerCase()) {
            case COLUMN:
                return isMeasurable(element.getColumn().getHeight());
            case ROW:
                return isMeasurable(element.getRow().getHeight());
            case BOX:
                return isMeasurable(element.getBox().getHeight());
            case SPACER:
                return isMeasurable(element.getSpacer().getHeight());
            case IMAGE:
                // Special-case. If the image height is proportional, then the width must be
                // measurable. This means either a fixed size, or expanded where we know the parent
                // dimension.
                Image img = element.getImage();
                if (img.getHeight().hasProportionalDimension()) {
                    boolean isContainerWidthKnown =
                            (containerWidth.hasExpandedDimension()
                                    || containerWidth.hasLinearDimension());
                    return img.getWidth().hasLinearDimension()
                            || (img.getWidth().hasExpandedDimension() && isContainerWidthKnown);
                } else {
                    return isMeasurable(element.getImage().getHeight());
                }
            case ARC:
            case TEXT:
            case SPANNABLE:
                return true;
            case INNER_NOT_SET:
                return false;
            default: // TODO(b/276703002): Remove default case
                return false;
        }
    }

    private boolean isMeasurable(ContainerDimension dimension) {
        return dimensionToPx(dimension) != LayoutParams.MATCH_PARENT;
    }

    private static boolean isMeasurable(ImageDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
            case PROPORTIONAL_DIMENSION:
                return true;
            case EXPANDED_DIMENSION:
            case INNER_NOT_SET:
                return false;
        }
        return false;
    }

    private static boolean isMeasurable(SpacerDimension dimension) {
        switch (dimension.getInnerCase()) {
            case LINEAR_DIMENSION:
                return true;
            case INNER_NOT_SET:
                return false;
        }
        return false;
    }

    private void inflateChildElements(
            @NonNull ViewGroup parent,
            @NonNull LayoutParams parentLayoutParams,
            PendingLayoutParams childLayoutParams,
            List<LayoutElement> childElements,
            String parentPosId,
            LayoutInfo.Builder layoutInfoBuilder,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        int index = FIRST_CHILD_INDEX;
        for (LayoutElement childElement : childElements) {
            String childPosId = ProtoLayoutDiffer.createNodePosId(parentPosId, index++);
            inflateLayoutElement(
                    new ParentViewWrapper(parent, parentLayoutParams, childLayoutParams),
                    childElement,
                    childPosId,
                    /* includeChildren= */ true,
                    layoutInfoBuilder,
                    pipelineMaker);
        }
    }

    /**
     * Inflates a ProtoLayout into {@code parent}.
     *
     * @param parent The view to attach the layout into.
     * @return The {@link InflateResult} class containing the first child that was inflated,
     *     animations to be played, and new nodes for the dynamic data pipeline. Callers should use
     *     {@link InflateResult#updateDynamicDataPipeline} to apply those changes using a UI Thread.
     *     <p>This may be null if the proto is empty the top-level LayoutElement has no inner set,
     *     or the top-level LayoutElement contains an unsupported inner type.
     */
    @Nullable
    public InflateResult inflate(@NonNull ViewGroup parent) {

        // This is a full re-inflation, so we don't need any previous rendering information.
        LayoutInfo.Builder layoutInfoBuilder =
                new LayoutInfo.Builder(/* previousLayoutInfo= */ null);

        // Go!
        Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker =
                mDataPipeline.map(
                        p ->
                                p.newPipelineMaker(
                                        ProtoLayoutInflater::getEnterAnimations,
                                        ProtoLayoutInflater::getExitAnimations));
        InflatedView firstInflatedChild =
                inflateLayoutElement(
                        new ParentViewWrapper(
                                parent,
                                new LayoutParams(
                                        LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)),
                        mLayoutProto.getRoot(),
                        ROOT_NODE_ID,
                        /* includeChildren= */ true,
                        layoutInfoBuilder,
                        pipelineMaker);
        if (firstInflatedChild == null) {
            return null;
        }
        if (mLayoutProto.hasFingerprint()) {
            parent.setTag(
                    R.id.rendered_metadata_tag,
                    new RenderedMetadata(mLayoutProto.getFingerprint(), layoutInfoBuilder.build()));
        }
        return new InflateResult(parent, firstInflatedChild.mView, pipelineMaker);
    }

    /**
     * Compute the mutation that must be applied to the given {@link ViewGroup} in order to produce
     * the given target layout.
     *
     * <p>If the return value is {@code null}, {@code parent} must be updated in full using {@link
     * #inflate}. Otherwise, call {ViewGroupMutation#isNoOp} on the return value to check if there
     * are any mutations to apply and call {@link #applyMutation} to apply them.
     *
     * <p>Can be called from a background thread.
     *
     * @param prevRenderedMetadata The metadata for the previous rendering of this view, either
     *     using {@code inflate} or {@code applyMutation}. This can be retrieved by calling {@link
     *     #getRenderedMetadata} on the previous layout view parent.
     * @param targetLayout The target layout that the mutation should result in.
     * @return The mutation that will produce the target layout.
     */
    @Nullable
    public ViewGroupMutation computeMutation(
            @NonNull RenderedMetadata prevRenderedMetadata, @NonNull Layout targetLayout) {
        if (prevRenderedMetadata.getTreeFingerprint() == null) {
            Log.w(TAG, "No previous fingerprint available.");
            return null;
        }
        @Nullable
        LayoutDiff diff =
                ProtoLayoutDiffer.getDiff(prevRenderedMetadata.getTreeFingerprint(), targetLayout);
        if (diff == null) {
            Log.w(TAG, "getDiff failed");
            return null;
        }

        List<InflatedView> inflatedViews = new ArrayList<>();
        LayoutInfo.Builder layoutInfoBuilder =
                new LayoutInfo.Builder(prevRenderedMetadata.getLayoutInfo());
        LayoutInfo prevLayoutInfo = prevRenderedMetadata.getLayoutInfo();
        Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker =
                mDataPipeline.map(
                        p ->
                                p.newPipelineMaker(
                                        ProtoLayoutInflater::getEnterAnimations,
                                        ProtoLayoutInflater::getExitAnimations));
        for (TreeNodeWithChange changedNode : diff.getChangedNodes()) {
            String nodePosId = changedNode.getPosId();
            if (nodePosId.isEmpty()) {
                // Failed to compute mutation. Need to update fully.
                Log.w(TAG, "Empty nodePosId");
                return null;
            }
            ViewProperties parentInfo;
            if (nodePosId.equals(ROOT_NODE_ID)) {
                parentInfo = ViewProperties.EMPTY;
            } else {
                String parentNodePosId = getParentNodePosId(nodePosId);
                if (parentNodePosId == null || !prevLayoutInfo.contains(parentNodePosId)) {
                    // Failed to compute mutation. Need to update fully.
                    Log.w(TAG, "Can't find view " + nodePosId);
                    return null;
                }

                // The parent node might also have been updated.
                @Nullable
                ViewProperties possibleUpdatedParentInfo =
                        layoutInfoBuilder.getViewPropertiesFor(parentNodePosId);
                parentInfo =
                        possibleUpdatedParentInfo != null
                                ? possibleUpdatedParentInfo
                                : checkNotNull(
                                        prevLayoutInfo.getViewPropertiesFor(parentNodePosId));
            }
            InflatedView inflatedView = null;
            @Nullable LayoutElement updatedLayoutElement = changedNode.getLayoutElement();
            @Nullable ArcLayoutElement updatedArcLayoutElement = changedNode.getArcLayoutElement();
            if (updatedLayoutElement != null) {
                inflatedView =
                        inflateLayoutElement(
                                new ParentViewWrapper(parentInfo),
                                updatedLayoutElement,
                                nodePosId,
                                !changedNode.isSelfOnlyChange(),
                                layoutInfoBuilder,
                                pipelineMaker);
            } else if (updatedArcLayoutElement != null) {
                inflatedView =
                        inflateArcLayoutElement(
                                new ParentViewWrapper(parentInfo),
                                updatedArcLayoutElement,
                                nodePosId,
                                layoutInfoBuilder,
                                pipelineMaker);
            }
            if (inflatedView == null) {
                // Failed to compute mutation. Need to update fully.
                Log.w(TAG, "No inflatedView");
                return null;
            }
            inflatedViews.add(inflatedView);

            if (!ProtoLayoutDiffer.UPDATE_ALL_CHILDREN_AFTER_ADD_REMOVE) {

                throw new UnsupportedOperationException();
            }
            if (!changedNode.isSelfOnlyChange()) {
                // A child addition/removal causes a full reinflation of the parent. So the only
                // case that we might not replace a node in pipeline is when it's removed as part of
                // a parent change.
                pipelineMaker.ifPresent(p -> p.markForChildRemoval(nodePosId));
            }
            pipelineMaker.ifPresent(
                    p -> p.markNodeAsChanged(nodePosId, !changedNode.isSelfOnlyChange()));
        }
        return new ViewGroupMutation(
                inflatedViews,
                new RenderedMetadata(targetLayout.getFingerprint(), layoutInfoBuilder.build()),
                prevRenderedMetadata.getTreeFingerprint().getRoot(),
                pipelineMaker);
    }

    /** Apply the mutation that was previously computed with {@link #computeMutation}. */
    @UiThread
    @NonNull
    public ListenableFuture<Void> applyMutation(
            @NonNull ViewGroup parent, @NonNull ViewGroupMutation groupMutation) {
        RenderedMetadata prevRenderedMetadata = getRenderedMetadata(parent);
        if (prevRenderedMetadata != null
                && !ProtoLayoutDiffer.areNodesEquivalent(
                        prevRenderedMetadata.getTreeFingerprint().getRoot(),
                        groupMutation.mPreMutationRootNodeFingerprint)) {

            // be considered unequal. Log.e(TAG, "View has changed. Skipping mutation."); return
            // false;
        }
        if (groupMutation.isNoOp()) {
            // Nothing to do.
            return immediateVoidFuture();
        }

        if (groupMutation.mPipelineMaker.isPresent()) {
            SettableFuture<Void> result = SettableFuture.create();
            groupMutation
                    .mPipelineMaker
                    .get()
                    .playExitAnimations(
                            parent,
                            /* isReattaching= */ false,
                            () -> {
                                try {
                                    applyMutationInternal(parent, groupMutation);
                                    result.set(null);
                                } catch (ViewMutationException ex) {
                                    result.setException(ex);
                                }
                            });
            return result;
        } else {
            try {
                applyMutationInternal(parent, groupMutation);
                return immediateVoidFuture();
            } catch (ViewMutationException ex) {
                return immediateFailedFuture(ex);
            }
        }
    }

    private void applyMutationInternal(
            @NonNull ViewGroup parent, @NonNull ViewGroupMutation groupMutation) {
        for (InflatedView inflatedView : groupMutation.mInflatedViews) {
            String posId = inflatedView.getTag();
            if (posId == null) {
                // Failed to apply the mutation. Need to update fully.
                throw new ViewMutationException("View has no tag");
            }
            View viewToUpdate = parent.findViewWithTag(posId);
            if (viewToUpdate == null) {
                // Failed to apply the mutation. Need to update fully.
                throw new ViewMutationException("Can't find view " + posId);
            }
            ViewParent potentialImmediateParent = viewToUpdate.getParent();
            if (!(potentialImmediateParent instanceof ViewGroup)) {
                // Failed to apply the mutation. Need to update fully.
                throw new ViewMutationException("Parent not a ViewGroup");
            }
            ViewGroup immediateParent = (ViewGroup) potentialImmediateParent;
            int childIndex = immediateParent.indexOfChild(viewToUpdate);
            if (childIndex == -1) {
                // Failed to apply the mutation. Need to update fully.
                throw new ViewMutationException("Can't find child at " + childIndex);
            }
            if (!inflatedView.addMissingChildrenFrom(viewToUpdate)) {
                throw new ViewMutationException("Failed to add missing children " + posId);
            }
            immediateParent.removeViewAt(childIndex);
            immediateParent.addView(inflatedView.mView, childIndex, inflatedView.mLayoutParams);

            if (DEBUG_DIFF_UPDATE_ENABLED) {
                // Visualize diff update (by flashing the inflated element).
                inflatedView
                        .mView
                        .animate()
                        .alpha(0.7f)
                        .setDuration(50)
                        .withEndAction(() -> inflatedView.mView.animate().alpha(1).setDuration(50));
            }
        }
        groupMutation.mPipelineMaker.ifPresent(
                pipe -> pipe.commit(parent, /* isReattaching= */ false));
        parent.setTag(R.id.rendered_metadata_tag, groupMutation.mRenderedMetadataAfterMutation);
    }

    /** Returns the {@link RenderedMetadata} attached to {@code inflateParent}. */
    @UiThread
    @Nullable
    public static RenderedMetadata getRenderedMetadata(@NonNull ViewGroup inflateParent) {
        Object prevMetadataObject = inflateParent.getTag(R.id.rendered_metadata_tag);
        if (prevMetadataObject instanceof RenderedMetadata) {
            return (RenderedMetadata) prevMetadataObject;
        } else {
            if (prevMetadataObject != null) {
                Log.w(TAG, "Incompatible prevMetadataObject");
            }
            return null;
        }
    }

    /** Clears the {@link RenderedMetadata} attached to {@code inflateParent}. */
    @UiThread
    public static void clearRenderedMetadata(@NonNull ViewGroup inflateParent) {
        Log.d(TAG, "Clearing rendered metadata. Next inflation won't use diff update.");
        inflateParent.setTag(R.id.rendered_metadata_tag, /* tag= */ null);
    }

    // dereference of possibly-null reference ((FrameLayout.LayoutParams)child.getLayoutParams())
    @SuppressWarnings("nullness:dereference.of.nullable")
    private static void applyGravityToFrameLayoutChildren(FrameLayout parent, int gravity) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);

            // All children should have a LayoutParams already set...
            if (!(child.getLayoutParams() instanceof FrameLayout.LayoutParams)) {
                // This...shouldn't happen.
                throw new IllegalStateException(
                        "Layout params of child is not a descendant of FrameLayout.LayoutParams.");
            }

            // Children should grow out from the middle of the layout.
            ((FrameLayout.LayoutParams) child.getLayoutParams()).gravity = gravity;
        }
    }

    static String roleToClassName(SemanticsRole role) {
        switch (role) {
            case SEMANTICS_ROLE_IMAGE:
                return "android.widget.ImageView";
            case SEMANTICS_ROLE_BUTTON:
                return "android.widget.Button";
            case SEMANTICS_ROLE_CHECKBOX:
                return "android.widget.CheckBox";
            case SEMANTICS_ROLE_SWITCH:
                return "android.widget.Switch";
            case SEMANTICS_ROLE_RADIOBUTTON:
                return "android.widget.RadioButton";
            default:
                return "";
        }
    }

    // getObsoleteContentDescription is used for backward compatibility
    @SuppressWarnings("deprecation")
    private void applySemantics(
            View view,
            Semantics semantics,
            String posId,
            Optional<ProtoLayoutDynamicDataPipeline.PipelineMaker> pipelineMaker) {
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        ViewCompat.setAccessibilityDelegate(
                view,
                new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);

                        String className = roleToClassName(semantics.getRole());
                        if (!className.isEmpty()) {
                            info.setClassName(className);
                        }
                        info.setFocusable(true);
                        info.setImportantForAccessibility(true);
                    }
                });

        if (semantics.hasContentDescription()) {
            handleProp(
                    semantics.getContentDescription(),
                    view::setContentDescription,
                    posId,
                    pipelineMaker);
        } else {
            // This is for backward compatibility
            view.setContentDescription(semantics.getObsoleteContentDescription());
        }

        if (semantics.hasStateDescription()) {
            handleProp(
                    semantics.getStateDescription(),
                    (state) -> ViewCompat.setStateDescription(view, state),
                    posId,
                    pipelineMaker);
        }
    }

    /** Creates a TextView with the fallbackTextAppearance from the current theme. */
    private TextView newThemedTextView() {
        return new TextView(
                mProtoLayoutThemeContext,
                /* attrs= */ null,
                mProtoLayoutTheme.getFallbackTextAppearanceResId());
    }

    /** Creates a CurvedTextView with the fallbackTextAppearance from the current theme. */
    private CurvedTextView newThemedCurvedTextView() {
        return new CurvedTextView(
                mProtoLayoutThemeContext,
                /* attrs= */ null,
                mProtoLayoutTheme.getFallbackTextAppearanceResId());
    }

    /** Implementation of ClickableSpan for ProtoLayout's Clickables. */
    private class ProtoLayoutClickableSpan extends ClickableSpan {
        private final Clickable mClickable;

        ProtoLayoutClickableSpan(@NonNull Clickable clickable) {
            this.mClickable = clickable;
        }

        @Override
        public void onClick(@NonNull View widget) {
            Action action = mClickable.getOnClick();

            switch (action.getValueCase()) {
                case LAUNCH_ACTION:
                    Intent i =
                            buildLaunchActionIntent(
                                    action.getLaunchAction(),
                                    mClickable.getId(),
                                    mClickableIdExtra);
                    if (i != null) {
                        dispatchLaunchActionIntent(i);
                    }
                    break;
                case LOAD_ACTION:
                    if (mLoadActionExecutor == null) {
                        Log.w(TAG, "Ignoring load action since an executor has not been provided.");
                        break;
                    }
                    mLoadActionExecutor.execute(
                                    () ->
                                        mLoadActionListener.onClick(
                                                buildState(
                                                        action.getLoadAction(),
                                                        mClickable.getId())));
                    break;
                case VALUE_NOT_SET:
                    break;
            }
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            // Don't change the underlying text appearance.
        }
    }

    // Android's normal ImageSpan (well, DynamicDrawableSpan) applies baseline alignment incorrectly
    // in some cases. It incorrectly assumes that the difference between the bottom (as passed to
    // draw) and baseline of the text is always equal to the font descent, when that doesn't always
    // hold. Instead, the "y" parameter is the Y coordinate of the baseline, so base the baseline
    // alignment on that rather than "bottom".
    @VisibleForTesting
    static class FixedImageSpan extends ImageSpan {
        @Nullable private WeakReference<Drawable> mDrawableRef;

        FixedImageSpan(@NonNull Drawable drawable) {
            super(drawable);
        }

        FixedImageSpan(@NonNull Drawable drawable, int verticalAlignment) {
            super(drawable, verticalAlignment);
        }

        @Override
        public void draw(
                @NonNull Canvas canvas,
                CharSequence text,
                int start,
                int end,
                float x,
                int top,
                int y,
                int bottom,
                @NonNull Paint paint) {
            Drawable b = getCachedDrawable();
            canvas.save();

            int transY = bottom - b.getBounds().bottom;
            if (mVerticalAlignment == ALIGN_BASELINE) {
                transY = y - b.getBounds().bottom;
            } else if (mVerticalAlignment == ALIGN_CENTER) {
                transY = (bottom - top) / 2 - b.getBounds().height() / 2;
            }

            canvas.translate(x, transY);
            b.draw(canvas);
            canvas.restore();
        }

        @VisibleForTesting
        Drawable getCachedDrawable() {
            WeakReference<Drawable> wr = mDrawableRef;
            Drawable d = null;

            if (wr != null) {
                d = wr.get();
            }

            if (d == null) {
                d = getDrawable();
                mDrawableRef = new WeakReference<>(d);
            }

            return d;
        }
    }

    /** Implementation of {@link Scroller} which inhibits all scrolling. */
    private static class InhibitingScroller extends Scroller {
        InhibitingScroller(Context context) {
            super(context);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {}

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {}
    }
}
