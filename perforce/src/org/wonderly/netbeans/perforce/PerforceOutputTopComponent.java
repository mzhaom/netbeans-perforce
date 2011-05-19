package org.wonderly.netbeans.perforce;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.html.HTML;
import org.openide.ErrorManager;
import org.openide.cookies.EditCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.wonderly.awt.Packer;
import org.wonderly.swing.VectorListModel;
import org.wonderly.swing.prefs.SwingPreferencesMapper;

/**
 * Top component which displays something.
 */
final class PerforceOutputTopComponent extends TopComponent {

    private static final long serialVersionUID = 1L;
	VectorListModel<String> histmod;
	private SwingPreferencesMapper mp;
	private Logger log = Logger.getLogger( PerforceOutputTopComponent.class.getName() );
    private static PerforceOutputTopComponent instance;
    /** path to the icon used by the component and its open action */
    static final String ICON_PATH = "org/wonderly/netbeans/perforce/p4.gif";

    private static final String PREFERRED_ID = "PerforceOutputTopComponent";

    private PerforceOutputTopComponent() {
        initComponents();
		lc = new LinkController();
		outputArea.addMouseListener( lc );
		outputArea.addMouseMotionListener( lc );
        setName(NbBundle.getMessage(PerforceOutputTopComponent.class, "CTL_PerforceOutputTopComponent"));
        setToolTipText(NbBundle.getMessage(PerforceOutputTopComponent.class, "HINT_PerforceOutputTopComponent"));
        setIcon( ImageUtilities.loadImage(ICON_PATH, true));
    }

	public @Override boolean canClose() {
		return super.canClose();
	}
	public void edit( final String name ) {
		runInSwing( new Runnable() {
			public void run() {
				sort("="+name);
				log.fine( "edit size: "+histmod.size() );
			}
		});
	}
	
	public void add( final String name ) {
		runInSwing( new Runnable() {
			public void run() {
				sort("+"+name);				
				log.fine( "add size: "+histmod.size() );
			}
		});
	}

	public void delete( final String name ) {
		runInSwing( new Runnable() {
			public void run() {
				sort("-"+name);				
				log.fine( "delete size: "+histmod.size() );
			}
		});
	}

	public void submit( final String name ) {
		if (log.isLoggable(Level.FINE))
			log.fine("submit: "+name );
		runInSwing( new Runnable() {
			public void run() {
				histmod.removeElement( "+"+name );
				histmod.removeElement( "-"+name );
				histmod.removeElement( "="+name );
				sort();
			}
		});
	}

	public void revert( final String name ) {
		if (log.isLoggable(Level.FINE))
			log.fine("revert: "+name);
		runInSwing( new Runnable() {
			public void run() {
				histmod.removeElement( "+"+name );
				histmod.removeElement( "-"+name );
				histmod.removeElement( "="+name );
				sort();
			}
		});
	}
	
	private void runInSwing( final Runnable r ) {
		if( SwingUtilities.isEventDispatchThread() ) {
			r.run();
		} else {
			try {
				SwingUtilities.invokeAndWait( r );
			} catch( Exception ex ) {
			}
		}
	}

	private void sort( String val ) {
		Level olevel = log.getLevel();
		log.setLevel( Level.FINE );
		log.fine("sorting: "+val);
		if( histmod.contains( val ) == false ) {
			log.fine( "after contains size: "+histmod.size() );
			log.fine("adding: "+val);
			Vector<String> v = new Vector<String>();
			for( String n : histmod ) {
				v.add(n);
			}
			v.add( val );
			log.fine( "after add size: "+histmod.size() );
			histmod.setContents( v );
			sort();
			histout.repaint();
			log.fine( "after paint size: "+histmod.size() );
			for( String s: histmod ) {
				log.fine("hist: "+s);
			}
		}
		log.setLevel( olevel );
	}

	private void sort() {
		log.fine( "before sort size: "+histmod.size() );
		Collections.sort( histmod, new Comparator() {
			public int compare( Object o1, Object o2 ) {
				return o1.toString().compareTo( o2.toString() );
			}
		});
		histout.setModel( histmod );
		log.fine( "after sort size: "+histmod.size() );
		histout.repaint();
	}
	LinkController lc;

