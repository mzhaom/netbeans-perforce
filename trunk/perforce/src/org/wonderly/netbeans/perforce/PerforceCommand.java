package org.wonderly.netbeans.perforce;

import org.openide.loaders.DataObject;
import org.openide.cookies.EditorCookie;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import java.util.*;
import java.io.*;
import org.openide.filesystems.*;
import java.util.logging.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.management.OperationsException;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.openide.LifecycleManager;
import org.wonderly.awt.*;
import org.wonderly.swing.*;

/**
 *  This is the base class for Perforce actions.  It knows how to run the command line
 *  collect the output and route it all to the window.
 */
public class PerforceCommand extends CookieAction {
	private static Logger log = Logger.getLogger( PerforceCommand.class.getName() );
	protected volatile String types[];
	protected final String id;
	protected final String icon;
	protected final boolean ask;

	public PerforceCommand( String actTypes[], String id, String icon ) {
		this( actTypes, id, icon, false );
	}

	public PerforceCommand( String actType, String id, String icon ) {
		this( new String[]{ actType }, id, icon, false );
	}

	public PerforceCommand( String actTypes[], String id, String icon, boolean ask ) {
		types = actTypes;
		this.id = id;
		this.ask = ask;
		this.icon = icon;
	}

	public PerforceCommand( String actType, String id, String icon, boolean ask ) {
		this( new String[]{ actType }, id, icon, ask );
	}

