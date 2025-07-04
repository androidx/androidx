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

package androidx.compose.ui.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.bytecode
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java

object AndroidStubs {

    val Context =
        bytecode(
            "libs/android.jar",
            java(
                """
package android.content;

import android.content.res.Resources;

public class Context {
    public Resources getResources() {
        return null;
    }
}
        """
            ),
            0xc2b7484b,
            """
            android/content/Context.class:
            H4sIAAAAAAAA/zv1b9c+BgYGRwZ+LgYmBmZ2BhYeBlYGNkYGgazEskT9nMS8
            dH3/pKzU5BJGBjabzLzMEjtGBmYNzTB2Bg5GBvHEvJSi/MwU/eT8vJLUvBJ9
            ZxBdAVTL4pyfksrIwO+TmZfqV5qblFoUkpiUAxThSU8tCUotzi8tSk4tZmRQ
            1ND0QTekKLVYH67EmpGBKxjMdMsE64daoQdyH4MiAzvQ2SDAxMAIcjiQ5ATy
            ZIE0I5Bm1drOwLgRyACaAiTZwIIgkpuBB6pUCqqUiXEDmjoOIMkLNpoPAIc3
            ZUAnAQAA
            """,
        )

    val Resources =
        bytecode(
            "libs/android.jar",
            java(
                """
package android.content.res;

public class Resources {
    public Configuration getConfiguration() {
        return null;
    }
}
        """
            ),
            0xf1ea767d,
            """
            android/content/res/Resources.class:
            H4sIAAAAAAAA/22Pz0rDQBDGv0nTRGttexZ68CC0HtwHUAQpeCoWqnjfJGPY
            Undhs/G59CR48AF8KOlsKILiHL75w8dvZr6+Pz4B3GA8QIJejnSIPjLCZKNf
            tNpqW6tVseEyELIrY024JvRm88ccB4SptpV3plKls4FtUJ4btebGtb7khpAu
            XMWE8dJYvmufC/YPutjKZFJzWDj7ZOrW62CcJZzN5sv/cL9sl4TBfUe/NZEz
            +ll2Ee/FKXJ5I0YCio+IHko3lUyS++fvoDcphCOadcNU9AjDvfVkb03o9Y8v
            6nGHHu0AlRLTZjcBAAA=
            """,
        )

    val Configuration =
        bytecode(
            "libs/android.jar",
            java(
                """
package android.content.res;

public class Configuration {
    public int screenWidthDp;
    public int screenHeightDp;
}

        """
            ),
            0xbb67f264,
            """
            android/content/res/Configuration.class:
            H4sIAAAAAAAA/1WPTU7DQAyFn2ma0NA/se+iu8KCuQBCQkUIJAQLUFlPEpO6
            Kh40nXAvVkgsOACHQkwCGxZ+9vssPdlf3x+fAM4xzbGHXoZkiD5SwnRjX63Z
            Wq3NXbHhMhDSU1EJZ4Te4miVYZ8wt1p5J5UpnQbWYDzvzNLpk9SNt0GcEka7
            0jPro1RhffFCoGvC+JddsdTr0MJk6SomTG5E+bZ5Ltg/2GIbSX7vGl/ypbTm
            8F/0SXsh5sji4YhFGCCPE+Hgr1P7StRhdLPOA/3jd9Bbtx5FTTuYRB13IZMf
            blk3qhEBAAA=
            """,
        )
}
