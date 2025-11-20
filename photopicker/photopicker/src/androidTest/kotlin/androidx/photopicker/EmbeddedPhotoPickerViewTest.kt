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

package androidx.photopicker

import android.net.Uri
import android.os.Build
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.RequiresExtension
import androidx.photopicker.test.R
import androidx.photopicker.testing.TestEmbeddedPhotoPickerProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CompletableFuture
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
class EmbeddedPhotoPickerViewTest {

    @get:Rule
    var activityRule: ActivityScenarioRule<EmbeddedPhotoPickerViewTestActivity> =
        ActivityScenarioRule(EmbeddedPhotoPickerViewTestActivity::class.java)

    @Test
    @ExperimentalPhotoPickerApi
    fun testEmbeddedPhotoPickerViewOpensSession() {
        val activity = activityRule.withActivity { this }

        val embeddedView = activity.findViewById<EmbeddedPhotoPickerView>(R.id.photopicker_test)
        val session: CompletableFuture<EmbeddedPhotoPickerSession> = CompletableFuture()

        val testProvider =
            TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )

        embeddedView.addEmbeddedPhotoPickerStateChangeListener(
            object : EmbeddedPhotoPickerView.EmbeddedPhotoPickerStateChangeListener {

                override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {
                    session.complete(newSession)
                }

                override fun onSessionError(throwable: Throwable) {}

                override fun onUriPermissionGranted(uris: List<Uri>) {}

                override fun onUriPermissionRevoked(uris: List<Uri>) {}

                override fun onSelectionComplete() {}
            }
        )

        embeddedView.setProvider(testProvider)
        embeddedView.setEmbeddedPhotoPickerFeatureInfo(
            EmbeddedPhotoPickerFeatureInfo.Builder().build()
        )

