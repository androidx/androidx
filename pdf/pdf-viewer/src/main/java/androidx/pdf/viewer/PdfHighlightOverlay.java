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

package androidx.pdf.viewer;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.MatchRects;
import androidx.pdf.models.PageSelection;
import androidx.pdf.util.HighlightOverlay;
import androidx.pdf.util.HighlightPaint;
import androidx.pdf.util.RectDrawSpec;

/**
 * A {@link HighlightOverlay} overlay that highlights all of the matches described in the given
 * {@link MatchRects} object or a given {@link PageSelection}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PdfHighlightOverlay extends HighlightOverlay {

    /** Highlights the selection blue. */
    public PdfHighlightOverlay(@NonNull PageSelection selection) {
        super(new RectDrawSpec(HighlightPaint.SELECTION, selection.getRects()));
    }

    /** Highlights all the matches yellow. */
    public PdfHighlightOverlay(@NonNull MatchRects matchRects) {
        super(new RectDrawSpec(HighlightPaint.MATCH, matchRects.flatten()));
    }

    /** Highlights the current match orange, and all the other matches yellow. */
    public PdfHighlightOverlay(@NonNull MatchRects matchRects, int currentMatch) {
        super(
                new RectDrawSpec(HighlightPaint.CURRENT_MATCH, matchRects.get(currentMatch)),
                new RectDrawSpec(HighlightPaint.MATCH,
                        matchRects.flattenExcludingMatch(currentMatch)));
    }
}
