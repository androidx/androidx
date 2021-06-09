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
import androidx.wear.tiles.TileProviderService
import androidx.wear.tiles.client.TileProviderClient
import androidx.wear.tiles.connection.DefaultTileProviderClient
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ServiceController
import java.util.concurrent.Executor

/**
 * TileProviderClient for testing purposes. This will pass calls through to the given instance of
 * [TileProviderService], handling all the service binding (via Robolectric), and
 * serialization/deserialization.
 *
 * Note that this class will not drive the full service lifecycle for the passed service instance.
 * On the first call to any of these methods, it will call your service's [Service.onCreate] method,
 * however, it will never call [Service.onDestroy]. Equally, where [DefaultTileProviderClient] will
 * unbind after a period of time, potentially destroying the service, this class wil Client will
 * unbind, but not destroy the service. If you wish to test service destruction, you can instead
 * call [Service.onDestroy] on the passed in `service` instance.
 */
public class TestingTileProviderClient<T : TileProviderService> : TileProviderClient {
    private val controller: ServiceController<T>
    private val componentName: ComponentName
    private val innerTileProvider: DefaultTileProviderClient
    private var hasBound = false

    public constructor(
        service: T,
        coroutineScope: CoroutineScope,
        coroutineDispatcher: CoroutineDispatcher
    ) {
        val bindIntent = Intent(TileProviderService.ACTION_BIND_TILE_PROVIDER)
        this.componentName = ComponentName(getApplicationContext(), service.javaClass)

        bindIntent.component = componentName
        this.controller = ServiceController.of(service, bindIntent)

        this.innerTileProvider = DefaultTileProviderClient(
            getApplicationContext(),
            componentName,
            coroutineScope,
            coroutineDispatcher
        )
    }

    public constructor(service: T, executor: Executor) {
        val bindIntent = Intent(TileProviderService.ACTION_BIND_TILE_PROVIDER)
        this.componentName = ComponentName(getApplicationContext(), service.javaClass)

        bindIntent.component = componentName
        this.controller = ServiceController.of(service, bindIntent)

        this.innerTileProvider = DefaultTileProviderClient(
            getApplicationContext(),
            componentName,
            executor
        )
    }

    override fun getApiVersion(): ListenableFuture<Int> {
        maybeBind()
        return innerTileProvider.apiVersion
    }

    override fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> {
        maybeBind()
        return innerTileProvider.tileRequest(requestParams)
    }

    override fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> {
        maybeBind()
        return innerTileProvider.resourcesRequest(requestParams)
    }

    override fun onTileAdded(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileProvider.onTileAdded()
    }

    override fun onTileRemoved(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileProvider.onTileRemoved()
    }

    override fun onTileEnter(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileProvider.onTileEnter()
    }

    override fun onTileLeave(): ListenableFuture<Void?> {
        maybeBind()
        return innerTileProvider.onTileLeave()
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