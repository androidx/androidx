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

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

public class Context {
    public Resources getResources() {
        return null;
    }

    public final CharSequence getText(int resId) {
        return null;
    }

    public final String getString(int resId) {
        return null;
    }

    public final String getString(int resId, Object... formatArgs) {
        return null;
    }

    public final int getColor(int id) {
        return -1;
    }

    public final Drawable getDrawable(int id) {
        return null;
    }

    public final ColorStateList getColorStateList(int id) {
        return null;
    }
}
        """
            ),
            0x136ce764,
            """
                android/content/Context.class:
                H4sIAAAAAAAA/4XRy07CQBQG4H+43+SmgGI0wY3FhX0AjInBmJA0mljixtVQ
                JlCCrU4H9TV8Dxe6MnHhA/hQxtMCCg0JXcy002/+mTPz/fP5BeAM9QwiiCYR
                yyGOBENxxB+5PubOQL/qjYSlGBIntmOrU4ao1rxJIsVQ405funZft1xHCUfp
                bb9/Jhtru33BUDBsR1xO7npCdnlvTCO5gVDXwnMn0hIeQ0NrGuEQKTz9j7QY
                kjSlG6Tuap2m8b+x9pBLUzxMhGMJcmlyppK2M2CoLMvpMBlN69wa4dJaK2mK
                4tru2JVUDaV1GLI0ci7507SSQ3+J+d4Hkt8PbcvT+7P/+hxSUmmeZCquhGF7
                KjR7sfJlSLMzZnASF3ZwfLMTPvY3jAaSdGv+kwLz743aNH3tUc+ojx99gL3T
                C6VQmwgGfZpFbkbrMxphbyGXQQkbyC+4yEqXJ1dAca0r44VcacFFV7oqmTI2
                w3mR15Crk9tCZe26++SqqK11B9Ru+3+w8wv/BgaFFgMAAA==
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

    val Drawable =
        bytecode(
            "libs/android.jar",
            java(
                """
package android.graphics.drawable;

public class Drawable {}

        """
            ),
            0xb5ee694f,
            """
            android/graphics/drawable/Drawable.class:
            H4sIAAAAAAAA/zv1b9c+BgYGRwZeLgYmBmZ2BhYeBlYGNkYGgazEskT9nMS8
            dH3/pKzU5BJGBjabzLzMEjtGBmYNzTB2Bg5GBqXEvJSi/MwU/fSixIKMzORi
            /ZSixPLEpJxUfRcog5GBxTk/BUjx+2TmpfqV5ialFoVAJLiC80uLklPdMkEc
            XpgGPZDFDIoM7ED3gAAjEAJdBCQ5gTxZMJ+BgVVrOwPjRrA0F5BkAwuyAElu
            IM3EwAMA4UOwktgAAAA=
            """,
        )

    val ColorStateList =
        bytecode(
            "libs/android.jar",
            java(
                """
package android.content.res;

public class ColorStateList {}

        """
            ),
            0xf625aa4f,
            """
            android/content/res/ColorStateList.class:
            H4sIAAAAAAAA/1VOuwrCQBCci3lojChYW2ilFt4PiCABK9EiYn9JDjmJd3Ce
            /peVYOEH+FHiJp27MMvMMsx8vq83gDV6MTy0IvgJAoQMg7O4C14JfeL7/CwL
            xxAulVZuxdCazo4R2gwToUtrVMkLo53Ujlt55ampjM2ccHKrrmTzU1NKhv5W
            abm7XXJpDyKvSIkzc7OF3KiaDP9tizoeY0TUqh5GS70IO8RGDQeC+RPs0bxj
            wrARfcIuXQ/JD3ogafXeAAAA
            """,
        )
}
