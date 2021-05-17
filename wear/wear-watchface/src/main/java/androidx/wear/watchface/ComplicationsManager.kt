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

package androidx.wear.watchface

import android.annotation.SuppressLint
import android.app.PendingIntent.CanceledException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.EmptyComplicationData
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting.ComplicationsOption
import java.lang.ref.WeakReference

private fun getComponentName(context: Context) = ComponentName(
    context.packageName,
    context.javaClass.name
)

/**
 * The [Complication]s associated with the [WatchFace]. Dynamic creation of complications isn't
 * supported, however complications can be enabled and disabled by [ComplicationsUserStyleSetting].
 *
 * @param complicationCollection The complications associated with the watch face, may be empty.
 * @param currentUserStyleRepository The [CurrentUserStyleRepository] used to listen for
 * [ComplicationsUserStyleSetting] changes and apply them.
 */
public class ComplicationsManager(
    complicationCollection: Collection<Complication>,
    private val currentUserStyleRepository: CurrentUserStyleRepository
) {
    /**
     * Interface used to report user taps on the complication. See [addTapListener] and
     * [removeTapListener].
     */
    public interface TapCallback {
        /**
         * Called when the user single taps on a complication.
         *
         * @param complicationId The watch face's id for the complication that was tapped
         */
        public fun onComplicationTapped(complicationId: Int) {}
    }

    /**
     * The [WatchState] of the associated watch face. This is only initialized after
     * [WatchFaceService.createComplicationsManager] has completed.
     *
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public lateinit var watchState: WatchState

    private lateinit var watchFaceHostApi: WatchFaceHostApi
    private lateinit var calendar: Calendar
    internal lateinit var renderer: Renderer
    private lateinit var pendingUpdate: CancellableUniqueTask

    /** A map of complication IDs to complications. */
    public val complications: Map<Int, Complication> =
        complicationCollection.associateBy(Complication::id)

    private class InitialComplicationConfig(
        val complicationBounds: ComplicationBounds,
        val enabled: Boolean,
        val accessibilityTraversalIndex: Int
    )

    // Copy of the original complication configs. This is necessary because the semantics of
    // [ComplicationsUserStyleSetting] are defined in terms of an override applied to the initial
    // config.
    private val initialComplicationConfigs: Map<Int, InitialComplicationConfig> =
        complicationCollection.associateBy(
            { it.id },
            {
                InitialComplicationConfig(
                    it.complicationBounds,
                    it.enabled,
                    it.accessibilityTraversalIndex
                )
            }
        )

    private val complicationListeners = HashSet<TapCallback>()

    @VisibleForTesting
    internal constructor(
        complicationCollection: Collection<Complication>,
        currentUserStyleRepository: CurrentUserStyleRepository,
        renderer: Renderer
    ) : this(complicationCollection, currentUserStyleRepository) {
        this.renderer = renderer
    }

    init {
        val complicationsStyleCategory =
            currentUserStyleRepository.schema.userStyleSettings.firstOrNull {
                it is ComplicationsUserStyleSetting
            }

        // Add a listener if we have a ComplicationsUserStyleSetting so we can track changes and
        // automatically apply them.
        if (complicationsStyleCategory != null) {
            // Ensure we apply any initial StyleCategoryOption overlay by initializing with null.
            var previousOption: ComplicationsOption? = null
            currentUserStyleRepository.addUserStyleChangeListener(
                object : CurrentUserStyleRepository.UserStyleChangeListener {
                    override fun onUserStyleChanged(userStyle: UserStyle) {
                        val newlySelectedOption =
                            userStyle[complicationsStyleCategory]!! as ComplicationsOption
                        if (previousOption != newlySelectedOption) {
                            previousOption = newlySelectedOption
                            applyComplicationsStyleCategoryOption(newlySelectedOption)
                        }
                    }
                }
            )
        }

        for ((_, complication) in complications) {
            complication.complicationsManager = this
        }
    }

    /** Finish initialization. */
    internal fun init(
        watchFaceHostApi: WatchFaceHostApi,
        calendar: Calendar,
        renderer: Renderer,
        complicationInvalidateListener: Complication.InvalidateListener
    ) {
        this.watchFaceHostApi = watchFaceHostApi
        this.calendar = calendar
        this.renderer = renderer
        pendingUpdate = CancellableUniqueTask(watchFaceHostApi.getHandler())

        for ((_, complication) in complications) {
            complication.init(complicationInvalidateListener)
        }

        // Activate complications.
        scheduleUpdate()
    }

    internal fun applyComplicationsStyleCategoryOption(styleOption: ComplicationsOption) {
        for ((id, complication) in complications) {
            val override = styleOption.complicationOverlays.find { it.complicationId == id }
            val initialConfig = initialComplicationConfigs[id]!!
            // Apply styleOption overrides.
            complication.complicationBounds =
                override?.complicationBounds ?: initialConfig.complicationBounds
            complication.enabled =
                override?.enabled ?: initialConfig.enabled
            complication.accessibilityTraversalIndex =
                override?.accessibilityTraversalIndex ?: initialConfig.accessibilityTraversalIndex
        }
    }

    /** Returns the [Complication] corresponding to [id], if there is one, or `null`. */
    public operator fun get(id: Int): Complication? = complications[id]

    internal fun scheduleUpdate() {
        if (this::pendingUpdate.isInitialized && !pendingUpdate.isPending()) {
            pendingUpdate.postUnique(this::updateComplications)
        }
    }

    private fun updateComplications() {
        val activeKeys = mutableListOf<Int>()

        // Work out what's changed using the dirty flags and issue appropriate watchFaceHostApi
        // calls.
        var enabledDirty = false
        var labelsDirty = false
        for ((id, complication) in complications) {
            enabledDirty = enabledDirty || complication.enabledDirty
            labelsDirty = labelsDirty || complication.enabledDirty

            if (complication.enabled) {
                activeKeys.add(id)

                labelsDirty =
                    labelsDirty || complication.dataDirty || complication.complicationBoundsDirty ||
                    complication.accessibilityTraversalIndexDirty

                if (complication.defaultProviderPolicyDirty ||
                    complication.defaultProviderTypeDirty
                ) {
                    watchFaceHostApi.setDefaultComplicationProviderWithFallbacks(
                        complication.id,
                        complication.defaultProviderPolicy.providersAsList(),
                        complication.defaultProviderPolicy.systemProviderFallback,
                        complication.defaultProviderType.toWireComplicationType()
                    )
                }

                complication.dataDirty = false
                complication.complicationBoundsDirty = false
                complication.supportedTypesDirty = false
                complication.defaultProviderPolicyDirty = false
                complication.defaultProviderTypeDirty = false
            }

            complication.enabledDirty = false
        }

        if (enabledDirty) {
            watchFaceHostApi.setActiveComplications(activeKeys.toIntArray())
        }

        if (labelsDirty) {
            watchFaceHostApi.updateContentDescriptionLabels()
        }
    }

    /**
     * Called when new complication data is received.
     *
     * @param watchFaceComplicationId The id of the complication that the data relates to. If this
     * id is unrecognized the call will be a NOP, the only circumstance when that happens is if the
     * watch face changes it's complication config between runs e.g. during development.
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData) {
        val complication = complications[watchFaceComplicationId] ?: return
        complication.dataDirty = complication.dataDirty ||
            (complication.renderer.getData() != data)
        complication.renderer.loadData(data, true)
        (complication.complicationData as MutableObservableWatchData<ComplicationData>).value =
            data
    }

    @UiThread
    internal fun clearComplicationData() {
        for ((_, complication) in complications) {
            complication.renderer.loadData(null, false)
            (complication.complicationData as MutableObservableWatchData).value =
                EmptyComplicationData()
        }
    }

    /**
     * Starts a short animation, briefly highlighting the complication to provide visual feedback
     * when the user has tapped on it.
     *
     * @param complicationId The watch face's ID of the complication to briefly highlight
     */
    @UiThread
    public fun displayPressedAnimation(complicationId: Int) {
        val complication = requireNotNull(complications[complicationId]) {
            "No complication found with ID $complicationId"
        }
        complication.setIsHighlighted(true)

        val weakRef = WeakReference(this)
        watchFaceHostApi.getHandler().postDelayed(
            {
                // The watch face might go away before this can run.
                if (weakRef.get() != null) {
                    complication.setIsHighlighted(false)
                }
            },
            WatchFaceImpl.CANCEL_COMPLICATION_HIGHLIGHTED_DELAY_MS
        )
    }

    /**
     * Returns the id of the complication at coordinates x, y or `null` if there isn't one.
     *
     * @param x The x coordinate of the point to perform a hit test
     * @param y The y coordinate of the point to perform a hit test
     * @return The complication at coordinates x, y or {@code null} if there isn't one
     */
    public fun getComplicationAt(@Px x: Int, @Px y: Int): Complication? =
        complications.values.firstOrNull { complication ->
            complication.enabled && complication.tapFilter.hitTest(
                complication,
                renderer.screenBounds,
                x,
                y
            )
        }

    /**
     * Returns the background complication if there is one or `null` otherwise.
     *
     * @return The background complication if there is one or `null` otherwise
     */
    public fun getBackgroundComplication(): Complication? =
        complications.entries.firstOrNull {
            it.value.boundsType == ComplicationBoundsType.BACKGROUND
        }?.value

    /**
     * Called when the user single taps on a complication, invokes the permission request helper
     * if needed, otherwise s the tap action.
     *
     * @param complicationId The watch face's id for the complication single tapped
     */
    @SuppressWarnings("SyntheticAccessor")
    @UiThread
    internal fun onComplicationSingleTapped(complicationId: Int) {
        // Check if the complication is missing permissions.
        val data = complications[complicationId]?.renderer?.getData() ?: return
        if (data.type == ComplicationType.NO_PERMISSION) {
            watchFaceHostApi.getContext().startActivity(
                ComplicationHelperActivity.createPermissionRequestHelperIntent(
                    watchFaceHostApi.getContext(),
                    getComponentName(watchFaceHostApi.getContext())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        try {
            data.tapAction?.send()
        } catch (e: CanceledException) {
            // In case the PendingIntent is no longer able to execute the request.
            // We don't need to do anything here.
        }
        for (complicationListener in complicationListeners) {
            complicationListener.onComplicationTapped(complicationId)
        }
    }

    /**
     * Adds a [TapCallback] which is called whenever the user interacts with a complication.
     */
    @UiThread
    @SuppressLint("ExecutorRegistration")
    public fun addTapListener(tapCallback: TapCallback) {
        complicationListeners.add(tapCallback)
    }

    /**
     * Removes a [TapCallback] previously added by [addTapListener].
     */
    @UiThread
    public fun removeTapListener(tapCallback: TapCallback) {
        complicationListeners.remove(tapCallback)
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("ComplicationsManager:")
        writer.increaseIndent()
        for ((_, complication) in complications) {
            complication.dump(writer)
        }
        writer.decreaseIndent()
    }

    internal fun getDefaultProviderPolicies(): Array<IdTypeAndDefaultProviderPolicyWireFormat> =
        complications.map {
            IdTypeAndDefaultProviderPolicyWireFormat(
                it.key,
                it.value.defaultProviderPolicy.providersAsList(),
                it.value.defaultProviderPolicy.systemProviderFallback,
                it.value.defaultProviderType.toWireComplicationType()
            )
        }.toTypedArray()
}
