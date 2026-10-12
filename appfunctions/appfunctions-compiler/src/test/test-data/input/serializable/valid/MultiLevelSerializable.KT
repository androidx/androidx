package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
class MyAttachment(val uri: String)

class CreateNoteFunction {

    @AppFunctionSerializable(isDescribedByKDoc = false)
    class Response(val createdNote: Data.MyNote)

    class Data {
        @AppFunctionSerializable(isDescribedByKDoc = false)
        class MyNote(val id: String, val attachment: MyAttachment)
    }
}