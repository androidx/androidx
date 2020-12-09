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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.support.wearable.watchface.accessibility.AccessibilityUtils
import android.support.wearable.watchface.accessibility.ContentDescriptionLabel
import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import androidx.wear.complications.ComplicationBounds
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.DefaultComplicationProviderPolicy
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.ComplicationType
import androidx.wear.complications.data.IdAndComplicationData
import androidx.wear.watchface.data.ComplicationBoundsType
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleRepository
import androidx.wear.watchface.style.UserStyleSetting.ComplicationsUserStyleSetting
import java.lang.ref.WeakReference

private fun getComponentName(context: Context) = ComponentName(
    context.packageName,
    context.javaClass.name
)

/**
 * The [Complication]s associated with the [WatchFace]. Dynamic creation of complications isn't
 * supported, however complications can be enabled and disabled by [ComplicationsUserStyleSetting].
 */
public class ComplicationsManager(
    /** The complications associated with the watch face, may be empty. */
    complicationCollection: Collection<Complication>,

    /**
     * The [UserStyleRepository] used to listen for [ComplicationsUserStyleSetting] changes and
     * apply them.
     */
    private val userStyleRepository: UserStyleRepository
) {
    /**
     * Interface used to report user taps on the complication. See [addTapListener] and
     * [removeTapListener].
     */
    public interface TapCallback {
        /**
         * Called when the user single taps on a complication.
         *
         * @param complicationId The watch face's id for the complication single tapped
         */
        public fun onComplicationSingleTapped(complicationId: Int) {}

        /**
         * Called when the user double taps on a complication, launches the complication
         * configuration activity.
         *
         * @param complicationId The watch face's id for the complication double tapped
         */
        public fun onComplicationDoubleTapped(complicationId: Int) {}
    }

    private lateinit var watchFaceHostApi: WatchFaceHostApi
    private lateinit var calendar: Calendar
    private lateinit var renderer: Renderer
    private lateinit var pendingUpdate: CancellableUniqueTask

    /** A map of complication IDs to complications. */
    public val complications: Map<Int, Complication> =
        complicationCollection.associateBy(Complication::id)

    private class InitialComplicationConfig(
        val complicationBounds: ComplicationBounds,
        val enabled: Boolean,
        val supportedTypes: List<ComplicationType>,
        val defaultProviderPolicy: DefaultComplicationProviderPolicy,
        val defaultProviderType: ComplicationType
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
                    it.supportedTypes,
                    it.defaultProviderPolicy,
                    it.defaultProviderType
                )
            }
        )

    private val complicationListeners = HashSet<TapCallback>()

    @VisibleForTesting
    internal constructor(
        complicationCollection: Collection<Complication>,
        userStyleRepository: UserStyleRepository,
        renderer: Renderer
    ) : this(complicationCollection, userStyleRepository) {
        this.renderer = renderer
    }

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
            complication.init(this, complicationInvalidateListener)
        }

        val complicationsStyleCategory =
            userStyleRepository.schema.userStyleSettings.firstOrNull {
                it is ComplicationsUserStyleSetting
            }

        // Add a listener if we have a ComplicationsUserStyleSetting so we can track changes and
        // automatically apply them.
        if (complicationsStyleCategory != null) {
            var previousOption =
                userStyleRepository.userStyle.selectedOptions[complicationsStyleCategory] as
                    ComplicationsUserStyleSetting.ComplicationsOption
            userStyleRepository.addUserStyleListener(
                object : UserStyleRepository.UserStyleListener {
                    override fun onUserStyleChanged(userStyle: UserStyle) {
                        val newlySelectedOption =
                            userStyle.selectedOptions[complicationsStyleCategory] as
                                ComplicationsUserStyleSetting.ComplicationsOption
                        if (previousOption != newlySelectedOption) {
                            previousOption = newlySelectedOption
                            applyComplicationsStyleCategoryOption(newlySelectedOption)
                        }
                    }
                }
            )
        }

        // Activate complications.
        scheduleUpdate()
    }

    internal fun applyComplicationsStyleCategoryOption(
        styleOption: ComplicationsUserStyleSetting.ComplicationsOption
    ) {
        for ((id, complication) in complications) {
            val override = styleOption.complicationOverlays.find { it.complicationId == id }
            val initialConfig = initialComplicationConfigs[id]!!
            // Apply styleOption overrides.
            complication.complicationBounds =
                override?.complicationBounds ?: initialConfig.complicationBounds
            complication.enabled =
                override?.enabled ?: initialConfig.enabled
            complication.supportedTypes =
                override?.supportedTypes ?: initialConfig.supportedTypes
            complication.defaultProviderPolicy =
                override?.defaultProviderPolicy ?: initialConfig.defaultProviderPolicy
            complication.defaultProviderType =
                override?.defaultProviderType ?: initialConfig.defaultProviderType
        }
    }

    /** Returns the [Complication] corresponding to [id], if there is one, or `null`. */
    public operator fun get(id: Int): Complication? = complications[id]

    internal fun scheduleUpdate() {
        if (!pendingUpdate.isPending()) {
            pendingUpdate.postUnique(this::updateComplications)
        }
    }

    internal fun getContentDescriptionLabels(): Array<ContentDescriptionLabel> {
        val labels = mutableListOf<ContentDescriptionLabel>()

        // Add a ContentDescriptionLabel for the main clock element.
        labels.add(
            ContentDescriptionLabel(
                renderer.getMainClockElementBounds(),
                AccessibilityUtils.makeTimeAsComplicationText(
                    watchFaceHostApi.getContext()
                )
            )
        )
        // Add a ContentDescriptionLabel for each enabled complication.
        for ((_, complication) in complications) {
            if (complication.enabled) {
                if (complication.boundsType == ComplicationBoundsType.BACKGROUND) {
                    ComplicationBoundsType.BACKGROUND
                } else {
                    complication.renderer.idAndData?.let {
                        labels.add(
                            ContentDescriptionLabel(
                                watchFaceHostApi.getContext(),
                                complication.computeBounds(renderer.screenBounds),
                                it.complicationData.asWireComplicationData()
                            )
                        )
                    }
                }
            }
        }

        return labels.toTypedArray()
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
                    labelsDirty || complication.dataDirty ||
                    complication.complicationBoundsDirty

                if (complication.defaultProviderPolicyDirty ||
                    complication.defaultProviderTypeDirty
                ) {
                    watchFaceHostApi.setDefaultComplicationProviderWithFallbacks(
                        complication.id,
                        complication.defaultProviderPolicy.providersAsList(),
                        complication.defaultProviderPolicy.systemProviderFallback,
                        complication.defaultProviderType.asWireComplicationType()
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
            // Register ContentDescriptionLabels which are used to provide accessibility data.
            watchFaceHostApi.setContentDescriptionLabels(
                getContentDescriptionLabels()
            )
        }
    }

    /**
     * Called when new complication data is received.
     *
     * @param watchFaceComplicationId The id of the complication that the data relates to. This
     *     will be an id that was previously sent in a call to [setActiveComplications].
     * @param data The [ComplicationData] that should be displayed in the complication.
     */
    @UiThread
    internal fun onComplicationDataUpdate(watchFaceComplicationId: Int, data: ComplicationData) {
        val complication = complications[watchFaceComplicationId]!!
        complication.dataDirty =
            complication.dataDirty || (complication.renderer.idAndData?.complicationData != data)
        complication.renderer.idAndData = IdAndComplicationData(watchFaceComplicationId, data)
        (complication.complicationData as MutableObservableWatchData<ComplicationData>).value =
            data
    }

    /**
     * Brings attention to the complication by briefly highlighting it to provide visual feedback
     * when the user has tapped on it.
     *
     * @param complicationId The watch face's ID of the complication to briefly highlight
     */
    @UiThread
    public fun bringAttentionToComplication(complicationId: Int) {
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
    public fun getComplicationAt(x: Int, y: Int): Complication? =
        complications.entries.firstOrNull {
            it.value.enabled && it.value.boundsType != ComplicationBoundsType.BACKGROUND &&
                it.value.computeBounds(renderer.screenBounds).contains(x, y)
        }?.value

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
        val data = complications[complicationId]?.renderer?.idAndData ?: return
        if (data.complicationData.type == ComplicationType.NO_PERMISSION) {
            watchFaceHostApi.getContext().startActivity(
                ComplicationHelperActivity.createPermissionRequestHelperIntent(
                    watchFaceHostApi.getContext(),
                    getComponentName(watchFaceHostApi.getContext())
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        data.complicationData.tapAction?.send()
        for (complicationListener in complicationListeners) {
            complicationListener.onComplicationSingleTapped(complicationId)
        }
    }

    /**
     * Called when the user double taps on a complication, launches the complication
     * configuration activity.
     *
     * @param complicationId The watch face's id for the complication double tapped
     */
    @SuppressWarnings("SyntheticAccessor")
    @UiThread
    internal fun onComplicationDoubleTapped(complicationId: Int) {
        // Check if the complication is missing permissions.
        val complication = complications[complicationId] ?: return
        val data = complication.renderer.idAndData ?: return
        if (data.complicationData.type == ComplicationType.NO_PERMISSION) {
            watchFaceHostApi.getContext().startActivity(
                ComplicationHelperActivity.createPermissionRequestHelperIntent(
                    watchFaceHostApi.getContext(),
                    getComponentName(watchFaceHostApi.getContext())
                )
            )
            return
        }
        watchFaceHostApi.getContext().startActivity(
            ComplicationHelperActivity.createProviderChooserHelperIntent(
                watchFaceHostApi.getContext(),
                getComponentName(watchFaceHostApi.getContext()),
                complicationId,
                IntArray(complication.supportedTypes.size) {
                    complication.supportedTypes[it].asWireComplicationType()
                }
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        for (complicationListener in complicationListeners) {
            complicationListener.onComplicationDoubleTapped(complicationId)
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
}
