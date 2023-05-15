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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.support.wearable.complications.ComplicationData
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.watchface.complications.SystemDataSources.DataSourceId
import androidx.wear.watchface.style.data.UserStyleWireFormat
import java.time.Duration
import kotlinx.coroutines.CoroutineScope

/** The API [WatchFaceImpl] uses to communicate with the system. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface WatchFaceHostApi {
    /** The [WatchFaceService.SystemTimeProvider]. */
    public val systemTimeProvider: WatchFaceService.SystemTimeProvider

    /**
     * Equivalent to [android.os.Build.VERSION.SDK_INT], but allows override for any
     * platform-independent versioning.
     *
     * This is meant to only be used in androidTest, which only support testing on one SDK. In
     * Robolectric tests use `@Config(sdk = [Build.VERSION_CODES.*])`.
     *
     * Note that this cannot override platform-dependent versioning, which means inconsistency.
     */
    public val wearSdkVersion: Int

    /** Returns the watch face's [Context]. */
    public fun getContext(): Context

    /** Returns the UI thread [Handler]. */
    public fun getUiThreadHandler(): Handler

    /** Returns the UI thread [CoroutineScope]. */
    public fun getUiThreadCoroutineScope(): CoroutineScope

    /** Returns the Worker thread [Handler]. */
    public fun getBackgroundThreadHandler(): Handler

    /** Returns the initial user style stored by the system if there is one or null otherwise. */
    public fun getInitialUserStyle(): UserStyleWireFormat?

    /**
     * Creates/updates ContentDescriptionLabels for text-to-speech screen readers to make your
     * [ComplicationSlot]s, buttons, and any other text on your watchface accessible.
     *
     * Each label is a region of the screen in absolute pixel coordinates, along with time-dependent
     * text, the labels are generated from data in [ComplicationSlotsManager],
     * [Renderer.additionalContentDescriptionLabels], [Renderer.screenBounds] and
     * [Renderer.getMainClockElementBounds].
     *
     * This is a fairly expensive operation so use it sparingly (e.g. do not call it in `onDraw()`).
     */
    public fun updateContentDescriptionLabels()

    /**
     * Sets the complicationSlots which are active in the watchface. ComplicationSlot data will be
     * received for these ids. This is to support the pre-android R flow.
     *
     * Any ids not in the provided [complicationSlotIds] will be considered inactive.
     *
     * If complication data sources and complication data types have been configured, the data
     * received will match the type chosen by the user. If no complication data source has been
     * configured, data of type [ComplicationData.TYPE_NOT_CONFIGURED] will be received.
     *
     * Ids here are chosen by the watch face to represent each complication and can be any integer.
     */
    public fun setActiveComplicationSlots(complicationSlotIds: IntArray)

    /**
     * For WSL flow, not used in androidx flow.
     *
     * Accepts a list of custom complication data sources to attempt to set as the default
     * complication data source for the specified watch face [ComplicationSlot] id. The custom
     * complication data sources are tried in turn, if the first doesn't exist then the next one is
     * tried and so on. If none of them exist then the specified system complication data source is
     * set as the default instead.
     *
     * This will do nothing if the complication data sources are not installed, or if the specified
     * type is not supported by the complication data sources, or if the user has already selected a
     * complication data source for the [ComplicationSlot].
     *
     * Note that if the watch face has not yet been granted the `RECEIVE_COMPLICATION_DATA`
     * permission, it will not be able to receive data from the complication data source unless the
     * complication data source is from the same app package as the watch face, or the complication
     * data source lists the watch face as a safe watch face. For system complication data sources
     * that may be used before your watch face has the permission, use a safe complication data
     * source instead.
     *
     * A complication data source not satisfying the above conditions may still be set as a default
     * using this method, but the watch face will receive placeholder data of type
     * [ComplicationData.TYPE_NO_PERMISSION] until the permission has been granted.
     *
     * @param complicationSlotId The [ComplicationSlot] id.
     * @param dataSources data sources The list of non-system complication data sources to try in
     *   order before falling back to fallbackSystemProvider. This list may be null.
     * @param fallbackSystemProvider The system complication data source to use if none of the
     *   complication data sources could be used.
     * @param type The type of complication data that should be provided. Must be one of the types
     *   defined in [ComplicationData].
     */
    public fun setDefaultComplicationDataSourceWithFallbacks(
        complicationSlotId: Int,
        dataSources: List<ComponentName>?,
        @DataSourceId fallbackSystemProvider: Int,
        type: Int
    )

    /** Schedules a call to [Renderer.renderInternal] to draw the next frame. */
    @UiThread public fun invalidate()

    public fun postInvalidate(delay: Duration = Duration.ZERO)

    /** Intent to launch the complication permission denied activity. */
    public fun getComplicationDeniedIntent(): Intent?

    /** Intent to launch the complication permission rationale activity. */
    public fun getComplicationRationaleIntent(): Intent?

    /**
     * Sent by the system at the top of the minute. This may trigger rendering if SysUI hasn't sent
     * called setWatchUiState.
     */
    @UiThread public fun onActionTimeTick() {}

    /** The engine must notify the system that the watch face's colors have changed. */
    @OptIn(WatchFaceExperimental::class)
    public fun onWatchFaceColorsChanged(watchFaceColors: WatchFaceColors?) {}

    /** Requests the system to capture an updated preview image. */
    public fun sendPreviewImageNeedsUpdateRequest() {}
}