	public class LinkController extends MouseAdapter implements MouseMotionListener {
		public void mouseMoved( MouseEvent ev ) {
			JTextPane editor = (JTextPane)ev.getSource();
			if( !editor.isEditable() ) {
				Point pt = new Point( ev.getX(), ev.getY() );
				int pos = editor.viewToModel( pt );
				if( pos >= 0 ) {
					Document doc = editor.getDocument();
					if( doc instanceof DefaultStyledDocument  == false )
						return;
					DefaultStyledDocument hdoc = (DefaultStyledDocument)doc;
					Element e = hdoc.getCharacterElement(pos);
					AttributeSet a = e.getAttributes();
					String file = (String)a.getAttribute( HTML.Attribute.HREF );
					if( file != null ) {
						if( isShowToolTip() ) {
							editor.setToolTipText( "Click to edit \""+new File( file).getName()+"\"" );
						}
						if( isShowCursorFeedback() ) {
							if( editor.getCursor() != handCursor ) {
								editor.setCursor( handCursor );
							}
						}
					} else {
						if( isShowToolTip() ) {
							editor.setToolTipText( null );
						}
						if( isShowCursorFeedback()) {
							if( editor.getCursor() != defaultCursor ) {
								editor.setCursor( defaultCursor );
							}
						}
					}
				} else {
					editor.setToolTipText( null );
				}
			}
		}

		private void debugOut( String ss ) {
			if (log.isLoggable(Level.FINE))
				log.fine(ss);
		}

		public @Override void mouseClicked( MouseEvent ev ) {
			final JTextPane editor = (JTextPane)ev.getSource();
			if( !editor.isEditable() ) {
				if (log.isLoggable(Level.INFO)) {
					log.info("got mouse click, in top componet, checking hyperlink");
				}
				final Point pt = new Point( ev.getX(), ev.getY() );
				new Thread() {
					public void run() {

						int pos = editor.viewToModel( pt );

						debugOut("Clicked for hyper pos="+pos+"\n" );

						if( pos >= 0 ) {
							Document doc = editor.getDocument();
							if( doc instanceof DefaultStyledDocument  == false ) {
								debugOut( "doc type is not default styled: "+doc.getClass().getName()+"\n" );
								return;
							}
							DefaultStyledDocument hdoc = (DefaultStyledDocument)doc;
							Element e = hdoc.getCharacterElement(pos);
							AttributeSet a = e.getAttributes();
							String file = (String)a.getAttribute( HTML.Attribute.HREF );
							debugOut( "file is="+file+" ("+file.getClass().getName()+")"+"\n" );

							if( file != null ) {
								long id = ((Long)a.getAttribute( "ID")).longValue();
								HyperlinkListener hl = hlis.get(id);
								debugOut(  "file id="+id+", issuing hyperlinkUpdate to:" +hl+"\n" );

								try {
									debugOut("find file: "+file+"\n"  );
									// It's not really possible to figure out the right '-d' argument
									// here without some other information.
									PerforceCommand.P4Command p4c = new PerforceCommand.P4Command(
										new String[]{
											//"-d", new File(file).getParent(),
											"-z", "tag",
											"where"
										},
										new String[]{ file }, null, false, false );
									int c = -1;
									debugOut("Waiting for where to exit\n");
									try {
										c = p4c.waitFor();
									} catch (InterruptedException ex) {
										Exceptions.printStackTrace(ex);
									}
									debugOut("p4 where exited: "+c+"\n");
									List<String>out = p4c.getOutput();
									debugOut("mapping is: "+out+"\n");
									String pth = null;
									for( String s : out ) {
										if( s.startsWith( "... path ") ) {
											pth = s.substring("... path ".length() );
											break;
										}
									}
									debugOut("returning file: URL to \""+pth+"\"");
									if( pth != null )
										hl.hyperlinkUpdate( new HyperlinkEvent(doc, EventType.ACTIVATED, new URL("file:/"+pth), file, e));
								} catch ( Exception ex) {
									Exceptions.printStackTrace(ex);
								}
							}
						}
					}
				}.start();
			}
		}
		
