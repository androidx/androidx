/* Copyright (C) 2025 The Android Open Source Project
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

package androidx.photopicker.testing

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.RequiresExtension
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Executor

/**
 * A test implementation of [EmbeddedPhotoPickerProvider] which provides a fake of the Embedded
 * PhotoPicker and provides additional methods to allow tests to fake user interactions so that the
 * client side flows can be tested.
 */
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
public class TestEmbeddedPhotoPickerProvider(private val context: Context) :
    EmbeddedPhotoPickerProvider {

    public companion object {
        /**
         * Construct a [EmbeddedPhotoPickerProvider] that runs a fake embedded photopicker view
         * using the InstrumentedTests context for the underlying empty View.
         *
         * @return A test only implementation of [EmbeddedPhotoPickerProvider].
         */
        @JvmStatic
        public fun get(): TestEmbeddedPhotoPickerProvider {
            return TestEmbeddedPhotoPickerProvider(
                InstrumentationRegistry.getInstrumentation().getContext()
            )
        }
    }

    internal val sessionClientMap:
        MutableMap<EmbeddedPhotoPickerSession, EmbeddedPhotoPickerClient> =
        mutableMapOf<EmbeddedPhotoPickerSession, EmbeddedPhotoPickerClient>()

    /** The list of sessions opened with this provider. */
    public val sessions: Set<EmbeddedPhotoPickerSession>
        get() = sessionClientMap.keys

    /**
     * Opens a fake [EmbeddedPhotoPickerSession] that can be used for testing.
     *
     * The session that is opened will render real, but empty Views in the client application, and
     * the [TestEmbeddedPhotoPickerProvider] provides additional method for faking user intections
     * between the client and EmbeddedPhotoPicker service.
     */
    public override fun openSession(
        hostToken: IBinder,
        displayId: Int,
        width: Int,
        height: Int,
        featureInfo: EmbeddedPhotoPickerFeatureInfo,
        clientExecutor: Executor,
        callback: EmbeddedPhotoPickerClient,
    ) {
        checkNotNull(hostToken) { "hostToken must not be null." }
        checkNotNull(featureInfo) { "featureInfo must not be null." }
        checkNotNull(clientExecutor) { "clientExecutor must not be null." }
        checkNotNull(callback) { "clientCallback must not be null." }
        clientExecutor.execute({
            val session =
                TestEmbeddedPhotoPickerSession(
                    context,
                    hostToken,
                    displayId,
                    width,
                    height,
                    featureInfo,
                    callback,
                )
            sessionClientMap.put(session, callback)
            callback.onSessionOpened(session)
        })
    }

    /**
     * Direct the provided [EmbeddedPhotoPickerSession] to throw an error to the client.
     *
     * @param session The EmbeddedPhotoPickerSession, created by calling openSession on this
     *   provider.
     * @param throwable The [Throwable] this session should throw. The [EmbeddedPhotoPickerClient]
     *   will receive this error via the [EmbeddedPhotoPickerClient#onSessionError] callback.
     */
    public fun notifySessionError(session: EmbeddedPhotoPickerSession, throwable: Throwable) {
        assert(sessionClientMap.containsKey(session)) {
            "This session is unknown to this TestEmbeddedPhotoPickerProvider."
        }
        val client = sessionClientMap.getValue(session)
        client.onSessionError(throwable)
    }

    /**
     * Direct the [EmbeddedPhotoPickerSession] to emit the provided list of [Uri] as selected by the
     * user.
     *
     * This will allow test cases to properly test the
     * [EmbeddedPhotoPickerClient#onUriPermissionGranted] callback.
     *
     * @param session The EmbeddedPhotoPickerSession, created by calling openSession on this
     *   provider.
     * @param uris The list of Uris that the session should mark as selected by the user.
     */
    public fun notifySelectedUris(session: EmbeddedPhotoPickerSession, uris: List<Uri>) {
        assert(sessionClientMap.containsKey(session)) {
            "This session is unknown to this TestEmbeddedPhotoPickerProvider."
        }
        val client = sessionClientMap.getValue(session)
        client.onUriPermissionGranted(uris)
    }

    /**
     * Direct the [EmbeddedPhotoPickerSession] to emit the provided list of [Uri] as deselected by
     * the user.
     *
     * This will allow test cases to properly test the
     * [EmbeddedPhotoPickerClient#onUriPermissionRevoked] callback.
     *
     * @param session The EmbeddedPhotoPickerSession, created by calling openSession on this
     *   provider.
     * @param uris The list of Uris that the session should mark as deselected by the user.
     */
    public fun notifyDeselectedUris(session: EmbeddedPhotoPickerSession, uris: List<Uri>) {
        assert(sessionClientMap.containsKey(session)) {
            "This session is unknown to this TestEmbeddedPhotoPickerProvider."
        }
        val client = sessionClientMap.getValue(session)
        client.onUriPermissionRevoked(uris)
    }

    /**
     * Direct the [EmbeddedPhotoPickerSession] to emit the user has completed selection media.
     *
     * This will allow test cases to properly test the
     * [EmbeddedPhotoPickerClient#onSelectionComplete] callback.
     *
     * @param session The EmbeddedPhotoPickerSession, created by calling openSession on this
     *   provider.
     */
    public fun notifySelectionComplete(session: EmbeddedPhotoPickerSession) {
        assert(sessionClientMap.containsKey(session)) {
            "This session is unknown to this TestEmbeddedPhotoPickerProvider."
        }
        val client = sessionClientMap.getValue(session)
        client.onSelectionComplete()
    }
}
