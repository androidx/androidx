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

package androidx.browser.trusted;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents screenOrientationLock type value of a Trusted Web Activity:
 * https://www.w3.org/TR/screen-orientation/#screenorientation-interface
 */
public final class ScreenOrientation {

    private ScreenOrientation() {}

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            DEFAULT, PORTRAIT_PRIMARY, PORTRAIT_SECONDARY, LANDSCAPE_PRIMARY, LANDSCAPE_SECONDARY,
            ANY, LANDSCAPE, PORTRAIT, NATURAL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface LockType {}

    /**
     * The default screen orientation is the set of orientations to which the screen is locked when
     * there is no current orientation lock.
     */
    public static final int DEFAULT = 0;

    /**
     *  Portrait-primary is an orientation where the screen width is less than or equal to the
     *  screen height. If the device's natural orientation is portrait, then it is in
     *  portrait-primary when held in that position.
     */
    public static final int PORTRAIT_PRIMARY = 1;

    /**
     * Portrait-secondary is an orientation where the screen width is less than or equal to the
     * screen height. If the device's natural orientation is portrait, then it is in
     * portrait-secondary when rotated 180° from its natural position.
     */
    public static final int PORTRAIT_SECONDARY = 2;

    /**
     * Landscape-primary is an orientation where the screen width is greater than the screen height.
     * If the device's natural orientation is landscape, then it is in landscape-primary when held
     * in that position.
     */
    public static final int LANDSCAPE_PRIMARY = 3;

    /**
     * Landscape-secondary is an orientation where the screen width is greater than the
     * screen height. If the device's natural orientation is landscape, it is in
     * landscape-secondary when rotated 180° from its natural orientation.
     */
    public static final int LANDSCAPE_SECONDARY = 4;

    /**
     * Any is an orientation that means the screen can be locked to any one of portrait-primary,
     * portrait-secondary, landscape-primary and landscape-secondary.
     */
    public static final int ANY = 5;

    /**
     * Landscape is an orientation where the screen width is greater than the screen height and
     * depending on platform convention locking the screen to landscape can represent
     * landscape-primary, landscape-secondary or both.
     */
    public static final int LANDSCAPE = 6;

    /**
     * Portrait is an orientation where the screen width is less than or equal to the screen height
     * and depending on platform convention locking the screen to portrait can represent
     * portrait-primary, portrait-secondary or both.
     */
    public static final int PORTRAIT = 7;

    /**
     * Natural is an orientation that refers to either portrait-primary or landscape-primary
     * depending on the device's usual orientation. This orientation is usually provided by
     * the underlying operating system.
     */
    public static final int NATURAL = 8;
}
