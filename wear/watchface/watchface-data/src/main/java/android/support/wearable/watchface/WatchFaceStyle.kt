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

package android.support.wearable.watchface

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo

/**
 * A style descriptor for watch faces.
 *
 * <p>Parameters here affect how the system UI will be drawn over a watch face. An instance of this
 * class should be passed in to [WatchFaceService.Engine.setWatchFaceStyle] in the `onCreate` method
 * of your [WatchFaceService.Engine.onCreate] override.
 *
 * <p>To construct a WatchFaceStyle use [WatchFaceStyle.Builder].
 */
@SuppressWarnings("BanParcelableUsage")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class WatchFaceStyle(
    public val component: ComponentName,
    public val viewProtectionMode: Int,
    public val statusBarGravity: Int,
    @ColorInt public val accentColor: Int,
    public val showUnreadCountIndicator: Boolean,
    public val hideNotificationIndicator: Boolean,
    public val acceptsTapEvents: Boolean,
    /**
     * Escape hatch needed by WearOS to implement backwards compatibility. Note WearOS support for
     * obsolete WatchFaceStyle properties may be removed without notice.
     */
    public val compatBundle: Bundle? = null
) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeBundle(
            Bundle().apply {
                putParcelable(Constants.KEY_COMPONENT, component)
                putInt(Constants.KEY_VIEW_PROTECTION_MODE, viewProtectionMode)
                putInt(Constants.KEY_STATUS_BAR_GRAVITY, statusBarGravity)
                putInt(Constants.KEY_ACCENT_COLOR, accentColor)
                putBoolean(Constants.KEY_SHOW_UNREAD_INDICATOR, showUnreadCountIndicator)
                putBoolean(Constants.KEY_HIDE_NOTIFICATION_INDICATOR, hideNotificationIndicator)
                putBoolean(Constants.KEY_ACCEPTS_TAPS, acceptsTapEvents)
                compatBundle?.let { putAll(compatBundle) }
            }
        )
    }

    override fun describeContents(): Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WatchFaceStyle

        if (component != other.component) return false
        if (viewProtectionMode != other.viewProtectionMode) return false
        if (statusBarGravity != other.statusBarGravity) return false
        if (accentColor != other.accentColor) return false
        if (showUnreadCountIndicator != other.showUnreadCountIndicator) return false
        if (hideNotificationIndicator != other.hideNotificationIndicator) return false
        if (acceptsTapEvents != other.acceptsTapEvents) return false

        return true
    }

    override fun hashCode(): Int {
        var result = component.hashCode()
        result = 31 * result + viewProtectionMode
        result = 31 * result + statusBarGravity
        result = 31 * result + accentColor
        result = 31 * result + showUnreadCountIndicator.hashCode()
        result = 31 * result + hideNotificationIndicator.hashCode()
        result = 31 * result + acceptsTapEvents.hashCode()
        return result
    }

    public companion object {

        public const val DEFAULT_ACCENT_COLOR: Int = Color.WHITE

        /**
         * Whether to put a semi-transparent black background behind the status bar to make it
         * visible on white backgrounds.
         */
        public const val PROTECT_STATUS_BAR: Int = 0x1

        /**
         * Whether to put a semi-transparent black background behind the "Ok Google" string to make
         * it visible on a white background.
         */
        public const val PROTECT_HOTWORD_INDICATOR: Int = 0x2

        /**
         * Whether to dim the entire watch face background slightly so the time and icons are always
         * visible.
         */
        public const val PROTECT_WHOLE_SCREEN: Int = 0x4

        /** Parcelable Creator */
        @JvmField
        @Suppress("DEPRECATION")
        public val CREATOR: Parcelable.Creator<WatchFaceStyle> =
            object : Parcelable.Creator<WatchFaceStyle> {
                override fun createFromParcel(parcel: Parcel): WatchFaceStyle {
                    val bundle = parcel.readBundle(this::class.java.classLoader)!!
                    return WatchFaceStyle(
                        bundle.getParcelable(Constants.KEY_COMPONENT)!!,
                        bundle.getInt(Constants.KEY_VIEW_PROTECTION_MODE),
                        bundle.getInt(Constants.KEY_STATUS_BAR_GRAVITY),
                        bundle.getInt(Constants.KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR),
                        bundle.getBoolean(Constants.KEY_SHOW_UNREAD_INDICATOR),
                        bundle.getBoolean(Constants.KEY_HIDE_NOTIFICATION_INDICATOR),
                        bundle.getBoolean(Constants.KEY_ACCEPTS_TAPS),
                        bundle
                    )
                }

                override fun newArray(size: Int) = arrayOfNulls<WatchFaceStyle?>(size)
            }
    }
}
