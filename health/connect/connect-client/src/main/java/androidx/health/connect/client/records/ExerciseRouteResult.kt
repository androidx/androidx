/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.health.connect.client.records

/** Result of the route associated with an exercise session a user does. */
abstract class ExerciseRouteResult internal constructor() {

    /** Class containing data for an [ExerciseRoute]. */
    class Data(val exerciseRoute: ExerciseRoute) : ExerciseRouteResult() {

        override fun equals(other: Any?): Boolean {
            if (other !is Data) {
                return false
            }
            return exerciseRoute == other.exerciseRoute
        }

        override fun hashCode(): Int {
            return 0
        }
    }

    /** Class indicating that a permission hasn't been granted and a value couldn't be returned. */
    class ConsentRequired : ExerciseRouteResult() {
        override fun equals(other: Any?): Boolean {
            return other is ConsentRequired
        }

        override fun hashCode(): Int {
            return 0
        }
    }

    /** Class indicating that there's no data to request permissions for. */
    class NoData : ExerciseRouteResult() {
        override fun equals(other: Any?): Boolean {
            return other is NoData
        }

        override fun hashCode(): Int {
            return 0
        }
    }
}
