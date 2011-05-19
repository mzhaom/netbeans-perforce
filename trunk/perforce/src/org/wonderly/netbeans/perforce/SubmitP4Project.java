package org.wonderly.netbeans.perforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.actions.CookieAction;
import org.wonderly.netbeans.perforce.PerforceCommand.P4Command;
import org.wonderly.swing.ComponentUpdateThread;

public final class SubmitP4Project extends PerforceCommand {
	public SubmitP4Project() {
		super( "submit", "CTL_SubmitP4Project", "psubmit.gif");
	}

	private Logger log = Logger.getLogger( SubmitP4Project.class.getName() );
  
	protected @Override void performAction(Node[] activatedNodes) {
		Project project = (Project) activatedNodes[0].getLookup().lookup(Project.class);
		FileObject projectFileObject = project.getProjectDirectory();
		if (log.isLoggable(Level.INFO))
			log.info("Getting new changelist for project: "+project);


		String[]n;
		try {
			n = projectDirs(activatedNodes);
		} catch (IOException ex) {
			reportException( ex );
			Exceptions.printStackTrace(ex);
			return;
		}
		final P4Command pc = new P4Command( new String[]{ "opened"}, n, null );

		new ComponentUpdateThread() {
			public Object construct() {
				int code = -1;
				try {
					code = pc.waitFor();
					log.info("opened exits: "+code);
				} catch (Exception ex) {
					log.log( Level.FINE, ex.toString(), ex );
				}
				if( code != 0 ) {
					reportException( new IOException( "Perforce opened command failed: "+code));
					return null;
				}
				//String[]nn = new String[oc.getOutput().size()];
//				PerforceCommand.infoText( "Checking:", pc.getOutput().toString() );
				log.info("output: opened file count: "+((pc.getOutput() != null) ? pc.getOutput().size() : 0));
				List<String>m = new ArrayList<String>();
				List<String>ccl = new ArrayList<String>();
				HashSet<Integer>changes = new HashSet<Integer>();
				for( String v : pc.getOutput() ) {
					String str;
					log.info("found opened file: "+v);

					String[]arr = v.split("#");
					ccl.add( str = arr[0]);
					if( arr.length > 1 ) {
						String end = arr[1];
						int idx = end.indexOf("change ");
						if( idx >= 0 ) {
							end = end.substring(idx+"change ".length());
							idx = end.indexOf(" ");
							if( idx == -1 ) {
								log.warning("unexpected format for opened file output, missing change # after 'change ': "+end );
								continue;
							}
							end = end.substring(0,idx);
							int cno = Integer.parseInt(end);

							log.info("   opened on change #"+cno );
							if( changes.contains(cno) == false ) {
								changes.add( cno );
								boolean collect = false;
								List<String> outs = PerforceOps.changeListDescription(cno);
//								PerforceCommand.infoText("checking for Description("+cno+"): "+outs.size()+" lines");
								for( String out : outs ) {
//									PerforceCommand.infoText("checking for Description("+cno+"): "+out);
									if( out.startsWith("Description") ) {
										collect = true;
										continue;
									}
									if( out.startsWith("File") ) {
										break;
									}
									if( collect ) {
//										PerforceCommand.infoText("Found Description("+cno+"): "+out);
										m.add( out.trim() );
									}
								}
							}
						}
					}
					if (log.isLoggable(Level.INFO))
						log.info("Using File: "+str);
				}
				String msg = "";
				for( String ms : m ) {
					if( msg.length() > 0 && ms.length() > 0 )
						msg += "\n";
					msg += ms;
				}
				P4ChangeList rcl = null;
				try {
					rcl = PerforceOps.newChangeList(msg);
				} catch( IOException ex) {
					reportException(ex);
					return null;
				}
				for( String s : ccl ) {
					log.info("submit project adding opened file: "+s );
					rcl.add( s );
				}

				try {
					PerforceOps.syncChangeList(rcl);
				} catch( Exception ex ) {
					reportException(ex);
					return null;
				}
				if (log.isLoggable(Level.INFO))
					log.info("Got new changelist: "+rcl);
				final P4ChangeList cl = rcl;
//
//				try {
//					cl.writeTo( System.out );
//				} catch( Exception ex ) {
//					log.log( Level.WARNING, ex.toString(), ex );
//				}
				log.info("preparing cl #"+cl.change+" for submission");
				if( cl.prepareSubmit() ) {
					log.info("submitting change content:\n---------------------\n"+cl.toString()+"\n----------------" );
					final P4Command p4c = new P4Command( new String[]{ "submit"}, new String[]{"-i"}, cl );
					new ComponentUpdateThread<Integer>() {
						public @Override Integer construct() {
							try {
								return new Integer( p4c.waitFor() );
							} catch( Exception ex ) {
								log.log( Level.SEVERE, ex.toString(), ex );
							}
							return new Integer(-1);
						}
						public @Override void finished() {
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
				return null;
			}
		}.start();
	}


	protected @Override int mode() {
		return CookieAction.MODE_ALL;
	}

//	public String getName() {
//		return NbBundle.getMessage(SubmitP4Project.class, "CTL_SubmitP4Project");
//	}
	
	protected @Override Class[] cookieClasses() {
		return new Class[] {
			Project.class
		};
	}
	
	protected @Override String iconResource() {
		return "org/wonderly/netbeans/perforce/psubmit.gif";
	}
//	
//	public HelpCtx getHelpCtx() {
//		return HelpCtx.DEFAULT_HELP;
//	}
//	
//	protected boolean asynchronous() {
//		return false;
//	}
	
}

