package com.mysdk

import androidx.privacysandbox.tools.PrivacySandboxCallback
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import com.myotherpackage.MyOtherPackageDataClass
import com.myotherpackage.MyOtherPackageInterface

@PrivacySandboxService
interface MySdk {
    suspend fun doStuff(x: Int, y: Int): String

    suspend fun getMyInterface(): MyMainPackageInterface

    suspend fun getMyOtherPackageInterface(): MyOtherPackageInterface
}

@PrivacySandboxInterface
interface MyMainPackageInterface {
    suspend fun doIntStuff(x: List<Int>): List<Int>

    suspend fun useDataClass(x: MyOtherPackageDataClass): MyOtherPackageDataClass
}
