/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui.model

enum class ComponentCategory(val displayName: String) {
    LAYOUT("Layout"),
    CONTENT("Content"),
    INPUT("Input"),
}

enum class UiComponent(val displayName: String, val category: ComponentCategory) {
    ROW("Row", ComponentCategory.LAYOUT),
    COLUMN("Column", ComponentCategory.LAYOUT),
    LIST("List", ComponentCategory.LAYOUT),
    CARD("Card", ComponentCategory.LAYOUT),
    TABS("Tabs", ComponentCategory.LAYOUT),
    MODAL("Modal", ComponentCategory.LAYOUT),
    TEXT("Text", ComponentCategory.CONTENT),
    IMAGE("Image", ComponentCategory.CONTENT),
    ICON("Icon", ComponentCategory.CONTENT),
    DIVIDER("Divider", ComponentCategory.CONTENT),
    VIDEO("Video", ComponentCategory.CONTENT),
    AUDIO_PLAYER("AudioPlayer", ComponentCategory.CONTENT),
    BUTTON("Button", ComponentCategory.INPUT),
    CHECK_BOX("CheckBox", ComponentCategory.INPUT),
    SLIDER("Slider", ComponentCategory.INPUT),
    TEXT_FIELD("TextField", ComponentCategory.INPUT),
    DATE_TIME_INPUT("DateTimeInput", ComponentCategory.INPUT),
    CHOICE_PICKER("ChoicePicker", ComponentCategory.INPUT);

    companion object {
        val byCategory: Map<ComponentCategory, List<UiComponent>> = entries.groupBy { it.category }
    }
}
