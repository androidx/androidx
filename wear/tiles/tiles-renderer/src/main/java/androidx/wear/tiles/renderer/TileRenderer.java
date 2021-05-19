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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.StateBuilders;
import androidx.wear.tiles.renderer.internal.StandardResourceResolvers;
import androidx.wear.tiles.renderer.internal.TileRendererInternal;

import java.util.concurrent.Executor;

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

    private final TileRendererInternal mRenderer;

    /**
     * Default constructor.
     *
     * @param appContext The application context.
     * @param layout The portion of the Tile to render.
     * @param resources The resources for the Tile.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRenderer(
            @NonNull Context appContext,
            @NonNull LayoutElementBuilders.Layout layout,
            @NonNull ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this(
                appContext,
                layout,
                /* tilesTheme= */ 0,
                resources,
                loadActionExecutor,
                loadActionListener);
    }

    /**
     * Default constructor.
     *
     * @param appContext The application context.
     * @param layout The portion of the Tile to render.
     * @param tilesTheme The theme to use for this Tile instance. This can be used to customise
     *     things like the default font family. Pass 0 to use the default theme.
     * @param resources The resources for the Tile.
     * @param loadActionListener Listener for clicks that will cause the contents to be reloaded.
     */
    public TileRenderer(
            @NonNull Context appContext,
            @NonNull LayoutElementBuilders.Layout layout,
            @StyleRes int tilesTheme,
            @NonNull ResourceBuilders.Resources resources,
            @NonNull Executor loadActionExecutor,
            @NonNull LoadActionListener loadActionListener) {
        this.mRenderer =
                new TileRendererInternal(
                        appContext,
                        layout.toProto(),
                        StandardResourceResolvers.forLocalApp(resources.toProto(), appContext)
                                .build(),
                        tilesTheme,
                        loadActionExecutor,
                        (s) -> loadActionListener.onClick(StateBuilders.State.fromProto(s)));
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
        return mRenderer.inflate(parent);
    }
}
