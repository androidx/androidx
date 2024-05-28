package androidx.health.services.client

import android.content.Intent
import androidx.health.services.client.impl.IpcConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VersionApiServiceTest {
    private lateinit var stub: VersionApiService.VersionApiServiceStub

    @Before
    fun setUp() {
        val intent = Intent(IpcConstants.VERSION_API_BIND_ACTION)

        stub =
            Robolectric.buildService(VersionApiService::class.java).create().get().onBind(intent)
                as VersionApiService.VersionApiServiceStub
    }

    @Test
    fun canonicalSdkVersionIsCorrect() {
        assertThat(stub.sdkVersion).isEqualTo(28)
    }

    @Test
    fun versionApiServiceVersionIsCorrect() {
        assertThat(stub.versionApiServiceVersion).isEqualTo(1)
    }
}
