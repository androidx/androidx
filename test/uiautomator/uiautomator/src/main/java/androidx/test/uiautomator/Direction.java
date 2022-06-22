/*
 * Copyright (C) 2014 The Android Open Source Project
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

package androidx.test.uiautomator;

/** An enumeration used to specify the primary direction of certain gestures. */
@SuppressWarnings("ImmutableEnumChecker")
public enum Direction {
    LEFT, RIGHT, UP, DOWN;

    private Direction mOpposite;
    static {
        LEFT.mOpposite = RIGHT;
        RIGHT.mOpposite = LEFT;
        UP.mOpposite = DOWN;
        DOWN.mOpposite = UP;
    }

    /** Returns the reverse of the given direction. */
    public static Direction reverse(Direction direction) {
        return direction.mOpposite;
    }
}
