package com.android.tools.build.jetifier.processor.transform.java

import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.transform.TransformationContext
import com.android.tools.build.jetifier.processor.transform.Transformer

class JavaTransformer internal constructor(private val context: TransformationContext) :
    Transformer {

    // Transforms only single java source files for now and not ones contained in archives.
    override fun canTransform(file: ArchiveFile) = file.isJavaFile() && file.isSingleFile

    override fun runTransform(file: ArchiveFile) {
        transformSource(file, context)
    }
}