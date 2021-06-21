/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles;

import androidx.annotation.RequiresOptIn;
import androidx.annotation.RequiresOptIn.Level;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Denotes that this API surface is experimental. It may change without warning, and it may not
 * render correctly on all tile renderers.
 */
@RequiresOptIn(level = Level.ERROR)
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD})
public @interface TilesExperimental {}
