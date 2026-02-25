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

package androidx.xr.arcore.samples

import androidx.annotation.Sampled
import androidx.lifecycle.Lifecycle
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @param session the [Session] to get the hand from
 * @param lifecycle the [Lifecycle] to scope the coroutine to
 */
@Sampled
fun getLeftHand(session: Session, lifecycle: Lifecycle) {
    val gestureRecognizer =
        object {
            private val gestureAnglesRange: Map<String, Array<Pair<Float, Float>>> =
                mapOf(
                    "Thumb" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.41f, 0.88f), // [1]thumb to palm
                            Pair(0.9f, 1f), // [2]thumb
                            Pair(0.92f, 1f), // [3]
                            Pair(0.55f, 0.94f), // [4]thumb to index
                            Pair(-0.12f, 0.77f), // [5]index
                            Pair(-0.37f, 0.49f), // [6]
                            Pair(0.68f, 0.95f), // [7]
                            Pair(0.93f, 1f), // [8]index to middle
                            Pair(-0.08f, 0.73f), // [9]middle
                            Pair(-0.26f, 0.52f), // [10]
                            Pair(0.39f, 0.85f), // [11]
                            Pair(0.96f, 1f), // [12]middle to ring
                            Pair(-0.17f, 0.83f), // [13]ring
                            Pair(-0.44f, 0.41f), // [14]
                            Pair(0.52f, 0.87f), // [15]
                            Pair(0.94f, 1f), // [16]ring to little
                            Pair(-0.39f, 0.84f), // [17]little
                            Pair(-0.48f, 0.61f), // [18]
                            Pair(0.59f, 0.91f), // [19]
                        ),
                    "V" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.64f, 0.92f), // [1]thumb to palm
                            Pair(0.74f, 0.96f), // [2]thumb
                            Pair(0.88f, 0.99f), // [3]
                            Pair(0.57f, 0.95f), // [4]thumb to index
                            Pair(0.85f, 1f), // [5]index
                            Pair(0.92f, 1f), // [6]
                            Pair(0.98f, 1f), // [7]
                            Pair(0.6f, 0.99f), // [8]index to middle
                            Pair(0.37f, 0.99f), // [9]middle
                            Pair(0.12f, 1f), // [10]
                            Pair(0.66f, 1f), // [11]
                            Pair(0.4f, 1f), // [12]middle to ring
                            Pair(-0.06f, 0.91f), // [13]ring
                            Pair(-0.26f, 0.96f), // [14]
                            Pair(0.63f, 0.98f), // [15]
                            Pair(0.76f, 0.99f), // [16]ring to little
                            Pair(-0.47f, 0.78f), // [17]little
                            Pair(0.01f, 0.97f), // [18]
                            Pair(0.66f, 0.98f), // [19]
                        ),
                    "Heart" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.62f, 0.95f), // [1]thumb to palm
                            Pair(0.95f, 1f), // [2]thumb
                            Pair(0.88f, 0.99f), // [3]
                            Pair(0.58f, 0.99f), // [4]thumb to index
                            Pair(0.06f, 0.99f), // [5]index
                            Pair(-0.2f, 0.91f), // [6]
                            Pair(0.81f, 1f), // [7]
                            Pair(0.31f, 1f), // [8]index to middle
                            Pair(-0.12f, 0.79f), // [9]middle
                            Pair(-0.3f, 0.55f), // [10]
                            Pair(0.53f, 0.92f), // [11]
                            Pair(0.92f, 1f), // [12]middle to ring
                            Pair(-0.24f, 0.78f), // [13]ring
                            Pair(-0.25f, 0.51f), // [14]
                            Pair(0.58f, 0.90f), // [15]
                            Pair(0.93f, 1f), // [16]ring to little
                            Pair(-0.49f, 0.84f), // [17]little
                            Pair(-0.16f, 0.64f), // [18]
                            Pair(0.68f, 0.92f), // [19]
                        ),
                )

            private fun deriveAngles(handJoints: Map<HandJointType, Pose>): FloatArray {
                val directions: Array<Vector3> =
                    arrayOf(
                        handJoints[HandJointType.HAND_JOINT_TYPE_PALM]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_WRIST]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_DISTAL]!!.forward,
                    )
                return floatArrayOf(
                    // palm to wrist
                    directions[0].dot(directions[1]),
                    // thumb to palm
                    directions[0].dot(directions[2]),
                    // thumb
                    directions[2].dot(directions[3]),
                    directions[3].dot(directions[4]),
                    // thumb to index
                    directions[2].dot(directions[5]),
                    // index
                    directions[5].dot(directions[6]),
                    directions[6].dot(directions[7]),
                    directions[7].dot(directions[8]),
                    // index to middle
                    directions[8].dot(directions[10]),
                    // middle
                    directions[9].dot(directions[10]),
                    directions[10].dot(directions[11]),
                    directions[11].dot(directions[12]),
                    // middle to ring
                    directions[10].dot(directions[14]),
                    // ring
                    directions[13].dot(directions[14]),
                    directions[14].dot(directions[15]),
                    directions[15].dot(directions[16]),
                    // ring to little
                    directions[14].dot(directions[18]),
                    // little
                    directions[17].dot(directions[18]),
                    directions[18].dot(directions[19]),
                    directions[19].dot(directions[20]),
                )
            }

            private fun guessGesture(angleData: FloatArray): String {
                return gestureAnglesRange.entries
                    .firstOrNull { (_, ranges) ->
                        angleData.indices.all { i ->
                            val (min, max) = ranges[i]
                            angleData[i] in min..max
                        }
                    }
                    ?.key ?: "Unknown"
            }

            fun parseJointsIntoGesture(handJoints: Map<HandJointType, Pose>): String {
                val angles = deriveAngles(handJoints)
                return guessGesture(angles)
            }
        }

    yourCoroutineScope.launch {
        Hand.left(session)?.state?.collect { leftHandState ->
            // early out since we only care if the hand is actively tracking.
            if (leftHandState.trackingState != TrackingState.TRACKING) return@collect

            val joints = leftHandState.handJoints

            when (gestureRecognizer.parseJointsIntoGesture(joints)) {
                "Thumb" -> {
                    // Handle the user making a "thumbs up" gesture.
                }
                "Heart" -> {
                    // Handle the user making a one-handed heart gesture.
                }
                "V" -> {
                    // Handle the user making a "V" with their index and middle finger.
                }
                "Unknown" -> {}
            }

            // This function could also just interpret the joint poses to try and detect if the
            // user is making a specific gesture.
            yourRenderHandModelFunction(joints, isLeftHand = true)
        }
    }
}

