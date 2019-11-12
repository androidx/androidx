/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.os.Build;

import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.util.concurrent.atomic.AtomicInteger;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ImageAnalysisNonBlockingAnalyzerTest {
    private ImageAnalysisNonBlockingAnalyzer mImageAnalysisNonBlockingAnalyzer;
    private static final AtomicInteger ROTATION = new AtomicInteger(0);
    private ImageAnalysis.Analyzer mAnalyzer;
    private ImageReaderProxy mImageReaderProxy;
    private ImageProxy mImageProxy;


    @Before
    public void setup() {
        mImageProxy = mock(ImageProxy.class);
        when(mImageProxy.getImageInfo()).thenReturn(mock(ImageInfo.class));
        mImageReaderProxy = mock(ImageReaderProxy.class);

        when(mImageReaderProxy.acquireLatestImage()).thenReturn(mImageProxy);

        mAnalyzer = mock(ImageAnalysis.Analyzer.class);
        mImageAnalysisNonBlockingAnalyzer = new ImageAnalysisNonBlockingAnalyzer(
                CameraXExecutors.directExecutor());
        mImageAnalysisNonBlockingAnalyzer.setAnalyzer(CameraXExecutors.mainThreadExecutor(),
                mAnalyzer);
        mImageAnalysisNonBlockingAnalyzer.setRelativeRotation(ROTATION.get());
    }

    @Test
    public void imageClosedAfterAnalyzerClosed() {
        mImageAnalysisNonBlockingAnalyzer.close();

        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);

        verify(mImageProxy, times(1)).close();
    }

    @Test
    public void analysisNotRunAfterAnalyzerClosed() {
        mImageAnalysisNonBlockingAnalyzer.close();

        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);

        verifyZeroInteractions(mAnalyzer);
    }

    @Test
    public void imageClosedWhenAnalyzerOpen() {
        mImageAnalysisNonBlockingAnalyzer.open();

        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);

        verify(mImageProxy, times(1)).close();
    }

    @Test
    public void analysisRunWhenAnalyzerOpen() {
        mImageAnalysisNonBlockingAnalyzer.open();

        mImageAnalysisNonBlockingAnalyzer.onImageAvailable(mImageReaderProxy);

        verify(mAnalyzer, times(1)).analyze(mImageProxy, ROTATION.get());
    }
}
