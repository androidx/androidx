/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.room.compiler.processing

<<<<<<< HEAD
import androidx.room.compiler.processing.XProcessingEnv
import androidx.room.compiler.processing.XProcessingStep
import androidx.room.compiler.processing.XRoundEnv
import androidx.room.compiler.processing.XTypeElement
import androidx.room.compiler.processing.compat.XConverters.toJavac
=======
>>>>>>> 65af8188878 (Fixed lint issues)
import androidx.room.compiler.processing.javac.JavacBasicAnnotationProcessor
import androidx.room.compiler.processing.util.XTestInvocation
import com.google.auto.common.BasicAnnotationProcessor.ProcessingStep
import javax.lang.model.SourceVersion

@Suppress("VisibleForTests")
@ExperimentalProcessingApi
class SyntheticJavacProcessor private constructor(
    private val impl: SyntheticProcessorImpl
) : JavacBasicAnnotationProcessor(), SyntheticProcessor by impl {
    constructor(handlers: List<(XTestInvocation) -> Unit>) : this(SyntheticProcessorImpl(handlers))

    override fun processingSteps(): Iterable<XProcessingStep> = impl.processingSteps()

<<<<<<< HEAD
    override fun postRound(env: XProcessingEnv, round: XRoundEnv) {
        if (!round.toJavac().processingOver()) {
            impl.postRound(env, round)
        }
=======
    // TODO: Figure out why the compiler forces us to implement this method.
    override fun initSteps(): Iterable<ProcessingStep> {
        throw AssertionError("If steps() is not implemented, initSteps() must be.")
>>>>>>> 65af8188878 (Fixed lint issues)
    }

    override fun getSupportedSourceVersion() = SourceVersion.latest()
}