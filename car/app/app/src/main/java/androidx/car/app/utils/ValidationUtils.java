/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.utils;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.List;

// TODO(rampara): Uncomment on addition of model module
//import androidx.car.app.model.CarText;
//import androidx.car.app.model.GridItem;
//import androidx.car.app.model.Row;
//import androidx.car.app.model.Toggle;

/**
 * Shared util methods for handling different distraction validation logic.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ValidationUtils {
    private static final int INVALID_INDEX = -1;

    /**
     * Returns {@code true} if the sizes and string contents of the two lists of items are equal,
     * {@code false} otherwise.
     */
    public static boolean itemsHaveSameContent(
            @NonNull List<Object> itemList1, @NonNull List<Object> itemList2,
            @NonNull Logger logger) {
        return itemsHaveSameContent(itemList1, INVALID_INDEX, itemList2, INVALID_INDEX, logger);
    }

    /**
     * Returns {@code true} if the sizes and string contents of the two lists of items are equal,
     * {@code false} otherwise.
     */
    public static boolean itemsHaveSameContent(
            @NonNull List<Object> itemList1,
            int itemList1SelectedIndex,
            @NonNull List<Object> itemList2,
            int itemList2SelectedIndex,
            @NonNull Logger logger) {
        if (itemList1.size() != itemList2.size()) {
            logger.log(
                    "Different item list sizes. Old: " + itemList1.size() + ". New: "
                            + itemList2.size());
            return false;
        }

        for (int i = 0; i < itemList1.size(); i++) {
            Object itemObj1 = itemList1.get(i);
            Object itemObj2 = itemList2.get(i);

            if (itemObj1.getClass() != itemObj2.getClass()) {
                logger.log(
                        "Different item types at index "
                                + i
                                + ". Old: "
                                + itemObj1.getClass()
                                + ". New: "
                                + itemObj2.getClass());
                return false;
            }

            // TODO(rampara): Uncomment on addition of model module
//            if (itemObj1 instanceof Row) {
//                if (!rowsHaveSameContent((Row) itemObj1, (Row) itemObj2, i, logger)) {
//                    return false;
//                }
//            } else if (itemObj1 instanceof GridItem) {
//                if (!gridItemsHaveSameContent(
//                        (GridItem) itemObj1,
//                        itemList1SelectedIndex == i,
//                        (GridItem) itemObj2,
//                        itemList2SelectedIndex == i,
//                        i,
//                        logger)) {
//                    return false;
//                }
//            }
        }

        return true;
    }

    /**
     * Returns {@code true} if the string contents of the two rows are equal, {@code false}
     * otherwise.
     */
    // TODO(rampara): Uncomment on addition of model module
//    private static boolean rowsHaveSameContent(Row row1, Row row2, int index, Logger logger) {
//        // Special case for rows with toggles - if the toggle state has changed, then text updates
//        // are allowed.
//        if (rowToggleStateHasChanged(row1, row2)) {
//            return true;
//        }
//
//        if (!carTextsHasSameString(row1.getTitle(), row2.getTitle())) {
//            logger.log(
//                    "Different row titles at index "
//                            + index
//                            + ". Old: "
//                            + row1.getTitle()
//                            + ". New: "
//                            + row2.getTitle());
//            return false;
//        }
//
//        List<CarText> row1Texts = row1.getText();
//        List<CarText> row2Texts = row2.getText();
//        if (row1Texts.size() != row2Texts.size()) {
//            logger.log(
//                    "Different text list size at row index "
//                            + index
//                            + ". Old: "
//                            + row1Texts.size()
//                            + ". New: "
//                            + row2Texts.size());
//            return false;
//        }
//
//        for (int j = 0; j < row1Texts.size(); j++) {
//            if (!carTextsHasSameString(row1Texts.get(j), row2Texts.get(j))) {
//                logger.log(
//                        "Different texts at row index "
//                                + index
//                                + ". Old row: "
//                                + row1Texts.get(j)
//                                + ". New row: "
//                                + row2Texts.get(j));
//                return false;
//            }
//        }
//
//        return true;
//    }

    /**
     * Returns {@code true} if string contents and images of the two grid items are equal, {@code
     * false} otherwise.
     */
    // TODO(rampara): Uncomment on addition of model module
//    private static boolean gridItemsHaveSameContent(
//            GridItem gridItem1,
//            boolean isGridItem1Selected,
//            GridItem gridItem2,
//            boolean isGridItem2Selected,
//            int index,
//            Logger logger) {
//        // Special case for grid items with toggles - if the toggle state has changed, then text
//        // and image updates are allowed.
//        if (gridItemToggleStateHasChanged(gridItem1, gridItem2)) {
//            return true;
//        }
//
//        // Special case for grid items that are selectable - if the selected state has changed,
//        then
//        // text and image updates are allowed.
//        if (isGridItem1Selected != isGridItem2Selected) {
//            return true;
//        }
//
//        if (!carTextsHasSameString(gridItem1.getTitle(), gridItem2.getTitle())) {
//            logger.log(
//                    "Different grid item titles at index "
//                            + index
//                            + ". Old: "
//                            + gridItem1.getTitle()
//                            + ". New: "
//                            + gridItem2.getTitle());
//            return false;
//        }
//
//        if (!carTextsHasSameString(gridItem1.getText(), gridItem2.getText())) {
//            logger.log(
//                    "Different grid item texts at index "
//                            + index
//                            + ". Old: "
//                            + gridItem1.getText()
//                            + ". New: "
//                            + gridItem2.getText());
//            return false;
//        }
//
//        if (!Objects.equals(gridItem1.getImage(), gridItem2.getImage())) {
//            logger.log("Different grid item images at index " + index);
//            return false;
//        }
//
//        if (gridItem1.getImageType() != gridItem2.getImageType()) {
//            logger.log(
//                    "Different grid item image types at index "
//                            + index
//                            + ". Old: "
//                            + gridItem1.getImageType()
//                            + ". New: "
//                            + gridItem2.getImageType());
//            return false;
//        }
//
//        return true;
//    }

    /**
     * Returns {@code true} if the strings of the two {@link CarText}s are the same, {@code false}
     * otherwise.
     *
     * <p>Spans that are attached to the strings are ignored from the comparison.
     */
    // TODO(rampara): Uncomment on addition of model module
//    private static boolean carTextsHasSameString(
//            @Nullable CarText carText1, @Nullable CarText carText2) {
//        // If both carText1 and carText2 are null, return true. If only one of them is null,
//        return
//        // false.
//        if (carText1 == null || carText2 == null) {
//            return carText1 == null && carText2 == null;
//        }
//
//        return Objects.equals(carText1.getText(), carText2.getText());
//    }
//
//    private static boolean rowToggleStateHasChanged(Row row1, Row row2) {
//        Toggle toggle1 = row1.getToggle();
//        Toggle toggle2 = row2.getToggle();
//
//        return toggle1 != null && toggle2 != null && toggle1.isChecked() != toggle2.isChecked();
//    }
//
//    private static boolean gridItemToggleStateHasChanged(GridItem gridItem1, GridItem gridItem2) {
//        Toggle toggle1 = gridItem1.getToggle();
//        Toggle toggle2 = gridItem2.getToggle();
//
//        return toggle1 != null && toggle2 != null && toggle1.isChecked() != toggle2.isChecked();
//    }
    private ValidationUtils() {
    }
}
