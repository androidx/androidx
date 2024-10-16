package androidx.core.telecom.test;

import androidx.core.telecom.extensions.ParticipantParcelable;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface ITestAppControlCallback {
    void onCallAdded(int requestId, in String callId);
    void onGlobalMuteStateChanged(boolean isMuted);
    void raiseHandStateAction(in String callId, boolean isHandRaised);
    void kickParticipantAction(in String callId, in ParticipantParcelable participant);
    void setLocalCallSilenceState(in String callId, boolean isLocallySilenced);
}