/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.events

import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.ui.pointer.PointerDeviceKind

// /// Base class for touch, stylus, or mouse events.
// ///
// /// Pointer events operate in the coordinate space of the screen, scaled to
// /// logical pixels. Logical pixels approximate a grid with about 38 pixels per
// /// centimeter, or 96 pixels per inch.
// ///
// /// This allows gestures to be recognized independent of the precise hardware
// /// characteristics of the device. In particular, features such as touch slop
// /// (see [kTouchSlop]) can be defined in terms of roughly physical lengths so
// /// that the user can shift their finger by the same distance on a high-density
// /// display as on a low-resolution device.
// ///
// /// For similar reasons, pointer events are not affected by any transforms in
// /// the rendering layer. This means that deltas may need to be scaled before
// /// being applied to movement within the rendering. For example, if a scrolling
// /// list is shown scaled by 2x, the pointer deltas will have to be scaled by the
// /// inverse amount if the list is to appear to scroll with the user"s finger.
// ///
// /// See also:
// ///
// ///  * [Window.devicePixelRatio], which defines the device"s current resolution.
abstract class PointerEvent(
    // /// Time of event dispatch, relative to an arbitrary timeline.
    val timeStamp: Duration = Duration.zero,
    // /// Unique identifier for the pointer, not reused.
    val pointer: Int = 0,
    // /// The kind of input device for which the event was generated.
    val kind: PointerDeviceKind = PointerDeviceKind.touch,
    // /// Unique identifier for the pointing device, reused across interactions.
    val device: Int = 0,
    // /// Coordinate of the position of the pointer, in logical pixels in the global
    // /// coordinate space.
    val position: Offset = Offset.zero,
    // /// Distance in logical pixels that the pointer moved since the last
    // /// PointerMoveEvent. Always 0.0 for down, up, and cancel events.
    val delta: Offset = Offset.zero,
    // /// Bit field using the *Button constants (PRIMARY_MOUSE_BUTTON,
    // /// SECONDARY_STYLUS_BUTTON, etc). For example, if this has the value 6 and the
    // /// [kind] is [PointerDeviceKind.invertedStylus], then this indicates an
    // /// upside-down stylus with both its primary and secondary buttons pressed.
    val buttons: Int = 0,
    // /// Set if the pointer is currently down. For touch and stylus pointers, this
    // /// means the object (finger, pen) is in contact with the input surface. For
    // /// mice, it means a button is pressed.
    val down: Boolean = false,
    // /// Set if an application from a different security domain is in any way
    // /// obscuring this application"s window. (Aspirational; not currently
    // /// implemented.)
    val obscured: Boolean = false,
    // /// The pressure of the touch as a number ranging from 0.0, indicating a touch
    // /// with no discernible pressure, to 1.0, indicating a touch with "normal"
    // /// pressure, and possibly beyond, indicating a stronger touch. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0.
    val pressure: Double = 1.0,
    // /// The minimum value that [pressure] can return for this pointer. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0. This will always be
    // /// a number less than or equal to 1.0.
    val pressureMin: Double = 1.0,
    // /// The maximum value that [pressure] can return for this pointer. For devices
    // /// that do not detect pressure (e.g. mice), returns 1.0. This will always be
    // /// a greater than or equal to 1.0.
    val pressureMax: Double = 1.0,
    // /// The distance of the detected object from the input surface (e.g. the
    // /// distance of a stylus or finger from a touch screen), in arbitrary units on
    // /// an arbitrary (not necessarily linear) scale. If the pointer is down, this
    // /// is 0.0 by definition.
    val distance: Double = 0.0,
    // /// The maximum value that a distance can return for this pointer. If this
    // /// input device cannot detect "hover touch" input events, then this will be
    // /// 0.0.
    val distanceMax: Double = 0.0,
    // /// The radius of the contact ellipse along the major axis, in logical pixels.
    val radiusMajor: Double = 0.0,
    // /// The radius of the contact ellipse along the minor axis, in logical pixels.
    val radiusMinor: Double = 0.0,
    // /// The minimum value that could be reported for radiusMajor and radiusMinor
    // /// for this pointer, in logical pixels.
    val radiusMin: Double = 0.0,
    // /// The minimum value that could be reported for radiusMajor and radiusMinor
    // /// for this pointer, in logical pixels.
    val radiusMax: Double = 0.0,
    // /// For PointerDeviceKind.touch events:
    // ///
    // /// The angle of the contact ellipse, in radius in the range:
    // ///
    // ///    -pi/2 < orientation <= pi/2
    // ///
    // /// ...giving the angle of the major axis of the ellipse with the y-axis
    // /// (negative angles indicating an orientation along the top-left /
    // /// bottom-right diagonal, positive angles indicating an orientation along the
    // /// top-right / bottom-left diagonal, and zero indicating an orientation
    // /// parallel with the y-axis).
    // ///
    // /// For PointerDeviceKind.stylus and PointerDeviceKind.invertedStylus events:
    // ///
    // /// The angle of the stylus, in radians in the range:
    // ///
    // ///    -pi < orientation <= pi
    // ///
    // /// ...giving the angle of the axis of the stylus projected onto the input
    // /// surface, relative to the positive y-axis of that surface (thus 0.0
    // /// indicates the stylus, if projected onto that surface, would go from the
    // /// contact point vertically up in the positive y-axis direction, pi would
    // /// indicate that the stylus would go down in the negative y-axis direction;
    // /// pi/4 would indicate that the stylus goes up and to the right, -pi/2 would
    // /// indicate that the stylus goes to the left, etc).
    val orientation: Double = 0.0,
    // /// For PointerDeviceKind.stylus and PointerDeviceKind.invertedStylus events:
    // ///
    // /// The angle of the stylus, in radians in the range:
    // ///
    // ///    0 <= tilt <= pi/2
    // ///
    // /// ...giving the angle of the axis of the stylus, relative to the axis
    // /// perpendicular to the input surface (thus 0.0 indicates the stylus is
    // /// orthogonal to the plane of the input surface, while pi/2 indicates that
    // /// the stylus is flat on that surface).
    val tilt: Double = 0.0,
    // /// We occasionally synthesize PointerEvents that aren"t exact translations
    // /// of [ui.PointerData] from the engine to cover small cross-OS discrepancies
    // /// in pointer behaviors.
    // ///
    // /// For instance, on end events, Android always drops any location changes
    // /// that happened between its reporting intervals when emitting the end events.
    // ///
    // /// On iOS, minor incorrect location changes from the previous move events
    // /// can be reported on end events. We synthesize a [PointerEvent] to cover
    // /// the difference between the 2 events in that case.
    val synthesized: Boolean = false
) {
    // /// The minimum value that a distance can return for this pointer (always 0.0).
    val distanceMin: Double = 0.0

    override fun toString(): String = "${this.javaClass.canonicalName}($position)"

    // /// Returns a complete textual description of this event.
    fun toStringFull(): String =
        "${this.javaClass.canonicalName}(" +
                "timeStamp: $timeStamp, " +
                "pointer: $pointer, " +
                "kind: $kind, " +
                "device: $device, " +
                "position: $position, " +
                "delta: $delta, " +
                "buttons: $buttons, " +
                "down: $down, " +
                "obscured: $obscured, " +
                "pressure: $pressure, " +
                "pressureMin: $pressureMin, " +
                "pressureMax: $pressureMax, " +
                "distance: $distance, " +
                "distanceMin: $distanceMin, " +
                "distanceMax: $distanceMax, " +
                "radiusMajor: $radiusMajor, " +
                "radiusMinor: $radiusMinor, " +
                "radiusMin: $radiusMin, " +
                "radiusMax: $radiusMax, " +
                "orientation: $orientation, " +
                "tilt: $tilt, " +
                "synthesized: $synthesized" +
                ")"
}
