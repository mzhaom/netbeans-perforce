package org.wonderly.netbeans.perforce;

import org.openide.nodes.Node;

public final class PerforceFileSyncWithEdit extends PerforceCommand {
    public PerforceFileSyncWithEdit() {
        super( new String[]{ "sync", "edit" }, "CTL_PerforceFileSyncWithEdit", "checkout.gif" );
    }
	private volatile boolean error;
	
	@Override
	protected void performAction( final Node[] activatedNodes ) {
		types = new String[] { "sync" };
		super.performAction( activatedNodes );
		types = new String[] { "edit" };
		super.performAction( activatedNodes );
	}
	@Override
	protected void onError( P4Command p4c, int code ) {
		error = true;
		super.onError( p4c, code );
	}
}

