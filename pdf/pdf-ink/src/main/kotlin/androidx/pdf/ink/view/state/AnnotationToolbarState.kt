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

package androidx.pdf.ink.view.state

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import androidx.pdf.ink.view.brush.BrushSizeSelectorView
import androidx.pdf.ink.view.colorpalette.ColorPaletteView
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.pdf.ink.view.draganddrop.ToolbarDockState
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.HIGHLIGHTER
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.PEN

/**
 * Represents the configurable attributes for a single brush-based annotation tool, such as a pen or
 * highlighter.
 *
 * @property selectedBrushSizeIndex The index of the selected stroke width in
 *   [BrushSizeSelectorView].
 * @property selectedColorIndex The index of the selected item in the [ColorPaletteView].
 * @property paletteItem The palette item selected in [ColorPaletteView]. This is stored as a
 *   convenient, cached value to easily configure UI components.
 */
@SuppressLint("BanParcelableUsage")
internal data class ToolAttributes(
    val selectedBrushSizeIndex: Int,
    val selectedColorIndex: Int,
    val paletteItem: PaletteItem,
) : Parcelable {

    constructor(
        parcel: Parcel
    ) : this(
        selectedBrushSizeIndex = parcel.readInt(),
        selectedColorIndex = parcel.readInt(),
        paletteItem =
            checkNotNull(
                ParcelCompat.readParcelable(
                    parcel,
                    PaletteItem::class.java.classLoader,
                    PaletteItem::class.java,
                )
            ),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(selectedBrushSizeIndex)
        parcel.writeInt(selectedColorIndex)
        parcel.writeParcelable(paletteItem, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ToolAttributes> {
        override fun createFromParcel(parcel: Parcel): ToolAttributes {
            return ToolAttributes(parcel)
        }

        override fun newArray(size: Int): Array<ToolAttributes?> {
            return arrayOfNulls(size)
        }
    }
}

/**
 * Encapsulates the complete, immutable state of the [androidx.pdf.ink.view.AnnotationToolbar].
 *
 * This data class serves as single source of truth for the entire toolbar's UI. Instead of each UI
 * component managing its own state, this class represents the complete snapshot of the toolbar at
 * any given moment. Views observe changes to this state and update themselves accordingly.
 */
@SuppressLint("BanParcelableUsage")
internal data class AnnotationToolbarState(
    /** The current selected tool on the toolbar. */
    val selectedTool: String?,
    /** Current state of the hide annotation button. */
    val isAnnotationVisible: Boolean,
    /** Whether the undo button is enabled. */
    val canUndo: Boolean,
    /** Whether the redo button is enabled. */
    val canRedo: Boolean,
    /** Whether the brush size slider is visible. */
    val showBrushSizeSlider: Boolean,
    /**
     * Whether the color palette button is enabled. Color palette is enabled only for some tools
     * e.g. [PEN], [HIGHLIGHTER]
     */
    val isColorPaletteEnabled: Boolean,
    /** Whether the color palette is visible. */
    val showColorPalette: Boolean,
    /**
     * Last selected configuration of pen. When the [PEN] is selected, this configuration will be
     * applied.
     */
    val penState: ToolAttributes,
    /**
     * Last selected configuration of highlighter. When the [HIGHLIGHTER] is selected, this
     * configuration will be applied.
     */
    val highlighterState: ToolAttributes,

    /** The current docking state of the toolbar. */
    @get:ToolbarDockState.DockState @param:ToolbarDockState.DockState val dockedState: Int,

    /** Whether the toolbar is currently expanded. */
    val isExpanded: Boolean,
) : Parcelable {

    // Secondary constructor to read from a Parcel.
    constructor(
        parcel: Parcel
    ) : this(
        selectedTool = parcel.readString(),
        isAnnotationVisible = parcel.readByte() != 0.toByte(),
        canUndo = parcel.readByte() != 0.toByte(),
        canRedo = parcel.readByte() != 0.toByte(),
        showBrushSizeSlider = parcel.readByte() != 0.toByte(),
        isColorPaletteEnabled = parcel.readByte() != 0.toByte(),
        showColorPalette = parcel.readByte() != 0.toByte(),
        penState =
            checkNotNull(
                ParcelCompat.readParcelable(
                    parcel,
                    ToolAttributes::class.java.classLoader,
                    ToolAttributes::class.java,
                )
            ),
        highlighterState =
            checkNotNull(
                ParcelCompat.readParcelable(
                    parcel,
                    ToolAttributes::class.java.classLoader,
                    ToolAttributes::class.java,
                )
            ),
        dockedState = parcel.readInt(),
        isExpanded = parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(selectedTool)
        parcel.writeByte(if (isAnnotationVisible) 1 else 0)
        parcel.writeByte(if (canUndo) 1 else 0)
        parcel.writeByte(if (canRedo) 1 else 0)
        parcel.writeByte(if (showBrushSizeSlider) 1 else 0)
        parcel.writeByte(if (isColorPaletteEnabled) 1 else 0)
        parcel.writeByte(if (showColorPalette) 1 else 0)
        parcel.writeParcelable(penState, flags)
        parcel.writeParcelable(highlighterState, flags)
        parcel.writeInt(dockedState)
        parcel.writeByte(if (isExpanded) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AnnotationToolbarState> {
        override fun createFromParcel(parcel: Parcel): AnnotationToolbarState {
            return AnnotationToolbarState(parcel)
        }

        override fun newArray(size: Int): Array<AnnotationToolbarState?> {
            return arrayOfNulls(size)
        }
    }
}
