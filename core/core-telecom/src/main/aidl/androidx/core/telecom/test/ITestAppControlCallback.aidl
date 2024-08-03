package androidx.core.telecom.test;

import androidx.core.telecom.extensions.ParticipantParcelable;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ITestAppControlCallback {
    void raiseHandStateAction(in String callId, boolean isHandRaised);
    void kickParticipantAction(in String callId, in ParticipantParcelable participant);
}