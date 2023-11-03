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
import android.graphics.Rect
import android.util.Log
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.wear.watchface.complications.ComplicationDataSourceInfo
import androidx.wear.watchface.complications.ComplicationSlotBounds
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationExperimental
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.control.data.IdTypeAndDefaultProviderPolicyWireFormat
import androidx.wear.watchface.data.ComplicationStateWireFormat
import androidx.wear.watchface.data.IdAndComplicationStateWireFormat
import androidx.wear.watchface.style.CurrentUserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting
import androidx.wear.watchface.style.UserStyleSetting.ComplicationSlotsUserStyleSetting.ComplicationSlotsOption
import androidx.wear.watchface.utility.TraceEvent
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private fun getComponentName(context: Context) =
    ComponentName(context.packageName, context.javaClass.name)

/**
 * The [ComplicationSlot]s associated with the [WatchFace]. Dynamic creation of ComplicationSlots
 * isn't supported, however complicationSlots can be enabled and disabled by
 * [ComplicationSlotsUserStyleSetting].
 *
 * @param complicationSlotCollection The [ComplicationSlot]s associated with the watch face, may be
 *   empty.
 * @param currentUserStyleRepository The [CurrentUserStyleRepository] used to listen for
 *   [ComplicationSlotsUserStyleSetting] changes and apply them.
 */
