#include <jni.h>
#include "sqlite3.h"
#include <sstream>
#include <stdlib.h>

/**
 * Throws SQLiteException with the given error code and message.
 *
 * @return true if the exception was thrown, otherwise false.
 */
static bool throwSQLiteException(JNIEnv *env, int errorCode, const char *errorMsg) {
    jclass exceptionClass = env->FindClass("androidx/sqlite/SQLiteException");
    if (exceptionClass == nullptr) {
        // If androidx's exception isn't found we are likely in Android's native where the
        // actual exception is type aliased. Clear the ClassNotFoundException and instead find
        // and throw Android's exception.
        env->ExceptionClear();
        exceptionClass = env->FindClass("android/database/SQLException");
    }
    std::stringstream message;
    message << "Error code: " << errorCode;
    if (errorMsg != nullptr) {
        message << ", message: " <<  errorMsg;
    }
    int throwResult = env->ThrowNew(exceptionClass, message.str().c_str());
    return throwResult == 0;
}

static bool throwIfNoRow(JNIEnv *env, sqlite3_stmt* stmt) {
    if (sqlite3_stmt_busy(stmt) == 0) {
        return throwSQLiteException(env, SQLITE_MISUSE, "no row");
    }
    return false;
}

static bool throwIfInvalidColumn(JNIEnv *env, sqlite3_stmt *stmt, int index) {
    if (index < 0 || index >= sqlite3_column_count(stmt)) {
        return throwSQLiteException(env, SQLITE_RANGE, "column index out of range");
    }
    return false;
}

static bool throwOutOfMemoryError(JNIEnv *env) {
    jclass exceptionClass = env->FindClass("java/lang/OutOfMemoryError");
    int throwResult = env->ThrowNew(exceptionClass, nullptr);
    return throwResult == 0;
}

static bool throwIfOutOfMemory(JNIEnv *env, sqlite3_stmt *stmt) {
    int lastRc = sqlite3_errcode(sqlite3_db_handle(stmt));
    if (lastRc == SQLITE_NOMEM) {
        return throwOutOfMemoryError(env);
    }
    return false;
}

extern "C" JNIEXPORT jint JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteDriverKt_nativeThreadSafeMode(
        JNIEnv* env,
        jclass clazz) {
    return sqlite3_threadsafe();
}

extern "C" JNIEXPORT jlong JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteDriverKt_nativeOpen(
        JNIEnv* env,
        jclass clazz,
        jstring name,
        int openFlags) {
    const char *path = env->GetStringUTFChars(name, nullptr);
    sqlite3 *db;
    int rc = sqlite3_open_v2(path, &db, openFlags, nullptr);
    env->ReleaseStringUTFChars(name, path);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, nullptr);
        return 0;
    }
    return reinterpret_cast<jlong>(db);
}

extern "C" JNIEXPORT jlong JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteConnectionKt_nativePrepare(
        JNIEnv* env,
        jclass clazz,
        jlong dbPointer,
        jstring sqlString) {
    sqlite3* db = reinterpret_cast<sqlite3*>(dbPointer);
    sqlite3_stmt* stmt;
    jsize sqlLength = env->GetStringLength(sqlString);
    // Java / jstring represents a string in UTF-16 encoding.
    const jchar* sql = env->GetStringCritical(sqlString, nullptr);
    int rc = sqlite3_prepare16_v2(db, sql, sqlLength * sizeof(jchar), &stmt, nullptr);
    env->ReleaseStringCritical(sqlString, sql);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(db));
        return 0;
    }
    return reinterpret_cast<jlong>(stmt);
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteConnectionKt_nativeClose(
        JNIEnv* env,
        jclass clazz,
        jlong dbPointer) {
    sqlite3 *db = reinterpret_cast<sqlite3*>(dbPointer);
    sqlite3_close_v2(db);
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeBindBlob(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jbyteArray value) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    jsize valueLength = env->GetArrayLength(value);
    jbyte* blob = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(value, nullptr));
    int rc = sqlite3_bind_blob(stmt, index, blob, valueLength, SQLITE_TRANSIENT);
    env->ReleasePrimitiveArrayCritical(value, blob, JNI_ABORT);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeBindDouble(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jdouble value) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_bind_double(stmt, index, value);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeBindLong(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jlong value) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_bind_int64(stmt, index, value);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeBindText(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jstring value) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    jsize valueLength = env->GetStringLength(value);
    const jchar* text = env->GetStringCritical(value, NULL);
    int rc = sqlite3_bind_text16(stmt, index, text, valueLength * sizeof(jchar), SQLITE_TRANSIENT);
    env->ReleaseStringCritical(value, text);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeBindNull(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_bind_null(stmt, index);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeStep(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_ROW) {
        return JNI_TRUE;
    }
    if (rc == SQLITE_DONE) {
        return JNI_FALSE;
    }
    throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    return JNI_FALSE;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetBlob(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return nullptr;
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    const void *blob = sqlite3_column_blob(stmt, index);
    if (blob == nullptr && throwIfOutOfMemory(env, stmt)) return nullptr;
    int size = sqlite3_column_bytes(stmt, index);
    if (size == 0 && throwIfOutOfMemory(env, stmt)) return nullptr;
    jbyteArray byteArray = env->NewByteArray(size);
    if (size > 0) {
        env->SetByteArrayRegion(byteArray, 0, size, static_cast<const jbyte*>(blob));
    }
    return byteArray;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetDouble(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0.0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0.0;
    return sqlite3_column_double(stmt, index);
}

extern "C" JNIEXPORT jlong JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetLong(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0;
    return sqlite3_column_int64(stmt, index);
}

extern "C" JNIEXPORT jstring JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetText(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return nullptr;
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    // Java / jstring represents a string in UTF-16 encoding.
    const jchar *text = static_cast<const jchar*>(sqlite3_column_text16(stmt, index));
    if (text == nullptr && throwIfOutOfMemory(env, stmt)) return nullptr;
    size_t length = sqlite3_column_bytes16(stmt, index) / sizeof(jchar);
    if (length == 0 && throwIfOutOfMemory(env, stmt)) return nullptr;
    return env->NewString(text, length);
}

extern "C" JNIEXPORT jint JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetColumnCount(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    return sqlite3_column_count(stmt);
}

extern "C" JNIEXPORT jstring JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetColumnName(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    const char *name = sqlite3_column_name(stmt, index);
    if (name == nullptr) {
        throwOutOfMemoryError(env);
        return nullptr;
    }
    return env->NewStringUTF(name);
}

extern "C" JNIEXPORT jint JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeGetColumnType(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0;
    return sqlite3_column_type(stmt, index);
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeReset(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_reset(stmt);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeClearBindings(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    int rc = sqlite3_clear_bindings(stmt);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

extern "C" JNIEXPORT void JNICALL
Java_androidx_sqlite_driver_bundled_BundledSQLiteStatementKt_nativeClose(
        JNIEnv* env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt* stmt = reinterpret_cast<sqlite3_stmt*>(stmtPointer);
    sqlite3_finalize(stmt);
}
