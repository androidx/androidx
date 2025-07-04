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

package androidx.photopicker.compose

import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.widget.photopicker.EmbeddedPhotoPickerClient
import android.widget.photopicker.EmbeddedPhotoPickerFeatureInfo
import android.widget.photopicker.EmbeddedPhotoPickerProvider
import android.widget.photopicker.EmbeddedPhotoPickerSession
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitCancellation

/**
 * State model of a EmbeddedPhotoPickerSession. This interface models the state and session
 * management lifecycle of the Embedded Photopicker between the client application and the remote
 * service.
 *
 * A default implementation is provided via the [rememberEmbeddedPhotoPickerState] composable which
 * can be used to obtain an implementation that should work for most use cases. This object can be
 * passed to the [EmbeddedPhotopicker] composable which provides the display layer for the Embedded
 * PhotoPicker.
 *
 * Callbacks to the underlying session are exposed here to push Compose state into the remote view.
 */
@ExperimentalPhotoPickerComposeApi
public interface EmbeddedPhotoPickerState {

    /** The current [SurfaceView#hostToken] */
    public var surfaceHostToken: IBinder?

    /** The current [Context#displayId] */
    public var displayId: Int

    /**
     * The current ready state of this state object. Signifies whether this state object is ready to
     * run a Embedded Photopicker session.
     */
    public var isReady: Boolean

    /**
     * The current expanded state of this state object. Signifies whether the underlying
     * EmbeddedPhotopicker session is expanded or collapsed.
     */
    public var isExpanded: Boolean

    /**
     * A set of currently selected media items that have been selected by the user during the
     * current Photopicker session.
     */
    public val selectedMedia: Set<Uri>

    /**
     * Receiver for when the Photopicker Session encounters a terminal error.
     *
     * @see EmbeddedPhotoPickerClient#onSessionError
     */
    public fun onSessionError(throwable: Throwable)

    /**
     * Receiver for when URI permission has been granted to item(s) selected by the user.
     *
     * @see EmbeddedPhotoPickerClient#onUriPermissionGranted
     */
    public fun onUriPermissionGranted(uris: List<Uri>)

    /**
     * Receiver for when URI permission has been revoked of an item deselected by the user.
     *
     * @see EmbeddedPhotoPickerClient#onUriPermissionRevoked
     */
    public fun onUriPermissionRevoked(uris: List<Uri>)

    /**
     * Receiver for when the user is done with their selection and the picker should be collapsed.
     *
     * @see EmbeddedPhotoPickerClient#onSelectionComplete
     */
    public fun onSelectionComplete()

    /** Sets the isExpanded state of the current session. */
    public fun setCurrentExpanded(expanded: Boolean)

    /**
     * Notify the underlying [EmbeddedPhotoPickerSession] that the current [Configuration] has
     * changed.
     */
    public fun notifyConfigurationChanged(config: Configuration)

    /**
     * Notify the underlying [EmbeddedPhotoPickerSession] that its parent view has been resized.
     *
     * @param size The new size of the parent view.
     */
    public fun notifyResized(size: IntSize)

    /**
     * Request the EmbeddedPhotoPickerSession deselect the media item with the provided Uri.
     *
     * NOTE: The Uri should have been received from the PhotoPicker. Regular [MediaStore] uris, or
     * other Uris passed here will be ignored by the Photopicker.
     *
     * @param uri the [Uri] that should be deselected in the PhotoPicker's interface.
     */
    public suspend fun deselectUri(uri: Uri): Unit = deselectUris(listOf(uri))

    /**
     * Request the EmbeddedPhotoPickerSession deselect media items with the provided list of Uris
     * from its interface.
     *
     * NOTE: The Uri should have been received from the PhotoPicker. Regular [MediaStore] uris, or
     * other Uris passed here will be ignored by the PhotoPicker.
     *
     * @param uris the [List] of [Uri] that should be deselected in the PhotoPicker's interface.
     */
    public suspend fun deselectUris(uris: List<Uri>): Unit

