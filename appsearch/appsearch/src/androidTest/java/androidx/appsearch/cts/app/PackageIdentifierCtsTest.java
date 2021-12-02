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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.PackageIdentifier;

import org.junit.Test;

public class PackageIdentifierCtsTest {
    @Test
    public void testGetters() {
        PackageIdentifier packageIdentifier = new PackageIdentifier("com.packageName",
                /*sha256Certificate=*/ new byte[]{100});
        assertThat(packageIdentifier.getPackageName()).isEqualTo("com.packageName");
        assertThat(packageIdentifier.getSha256Certificate()).isEqualTo(new byte[]{100});
    }
}
