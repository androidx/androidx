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
import androidx.wear.protolayout.expression.pipeline.ObservableStateStore;
import androidx.wear.protolayout.proto.LayoutElementProto;
import androidx.wear.protolayout.proto.ResourceProto;
import androidx.wear.protolayout.proto.StateProto;
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.StateBuilders;
import androidx.wear.tiles.TileService;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Renderer for Wear Tiles.
 *
 * <p>This variant uses Android views to represent the contents of the Wear Tile.
 */
public final class TileRenderer {
    /**
     * Listener for clicks on Clickable objects that have an Action to (re)load the contents of a
     * tile.
     */
    public interface LoadActionListener {

        /**
         * Called when a Clickable that has a LoadAction is clicked.
         *
         * @param nextState The state that the next tile should be in.
         */
        void onClick(@NonNull StateBuilders.State nextState);
    }

    @NonNull private final ProtoLayoutViewInstance mInstance;
    @NonNull private final LayoutElementProto.Layout mLayout;
    @NonNull private final ResourceProto.Resources mResources;
    @NonNull private final ListeningExecutorService mUiExecutor;

    /**
     * Default constructor.
     *
     * @param uiContext A {@link Context} suitable for interacting with the UI.
     * @param layout The portion of the Tile to render.
     * @param resources The resources for the Tile.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRenderer(
            @NonNull Context uiContext,
            @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout,
            @NonNull androidx.wear.tiles.ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this(
                uiContext,
                layout,
                /* tilesTheme= */ 0,
                resources,
                loadActionExecutor,
                loadActionListener);
    }

    /**
     * Default constructor.
     *
     * @param uiContext A {@link Context} suitable for interacting with the UI.
     * @param layout The portion of the Tile to render.
     * @param tilesTheme The theme to use for this Tile instance. This can be used to customise
     *     things like the default font family. Pass 0 to use the default theme.
     * @param resources The resources for the Tile.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRenderer(
            @NonNull Context uiContext,
            @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout,
            @StyleRes int tilesTheme,
            @NonNull androidx.wear.tiles.ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this.mLayout = fromTileLayout(layout);
        this.mResources = fromTileResources(resources);
        this.mUiExecutor = MoreExecutors.newDirectExecutorService();
        ProtoLayoutViewInstance.LoadActionListener instanceListener =
                nextState -> loadActionExecutor.execute(
                        () -> loadActionListener.onClick(fromProtoLayoutState(nextState)));

        ProtoLayoutViewInstance.Config.Builder config =
                new ProtoLayoutViewInstance.Config.Builder(uiContext, mUiExecutor, mUiExecutor,
                        TileService.EXTRA_CLICKABLE_ID)
                        .setAnimationEnabled(true)
                        .setIsViewFullyVisible(true)
                        .setStateStore(new ObservableStateStore(ImmutableMap.of()))
                        .setLoadActionListener(instanceListener);
        this.mInstance = new ProtoLayoutViewInstance(config.build());
    }

    @NonNull private ResourceProto.Resources fromTileResources(
            @NonNull androidx.wear.tiles.ResourceBuilders.Resources resources) {
        return checkNotNull(
                ResourceBuilders.Resources
                        .fromByteArray(resources.toByteArray())).toProto();
    }

    @NonNull private LayoutElementProto.Layout fromTileLayout(
            @NonNull androidx.wear.tiles.LayoutElementBuilders.Layout layout) {
        return checkNotNull(
                LayoutElementBuilders.Layout
                        .fromByteArray(layout.toByteArray())).toProto();
    }

    @NonNull StateBuilders.State fromProtoLayoutState(@NonNull StateProto.State state) {
        return StateBuilders.State.fromProto(state);
    }

    /**
     * Inflates a Tile into {@code parent}.
     *
     * @param parent The view to attach the tile into.
     * @return The first child that was inflated. This may be null if the proto is empty the
     *     top-level LayoutElement has no inner set, or the top-level LayoutElement contains an
     *     unsupported inner type.
     */
    @Nullable
    public View inflate(@NonNull ViewGroup parent) {
        mInstance.renderAndAttach(mLayout, mResources, parent);
        boolean finished;
        try {
            mUiExecutor.shutdown();
            finished = mUiExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Rendering tile has not successfully finished.");
        }
        // TODO(b/271076323): Update when renderAndAttach returns result.
        return finished ? parent.getChildAt(0) : null;
    }
}
