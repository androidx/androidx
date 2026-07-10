/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.integration.view.demos.examples;

import static androidx.compose.remote.core.RcProfiles.PROFILE_ANDROIDX;
import static androidx.compose.remote.core.RcProfiles.PROFILE_EXPERIMENTAL;

import android.annotation.SuppressLint;

import androidx.compose.remote.creation.RemoteComposeWriterAndroid;
import androidx.compose.remote.creation.RemotePath;
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices;

import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Random;

/**
 * A collection of utilities for the demos
 */
public class ExampleUtils {
    private ExampleUtils() {
    }

    /**
     * get the writer for the demos
     *
     * @param api the API level
     * @return RemoteComposeWriterAndroid
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull RemoteComposeWriterAndroid getWriter(int api) {
        return new RemoteComposeWriterAndroid(500, 500, "sd",
                api, PROFILE_ANDROIDX | PROFILE_EXPERIMENTAL,
                new AndroidxRcPlatformServices());
    }


    public static class Maze {
        private Maze() {
        }

        public static int MASK_LEFT = 1;
        public static int MASK_RIGHT = 2;
        public static int MASK_TOP = 4;
        public static int MASK_BOTTOM = 8;

        /**
         * generate a maze
         *
         * @param w    width of maze
         * @param h    height of maze
         * @param seed random seed
         * @return the maze as a byte array bits 1111 represent walls
         */
        public static byte @NonNull [] genMaze(int w, int h, long seed) {
            int leftId = 1;
            int rightId = 2;
            int topId = 4;
            int bottomId = 8;
            int[] dirTable = {-1, 1, -w, w};
            int[] dirBit = {leftId, rightId, topId, bottomId};
            int[] op_dirBit = {rightId, leftId, bottomId, topId};
            int visited = 16;
            int size = w * h;
            byte[] maze = new byte[size];
            Arrays.fill(maze, 0, size, (byte) (leftId | rightId | topId | bottomId));
            for (int x = 0; x < w; x++) {
                maze[x] &= (byte) ~topId;
                maze[x + (h - 1) * w] &= (byte) ~bottomId;
            }
            for (int y = 0; y < h; y++) {
                maze[y * w] &= (byte) ~leftId;
                maze[y * w + (w - 1)] &= (byte) ~rightId;
            }
            int[] surface = new int[size];
            int count = 0;
            Random r = new Random(seed);
            int start = r.nextInt(size);
            surface[count] = start;
            maze[start] |= (byte) visited;
            count++;
            while (count < size) {
                int point = r.nextInt(count * 4);
                int dir = point % 4;
                point /= 4;
                int s = surface[point];
                if ((maze[s] & dirBit[dir]) == 0) {
                    continue;
                }
                int neighbour = s + dirTable[dir];
                if ((maze[neighbour] & visited) != 0) {
                    continue;
                }
                maze[neighbour] |= (byte) visited;
                maze[neighbour] &= (byte) ~op_dirBit[dir];
                maze[s] &= (byte) ~dirBit[dir];
                surface[count] = neighbour;
                count++;

            }
            for (int x = 0; x < w; x++) {
                maze[x] |= (byte) topId;
                maze[x + (h - 1) * w] |= (byte) bottomId;
            }
            for (int y = 0; y < h; y++) {
                maze[y * w] |= (byte) leftId;
                maze[y * w + (w - 1)] |= (byte) rightId;
            }
            return maze;
        }

        /**
         * a breath first search from begin to end
         * for building a paths
         *
         * @param map   maze as a byte array
         * @param w     width of maze
         * @param h     height of maze
         * @param start starting point
         * @param end   ending point
         * @return the path as an int array
         */
        public static int @NonNull [] calculatePath(byte @NonNull [] map, int w, int h, int start,
                int end) {
            int leftId = 1;
            int rightId = 2;
            int topId = 4;
            int bottomId = 8;
            int fillVal = 32;
            int[] que = new int[w * h];
            int qstart = 0;
            int qend = 0;
            que[qend] = start;
            map[start] |= (byte) fillVal;
            int[] path = new int[w * h];

            qend++;
            while (qend > qstart) {
                int point = que[qstart];
                qstart++;
                byte mp = map[point];
                if ((mp & leftId) == 0) {
                    int left = point - 1;
                    if ((map[left] & fillVal) == 0) {
                        map[left] |= (byte) fillVal;
                        que[qend] = left;
                        path[left] = point;
                        qend++;
                        if (left == end) break;
                    }
                }
                if ((mp & rightId) == 0) {
                    int right = point + 1;
                    if ((map[right] & fillVal) == 0) {
                        map[right] |= (byte) fillVal;
                        que[qend] = right;
                        path[right] = point;
                        qend++;
                        if (right == end) break;

                    }
                }
                if ((mp & topId) == 0) {
                    int up = point - w;
                    if ((map[up] & fillVal) == 0) {
                        map[up] |= (byte) fillVal;
                        que[qend] = up;
                        path[up] = point;
                        qend++;
                        if (up == end) break;

                    }
                }
                if ((mp & bottomId) == 0) {
                    int down = point + w;
                    if ((map[down] & fillVal) == 0) {
                        map[down] |= (byte) fillVal;
                        que[qend] = down;
                        path[down] = point;
                        qend++;
                        if (down == end) break;
                    }
                }
            }
            int count = 1;
            for (int p = end; p != start; p = path[p]) {
                count++;
            }
            int[] ret = new int[count];
            for (int p = end; p != start; p = path[p]) {
                count--;
                ret[count] = p;
            }
            ret[0] = start;
            return ret;
        }

