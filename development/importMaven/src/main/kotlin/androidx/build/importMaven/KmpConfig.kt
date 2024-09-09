package androidx.build.importMaven

import org.jetbrains.kotlin.konan.target.KonanTarget

/** Common configuration for KMP builds. */
internal object KmpConfig {
    /** host machines where we support compiling KMP */
    val SUPPORTED_HOSTS =
        listOf(
            KonanTarget.LINUX_X64,
            KonanTarget.MACOS_ARM64,
            KonanTarget.MACOS_X64,
        )

    /** Supported konan targets */
    val SUPPORTED_KONAN_TARGETS =
        listOf(
            KonanTarget.MACOS_ARM64,
            KonanTarget.MACOS_X64,
            KonanTarget.LINUX_ARM64,
            KonanTarget.LINUX_X64,
            KonanTarget.MINGW_X64,
            KonanTarget.IOS_ARM64,
            KonanTarget.IOS_SIMULATOR_ARM64,
            KonanTarget.IOS_X64,
            KonanTarget.WATCHOS_ARM32,
            KonanTarget.WATCHOS_ARM64,
            KonanTarget.WATCHOS_SIMULATOR_ARM64,
            KonanTarget.WATCHOS_X64,
            KonanTarget.WATCHOS_DEVICE_ARM64,
            KonanTarget.TVOS_ARM64,
            KonanTarget.TVOS_SIMULATOR_ARM64,
            KonanTarget.TVOS_X64,
            KonanTarget.ANDROID_X86,
            KonanTarget.ANDROID_X64,
            KonanTarget.ANDROID_ARM32,
            KonanTarget.ANDROID_ARM64,
        )
}