    /**
     * Run a [EmbeddedPhotoPickerSession] using the state provided by this state object.
     *
     * This suspended function should be started from a [LaunchedEffect] in the composable hosting
     * the SurfaceView the [EmbeddedPhotoPicker] is drawing to. The function should perform the
     * necessary startup logic with the EmbeddedPhotoPickerProvider, and then run the provided
     * client in a child scope of the current CoroutineScope. Throughout the entire session, and
     * then close / cleanup any resources being used when the suspended function is cancelled.
     *
     * @param provider The provider that should be used for this session.
     * @param featureInfo The client provided EmbeddedPhotoPickerFeatureInfo to configure the
     *   Embedded PhotoPicker.
     * @param onReceiveSession A callback from the display layer, which can be used to pass the
     *   opened Session back to the display layer so it can be attached to the SurfaceView.
     */
    @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
    public suspend fun runSession(
        provider: EmbeddedPhotoPickerProvider,
        featureInfo: EmbeddedPhotoPickerFeatureInfo,
        onReceiveSession: (EmbeddedPhotoPickerSession) -> Unit,
    )
}

/**
 * Abstract base class for managing the state of an embedded photo picker.
 *
 * This class provides common functionalities for handling the picker's lifecycle, UI state (such as
 * expansion and readiness), selected media, and communication with the underlying
 * [EmbeddedPhotoPickerProvider]. It is designed to be subclassed to provide specific
 * implementations for event handling.
 *
 * In order to make the state object ready and before calling [runSession], the composable that
 * receives this state object should set the [surfaceHostToken] once the SurfaceView has attached to
 * the window. Additionally, [notifyResized] should also be called to provide the initial size of
 * the SurfaceView. (This should also be called any time the SurfaceView changes size, see
 * [Modifier.onSizeChanged] as a modifier that can listen for size changes.)
 *
 * Key responsibilities include:
 * - Managing the [isExpanded] and [isReady] states.
 * - Holding references to the [surfaceHostToken], [displayId], and current [surfaceSize].
 * - Tracking [selectedMedia] URIs.
 * - Orchestrating the photo picker session via the [runSession] suspend function.
 * - Providing hooks for subclasses to react to session events ([onSessionError]), URI permission
 *   changes ([onUriPermissionGranted], [onUriPermissionRevoked]), and selection completion
 *   ([onSelectionComplete]).
 *
 * This API is experimental, as indicated by [ExperimentalPhotoPickerComposeApi], and requires
 * Android U (API level 34, version 15 of the extension), as indicated by [RequiresExtension].
 *
 * @param isExpanded Initial expanded state of the photo picker. Defaults to `false`.
 * @param media Initial set of selected media URIs. Defaults to an empty set. This set should only
 *   include URIs that have been received from the Photopicker. Do not pass general MediaStore URIs
 *   here, they will be ignored.
 * @property isReady Indicates whether the photo picker state is ready to start a session. This
 *   becomes `true` once surface information (host token, display ID, and size) is available.
 * @property isExpanded Current expanded state of the photo picker. Can be modified using
 *   [setCurrentExpanded].
 * @property surfaceHostToken The host token for the `SurfaceControlViewHost` where the picker UI
 *   will be rendered. Set externally before [runSession] is called.
 * @property displayId The ID of the display where the photo picker is shown. Set externally before
 *   [runSession] is called.
 * @property selectedMedia A read-only set of URIs representing the currently selected media items.
 *   Updated internally based on granted/revoked permissions.
 */
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
@ExperimentalPhotoPickerComposeApi
public abstract class AbstractEmbeddedPhotoPickerState(
    isExpanded: Boolean = false,
    media: Set<Uri> = emptySet<Uri>(),
) : EmbeddedPhotoPickerState {

    private val openSession: AtomicReference<EmbeddedPhotoPickerSession?> = AtomicReference(null)
    internal var surfaceSize: IntSize = IntSize.Zero

    public final override var isReady: Boolean by mutableStateOf<Boolean>(false)
    public final override var isExpanded: Boolean by mutableStateOf<Boolean>(isExpanded)

    public final override var surfaceHostToken: IBinder? by mutableStateOf(null)
    public final override var displayId: Int by mutableIntStateOf(-1)

    private val _selectedMedia: MutableSet<Uri>
    public final override val selectedMedia: Set<Uri>
        get() = _selectedMedia.toSet()

    init {
        _selectedMedia = mutableSetOf(*media.toTypedArray())
    }

    final override fun notifyConfigurationChanged(config: Configuration) {
        openSession.get()?.notifyConfigurationChanged(config)
    }

    public abstract override fun onSessionError(throwable: Throwable)

    public abstract override fun onUriPermissionGranted(uris: List<Uri>)

    public abstract override fun onUriPermissionRevoked(uris: List<Uri>)

    public abstract override fun onSelectionComplete()

    public final override fun setCurrentExpanded(expanded: Boolean) {
        isExpanded = expanded
        openSession.get()?.notifyPhotoPickerExpanded(expanded)
    }

    public final override fun notifyResized(size: IntSize) {
        surfaceSize = size
        openSession.get()?.notifyResized(size.width, size.height)

        // If not currently ready, checkIfReady as size is one of the conditions
        // needed to be ready.
        if (!isReady) {
            checkIfReady()
        }
    }

    public final override suspend fun deselectUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            openSession.get()?.requestRevokeUriPermission(uris)
                // Throw if this is called when openSession is null (a session has not been received
                // yet)
                ?: throw IllegalStateException(
                    "Cannot request deselect: there is no running session, ensure runSession has " +
                        "been called first."
                )
        }
    }

    public final override suspend fun runSession(
        provider: EmbeddedPhotoPickerProvider,
        featureInfo: EmbeddedPhotoPickerFeatureInfo,
        onReceiveSession: (EmbeddedPhotoPickerSession) -> Unit,
    ) {
        assert(isReady) {
            "This state object is not currently ready to runSession. " +
                "Ensure initialization is complete before calling runSession."
        }
        val deferredSession =
            CompletableDeferred<EmbeddedPhotoPickerSession>(parent = coroutineContext[Job])

        val innerClientCallback: EmbeddedPhotoPickerClient =
            createEmbeddedPhotoPickerClient(
                onSessionOpened = {
                    // Hoist the open session up to the main composable so that it can
                    // be attached to the surface view.
                    onReceiveSession(it)
                    // Pass the session up to the state object for callback purposes.
                    openSession.set(it)
                    // And finally, pass it to this suspend fun which will use it to run
                    // the client.
                    deferredSession.complete(it)
                },
                onSessionError = ::onSessionError,
                onUriPermissionGranted = {
                    _selectedMedia.addAll(it)
                    onUriPermissionGranted(it)
                },
                onUriPermissionRevoked = {
                    _selectedMedia.removeAll(it)
                    onUriPermissionRevoked(it)
                },
                onSelectionComplete = ::onSelectionComplete,
            )

        // Modify the incoming featureInfo object to capture any state held by this object so that
        // sessions that are created with this state object, reflect the state held here.
        val featureInfoWithLocalState: EmbeddedPhotoPickerFeatureInfo =
            EmbeddedPhotoPickerFeatureInfo.Builder()
                .apply {
                    // Copy properties from the client's provided FeatureInfo
                    setAccentColor(featureInfo.getAccentColor())
                    setMaxSelectionLimit(featureInfo.getMaxSelectionLimit())
                    setMimeTypes(featureInfo.getMimeTypes())
                    setOrderedSelection(featureInfo.isOrderedSelection())
                    setThemeNightMode(featureInfo.getThemeNightMode())

                    // Restore any already selectedMedia that are being held in this state object,
                    // plus
                    // copy in anything the client has asked to be preselected as well.
                    setPreSelectedUris(
                        listOf(
                            *selectedMedia.toTypedArray(),
                            *featureInfo.getPreSelectedUris().toTypedArray(),
                        )
                    )
                }
                .build()

        provider.openSession(
            /* hostToken =       */ checkNotNull(surfaceHostToken) {
                "Expected surfaceHostToken to not be null"
            },
            /* displayId =       */ displayId,
            /* width =           */ surfaceSize.width,
            /* height =          */ surfaceSize.height,
            /* featureInfo =     */ featureInfoWithLocalState,
            /* clientExecutor =  */ coroutineContext[CoroutineDispatcher]?.asExecutor()
                // Fallback to Main.immediate if the dispatcher in this context is null.
                // (i.e) for Instrumented tests.
                ?: Dispatchers.Main.immediate.asExecutor(),
            /* callback =        */ innerClientCallback,
        )

        // Acquire the session from the provider before starting the client.
        val session = deferredSession.await()

        // Pass the initial expanded state as the session starts.
        session.notifyPhotoPickerExpanded(isExpanded)

        try {
            awaitCancellation()
        } finally {
            // When this suspended function is cancelled clean up the session by closing it.
            session.close()
        }
    }

    private fun checkIfReady() {
        surfaceHostToken?.let {
            // Ensure the surface size and displayId are something other than initialization.
            if (surfaceSize != IntSize.Zero && displayId != -1) {
                isReady = true
            }
        }
    }

    public companion object {
        /**
         * Generates an object which implements [EmbeddedPhotoPickerClient] that is passed to the
         * [EmbeddedPhotoPickerProvider] during openSession that will proxy calls that it receives
         * to the relevant provided input callable.
         */
        @RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
        public fun createEmbeddedPhotoPickerClient(
            onSessionOpened: (EmbeddedPhotoPickerSession) -> Unit,
            onSessionError: (Throwable) -> Unit,
            onUriPermissionGranted: (List<Uri>) -> Unit,
            onUriPermissionRevoked: (List<Uri>) -> Unit,
            onSelectionComplete: () -> Unit,
        ): EmbeddedPhotoPickerClient {
            return object : EmbeddedPhotoPickerClient {
                override fun onSessionOpened(session: EmbeddedPhotoPickerSession) {
                    onSessionOpened(session)
                }

                override fun onSessionError(throwable: Throwable) {
                    onSessionError(throwable)
                }

                override fun onUriPermissionGranted(uris: List<Uri>) {
                    onUriPermissionGranted(uris)
                }

                override fun onUriPermissionRevoked(uris: List<Uri>) {
                    onUriPermissionRevoked(uris)
                }

                override fun onSelectionComplete() {
                    onSelectionComplete()
                }
            }
        }
    }
}

