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

package androidx.camera.viewfinder.impl

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.AttachedSurfaceControl
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import androidx.annotation.RequiresApi
import androidx.camera.viewfinder.core.impl.SurfaceControlCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@SuppressLint("NewApi")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class SurfaceControlCompatTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    @Config(sdk = [28])
    fun setContentRotation_returnsFalseBelowApi29() {
        val surfaceView = SurfaceView(context)
        val compat = SurfaceControlCompat.wrap(surfaceView)

        assertThat(compat.setContentRotation(90, 200, 100, syncView = View(context))).isFalse()
    }

    @Test
    @Config(sdk = [29, 30])
    fun setContentRotation_appliesAndClosesImmediately_onApi29To30() =
        withSetContentRotation { applied, tx, _ ->
            assertThat(applied).isTrue()
            assertThat(tx.applyCount).isEqualTo(1)
            assertThat(tx.closeCount).isEqualTo(1)
        }

    @Test
    @Config(sdk = [31, 32, 33])
    fun setContentRotation_doesNotPrematurelyCloseTransaction_whenApplyOnDrawSucceedsOnApi31To33() =
        withSetContentRotation(applyOnDrawResult = true) { applied, tx, invalidateCount ->
            assertThat(applied).isTrue()
            assertThat(invalidateCount).isEqualTo(1)
            assertThat(tx.applyCount).isEqualTo(0)
            // On API 31-33, ViewRootImpl retains the Transaction reference for a RenderThread
            // callback, so close() must not be called before RenderThread consumes it.
            assertThat(tx.closeCount).isEqualTo(0)
        }

    @Test
    @Config(sdk = [34])
    fun setContentRotation_closesTransactionDeterministically_whenApplyOnDrawSucceedsOnApi34Plus() =
        withSetContentRotation(applyOnDrawResult = true) { applied, tx, invalidateCount ->
            assertThat(applied).isTrue()
            assertThat(invalidateCount).isEqualTo(1)
            assertThat(tx.applyCount).isEqualTo(0)
            // On API 34+, ViewRootImpl synchronously merges into mPendingTransaction, so the
            // caller's Transaction is closed right away.
            assertThat(tx.closeCount).isEqualTo(1)
        }

    @Test
    @Config(sdk = [31, 34])
    fun setContentRotation_appliesAndClosesTransaction_whenApplyOnDrawReturnsFalse() =
        withSetContentRotation(applyOnDrawResult = false) { applied, tx, invalidateCount ->
            assertThat(applied).isTrue()
            assertThat(invalidateCount).isEqualTo(0)
            assertThat(tx.applyCount).isEqualTo(1)
            assertThat(tx.closeCount).isEqualTo(1)
        }

    /**
     * Calls [SurfaceControlCompat.setContentRotation] on a test layer and passes its result, the
     * [RecordingTransaction] it used, and the number of invalidations of the sync view to [block].
     *
     * @param applyOnDrawResult The result of `applyTransactionOnDraw()` for the sync view, or
     *   `null` to use a plain [View] without a root surface control.
     */
    private fun withSetContentRotation(
        applyOnDrawResult: Boolean? = null,
        block: (applied: Boolean, tx: RecordingTransaction, invalidateCount: Int) -> Unit,
    ) {
        val rawControl = SurfaceControl.Builder().setName("test").setBufferSize(200, 100).build()
        var capturedTransaction: RecordingTransaction? = null
        try {
            val compat =
                SurfaceControlCompat.wrap(surfaceViewWithControl(rawControl)) {
                    RecordingTransaction().also { capturedTransaction = it }
                }
            val syncView =
                if (applyOnDrawResult != null) {
                    SyncTrackingView(context, applyOnDrawResult)
                } else {
                    View(context)
                }
            val applied = compat.setContentRotation(90, 200, 100, syncView)
            if (syncView is SyncTrackingView) {
                // The Transaction must not be closed before it is handed to applyTransactionOnDraw.
                assertThat(syncView.closeCountWhenApplied).isEqualTo(0)
            }
            block(
                applied,
                checkNotNull(capturedTransaction),
                (syncView as? SyncTrackingView)?.invalidateCount ?: 0,
            )
        } finally {
            capturedTransaction?.takeIf { it.closeCount == 0 }?.close()
            rawControl.release()
        }
    }

    private fun surfaceViewWithControl(control: SurfaceControl): SurfaceView =
        object : SurfaceView(context) {
            override fun getSurfaceControl(): SurfaceControl = control
        }

    private class SyncTrackingView(context: Context, private val applyOnDrawResult: Boolean) :
        View(context) {
        var invalidateCount = 0
        var closeCountWhenApplied: Int? = null

        override fun invalidate() {
            super.invalidate()
            invalidateCount++
        }

        override fun getRootSurfaceControl(): AttachedSurfaceControl =
            object : AttachedSurfaceControl {
                override fun buildReparentTransaction(
                    child: SurfaceControl
                ): SurfaceControl.Transaction? = null

                override fun applyTransactionOnDraw(t: SurfaceControl.Transaction): Boolean {
                    closeCountWhenApplied = (t as RecordingTransaction).closeCount
                    return applyOnDrawResult
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private class RecordingTransaction : SurfaceControl.Transaction() {
        var applyCount = 0
            private set

        var closeCount = 0
            private set

        override fun apply() {
            applyCount++
            super.apply()
        }

        override fun close() {
            closeCount++
            super.close()
        }
    }
}
