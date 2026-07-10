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

package androidx.wear.tiles

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo
import androidx.concurrent.futures.SuspendToFutureAdapter
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.ProtoLayoutScope
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.ResourceBuilders.Resources
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.createMaterialScope
import androidx.wear.tiles.RequestBuilders.ResourcesRequest
import androidx.wear.tiles.RequestBuilders.TileRequest
import androidx.wear.tiles.TileBuilders.Tile
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope

/**
 * Base class for a service providing data for an app's tile that includes [ProtoLayoutScope] for
 * automatic resource registration and opinionated [MaterialScope] for Material3 styling of
 * components and layout.
 *
 * A provider service must implement [tileResponse] to respond to requests for updates from the
 * system. Tile response should contain layout and resources in the same response.
 *
 * The manifest declaration of this service must include an intent filter for
 * [TileService.ACTION_BIND_TILE_PROVIDER].
 *
 * The manifest entry should also include
 * `android:permission="com.google.android.wearable.permission.BIND_TILE_PROVIDER"` to ensure that
 * only the system can bind to it.
 *
 * @param allowDynamicTheme if dynamic colors theme should be used on Material3 components, meaning
 *   that colors will follow the system theme if enabled on the device. If not overridden, defaults
 *   to using the system theme.
 * @param defaultColorScheme the default static color theme to be used, when [allowDynamicTheme] is
 *   false, or when dynamic theming is disabled by the system or user. If not overridden, defaults
 *   to default theme.
 * @param serviceScope the scope to be used for [tileResponse] call, otherwise the scope running on
 *   a main thread will be used.
 * @sample androidx.wear.tiles.snippet_samples.material3TileServiceHelloWorld
 */
public abstract class Material3TileService
@JvmOverloads // For hilt support
public constructor(
    private val allowDynamicTheme: Boolean = true,
    private val defaultColorScheme: ColorScheme = ColorScheme(),
    private val serviceScope: CoroutineScope? = null,
) : TileService() {
    /**
     * Called when the system is requesting a new timeline from this Tile Provider.
     *
     * Resulting [Tile] can be created within the receiver [MaterialScope] which is used by all
     * ProtoLayout Material3 components and layout to support opinionated defaults and to provide
     * the global information for styling Material3 components. Additionally, the [MaterialScope]
     * contains [ProtoLayoutScope] for automatic resource registration for any [Image] elements used
     * within a Tile.
     *
     * Note that, all resources used within the tile layout should be directly embedded as
     * [ResourceBuilders.ImageResource] in layout using the corresponding API (such as
     * [androidx.wear.protolayout.layout.basicImage] or Material3 counterparts (for example
     * [androidx.wear.protolayout.material3.backgroundImage]).
     *
     * This runs a suspending function on the Main thread, unless [serviceScope] is overridden.
     *
     * @param requestParams Parameters about the request. See [TileRequest] for more info. Note that
     *   handle to some of them exists in the [MaterialScope] (for example `deviceConfiguration` for
     *   [DeviceParameters].
     */
    protected abstract suspend fun MaterialScope.tileResponse(requestParams: TileRequest): Tile

    @CallSuper
    override fun onCreate() {
        super.onCreate()
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * Default implementation of [TileService.onTileRequest] that creates [MaterialScope] with
     * [ProtoLayoutScope] and calls provider's [tileResponse] to get the layout and resources.
     */
    @Suppress("AsyncSuffixFuture") // Overriding existing API
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected final override fun onTileRequest(requestParams: TileRequest): ListenableFuture<Tile> {
        return materialScopeWithResourcesForTile(
            context = this,
            deviceConfiguration = requestParams.deviceConfiguration,
            protoLayoutScope = requestParams.scope,
            allowDynamicTheme = allowDynamicTheme,
            defaultColorScheme = defaultColorScheme,
        ) {
            // TODO: b/445115718 - Replace this with lifecycle scope.
            SuspendToFutureAdapter.launchFuture(
                serviceScope?.coroutineContext ?: EmptyCoroutineContext
            ) {
                tileResponse(requestParams)
            }
        }
    }

    /**
     * Noop implementation of [TileService.onTileResourcesRequest] as [ProtoLayoutScope] and its
     * APIs should be used.
     */
    @Suppress(
        "AsyncSuffixFuture"
    ) // Overriding existing API and ProtoLayout library is in the same ownership
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected final override fun onTileResourcesRequest(
        requestParams: ResourcesRequest
    ): ListenableFuture<Resources> =
        createImmediateFuture(Resources.Builder().setVersion(requestParams.version).build())

    // Restrict overrides so that clients can't override it so it's not misused
    @Deprecated("use #onRecentInteractionEventsAsync(List)")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected final override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {}

    // Restrict overrides so that clients can't override it so it's not misused
    @Deprecated("use #onRecentInteractionEventsAsync(List)")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    protected final override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) {}
}

/**
 * Fork of `MaterialScope.materialScopeWithResources` to have [Tile] as a slot instead of
 * [LayoutElementBuilders.LayoutElement].
 */
private fun materialScopeWithResourcesForTile(
    context: Context,
    protoLayoutScope: ProtoLayoutScope,
    deviceConfiguration: DeviceParameters,
    allowDynamicTheme: Boolean,
    defaultColorScheme: ColorScheme,
    layout: MaterialScope.() -> ListenableFuture<Tile>,
): ListenableFuture<Tile> =
    createMaterialScope(
            context = context,
            deviceConfiguration = deviceConfiguration,
            protoLayoutScope = protoLayoutScope,
            allowDynamicTheme = allowDynamicTheme,
            defaultColorScheme = defaultColorScheme,
        )
        .layout()