		Cursor defaultCursor = Cursor.getDefaultCursor();
		Cursor handCursor = Cursor.getPredefinedCursor( Cursor.HAND_CURSOR );

		private boolean isShowCursorFeedback() {
			return true;
		}

		private boolean isShowToolTip() {
			return true;
		}
	}

	private static ConcurrentHashMap<Long,HyperlinkListener> hlis = 
		new ConcurrentHashMap<Long,HyperlinkListener> ();

	AtomicLong id = new AtomicLong();
	public void addOutput( final String before, final String str, final String after ) {
		log.fine(str);
		runInSwing( new Runnable() {
			public void run() {
				try {
					Color c = getForeground();
					outputArea.setForeground(c);
					SimpleAttributeSet attrs = new SimpleAttributeSet();
					StyleConstants.setUnderline( attrs,  true );
					StyleConstants.setForeground( attrs, Color.blue.darker() );
					final long i;
					attrs.addAttribute( "ID", i = id.incrementAndGet() );
					HyperlinkListener hl = new HyperlinkListener() {
						public void hyperlinkUpdate(HyperlinkEvent e) {
							try {
								Element em  = e.getSourceElement();
								String fname = em.getAttributes().getAttribute( HTML.Attribute.HREF ).toString();
								//setForeground( Color.orange );
								//addOutput("should open: "+e.getURL()+": "+e.getURL().openConnection()+"\n");
								FileObject fo = FileUtil.toFileObject( new File( e.getURL().getPath() ) );
								DataObject dob = DataObject.find( fo );
								String mt = fo.getMIMEType();
								EditCookie ec = (EditCookie)dob.getCookie( EditCookie.class );
								ec.edit();
							} catch( Exception ex ) {
								setForeground( Color.red );
								addOutput( ex.toString()+"\n" );
							}
						}
					};
					hlis.put( i, hl );
					attrs.addAttribute( HTML.Attribute.HREF, str );
					StyledDocument doc = outputArea.getStyledDocument();
					doc.insertString( doc.getLength(), before, null );
					doc.insertString(doc.getLength(), str, attrs);
					doc.insertString( doc.getLength(), after, null );
					outputArea.setCaretPosition(outputArea.getDocument().getLength());
					outScroller.getVerticalScrollBar().setValue(outScroller.getVerticalScrollBar().getMaximum());
					outputArea.repaint();
					setForeground(Color.black );
				} catch (BadLocationException ex) {
					Exceptions.printStackTrace(ex);
				}
			}
		});
	}


	public void addOutput( final String str ) {
		log.fine(str);
		runInSwing( new Runnable() {
			public void run() {
				try {
					Color c = getForeground();
					outputArea.setForeground(c);
					Style style = outputArea.addStyle("" + c, null);
					StyleConstants.setForeground(style, c);
					StyledDocument doc = outputArea.getStyledDocument();
					doc.insertString(doc.getLength(), str, style);
					outputArea.setCaretPosition(outputArea.getDocument().getLength());
					outScroller.getVerticalScrollBar().setValue(outScroller.getVerticalScrollBar().getMaximum());
					outputArea.repaint();
					setForeground(Color.black );
				} catch (BadLocationException ex) {
					Exceptions.printStackTrace(ex);
				}
			}
		});
	}

//    public javax.swing.JTextArea getTextArea() {
//        return outputArea;
//    }

