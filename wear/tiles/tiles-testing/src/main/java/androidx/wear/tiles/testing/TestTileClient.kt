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

package androidx.wear.tiles.testing

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.client.TileClient
import androidx.wear.tiles.connection.DefaultTileClient
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import java.util.concurrent.Executor

/**
 * [TileClient] for testing purposes. This will pass calls through to the given instance of
 * [TileService], handling all the service binding (via Robolectric), and
 * serialization/deserialization.
 *
 * Note that this class will not drive the full service lifecycle for the passed service instance.
 * On the first call to any of these methods, it will call your service's [Service.onCreate] method,
 * however, it will never call [Service.onDestroy]. Equally, where [DefaultTileClient] will
 * unbind after a period of time, potentially destroying the service, this class wil Client will
 * unbind, but not destroy the service. If you wish to test service destruction, you can instead
 * call [Service.onDestroy] on the passed in `service` instance.
 */
public class TestTileClient<T : TileService> :
    TileClient {
    private val controller: ServiceController<T>
    private val componentName: ComponentName
    private val innerTileService: DefaultTileClient
    private var hasBound = false

    /**
     * Build a [TestTileClient] for use with a coroutine dispatcher.
     *
     * @param service An instance of the [TileService] class to bind to.
     * @param coroutineScope A [CoroutineScope] to use when dispatching calls to the
     *   [TileService]. Cancelling the passed [CoroutineScope] will also cancel any pending
     *   work in this class.
     * @param coroutineDispatcher A [CoroutineDispatcher] to use when dispatching work from this
     *   class.
     */
    public constructor(
        service: T,
        coroutineScope: CoroutineScope,
        coroutineDispatcher: CoroutineDispatcher
    ) {
        val bindIntent = Intent(TileService.ACTION_BIND_TILE_PROVIDER)
        this.componentName = ComponentName(getApplicationContext(), service.javaClass)

        bindIntent.component = componentName
        this.controller = ServiceController.of(service, bindIntent)

        this.innerTileService = DefaultTileClient(
            getApplicationContext(),
            componentName,
            coroutineScope,
            coroutineDispatcher
        )
    }

    /**
     * Build a [TestTileClient] for use with a given [Executor]
     *
     * @param service An instance of the [TileService] class to bind to.
     * @param executor An [Executor] to use when dispatching calls to the [TileService].
     */
    public constructor(service: T, executor: Executor) {
        val bindIntent = Intent(TileService.ACTION_BIND_TILE_PROVIDER)
        this.componentName = ComponentName(getApplicationContext(), service.javaClass)

        bindIntent.component = componentName
        this.controller = ServiceController.of(service, bindIntent)

        this.innerTileService = DefaultTileClient(
            getApplicationContext(),
            componentName,
            executor
        )
    }

    override fun requestApiVersion(): ListenableFuture<Int> {
        maybeBind()
        return innerTileService.requestApiVersion()
    }

    override fun requestTile(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        maybeBind()
        return innerTileService.requestTile(requestParams)
    }

    override fun requestResources(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        maybeBind()
        return innerTileService.requestResources(requestParams)
    }

    override fun sendOnTileAddedEvent(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileService.sendOnTileAddedEvent()
    }

    override fun sendOnTileRemovedEvent(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileService.sendOnTileRemovedEvent()
    }

    override fun sendOnTileEnterEvent(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileService.sendOnTileEnterEvent()
    }

    override fun sendOnTileLeaveEvent(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileService.sendOnTileLeaveEvent()
    }

    private fun maybeBind() {
        if (!hasBound) {
            val binder = controller.create().get().onBind(controller.intent)

            shadowOf(getApplicationContext<Application>())
                .setComponentNameAndServiceForBindServiceForIntent(
                    controller.intent,
                    componentName,
                    binder
                )
        }
    }
}