	public String[] projectFiles( Node[] activatedNodes ) throws IOException {
		return projectFiles( activatedNodes, false );
	}
	public String[] projectDirs( Node[] activatedNodes ) throws IOException {
		return projectFiles( activatedNodes, true );
	}
	public String[] suiteConfigFiles( Node[] activatedNodes, boolean dirs ) throws IOException {
		Project project = (Project) activatedNodes[0].getLookup().lookup(Project.class);
		FileObject projectFileObject = project.getProjectDirectory();
		return new String[]{};
	}
	public String[] projectFiles( Node[] activatedNodes, boolean dirs ) throws IOException {
		Project project = (Project) activatedNodes[0].getLookup().lookup(Project.class);
		FileObject projectFileObject = project.getProjectDirectory();
		Sources projectSources = (Sources)project.getLookup().lookup( Sources.class );
		if( projectSources == null ) {
			return suiteConfigFiles( activatedNodes, dirs  );
		}
		SourceGroup[] srcGroups = projectSources.getSourceGroups( Sources.TYPE_GENERIC );
		ArrayList<String> l = new ArrayList<String>();
		for(SourceGroup grp: srcGroups ) {
			FileObject root = grp.getRootFolder();
			if( countChildren( root.getChildren() ) > 0 ) {
				File f = FileUtil.toFile ( root );

				if( f != null ) {
					String str;
					if( dirs ) {
						l.add( str = f.getCanonicalPath()+"/..." );
						log.info("Adding source path: "+str);
					} else {
						listChildren(root, l);
					}
				}
			} else {
				l.add( root.getPath() );
			}
		}
		if( dirs )
			l.add( projectFileObject.getPath()+"/..." );

		String[]n = new String[l.size()];
		l.toArray(n);

		return n;
	}
	public static void runInSwing( final Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeLater( r );
			} catch( Exception ex) {
			}
		}
	}

	static void debugText( final String pref, final String str ) {
		if (log.isLoggable(Level.FINE))
			log.fine( ((pref == null || pref.length() == 0)? "" : pref+": ") + str );
	}

	static void debugText(  final String str ) {
		if (log.isLoggable(Level.FINE))
			log.fine( str );
	}

	static void infoText( final String pref, final String str ) {
		log.info( ((pref == null || pref.length() == 0)? "" : pref+": ") + str );
		runInSwing( new Runnable() {
			public void run() {
				PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
				try {
					potc.setForeground( Color.green.darker().darker() );
					potc.addOutput(((pref == null || pref.length() == 0)? "" : pref+": ") +str);
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
  static void hyperText( final String before, final String link, final String after ) {
        runInSwing( new Runnable() {
            public void run() {
                PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
                try {
					potc.setForeground( Color.black );
                    potc.addOutput( before, link, after );
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

    public static File fileFor( Node n  ){
        DataObject c = (DataObject) n.getCookie(DataObject.class);
        FileObject fc;
		if(c == null ) {
			Project project = (Project) n.getLookup().lookup(Project.class);
			if( project != null ) {
				fc = project.getProjectDirectory();
			} else
				throw new NullPointerException("can't find FileObject for:"+n);
        } else {
			fc = c.getPrimaryFile();
		}
        File f = FileUtil.toFile ( fc );
        return f;
    }

	protected static class P4Command {
		private final Vector<String>outl = new Vector<String>();
		private final Vector<String>eoutl = new Vector<String>();
		private volatile Process p;
		private volatile boolean exited;
		private final Logger log = Logger.getLogger( getClass().getName() );
		private volatile Thread outth, errth;
        private volatile PerforceOutputTopComponent potc = null;
		private final MyLock outLock = new MyLock( );
		private final MyLock errLock = new MyLock( );
		private final MyLock started = new MyLock( );
		public synchronized int waitFor() throws InterruptedException, IOException {
			if( exited ) {
				errorText( p+"", "already Exited: "+p.exitValue() );
				throw new IllegalStateException("waitFor called on already waited for instance: "+p );
			}
			debugText("Checking process has started");
			started.lock();
			debugText("Process has started");
			started.unlock();
			exited = true;
			debugText("waitFor: waiting for: "+p );
			if( p == null )
				return 0;
			p.getOutputStream().close();
			// wait for command to finish
			debugText("wait for p:="+p+" to exit");
			int code = p.waitFor();
			debugText("p exited: "+p.exitValue());

			try {
				// make sure output threads are gone
				debugText("join output thread: "+outth );
				outLock.lock();
				outth.join(400000);
				debugText(this+": output thread done", ""+code );

				debugText("join error thread: "+errth );
				errLock.lock();
				errth.join(400000);
				debugText(this+": error thread done", ""+code );
				debugText(this+": exit was", ""+code );
			} finally {

				debugText("unlocking after join");
				outLock.unlock();
				errLock.unlock();
			}
			debugText("waitFor: waited for: "+p );
			return code;
		}
 
		public Process getProcess() {
			return p;
		}

		private ArrayList<String>l;
		public String getCommand() {
			String ret = "";
			for( int i = 0; l != null && i < l.size(); ++i ) {
				if( i > 0 )
					ret += " ";
				String v = l.get(i);
				if( v.indexOf(' ') > 0 )
					ret += "\""+v+"\"";
				else
					ret += v;
			}
			return ret;
		}

        public P4Command( String cmd, final String type[], Node[]nds ) {
			this();
            l = new ArrayList<String>();
            l.add(cmd);
            for( String st: type ) {
                l.add(st);
            }
            debugText("Command line",""+l);
            for(Node n: nds ) {
//                DataObject c = (DataObject) n.getCookie(DataObject.class);
//                if(c == null ) {
//                    throw new NullPointerException("can't find FileObject for:"+n);
//                }
//                FileObject fc = c.getPrimaryFile();
                File f = fileFor(n);
                try {
                    l.add( f.getCanonicalPath() );
                } catch( Exception exx ) {
                    reportException(exx);
                }
            }

            String[]n = new String[l.size()];
            l.toArray(n);
            try {
                String sc = "";
                for( String c : n ) {
                    sc += c+" ";
                }
                debugText("starting",sc );
                p = Runtime.getRuntime().exec( n, PerforceOptions.getPropertyValues());
                InputStream os = p.getInputStream();
                final BufferedReader rd = new BufferedReader( new InputStreamReader(os));
                InputStream es = p.getErrorStream();
                final BufferedReader erd = new BufferedReader( new InputStreamReader(es));
                p.getOutputStream().close();

                outth = new Thread() {
					@Override
                    public void run(){
                        String str;
                        try {
                            try {
                                while( (str = rd.readLine()) != null ) {
                                    outl.add(str);
									if( str.indexOf( "#") == -1 )
										continue;

									str = str.split("#")[0];
									if( type[0].equals("edit") ) {
										potc.edit(str);
									} else if( type[0].equals("add") ) {
										potc.add( str );
									} else if( type[0].equals("submit") ) {
										potc.submit(str);
									} else if( type[0].equals("delete") ) {
										potc.delete( str );
									} else if( type[0].equals( "revert") ) {
										potc.revert(str);
									}
                                }
                            } finally {
                                rd.close();
                            }
                        } catch( Exception ex){
                            log.log(Level.WARNING,ex.toString(),ex);
                        }
                    }
                };
				outth.start();

                errth = new Thread() {
					@Override
                    public void run(){
                        String str;
                        try {
                            try {
                                while( (str = erd.readLine()) != null ) {
                                    errorText( str );
                                    eoutl.add(str);
                                }
                            } finally {
                                erd.close();
                            }
                        } catch( Exception ex){
                             log.log(Level.WARNING,ex.toString(),ex);
                        }
                    }
                };
				errth.start();
             } catch( Exception ex ) {
                log.log(Level.SEVERE,ex.toString(),ex);   
            }
            openWindow();
        }		
		P4Command() {
			debugText("Getting top component instance");
			runInSwing( new Runnable() {
				public void run() {
					 potc = PerforceOutputTopComponent.findInstance();
				}
			});
			debugText("Got instance: "+potc);
		}
		P4Command( String type[], String[] files, InputStreamProvider pr ) {
			this( type, files, pr, true, true );
		}

		boolean isVisible( String f ) {
			debugText("Checking visible: "+f);
			if( f.startsWith("//") && f.indexOf("\\") == -1 )
				return false;
			File ff = new File(f);
			File parent = ff.getParentFile();
			debugText("parent is "+parent );
			debugText("check have: "+ff);
			boolean have = ff.exists();
			debugText("have: "+ff+" is "+have);
			boolean vis = have || (parent != null ? parent.exists() : have);
			debugText(f+" is "+(vis?"":"not ")+"visible");
			return vis;
		}
		boolean isMinusDCommand( String[]cmdargs ) {
//			for( String a : cmdargs ) {
//				if( a.equals("opened") )
//					return false;
//				else if( a.equals("submit") )
//					return false;
//				else if( a.equals("add") )
//					return false;
//				else if( a.equals("edit") )
//					return false;
//			}
			return true;
		}
		P4Command( String type[], String[] files, final InputStreamProvider pr, final boolean seeOutput, final boolean seeCommand ) {
			this();
			debugText("Building command line for \"p4\" "+Arrays.toString( type ));
			l = new ArrayList<String>();
			l.add("p4");
			debugText("Checking if -d is needed");
			if( l.contains("-d") == false && isMinusDCommand(type) ) {
				if( (files.length > 0 && files[0] != null && isVisible(files[0])) ) {
					l.add("-d");
					l.add(new File(files[0]).getParent() );
				}
			}
			l.addAll(Arrays.asList(type));
			debugText("Adding all files");
			l.addAll(Arrays.asList(files));
			debugText("Copying to array");
			final String[]n = new String[l.size()];
			l.toArray( n );
			debugText("built command line: "+l );

			debugText("Locking for thread start");
			started.lock();
			new Thread() {
				public @Override void run() {
					runCommandLine( n, pr, seeOutput, seeCommand );
				}
			}.start();
		}

		public P4Command( String type[], Node[]nds ) {
			this();

			l = new ArrayList<String>();
			l.add("p4");
//			l.add("-z");
//			l.add("tag");
			// Set the directory to that of the first file in the list to make
			// P4CONFIG work correctly.
			if( nds.length > 0 ) {
//				l.add( "-d");
				String parentDir = null;
				if( nds[0] == null ) {
					throw new NullPointerException( nds.length+" arguments provide, first is null, must be non-null: "+Arrays.toString( nds ) );
				}
				DataObject c = null;
				for( Node n : nds ) {
					c = (DataObject) n.getCookie(DataObject.class);
					if( c == null ) {
						log.info( "no data object for "+n );
					} else {
						break;
					}
				}
				if( c == null ) {
					JOptionPane.showMessageDialog( null, "no project object found");
					return;
				}
				for( FileObject fc : (Set<FileObject>)c.files() ) {
					log.info("Look at file Object (folder="+fc.isFolder()+") "+fc);
					if( fc.isFolder() == false ) {
						File f = FileUtil.toFile( fc );
						if( f != null ) {
							log.info("using files directory for -d: "+f.getParent());
							parentDir = f.getParent( );
						}
					} else {
						log.info("using directory for -d: "+fc );
						parentDir = FileUtil.toFile( fc ).toString();
					}
					break;
				}
				if( parentDir != null  && l.contains("-d") == false && isMinusDCommand(type) ) {
					l.add("-d");
					l.add( parentDir );
				}
			}
			for( String st: type ) {
				l.add(st);
			}

			P4ChangeList pl = null;
			if( type[0].equals("info") == false ) {
				for(Node n: nds ) {
					DataObject c = (DataObject) n.getCookie(DataObject.class);
					if(c == null ) {
						log.info("project node? checking: "+n );
						// User has selected folder/project for action, not file

						Project pc = (Project)n.getLookup().lookup( Project.class );
						if( pc == null )
							throw new NullPointerException("No project object available");

						FileObject proj = pc.getProjectDirectory();
						String projdir = "";
						if( proj != null ) {
							if( type[0].equals("edit") ) {
								File f = FileUtil.toFile ( proj );
								try {
									if( f != null ) {
										String str;
										projdir = f.getCanonicalPath();

										if( filesUnder(  projdir + File.separator + "nbproject" ).size() > 0 ) {
											l.add( str = projdir + File.separator + "nbproject" + File.separator+"..." );
											log.info("Adding nbproject path under: "+projdir );
										}
										if( filesUnder(  projdir + File.separator + "build"     ).size() > 0 ) {
											l.add( str = projdir + File.separator + "build"     + File.separator+"..." );
											log.info("Adding build path under: "+projdir );
										}
									}
								} catch( Exception exx ) {
									reportException(exx);
								}
							} else if( type[0].equals("add") ) {
								try {
									listChildren( proj.getFileObject( "build" ), l );
									listChildren( proj.getFileObject( "nbproject" ), l );
								} catch( Exception exx ) {
									reportException(exx);
								}
	//						} else if( type[0].equals("submit") ) {
							} else {
								log.info("processing type: "+type[0] );
								File f = FileUtil.toFile ( proj );
								try {
									if( f != null ) {
										String str;
										l.add( str = f.getCanonicalPath()+File.separator+"..." );
										projdir = f.getCanonicalPath();
										log.info("Adding project path: "+str);
									}
								} catch( Exception exx ) {
									reportException(exx);
								}
							}
						}
						Sources srcs = (Sources)pc.getLookup().lookup(Sources.class);
						if( srcs == null )
							throw new NullPointerException( "Project: "+n+" has no sources object");
						SourceGroup grps[] = srcs.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
						for(SourceGroup grp: grps ) {
							FileObject root = grp.getRootFolder();
							log.info("Checking for files under: "+FileUtil.toFile(root) );
							if( countChildren( root.getChildren() ) > 0 ) {
								File f = FileUtil.toFile ( root );
								log.info("found some children sources under: "+f);
								try {
									if( f != null ) {
	//									if( f.getCanonicalPath().startsWith( projdir ) == false ) {
											listChildren( root, l );
	//									}
									}
								} catch( Exception exx ) {
									reportException(exx);
								}
							}
						}

					} else {
						log.info("got data object for node: "+n );
						// User has selected 1 or more files.
						if( type[0].equals("submit") ) {
							try {
								pl = PerforceOps.newChangeList();
								l.add("-c");
								l.add(pl.change + "");
							} catch (IOException ex) {
								log.log(Level.SEVERE, ex.toString(), ex);
								throw new RuntimeException( ex.toString(), ex );
							}
						}
						for( FileObject fc : (Set<FileObject>)c.files() ) {
							File f = FileUtil.toFile ( fc );

							try {
								if( f != null ) {
									if( type[0].equals("add") ) {
										if( f.isDirectory() ) {
											listChildren( fc, l );
										} else {
											l.add( f.getCanonicalPath() );
										}
									} else if( type[0].equals("submit") ) {
										if( f.isDirectory() ) {
											pl.add( f.getCanonicalPath()+"/..." );
										} else {
											pl.add( f.getCanonicalPath() );
										}
									} else {

										if( f.isDirectory() ) {
											l.add( f.getCanonicalPath()+"/..." );
										} else {
											l.add( f.getCanonicalPath() );
										}
									}
								}
							} catch( Exception exx ) {
								reportException(exx);
							}
						}
						if( pl != null ) {
							if( !pl.prepareSubmit() ) {
								pl.cancel();
								throw new IllegalStateException("Operation Cancelled, or no Files to process");
							}
							try {
								log.info("updating change list with files.");
								PerforceOps.syncChangeList(pl);
							} catch (IOException ex) {
								Exceptions.printStackTrace(ex);
								log.log(Level.SEVERE, ex.toString(), ex);
								throw new RuntimeException( ex.toString(), ex );
							}
						}
					}
				}
			}
			String[]n = new String[l.size()];
			l.toArray(n);
			runCommandLine( n, null,
				type[0].equals("print") == false && Arrays.asList( type ).contains("where") == false, 
				type[0].equals("print") == false && Arrays.asList( type ).contains("where") == false );
		}


		void runCommandLine( final String[] n, final InputStreamProvider pr ) {
			runCommandLine( n, pr, true, true );
		}

		void runCommandLine( final String[] n, final InputStreamProvider pr, final boolean seeOutput, boolean seeCommand ) {
			if( seeCommand )
				infoText("Executing",Arrays.toString(n));
			try {
				String sc = "";
				for( String c : n ) {
					sc += c+" ";
				}

				debugText( "starting", sc );
				String[]props = PerforceOptions.getPropertyValues();
//				debugText("Environment", Arrays.toString(props) );
				debugText("Execing command: "+Arrays.toString(n));
				p = Runtime.getRuntime().exec( n, props );
				debugText("Setting started to unlocked");
				started.unlock();
				InputStream os = p.getInputStream();
				final BufferedReader rd = new BufferedReader( new InputStreamReader(os));
				InputStream es = p.getErrorStream();
				final BufferedReader erd = new BufferedReader( new InputStreamReader(es));
				if( pr != null ) {
					log.info("Configuring for writer: "+pr);
					new Thread() {
						@Override
						public void run() {
							try {
								pr.writeTo(p.getOutputStream());
								p.getOutputStream().close();
							} catch (IOException ex) {
								log.log( Level.WARNING, ex.toString(), ex );
							}
							log.info("input Stream closed");
						}
					}.start();
				} else {
					log.info("Closing input Stream, no source");
					p.getOutputStream().close();
				}
//				final JTextArea area = potc.getTextArea();
				outLock.lock();
				errLock.lock();
				outth = new Thread() {
					@Override
					public void run(){
						String str;
						try {
							try {
								log.info("Starting output reading");
								while( (str = rd.readLine()) != null ) {
									if( seeOutput ) {
										if( str.indexOf("#") == -1 ) {
											int idx;
											if( (idx = str.indexOf(" - empty, assuming text")) > 0 ||
													(idx = str.indexOf(" - can't add existing file")) > 0 ||
													(idx = str.indexOf(" - can't add (already opened for edit)")) > 0 ) {
												String trail = str.substring(idx);
												String lead = "";
												if( str.startsWith("edit ") ) {
													str = str.substring("edit ".length() );
													lead = "edit ";
												} else if( str.startsWith("add ") ) {
													str = str.substring("add ".length() );
													lead = "edit ";
												}
												if( trail.contains( "opened for delete" ) == false ) {
													hyperText( lead, str.substring(0,idx), trail );
												}
											} else {
												infoText( str );
											}
										} else {
											String lead = "";
											if( str.startsWith("edit ") ) {
												str = str.substring("edit ".length() );
												lead = "edit ";
											} else if( str.startsWith("add ") ) {
												str = str.substring("add ".length() );
												lead = "edit ";
											}
											String trail = str.split("#")[1];
											if( trail.contains( "opened for delete" ) == false ) {
												hyperText( lead, str.split("#")[0], "#"+trail );
											}
										}
									}
									//infoText("adding \""+str+"\" to list: "+outl );
									outl.add(str);
									if( str.indexOf( "#") == -1 )
										continue;

									str = str.split("#")[0];
									if( n[1].equals("edit") ) {
										potc.edit(str);
									} else if( n[1].equals("add") ) {
										potc.add( str );
									} else if( n[1].equals("submit") ) {
										potc.submit(str);
									} else if( n[1].equals("delete") ) {
										potc.delete( str );
									} else if( n[1].equals( "revert") ) {
										potc.revert(str);
									}
								}
							} finally {
								rd.close();
								log.info("unlocking output lock");
								outLock.unlock();
							}
							if (log.isLoggable(Level.FINE))
								log.fine("output eof reached");
						} catch( Exception ex){
							log.log(Level.WARNING,ex.toString(),ex);
						}
					}
				};
				outth.start();

				errth  = new Thread() {
					@Override
					public void run(){
						String str;
						try {
							try {
								if (log.isLoggable(Level.FINE))
									log.fine("error Thread starting");
								while( (str = erd.readLine()) != null ) {
									errorText( str );
									eoutl.add(str);
                                    if (log.isLoggable(Level.FINE))
										log.fine("added error: "+str);
								}
							} finally {
								erd.close();
							}
							if (log.isLoggable(Level.FINE))
								log.fine("error eof reached");
						} catch( Exception ex) {
							 log.log(Level.WARNING,ex.toString(),ex);
						} finally {
							log.info("unlocking error lock");
							errLock.unlock();
						}
						if (log.isLoggable(Level.FINE))
							log.fine("Error Thread exiting");
					}
				};
				errth.start();
				if (log.isLoggable(Level.FINE))
					log.fine(n[0]+" "+n[1]+" processing started");
			} catch( RuntimeException ex ) {
				log.log(Level.SEVERE,ex.toString(),ex);
				throw ex;
			} catch( Exception ex ) {
				log.log(Level.SEVERE,ex.toString(),ex);
			}
			openWindow();
			//infoText("Command line started: "+Arrays.toString(n));
		}

		public java.util.List<String> getOutput() {
			return outl;
		}

		public java.util.List<String> getError() {
			return eoutl;
		}
	}

	private static class MyLock {
		volatile boolean locked;
		Object lock = new Object();
		public void lock() {
			synchronized( lock ) {
				while( locked ) {
					try {
						lock.wait();
					} catch( Exception ex ) {
						log.log(Level.SEVERE, ex.toString(), ex);
					}
					if( !locked ) {
						break;
					}
				}
				locked = true;
			}
		}
		public void unlock() {
			synchronized( lock ) {
				locked = false;
				lock.notifyAll();
			}
		}
	}

	protected static int countChildren( FileObject[] fos ) {
		int cnt = 0;
		for( FileObject f: fos ) {
			if( f.isFolder() ) {
				int cld = countChildren( f.getChildren() );
				if (log.isLoggable(Level.FINE))
					log.fine( "found " + cld + " children under: " + FileUtil.toFile( f ) );
				cnt += cld;
			} else {
				++cnt;
			}
		}
		return cnt;
	}

	private static void listChildren( FileObject root, List<String> list ) throws IOException {
		for( FileObject fo : root.getChildren() ) {
			if( fo.isFolder() == false ) {
				File f = FileUtil.toFile ( fo );
				String str;
				list.add( str = f.getCanonicalPath() );
				if (log.isLoggable(Level.FINE))
					log.fine("Adding source file: "+str);
			} else {
				listChildren( fo, list );
			}
		}
	}

	private static void listChildrenObjects( FileObject root, List<FileObject> list ) throws IOException {
		for( FileObject fo : root.getChildren() ) {
			if( fo.isFolder() == false ) {
				list.add( fo );
			} else {
				listChildrenObjects( fo, list );
			}
		}
	}

	private static List<String> filesUnder( String dir ) throws IOException {
		if (log.isLoggable(Level.FINE))
			log.fine( "counting files under: " + dir );
		final P4Command pc = new P4Command( new String[]{ "have"}, 
				new String[]{ dir+"\\..." }, null );
		ArrayList<String>cl = new ArrayList<String>();
		int code = -1;
		try {
			if (log.isLoggable(Level.FINE))
				log.fine("Waiting for p4 have "+dir+"\\...");
			code = pc.waitFor();
			if (log.isLoggable(Level.FINE))
				log.fine("have exits: "+code);
		} catch (Exception ex) {
			log.log( Level.WARNING, ex.toString(), ex );
		}
		if( code != 0 ) {
			throw new IOException( "Perforce opened command failed: "+code);
		}
		if (log.isLoggable(Level.FINE))
			log.fine("output: "+pc.getOutput()+", len: "+((pc.outl != null) ? pc.outl.size() : 0));
		for( String v : pc.getOutput() ) {
			String str;
			cl.add( str = v.split("#")[0]);
			if (log.isLoggable(Level.FINE))
				log.fine("have Using File: "+str);
		}
		return cl;
	}

	public static void openWindow() {
		runInSwing( new Runnable() {
			public void run() {
				PerforceOutputTopComponent.findInstance().open();
				PerforceOutputTopComponent.findInstance().requestActive();
				PerforceOutputTopComponent.findInstance().requestVisible();
			}
		});
	}

	static void showResults( P4Command pc ) {
		java.util.List<String>out = pc.getOutput();
		for( String s: out ) {
			//infoText(s);
			if (log.isLoggable(Level.FINE)) {
				log.fine(s);
			}
		}
	}
	
	String fromArray( Object[] arr ) {
		String str = "";
		for( int i = 0; i < arr.length; ++i ) {
			if( i > 0 )
				str += " ";
			str += arr[i].toString();
		}
		return str;
	}

	protected void performAction( final Node[] activatedNodes ) {
        LifecycleManager.getDefault().saveAll(); 
		if( ask ) {
			JPanel p = new JPanel();
			Packer pk = new Packer( p );
			int y = -1;
			if( activatedNodes.length < 6 ) {
				pk.pack( new JLabel( "Really " + fromArray(types) + " "+
					activatedNodes.length+" file"+(activatedNodes.length>1?"s":""))
					).gridx(0).gridy(++y).fillx().inset(6,6,6,6);
				pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(6,6,6,6);
				for( Node n : activatedNodes ) {
					DataObject c = (DataObject) n.getCookie(DataObject.class);  
					EditorCookie ec = (EditorCookie) n.getCookie(EditorCookie.class);
					if( ec != null && ec.isModified() ) {
						try {
							ec.saveDocument();
						} catch( Exception ex ) {
							reportException(ex);
						}
					}

					if(c == null ) {
						throw new NullPointerException("can't find FileObject for:"+n);
					}
					for( FileObject fc : (Set<FileObject>)c.files() ) {
						File f = FileUtil.toFile ( fc );
						pk.pack(new JLabel(f.getName())).gridx(0).gridy(++y).inset(2,6,2,2).fillx();
					}
			   }
			} else {
				for( Node n : activatedNodes ) {
					DataObject c = (DataObject) n.getCookie(DataObject.class);
					EditorCookie ec = (EditorCookie) n.getCookie(EditorCookie.class);
					if( ec != null && ec.isModified() ) {
						try {
							ec.saveDocument();
						} catch( Exception ex ) {
							reportException(ex);
						}
					}
				}
				pk.pack( new JLabel( "Really "+Arrays.toString(types)+" "+activatedNodes.length+
						" files") ).gridx(0).gridy(++y).fillx().inset(5,5,5,5);
			}
			pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(6,6,6,6);

			final JButton bs[] = new JButton[3];
			bs[2] = new JButton( NbBundle.getMessage( getClass(), "cancel_button" ) );
			bs[1] = new JButton( NbBundle.getMessage( getClass(), "review_button" ) );
			bs[0] = new JButton( NbBundle.getMessage( getClass(), "okay_button" ) );

			final boolean cancelled[] = new boolean[1];

			bs[2].addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					cancelled[0] = true;
				}
			});

			bs[1].addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					try {
						infoText("diff", "Starting");
						for( Node n : activatedNodes ) {
							new PerforceDiff().performAction( new Node[]{ n } );
						}
					} catch( Exception exx ) {
						reportException(exx);
					}
				}
			});

			bs[0].addActionListener( new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					cancelled[0] = false;
				}
			});

			final Dialog dlg[] = new Dialog[1];
			dlg[0] = DialogSupport.createDialog( "Confirm "+Arrays.toString(types)+" operation",
				p, true, bs, false, 2, 2,
				new ActionListener() {
					public void actionPerformed( ActionEvent ev ) {
						if( ev.getSource() != bs[1] ) {
							dlg[0].setVisible(false);
							dlg[0].dispose();
						}
					}
				}
			);

			dlg[0].pack();
			dlg[0].setVisible( true );
			if( cancelled[0] )
				return;
		}

		final P4Command p4c = new P4Command( types, activatedNodes );
		try {
			new ComponentUpdateThread<Object[]>() {
				public Object[] construct() {
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
							onError( p4c, code );
							showError( p4c, code );
						} else {
							if (log.isLoggable(Level.FINE))
								log.fine("p4 "+toArgs(types)+": exits "+code);
							infoText( "p4 "+toArgs(types)+" exited", ""+code );
							showResults( p4c );
						}
				} finally {
						super.finished();
						new Thread() {
							public void run() {
								refreshFiles( activatedNodes );
							}
						}.start();
					}
				}

			}.start();
		} catch( Exception exx ) {
			onError( p4c, exx );
			reportException(exx);
		}
	}

	static String toArgs(String[] types) {
		String str = "";

		for( int i = 0; i < types.length; ++i ) {
			if( i > 0 )
				str += " ";
			str += types[i];
		}
		return str;
	}

	private void refreshFiles( Node[] nds ) {
		List<FileObject> l = new ArrayList<FileObject>();
		for(Node n: nds ) {
			DataObject c = (DataObject) n.getCookie(DataObject.class);
			if( c == null ) {
				log.info( "no data object for: Node="+n );
				continue;
			}
		
			for( FileObject fc : (Set<FileObject>)c.files() ) {
				File f = FileUtil.toFile ( fc );

				try {
					if( f != null ) {
						if( f.isDirectory() ) {
							listChildrenObjects( fc, l );
						} else {
							l.add( FileUtil.toFileObject( f ) );
						}
					}
				} catch( Exception exx ) {
					reportException(exx);
				}
			}
		}
		for( FileObject f : l ) {
			FileUtil.refreshFor( FileUtil.toFile( f ) );
			try {
				f.getFileSystem().refresh(false);
			} catch (FileStateInvalidException ex) {
				reportException(ex);
			}
//			f.refresh( false );
		}
	}

	static void showError( String[] p4c, int code, String[]outs, String[]errs ) {
		JPanel p = new JPanel();
		Packer pk = new Packer( p );
		int y = -1;
		JLabel l;
		String[]strs = outs;
		pk.pack(l = new JLabel( Arrays.toString(p4c)+" exited: "+code)).gridx(0).gridy(++y).inset(6,6,6,6);
		l.setFont( new Font( "courier",Font.BOLD,16));
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,10,5,10);
//		String[]strs = out.buf.toString().split("\n");
		Font f = new Font( "courier",Font.PLAIN,12);

		for( int i = 0; i < strs.length; ++i ) {
			pk.pack( l=new JLabel(strs[i] ) ).gridx(0).gridy(++y).fillx();
			l.setFont(f);
			if( i + 1 < strs.length && i > 2 ) {
				pk.pack( l=new JLabel("...")).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
				l.setFont(f);
				break;
			}
		}

		if( strs.length > 0 ) {
			pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,5,5,5);
		}
		strs = errs;
		for( String s : strs ) {
			pk.pack( l=new JLabel(s) ).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
			l.setFont(f);
		}

		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(10,10,10,10);
		final Dialog dlg[] = new Dialog[1];
		final JButton bs[] = new JButton[1];
		bs[0] = new JButton( NbBundle.getMessage( PerforceCommand.class, "okay_button" ) );

		dlg[0] = DialogSupport.createDialog( "Error Performing Perforce Operation",
			p, false, bs, false, 0, 0,
			new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					dlg[0].setVisible(false);
					dlg[0].dispose();
				}
			}
		);
		dlg[0].pack();
		dlg[0].setVisible(true);
	}
	
	protected void onError( P4Command p4c, int code ) {
		log.warning(p4c+": exited: "+code );
		errorText( p4c+": exited", ""+code );
	}
	
	protected void onError( P4Command p4c, Throwable ex ) {
		log.log( Level.WARNING, p4c+": failed: "+ex, ex );
		errorText( p4c+": failed", ""+ex );
	}

	protected static void showError( P4Command p4c, int code ) {
		JPanel p = new JPanel();
		Packer pk = new Packer( p );
		int y = -1;
		JTextArea l;
		pk.pack( new JScrollPane( l = new JTextArea( 12, 70 ) ) ).fillboth().gridx(0).gridy(++y).inset(6,6,6,6);
		l.setText( p4c.getCommand() );
		l.setEditable(false);
		l.setFont( new Font( "courier",Font.BOLD,12));
		l.setWrapStyleWord( true );
		l.setLineWrap( true );
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,10,5,10);
		pk.pack( new JLabel( "exit code: "+code ) ).gridx(0).gridy(++y).fillx().inset(5,5,5,5);
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,10,5,10);
		java.util.List<String>strs = p4c.getOutput();
		Font f = new Font( "courier",Font.PLAIN,9);
		int added = 0;
		boolean first = true;
		l.append("\n---------------- MESSAGE OUTPUT ----------------\n");
		for( int i = 0; i < strs.size(); ++i ) {
//			JLabel label;
			if( strs.get(i).trim().length() == 0 && first )
				continue;
			first = false;
			l.append( strs.get(i)+"\n");
//			pk.pack( label = new JLabel(strs.get(i) ) ).gridx(0).gridy(++y).fillx();
//			label.setFont(f);
//			if( i + 1 < strs.size() && i > 2 ) {
//				pk.pack( label = new JLabel("...")).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
//				label.setFont(f);
//				break;
//			}
		}