public class ComplicationSlotsManager(
    complicationSlotCollection: Collection<ComplicationSlot>,
    private val currentUserStyleRepository: CurrentUserStyleRepository
) {
    internal companion object {
        internal const val TAG = "ComplicationSlotsManager"
    }

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
     */
    @VisibleForTesting
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @field:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public lateinit var watchState: WatchState

    internal lateinit var watchFaceHostApi: WatchFaceHostApi
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
        val accessibilityTraversalIndex: Int,
        val nameResourceId: Int?,
        val screenReaderNameResourceId: Int?
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
                    it.accessibilityTraversalIndex,
                    it.nameResourceId,
                    it.screenReaderNameResourceId
                )
            }
        )

    private val complicationListeners = HashSet<TapCallback>()

    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var configExtrasChangeCallback: WatchFace.ComplicationSlotConfigExtrasChangeCallback? =
        null

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @VisibleForTesting
    public constructor(
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

    private fun applyInitialComplicationConfig() {
        for ((id, complication) in complicationSlots) {
            val initialConfig = initialComplicationConfigs[id]!!
            complication.complicationSlotBounds = initialConfig.complicationSlotBounds
            complication.enabled = initialConfig.enabled
            complication.accessibilityTraversalIndex = initialConfig.accessibilityTraversalIndex
            complication.nameResourceId = initialConfig.nameResourceId
            complication.screenReaderNameResourceId = initialConfig.screenReaderNameResourceId
        }
        onComplicationsUpdated()
    }

    @WorkerThread
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun listenForStyleChanges(coroutineScope: CoroutineScope) {
        var previousOption =
            currentUserStyleRepository.schema.findComplicationSlotsOptionForUserStyle(
                currentUserStyleRepository.userStyle.value
            )

        // Apply the initial settings on the worker thread.
        previousOption?.let { applyComplicationSlotsStyleCategoryOption(it) }

        // Add a listener so we can track changes and automatically apply them on the UIThread
        coroutineScope.launch {
            currentUserStyleRepository.userStyle.collect {
                val newlySelectedOption =
                    currentUserStyleRepository.schema.findComplicationSlotsOptionForUserStyle(
                        currentUserStyleRepository.userStyle.value
                    )

                if (previousOption != newlySelectedOption) {
                    previousOption = newlySelectedOption
                    if (newlySelectedOption == null) {
                        applyInitialComplicationConfig()
                    } else {
                        applyComplicationSlotsStyleCategoryOption(newlySelectedOption)
                    }
                }
            }
        }
    }

    /** Finish initialization. */
    @WorkerThread
    internal fun init(
        renderer: Renderer,
        complicationSlotInvalidateListener: ComplicationSlot.InvalidateListener
    ) =
        TraceEvent("ComplicationSlotsManager.init").use {
            this.renderer = renderer

            for ((_, complication) in complicationSlots) {
                complication.init(
                    complicationSlotInvalidateListener,
                    renderer.watchState.isHeadless
                )

                // Force lazy construction of renderers.
                complication.renderer.onRendererCreated(renderer)
            }

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
            complication.enabled = override?.enabled ?: initialConfig.enabled
            complication.accessibilityTraversalIndex =
                override?.accessibilityTraversalIndex ?: initialConfig.accessibilityTraversalIndex
            complication.nameResourceId = override?.nameResourceId ?: initialConfig.nameResourceId
            complication.screenReaderNameResourceId =
                override?.screenReaderNameResourceId ?: initialConfig.screenReaderNameResourceId
        }
        onComplicationsUpdated()
    }

    /** Returns the [ComplicationSlot] corresponding to [id], if there is one, or `null`. */
    public operator fun get(id: Int): ComplicationSlot? = complicationSlots[id]

    @UiThread
    internal fun onComplicationsUpdated() =
        TraceEvent("ComplicationSlotsManager.updateComplications").use {
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
                        labelsDirty ||
                            complication.dataDirty ||
                            complication.complicationBoundsDirty ||
                            complication.accessibilityTraversalIndexDirty ||
                            complication.nameResourceIdDirty ||
                            complication.screenReaderNameResourceIdDirty

                    if (
                        complication.defaultDataSourcePolicyDirty ||
                            complication.defaultDataSourceTypeDirty
                    ) {
                        // Note this is a NOP in the androidx flow.
                        watchFaceHostApi.setDefaultComplicationDataSourceWithFallbacks(
                            complication.id,
                            complication.defaultDataSourcePolicy.dataSourcesAsList(),
                            complication.defaultDataSourcePolicy.systemDataSourceFallback,
                            complication.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType
                                .toWireComplicationType()
                        )
                    }

                    complication.dataDirty = false
                    complication.complicationBoundsDirty = false
                    complication.defaultDataSourcePolicyDirty = false
                    complication.defaultDataSourceTypeDirty = false
                    complication.accessibilityTraversalIndexDirty = false
                    complication.nameResourceIdDirty = false
                    complication.screenReaderNameResourceIdDirty = false
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
     * @param complicationSlotId The id of the complication that the data relates to. If this id is
     *   unrecognized the call will be a NOP, the only circumstance when that happens is if the
     *   watch face changes it's complication config between runs e.g. during development.
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationDataUpdate(
        complicationSlotId: Int,
        data: ComplicationData,
        instant: Instant
    ) {
        val complication = complicationSlots[complicationSlotId]
        if (complication == null) {
            Log.e(
                TAG,
                "onComplicationDataUpdate failed due to invalid complicationSlotId=" +
                    "$complicationSlotId with data=$data"
            )
            return
        }
        complication.dataDirty = complication.dataDirty || (complication.renderer.getData() != data)
        complication.setComplicationData(data, instant)
    }

    /**
     * Sets complication data, returning a restoration function.
     *
     * As this is used for screen shots, dirty bit (used for content description) is not set.
     */
    @UiThread
    internal fun setComplicationDataForScreenshot(
        slotIdToData: Map<Int, ComplicationData>,
        instant: Instant,
    ): AutoCloseable {
        val restores = mutableListOf<AutoCloseable>()
        val restore = AutoCloseable { restores.forEach(AutoCloseable::close) }
        try {
            for ((id, data) in slotIdToData) {
                val slot = complicationSlots[id]
                if (slot == null) {
                    Log.e(
                        TAG,
                        "setComplicationDataForScreenshot failed due to invalid " +
                            "complicationSlotId=$id with data=$data"
                    )
                    continue
                }
                restores.add(slot.setComplicationDataForScreenshot(data, instant))
            }
        } catch (e: Throwable) {
            // Cleanup changes on failure.
            restore.close()
            throw e
        }
        return restore
    }

    /**
     * For each slot, if the ComplicationData is timeline complication data then the correct
     * override is selected for [instant].
     */
    @UiThread
    internal fun selectComplicationDataForInstant(instant: Instant) {
        for ((_, complication) in complicationSlots) {
            complication.selectComplicationDataForInstant(instant, forceUpdate = false)
        }

        // selectComplicationDataForInstant may have changed the complication, if so we need to
        // update the content description labels.
        if (complicationSlots.isNotEmpty()) {
            onComplicationsUpdated()
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun freezeSlotForEdit(
        slotId: Int,
        from: ComplicationDataSourceInfo?,
        to: ComplicationDataSourceInfo?,
    ) {
        complicationSlots[slotId]?.freezeForEdit(from = from, to = to)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun unfreezeAllSlotsForEdit(clearData: Boolean) {
        for (slot in complicationSlots.values) {
            slot.unfreezeForEdit(clearData)
        }
    }

    /**
     * Returns the id of the complication slot at coordinates x, y or `null` if there isn't one.
     * Initially checks slots without margins (should be no overlaps) then then if there was no hit
     * it tries again this time with margins (overlaps are possible) reporting the first hit if any.
     *
     * @param x The x coordinate of the point to perform a hit test
     * @param y The y coordinate of the point to perform a hit test
     * @return The [ComplicationSlot] at coordinates x, y or {@code null} if there isn't one
     */
    public fun getComplicationSlotAt(@Px x: Int, @Px y: Int): ComplicationSlot? =
        findLowestIdMatchingComplicationOrNull { complication ->
            complication.enabled &&
                complication.tapFilter.hitTest(
                    complication,
                    renderer.screenBounds,
                    x,
                    y,
                    includeMargins = false
                )
        }
            ?: findLowestIdMatchingComplicationOrNull { complication ->
                complication.enabled &&
                    complication.tapFilter.hitTest(
                        complication,
                        renderer.screenBounds,
                        x,
                        y,
                        includeMargins = true
                    )
            }

    /**
     * Finds the [ComplicationSlot] with the lowest id for which [predicate] returns true, returns
     * `null` otherwise.
     */
    private fun findLowestIdMatchingComplicationOrNull(
        predicate: (complication: ComplicationSlot) -> Boolean
    ): ComplicationSlot? {
        var bestComplication: ComplicationSlot? = null
        var bestId = 0
        for ((id, complication) in complicationSlots) {
            if (predicate.invoke(complication) && (bestComplication == null || bestId > id)) {
                bestComplication = complication
                bestId = id
            }
        }
        return bestComplication
    }

    /**
     * Returns the background [ComplicationSlot] if there is one or `null` otherwise.
     *
     * @return The background [ComplicationSlot] if there is one or `null` otherwise
     */
    public fun getBackgroundComplicationSlot(): ComplicationSlot? =
        complicationSlots.entries
            .firstOrNull { it.value.boundsType == ComplicationSlotBoundsType.BACKGROUND }
            ?.value

    /**
     * Called when the user single taps on a [ComplicationSlot], invokes the permission request
     * helper if needed, otherwise returns the tap action.
     *
     * @param complicationSlotId The ID for the [ComplicationSlot] that was single tapped
     */
    @UiThread
    internal fun onComplicationSlotSingleTapped(complicationSlotId: Int) {
        // Check if the complication is missing permissions.
        val data = complicationSlots[complicationSlotId]?.renderer?.getData() ?: return
        if (data.type == ComplicationType.NO_PERMISSION) {
            watchFaceHostApi
                .getContext()
                .startActivity(
                    ComplicationHelperActivity.createPermissionRequestHelperIntent(
                            watchFaceHostApi.getContext(),
                            getComponentName(watchFaceHostApi.getContext()),
                            watchFaceHostApi.getComplicationDeniedIntent(),
                            watchFaceHostApi.getComplicationRationaleIntent()
                        )
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    /**
     * Note getComplicationState may be called before [init], this is why it requires
     * [screenBounds].
     */
    @OptIn(ComplicationExperimental::class)
    @UiThread
    internal fun getComplicationsState(screenBounds: Rect) =
        complicationSlots.map {
            val systemDataSourceFallbackDefaultType =
                it.value.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType
                    .toWireComplicationType()
            IdAndComplicationStateWireFormat(
                it.key,
                ComplicationStateWireFormat(
                    it.value.computeBounds(screenBounds, applyMargins = false),
                    it.value.computeBounds(screenBounds, applyMargins = true),
                    it.value.boundsType,
                    ComplicationType.toWireTypes(it.value.supportedTypes),
                    it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                    it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                    systemDataSourceFallbackDefaultType,
                    it.value.defaultDataSourcePolicy.primaryDataSourceDefaultType
                        ?.toWireComplicationType()
                        ?: systemDataSourceFallbackDefaultType,
                    it.value.defaultDataSourcePolicy.secondaryDataSourceDefaultType
                        ?.toWireComplicationType()
                        ?: systemDataSourceFallbackDefaultType,
                    it.value.enabled,
                    it.value.initiallyEnabled,
                    it.value.renderer.getData().type.toWireComplicationType(),
                    it.value.fixedComplicationDataSource,
                    it.value.configExtras,
                    it.value.nameResourceId,
                    it.value.screenReaderNameResourceId,
                    it.value.boundingArc?.toWireFormat()
                )
            )
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

    /** Removes a [TapCallback] previously added by [addTapListener]. */
    @UiThread
    public fun removeTapListener(tapCallback: TapCallback) {
        complicationListeners.remove(tapCallback)
    }

    @UiThread
    internal fun dump(writer: IndentingPrintWriter) {
        writer.println("ComplicationSlotsManager:")
        writer.println(
            "lastComplicationTapDownEvents=" +
                lastComplicationTapDownEvents
                    .map { it.key.toString() + "->" + it.value }
                    .joinToString(", ")
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
        complicationSlots
            .map {
                IdTypeAndDefaultProviderPolicyWireFormat(
                    it.key,
                    it.value.defaultDataSourcePolicy.dataSourcesAsList(),
                    it.value.defaultDataSourcePolicy.systemDataSourceFallback,
                    it.value.defaultDataSourcePolicy.systemDataSourceFallbackDefaultType
                        .toWireComplicationType()
                )
            }
            .toTypedArray()

    /**
     * Returns the earliest [Instant] after [afterInstant] at which any complication field in any
     * enabled complication may change.
     */
    internal fun getNextChangeInstant(afterInstant: Instant): Instant {
        var minInstant = Instant.MAX
        for ((_, complication) in complicationSlots) {
            if (!complication.enabled) {
                continue
            }
            val instant = complication.complicationData.value.getNextChangeInstant(afterInstant)
            if (instant.isBefore(minInstant)) {
                minInstant = instant
            }
        }
        return minInstant
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComplicationSlotsManager

        return complicationSlots == other.complicationSlots
    }

    override fun hashCode(): Int {
        return complicationSlots.hashCode()
    }
}
