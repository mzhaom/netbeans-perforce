package org.wonderly.netbeans.perforce;

public final class PerforceFileEdit extends PerforceCommand {
    public PerforceFileEdit() {
        super( new String[]{ "edit" }, "CTL_PerforceFileEdit", "checkout.gif" );
    }
}

