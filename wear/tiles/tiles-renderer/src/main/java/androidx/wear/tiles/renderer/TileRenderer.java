/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer;

import static androidx.core.util.Preconditions.checkNotNull;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import androidx.wear.protolayout.StateBuilders;
import androidx.wear.protolayout.expression.pipeline.StateStore;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance;
import androidx.wear.tiles.TileService;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Renderer for Wear Tiles.
 *
 * <p>This variant uses Android views to represent the contents of the Wear Tile.
 */
public final class TileRenderer {
    /**
     * Listener for clicks on Clickable objects that have an Action to (re)load the contents of a
     * tile.
     *
     * @deprecated Use {@link Consumer<StateBuilders.State>} with {@link #TileRenderer(Context,
     *     Executor, Consumer)}.
     */
    @Deprecated
    public interface LoadActionListener {

        /**
         * Called when a Clickable that has a LoadAction is clicked.
         *
         * @param nextState The state that the next tile should be in.
         */
        void onClick(@NonNull androidx.wear.tiles.StateBuilders.State nextState);
    }

    @NonNull private final ProtoLayoutViewInstance mInstance;
    @Nullable private final LayoutElementProto.Layout mLayout;
    @Nullable private final ResourceProto.Resources mResources;
    @NonNull private final ListeningExecutorService mUiExecutor;

    /**
     * Default constructor.
     *
     * @param uiContext A {@link Context} suitable for interacting with the UI.
     * @param layout The portion of the Tile to render.
     * @param resources The resources for the Tile.
     * @param loadActionExecutor Executor for {@code loadActionListener}.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     * @deprecated Use {@link #TileRenderer(Context, Executor, Consumer)} which accepts Layout and
     *     Resources in {@link #inflateAsync(LayoutElementBuilders.Layout,
     *     ResourceBuilders.Resources, ViewGroup)} method.
     */
    @Deprecated
    public TileRenderer(
            @NonNull Context uiContext,
            @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout,
            @NonNull androidx.wear.tiles.ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this(
                uiContext,
                loadActionExecutor,
                toStateConsumer(loadActionListener),
                layout.toProto(),
                resources.toProto());
    }

    /**
     * Default constructor.
     *
     * @param uiContext A {@link Context} suitable for interacting with the UI.
     * @param layout The portion of the Tile to render.
     * @param tilesTheme The theme to use for this Tile instance. This can be used to customise
     *     things like the default font family. Pass 0 to use the default theme.
     * @param resources The resources for the Tile.
     * @param loadActionExecutor Executor for {@code loadActionListener}.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     * @deprecated Use {@link #TileRenderer(Context, Executor, Consumer)} which accepts Layout and
     *     Resources in {@link #inflateAsync(LayoutElementBuilders.Layout,
     *     ResourceBuilders.Resources, ViewGroup)} method.
     */
    @Deprecated
    public TileRenderer(
            @NonNull Context uiContext,
            @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout,
            @StyleRes int tilesTheme,
            @NonNull androidx.wear.tiles.ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        // TODO(b/272527869): Enable setting theme.
        this(
                uiContext,
                loadActionExecutor,
                toStateConsumer(loadActionListener),
                layout.toProto(),
                resources.toProto());
    }

    /**
     * @param uiContext A {@link Context} suitable for interacting with the UI.
     * @param loadActionExecutor Executor for {@code loadActionListener}.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRenderer(
            @NonNull Context uiContext,
            @NonNull Executor loadActionExecutor,
            @NonNull Consumer<StateBuilders.State> loadActionListener) {
        this(
                uiContext,
                loadActionExecutor,
                loadActionListener,
                /* layout= */ null,
                /* resources= */ null);
    }

    private TileRenderer(
            @NonNull Context uiContext,
            @NonNull Executor loadActionExecutor,
            @NonNull Consumer<StateBuilders.State> loadActionListener,
            @Nullable LayoutElementProto.Layout layout,
            @Nullable ResourceProto.Resources resources) {
        this.mLayout = layout;
        this.mResources = resources;
        this.mUiExecutor = MoreExecutors.newDirectExecutorService();
        ProtoLayoutViewInstance.LoadActionListener instanceListener =
                nextState ->
                        loadActionExecutor.execute(
                                () ->
                                        loadActionListener.accept(
                                                StateBuilders.State.fromProto(nextState)));

        ProtoLayoutViewInstance.Config.Builder config =
                new ProtoLayoutViewInstance.Config.Builder(
                                uiContext, mUiExecutor, mUiExecutor, TileService.EXTRA_CLICKABLE_ID)
                        .setAnimationEnabled(true)
                        .setIsViewFullyVisible(true)
                        .setStateStore(new StateStore(ImmutableMap.of()))
                        .setLoadActionListener(instanceListener);
        this.mInstance = new ProtoLayoutViewInstance(config.build());
    }

    @NonNull
    @SuppressWarnings("deprecation") // TODO(b/276343540): Use protolayout types
    private static Consumer<StateBuilders.State> toStateConsumer(
            @NonNull LoadActionListener loadActionListener) {
        return nextState ->
                loadActionListener.onClick(
                        androidx.wear.tiles.StateBuilders.State.fromProto(nextState.toProto()));
    }

    /**
     * Inflates a Tile into {@code parent}.
     *
     * @param parent The view to attach the tile into.
     * @return The first child that was inflated. This may be null if the Layout is empty or the
     *     top-level LayoutElement has no inner set, or the top-level LayoutElement contains an
     *     unsupported inner type.
     * @deprecated Use {@link #inflateAsync(LayoutElementBuilders.Layout,
     *     ResourceBuilders.Resources, ViewGroup)} instead. Note: This method only works with the
     *     deprecated constructors that accept Layout and Resources.
     */
    @Deprecated
    @Nullable
    public View inflate(@NonNull ViewGroup parent) {
        String errorMessage =
                "This method only works with the deprecated constructors that accept Layout and"
                    + " Resources.";
        try {
            // Waiting for the result from future for backwards compatibility.
            return inflateLayout(
                    checkNotNull(mLayout, errorMessage),
                    checkNotNull(mResources, errorMessage),
                    parent).get(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | CancellationException |
                 TimeoutException e) {
            // Wrap checked exceptions to avoid changing the method signature.
            throw new RuntimeException("Rendering tile has not successfully finished.", e);
        }
    }

    /**
     * Inflates a Tile into {@code parent}.
     *
     * @param layout    The portion of the Tile to render.
     * @param resources The resources for the Tile.
     * @param parent    The view to attach the tile into.
     * @return The future with the first child that was inflated. This may be null if the Layout is
     * empty or the top-level LayoutElement has no inner set, or the top-level LayoutElement
     * contains an
     * unsupported inner type.
     */
    @NonNull
    public ListenableFuture<View> inflateAsync(
            @NonNull LayoutElementBuilders.Layout layout,
            @NonNull ResourceBuilders.Resources resources,
            @NonNull ViewGroup parent) {
        return inflateLayout(layout.toProto(), resources.toProto(), parent);
    }

    @NonNull
    private ListenableFuture<View> inflateLayout(
            @NonNull LayoutElementProto.Layout layout,
            @NonNull ResourceProto.Resources resources,
            @NonNull ViewGroup parent) {
        ListenableFuture<Void> result = mInstance.renderAndAttach(layout, resources, parent);
            return FluentFuture.from(result)
                    .transform(ignored -> parent.getChildAt(0), mUiExecutor);
    }
}
