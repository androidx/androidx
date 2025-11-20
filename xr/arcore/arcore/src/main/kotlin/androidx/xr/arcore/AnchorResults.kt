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

/** Result of a [Anchor.create] or [Anchor.load] call. */
public sealed class AnchorCreateResult

/**
 * Result of a successful [Anchor.create] or [Anchor.load] call.
 *
 * @property anchor the [Anchor] that was created.
 */
public class AnchorCreateSuccess(public val anchor: Anchor) : AnchorCreateResult()

/**
 * Result of an unsuccessful [Anchor.create] or [Anchor.load] call. The resources allocated for
 * anchors has been exhausted.
 */
public class AnchorCreateResourcesExhausted() : AnchorCreateResult()

/** Result of an unsuccessful [Anchor.create] call. Required tracking is not available. */
public class AnchorCreateTrackingUnavailable() : AnchorCreateResult()

/** Result of an unsuccessful [Anchor.load] call. The anchor was loaded from an invalid UUID. */
public class AnchorLoadInvalidUuid() : AnchorCreateResult()

/**
 * Result of an unsuccessful [Anchor.create] call. The anchor create call was made when the session
 * state was invalid.
 */
public class AnchorCreateIllegalState() : AnchorCreateResult()

/**
 * Result of an unsuccessful [Anchor.create] call. The anchor was not created due to an
 * authorization error.
 */
public class AnchorCreateNotAuthorized() : AnchorCreateResult()

/**
 * Result of an unsuccessful [Anchor.create] call. The anchor was not created due to an unsupported
 * location.
 */
public class AnchorCreateUnsupportedLocation() : AnchorCreateResult()
