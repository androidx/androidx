/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.DaoReturnTypeConverter
import androidx.room3.OperationType
import androidx.room3.integration.kotlintestapp.vo.Either.Left
import androidx.room3.integration.kotlintestapp.vo.Either.Right
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class CustomDaoReturnType<T>(val data: T)

class CustomDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <T> convert(executeAndConvert: suspend () -> T): CustomDaoReturnType<T> {
        val data = executeAndConvert.invoke()
        return createCustomType(data)
    }

    private fun <T> createCustomType(data: T) = CustomDaoReturnType(data)
}

class ResultDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <T> convert(executeAndConvert: suspend () -> T): Result<T> {
        return runCatching { executeAndConvert.invoke() }
    }
}

class EitherDaoReturnTypeConverter {
    @DaoReturnTypeConverter([OperationType.READ, OperationType.WRITE])
    suspend fun <R> convert(executeAndConvert: suspend () -> R): Either<Throwable, R> {
        return try {
            Right(executeAndConvert.invoke())
        } catch (e: Throwable) {
            Left(e)
        }
    }
}

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()
}

@OptIn(ExperimentalContracts::class)
fun <L, R> Either<L, R>.isLeft(): Boolean {
    contract {
        returns(true) implies (this@isLeft is Left<L>)
        returns(false) implies (this@isLeft is Right<R>)
    }
    return this is Left<L>
}

@OptIn(ExperimentalContracts::class)
fun <L, R> Either<L, R>.isRight(): Boolean {
    contract {
        returns(true) implies (this@isRight is Right<R>)
        returns(false) implies (this@isRight is Left<L>)
    }
    return this is Right<R>
}