        /**
         * distance from path map
         * for building a paths
         *
         * @param map   maze as a byte array
         * @param w     width of maze
         * @param h     height of maze
         * @param trail the path
         * @return the distance from path as a byte array 0 == on path
         */
        public static byte @NonNull [] distanceFromPathMap(byte @NonNull [] map, int w, int h,
                int @NonNull [] trail) {
            int leftId = 1;
            int rightId = 2;
            int topId = 4;
            int bottomId = 8;
            int fillId = 32;
            int qSize = w * h;
            int[] que = new int[qSize];
            byte[] ret = new byte[w * h];
            Arrays.fill(ret, (byte) 255);
            int qstart = 0;
            int qend = 0;
            // clear out the fill marker
            for (int i = 0; i < map.length; i++) {
                map[i] &= (byte) ~fillId;
            }
            //enqueue the current path
            for (int i = 0; i < trail.length; i++) {
                que[qend] = trail[i];
                ret[trail[i]] = 0;
                qend++;
            }


            qend++;
            while (qend > qstart) {
                int point = que[qstart % qSize];
                qstart++;
                int dist = (0xff & ret[point]) + 1;
                if ((map[point] & leftId) == 0) {
                    int left = point - 1;
                    if ((map[left] & fillId) == 0 && ((ret[left] & 0xFF) > dist)) {
                        map[left] |= (byte) fillId;
                        que[qend % qSize] = left;
                        ret[left] = (byte) dist;
                        qend++;
                    }
                }
                if ((map[point] & rightId) == 0) {
                    int right = point + 1;

                    if ((map[right] & fillId) == 0 && ((ret[right] & 0xFF) > dist)) {
                        map[right] |= (byte) fillId;
                        que[qend % qSize] = right;
                        ret[right] = (byte) dist;
                        //System.out.println(dist +" = "+ (0xFF&ret[right]));
                        qend++;
                    }
                }
                if ((map[point] & topId) == 0) {
                    int up = point - w;
                    if ((map[up] & fillId) == 0 && ((ret[up] & 0xFF) > dist)) {
                        map[up] |= (byte) fillId;
                        que[qend % qSize] = up;
                        ret[up] = (byte) dist;
                        qend++;
                    }
                }
                if ((map[point] & bottomId) == 0) {
                    int down = point + w;
                    if ((map[down] & fillId) == 0 && ((ret[down] & 0xFF) > dist)) {
                        map[down] |= (byte) fillId;
                        que[qend % qSize] = down;
                        ret[down] = (byte) dist;
                        qend++;
                    }
                }
            }

            return ret;
        }

        /**
         * generate a path from a maze
         *
         * @param maze maze as a byte array
         * @param mw   width of maze
         * @param mh   height of maze
         * @return the path as a RemotePath
         */
        @SuppressLint("RestrictedApiAndroidX")
        public static @NonNull RemotePath genPath(byte @NonNull [] maze, int mw, int mh) {
            int w = 1000;
            int h = 1000;

            float wx = w / (float) mw;
            float wy = h / (float) mh;

            RemotePath path = new RemotePath();
            for (int y = 0; y < mh; y++) {
                for (int x = 0; x < mw; x++) {
                    int p = x + mw * y;
                    int m = maze[p];
                    boolean left = (m & 1) != 0;
                    boolean right = (m & 2) != 0;
                    boolean top = (m & 4) != 0;
                    boolean bottom = (m & 8) != 0;
                    float px = x * wx;
                    float py = y * wy;

                    if (left) {
                        path.moveTo(px, py);
                        path.lineTo(px, py + wy);
                    }
                    if (right) {
                        path.moveTo(px + wx, py);
                        path.lineTo(px + wx, py + wy);
                    }
                    if (bottom) {
                        path.moveTo(px, py + wy);
                        path.lineTo(px + wx, py + wy);
                    }
                    if (top) {
                        path.moveTo(px, py);
                        path.lineTo(px + wx, py);

                    }
                }
            }
            return path;
        }


        /**
         * generate walls from a maze
         *
         * @param maze maze as a byte array
         * @param mw   width of maze
         * @param mh   height of maze
         * @return the walls coded as the position of the walls left above right below
         */
        public static float @NonNull [][] genWalls(byte @NonNull [] maze, int mw, int mh) {
            int w = 1000;
            int h = 1000;
            float[][] wall = new float[4][mw * mh];

            float wx = w / (float) mw;
            float wy = h / (float) mh;

            for (int y = 0; y < mh; y++) {
                for (int x = 0; x < mw; x++) {
                    int p = x + mw * y;
                    int m = maze[p];
                    boolean left = (m & 1) != 0;
                    boolean right = (m & 2) != 0;
                    boolean top = (m & 4) != 0;
                    boolean bottom = (m & 8) != 0;
                    float px = x * wx;
                    float py = y * wy;
                    wall[0][p] = left ? px : wall[0][p - 1];
                    wall[1][p] = right ? px + wx : Float.NaN;
                    wall[2][p] = bottom ? py + wy : Float.NaN;
                    wall[3][p] = top ? py : wall[3][p - mw];
                }
            }
            for (int y = 0; y < mh; y++) {
                for (int x = mw - 2; x >= 0; x--) {
                    int p = x + mw * y;
                    if (Float.isNaN(wall[1][p])) {
                        wall[1][p] = wall[1][p + 1];
                    }
                }
            }
            for (int y = mh - 2; y >= 0; y--) {
                for (int x = 0; x < mw; x++) {
                    int p = x + mw * y;
                    if (Float.isNaN(wall[2][p])) {
                        wall[2][p] = wall[2][p + mw];
                    }
                }
            }

            return wall;
        }
    }

}
