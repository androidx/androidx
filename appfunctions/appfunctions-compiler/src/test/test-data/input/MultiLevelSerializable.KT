package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class MyAttachment(val uri: String)

class CreateNoteFunction {

    @AppFunctionSerializable
    class Response(val createdNote: Data.MyNote)

    class Data {
        @AppFunctionSerializable
        class MyNote(val id: String, val attachment: MyAttachment)
    }
}