    private void initComponents() {
        popupMenu = new javax.swing.JPopupMenu();

        splitHori = new JRadioButtonMenuItem("Split Horizontal");
        splitHori.setSelected(true);
        splitVert = new JRadioButtonMenuItem("Split Vertical");
		final JMenuItem clear = new JMenuItem("Clear Output");
		clear.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				outputArea.setText("");
				hlis.clear();
			}
		});
        jSeparator1 = new javax.swing.JSeparator();
        showList = new JCheckBoxMenuItem("Show File List");
        showMessages = new JCheckBoxMenuItem("Show Perforce Messages");

        splitBtnGroup = new javax.swing.ButtonGroup();
        mainpanel = new javax.swing.JPanel();
        splitpane = new javax.swing.JSplitPane();
        histPanel = new javax.swing.JPanel();
        histout = new javax.swing.JList( histmod = new VectorListModel<String>() );
        histScroller = new javax.swing.JScrollPane( histout );
        outPanel = new javax.swing.JPanel();
        outputArea = new javax.swing.JTextPane();
		outputArea.setEditable( false );
        outScroller = new javax.swing.JScrollPane(outputArea);

		splitHori.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				splitHoriActionPerformed( ev );
			}
		});

		splitVert.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				splitVertActionPerformed( ev );
			}
		});

		showList.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				showListActionPerformed( ev );
			}
		});

		showMessages.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent ev ) {
				showMessagesActionPerformed( ev );
			}
		});

        popupMenu.add(splitHori);
        popupMenu.add(splitVert);
        popupMenu.add(jSeparator1);
        popupMenu.add(showList);
        popupMenu.add(showMessages);
        popupMenu.addSeparator();
        popupMenu.add(clear);

        splitBtnGroup.add( splitHori );
        splitBtnGroup.add( splitVert );
		
		Preferences pr = Preferences.userNodeForPackage( getClass() );
		mp = new SwingPreferencesMapper( pr );

		mp.map("splitHori", false, splitHori );
		splitVert.setSelected( !splitHori.isSelected() );

		mp.map( "showMsgs", true, showMessages );
		mp.map( "showList", true, showList );
        splitpane.setDividerLocation(100);
		if( splitHori.isSelected() == false && splitVert.isSelected() == false )
			splitHori.setSelected( true );
        splitpane.setOrientation( splitHori.isSelected() ?
            JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT );
        splitpane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
        });

        histPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Depot File List"));
        histout.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
        });
        Packer jpk2 = new Packer( histPanel );
        jpk2.pack( histScroller ).fillboth();

        outPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Perforce Output"));
