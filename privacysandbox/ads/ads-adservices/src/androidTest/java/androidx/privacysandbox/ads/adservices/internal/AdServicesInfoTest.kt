package androidx.privacysandbox.ads.adservices.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class AdServicesInfoTest {
    @Test
    @SdkSuppress(maxSdkVersion = 32)
    fun testVersionSMinus() {
        assertThat(AdServicesInfo.adServicesVersion()).isEqualTo(0)
    }
}