/**
 * Defines callback methods for client interactions. These callbacks are invoked by the service to
 * notify the client about various events.
 */
internal interface ClientCallbacks {
    /**
     * Called when a session error occurs.
     *
     * @param throwable The error that occurred.
     */
    var onSessionError: (Throwable) -> Unit

    /**
     * Called when URI permissions are granted to the service.
     *
     * @param uris A list of URIs for which permissions have been granted.
     */
    var onUriPermissionGranted: (List<Uri>) -> Unit

    /**
     * Called when URI permissions are revoked from the service.
     *
     * @param uris A list of URIs for which permissions have been revoked.
     */
    var onUriPermissionRevoked: (List<Uri>) -> Unit

    /** Called when the selection process is complete. */
    var onSelectionComplete: () -> Unit
}

/**
 * Internal implementation of [AbstractEmbeddedPhotoPickerState].
 *
 * This largely relies on the abstract class for its implementation, but handles session related
 * state serialization and restoration. Additionally, it also wires the client's callbacks to the
 * state objects callbacks.
 *
 * @param isExpanded Initial state of expansion. Defaults to `false`.
 * @param selectedMedia Initial set of selected media. Defaults to an empty set.
 */
@ExperimentalPhotoPickerComposeApi
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
internal class EmbeddedPhotoPickerStateImpl(
    isExpanded: Boolean = false,
    selectedMedia: Set<Uri> = emptySet(),
) : AbstractEmbeddedPhotoPickerState(isExpanded, selectedMedia) {

    companion object {
        /**
         * A [Saver] for [EmbeddedPhotoPickerStateImpl] to enable state restoration, for example,
         * across configuration changes. It saves and restores the `selectedMedia` and `isExpanded`
         * properties.
         */
        val saver = run {
            val selectedUrisKey = "selectedUris"
            val isExpandedKey = "isExpanded"
            mapSaver(
                save = {
                    mapOf(selectedUrisKey to it.selectedMedia, isExpandedKey to it.isExpanded)
                },
                restore = {

                    // Casting here using `as?` such that if there is no value, or it can't fulfill
                    // the collection interface, the value will be null and ignored.
                    @Suppress("UNCHECKED_CAST")
                    val uris: Collection<Uri>? = it[selectedUrisKey] as? Collection<Uri>
                    val isExpanded: Boolean = it[isExpandedKey] as? Boolean ?: false

                    EmbeddedPhotoPickerStateImpl(isExpanded, uris?.toSet() ?: emptySet<Uri>())
                },
            )
        }
    }

    /**
     * Holds client-provided callbacks for photo picker events. These callbacks are invoked by the
     * corresponding `on...` methods of this class.
     */
    internal var clientCallbacks: ClientCallbacks =
        object : ClientCallbacks {
            override var onSessionError by mutableStateOf<(Throwable) -> Unit>({})
            override var onUriPermissionGranted by mutableStateOf<(List<Uri>) -> Unit>({})
            override var onUriPermissionRevoked by mutableStateOf<(List<Uri>) -> Unit>({})
            override var onSelectionComplete by mutableStateOf<() -> Unit>({})
        }

    /**
     * Invoked when a session error occurs in the photo picker. Delegates to the
     * [ClientCallbacks.onSessionError] callback.
     *
     * @param throwable The error that occurred.
     */
    override fun onSessionError(throwable: Throwable) {
        clientCallbacks.onSessionError(throwable)
    }

    /**
     * Invoked when URI permissions are granted for a list of URIs. Delegates to the
     * [ClientCallbacks.onUriPermissionGranted] callback.
     *
     * @param uris The list of [Uri]s for which permission was granted.
     */
    override fun onUriPermissionGranted(uris: List<Uri>) {
        clientCallbacks.onUriPermissionGranted(uris)
    }

    /**
     * Invoked when URI permissions are revoked for a list of URIs. Delegates to the
     * [ClientCallbacks.onUriPermissionRevoked] callback.
     *
     * @param uris The list of [Uri]s for which permission was revoked.
     */
    override fun onUriPermissionRevoked(uris: List<Uri>) {
        clientCallbacks.onUriPermissionRevoked(uris)
    }

    /**
     * Invoked when the user completes their selection in the photo picker. Delegates to the
     * [ClientCallbacks.onSelectionComplete] callback.
     */
    override fun onSelectionComplete() {
        clientCallbacks.onSelectionComplete()
    }
}

