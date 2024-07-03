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

package androidx.pdf.viewer.loader;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import android.os.RemoteException;

import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.service.PdfDocumentRemoteProto;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Tests for {@link PdfTaskExecutor}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class PdfTaskExecutorTest {

    @Mock
    private PdfLoader mPdfLoader;
    @Mock
    private PdfDocumentRemote mPdfDocument;
    @Mock
    private WeakPdfLoaderCallbacks mCallbacks;
    private PdfTaskExecutor mExecutor;

    private List<String> mFinishedTaskResults;

    private CountDownLatch mStartTaskLatch;
    private CountDownLatch mFinishedTaskLatch;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mPdfLoader.getCallbacks()).thenReturn(mCallbacks);
        when(mPdfLoader.getLoadedPdfDocument(isA(String.class))).thenReturn(mPdfDocument);
        mExecutor = new PdfTaskExecutor();
        mExecutor.start();

        mStartTaskLatch = new CountDownLatch(1);
        mFinishedTaskResults = new ArrayList<>();
    }

    @Test
    public void testSchedule() throws Exception {
        mFinishedTaskLatch = new CountDownLatch(6);

        doSchedule(Priority.INITIALIZE, "init");
        doSchedule(Priority.BITMAP, "bitmap1");
        doSchedule(Priority.TEXT, "text1");
        doSchedule(Priority.BITMAP, "bitmap2");
        doSchedule(Priority.TEXT, "text2");

        mStartTaskLatch.countDown();
        mFinishedTaskLatch.await(1, TimeUnit.SECONDS);
        assertThat(mFinishedTaskResults)
                .isEqualTo(Arrays.asList("init", "bitmap1", "bitmap2", "text1", "text2"));
    }

    @Test
    public void testCancel() throws Exception {
        mFinishedTaskLatch = new CountDownLatch(2);
        doSchedule(Priority.INITIALIZE, "init");
        TestTask task = new TestTask(Priority.BITMAP, "cancelled");
        mExecutor.schedule(task);
        task.cancel();

        mStartTaskLatch.countDown();
        mFinishedTaskLatch.await(1, TimeUnit.SECONDS);
        assertThat(mFinishedTaskResults).isEqualTo(Arrays.asList(new String[]{"init"}));
    }

    private void doSchedule(final Priority priority, final String result) {
        mExecutor.schedule(new TestTask(priority, result));
    }

    class TestTask extends AbstractPdfTask<String> {
        private final String mResult;

        TestTask(Priority priority, String result) {
            super(mPdfLoader, priority);
            this.mResult = result;
        }

        @Override
        protected String getLogTag() {
            return "TestTask";
        }

        @Override
        protected String doInBackground(PdfDocumentRemoteProto pdf) throws RemoteException {
            try {
                mStartTaskLatch.await();
                return mResult;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void doCallback(PdfLoaderCallbacks callbacks, String result) {
            mFinishedTaskResults.add(result);
        }

        @Override
        protected void cleanup() {
            mFinishedTaskLatch.countDown();
        }
    }
}

