package com.mysdk

import com.mysdk.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class TestSandboxSdkClientProxy(
    public val remote: ITestSandboxSdk,
) : TestSandboxSdk {
    public override suspend fun doSomethingAsync(
        first: Int,
        second: String,
        third: Long,
    ): Boolean = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IBooleanTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: Boolean) {
                it.resumeWith(Result.success(result))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.doSomethingAsync(first, second, third, transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override fun echoBoolean(input: Boolean) {
        remote.echoBoolean(input)
    }

    public override fun echoChar(input: Char) {
        remote.echoChar(input)
    }

    public override fun echoDouble(input: Double) {
        remote.echoDouble(input)
    }

    public override fun echoFloat(input: Float) {
        remote.echoFloat(input)
    }

    public override fun echoInt(input: Int) {
        remote.echoInt(input)
    }

    public override fun echoLong(input: Long) {
        remote.echoLong(input)
    }

    public override fun echoString(input: String) {
        remote.echoString(input)
    }

    public override suspend fun processBooleanList(x: List<Boolean>): List<Boolean> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListBooleanTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: BooleanArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processBooleanList(x.toBooleanArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processCharList(x: List<Char>): List<Char> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListCharTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: CharArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processCharList(x.toCharArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processDoubleList(x: List<Double>): List<Double> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListDoubleTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: DoubleArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processDoubleList(x.toDoubleArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processFloatList(x: List<Float>): List<Float> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListFloatTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: FloatArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processFloatList(x.toFloatArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processIntList(x: List<Int>): List<Int> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListIntTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IntArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processIntList(x.toIntArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processLongList(x: List<Long>): List<Long> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListLongTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: LongArray) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processLongList(x.toLongArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processNullableInt(x: Int?): Int? = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListIntTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IntArray) {
                it.resumeWith(Result.success(result.firstOrNull()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processNullableInt(if (x == null) intArrayOf() else intArrayOf(x),
                transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processShortList(x: List<Short>): List<Short> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListShortTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IntArray) {
                it.resumeWith(Result.success(result.map { it.toShort() }.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processShortList(x.map { it.toInt() }.toIntArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processStringList(x: List<String>): List<String> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListStringTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: Array<String>) {
                it.resumeWith(Result.success(result.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processStringList(x.toTypedArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override fun receiveAndReturnNothing() {
        remote.receiveAndReturnNothing()
    }

    public override suspend fun receiveAndReturnNothingAsync(): Unit = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IUnitTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess() {
                it.resumeWith(Result.success(Unit))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.receiveAndReturnNothingAsync(transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override fun receiveMultipleArguments(
        first: Int,
        second: String,
        third: Long,
    ) {
        remote.receiveMultipleArguments(first, second, third)
    }
}