//        outputArea.setColumns(20);
//        outputArea.setRows(5);
        outputArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                popupHandler(evt);
            }
        });
        Packer jpk1 = new Packer( outPanel );
        jpk1.pack( outScroller ).fillboth();

        splitpane.setLeftComponent(histPanel);
        splitpane.setRightComponent(outPanel);
        
        Packer pk = new Packer( this );
        pk.pack( mainpanel ).fillboth();
		setupPane();
    }

	private void splitVertActionPerformed(java.awt.event.ActionEvent evt) {
		this.firePropertyChange( "splitVertical", !splitVert.isSelected(), splitVert.isSelected() );
		splitpane.setOrientation( JSplitPane.VERTICAL_SPLIT );
		splitpane.setDividerLocation(.5);
		splitpane.repaint();
		mp.commit();
	}

	private void splitHoriActionPerformed(java.awt.event.ActionEvent evt) {
		this.firePropertyChange( "splitVertical", !splitVert.isSelected(), splitVert.isSelected() );
		splitpane.setOrientation( JSplitPane.HORIZONTAL_SPLIT );
		splitpane.setDividerLocation(.5);
		splitpane.repaint();
		mp.commit();
	}

	private void setupPane() {

		boolean isVisMsgs = showMessages.isSelected();
		boolean isVisHist = showList.isSelected();
		
		if( !isVisMsgs && !isVisHist ) {
			showMessages.setSelected( isVisMsgs = true );
			showList.setSelected( isVisHist = true );
		}

		if( isVisHist && isVisMsgs ) {
			splitHori.setEnabled(true);
			splitVert.setEnabled(true);
			showList.setEnabled(true);
			showMessages.setEnabled(true);
			Packer pk = new Packer( mainpanel );
			mainpanel.remove( outPanel );
			mainpanel.remove( histPanel );
			pk.pack( splitpane ).fillboth();
			splitpane.setLeftComponent(histPanel);
			splitpane.setRightComponent(outPanel);
			mainpanel.revalidate();
			mainpanel.repaint();
			mp.commit();
		} else if( isVisHist ) {
			splitHori.setEnabled(false);
			splitVert.setEnabled(false);
			mainpanel.remove( splitpane );
			Packer pk = new Packer( mainpanel );
			pk.pack( histPanel ).fillboth();
			showList.setEnabled(false);
			mainpanel.revalidate();
			mainpanel.repaint();
			mp.commit();
		} else if( isVisMsgs ) {
			splitHori.setEnabled(false);
			splitVert.setEnabled(false);
			mainpanel.remove( splitpane );
			Packer pk = new Packer( mainpanel );
			pk.pack( outPanel ).fillboth();
			showMessages.setEnabled(false);
			mainpanel.revalidate();
			mainpanel.repaint();
			mp.commit();
		} else {
			log.warning("unexpected option configuration for visible panels");
		}
	}

	private void showListActionPerformed(java.awt.event.ActionEvent evt) {
		setupPane();
		this.firePropertyChange( "showHistory", !showList.isSelected(), showList.isSelected() );
	}

	private void showMessagesActionPerformed(java.awt.event.ActionEvent evt) {
		setupPane();
		this.firePropertyChange( "showMessages", !showMessages.isSelected(), showMessages.isSelected() );
	}

	private void popupHandler(java.awt.event.MouseEvent evt) {
		if( evt.isPopupTrigger() ) {
			popupMenu.show( (JComponent)evt.getSource(), evt.getX(), evt.getY() );

		}
	}
    
    // Variables declaration - do not modify
    private javax.swing.JScrollPane histScroller;
    private javax.swing.JList histout;
    private javax.swing.JPanel outPanel;
    private javax.swing.JPanel histPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPanel mainpanel;
    private javax.swing.JScrollPane outScroller;
    private javax.swing.JTextPane outputArea;
    private javax.swing.JPopupMenu popupMenu;
    private javax.swing.JCheckBoxMenuItem showList;
    private javax.swing.JCheckBoxMenuItem showMessages;
    private javax.swing.ButtonGroup splitBtnGroup;
    private javax.swing.JRadioButtonMenuItem splitHori;
    private javax.swing.JRadioButtonMenuItem splitVert;
    private javax.swing.JSplitPane splitpane;
    // End of variables declaration
    
    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized PerforceOutputTopComponent getDefault() {
        if (instance == null) {
            instance = new PerforceOutputTopComponent();
        }
        return instance;
    }
    
    /**
     * Obtain the PerforceOutputTopComponent instance. Never call {@link #getDefault} directly!
     */
    public static synchronized PerforceOutputTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            ErrorManager.getDefault().log(ErrorManager.WARNING, "Cannot find PerforceOutput component. It will not be located properly in the window system.");
            return getDefault();
        }
        if (win instanceof PerforceOutputTopComponent) {
            return (PerforceOutputTopComponent)win;
        }
        ErrorManager.getDefault().log(ErrorManager.WARNING, "There seem to be multiple components with the '" + 
			PREFERRED_ID + "' ID. That is a potential source of errors and unexpected behavior: "+win+" >> \""+
			WindowManager.getDefault().findTopComponentID( win ) +"\"");
        return getDefault();
    }
    
    public @Override int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }
    
    public @Override void componentOpened() {
        // TODO add custom code on component opening
		super.componentOpened();
    }

    public @Override void componentDeactivated() {
        // TODO add custom code on component closing
		super.componentDeactivated();
    }
    public @Override void componentClosed() {
        // TODO add custom code on component closing
		super.componentClosed();
    }
    
    /** replaces this in object stream */
    public @Override Object writeReplace() {
        return new ResolvableHelper();
    }
    
    protected @Override String preferredID() {
        return PREFERRED_ID;
    }
    
    final static class ResolvableHelper implements Serializable {
        private static final long serialVersionUID = 1L;
        public Object readResolve() {
            return PerforceOutputTopComponent.getDefault();
        }
    }
}
