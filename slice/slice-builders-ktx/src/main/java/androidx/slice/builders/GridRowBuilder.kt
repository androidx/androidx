/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.slice.builders

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class GridRowBuilderDsl : GridRowBuilder()

/**
 * Helper class annotated with @SliceMarker, which is annotated with @DslMarker.
 * Two implicit receivers that are annotated with @SliceMarker are not accessible in the same scope,
 * ensuring a type-safe DSL.
 */
@SliceMarker
class CellBuilderDsl : GridRowBuilder.CellBuilder()

/**
 * @see GridRowBuilder.addCell
 */
inline fun GridRowBuilderDsl.cell(buildCell: CellBuilderDsl.() -> Unit) =
    addCell(CellBuilderDsl().apply { buildCell() })

/**
 * @see GridRowBuilder.setSeeMoreCell
 */
inline fun GridRowBuilderDsl.seeMoreCell(buildCell: CellBuilderDsl.() -> Unit) =
    setSeeMoreCell(CellBuilderDsl().apply { buildCell() })
