package org.wonderly.netbeans.perforce;

import java.awt.Dialog;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import java.util.*;
import java.io.*;
import org.openide.filesystems.*;
import java.util.logging.*;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.wonderly.awt.Packer;
import org.wonderly.netbeans.perforce.PerforceCommand.P4Command;
import org.wonderly.swing.ComponentUpdateThread;

public final class PerforceSubmit extends CookieAction {
	private static Logger log = Logger.getLogger( PerforceCommand.class.getName() );
	String types[];
	String id;
	String icon;
	boolean ask;

   public PerforceSubmit() {
       this( "submit", "CTL_PerforceSubmit", "submit.gif");
   }

	public PerforceSubmit( String actTypes[], String id, String icon ) {
		this( actTypes, id, icon, true );
	}

	public PerforceSubmit( String actType, String id, String icon ) {
		this( new String[]{ actType }, id, icon, false );
	}

	public PerforceSubmit( String actTypes[], String id, String icon, boolean ask ) {
		types = actTypes;
		this.id = id;
		this.ask = ask;
		this.icon = icon;
	}

	public PerforceSubmit( String actType, String id, String icon, boolean ask ) {
		this( new String[]{ actType }, id, icon, ask );
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
//
//	static void infoText( final String str ) {
//		runInSwing( new Runnable() {
//			public void run() {
//				PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
//				try {
//					potc.addOutput(str);
//					potc.addOutput("\n");
//				} catch( Exception ex ) {
//					log.log( Level.SEVERE, ex.toString(), ex );
//				}
//			}
//		});
//	}
//
//	private class P4Command {
//		ArrayList<String>outl = new ArrayList<String>();
//		ArrayList<String>eoutl = new ArrayList<String>();
//		Process p;
//		Logger log = Logger.getLogger( getClass().getName() );
//
//		public Process getProcess() {
//			return p;
//		}
//
//		private ArrayList<String>l;
//		public String getCommand() {
//			String ret = "";
//			for( int i = 0; l != null && i < l.size(); ++i ) {
//				if( i > 0 )
//					ret += " ";
//				String v = l.get(i);
//				if( v.indexOf(' ') > 0 )
//					ret += "\""+v+"\"";
//				else
//					ret += v;
//			}
//			return ret;
//		}
//
//		public P4Command( final String type[], Node[]nds ) {
//			l = new ArrayList<String>();
//			l.add("p4");
//			for( String st: type ) {
//				l.add(st);
//			}
//
//			for(Node n: nds ) {
//				DataObject c = (DataObject) n.getCookie(DataObject.class);
//				if(c == null ) {
//					//Project pc = (Project) n.getCookie(Project.class);
//					//if( pc == null ) {
//						Project pc = (Project)n.getLookup().lookup( Project.class );
//						if( pc == null )
//							throw new NullPointerException("No project object available");
//					//}
//					FileObject proj = pc.getProjectDirectory();
//					String projdir = "";
//					if( proj != null ) {
//						File f = FileUtil.toFile ( proj );
//						try {
//							if( f != null ) {
//								String str;
//								l.add( str = f.getCanonicalPath()+File.separator+"..." );
//								projdir = f.getCanonicalPath();
//								log.info("Adding project path: "+str);
//							}
//						} catch( Exception exx ) {
//							reportException(exx);
//						}
//					}
//				} else {
//					for( FileObject fc : (Set<FileObject>)c.files() ) {
//						File f = FileUtil.toFile ( fc );
//
//						try {
//							if( f != null ) {
//								if( f.isDirectory() ) {
//									l.add( f.getCanonicalPath()+"/..." );								 
//								} else {
//									l.add( f.getCanonicalPath() );
//								}
//							}
//						} catch( Exception exx ) {
//							reportException(exx);
//						}
//					}
//				}
//			}
//
//			String[]n = new String[l.size()];
//			l.toArray(n);
//			try {
//				String sc = "";
//				for( String c : n ) {
//					sc += c+" ";
//				}
//				infoText("starting: "+sc );
//				p = Runtime.getRuntime().exec( n, PerforceOptions.getPropertyValues());
//				InputStream os = p.getInputStream();
//				final BufferedReader rd = new BufferedReader( new InputStreamReader(os));
//				InputStream es = p.getErrorStream();
//				final BufferedReader erd = new BufferedReader( new InputStreamReader(es));
//				p.getOutputStream().close();
//				final PerforceOutputTopComponent potc = PerforceOutputTopComponent.findInstance();
//
//				new Thread() {
//					public void run(){
//						String str;
//						try {
//							try {
//								while( (str = rd.readLine()) != null ) {
//									infoText(str);
//									outl.add(str);
//									if( str.indexOf( "#") == -1 )
//										continue;
//
//									str = str.split("#")[0];
//									if( type[0].equals("edit") ) {
//										potc.edit(str);
//									} else if( type[0].equals("add") ) {
//										potc.add( str );
//									} else if( type[0].equals("submit") ) {
//										potc.submit(str);
//									} else if( type[0].equals("delete") ) {
//										potc.delete( str );
//									} else if( type[0].equals( "revert") ) {
//										potc.revert(str);
//									}
//								}
//							} finally {
//								rd.close();
//							}
//						} catch( Exception ex){
//							log.log(Level.WARNING,ex.toString(),ex);
//						}
//					}
//				}.start();
//
//				new Thread() {
//					public void run(){
//						String str;
//						try {
//							try {
//								while( (str = erd.readLine()) != null ) {
//									infoText( str );
//									eoutl.add(str);
//								}
//							} finally {
//								erd.close();
//							}
//						} catch( Exception ex){
//							 log.log(Level.WARNING,ex.toString(),ex);
//						}
//					}
//				}.start();
//			 } catch( Exception ex ) {
//				log.log(Level.SEVERE,ex.toString(),ex);   
//			}
//			openWindow();
//		}
//
//		private int countChildren( FileObject[] fos ) {
//			int cnt = 0;
//			for( FileObject f: fos ) {
//				if( f.isFolder() ) {
//					cnt += countChildren( f.getChildren() );
//				} else {
//					++cnt;
//				}
//			}
//			return cnt;
//		}
//
//		public java.util.List<String> getOutput() {
//			return outl;
//		}
//
//		public java.util.List<String> getError() {
//			return eoutl;
//		}
//	}

	public static void openWindow() {
		PerforceOutputTopComponent.findInstance().open();
		PerforceOutputTopComponent.findInstance().requestActive();
		PerforceOutputTopComponent.findInstance().requestVisible();
	}

	private void showResults( P4Command pc ) {
		java.util.List<String>out = pc.getOutput();
		for( String s: out ) {
			PerforceCommand.infoText(s);
		} 
	}

	protected void performAction( final Node[] activatedNodes ) {
//        System.out.println("dumping node files");
//        for( Node n : activatedNodes ) {
//            DataObject dob = (DataObject)n.getCookie(DataObject.class);
//            Set<FileObject> s = (Set<FileObject>)dob.files();
//            for( FileObject f : s ) {
//                System.out.println(n.getName()+": file "+f.getPath());            
//            }
//        }
        LifecycleManager.getDefault().saveAll(); 
		if( ask ) {
			JPanel p = new JPanel();
			Packer pk = new Packer( p );
			int y = -1;
			if( activatedNodes.length < 6 ) {
				pk.pack( new JLabel( "Really " + Arrays.toString(types) )
					).gridx(0).gridy(++y).fillx().inset(6,6,6,6);
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
						pk.pack(new JLabel(f.getName())).gridx(0).gridy(++y).inset(2,2,2,2).fillx();
					}
			   }
			} else {
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
						PerforceCommand.infoText("Starting diff");
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
						log.info("dialog action: "+ev.getSource());
						cancelled[0] = ev.getSource() == bs[2];
						if( ev.getSource() != bs[1] ) {
							dlg[0].setVisible(false);
							dlg[0].dispose();
						}
					}
				}
			);

			dlg[0].pack();
			dlg[0].setVisible( true );
			log.info("dialog closes, cancelled="+cancelled[0]);
			if( cancelled[0] )
				return;
		}

		try {
//            for( Node n: activatedNodes ) {
//                System.out.println("n.getParent(): "+n.getParentNode());
//             
//                    Children cn = n.getChildren();
//                    Node[] cns = cn.getNodes();
//                    for( Node c : cns ) {
//                        System.out.println("Child node: "+c);
//                        
//                    }
//            }
//			final P4Command fp4c = new P4Command( new String[]{"files"}, activatedNodes );
//			final Process fpp = fp4c.getProcess();
//			new ComponentUpdateThread() {
//				public Object construct() {
//					try {
//						return new Integer( fpp.waitFor() );
//					} catch( Exception ex ) {
//						log.log( Level.SEVERE, ex.toString(), ex );
//					}
//					return new Integer(-1);
//				}
//				public void finished() {
//					try {
//						int code = ((Integer)getValue()).intValue();
//						if( code != 0 ) {
//							showError( fp4c, code );
//						} else {
							final P4Command p4c = new P4Command( types, activatedNodes );
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
//						}
//					} finally {
//						super.finished();
//					}
//				}
//			}.start();
		} catch( Exception exx ) {
			if( exx instanceof IllegalStateException == false )
				reportException(exx);
		}
	}

	private void showError( P4Command p4c, int code ) {
		PerforceCommand.showError( p4c, code );
	}
	private void oshowError( P4Command p4c, int code ) {
		JPanel p = new JPanel();
		Packer pk = new Packer( p );
		int y = -1;
		JLabel l;
		pk.pack(l = new JLabel(p4c.getCommand()+" exited: "+code)).gridx(0).gridy(++y).inset(6,6,6,6);
		l.setFont( new Font( "courier",Font.BOLD,16));
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,10,5,10);
		java.util.List<String>strs = p4c.getOutput();
		Font f = new Font( "courier",Font.PLAIN,12);

		for( int i = 0; i < strs.size(); ++i ) {
			pk.pack( l=new JLabel(strs.get(i) ) ).gridx(0).gridy(++y).fillx();
			l.setFont(f);
			if( i + 1 < strs.size() && i > 2 ) {
				pk.pack( l=new JLabel("...")).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
				l.setFont(f);
				break;
			}
		}
		if( strs.size() > 0 ) {
			pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(5,5,5,5);
		}
		strs = p4c.getError();
		for( String s : strs ) {
			pk.pack( l=new JLabel(s) ).gridx(0).gridy(++y).fillx().inset(0,5,0,0);
			l.setFont(f);
		}
		pk.pack( new JSeparator() ).gridx(0).gridy(++y).fillx().inset(10,10,10,10);
		final Dialog dlg[] = new Dialog[1];
		final JButton bs[] = new JButton[1];
		bs[0] = new JButton( NbBundle.getMessage( getClass(), "okay_button" ) );

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

	protected void initialize() {
		super.initialize();
		// see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
	if( icon == null )
		putValue("noIconInMenu", Boolean.TRUE);
	}

	private void reportException(Throwable exx){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter( sw );
		exx.printStackTrace(pw);
		pw.close();
		PerforceCommand.infoText( sw.toString() );
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
	protected String iconResource() {
		return icon == null ? null : "org/wonderly/netbeans/perforce/"+icon;

   }

	public HelpCtx getHelpCtx() {
		return HelpCtx.DEFAULT_HELP;
	}

	protected boolean asynchronous() {
		return false;
	}
}

