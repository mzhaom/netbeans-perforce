package org.wonderly.netbeans.perforce;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.actions.CookieAction;
import org.wonderly.swing.ComponentUpdateThread;

public final class PerforceAddProject  extends PerforceCommand {
	Logger log = Logger.getLogger(getClass().getName());
   public PerforceAddProject() {
       super( "add", "CTL_PerforceAddProject", "padd.gif");
   }

	protected int mode() {
		return CookieAction.MODE_EXACTLY_ONE;
	}

//	public String getName() {
//		return NbBundle.getMessage(PerforceAddProject.class, "CTL_PerforceAddProject");
//	}
	
	protected Class[] cookieClasses() {
		return new Class[] {
			Project.class
		};
	}

	protected void performAction(Node[] activatedNodes) {
		Project project = (Project) activatedNodes[0].getLookup().lookup(Project.class);
		FileObject projectFileObject = project.getProjectDirectory();
		if (log.isLoggable(Level.INFO))
			log.info("Getting new changelist for project: "+project);

		String[]sourcefiles;
		try {
			for( Node n : activatedNodes ) {
				PerforceCommand.infoText("Checking for source files under project at "+PerforceCommand.fileFor(n) );
			}
			sourcefiles = projectFiles(activatedNodes);
		} catch (IOException ex) {
			reportException( ex );
			Exceptions.printStackTrace(ex);
			return;
		}
		if( sourcefiles.length == 0 ) {
			PerforceCommand.errorText( "Project ("+project+") has no source files" );
			try {
				sourcefiles = projectDirs(activatedNodes);
			} catch (IOException ex) {
				reportException( ex );
				Exceptions.printStackTrace(ex);
				return;
			}
			for( String d : sourcefiles ) {
				PerforceCommand.errorText("Project includes directory: "+d );
			}
			return;
		}

		P4ChangeList rcl = null;
		try {
			rcl = PerforceOps.newChangeList("adding "+project.toString()+" files");
		} catch( IOException ex) {
			reportException(ex);
			return;
		}
		final P4ChangeList cl = rcl;

		if (log.isLoggable(Level.INFO))
			log.info("Got new changelist: "+cl);

		final P4Command p4c = new P4Command( new String[]{ "add", "-c", cl.change+"" }, sourcefiles, null );
		new ComponentUpdateThread() {
			public Object construct() {
				try {
					return new Integer( p4c.waitFor() );
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
				return new Integer(-1);
			}
			public void finished() {
				try {
					int code = ((Integer)getValue()).intValue();
					if( code != 0 ) {
						showError( p4c, code );
					} else {
						if (log.isLoggable(Level.FINE))
							log.fine("p4 "+Arrays.toString(types)+": exits "+code);
						PerforceCommand.infoText( "p4 "+PerforceCommand.toArgs(types)+" exited", ""+code );
						showResults( p4c );
					}
			   } finally {
					super.finished();
				}
			}
		}.start();
	}
}

