/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.view

import android.app.Activity
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.SurfaceRequest
import androidx.camera.testing.fakes.FakeCamera
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument
import org.robolectric.shadows.ShadowSurfaceView

/** Robolectric unit tests for [SurfaceViewImplementation]. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(
    sdk = [Config.TARGET_SDK],
    shadows = [SurfaceViewImplementationRobolectricTest.ShadowSurfaceViewWithSurface::class],
)
class SurfaceViewImplementationRobolectricTest {

    companion object {
        private val DEFAULT_SIZE = Size(640, 480)
        private val DIFFERENT_SIZE = Size(720, 480)
        private val allocatedSurfaceTextures = mutableListOf<SurfaceTexture>()
        private val allocatedSurfaces = mutableListOf<Surface>()
    }

    private lateinit var activityController: ActivityController<Activity>
    private lateinit var parent: FrameLayout
    private lateinit var previewTransformation: PreviewTransformation
    private lateinit var implementation: SurfaceViewImplementation
    private val surfaceRequests = mutableListOf<SurfaceRequest>()

    @Before
    fun setUp() {
        activityController = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = activityController.get()
        parent = FrameLayout(activity)
        // Simulate Android's synchronous dispatchDetachedFromWindow() -> surfaceDestroyed() when
        // initializePreview() calls mParent.removeAllViews() on a still-attached SurfaceView.
        parent.setOnHierarchyChangeListener(
            object : ViewGroup.OnHierarchyChangeListener {
                override fun onChildViewAdded(parent: View, child: View) {}

                override fun onChildViewRemoved(parent: View, child: View) {
                    if (child is SurfaceView) {
                        for (cb in callbacksFor(child)) {
                            cb.surfaceDestroyed(child.holder)
                        }
                    }
                }
            }
        )
        activity.setContentView(parent)
        previewTransformation = PreviewTransformation()
        implementation = SurfaceViewImplementation(parent, previewTransformation)
    }

    @After
    fun tearDown() {
        for (request in surfaceRequests) {
            request.willNotProvideSurface()
            request.deferrableSurface.close()
        }
        surfaceRequests.clear()
        if (::activityController.isInitialized) {
            activityController.pause().stop().destroy()
        }
        for (surface in allocatedSurfaces) {
            surface.release()
        }
        allocatedSurfaces.clear()
        for (surfaceTexture in allocatedSurfaceTextures) {
            surfaceTexture.release()
        }
        allocatedSurfaceTextures.clear()
    }

    @Test
    @Config(sdk = [28])
    fun reuseSurfaceView_afterSurfaceDestroyed_onApi28() {
        assertSurfaceViewReuseAfterDestroy(expectRecreated = false)
    }

    @Test
    @Config(sdk = [29, 30, 31])
    fun recreateSurfaceView_afterSurfaceDestroyed_onApi29To31() {
        assertSurfaceViewReuseAfterDestroy(expectRecreated = true)
    }

    @Test
    @Config(sdk = [32, Config.TARGET_SDK])
    fun reuseSurfaceView_afterSurfaceDestroyed_onApi32Plus() {
        assertSurfaceViewReuseAfterDestroy(expectRecreated = false)
    }

    @Test
    @Config(sdk = [31])
    fun pendingRequestDestroyedBeforeProvideSurface_recreatesAndDoesNotLoopOnApi31() {
        var r1Invalidations = 0
        var r2Invalidations = 0
        lateinit var r2: SurfaceRequest
        val r1 =
            createSurfaceRequest(DEFAULT_SIZE) {
                r1Invalidations++
                r2 = createSurfaceRequest(DEFAULT_SIZE) { r2Invalidations++ }
                implementation.onSurfaceRequested(r2, null)
            }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        assertThat(callbacksFor(sv1)).hasSize(1)

        // Detach before surfaceChanged provides a Surface, then reattach within the same frame.
        driveSurfaceDestroyed(sv1)
        driveSurfaceCreated(sv1)
        shadowOf(getMainLooper()).idle()

        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        assertThat(callbacksFor(sv1)).isEmpty()
        assertThat(callbacksFor(sv2)).hasSize(1)

        driveSurfaceReady(sv2, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(r1Invalidations).isEqualTo(1)
        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun resolutionChange_afterSurfaceDestroyedAndCreated_doesNotSpuriouslyInvalidateNewRequest() {
        var r1Invalidations = 0
        val r1 = createSurfaceRequest(DEFAULT_SIZE) { r1Invalidations++ }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        assertThat(r1.deferrableSurface.surface.isDone).isTrue()

        // Previous destroy/create leaves mSurfaceRequest == null before r2 arrives at a new size.
        driveSurfaceDestroyed(sv1)
        driveSurfaceCreated(sv1)
        assertThat(r1Invalidations).isEqualTo(1)

        var r2Invalidations = 0
        val r2 = createSurfaceRequest(DIFFERENT_SIZE) { r2Invalidations++ }
        implementation.onSurfaceRequested(r2, null)
        shadowOf(getMainLooper()).idle()

        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        assertThat(callbacksFor(sv1)).isEmpty()
        assertThat(callbacksFor(sv2)).hasSize(1)

        driveSurfaceReady(sv2, DIFFERENT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun snapshottedSurfaceChangedFromAbandonedSurfaceView_isIgnoredOnApi31() {
        var r1Invalidations = 0
        var r2Invalidations = 0
        lateinit var r2: SurfaceRequest
        val r1 =
            createSurfaceRequest(DEFAULT_SIZE) {
                r1Invalidations++
                r2 = createSurfaceRequest(DEFAULT_SIZE) { r2Invalidations++ }
                implementation.onSurfaceRequested(r2, null)
            }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        assertThat(r1.deferrableSurface.surface.isDone).isTrue()

        driveSurfaceDestroyed(sv1)
        // Emulate SurfaceView.updateSurface() snapshotting callbacks[] before surfaceCreated().
        val snapshottedCallbacks = callbacksFor(sv1)
        for (cb in snapshottedCallbacks) {
            cb.surfaceCreated(sv1.holder)
        }
        // r1.invalidate() ran synchronously inside surfaceCreated and replaced sv1 with sv2.
        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        assertThat(callbacksFor(sv1)).isEmpty()

        // The rest of SV1's snapshotted loop delivers surfaceChanged(sv1.holder) after
        // removeCallback.
        for (cb in snapshottedCallbacks) {
            cb.surfaceChanged(
                sv1.holder,
                PixelFormat.RGBA_8888,
                DEFAULT_SIZE.width,
                DEFAULT_SIZE.height,
            )
        }
        shadowOf(getMainLooper()).idle()

        // Stale SV1 callback must be ignored so r2 is not completed with SV1's size before SV2
        // attaches.
        assertThat(r2.deferrableSurface.surface.isDone).isFalse()

        driveSurfaceReady(sv2, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(r1Invalidations).isEqualTo(1)
        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun recreateSurfaceView_resetsSurfaceProvidedGuardBeforeNewSurfaceReadyOnApi31() {
        parent.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
        )
        parent.layout(0, 0, 1000, 1000)

        lateinit var r2: SurfaceRequest
        val r1 =
            createSurfaceRequest(DEFAULT_SIZE) {
                r2 = createSurfaceRequest(DEFAULT_SIZE)
                previewTransformation.setTransformationInfo(
                    checkNotNull(r2.transformationInfo),
                    r2.resolution,
                    /* useFrontCameraRotation= */ false,
                )
                implementation.onSurfaceRequested(r2, null)
            }
        previewTransformation.setTransformationInfo(
            checkNotNull(r1.transformationInfo),
            r1.resolution,
            /* useFrontCameraRotation= */ false,
        )
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()
        assertThat(sv1.scaleX != 1f || sv1.scaleY != 1f).isTrue()

        driveSurfaceDestroyed(sv1)
        driveSurfaceCreated(sv1)
        shadowOf(getMainLooper()).idle()

        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        // Before sv2 provides its surface, redrawPreview() must stay gated (b/183864890).
        implementation.redrawPreview()
        assertThat(sv2.scaleX).isEqualTo(1f)
        assertThat(sv2.scaleY).isEqualTo(1f)

        driveSurfaceReady(sv2, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()
        assertThat(sv2.scaleX != 1f || sv2.scaleY != 1f).isTrue()
    }

    @Test
    fun resolutionChange_whileSurfaceActive_closesPreviousDeferrableSurface() {
        val r1 = createSurfaceRequest(DEFAULT_SIZE)
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        assertThat(r1.deferrableSurface.surface.isDone).isTrue()
        assertThat(r1.deferrableSurface.isClosed).isFalse()

        var r2Invalidations = 0
        val r2 = createSurfaceRequest(DIFFERENT_SIZE) { r2Invalidations++ }
        implementation.onSurfaceRequested(r2, null)
        shadowOf(getMainLooper()).idle()

        // removeCallback() suppresses sv1's surfaceDestroyed(), so resetForNewSurfaceView() must
        // close r1's DeferrableSurface directly without invalidating r2.
        assertThat(r1.deferrableSurface.isClosed).isTrue()
        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        driveSurfaceReady(sv2, DIFFERENT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun newRequestAfterSurfaceDestroyedBeforeSurfaceCreated_invalidatesOnceOnApi31() {
        var r1Invalidations = 0
        val r1 = createSurfaceRequest(DEFAULT_SIZE) { r1Invalidations++ }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)

        // Detach destroys sv1's surface, and r2 arrives before sv1 reattaches.
        driveSurfaceDestroyed(sv1)
        var r2Invalidations = 0
        var r3Invalidations = 0
        lateinit var r3: SurfaceRequest
        val r2 =
            createSurfaceRequest(DEFAULT_SIZE) {
                r2Invalidations++
                r3 = createSurfaceRequest(DEFAULT_SIZE) { r3Invalidations++ }
                implementation.onSurfaceRequested(r3, null)
            }
        implementation.onSurfaceRequested(r2, null)
        shadowOf(getMainLooper()).idle()

        // r1 was cleared by resetForNewSurfaceView(); mNeedToInvalidate remains active for
        // genuine pre-surfaceCreated requests (b/242023282), invalidating r2 once and reusing sv2.
        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        assertThat(r1Invalidations).isEqualTo(0)
        assertThat(r2Invalidations).isEqualTo(1)

        driveSurfaceReady(sv2, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(implementation.mSurfaceView).isSameInstanceAs(sv2)
        assertThat(r3Invalidations).isEqualTo(0)
        assertThat(r3.deferrableSurface.surface.isDone).isTrue()
    }

    @Test
    @Config(sdk = [31])
    fun postedReplacementRequest_afterSurfaceDestroyedAndCreated_completesOnNewSurfaceViewOnApi31() {
        var r1Invalidations = 0
        var r2Invalidations = 0
        lateinit var r2: SurfaceRequest
        val handler = Handler(getMainLooper())
        val r1 =
            createSurfaceRequest(DEFAULT_SIZE, hasCameraTransform = false) {
                r1Invalidations++
                handler.post {
                    r2 =
                        createSurfaceRequest(DEFAULT_SIZE, hasCameraTransform = false) {
                            r2Invalidations++
                        }
                    implementation.onSurfaceRequested(r2, null)
                }
            }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        assertThat(r1.deferrableSurface.surface.isDone).isTrue()

        // Same-frame detach/reattach posts r2 via the main looper so sv1 finishes surfaceChanged
        // before onSurfaceRequested(r2) runs.
        driveSurfaceDestroyed(sv1)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()

        val sv2 = checkNotNull(implementation.mSurfaceView)
        assertThat(sv2).isNotSameInstanceAs(sv1)
        assertThat(callbacksFor(sv1)).isEmpty()
        assertThat(callbacksFor(sv2)).hasSize(1)

        driveSurfaceReady(sv2, DEFAULT_SIZE)
        shadowOf(getMainLooper()).idle()

        assertThat(r1Invalidations).isEqualTo(1)
        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    private fun assertSurfaceViewReuseAfterDestroy(expectRecreated: Boolean) {
        var r1Invalidations = 0
        var r2Invalidations = 0
        lateinit var r2: SurfaceRequest
        val r1 =
            createSurfaceRequest(DEFAULT_SIZE) {
                r1Invalidations++
                r2 = createSurfaceRequest(DEFAULT_SIZE) { r2Invalidations++ }
                implementation.onSurfaceRequested(r2, null)
            }
        implementation.onSurfaceRequested(r1, null)
        shadowOf(getMainLooper()).idle()

        val sv1 = checkNotNull(implementation.mSurfaceView)
        driveSurfaceReady(sv1, DEFAULT_SIZE)
        assertThat(r1.deferrableSurface.surface.isDone).isTrue()

        // Same-frame detach -> reattach: surfaceDestroyed followed by surfaceCreated -> invalidate.
        driveSurfaceDestroyed(sv1)
        driveSurfaceCreated(sv1)
        shadowOf(getMainLooper()).idle()

        val sv2 = checkNotNull(implementation.mSurfaceView)
        if (expectRecreated) {
            assertThat(sv2).isNotSameInstanceAs(sv1)
            assertThat(callbacksFor(sv1)).isEmpty()
            assertThat(callbacksFor(sv2)).hasSize(1)
            driveSurfaceReady(sv2, DEFAULT_SIZE)
        } else {
            assertThat(sv2).isSameInstanceAs(sv1)
            assertThat(callbacksFor(sv1)).hasSize(1)
            driveSurfaceChanged(sv1, DEFAULT_SIZE)
        }
        shadowOf(getMainLooper()).idle()

        assertThat(r1Invalidations).isEqualTo(1)
        assertThat(r2Invalidations).isEqualTo(0)
        assertThat(r2.deferrableSurface.surface.isDone).isTrue()
    }

    private fun createSurfaceRequest(
        size: Size,
        hasCameraTransform: Boolean = true,
        onInvalidated: () -> Unit = {},
    ): SurfaceRequest =
        SurfaceRequest(size, FakeCamera(), onInvalidated).apply {
            surfaceRequests.add(this)
            updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                    Rect(0, 0, size.width, size.height),
                    0,
                    Surface.ROTATION_0,
                    hasCameraTransform,
                    Matrix(),
                    /* mirroring= */ false,
                )
            )
        }

    private fun callbacksFor(sv: SurfaceView): List<SurfaceHolder.Callback> =
        (sv.holder as ShadowSurfaceView.FakeSurfaceHolder).callbacks.toList()

    private fun driveSurfaceReady(sv: SurfaceView, size: Size) {
        driveSurfaceCreated(sv)
        driveSurfaceChanged(sv, size)
    }

    private fun driveSurfaceCreated(sv: SurfaceView) {
        for (cb in callbacksFor(sv)) {
            cb.surfaceCreated(sv.holder)
        }
    }

    private fun driveSurfaceChanged(sv: SurfaceView, size: Size) {
        for (cb in callbacksFor(sv)) {
            cb.surfaceChanged(sv.holder, PixelFormat.RGBA_8888, size.width, size.height)
        }
    }

    private fun driveSurfaceDestroyed(sv: SurfaceView) {
        for (cb in callbacksFor(sv)) {
            cb.surfaceDestroyed(sv.holder)
        }
    }

    @Implements(SurfaceView::class)
    class ShadowSurfaceViewWithSurface : ShadowSurfaceView() {
        private val validHolder =
            object : FakeSurfaceHolder() {
                private val validSurface: Surface by lazy {
                    val surfaceTexture = SurfaceTexture(0).also { allocatedSurfaceTextures.add(it) }
                    Surface(surfaceTexture).also { allocatedSurfaces.add(it) }
                }

                override fun getSurface(): Surface = validSurface
            }

        @Implementation public override fun getHolder(): SurfaceHolder = validHolder
    }
}
