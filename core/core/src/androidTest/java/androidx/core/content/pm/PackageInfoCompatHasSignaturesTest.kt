/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.content.pm

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import androidx.core.content.pm.PackageInfoCompatHasSignaturesTest.Companion.Params.QueryType
import androidx.core.content.pm.PackageInfoCompatHasSignaturesTest.MockCerts.MockSignatures
import androidx.core.content.pm.PackageInfoCompatHasSignaturesTest.MockCerts.MockSigningInfo
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.mockito.mockThrowOnUnmocked
import androidx.testutils.mockito.whenever
import com.google.common.truth.Truth.assertThat
import java.security.MessageDigest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito.any
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.eq
import org.mockito.internal.util.reflection.FieldSetter

/**
 * Verifies [PackageInfoCompat.hasSignatures].
 *
 * Due to testability restrictions with the [SigningInfo] and [Signature] classes and infrastructure
 * for install test packages in a device test, this test uses mocked classes to verify the correct
 * method calls. Mocking in general is preferable to signing several test packages as this isolates
 * the test parameters to inside the test class.
 *
 * As final class mocking is only available starting from [Build.VERSION_CODES.P], this test
 * manually runs itself for both [Build.VERSION_CODES.O] and the current device SDK version by
 * swapping [Build.VERSION.SDK_INT].
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@LargeTest
@RunWith(Parameterized::class)
class PackageInfoCompatHasSignaturesTest {

    companion object {
        // Following are random public certs (effectively random strings) as this test does not
        // validate the actual signature integrity. Only the fact that the hashes and comparisons
        // work and return the correct values.

        private const val CERT_1 =
            "2d2d2d2d2d424547494e2043455254494649434154452d2d2d2d2d0a4d494" +
                "9422b44434341574767417749424167495548384f42374c355a53594f7852577056774e454f4c336" +
                "5726c5077774451594a4b6f5a496876634e4151454c0a425141774454454c4d416b4741315545426" +
                "84d4356564d774942634e4d6a41774f5449794d6a4d774d6a557a576867504d7a41794d4441784d6" +
                "a51794d7a41790a4e544e614d413078437a414a42674e5642415954416c56544d4947664d4130474" +
                "35371475349623344514542415155414134474e4144434269514b42675144450a6f3650386341636" +
                "c77734a646e773457415a755a685244795031556473334d5766703738434448344548614d682f393" +
                "54a7941316e5a776e2f644174747375640a6e464356713065592b32736d373663334d454a542b456" +
                "86b443170792f6148324f366c3639314d2b334e7a6a616272752f4c457451364d736232494553454" +
                "2690a7a63415350756a4a635458586b346a6d44535a4d6d6359653259466d506b633151534f31387" +
                "875446a514944415141426f314d775554416442674e56485134450a4667515534746446716839634" +
                "16d4d35707665674d514265476c442b4b774d77487759445652306a42426777466f4155347464467" +
                "1683963416d4d35707665670a4d514265476c442b4b774d7744775944565230544151482f4241557" +
                "7417745422f7a414e42676b71686b6947397730424151734641414f426751436a70535a760a4d546" +
                "76f584c3042304b393577486b61353476685a6c2f5a4c6231427243752f686431746761736766434" +
                "9566d4d34754335614774697a422b4a3335462f4f2b0a5344572b62585854314c634b4951795a625" +
                "66772335537736c39584f5773322f55474a33653739555948473144656f497235367534475074312" +
                "b5338746347500a464b36496e4e42534a56584a325231446b7a754e5843476d63766a4d7a4e426b7" +
                "47034504d773d3d0a2d2d2d2d2d454e442043455254494649434154452d2d2d2d2d0a"

        private const val CERT_2 =
            "2d2d2d2d2d424547494e2043455254494649434154452d2d2d2d2d0a4d494" +
                "9422b4443434157476741774942416749554739426d31332f566c61747370564461486d46574f6c7" +
                "65a696b45774451594a4b6f5a496876634e4151454c0a425141774454454c4d416b4741315545426" +
                "84d4356564d774942634e4d6a41774f5449794d6a4d774d7a4130576867504d7a41794d4441784d6" +
                "a51794d7a417a0a4d4452614d413078437a414a42674e5642415954416c56544d4947664d4130474" +
                "35371475349623344514542415155414134474e4144434269514b42675144510a595875516f67783" +
                "4324c77572b3568656b6f694c50507178655964494250555668743442584d6e494f7835434449665" +
                "96d6461424650645865685546395036340a7974576a2b316963677452776e4c2f62487a525953413" +
                "637514c39492b7a45456e2b7342777779566f51325858644c51546f49394f537a54444375744f4c4" +
                "2430a6f65754f46727373566642676f4d6838685a4f5a31775448442f706c6b38543541384463313" +
                "7505159774944415141426f314d775554416442674e56485134450a466751554c7a5754614673507" +
                "23161526d304166556569704b346d6d75785977487759445652306a42426777466f41554c7a57546" +
                "1467350723161526d3041660a556569704b346d6d7578597744775944565230544151482f4241557" +
                "7417745422f7a414e42676b71686b6947397730424151734641414f42675141496147524d0a4d423" +
                "74c74464957714847542f69766f56572b4f6a58664f477332554f75416455776d7a6b374b7a57727" +
                "874744639616a355250307756637755625654444e740a464c326b4c3171574450513471613333643" +
                "34744325555416b49474b724d514668523839756a303438514c7871386a72466f663447324572755" +
                "85353354d79790a5669573735357038354f50704c635a753939796c2b536d7675633938685170796" +
                "a6f564f6c773d3d0a2d2d2d2d2d454e442043455254494649434154452d2d2d2d2d0a"

        private const val CERT_3 =
            "2d2d2d2d2d424547494e2043455254494649434154452d2d2d2d2d0a4d494" +
                "9422b444343415747674177494241674955484f6f6d736b2b642f79336c4854434e3371675166413" +
                "335646e51774451594a4b6f5a496876634e4151454c0a425141774454454c4d416b4741315545426" +
                "84d4356564d774942634e4d6a41774f5449794d6a4d774d7a4578576867504d7a41794d4441784d6" +
                "a51794d7a417a0a4d5446614d413078437a414a42674e5642415954416c56544d4947664d4130474" +
                "35371475349623344514542415155414134474e4144434269514b42675144520a6355494b3450724" +
                "d396930685834546168485055334c575665677630546668307273785153637042496a73306b6a6a6" +
                "34e78342f31363948674c70476f5a334d0a63424350612f61574a4778794c7145514537774b77644" +
                "a6148596b4b56706e55706a4d313030634b6b6b4a356565336b56414958746f2f6c436b626b554a6" +
                "1730a47334f71307677774936656130707336684350313863693066727844766d6630536e2b54615" +
                "2396a31774944415141426f314d775554416442674e56485134450a466751554464553443534c393" +
                "746516d774954555332444e4472356b464c5177487759445652306a42426777466f4155446455344" +
                "3534c393746516d774954550a5332444e4472356b464c517744775944565230544151482f4241557" +
                "7417745422f7a414e42676b71686b6947397730424151734641414f426751437a567054470a59796" +
                "1444a6c456279447775443457616b38306d5a4153613534646a69446e6335324d30776e614145776" +
                "84e496978623547465a50357878337859302f494c520a6a72544a6e6744377643586c556f5256384" +
                "379794653534169306f3977544b475554434d762b303446324a6c474a4b7665486a346d473544746" +
                "6335331574b520a6644454a792b456376563658314b716a73466d524a4a6d7a30347464525363304" +
                "c74622f2f673d3d0a2d2d2d2d2d454e442043455254494649434154452d2d2d2d2d0a"

        private const val CERT_4 =
            "2d2d2d2d2d424547494e2043455254494649434154452d2d2d2d2d0a4d494" +
                "9422b444343415747674177494241674955526f49427173485858413246636938324d412b73706d5" +
                "6684d7634774451594a4b6f5a496876634e4151454c0a425141774454454c4d416b4741315545426" +
                "84d4356564d774942634e4d6a41774f5449304d546b784d7a4d77576867504d7a41794d4441784d6" +
                "a59784f54457a0a4d7a42614d413078437a414a42674e5642415954416c56544d4947664d4130474" +
                "35371475349623344514542415155414134474e4144434269514b426751432b0a306549525344554" +
                "e4972666663486f4d61697431705a6b39534769616c41694e56484b6e4950466876754233497a475" +
                "05a4d476f6d6a3956534667766e7047360a4f7166453033734e575949503944776772485546692f6" +
                "e356f45504f742f617643746b4b71623957737531777643746b37795163354d626276644e6b78344" +
                "c740a3679724a7151545946424479356c49624c67454b4d744a5344584246356a38747173326e705" +
                "145514f774944415141426f314d775554416442674e56485134450a4667515557354c6e5751344f3" +
                "2523576515731355452564955726f744e476b77487759445652306a42426777466f415557354c6e5" +
                "751344f32523576515731350a5452564955726f744e476b7744775944565230544151482f4241557" +
                "7417745422f7a414e42676b71686b6947397730424151734641414f42675141685768654f0a77525" +
                "85339365536444a705459597374754741634a77414e434d3244503938325653613136766e7769653" +
                "842477a6a724a51794f354e4a4846637a4f566e54330a626834496a65337751787551334138566e4" +
                "54d334230683553373030524c524337373936555a787465683874304f6c7a515031703358452b776" +
                "571797a6c4e330a4f494f435a486f6b494b6f4957527964734e58547a55353448625850597275556" +
                "e71574451673d3d0a2d2d2d2d2d454e442043455254494649434154452d2d2d2d2d0a"

        private const val TEST_PKG_NAME = "com.example.app"

        private val nullCerts: List<Certificate>? = null
        private val emptyCerts = emptyList<Certificate>()
        private val multiSignerCerts = listOf(CERT_1, CERT_3).map(::Certificate)
        private val pastHistoryCerts = listOf(CERT_1, CERT_2, CERT_3).map(::Certificate)
        private val noHistoryCerts = listOf(CERT_1).map(::Certificate)
        private val extraCert = Certificate(CERT_4)

        data class Params(
            val sdkVersion: Int,
            val mockCerts: MockCerts,
            val queryType: QueryType,
            val certType: CertType,
            val matchExact: Boolean
        ) {
            enum class CertType(val flag: Int) {
                X509(PackageManager.CERT_INPUT_RAW_X509),
                SHA256(PackageManager.CERT_INPUT_SHA256)
            }

            enum class QueryType {
                NONE,
                EXACT_COUNT,
                FEWER,
                MORE
            }

            val queryCerts =
                when (queryType) {
                    QueryType.NONE -> emptyList()
                    QueryType.EXACT_COUNT -> mockCerts.certificates.orEmpty()
                    QueryType.FEWER -> mockCerts.certificates!!.drop(1)
                    QueryType.MORE -> mockCerts.certificates.orEmpty() + extraCert
                }

            val success =
                when (mockCerts.certificates) {
                    // If the certs returned in the package are null/empty, the query can never
                    // succeed
                    nullCerts,
                    emptyCerts -> false
                    // Otherwise success depends on what the query set is
                    else ->
                        when (queryType) {
                            // None always fails, to ensure verify cannot accidentally succeed
                            QueryType.NONE -> false
                            // If querying the exact same certs, then always succeed
                            QueryType.EXACT_COUNT -> true
                            // Otherwise if querying fewer, only succeed if not matching exactly all
                            QueryType.FEWER -> !matchExact
                            // Otherwise matching more than available, which should always fail
                            QueryType.MORE -> false
                        }
                }

            // For naming the test method variant
            override fun toString(): String {
                val certsVariant =
                    when (mockCerts.certificates) {
                        nullCerts -> "null"
                        emptyCerts -> "empty"
                        multiSignerCerts -> "multiSign"
                        pastHistoryCerts -> "pastHistory"
                        noHistoryCerts -> "noHistory"
                        else -> throw IllegalArgumentException("Invalid mockCerts $mockCerts")
                    }

                @Suppress("DEPRECATION")
                val queryFlag =
                    when (val flag = mockCerts.flag) {
                        PackageManager.GET_SIGNATURES -> "GET_SIGNATURES"
                        PackageManager.GET_SIGNING_CERTIFICATES -> "GET_SIGNING_CERTIFICATES"
                        else -> throw IllegalArgumentException("Invalid certs type $flag")
                    }

                val sdkVersionName =
                    sdkVersion.takeUnless { it == Build.VERSION.SDK_INT } ?: "current"

                return "$queryFlag," +
                    "$certsVariant${certType.name}," +
                    "sdkVersion=$sdkVersionName," +
                    "query=$queryType," +
                    "matchExact=$matchExact"
            }
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun parameters(): Array<Params> {
            return listOf(
                    listOf(
                            MockSignatures(nullCerts),
                            MockSignatures(emptyCerts),
                            MockSignatures(multiSignerCerts),
                            // Legacy GET_SIGNATURES cannot include certificate history
                            MockSignatures(noHistoryCerts)
                        )
                        .associateWith { Build.VERSION_CODES.O_MR1 },
                    listOf(
                            MockSigningInfo(
                                multiSigners = false,
                                hasHistory = null,
                                contentsSigners = null,
                                certHistory = null
                            ),
                            MockSigningInfo(
                                multiSigners = false,
                                hasHistory = null,
                                contentsSigners = null,
                                certHistory = emptyCerts
                            ),
                            MockSigningInfo(
                                multiSigners = true,
                                hasHistory = null,
                                contentsSigners = multiSignerCerts,
                                certHistory = null
                            ),
                            MockSigningInfo(
                                multiSigners = false,
                                hasHistory = true,
                                contentsSigners = null,
                                certHistory = pastHistoryCerts
                            ),
                            MockSigningInfo(
                                multiSigners = false,
                                hasHistory = false,
                                contentsSigners = null,
                                certHistory = noHistoryCerts
                            )
                        )
                        .associateWith { Build.VERSION.SDK_INT }
                )
                .flatMap {
                    // Multiply all base params by QueryType, CertType and matchExact values to
                    // get the complete set of possibilities
                    it.entries.flatMap { (mockCerts, sdkVersion) ->
                        listOfNotNull(
                                QueryType.NONE,
                                QueryType.EXACT_COUNT,
                                QueryType.MORE,
                                QueryType.FEWER.takeIf {
                                    val certificates = mockCerts.certificates
                                    !certificates.isNullOrEmpty() && certificates.size > 1
                                }
                            )
                            .flatMap { queryType ->
                                listOf(Params.CertType.X509, Params.CertType.SHA256).flatMap {
                                    certType ->
                                    listOf(true, false).map { matchExact ->
                                        Params(
                                            sdkVersion,
                                            mockCerts,
                                            queryType,
                                            certType,
                                            matchExact
                                        )
                                    }
                                }
                            }
                    }
                }
                .toTypedArray()
        }

        private val sdkIntField = Build.VERSION::class.java.getDeclaredField("SDK_INT")

        private fun setDeviceSdkVersion(sdkVersion: Int) {
            FieldSetter.setField(Build.VERSION::class.java, sdkIntField, sdkVersion)
            assertThat(Build.VERSION.SDK_INT).isEqualTo(sdkVersion)
        }
    }

    @Parameterized.Parameter(0) lateinit var params: Params

    private var savedSdkVersion: Int = Build.VERSION.SDK_INT

    @Before
    fun saveSdkVersion() {
        savedSdkVersion = Build.VERSION.SDK_INT
    }

    @After
    fun resetSdkVersion() {
        if (Build.VERSION.SDK_INT != savedSdkVersion) {
            setDeviceSdkVersion(savedSdkVersion)
        }
    }

    @Test
    fun verify() {
        val mock = mockPackageManager()
        val certs =
            params.queryCerts
                .map { it.bytes(params.certType) }
                .associateWith { params.certType.flag }

        // SDK_INT must be changed after mocks are built, since MockMaker will do an SDK check
        if (Build.VERSION.SDK_INT != params.sdkVersion) {
            setDeviceSdkVersion(params.sdkVersion)
        }

        assertThat(PackageInfoCompat.hasSignatures(mock, TEST_PKG_NAME, certs, params.matchExact))
            .isEqualTo(params.success)

        if (Build.VERSION.SDK_INT != savedSdkVersion) {
            setDeviceSdkVersion(savedSdkVersion)
        }
    }

    @Suppress("DEPRECATION")
    private fun mockPackageManager() =
        mockThrowOnUnmocked<PackageManager> {
            val mockCerts = params.mockCerts
            whenever(getPackageInfo(TEST_PKG_NAME, params.mockCerts.flag)) {
                PackageInfo().apply {
                    when (mockCerts) {
                        is MockSignatures -> {
                            @Suppress("DEPRECATION")
                            signatures =
                                mockCerts.certificates?.map { it.signature }?.toTypedArray()
                        }
                        is MockSigningInfo -> {
                            signingInfo =
                                mockThrowOnUnmocked<SigningInfo> {
                                    whenever(hasMultipleSigners()) { mockCerts.multiSigners }

                                    mockCerts.hasHistory?.let {
                                        // Only allow this method if params specify it. to ensure
                                        // past
                                        // certificates aren't considered when multi-signing is
                                        // enabled
                                        whenever(hasPastSigningCertificates()) { it }
                                    }

                                    mockCerts.contentsSigners
                                        ?.map { it.signature }
                                        ?.toTypedArray()
                                        ?.let { whenever(apkContentsSigners) { it } }

                                    // Only allow fetching history if not multi signed
                                    if (!hasMultipleSigners()) {
                                        whenever(signingCertificateHistory) {
                                            mockCerts.certHistory
                                                ?.map { it.signature }
                                                ?.toTypedArray()
                                        }
                                    }
                                }
                        }
                    }
                }
            }

            if (!params.matchExact && params.sdkVersion >= Build.VERSION_CODES.P) {
                whenever(hasSigningCertificate(eq(TEST_PKG_NAME), any(), anyInt())) {
                    val certs = params.mockCerts.certificates?.asSequence() ?: return@whenever false
                    val query = getArgument(1) as ByteArray
                    val certType =
                        when (val type = getArgument(2) as Int) {
                            PackageManager.CERT_INPUT_RAW_X509 -> Params.CertType.X509
                            PackageManager.CERT_INPUT_SHA256 -> Params.CertType.SHA256
                            else -> throw IllegalArgumentException("Invalid type $type")
                        }
                    certs.map { it.bytes(certType) }.contains(query)
                }
            }
        }

    sealed class MockCerts {
        abstract val certificates: List<Certificate>?
        abstract val flag: Int

        data class MockSignatures(override val certificates: List<Certificate>?) : MockCerts() {
            @Suppress("DEPRECATION") override val flag = PackageManager.GET_SIGNATURES
        }

        data class MockSigningInfo(
            val multiSigners: Boolean,
            val hasHistory: Boolean?,
            val contentsSigners: List<Certificate>?,
            val certHistory: List<Certificate>?
        ) : MockCerts() {
            override val certificates = contentsSigners ?: certHistory
            override val flag = PackageManager.GET_SIGNING_CERTIFICATES
        }
    }

    /** [Signature] wrapper to cache arrays and digests. */
    data class Certificate(val publicCertX509: String) {
        val signature = Signature(publicCertX509)
        private val x509Bytes = signature.toByteArray()!!
        private val sha256Bytes = MessageDigest.getInstance("SHA256").digest(x509Bytes)

        fun bytes(certType: Params.CertType): ByteArray =
            when (certType) {
                Params.CertType.X509 -> x509Bytes
                Params.CertType.SHA256 -> sha256Bytes
            }
    }
}
