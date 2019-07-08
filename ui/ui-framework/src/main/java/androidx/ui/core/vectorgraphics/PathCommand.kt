/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.vectorgraphics

enum class PathCommand(private val mKey: Char) {

    RelativeClose(RelativeCloseKey),
    Close(CloseKey),
    RelativeMoveTo(RelativeMoveToKey),
    MoveTo(MoveToKey),
    RelativeLineTo(RelativeLineToKey),
    LineTo(LineToKey),
    RelativeHorizontalTo(RelativeHorizontalToKey),
    HorizontalLineTo(HorizontalLineToKey),
    RelativeVerticalTo(RelativeVerticalToKey),
    VerticalLineTo(VerticalLineToKey),
    RelativeCurveTo(RelativeCurveToKey),
    CurveTo(CurveToKey),
    RelativeReflectiveCurveTo(RelativeReflectiveCurveToKey),
    ReflectiveCurveTo(ReflectiveCurveToKey),
    RelativeQuadTo(RelativeQuadToKey),
    QuadTo(QuadToKey),
    RelativeReflectiveQuadTo(RelativeReflectiveQuadToKey),
    ReflectiveQuadTo(ReflectiveQuadToKey),
    RelativeArcTo(RelativeArcToKey),
    ArcTo(ArcToKey);

    /**
     * Return the serialized key that represents this path command as a character
     */
    fun toKey(): Char = mKey
}

/**
 * Return the corresponding PathCommand for the given character key if it exists.
 * If the key is unknown then IllegalArgumentException is thrown
 * @return PathCommand that matches the key
 * @throws IllegalArgumentException if the key is invalid
 */
@Throws(IllegalArgumentException::class)
fun Char.toPathCommand(): PathCommand = when (this) {
    RelativeCloseKey -> PathCommand.RelativeClose
    CloseKey -> PathCommand.Close
    RelativeMoveToKey -> PathCommand.RelativeMoveTo
    MoveToKey -> PathCommand.MoveTo
    RelativeLineToKey -> PathCommand.RelativeLineTo
    LineToKey -> PathCommand.LineTo
    RelativeHorizontalToKey -> PathCommand.RelativeHorizontalTo
    HorizontalLineToKey -> PathCommand.HorizontalLineTo
    RelativeVerticalToKey -> PathCommand.RelativeVerticalTo
    VerticalLineToKey -> PathCommand.VerticalLineTo
    RelativeCurveToKey -> PathCommand.RelativeCurveTo
    CurveToKey -> PathCommand.CurveTo
    RelativeReflectiveCurveToKey -> PathCommand.RelativeReflectiveCurveTo
    ReflectiveCurveToKey -> PathCommand.ReflectiveCurveTo
    RelativeQuadToKey -> PathCommand.RelativeQuadTo
    QuadToKey -> PathCommand.QuadTo
    RelativeReflectiveQuadToKey -> PathCommand.RelativeReflectiveQuadTo
    ReflectiveQuadToKey -> PathCommand.ReflectiveQuadTo
    RelativeArcToKey -> PathCommand.RelativeArcTo
    ArcToKey -> PathCommand.ArcTo
    else -> throw IllegalArgumentException("Unknown command for: $this")
}

/**
 * Compile time character constants to support exhaustive switch statements and reuse
 * of values between the enum definition and the pathCommandFromKey method
 */
private const val RelativeCloseKey = 'z'
private const val CloseKey = 'Z'
private const val RelativeMoveToKey = 'm'
private const val MoveToKey = 'M'
private const val RelativeLineToKey = 'l'
private const val LineToKey = 'L'
private const val RelativeHorizontalToKey = 'h'
private const val HorizontalLineToKey = 'H'
private const val RelativeVerticalToKey = 'v'
private const val VerticalLineToKey = 'V'
private const val RelativeCurveToKey = 'c'
private const val CurveToKey = 'C'
private const val RelativeReflectiveCurveToKey = 's'
private const val ReflectiveCurveToKey = 'S'
private const val RelativeQuadToKey = 'q'
private const val QuadToKey = 'Q'
private const val RelativeReflectiveQuadToKey = 't'
private const val ReflectiveQuadToKey = 'T'
private const val RelativeArcToKey = 'a'
private const val ArcToKey = 'A'
