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
import android.os.Handler
import android.support.wearable.complications.ComplicationData
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.wear.complications.SystemProviders.ProviderId
import androidx.wear.watchface.style.data.UserStyleWireFormat

/**
 * The API [WatchFaceImpl] uses to communicate with the system.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface WatchFaceHostApi {
    /** Returns the watch face's [Context]. */
    public fun getContext(): Context

    /** Returns the UI thread [Handler]. */
    public fun getUiThreadHandler(): Handler

    /** Returns the Worker thread [Handler]. */
    public fun getBackgroundThreadHandler(): Handler

    /** Returns the initial user style stored by the system if there is one or null otherwise. */
    public fun getInitialUserStyle(): UserStyleWireFormat?

    /**
     * Sets ContentDescriptionLabels for text-to-speech screen readers to make your
     * complications, buttons, and any other text on your watchface accessible.
     *
     * Each label is a region of the screen in absolute coordinates, along with
     * time-dependent text. The regions must not overlap.
     *
     * You must set all labels at the same time; previous labels will be cleared. An empty
     * array clears all labels.
     *
     * In addition to labeling your complications, please include a label that will read the
     * current time. You can use [android.support.wearable.watchface.accessibility
     * .AccessibilityUtils.makeTimeAsComplicationText] to generate the proper
     * [android.support.wearable.complications.ComplicationText].
     *
     * This is a fairly expensive operation so use it sparingly (e.g. do not call it in
     * `onDraw()`).
     */
    public fun updateContentDescriptionLabels()

    /**
     * Sets the complications which are active in the watchface. Complication data will be
     * received for these ids.
     *
     * Any ids not in the provided [watchFaceComplicationIds] will be considered inactive.
     *
     * If providers and complication data types have been configured, the data received will
     * match the type chosen by the user. If no provider has been configured, data of type
     * [ComplicationData.TYPE_NOT_CONFIGURED] will be received.
     *
     * Ids here are chosen by the watch face to represent each complication and can be any
     * integer.
     */
    public fun setActiveComplications(watchFaceComplicationIds: IntArray)

    /**
     * Accepts a list of custom providers to attempt to set as the default provider for the
     * specified watch face complication id. The custom providers are tried in turn, if the
     * first doesn't exist then the next one is tried and so on. If none of them exist then the
     * specified system provider is set as the default instead.
     *
     * This will do nothing if the providers are not installed, or if the specified type is
     * not supported by the providers, or if the user has already selected a provider for the
     * complication.
     *
     * Note that if the watch face has not yet been granted the `RECEIVE_COMPLICATION_DATA`
     * permission, it will not be able to receive data from the provider unless the provider is
     * from the same app package as the watch face, or the provider lists the watch face as a
     * safe watch face. For system providers that may be used before your watch face has the
     * permission, use [setDefaultSystemComplicationProvider] with a safe provider
     * instead.
     *
     * A provider not satisfying the above conditions may still be set as a default using
     * this method, but the watch face will receive placeholder data of type
     * [ComplicationData.TYPE_NO_PERMISSION] until the permission has been granted.
     *
     * @param watchFaceComplicationId The watch face's ID for the complication
     * @param providers The list of non-system providers to try in order before falling back to
     * fallbackSystemProvider. This list may be null.
     * @param fallbackSystemProvider The system provider to use if none of the providers could be
     * used.
     * @param type The type of complication data that should be provided. Must be one of the types
     * defined in [ComplicationData].
     */
    public fun setDefaultComplicationProviderWithFallbacks(
        watchFaceComplicationId: Int,
        providers: List<ComponentName>?,
        @ProviderId fallbackSystemProvider: Int,
        type: Int
    )

    /** Schedules a call to [Renderer.renderInternal] to draw the next frame. */
    @UiThread
    public fun invalidate()
}
