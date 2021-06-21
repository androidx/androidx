/*
 * Copyright (C) 2021 The Android Open Source Project
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

/**
 * Hide the perfetto.protos package, as it's an implementation detail of benchmark.macro
 *
 * Note: other attempts to use these protos in the macrobench process will clash with our
 * definitions. If this becomes an issue, we can move ours to a separate, internal package.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
package perfetto.protos;

import androidx.annotation.RestrictTo;
