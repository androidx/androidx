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
    HAND_JOINT_TYPE_PALM,
    /** The wrist joint, where the hand connects to the forearm. */
    HAND_JOINT_TYPE_WRIST,

    // Thumb Joints
    /** The base of the thumb (the first joint connecting the thumb to the palm). */
    HAND_JOINT_TYPE_THUMB_METACARPAL,
    /** The second joint of the thumb, closer to the palm. */
    HAND_JOINT_TYPE_THUMB_PROXIMAL,
    /** The third joint of the thumb, further from the palm. */
    HAND_JOINT_TYPE_THUMB_DISTAL,
    /** The tip of the thumb. */
    HAND_JOINT_TYPE_THUMB_TIP,

    // Index Finger Joints
    /** The base of the index finger, connecting it to the hand. */
    HAND_JOINT_TYPE_INDEX_METACARPAL,
    /** The first joint of the index finger, closer to the palm. */
    HAND_JOINT_TYPE_INDEX_PROXIMAL,
    /** The second joint of the index finger, between the proximal and distal joints. */
    HAND_JOINT_TYPE_INDEX_INTERMEDIATE,
    /** The third joint of the index finger, closest to the fingertip. */
    HAND_JOINT_TYPE_INDEX_DISTAL,
    /** The tip of the index finger. */
    HAND_JOINT_TYPE_INDEX_TIP,

    // Middle Finger Joints
    /** The base of the middle finger, connecting it to the hand. */
    HAND_JOINT_TYPE_MIDDLE_METACARPAL,
    /** The first joint of the middle finger, closer to the palm. */
    HAND_JOINT_TYPE_MIDDLE_PROXIMAL,
    /** The second joint of the middle finger, between the proximal and distal joints. */
    HAND_JOINT_TYPE_MIDDLE_INTERMEDIATE,
    /** The third joint of the middle finger, closest to the fingertip. */
    HAND_JOINT_TYPE_MIDDLE_DISTAL,
    /** The tip of the middle finger. */
    HAND_JOINT_TYPE_MIDDLE_TIP,

    // Ring Finger Joints
    /** The base of the ring finger, connecting it to the hand. */
    HAND_JOINT_TYPE_RING_METACARPAL,
    /** The first joint of the ring finger, closer to the palm. */
    HAND_JOINT_TYPE_RING_PROXIMAL,
    /** The second joint of the ring finger, between the proximal and distal joints. */
    HAND_JOINT_TYPE_RING_INTERMEDIATE,
    /** The third joint of the ring finger, closest to the fingertip. */
    HAND_JOINT_TYPE_RING_DISTAL,
    /** The tip of the ring finger. */
    HAND_JOINT_TYPE_RING_TIP,

    // Little Finger (Pinky) Joints
    /** The base of the little finger (pinky), connecting it to the hand. */
    HAND_JOINT_TYPE_LITTLE_METACARPAL,
    /** The first joint of the little finger, closer to the palm. */
    HAND_JOINT_TYPE_LITTLE_PROXIMAL,
    /** The second joint of the little finger, between the proximal and distal joints. */
    HAND_JOINT_TYPE_LITTLE_INTERMEDIATE,
    /** The third joint of the little finger, closest to the fingertip. */
    HAND_JOINT_TYPE_LITTLE_DISTAL,
    /** The tip of the little finger (pinky). */
    HAND_JOINT_TYPE_LITTLE_TIP,
}
