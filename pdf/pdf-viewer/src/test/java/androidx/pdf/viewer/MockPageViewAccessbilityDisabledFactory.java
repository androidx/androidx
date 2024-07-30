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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.pdf.find.FindInFileView;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.TileBoard;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.MosaicView;
import androidx.pdf.widget.ZoomView;

public class MockPageViewAccessbilityDisabledFactory extends PageViewFactory {
    public MockPageViewAccessbilityDisabledFactory(@NonNull Context context,
            @NonNull PdfLoader pdfLoader,
            @NonNull PaginatedView paginatedView,
            @NonNull ZoomView zoomView,
            @NonNull SingleTapHandler singleTapHandler,
            @NonNull FindInFileView findInFileView) {
        super(context, pdfLoader, paginatedView, zoomView, singleTapHandler, findInFileView);
    }

    @NonNull
    @Override
    protected MosaicView.BitmapSource createBitmapSource(int pageNum) {
        return new MosaicView.BitmapSource() {
            @Override
            public void requestPageBitmap(@NonNull Dimensions pageSize,
                    boolean alsoRequestingTiles) {

            }

            @Override
            public void requestNewTiles(@NonNull Dimensions pageSize,
                    @NonNull Iterable<TileBoard.TileInfo> newTiles) {

            }

            @Override
            public void cancelTiles(@NonNull Iterable<Integer> tileIds) {

            }
        };
    }

    @Override
    protected boolean isTouchExplorationEnabled(@NonNull Context context) {
        return false;
    }
}
