/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

/** Constants defining the limits of the RemoteCompose player. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class Limits {
    /** Maximum number of operations processed per frame */
    public static int MAX_OP_COUNT = 20_000;

    /** Default initial size of the wire buffer */
    public static final int BUFFER_SIZE = 1024 * 1024;

    /** Maximum number of entries in the ID lookup table (bitmaps, fonts, etc.) */
    public static final int MAX_TABLE_SIZE = 1000;

    /** Maximum number of entries in a data map */
    public static final int MAX_DATA_MAP_SIZE = 2000;

    /** Maximum number of state variables tracked */
    public static final int MAX_STATE_DATA = 10000;

    /** Maximum size of UTF-8 strings in bytes */
    public static final int MAX_STRING_SIZE = 4000;

    /** Maximum dimension (width or height) of an image */
    public static int MAX_IMAGE_DIMENSION = 8000;

    /** Maximum memory allowed for bitmaps in a single player instance (in bytes) */
    public static int MAX_BITMAP_MEMORY = 20 * 1024 * 1024;

    /** Default maximum frames per second for the player */
    public static int DEFAULT_MAX_FPS = 60;

    /** Absolute maximum frames per second for the player */
    public static int MAX_FPS = 120;

    /** Maximum length of RPN expressions (number of operations) */
    public static final int MAX_EXPRESSION_SIZE = 32;

    /** Maximum number of floats in a shader data packet */
    public static final int MAX_SHADER_FLOAT_COUNT = 200;

    /** Maximum size of float arrays in particle systems */
    public static final int MAX_PARTICLE_FLOAT_ARRAY_SIZE = 2000;

    /** Maximum number of arguments for remote functions */
    public static final int MAX_FUNCTION_ARGUMENTS = 32;

    /** Maximum number of cached items in player-side LRU caches */
    public static final int MAX_CACHE_ENTRIES = 20;

    /** Enable the player to generate haptic feedback */
    public static final boolean ENABLE_HAPTIC_FEEDBACK = true;

    /** Enable the player to support Image URLs */
    public static final boolean ENABLE_IMAGE_URLS = true;

    private Limits() {}
}
