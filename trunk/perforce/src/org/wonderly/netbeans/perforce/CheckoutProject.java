package org.wonderly.netbeans.perforce;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.actions.CookieAction;
import org.wonderly.swing.ComponentUpdateThread;

public final class CheckoutProject extends PerforceCommand {
	Logger log = Logger.getLogger(getClass().getName());
	public CheckoutProject() {
		super( "edit", "CTL_CheckoutProject", "pcheckout.gif");
	}
	protected @Override int mode() {
		return CookieAction.MODE_EXACTLY_ONE;
	}

	protected @Override Class[] cookieClasses() {
		return new Class[] {
			Project.class
		};
	}

	protected void performAction( final Node[] activatedNodes) {
		new ComponentUpdateThread<Void>(){
			public void setup() {
				setEnabled(false);
			}
			public Void construct() {
				doPerformAction( activatedNodes );
				return null;
			}
			public void finished() {
				try {
					setEnabled(true);
				} finally {
					super.finished();
				}
			}
		}.start();
	}
	private void doPerformAction( Node[] activatedNodes ) {
		Project project = (Project) activatedNodes[0].getLookup().lookup(Project.class);
		FileObject projectFileObject = project.getProjectDirectory();
		if (log.isLoggable(Level.INFO))
			log.info("Getting new changelist for project: "+project);
		P4ChangeList rcl = null;
		try {
			rcl = PerforceOps.newChangeList();
		} catch( IOException ex) {
			reportException(ex);
			return;
		}
		final P4ChangeList cl = rcl;

		if (log.isLoggable(Level.INFO))
			log.info("Got new changelist: "+cl);

		String[]n;
		try {
			n = projectFiles(activatedNodes);
		} catch (IOException ex) {
			reportException( ex );
			Exceptions.printStackTrace(ex);
			return;
		}
		final P4Command pc = new P4Command( new String[]{ "edit"}, n, null );
		int code = -1;
		try {
			code = pc.waitFor();
		} catch (InterruptedException ex) {
			reportException( ex );
			Exceptions.printStackTrace(ex);
		} catch (IOException ex) {
			reportException( ex );
			Exceptions.printStackTrace(ex);
		}

		if( code != 0 ) {
			showError( pc, code );
		}
	}

}