package org.wonderly.netbeans.perforce;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.wonderly.netbeans.perforce.PerforceCommand.P4Command;
import org.wonderly.swing.ComponentUpdateThread;

/**
 *  This class provides the interface to running a "p4merge" to show
 *  visual difference indications.
 */
public class PerforceDiff extends CookieAction {
    private static Logger log = Logger.getLogger( PerforceCommand.class.getName() );
    String types[];
    String id;
    boolean ask;

    public PerforceDiff( String actTypes[], String id ) {
        this( actTypes, id, false );
    }

    public PerforceDiff( String actType, String id ) {
        this( new String[]{ actType }, id, false );
    }

    public PerforceDiff( String actTypes[], String id, boolean ask ) {
        types = actTypes;
        this.id = id;
        this.ask = ask;
    }

    public PerforceDiff( String actType, String id, boolean ask ) {
        this( new String[]{ actType }, id, ask );
    }

    public PerforceDiff() {
        this( "", "CTL_PerforceDiff", false );
    }
    public static void runInSwing( final Runnable r ) {
        if( SwingUtilities.isEventDispatchThread() ) {
            r.run();
        } else {
            try {
                SwingUtilities.invokeAndWait( r );
            } catch( Exception ex) {
            }
        }
    }
    
	@Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
//        putValue("noIconInMenu", Boolean.TRUE);
    }

    static void infoText( final String str ) {
        runInSwing( new Runnable() {
            public void run() {
                PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
                try {
					potc.setForeground( Color.black );
                    potc.addOutput(str);
                    potc.addOutput("\n");
                } catch( Exception ex ) {
                    log.log( Level.SEVERE, ex.toString(), ex );
                }
            }
        });
    }

	static void infoText( final String pref, final String str ) {
		log.info( ((pref == null || pref.length() == 0)? "" : pref+": ") + str );
		runInSwing( new Runnable() {
			public void run() {
				PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
				try {
					potc.setForeground( Color.blue );
					potc.addOutput(((pref == null || pref.length() == 0)? "" : pref+": ") +str);
					potc.addOutput("\n");
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
		});
	}

    static void errorText( final String str ) {
        runInSwing( new Runnable() {
            public void run() {
                PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
                try {
					potc.setForeground( Color.red );
                    potc.addOutput(str);
                    potc.addOutput("\n");
                } catch( Exception ex ) {
                    log.log( Level.SEVERE, ex.toString(), ex );
                }
            }
        });
    }

	static void errorText( final String pref, final String str ) {
		log.info( ((pref == null || pref.length() == 0)? "" : pref+": ") + str );
		runInSwing( new Runnable() {
			public void run() {
				PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
				try {
					potc.setForeground( Color.red );
					potc.addOutput(((pref == null || pref.length() == 0)? "" : pref+": ") +str);
					potc.addOutput("\n");
				} catch( Exception ex ) {
					log.log( Level.SEVERE, ex.toString(), ex );
				}
			}
		});
	}
	private File fileFor( Node n  ){
        DataObject c = (DataObject) n.getCookie(DataObject.class);
        if(c == null ) {
            throw new NullPointerException("can't find FileObject for:"+n);
        }
        FileObject fc = c.getPrimaryFile();
        File f = FileUtil.toFile ( fc );
        return f;
    }

    public static void openWindow() {
        PerforceOutputTopComponent.findInstance().open();
        PerforceOutputTopComponent.findInstance().requestActive();
        PerforceOutputTopComponent.findInstance().requestVisible();
    }
//
//    private void showResults( P4Command pc ) {
//        java.util.List<String>out = pc.getOutput();
//        for( String s: out ) {
//            infoText(s);
//        }
//    }

    protected void performAction( final Node[] activatedNodes ) {
        final String[]files = new String[2];

        try {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Starting diff for: "+Arrays.toString( activatedNodes ));
			}
            //infoText("Starting diff for", Arrays.toString( activatedNodes ) );

			// Start the p4 print command to get the current version from the depot
            final P4Command p4c = new P4Command( new String[]{
				"print", "-q"}, new Node[]{ activatedNodes[0] } );
            final Process pp = p4c.getProcess();
			
			// Use ComponentUpdateThread to have access to a swing thread when we are done.
            new ComponentUpdateThread( ){
                public Object construct() {
                    try {
                        int code = pp.waitFor();
                        if (log.isLoggable(Level.FINE))
							log.fine("print: exits "+code);

						if( code != 0 ) {
                            showError( p4c, code );
                            return new Boolean(false);
                        }
                        File f = fileFor( activatedNodes[0] );
                        File df = File.createTempFile( "depot-", f.getName() );
                        PrintWriter pw = new PrintWriter( 
							new BufferedWriter( new FileWriter( df ) ) );
                        files[0] = df.toString();
                        files[1] = f.toString();
                        if (log.isLoggable(Level.FINE))
							log.fine("writing output to "+df );

						// Copy the output into the file to diff with
                        try {
                            for( String s : p4c.getOutput() ){
                                pw.println(s);
                            }
                        } finally {
                            pw.close();
                        }
                    } catch( Exception ex ) {
                        log.log( Level.SEVERE, ex.toString(), ex );
                    }
                    return null;
                }
				@Override
                public void finished() {
                    try {
                        if( getValue() != null )
                            return;
						// Run the merge command to see the differences
                        final P4Command p4c;
						final String p4diff = PerforceOptions.p4DiffPath();
						p4c = new P4Command( p4diff, files, new Node[]{} );
                        final Process pp = p4c.getProcess();
						
						// At some point we could put a component into here to disable...
                        new ComponentUpdateThread() {
                            public @Override Object construct() {
                                try {
                                    return new Integer( pp.waitFor() );
                                } catch( Exception ex ) {
                                    log.log( Level.SEVERE, ex.toString(), ex );
                                }
                                return new Integer(-1);
                            }
							@Override
                            public void finished() {
                                try {
                                    int code = ((Integer)getValue()).intValue();
									// Need to do this in a swing thread.
                                    if( code != 0 ) {
										if( code != 2 ) {
											errorText( p4diff+" "+files[0]+
												" "+files[1]+" exited", ""+code );
											showError( p4c, code );
										} else {
											infoText(p4diff,"window closed: exit="+code);
										}
                                    } else {
										infoText( p4diff+" "+files[0]+
											" "+files[1]+" exited", ""+code );
									}
                               } finally {
                                    super.finished();
                                }
                            }
                        }.start();
                    } catch( Exception exx ) {
                        reportException(exx);
                    } finally {
                        super.finished();
                    }
                }
            }.start();
        } catch( Exception exx ) {
            reportException(exx);
        }
    }
    
     private void showError( P4Command p4c, int code ) {
		 PerforceCommand.showError( p4c, code );
	 }

    private void reportException(Throwable exx){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter( sw );
        exx.printStackTrace(pw);
        pw.close();
        infoText( sw.toString() );
        log.log(Level.SEVERE,exx.toString(),exx);
    }

    protected int mode() {
        return CookieAction.MODE_ANY;
    }

    public String getName() {
        return NbBundle.getMessage(getClass(), id );
    }

    protected Class[] cookieClasses() {
        return new Class[] {
            DataObject.class
        };
    }
    protected @Override String iconResource() {
        return "org/wonderly/netbeans/perforce/diff.gif";
     
   }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    protected @Override boolean asynchronous() {
        return false;
    }
}