/**
 * Generates a [EmbeddedPhotoPickerState] object and remembers it. This object can be used to
 * interact with the remote EmbeddedPhotoPickerSession. This object's state will survive the
 * activity or process recreation but the underlying EmbeddedPhotoPickerSession will be recreated
 * along with the activity. (When [EmbeddedPhotoPickerState#runSession] is next called.)
 *
 * If a clean state object is needed, be sure to provide some input keys that are unique.
 *
 * @param inputs A set of inputs such that, when any of them have changed, will cause the state to
 *   reset and [init] to be rerun. Note that state restoration DOES NOT validate against inputs
 *   provided before value was saved.
 * @param initialExpandedValue the initial expanded state of the photopicker. This property only
 *   affects the initial value, and has no further effect.
 * @param initialMediaSelection the initial set of media that should be selected inside of the
 *   Photopicker. Note: this should only be URIs that have been given to the calling application by
 *   the Photopicker itself, do not pass generic URIs or MediaStore URIs, as they will be ignored.
 * @param onSessionError Called when the PhotoPicker has indicated an error with the current
 *   session.
 * @param onUriPermissionGranted Called when the user has selected media items in the embedded
 *   session.
 * @param onUriPermissionRevoked Called when the user has deselected media items in the embedded
 *   session.
 * @param onSelectionComplete Called when the user is done with their selection and the app should
 *   collapse or close the PhotoPicker.
 * @return state object for interacting with an EmbeddedPhotoPickerSession.
 */
