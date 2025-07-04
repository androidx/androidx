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
package androidx.xr.scenecore

import androidx.annotation.IntDef

/** Type of plane based on orientation i.e. Horizontal or Vertical. */
// TODO - b/419544472 Align on a common implementation for this type in SceneCore & ARCore.
public object PlaneOrientation {
    public const val HORIZONTAL: Int = 0
    public const val VERTICAL: Int = 1
    public const val ANY: Int = 2
}

@Retention(AnnotationRetention.SOURCE)
@IntDef(PlaneOrientation.HORIZONTAL, PlaneOrientation.VERTICAL, PlaneOrientation.ANY)
@Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
internal annotation class PlaneOrientationValue

/** Semantic plane types. */
// TODO - b/419544472 Align on a common implementation for this type in SceneCore & ARCore.
public object PlaneSemanticType {

    public const val WALL: Int = 0
    public const val FLOOR: Int = 1
    public const val CEILING: Int = 2
    public const val TABLE: Int = 3
    public const val ANY: Int = 4
}

@Retention(AnnotationRetention.SOURCE)
@IntDef(
    PlaneSemanticType.WALL,
    PlaneSemanticType.FLOOR,
    PlaneSemanticType.CEILING,
    PlaneSemanticType.TABLE,
    PlaneSemanticType.ANY,
)
@Target(AnnotationTarget.TYPE, AnnotationTarget.PROPERTY, AnnotationTarget.VALUE_PARAMETER)
internal annotation class PlaneSemanticTypeValue
