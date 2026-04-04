/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.arcore

/** Result of an attempt to create an anchor. */
public sealed class AnchorResult

/**
 * Result of a successful attempt to create an anchor.
 *
 * @property anchor the [Anchor] that was created
 */
public class AnchorCreateSuccess(public val anchor: Anchor) : AnchorResult()

/**
 * Result of an unsuccessful attempt to create an [Anchor].
 *
 * The resources allocated for anchors has been exhausted.
 */
public class AnchorCreateResourcesExhausted : AnchorResult()

/**
 * Result of an unsuccessful attempt to create an [Anchor].
 *
 * Required tracking is not available.
 */
public class AnchorCreateTrackingUnavailable : AnchorResult()

// Prevent exhaustive when by consumers to allow for future extensions of [AnchorResult].
private class AnchorResultHidden() : AnchorResult()
