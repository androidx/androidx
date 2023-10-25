#include "sqlite3.h"
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif
JNIEXPORT jstring JNICALL
Java_androidx_sqliteMultiplatform_unbundled_SimpleDatabaseSubject_openDatabaseAndReadVersion
    (JNIEnv *env, jobject obj) {
    sqlite3 *db;
    int resultCode;
    /* Open database */
    resultCode = sqlite3_open(":memory:", &db);
    if (resultCode) {
        return (*env)->NewStringUTF(env, sqlite3_errmsg(db));
    }
    sqlite3_stmt *stmt;
    resultCode = sqlite3_prepare_v2(db, "select sqlite_version();", -1, &stmt, NULL);
    if (resultCode) {
        return (*env)->NewStringUTF(env, sqlite3_errmsg(db));
    }
    if (sqlite3_step(stmt) == SQLITE_ROW) {
        return (*env)->NewStringUTF(env, sqlite3_column_text(stmt, 0));
    }
    return (*env)->NewStringUTF(env, "couldn't read db version");
}
#ifdef __cplusplus
}
#endif