/**
 * @param session the [Session] to get the hand from
 * @param lifecycle the [Lifecycle] to scope the coroutine to
 */
@Sampled
fun getRightHand(session: Session, lifecycle: Lifecycle) {
    val gestureRecognizer =
        object {
            private val gestureAnglesRange: Map<String, Array<Pair<Float, Float>>> =
                mapOf(
                    "Thumb" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.41f, 0.88f), // [1]thumb to palm
                            Pair(0.9f, 1f), // [2]thumb
                            Pair(0.92f, 1f), // [3]
                            Pair(0.55f, 0.94f), // [4]thumb to index
                            Pair(-0.12f, 0.77f), // [5]index
                            Pair(-0.37f, 0.49f), // [6]
                            Pair(0.68f, 0.95f), // [7]
                            Pair(0.93f, 1f), // [8]index to middle
                            Pair(-0.08f, 0.73f), // [9]middle
                            Pair(-0.26f, 0.52f), // [10]
                            Pair(0.39f, 0.85f), // [11]
                            Pair(0.96f, 1f), // [12]middle to ring
                            Pair(-0.17f, 0.83f), // [13]ring
                            Pair(-0.44f, 0.41f), // [14]
                            Pair(0.52f, 0.87f), // [15]
                            Pair(0.94f, 1f), // [16]ring to little
                            Pair(-0.39f, 0.84f), // [17]little
                            Pair(-0.48f, 0.61f), // [18]
                            Pair(0.59f, 0.91f), // [19]
                        ),
                    "V" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.64f, 0.92f), // [1]thumb to palm
                            Pair(0.74f, 0.96f), // [2]thumb
                            Pair(0.88f, 0.99f), // [3]
                            Pair(0.57f, 0.95f), // [4]thumb to index
                            Pair(0.85f, 1f), // [5]index
                            Pair(0.92f, 1f), // [6]
                            Pair(0.98f, 1f), // [7]
                            Pair(0.6f, 0.99f), // [8]index to middle
                            Pair(0.37f, 0.99f), // [9]middle
                            Pair(0.12f, 1f), // [10]
                            Pair(0.66f, 1f), // [11]
                            Pair(0.4f, 1f), // [12]middle to ring
                            Pair(-0.06f, 0.91f), // [13]ring
                            Pair(-0.26f, 0.96f), // [14]
                            Pair(0.63f, 0.98f), // [15]
                            Pair(0.76f, 0.99f), // [16]ring to little
                            Pair(-0.47f, 0.78f), // [17]little
                            Pair(0.01f, 0.97f), // [18]
                            Pair(0.66f, 0.98f), // [19]
                        ),
                    "Heart" to
                        arrayOf(
                            Pair(0.98f, 1f), // [0]palm to wrist
                            Pair(0.62f, 0.95f), // [1]thumb to palm
                            Pair(0.95f, 1f), // [2]thumb
                            Pair(0.88f, 0.99f), // [3]
                            Pair(0.58f, 0.99f), // [4]thumb to index
                            Pair(0.06f, 0.99f), // [5]index
                            Pair(-0.2f, 0.91f), // [6]
                            Pair(0.81f, 1f), // [7]
                            Pair(0.31f, 1f), // [8]index to middle
                            Pair(-0.12f, 0.79f), // [9]middle
                            Pair(-0.3f, 0.55f), // [10]
                            Pair(0.53f, 0.92f), // [11]
                            Pair(0.92f, 1f), // [12]middle to ring
                            Pair(-0.24f, 0.78f), // [13]ring
                            Pair(-0.25f, 0.51f), // [14]
                            Pair(0.58f, 0.90f), // [15]
                            Pair(0.93f, 1f), // [16]ring to little
                            Pair(-0.49f, 0.84f), // [17]little
                            Pair(-0.16f, 0.64f), // [18]
                            Pair(0.68f, 0.92f), // [19]
                        ),
                )

            private fun deriveAngles(handJoints: Map<HandJointType, Pose>): FloatArray {
                val directions: Array<Vector3> =
                    arrayOf(
                        handJoints[HandJointType.HAND_JOINT_TYPE_PALM]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_WRIST]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_THUMB_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_INDEX_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_MIDDLE_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_RING_DISTAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_METACARPAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_PROXIMAL]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_INTERMEDIATE]!!.forward,
                        handJoints[HandJointType.HAND_JOINT_TYPE_LITTLE_DISTAL]!!.forward,
                    )
                return floatArrayOf(
                    // palm to wrist
                    directions[0].dot(directions[1]),
                    // thumb to palm
                    directions[0].dot(directions[2]),
                    // thumb
                    directions[2].dot(directions[3]),
                    directions[3].dot(directions[4]),
                    // thumb to index
                    directions[2].dot(directions[5]),
                    // index
                    directions[5].dot(directions[6]),
                    directions[6].dot(directions[7]),
                    directions[7].dot(directions[8]),
                    // index to middle
                    directions[8].dot(directions[10]),
                    // middle
                    directions[9].dot(directions[10]),
                    directions[10].dot(directions[11]),
                    directions[11].dot(directions[12]),
                    // middle to ring
                    directions[10].dot(directions[14]),
                    // ring
                    directions[13].dot(directions[14]),
                    directions[14].dot(directions[15]),
                    directions[15].dot(directions[16]),
                    // ring to little
                    directions[14].dot(directions[18]),
                    // little
                    directions[17].dot(directions[18]),
                    directions[18].dot(directions[19]),
                    directions[19].dot(directions[20]),
                )
            }

            private fun guessGesture(angleData: FloatArray): String {
                return gestureAnglesRange.entries
                    .firstOrNull { (_, ranges) ->
                        angleData.indices.all { i ->
                            val (min, max) = ranges[i]
                            angleData[i] in min..max
                        }
                    }
                    ?.key ?: "Unknown"
            }

            fun parseJointsIntoGesture(handJoints: Map<HandJointType, Pose>): String {
                val angles = deriveAngles(handJoints)
                return guessGesture(angles)
            }
        }

    yourCoroutineScope.launch {
        Hand.right(session)?.state?.collect { rightHandState ->
            // early out since we only care if the hand is actively tracking.
            if (rightHandState.trackingState != TrackingState.TRACKING) return@collect

            val joints = rightHandState.handJoints

            when (gestureRecognizer.parseJointsIntoGesture(joints)) {
                "Thumb" -> {
                    // Handle the user making a "thumbs up" gesture.
                }
                "Heart" -> {
                    // Handle the user making a one-handed heart gesture.
                }
                "V" -> {
                    // Handle the user making a "V" with their index and middle finger.
                }
                "Unknown" -> {}
            }

            // This function could also just interpret the joint poses to try and detect if the
            // user is making a specific gesture.
            yourRenderHandModelFunction(joints, isLeftHand = false)
        }
    }
}

private fun yourRenderHandModelFunction(joints: Map<HandJointType, Pose>, isLeftHand: Boolean) {}

private val yourCoroutineScope = MainScope()
