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
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.utility.TraceEvent
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private fun getComponentName(context: Context) = ComponentName(
    context.packageName,
    context.javaClass.name
)

/**
 * The [ComplicationSlot]s associated with the [WatchFace]. Dynamic creation of ComplicationSlots
 * isn't supported, however complicationSlots can be enabled and disabled by
 * [ComplicationSlotsUserStyleSetting].
 *
 * @param complicationSlotCollection The [ComplicationSlot]s associated with the watch face, may be
 * empty.
 * @param currentUserStyleRepository The [CurrentUserStyleRepository] used to listen for
 * [ComplicationSlotsUserStyleSetting] changes and apply them.
 */
public class ComplicationSlotsManager(
    complicationSlotCollection: Collection<ComplicationSlot>,
    private val currentUserStyleRepository: CurrentUserStyleRepository
) {
    /**
     * Interface used to report user taps on the [ComplicationSlot]. See [addTapListener] and
     * [removeTapListener].
     */
    public interface TapCallback {
        /**
         * Called when the user single taps on a complication.
         *
         * @param complicationSlotId The id for the [ComplicationSlot] that was tapped
         */
        public fun onComplicationSlotTapped(complicationSlotId: Int) {}
    }

    /**
     * The [WatchState] of the associated watch face. This is only initialized after
     * [WatchFaceService.createComplicationSlotsManager] has completed.
     *
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public lateinit var watchState: WatchState

    private lateinit var watchFaceHostApi: WatchFaceHostApi
    internal lateinit var renderer: Renderer

    /** A map of complication IDs to complicationSlots. */
    public val complicationSlots: Map<Int, ComplicationSlot> =
        complicationSlotCollection.associateBy(ComplicationSlot::id)

    /**
     * Map of [ComplicationSlot] id to the latest [TapType.DOWN] [TapEvent] that the
     * ComplicationSlot received, if any.
     */
    public val lastComplicationTapDownEvents: Map<Int, TapEvent> = HashMap()

    private class InitialComplicationConfig(
        val complicationSlotBounds: ComplicationSlotBounds,
        val enabled: Boolean,
        val accessibilityTraversalIndex: Int
    )

    // Copy of the original complication configs. This is necessary because the semantics of
    // [ComplicationSlotsUserStyleSetting] are defined in terms of an override applied to the
    // initial config.
    private val initialComplicationConfigs: Map<Int, InitialComplicationConfig> =
        complicationSlotCollection.associateBy(
            { it.id },
            {
                InitialComplicationConfig(
                    it.complicationSlotBounds,
                    it.enabled,
                    it.accessibilityTraversalIndex
                )
            }
        )

    private val complicationListeners = HashSet<TapCallback>()

    @VisibleForTesting
    internal constructor(
        complicationSlotCollection: Collection<ComplicationSlot>,
        currentUserStyleRepository: CurrentUserStyleRepository,
        renderer: Renderer
    ) : this(complicationSlotCollection, currentUserStyleRepository) {
        this.renderer = renderer
    }

    init {
        for ((_, complication) in complicationSlots) {
            complication.complicationSlotsManager = this
        }
    }

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun listenForStyleChanges(coroutineScope: CoroutineScope) {
        val complicationsStyleCategory =
            currentUserStyleRepository.schema.userStyleSettings.firstOrNull {
                it is ComplicationSlotsUserStyleSetting
            }

        // Add a listener if we have a ComplicationSlotsUserStyleSetting so we can track changes and
        // automatically apply them.
        if (complicationsStyleCategory != null) {
            // Ensure we apply any initial StyleCategoryOption overlay by initializing with null.
            var previousOption: ComplicationSlotsOption? = null
            coroutineScope.launch {
                currentUserStyleRepository.userStyle.collect { userStyle ->
                    val newlySelectedOption =
                        userStyle[complicationsStyleCategory]!! as ComplicationSlotsOption
                    if (previousOption != newlySelectedOption) {
                        previousOption = newlySelectedOption
                        applyComplicationSlotsStyleCategoryOption(newlySelectedOption)
                    }
                }
            }
        }
    }

    /** Finish initialization. */
    @WorkerThread
    internal fun init(
        watchFaceHostApi: WatchFaceHostApi,
        renderer: Renderer,
        complicationSlotInvalidateListener: ComplicationSlot.InvalidateListener
    ) = TraceEvent("ComplicationSlotsManager.init").use {
        this.watchFaceHostApi = watchFaceHostApi
        this.renderer = renderer

        for ((_, complication) in complicationSlots) {
            complication.init(complicationSlotInvalidateListener, renderer.watchState.isHeadless)

            // Force lazy construction of renderers.
            complication.renderer.onRendererCreated(renderer)
        }

        listenForStyleChanges(watchFaceHostApi.getUiThreadCoroutineScope())

        require(
            complicationSlots.values.distinctBy { it.renderer }.size ==
                complicationSlots.values.size
        ) {
            "Complication renderer instances are not sharable."
        }

        // Activate complicationSlots.
        onComplicationsUpdated()
    }

    internal fun applyComplicationSlotsStyleCategoryOption(styleOption: ComplicationSlotsOption) {
        for ((id, complication) in complicationSlots) {
            val override = styleOption.complicationSlotOverlays.find { it.complicationSlotId == id }
            val initialConfig = initialComplicationConfigs[id]!!
            // Apply styleOption overrides.
            complication.complicationSlotBounds =
                override?.complicationSlotBounds ?: initialConfig.complicationSlotBounds
            complication.enabled =
                override?.enabled ?: initialConfig.enabled
            complication.accessibilityTraversalIndex =
                override?.accessibilityTraversalIndex ?: initialConfig.accessibilityTraversalIndex
        }
        onComplicationsUpdated()
    }

    /** Returns the [ComplicationSlot] corresponding to [id], if there is one, or `null`. */
    public operator fun get(id: Int): ComplicationSlot? = complicationSlots[id]

    @UiThread
    internal fun onComplicationsUpdated() = TraceEvent(
        "ComplicationSlotsManager.updateComplications"
    ).use {
        if (!this::watchFaceHostApi.isInitialized) {
            return
        }
        val activeKeys = mutableListOf<Int>()

        // Work out what's changed using the dirty flags and issue appropriate watchFaceHostApi
        // calls.
        var enabledDirty = false
        var labelsDirty = false
        for ((id, complication) in complicationSlots) {
            enabledDirty = enabledDirty || complication.enabledDirty
            labelsDirty = labelsDirty || complication.enabledDirty

            if (complication.enabled) {
                activeKeys.add(id)

                labelsDirty =
                    labelsDirty || complication.dataDirty || complication.complicationBoundsDirty ||
                    complication.accessibilityTraversalIndexDirty

                if (complication.defaultDataSourcePolicyDirty ||
                    complication.defaultDataSourceTypeDirty
                ) {
                    watchFaceHostApi.setDefaultComplicationDataSourceWithFallbacks(
                        complication.id,
                        complication.defaultDataSourcePolicy.dataSourcesAsList(),
                        complication.defaultDataSourcePolicy.systemDataSourceFallback,
                        complication.defaultDataSourceType.toWireComplicationType()
                    )
                }

                complication.dataDirty = false
                complication.complicationBoundsDirty = false
                complication.supportedTypesDirty = false
                complication.defaultDataSourcePolicyDirty = false
                complication.defaultDataSourceTypeDirty = false
            }

            complication.enabledDirty = false
        }

        if (enabledDirty) {
            watchFaceHostApi.setActiveComplicationSlots(activeKeys.toIntArray())
        }

        if (labelsDirty) {
            watchFaceHostApi.updateContentDescriptionLabels()
        }
    }

    /**
     * Called when new complication data is received.
     *
     * @param complicationSlotId The id of the complication that the data relates to. If this
     * id is unrecognized the call will be a NOP, the only circumstance when that happens is if the
     * watch face changes it's complication config between runs e.g. during development.
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationDataUpdate(complicationSlotId: Int, data: ComplicationData) {
        val complication = complicationSlots[complicationSlotId] ?: return
        complication.dataDirty = complication.dataDirty ||
            (complication.renderer.getData() != data)
        complication.renderer.loadData(data, true)
        (complication.complicationData as MutableStateFlow<ComplicationData>).value = data
    }

    /**
     * For use by screen shot code which will reset the data afterwards, hence dirty bit not set.
     */
    internal fun setComplicationDataUpdateSync(complicationSlotId: Int, data: ComplicationData) {
        val complication = complicationSlots[complicationSlotId] ?: return
        complication.renderer.loadData(data, false)
        (complication.complicationData as MutableStateFlow<ComplicationData>).value = data
    }

    @UiThread
    internal fun clearComplicationData() {
        for ((_, complication) in complicationSlots) {
            complication.renderer.loadData(NoDataComplicationData(), false)
            (complication.complicationData as MutableStateFlow).value = NoDataComplicationData()
        }
    }

    /**
     * Returns the id of the complication slot at coordinates x, y or `null` if there isn't one.
     *
     * @param x The x coordinate of the point to perform a hit test
     * @param y The y coordinate of the point to perform a hit test
     * @return The [ComplicationSlot] at coordinates x, y or {@code null} if there isn't one
     */
    public fun getComplicationSlotAt(@Px x: Int, @Px y: Int): ComplicationSlot? =
        complicationSlots.values.firstOrNull { complication ->
            complication.enabled && complication.tapFilter.hitTest(
                complication,
                renderer.screenBounds,
                x,
                y
            )
        }

    /**
     * Returns the background [ComplicationSlot] if there is one or `null` otherwise.
     *
     * @return The background [ComplicationSlot] if there is one or `null` otherwise
     */
    public fun getBackgroundComplicationSlot(): ComplicationSlot? =
        complicationSlots.entries.firstOrNull {
            it.value.boundsType == ComplicationSlotBoundsType.BACKGROUND
        }?.value

    /**
     * Called when the user single taps on a [ComplicationSlot], invokes the permission request
     * helper if needed, otherwise s the tap action.
     *
     * @param complicationSlotId The ID for the [ComplicationSlot] that was single tapped
     */
    @SuppressWarnings("SyntheticAccessor")
    @UiThread
    internal fun onComplicationSlotSingleTapped(complicationSlotId: Int) {
        // Check if the complication is missing permissions.
        val data = complicationSlots[complicationSlotId]?.renderer?.getData() ?: return
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
            complicationListener.onComplicationSlotTapped(complicationSlotId)
        }
    }

    @UiThread
    internal fun onTapDown(complicationSlotId: Int, tapEvent: TapEvent) {
        (lastComplicationTapDownEvents as HashMap)[complicationSlotId] = tapEvent
    }

    /**
     * Adds a [TapCallback] which is called whenever the user interacts with a complication slot.
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
        writer.println("ComplicationSlotsManager:")
        writer.println(
            "lastComplicationTapDownEvents=" + lastComplicationTapDownEvents.map {
                it.key.toString() + "->" + it.value
            }.joinToString(", ")
        )
        writer.increaseIndent()
        for ((_, complication) in complicationSlots) {
            complication.dump(writer)
        }
        writer.decreaseIndent()
    }

    /**
     * This will be called from a binder thread. That's OK because we don't expect this
     * ComplicationSlotsManager to be accessed at all from the UiThread in that scenario. See
     * [androidx.wear.watchface.control.IWatchFaceInstanceServiceStub.getDefaultProviderPolicies].
     */
    @WorkerThread
    internal fun getDefaultProviderPolicies(): Array<IdTypeAndDefaultProviderPolicyWireFormat> =
        complicationSlots.map {
            IdTypeAndDefaultProviderPolicyWireFormat(
                it.key,
                it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                it.value.defaultDataSourceType.toWireComplicationType()
            )
        }.toTypedArray()
}