        assertThat(session.get()).isNotNull()
    }

    @Test
    @ExperimentalPhotoPickerApi
    fun testEmbeddedPhotoPickerViewThrowsSessionError() {
        val activity = activityRule.withActivity { this }

        val embeddedView = activity.findViewById<EmbeddedPhotoPickerView>(R.id.photopicker_test)
        val session: CompletableFuture<EmbeddedPhotoPickerSession> = CompletableFuture()
        val error: CompletableFuture<Throwable> = CompletableFuture()

        val throwable = RuntimeException("Test")

        val testProvider =
            TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )

        embeddedView.addEmbeddedPhotoPickerStateChangeListener(
            object : EmbeddedPhotoPickerView.EmbeddedPhotoPickerStateChangeListener {

                override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {
                    session.complete(newSession)
                }

                override fun onSessionError(throwable: Throwable) {
                    error.complete(throwable)
                }

                override fun onUriPermissionGranted(uris: List<Uri>) {}

                override fun onUriPermissionRevoked(uris: List<Uri>) {}

                override fun onSelectionComplete() {}
            }
        )

        embeddedView.setProvider(testProvider)
        embeddedView.setEmbeddedPhotoPickerFeatureInfo(
            EmbeddedPhotoPickerFeatureInfo.Builder().build()
        )

        testProvider.notifySessionError(session.get(), throwable)
        assertThat(error.get()).isEqualTo(throwable)
    }

    @Test
    @ExperimentalPhotoPickerApi
    fun testEmbeddedPhotoPickerViewEmitsSelections() {
        val activity = activityRule.withActivity { this }

        val embeddedView = activity.findViewById<EmbeddedPhotoPickerView>(R.id.photopicker_test)
        val session: CompletableFuture<EmbeddedPhotoPickerSession> = CompletableFuture()

        val grantedUris = mutableListOf<Uri>()

        val testProvider =
            TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )

        embeddedView.addEmbeddedPhotoPickerStateChangeListener(
            object : EmbeddedPhotoPickerView.EmbeddedPhotoPickerStateChangeListener {

                override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {
                    session.complete(newSession)
                }

                override fun onSessionError(throwable: Throwable) {}

                override fun onUriPermissionGranted(uris: List<Uri>) {
                    grantedUris.addAll(uris)
                }

                override fun onUriPermissionRevoked(uris: List<Uri>) {}

                override fun onSelectionComplete() {}
            }
        )

        embeddedView.setProvider(testProvider)
        embeddedView.setEmbeddedPhotoPickerFeatureInfo(
            EmbeddedPhotoPickerFeatureInfo.Builder().build()
        )

        assertThat(grantedUris).isEmpty()

        val uri_1 = Uri.fromParts("content", "1234", null)
        val uri_2 = Uri.fromParts("content", "4567", null)
        val uri_3 = Uri.fromParts("content", "8900", null)
        val uri_4 = Uri.fromParts("content", "9999", null)

        testProvider.notifySelectedUris(session.get(), listOf(uri_1, uri_2))
        assertThat(grantedUris).containsExactly(uri_1, uri_2)

        testProvider.notifySelectedUris(session.get(), listOf(uri_3, uri_4))
        assertThat(grantedUris).containsExactly(uri_1, uri_2, uri_3, uri_4)
    }

    @Test
    @ExperimentalPhotoPickerApi
    fun testEmbeddedPhotoPickerViewEmitsDeSelections() {
        val activity = activityRule.withActivity { this }

        val embeddedView = activity.findViewById<EmbeddedPhotoPickerView>(R.id.photopicker_test)
        val session: CompletableFuture<EmbeddedPhotoPickerSession> = CompletableFuture()

        val deselectedUris = mutableListOf<Uri>()

        val testProvider =
            TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )

        embeddedView.addEmbeddedPhotoPickerStateChangeListener(
            object : EmbeddedPhotoPickerView.EmbeddedPhotoPickerStateChangeListener {

                override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {
                    session.complete(newSession)
                }

                override fun onSessionError(throwable: Throwable) {}

                override fun onUriPermissionGranted(uris: List<Uri>) {}

                override fun onUriPermissionRevoked(uris: List<Uri>) {
                    deselectedUris.addAll(uris)
                }

                override fun onSelectionComplete() {}
            }
        )

        embeddedView.setProvider(testProvider)
        embeddedView.setEmbeddedPhotoPickerFeatureInfo(
            EmbeddedPhotoPickerFeatureInfo.Builder().build()
        )

        assertThat(deselectedUris).isEmpty()

        val uri_1 = Uri.fromParts("content", "1234", null)

        testProvider.notifyDeselectedUris(session.get(), listOf(uri_1))
        assertThat(deselectedUris).containsExactly(uri_1)
    }

    @Test
    @ExperimentalPhotoPickerApi
    fun testEmbeddedPhotoPickerViewEmitsSelectionComplete() {
        val activity = activityRule.withActivity { this }

        val embeddedView = activity.findViewById<EmbeddedPhotoPickerView>(R.id.photopicker_test)
        val session: CompletableFuture<EmbeddedPhotoPickerSession> = CompletableFuture()
        val selectionComplete: CompletableFuture<Boolean> = CompletableFuture()

        val testProvider =
            TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().targetContext
            )

        embeddedView.addEmbeddedPhotoPickerStateChangeListener(
            object : EmbeddedPhotoPickerView.EmbeddedPhotoPickerStateChangeListener {

                override fun onSessionOpened(newSession: EmbeddedPhotoPickerSession) {
                    session.complete(newSession)
                }

                override fun onSessionError(throwable: Throwable) {}

                override fun onUriPermissionGranted(uris: List<Uri>) {}

                override fun onUriPermissionRevoked(uris: List<Uri>) {}

                override fun onSelectionComplete() {
                    selectionComplete.complete(true)
                }
            }
        )

        embeddedView.setProvider(testProvider)
        embeddedView.setEmbeddedPhotoPickerFeatureInfo(
            EmbeddedPhotoPickerFeatureInfo.Builder().build()
        )

        testProvider.notifySelectionComplete(session.get())
        assertThat(selectionComplete.get()).isTrue()
    }
}
