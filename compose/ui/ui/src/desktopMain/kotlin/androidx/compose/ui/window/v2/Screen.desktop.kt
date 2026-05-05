/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.window.v2

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.DpInsets
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.minus
import androidx.compose.ui.window.toDpInsets
import androidx.compose.ui.window.toDpRect
import java.awt.GraphicsDevice
import java.awt.Toolkit


/**
 * Represents a screen (a graphical device on which windows can be rendered).
 *
 * Note that a [Screen] holds a reference to an underlying native object representing it.
 * Additionally, screens can come and go (the user may disconnect one, for example).
 * Therefore, it is highly discouraged to keep long-term references to [Screen] objects, beyond
 * their use in [WindowScreenProviderScope] or [WindowGeometryProviderScope].
 *
 * Note: this class may be moved to `androidx.compose.ui.window` before stabilization.
 */
@ExperimentalComposeUiApi
class Screen internal constructor(
    internal val device: GraphicsDevice
) {

    /**
     * The identifier of the screen.
     */
    val id: String = device.iDstring

    private val configuration
        get() = device.defaultConfiguration

    /**
     * The bounds of the screen in the coordinate system of all screens.
     *
     * Note that the coordinates may be negative, as the screen may be positioned
     * to the left or above the primary screen.
     */
    val bounds: DpRect
        get() = configuration.bounds.toDpRect()

    /**
     * The insets of the screen.
     */
    val insets: DpInsets
        get() = Toolkit.getDefaultToolkit().getScreenInsets(configuration).toDpInsets()

    /**
     * The bounds of the screen excluding the insets.
     */
    val availableBounds: DpRect
        get() = bounds - insets

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Screen) return false

        return this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String = "Screen $id"
}
