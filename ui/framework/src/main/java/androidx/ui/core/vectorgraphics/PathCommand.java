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
package androidx.ui.core.vectorgraphics;

// TODO njawad refactor in Kotlin enum once b/128539891 is resolved
public enum PathCommand {

    RelativeClose(Keys.RelativeCloseKey),
    Close(Keys.CloseKey),
    RelativeMoveTo(Keys.RelativeMoveToKey),
    MoveTo(Keys.MoveToKey),
    RelativeLineTo(Keys.RelativeLineToKey),
    LineTo(Keys.LineToKey),
    RelativeHorizontalTo(Keys.RelativeHorizontalToKey),
    HorizontalLineTo(Keys.HorizontalLineToKey),
    RelativeVerticalTo(Keys.RelativeVerticalToKey),
    VerticalLineTo(Keys.VerticalLineToKey),
    RelativeCurveTo(Keys.RelativeCurveToKey),
    CurveTo(Keys.CurveToKey),
    RelativeReflectiveCurveTo(Keys.RelativeReflectiveCurveToKey),
    ReflectiveCurveTo(Keys.ReflectiveCurveToKey),
    RelativeQuadTo(Keys.RelativeQuadToKey),
    QuadTo(Keys.QuadToKey),
    RelativeReflectiveQuadTo(Keys.RelativeReflectiveQuadToKey),
    ReflectiveQuadTo(Keys.ReflectiveQuadToKey),
    RelativeArcTo(Keys.RelativeArcToKey),
    ArcTo(Keys.ArcToKey);

    private final char mKey;

    PathCommand(char key) {
        mKey = key;
    }

    /**
     * Return the serialized key that represents this path command as a character
     */
    public char toKey() {
        return mKey;
    }

    /**
     * Return the corresponding PathCommand for the given character key if it exists.
     * If the key is unknown then amn UnknownPathCommandException is thrown
     * @param key key representing a specific PathCommand
     * @return PathCommand that matches the provided key
     * @throws IllegalArgumentException if the key is invalid
     */
    public static PathCommand pathCommandFromKey(char key) throws IllegalArgumentException {
        switch (key) {
            case Keys.RelativeCloseKey: return RelativeClose;
            case Keys.CloseKey: return Close;
            case Keys.RelativeMoveToKey: return RelativeMoveTo;
            case Keys.MoveToKey: return MoveTo;
            case Keys.RelativeLineToKey: return RelativeLineTo;
            case Keys.LineToKey: return LineTo;
            case Keys.RelativeHorizontalToKey: return RelativeHorizontalTo;
            case Keys.HorizontalLineToKey: return HorizontalLineTo;
            case Keys.RelativeVerticalToKey: return RelativeVerticalTo;
            case Keys.VerticalLineToKey: return VerticalLineTo;
            case Keys.RelativeCurveToKey: return RelativeCurveTo;
            case Keys.CurveToKey: return CurveTo;
            case Keys.RelativeReflectiveCurveToKey: return RelativeReflectiveCurveTo;
            case Keys.ReflectiveCurveToKey: return ReflectiveCurveTo;
            case Keys.RelativeQuadToKey: return RelativeQuadTo;
            case Keys.QuadToKey: return QuadTo;
            case Keys.RelativeReflectiveQuadToKey: return RelativeReflectiveQuadTo;
            case Keys.ReflectiveQuadToKey: return ReflectiveQuadTo;
            case Keys.RelativeArcToKey: return RelativeArcTo;
            case Keys.ArcToKey: return ArcTo;
            default: throw new IllegalArgumentException("Unknown command for: " + key);
        }
    }

    /**
     * Compile time character constants to support exhaustive switch statements and reuse
     * of values between the enum definition and the pathCommandFromKey method
     *
     * Defined as a static inner class to avoid forward definition errors
     */
    private static final class Keys {
        private static final char RelativeCloseKey = 'z';
        private static final char CloseKey = 'Z';
        private static final char RelativeMoveToKey = 'm';
        private static final char MoveToKey = 'M';
        private static final char RelativeLineToKey = 'l';
        private static final char LineToKey = 'L';
        private static final char RelativeHorizontalToKey = 'h';
        private static final char HorizontalLineToKey = 'H';
        private static final char RelativeVerticalToKey = 'v';
        private static final char VerticalLineToKey = 'V';
        private static final char RelativeCurveToKey = 'c';
        private static final char CurveToKey = 'C';
        private static final char RelativeReflectiveCurveToKey = 's';
        private static final char ReflectiveCurveToKey = 'S';
        private static final char RelativeQuadToKey = 'q';
        private static final char QuadToKey = 'Q';
        private static final char RelativeReflectiveQuadToKey = 't';
        private static final char ReflectiveQuadToKey = 'T';
        private static final char RelativeArcToKey = 'a';
        private static final char ArcToKey = 'A';
    }
}
