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
        message << ", message: " << errorMsg;
    }
    int throwResult = env->ThrowNew(exceptionClass, message.str().c_str());
    return throwResult == 0;
}

static bool throwIfNoRow(JNIEnv *env, sqlite3_stmt *stmt) {
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

static jint JNICALL nativeThreadSafeMode(
        JNIEnv *env,
        jclass clazz) {
    return sqlite3_threadsafe();
}

static jlong JNICALL nativeOpen(
        JNIEnv *env,
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

static jboolean JNICALL nativeInTransaction(
        JNIEnv *env,
        jclass clazz,
        jlong dbPointer) {
    sqlite3 *db = reinterpret_cast<sqlite3 *>(dbPointer);
    if (sqlite3_get_autocommit(db) == 0) {
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

static jlong JNICALL nativePrepare(
        JNIEnv *env,
        jclass clazz,
        jlong dbPointer,
        jstring sqlString) {
    sqlite3 *db = reinterpret_cast<sqlite3 *>(dbPointer);
    sqlite3_stmt *stmt;
    jsize sqlLength = env->GetStringLength(sqlString);
    // Java / jstring represents a string in UTF-16 encoding.
    const jchar *sql = env->GetStringCritical(sqlString, nullptr);
    int rc = sqlite3_prepare16_v2(db, sql, sqlLength * sizeof(jchar), &stmt, nullptr);
    env->ReleaseStringCritical(sqlString, sql);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(db));
        return 0;
    }
    return reinterpret_cast<jlong>(stmt);
}

static void JNICALL nativeConnectionClose(
        JNIEnv *env,
        jclass clazz,
        jlong dbPointer) {
    sqlite3 *db = reinterpret_cast<sqlite3 *>(dbPointer);
    sqlite3_close_v2(db);
}

static void JNICALL nativeBindBlob(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jbyteArray value) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    jsize valueLength = env->GetArrayLength(value);
    jbyte *blob = static_cast<jbyte *>(env->GetPrimitiveArrayCritical(value, nullptr));
    int rc = sqlite3_bind_blob(stmt, index, blob, valueLength, SQLITE_TRANSIENT);
    env->ReleasePrimitiveArrayCritical(value, blob, JNI_ABORT);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeBindDouble(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jdouble value) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    int rc = sqlite3_bind_double(stmt, index, value);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeBindLong(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jlong value) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    int rc = sqlite3_bind_int64(stmt, index, value);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeBindText(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index,
        jstring value) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    jsize valueLength = env->GetStringLength(value);
    const jchar *text = env->GetStringCritical(value, NULL);
    int rc = sqlite3_bind_text16(stmt, index, text, valueLength * sizeof(jchar), SQLITE_TRANSIENT);
    env->ReleaseStringCritical(value, text);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeBindNull(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    int rc = sqlite3_bind_null(stmt, index);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static jboolean JNICALL nativeStep(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
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

static jbyteArray JNICALL nativeGetBlob(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return nullptr;
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    const void *blob = sqlite3_column_blob(stmt, index);
    if (blob == nullptr && throwIfOutOfMemory(env, stmt)) return nullptr;
    int size = sqlite3_column_bytes(stmt, index);
    if (size == 0 && throwIfOutOfMemory(env, stmt)) return nullptr;
    jbyteArray byteArray = env->NewByteArray(size);
    if (size > 0) {
        env->SetByteArrayRegion(byteArray, 0, size, static_cast<const jbyte *>(blob));
    }
    return byteArray;
}

static jdouble JNICALL nativeGetDouble(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0.0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0.0;
    return sqlite3_column_double(stmt, index);
}

static jlong JNICALL nativeGetLong(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0;
    return sqlite3_column_int64(stmt, index);
}

static jstring JNICALL nativeGetText(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return nullptr;
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    // Java / jstring represents a string in UTF-16 encoding.
    const jchar *text = static_cast<const jchar *>(sqlite3_column_text16(stmt, index));
    if (text == nullptr && throwIfOutOfMemory(env, stmt)) return nullptr;
    size_t length = sqlite3_column_bytes16(stmt, index) / sizeof(jchar);
    if (length == 0 && throwIfOutOfMemory(env, stmt)) return nullptr;
    return env->NewString(text, length);
}

static jint JNICALL nativeGetColumnCount(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    return sqlite3_column_count(stmt);
}

static jstring JNICALL nativeGetColumnName(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfInvalidColumn(env, stmt, index)) return nullptr;
    const char *name = sqlite3_column_name(stmt, index);
    if (name == nullptr) {
        throwOutOfMemoryError(env);
        return nullptr;
    }
    return env->NewStringUTF(name);
}

static jint JNICALL nativeGetColumnType(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer,
        jint index) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    if (throwIfNoRow(env, stmt)) return 0;
    if (throwIfInvalidColumn(env, stmt, index)) return 0;
    return sqlite3_column_type(stmt, index);
}

static void JNICALL nativeReset(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    int rc = sqlite3_reset(stmt);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeClearBindings(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    int rc = sqlite3_clear_bindings(stmt);
    if (rc != SQLITE_OK) {
        throwSQLiteException(env, rc, sqlite3_errmsg(sqlite3_db_handle(stmt)));
    }
}

static void JNICALL nativeStatementClose(
        JNIEnv *env,
        jclass clazz,
        jlong stmtPointer) {
    sqlite3_stmt *stmt = reinterpret_cast<sqlite3_stmt *>(stmtPointer);
    sqlite3_finalize(stmt);
}

static const JNINativeMethod sDriverMethods[] = {
        {"nativeThreadSafeMode", "()I",                    (void *) nativeThreadSafeMode},
        {"nativeOpen",           "(Ljava/lang/String;I)J", (void *) nativeOpen}
};

static const JNINativeMethod sConnectionMethods[] = {
        {"nativeInTransaction", "(J)Z",                   (void *) nativeInTransaction},
        {"nativePrepare",       "(JLjava/lang/String;)J", (void *) nativePrepare},
        {"nativeClose",         "(J)V",                   (void *) nativeConnectionClose}
};

static const JNINativeMethod sStatementMethods[] = {
        {"nativeBindBlob",       "(JI[B)V",                 (void *) nativeBindBlob},
        {"nativeBindDouble",     "(JID)V",                  (void *) nativeBindDouble},
        {"nativeBindLong",       "(JIJ)V",                  (void *) nativeBindLong},
        {"nativeBindText",       "(JILjava/lang/String;)V", (void *) nativeBindText},
        {"nativeBindNull",       "(JI)V",                   (void *) nativeBindNull},
        {"nativeStep",           "(J)Z",                    (void *) nativeStep},
        {"nativeGetBlob",        "(JI)[B",                  (void *) nativeGetBlob},
        {"nativeGetDouble",      "(JI)D",                   (void *) nativeGetDouble},
        {"nativeGetLong",        "(JI)J",                   (void *) nativeGetLong},
        {"nativeGetText",        "(JI)Ljava/lang/String;",  (void *) nativeGetText},
        {"nativeGetColumnCount", "(J)I",                    (void *) nativeGetColumnCount},
        {"nativeGetColumnName",  "(JI)Ljava/lang/String;",  (void *) nativeGetColumnName},
        {"nativeGetColumnType",  "(JI)I",                   (void *) nativeGetColumnType},
        {"nativeReset",          "(J)V",                    (void *) nativeReset},
        {"nativeClearBindings",  "(J)V",                    (void *) nativeClearBindings},
        {"nativeClose",          "(J)V",                    (void *) nativeStatementClose},
};

static int register_methods(JNIEnv *env, const char *className,
                            const JNINativeMethod *methods,
                            int methodCount) {
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_ERR;
    }
    int result = env->RegisterNatives(clazz, methods, methodCount);
    env->DeleteLocalRef(clazz);
    if (result != 0) {
        return JNI_ERR;
    }
    return JNI_OK;
}

jint JNI_OnLoad(JavaVM *vm, void * /* reserved */) {
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6)) {
        return JNI_ERR;
    }

    const int driverMethodCount = sizeof(sDriverMethods) / sizeof(sDriverMethods[0]);
    if (register_methods(env, "androidx/sqlite/driver/bundled/BundledSQLiteDriverKt",
                         sDriverMethods, driverMethodCount) != JNI_OK) {
        return JNI_ERR;
    }
    const int connectionMethodCount = sizeof(sConnectionMethods) / sizeof(sConnectionMethods[0]);
    if (register_methods(env, "androidx/sqlite/driver/bundled/BundledSQLiteConnectionKt",
                         sConnectionMethods, connectionMethodCount) != JNI_OK) {
        return JNI_ERR;
    }
    const int statementMethodCount = sizeof(sStatementMethods) / sizeof(sStatementMethods[0]);
    if (register_methods(env, "androidx/sqlite/driver/bundled/BundledSQLiteStatementKt",
                         sStatementMethods, statementMethodCount) != JNI_OK) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
