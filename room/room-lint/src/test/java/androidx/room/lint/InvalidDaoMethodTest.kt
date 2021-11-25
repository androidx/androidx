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
@file:Suppress("UnstableApiUsage")

package androidx.room.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import org.junit.Test

class InvalidDaoMethodTest {

    private fun check(testFile: TestFile): TestLintResult {
        return lint().files(
            java(Stubs.RX_JAVA2_COMPLETABLE),
            java(Stubs.RX_JAVA3_COMPLETABLE),
            java(Stubs.DAO_ANNOTATION),
            java(Stubs.INSERT_ANNOTATION),
            testFile
        ).issues(InvalidDaoMethodDetector.ISSUE).run()
    }

    @Test
    fun testValidSuspendDaoMethod() {
        val input = kotlin(
            """
                package androidx.sample
                
                import androidx.room.Dao
                import androidx.room.Insert
                
                @Dao
                interface TestDao {
                    
                    @Insert
                    suspend fun foo()
                }
            """.trimIndent()
        )
        check(input).expect("No warnings.")
    }

    @Test
    fun testValidRxJava2TypeReturningDaoMethod() {
        val input = kotlin(
            """
                package androidx.sample
                
                import io.reactivex.Completable
                import androidx.room.Dao
                import androidx.room.Insert
                
                @Dao
                interface TestDao {
                    
                    @Insert
                    fun foo(): Completable
                }
            """.trimIndent()
        )
        check(input).expect("No warnings.")
    }

    @Test
    fun testValidRxJava3TypeReturningDaoMethod() {
        val input = kotlin(
            """
                package androidx.sample
                
                import io.reactivex.rxjava3.core.Completable
                import androidx.room.Dao
                import androidx.room.Insert
                
                @Dao
                interface TestDao {
                    
                    @Insert
                    fun foo(): Completable
                }
            """.trimIndent()
        )
        check(input).expect("No warnings.")
    }

    @Test
    fun testInvalidSuspendRxJava2TypeReturningDaoMethod() {
        val input = kotlin(
            """
                package androidx.sample
                
                import io.reactivex.Completable
                import androidx.room.Dao
                import androidx.room.Insert
                
                @Dao
                interface TestDao {
                    
                    @Insert
                    suspend fun foo(): Completable
                }
            """.trimIndent()
        )

        val expected = """
            src/androidx/sample/TestDao.kt:11: Warning: Invalid Dao method. [InvalidDaoMethod]
                suspend fun foo(): Completable
                            ~~~
            0 errors, 1 warnings
        """.trimIndent()
        check(input).expect(expected)
    }

    @Test
    fun testInvalidSuspendRxJava3TypeReturningDaoMethod() {
        val input = kotlin(
            """
                package androidx.sample
                
                import io.reactivex.rxjava3.core.Completable
                import androidx.room.Dao
                import androidx.room.Insert
                
                @Dao
                interface TestDao {
                    
                    @Insert
                    suspend fun foo(): Completable
                }
            """.trimIndent()
        )

        val expected = """
            src/androidx/sample/TestDao.kt:11: Warning: Invalid Dao method. [InvalidDaoMethod]
                suspend fun foo(): Completable
                            ~~~
            0 errors, 1 warnings
        """.trimIndent()
        check(input).expect(expected)
    }
}