//		if( added > 0 ) {
//			pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,5,5,5);
//		}
		l.append("----------------- ERROR OUTPUT -----------------\n");
		strs = p4c.getError();
		for( String s : strs ) {
//			JLabel label;
//			pk.pack( label=new JLabel(s) ).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
//			label.setFont(f);
			l.append( s+"\n");
		}
//		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(10,10,10,10);
		final Dialog dlg[] = new Dialog[1];
		final JButton bs[] = new JButton[1];
		bs[0] = new JButton( NbBundle.getMessage( PerforceCommand.class, "okay_button" ) );

		dlg[0] = DialogSupport.createDialog( "Error Performing Perforce Operation",
			p, false, bs, false, 0, 0,
			new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					dlg[0].setVisible(false);
					dlg[0].dispose();
				}
			}
		);
		dlg[0].pack();
		dlg[0].setVisible(true);
	}

	@Override
	protected void initialize() {
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
		if( icon == null )
			putValue("noIconInMenu", Boolean.TRUE);
	}

	static void reportException(Throwable exx){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		exx.printStackTrace(pw);
		pw.close();
		infoText( "exception",sw.toString() );
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

	@Override
	protected String iconResource() {
		return icon == null ? null : "org/wonderly/netbeans/perforce/"+icon;
	}

	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}

	@Override
	protected boolean asynchronous() {
		return false;
	}
}