@ExperimentalPhotoPickerComposeApi
@Composable
@RequiresExtension(extension = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, version = 15)
public fun rememberEmbeddedPhotoPickerState(
    vararg inputs: Any?,
    initialExpandedValue: Boolean = false,
    initialMediaSelection: Set<Uri> = emptySet<Uri>(),
    onSessionError: (Throwable) -> Unit = {},
    onUriPermissionGranted: (List<Uri>) -> Unit = {},
    onUriPermissionRevoked: (List<Uri>) -> Unit = {},
    onSelectionComplete: () -> Unit = {},
): EmbeddedPhotoPickerState {
    val config = LocalConfiguration.current
    val context = LocalContext.current
    val displayId = remember(context) { context.display.displayId }

    // Use context as a minimum key (plus whatever the client supplies) so when this recomposes the
    // constructor doesn't run for trivial internal state changes.
    val state =
        rememberSaveable(context, *inputs, saver = EmbeddedPhotoPickerStateImpl.saver) {
                EmbeddedPhotoPickerStateImpl(initialExpandedValue, initialMediaSelection)
            }
            .apply {
                this.clientCallbacks.onSessionError = onSessionError
                this.clientCallbacks.onUriPermissionGranted = onUriPermissionGranted
                this.clientCallbacks.onUriPermissionRevoked = onUriPermissionRevoked
                this.clientCallbacks.onSelectionComplete = onSelectionComplete
            }

    SideEffect {
        state.displayId = displayId
        state.notifyConfigurationChanged(config)
    }

    return state
}
