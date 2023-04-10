/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.constraintlayout.core.scout;

/**
 * Possible directions for a connection
 */
public enum Direction {
    NORTH(0),
    SOUTH(1),
    WEST(2),
    EAST(3),
    BASE(4);
    private final int mDirection;

    static final int ORIENTATION_VERTICAL = 0;
    static final int ORIENTATION_HORIZONTAL = 1;

    private static Direction[] sAllDirections = Direction.values();
    private static Direction[] sVertical = {NORTH, SOUTH, BASE};
    private static Direction[] sHorizontal = {WEST, EAST};

    Direction(int n) {
        mDirection = n;
    }

    /**
     * Get an array of all directions
     *
     * @return array of all directions
     */
    static Direction[] getAllDirections() {
        return sAllDirections;
    }

    /**
     * get a String representing the direction integer
     *
     * @param directionInteger direction as an integer
     * @return single letter string to describe the direction
     */
    static String toString(int directionInteger) {
        return Direction.get(directionInteger).toString();
    }

    @Override
    public String toString() {
        switch (this) {
            case NORTH:
                return "N";
            case SOUTH:
                return "S";
            case EAST:
                return "E";
            case WEST:
                return "W";
            case BASE:
                return "B";
        }
        return "?";
    }

    /**
     * get the direction as an integer
     *
     * @return direction as an integer
     */
    int getDirection() {
        return mDirection;
    }

    /**
     * gets the opposite direction
     *
     * @return the opposite direction
     */
    Direction getOpposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case SOUTH:
                return NORTH;
            case EAST:
                return WEST;
            case WEST:
                return EAST;
            case BASE:
                return BASE;
            default:
                return BASE;
        }
    }

    /**
     * convert from an ordinal of direction to actual direction
     *
     * @return Enum member equivalent to integer
     */
    static Direction get(int directionInteger) {
        return sAllDirections[directionInteger];
    }

    /**
     * Directions can be a positive or negative (right and down) being positive
     * reverse indicates the direction is negative
     *
     * @return true for north and east
     */
    boolean reverse() {
        return (this == NORTH || this == WEST);
    }

    /**
     * gets the viable directions for horizontal or vertical
     *
     * @param orientation 0 = vertical 1 = horizontal
     * @return array of directions for vertical or horizontal
     */
    static Direction[] getDirections(int orientation) {
        if (orientation == ORIENTATION_VERTICAL) {
            return sVertical;
        }
        return sHorizontal;
    }

    /**
     * Return the number of connection types support by this direction
     *
     * @return number of types allowed for this connection
     */
    public int connectTypes() {
        switch (this) {
            case NORTH:
            case SOUTH:
                return 2;
            case EAST:
            case WEST:
                return 2;
            case BASE:
                return 1;
        }
        return 1;
    }
}
