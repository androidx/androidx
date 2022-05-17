/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import org.junit.Assert
import org.junit.Test

class AndroidXImplPluginTest {

    @Test
    fun testRemoveTargetSdkVersion() {
        /* ktlint-disable max-line-length */
        val manifest = """
<?xml version="1.0" encoding="utf-8"?>
<!--
 Copyright (C) 2014 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.core" >

    <uses-sdk
        android:minSdkVersion="14"
        android:targetSdkVersion="31" />

    <application android:appComponentFactory="androidx.core.app.CoreComponentFactory" />

</manifest>
        """

        // Expect that the android:targetSdkVersion element is removed.
        val expected = """
<?xml version="1.0" encoding="utf-8"?>
<!--
 Copyright (C) 2014 The Android Open Source Project

     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
-->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.core" >

    <uses-sdk
        android:minSdkVersion="14" />

    <application android:appComponentFactory="androidx.core.app.CoreComponentFactory" />

</manifest>
        """
        /* ktlint-enable max-line-length */

        val actual = removeTargetSdkVersion(manifest)
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testRemoveLineAndColumnAttributes() {
        /* ktlint-disable max-line-length */
        val baseline = """
<?xml version="1.0" encoding="UTF-8"?>
<issues format="6" by="lint 7.4.0-alpha01" type="baseline" client="gradle" dependencies="false" name="AGP (7.4.0-alpha01)" variant="all" version="7.4.0-alpha01">

    <issue
        id="NewApi"
        message="Call requires API level 24 (current min is 14): `java.util.Optional#of`"
        errorLine1="                return Optional.of(modeCompat);"
        errorLine2="                                ~~">
        <location
            file="src/test/java/androidx/core/view/DisplayCompatTest.java"
            line="74"
            column="33"/>
    </issue>

</issues>
        """

        // Expect that the line and column attributes are removed.
        val expected = """
<?xml version="1.0" encoding="UTF-8"?>
<issues format="6" by="lint 7.4.0-alpha01" type="baseline" client="gradle" dependencies="false" name="AGP (7.4.0-alpha01)" variant="all" version="7.4.0-alpha01">

    <issue
        id="NewApi"
        message="Call requires API level 24 (current min is 14): `java.util.Optional#of`"
        errorLine1="                return Optional.of(modeCompat);"
        errorLine2="                                ~~">
        <location
            file="src/test/java/androidx/core/view/DisplayCompatTest.java"/>
    </issue>

</issues>
        """
        /* ktlint-enable max-line-length */

        val actual = removeLineAndColumnAttributes(baseline)
        Assert.assertEquals(expected, actual)
    }
}
