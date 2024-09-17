package com.sdk

import androidx.privacysandbox.activity.core.SdkActivityLauncher

public interface MyInterface {
    public suspend fun add(x: Int, y: Int): Int

    public fun doSomething(firstInterface: MyInterface, secondInterface: MySecondInterface)

    public fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?)

    public fun doSomethingWithSdkActivityLauncher(launcher: SdkActivityLauncher)

    public companion object {
        public const val MY_CONST_INT: Int = 7

        public const val MY_CONST_BOOL: Boolean = true

        public const val MY_CONST_STR: String = "tea > coffee"

        public const val MY_CONST_MULTILINE_STR: String =
                "\n            {}'${'$'}${'$'}${'$'}${'$'}\"*oss\n            snoateusoeut\n            tea > coffee\n        "

        public const val MY_CONST_SHORT: Short = 42

        public const val MY_CONST_CHAR_1: Char = '\''

        public const val MY_CONST_CHAR_2: Char = '\"'

        public const val MY_CONST_CHAR_3: Char = '!'
    }
}
