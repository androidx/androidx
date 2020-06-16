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

/**
 * A style descriptor for watch faces.
 *
 * <p>Parameters here affect how the system UI will be drawn over a watch face. An instance of this
 * class should be passed in to {@link WatchFaceService.Engine#setWatchFaceStyle} in the {@code
 * onCreate} method of your {@link WatchFaceService.Engine#onCreate} override.
 *
 * <p>To construct a WatchFaceStyle use {@link WatchFaceStyle.Builder}.
 */
@SuppressWarnings("BanParcelableUsage")
class WatchFaceStyle(
    val component: ComponentName,
    val viewProtectionMode: Int,
    val statusBarGravity: Int,
    @ColorInt val accentColor: Int,
    val showUnreadCountIndicator: Boolean,
    val hideNotificationIndicator: Boolean,
    val acceptsTapEvents: Boolean
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
            }
        )
    }

    override fun describeContents() = 0

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

    companion object {

        const val DEFAULT_ACCENT_COLOR = Color.WHITE

        /**
         * Whether to put a semi-transparent black background behind the status bar to make it visible
         * on white backgrounds.
         */
        const val PROTECT_STATUS_BAR = 0x1

        /**
         * Whether to put a semi-transparent black background behind the "Ok Google" string to make it
         * visible on a white background.
         */
        const val PROTECT_HOTWORD_INDICATOR = 0x2

        /**
         * Whether to dim the entire watch face background slightly so the time and icons are always
         * visible.
         */
        const val PROTECT_WHOLE_SCREEN = 0x4

        /** Parcelable Creator */
        @JvmField
        val CREATOR = object : Parcelable.Creator<WatchFaceStyle> {
            override fun createFromParcel(parcel: Parcel): WatchFaceStyle {
                val bundle = parcel.readBundle(this::class.java.classLoader)!!
                return WatchFaceStyle(
                    bundle.getParcelable(Constants.KEY_COMPONENT)!!,
                    bundle.getInt(Constants.KEY_VIEW_PROTECTION_MODE),
                    bundle.getInt(Constants.KEY_STATUS_BAR_GRAVITY),
                    bundle.getInt(Constants.KEY_ACCENT_COLOR, DEFAULT_ACCENT_COLOR),
                    bundle.getBoolean(Constants.KEY_SHOW_UNREAD_INDICATOR),
                    bundle.getBoolean(Constants.KEY_HIDE_NOTIFICATION_INDICATOR),
                    bundle.getBoolean(Constants.KEY_ACCEPTS_TAPS)
                )
            }

            override fun newArray(size: Int) = arrayOfNulls<WatchFaceStyle?>(size)
        }
    }

    class Builder(private val componentName: ComponentName) {
        private var viewProtectionMode: Int = 0
        private var statusBarGravity: Int = 0
        @ColorInt
        private var accentColor: Int = DEFAULT_ACCENT_COLOR
        private var showUnreadCountIndicator: Boolean = false
        private var hideNotificationIndicator: Boolean = false
        private var acceptsTapEvents: Boolean = true

        /**
         * @param viewProtectionMode The view protection mode bit field, must be a combination of
         *     zero or more of {@link #PROTECT_STATUS_BAR}, {@link #PROTECT_HOTWORD_INDICATOR},
         *     {@link #PROTECT_WHOLE_SCREEN}.
         * @throws IllegalArgumentException if viewProtectionMode has an unexpected value
         */
        fun setViewProtectionMode(viewProtectionMode: Int) = apply {
            if (viewProtectionMode < 0 ||
                viewProtectionMode >
                PROTECT_STATUS_BAR + PROTECT_HOTWORD_INDICATOR + PROTECT_WHOLE_SCREEN
            ) {
                throw IllegalArgumentException(
                    "View protection must be combination " +
                            "PROTECT_STATUS_BAR, PROTECT_HOTWORD_INDICATOR or PROTECT_WHOLE_SCREEN"
                )
            }
            this.viewProtectionMode = viewProtectionMode
        }

        /**
         * Sets position of status icons (battery state, lack of connection) on the screen.
         *
         * @param statusBarGravity This must be any combination of horizontal Gravity constant ({@link
         *     Gravity#LEFT}, {@link Gravity#CENTER_HORIZONTAL}, {@link Gravity#RIGHT}) and vertical
         *     Gravity constants ({@link Gravity#TOP}, {@link Gravity#CENTER_VERTICAL}, {@link
         *     Gravity#BOTTOM}), e.g. {@code Gravity.LEFT | Gravity.BOTTOM}. On circular screens, only
         *     the vertical gravity is respected.
         */
        fun setStatusBarGravity(statusBarGravity: Int) = apply {
            this.statusBarGravity = statusBarGravity
        }

        /**
         * Sets the accent color which can be set by developers to customise watch face. It will be used
         * when drawing the unread notification indicator. Default color is white.
         */
        fun setAccentColor(@ColorInt accentColor: Int) = apply {
            this.accentColor = accentColor
        }

        /**
         * Sets whether to add an indicator of how many unread cards there are in the stream. The
         * indicator will be displayed next to status icons (battery state, lack of connection).
         *
         * @param showUnreadCountIndicator if true an indicator will be shown
         */
        fun setShowUnreadCountIndicator(showUnreadCountIndicator: Boolean) = apply {
            this.showUnreadCountIndicator = showUnreadCountIndicator
        }

        /**
         * Sets whether to hide the dot indicator that is displayed at the bottom of the watch face if
         * there are any unread notifications. The default value is false, but note that the dot will
         * not be displayed if the numerical unread count indicator is being shown (i.e. if {@link
         * #getShowUnreadCountIndicator} is true).
         *
         * @param hideNotificationIndicator if true an indicator will be hidden
         */
        fun setHideNotificationIndicator(hideNotificationIndicator: Boolean) = apply {
            this.hideNotificationIndicator = hideNotificationIndicator
        }

        /**
         * Sets whether this watchface accepts tap events. The default is false.
         *
         * <p>Watchfaces that set this {@code true} are indicating they are prepared to receive {@link
         * android.support.wearable.watchface.WatchFaceService#TAP_TYPE_TOUCH}, {@link
         * android.support.wearable.watchface.WatchFaceService#TAP_TYPE_TOUCH_CANCEL}, and {@link
         * android.support.wearable.watchface.WatchFaceService#TAP_TYPE_TAP} events.
         *
         * @param acceptsTapEvents whether to receive touch events.
         */
        fun setAcceptsTapEvents(acceptsTapEvents: Boolean) = apply {
            this.acceptsTapEvents = acceptsTapEvents
        }

        fun build() = WatchFaceStyle(
            componentName,
            viewProtectionMode,
            statusBarGravity,
            accentColor,
            showUnreadCountIndicator,
            hideNotificationIndicator,
            acceptsTapEvents
        )
    }
}
