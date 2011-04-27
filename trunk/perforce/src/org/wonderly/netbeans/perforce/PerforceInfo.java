/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wonderly.netbeans.perforce;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.wonderly.netbeans.perforce.PerforceCommand.P4Command;
import org.wonderly.swing.ComponentUpdateThread;

public final class PerforceInfo extends PerforceCommand {
	public PerforceInfo() {
       super( "info", "CTL_PerforceInfo", null);
	}
}
final class cPerforceInfo extends CallableSystemAction {
	transient Logger log = Logger.getLogger(getClass().getName());

    public void performAction() {
		final Node[]activatedNodes = new Node[0];
		final String[]types = {"info"};
		try {
			new ComponentUpdateThread<Object[]>() {
				public Object[] construct() {
					P4Command p4c = new P4Command( types, activatedNodes );
					try {
						return new Object[]{ p4c, new Integer( p4c.waitFor() )};
					} catch( Exception ex ) {
						log.log( Level.SEVERE, ex.toString(), ex );
					}
					return new Object[]{ p4c, new Integer(-1) };
				}
				@Override
				public void finished() {
					try {
						Object arr[] = getValue();
						P4Command p4c = (P4Command)arr[0];
						int code = ((Integer)arr[1]).intValue();
						if( code != 0 ) {
							PerforceCommand.showError( p4c, code );
						} else {
							System.out.println("p4 "+Arrays.toString(types)+": exits "+code);
							PerforceCommand.infoText( "p4 "+Arrays.toString(types)+" exited: "+code );
							PerforceCommand.showResults( p4c );
						}
				   } finally {
						super.finished();
					}
				}
			}.start();
		} catch( Exception exx ) {
			PerforceCommand.reportException(exx);
		}
    }

    public String getName() {
        return NbBundle.getMessage(PerforceInfo.class, "CTL_PerforceInfo");
    }
    
    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

}
