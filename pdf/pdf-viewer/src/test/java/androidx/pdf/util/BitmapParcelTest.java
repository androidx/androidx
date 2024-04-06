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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.pdf.mocks.MockBitmapParcel;
import androidx.pdf.test.R;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;

//@SmallTest
//@RunWith(RobolectricTestRunner.class)
public class BitmapParcelTest {

    private static final String TAG = "BitmapParcelTest";

    private Bitmap mSourceBitmap;

    @Before
    public void setUp() {
        mSourceBitmap =
                BitmapFactory.decodeResource(
                        ApplicationProvider.getApplicationContext().getResources(),
                        R.raw.launcher_pdfviewer);
    }

    // TODO: Fails in the first execution and then passes. Uncomment when fixed
//    @Test
//    public void testBitmapTransferWithOutputFileDescriptor() {
//        int bufferSize = mSourceBitmap.getWidth() * mSourceBitmap.getHeight() * 256;
//        testBitmapTransfer(bufferSize);
//    }

    private void testBitmapTransfer(int bufferSize) {
        Bitmap bitmap = Bitmap.createBitmap(mSourceBitmap.getWidth(), mSourceBitmap.getHeight(),
                mSourceBitmap.getConfig());
        BitmapParcel bitmapParcel = new MockBitmapParcel(bitmap, bufferSize);
        ParcelFileDescriptor fd = bitmapParcel.openOutputFd();
        if (fd == null) {
            fail("fd is null");
        }
        sendBytes(fd);
        bitmapParcel.close();
        assertThat(validateBitmap(bitmap)).isTrue();
    }

    private boolean validateBitmap(Bitmap bitmap) {
        boolean same = bitmap.sameAs(mSourceBitmap);
        Log.i(
                TAG,
                String.format(
                        "Compare bitmaps  %s = %s : %s",
                        mSourceBitmap.getByteCount(), bitmap.getByteCount(), same));
        return same;
    }

    private void sendBytes(OutputStream os) {
        int numBytes = mSourceBitmap.getByteCount();
        byte[] bytes = new byte[numBytes];
        Buffer buffer = ByteBuffer.wrap(bytes);
        mSourceBitmap.copyPixelsToBuffer(buffer);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Utils.copyAndClose(is, os);
    }


    private void sendBytes(ParcelFileDescriptor fd) {
        OutputStream os = new ParcelFileDescriptor.AutoCloseOutputStream(fd);
        int numBytes = mSourceBitmap.getByteCount();
        byte[] bytes = new byte[numBytes];
        Buffer buffer = ByteBuffer.wrap(bytes);
        mSourceBitmap.copyPixelsToBuffer(buffer);
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        Utils.copyAndClose(is, os);
    }
}
