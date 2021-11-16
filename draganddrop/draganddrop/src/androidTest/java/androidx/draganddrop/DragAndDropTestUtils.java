/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.draganddrop;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import android.content.ClipData;
import android.content.ClipData.Item;
import android.content.ClipDescription;
import android.net.Uri;
import android.view.DragEvent;

/** Helper utilities for creating drag events. */
public final class DragAndDropTestUtils {

    private static final String LABEL = "Label";
    static final String SAMPLE_TEXT = "Drag Text";
    static final Uri SAMPLE_URI = Uri.parse("http://www.google.com");

    private DragAndDropTestUtils() {}

    /**
     * Makes a stub drag event containing fake text data.
     *
     * @param action One of the {@link DragEvent} actions.
     */
    public static DragEvent makeTextDragEvent(int action) {
        return makeDragEvent(action, new Item(SAMPLE_TEXT), ClipDescription.MIMETYPE_TEXT_PLAIN);
    }

    /**
     * Makes a stub drag event containing text data.
     *
     * @param action One of the {@link DragEvent} actions.
     * @param text The text being dragged.
     */
    public static DragEvent makeTextDragEvent(int action, String text) {
        return makeDragEvent(action, new Item(text), ClipDescription.MIMETYPE_TEXT_PLAIN);
    }

    /**
     * Makes a stub drag event containing an image mimetype and fake uri.
     *
     * @param action One of the {@link DragEvent} actions.
     */
    public static DragEvent makeImageDragEvent(int action) {
        // We're not actually resolving Uris in these tests, so this can be anything:
        String mimeType = "image/*";
        return makeDragEvent(action, new Item(SAMPLE_URI), mimeType);
    }

    private static DragEvent makeDragEvent(int action, ClipData.Item item, String mimeType) {
        ClipData clipData = new ClipData(LABEL, new String[] {mimeType}, item);
        ClipDescription clipDescription = new ClipDescription(LABEL, new String[] {mimeType});
        DragEvent dragEvent = mock(DragEvent.class);
        doReturn(action).when(dragEvent).getAction();
        doReturn(clipData).when(dragEvent).getClipData();
        doReturn(clipDescription).when(dragEvent).getClipDescription();
        return dragEvent;
    }
}
