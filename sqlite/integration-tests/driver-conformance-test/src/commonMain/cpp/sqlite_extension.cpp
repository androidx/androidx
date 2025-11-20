#include "sqlite3ext.h"
SQLITE_EXTENSION_INIT1

/**
 * A sample SQL function that return a TEXT message.
 */
static void helloWorld(
        sqlite3_context *context,
        int argc,
        sqlite3_value **argv) {
    sqlite3_result_text(context, "Hello from sqlite_extension.cpp!", -1, SQLITE_STATIC);
}

/**
 * SQLite extension initialization function.
 * See https://www.sqlite.org/loadext.html
 */
#ifdef _WIN32
__declspec(dllexport)
#endif
extern "C" int sqlite3_test_extension_init(
        sqlite3 *db,
        char **pzErrMsg,
        const sqlite3_api_routines *pApi) {
    int rc = SQLITE_OK;
    SQLITE_EXTENSION_INIT2(pApi);
    rc = sqlite3_create_function(
            /*db=*/ db,
            /*zFunctionName=*/ "hello_world",
            /*nArg=*/ 0,
            /*eTextRep=*/ SQLITE_UTF8 | SQLITE_DETERMINISTIC,
            /*pApp=*/ nullptr,
            /*xFunc=*/ helloWorld,
            /*xStep=*/ nullptr,
            /*xFinal=*/ nullptr);
    return rc;
}
