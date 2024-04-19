package com.mysdk

import java.util.concurrent.CancellationException

public class PrivacySandboxCancellationException(
    public override val message: String?,
    public override val cause: Throwable?,
) : CancellationException()
