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

package androidx.wear.watchface

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.SurfaceHolder
import androidx.test.core.app.ApplicationProvider
import androidx.wear.watchface.control.IInteractiveWatchFace
import androidx.wear.watchface.control.IPendingInteractiveWatchFace
import androidx.wear.watchface.control.InteractiveInstanceManager
import androidx.wear.watchface.control.data.CrashInfoParcel
import androidx.wear.watchface.control.data.WallpaperInteractiveWatchFaceInstanceParams
import androidx.wear.watchface.data.DeviceConfig
import androidx.wear.watchface.data.WatchUiState
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSchema
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.annotation.Config
import java.util.ArrayDeque
import java.util.PriorityQueue

internal class TestAsyncWatchFaceService(
    private val handler: Handler,
    private val factory: AsyncWatchFaceFactory,
    private val watchState: MutableWatchState,
    private val directBootParams: WallpaperInteractiveWatchFaceInstanceParams?
) : WatchFaceService() {
    init {
        attachBaseContext(ApplicationProvider.getApplicationContext())
    }

    abstract class AsyncWatchFaceFactory {
        abstract fun createUserStyleSchema(): UserStyleSchema

        abstract fun createComplicationsManager(
            currentUserStyleRepository: CurrentUserStyleRepository
        ): ComplicationsManager

        abstract fun createWatchFaceAsync(
            surfaceHolder: SurfaceHolder,
            watchState: WatchState,
            complicationsManager: ComplicationsManager,
            currentUserStyleRepository: CurrentUserStyleRepository
        ): Deferred<WatchFace>
    }

    override fun createUserStyleSchema() = factory.createUserStyleSchema()

    override fun createComplicationsManager(
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = factory.createComplicationsManager(currentUserStyleRepository)

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationsManager: ComplicationsManager,
        currentUserStyleRepository: CurrentUserStyleRepository
    ) = factory.createWatchFaceAsync(
        surfaceHolder,
        watchState,
        complicationsManager,
        currentUserStyleRepository
    ).await()

    override fun getUiThreadHandlerImpl() = handler

    override fun getBackgroundThreadHandlerImpl() = handler

    override fun getMutableWatchState() = watchState

    override fun readDirectBootPrefs(
        context: Context,
        fileName: String
    ) = directBootParams

    override fun writeDirectBootPrefs(
        context: Context,
        fileName: String,
        prefs: WallpaperInteractiveWatchFaceInstanceParams
    ) {
    }

    override fun expectPreRInitFlow() = false
}

@Config(manifest = Config.NONE)
@RunWith(WatchFaceTestRunner::class)
public class AsyncWatchFaceInitTest {
    private val handler = mock<Handler>()
    private val surfaceHolder = mock<SurfaceHolder>()
    private var looperTimeMillis = 0L
    private val pendingTasks = PriorityQueue<Task>()
    private val initParams = WallpaperInteractiveWatchFaceInstanceParams(
        "instanceId",
        DeviceConfig(
            false,
            false,
            0,
            0
        ),
        WatchUiState(false, 0),
        UserStyle(emptyMap()).toWireFormat(),
        null
    )

    private class Task(val runTimeMillis: Long, val runnable: Runnable) : Comparable<Task> {
        override fun compareTo(other: Task) = runTimeMillis.compareTo(other.runTimeMillis)
    }

    private fun runPostedTasksFor(durationMillis: Long) {
        looperTimeMillis += durationMillis
        while (pendingTasks.isNotEmpty() &&
            pendingTasks.peek()!!.runTimeMillis <= looperTimeMillis
        ) {
            pendingTasks.remove().runnable.run()
        }
    }

    @Before
    public fun setUp() {
        Mockito.`when`(handler.getLooper()).thenReturn(Looper.myLooper())

        // Capture tasks posted to mHandler and insert in mPendingTasks which is under our control.
        Mockito.doAnswer {
            pendingTasks.add(
                Task(
                    looperTimeMillis,
                    it.arguments[0] as Runnable
                )
            )
        }.`when`(handler).post(ArgumentMatchers.any())

        Mockito.doAnswer {
            pendingTasks.add(
                Task(
                    looperTimeMillis + it.arguments[1] as Long,
                    it.arguments[0] as Runnable
                )
            )
        }.`when`(handler).postDelayed(ArgumentMatchers.any(), ArgumentMatchers.anyLong())

        Mockito.doAnswer {
            // Remove task from the priority queue.  There's no good way of doing this quickly.
            val queue = ArrayDeque<Task>()
            while (pendingTasks.isNotEmpty()) {
                val task = pendingTasks.remove()
                if (task.runnable != it.arguments[0]) {
                    queue.add(task)
                }
            }

            // Push filtered tasks back on the queue.
            while (queue.isNotEmpty()) {
                pendingTasks.add(queue.remove())
            }
        }.`when`(handler).removeCallbacks(ArgumentMatchers.any())
    }

