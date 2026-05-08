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

package androidx.xr.arcore

/** Represents the type of hand joint. */
public enum class HandJointType {
    /** The center of the palm. Often used as a reference point for hand tracking. */
    PALM,
    /** The wrist joint, where the hand connects to the forearm. */
    WRIST,

    // Thumb Joints
    /** The base of the thumb (the first joint connecting the thumb to the palm). */
    THUMB_METACARPAL,
    /** The second joint of the thumb, closer to the palm. */
    THUMB_PROXIMAL,
    /** The third joint of the thumb, further from the palm. */
    THUMB_DISTAL,
    /** The tip of the thumb. */
    THUMB_TIP,

    // Index Finger Joints
    /** The base of the index finger, connecting it to the hand. */
    INDEX_METACARPAL,
    /** The first joint of the index finger, closer to the palm. */
    INDEX_PROXIMAL,
    /** The second joint of the index finger, between the proximal and distal joints. */
    INDEX_INTERMEDIATE,
    /** The third joint of the index finger, closest to the fingertip. */
    INDEX_DISTAL,
    /** The tip of the index finger. */
    INDEX_TIP,

    // Middle Finger Joints
    /** The base of the middle finger, connecting it to the hand. */
    MIDDLE_METACARPAL,
    /** The first joint of the middle finger, closer to the palm. */
    MIDDLE_PROXIMAL,
    /** The second joint of the middle finger, between the proximal and distal joints. */
    MIDDLE_INTERMEDIATE,
    /** The third joint of the middle finger, closest to the fingertip. */
    MIDDLE_DISTAL,
    /** The tip of the middle finger. */
    MIDDLE_TIP,

    // Ring Finger Joints
    /** The base of the ring finger, connecting it to the hand. */
    RING_METACARPAL,
    /** The first joint of the ring finger, closer to the palm. */
    RING_PROXIMAL,
    /** The second joint of the ring finger, between the proximal and distal joints. */
    RING_INTERMEDIATE,
    /** The third joint of the ring finger, closest to the fingertip. */
    RING_DISTAL,
    /** The tip of the ring finger. */
    RING_TIP,

    // Little Finger (Pinky) Joints
    /** The base of the little finger (pinky), connecting it to the hand. */
    LITTLE_METACARPAL,
    /** The first joint of the little finger, closer to the palm. */
    LITTLE_PROXIMAL,
    /** The second joint of the little finger, between the proximal and distal joints. */
    LITTLE_INTERMEDIATE,
    /** The third joint of the little finger, closest to the fingertip. */
    LITTLE_DISTAL,
    /** The tip of the little finger (pinky). */
    LITTLE_TIP,
}
