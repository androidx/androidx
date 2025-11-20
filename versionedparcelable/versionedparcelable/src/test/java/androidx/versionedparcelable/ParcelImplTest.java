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

package androidx.versionedparcelable;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Config.TARGET_SDK})
public class ParcelImplTest {

    @Test
    public void testFakeParcelableInit_throwsInitializedException() {
        ExceptionInInitializerError e = assertThrows(
                ExceptionInInitializerError.class, FakeParcelable::new);

        assertTrue(e.getCause() instanceof InitializedException);
    }

    @Test
    public void testCreateFromParcel_withNonVersionedParcelableClass_throwsNoSuchMethodException() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            Parcel p = Parcel.obtain();
            p.writeString("androidx.versionedparcelable.ParcelImplTest$FakeParcelable");
            p.setDataPosition(0);
            ParcelImpl.CREATOR.createFromParcel(p);
        });

        assertTrue(e.getCause() instanceof NoSuchMethodException);
    }

    @Test
    public void testCreateFromParcel_withMissingClass_throwsClassNotFoundException() {
        RuntimeException e = assertThrows(RuntimeException.class, () -> {
            Parcel p = Parcel.obtain();
            p.writeString("androidx.versionedparcelable.MissingParcelable");
            p.setDataPosition(0);
            ParcelImpl.CREATOR.createFromParcel(p);
        });

        assertTrue(e.getCause() instanceof ClassNotFoundException);
    }

    public static class FakeParcelable {
        static {
            //noinspection ConstantValue
            if (true) {
                throw new InitializedException();
            }
        }
    }

    private static class InitializedException extends RuntimeException { }
}
