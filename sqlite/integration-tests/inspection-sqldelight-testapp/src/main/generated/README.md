Unfortunately sqldelight's plugin and dokka have a conflict with two versions of
com.intellij.* files presented at the same time in the classpath. To avoid constant search for
compatible version the generated code was simply committed.