    @Test
    public fun createInteractiveInstanceFailsIfDirectBootWatchFaceCreationIsInProgress() {
        val completableWatchFace = CompletableDeferred<WatchFace>()
        val service = TestAsyncWatchFaceService(
            handler,
            object : TestAsyncWatchFaceService.AsyncWatchFaceFactory() {
                override fun createUserStyleSchema() = UserStyleSchema(emptyList())

                override fun createComplicationsManager(
                    currentUserStyleRepository: CurrentUserStyleRepository
                ) = ComplicationsManager(emptyList(), currentUserStyleRepository)

                override fun createWatchFaceAsync(
                    surfaceHolder: SurfaceHolder,
                    watchState: WatchState,
                    complicationsManager: ComplicationsManager,
                    currentUserStyleRepository: CurrentUserStyleRepository
                ) = completableWatchFace
            },
            MutableWatchState(),
            initParams
        )

        val engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)

        runPostedTasksFor(0)

        lateinit var pendingException: Exception
        engineWrapper.backgroundThreadCoroutineScope.launch {
            try {
                // This should fail because the direct boot instance is being constructed.
                engineWrapper.createInteractiveInstance(initParams, "test")
            } catch (e: Exception) {
                pendingException = e
            }
        }

        runPostedTasksFor(0)

        assertThat(pendingException.message).startsWith("WatchFace already exists!")
    }

    @Test
    public fun directBootAndGetExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance() {
        val completableDirectBootWatchFace = CompletableDeferred<WatchFace>()
        lateinit var pendingCurrentUserStyleRepository: CurrentUserStyleRepository
        lateinit var pendingSurfaceHolder: SurfaceHolder
        lateinit var pendingWatchState: WatchState

        // There shouldn't be an existing instance, so we expect null.
        var pendingInteractiveWatchFaceWcs: IInteractiveWatchFace? = null
        assertNull(
            InteractiveInstanceManager
                .getExistingInstanceOrSetPendingWallpaperInteractiveWatchFaceInstance(
                    InteractiveInstanceManager.PendingWallpaperInteractiveWatchFaceInstance(
                        initParams,
                        object : IPendingInteractiveWatchFace.Stub() {
                            override fun getApiVersion() =
                                IPendingInteractiveWatchFace.API_VERSION

                            override fun onInteractiveWatchFaceCreated(
                                iInteractiveWatchFaceWcs: IInteractiveWatchFace?
                            ) {
                                pendingInteractiveWatchFaceWcs = iInteractiveWatchFaceWcs
                            }

                            override fun onInteractiveWatchFaceCrashed(
                                exception: CrashInfoParcel?
                            ) {
                                fail("WatchFace crashed: $exception")
                            }
                        }
                    )
                )
        )

        val service = TestAsyncWatchFaceService(
            handler,
            object : TestAsyncWatchFaceService.AsyncWatchFaceFactory() {
                override fun createUserStyleSchema() = UserStyleSchema(emptyList())

                override fun createComplicationsManager(
                    currentUserStyleRepository: CurrentUserStyleRepository
                ) = ComplicationsManager(emptyList(), currentUserStyleRepository)

                override fun createWatchFaceAsync(
                    surfaceHolder: SurfaceHolder,
                    watchState: WatchState,
                    complicationsManager: ComplicationsManager,
                    currentUserStyleRepository: CurrentUserStyleRepository
                ): Deferred<WatchFace> {
                    pendingSurfaceHolder = surfaceHolder
                    pendingWatchState = watchState
                    pendingCurrentUserStyleRepository = currentUserStyleRepository
                    return completableDirectBootWatchFace
                }
            },
            MutableWatchState(),
            initParams
        )

        val engineWrapper = service.onCreateEngine() as WatchFaceService.EngineWrapper
        engineWrapper.onSurfaceChanged(surfaceHolder, 0, 100, 100)
        runPostedTasksFor(0)

        // Complete the direct boot watch face which should trigger the callback which sets
        // pendingInteractiveWatchFaceWcs.
        completableDirectBootWatchFace.complete(
            WatchFace(
                WatchFaceType.ANALOG,
                TestRenderer(
                    pendingSurfaceHolder,
                    pendingCurrentUserStyleRepository,
                    pendingWatchState,
                    16L
                )
            )
        )

        runPostedTasksFor(0)

        assertNotNull(pendingInteractiveWatchFaceWcs)
    }
}