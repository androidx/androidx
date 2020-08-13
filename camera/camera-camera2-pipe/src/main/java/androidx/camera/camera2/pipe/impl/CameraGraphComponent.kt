/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.impl

import android.os.Process
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.Request
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Qualifier
import javax.inject.Scope

@Scope
annotation class CameraGraphScope

@Qualifier
annotation class ForCameraGraph

@CameraGraphScope
@Subcomponent(modules = [CameraGraphModule::class])
interface CameraGraphComponent {
    fun cameraGraph(): CameraGraph

    @Subcomponent.Builder
    interface Builder {
        fun cameraGraphModule(module: CameraGraphModule): Builder
        fun build(): CameraGraphComponent
    }
}

@Module(
    includes = [
        CameraGraphBindings::class,
        CameraGraphProviders::class]
)
class CameraGraphModule(private val config: CameraGraph.Config) {
    @Provides
    fun provideCameraGraphConfig(): CameraGraph.Config = config
}

@Module
abstract class CameraGraphBindings {
    @Binds
    abstract fun bindCameraGraph(cameraGraph: CameraGraphImpl): CameraGraph

    @Binds
    abstract fun bindGraphProcessor(graphProcessor: GraphProcessorImpl): GraphProcessor
}

@Module
object CameraGraphProviders {
    @CameraGraphScope
    @Provides
    @ForCameraGraph
    fun provideCameraGraphCoroutineScope(
        @ForCameraGraph dispatcher: CoroutineDispatcher
    ): CoroutineScope {
        return CoroutineScope(dispatcher.plus(CoroutineName("CXCP-Graph")))
    }

    @CameraGraphScope
    @Provides
    @ForCameraGraph
    fun provideCameraGraphCoroutineDispatcher(): CoroutineDispatcher {
        // TODO: Figure out how to make sure the dispatcher gets shut down.
        return Executors.newFixedThreadPool(1) {
            object : Thread(it) {
                init {
                    name = "CXCP-Graph"
                }

                override fun run() {
                    Process.setThreadPriority(
                        Process.THREAD_PRIORITY_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE
                    )
                    super.run()
                }
            }
        }.asCoroutineDispatcher()
    }

    @CameraGraphScope
    @Provides
    @ForCameraGraph
    fun provideRequestListeners(
        graphConfig: CameraGraph.Config
    ): java.util.ArrayList<Request.Listener> {
        // TODO: Dagger doesn't appear to like standard kotlin lists. Replace this with a standard
        //   Kotlin list interfaces when dagger compiles with them.
        // TODO: Add internal listeners before adding external global listeners.
        return java.util.ArrayList(graphConfig.listeners)
    }
}