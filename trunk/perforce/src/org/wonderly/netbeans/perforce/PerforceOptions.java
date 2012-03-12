/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.wonderly.netbeans.perforce;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.wonderly.awt.Packer;
import org.wonderly.swing.ModalComponent;
import org.wonderly.swing.prefs.SwingPreferencesMapper;

/**
 *
 * @author gregg
 */
public class PerforceOptions  extends AdvancedOption {
	private static Logger log = Logger.getLogger( PerforceOptions.class.getName() );

	static SwingPreferencesMapper pm;
	public @Override String getDisplayName() {
		return "Perforce";
	}

	public PerforceOptions() {
		super();
	}
	public @Override String getTooltip() {
		return "Perforce Depot Options";
	}

	public static String[] getPropertyValues() {
		//if( true ) return null;
		Map<String,String> p = System.getenv();
		List<String>vals = new ArrayList<String>();
		for( String key : p.keySet() ) {
			vals.add( key+"="+p.get(key));
		}
		//if( true) return null;
		if( p4cfg.isSelected() ) {
			vals.add("P4CONFIG="+p4config.getText());
		} else {
			vals.add( "P4USER="+p4user.getText() );
			vals.add( "P4PORT="+p4port.getText() );
			vals.add( "P4CLIENT="+p4client.getText() );
		}
		vals.add( "P4DIFF="+p4diff.getText() );
		String[]arr = new String[vals.size()];
		vals.toArray(arr);
		return arr;
	}

	private static JRadioButton p4cfg, p4vals;
	private static JTextField p4config;
	private static JTextField p4user, p4port, p4client, p4diff;
	
	public static String p4DiffPath() {
		return p4diff == null ? "p4merge" : p4diff.getText();
	}

	static void dumpPrefs( Preferences p ) {
		String[] arr;
		try {
			arr = p.keys();
		} catch (BackingStoreException ex) {
			Exceptions.printStackTrace(ex);
			return;
		}
		for( String key: arr ) {
			log.info("pref: "+key+"="+p.get(key, "<unset>") );
		}
		try {
			arr = p.childrenNames();
		} catch (BackingStoreException ex) {
			Exceptions.printStackTrace(ex);
			return;
		}
		for( String key: arr ) {
			log.info("pref: child="+key );
		}

	}
	static {
		Preferences prefs = NbPreferences.forModule( PerforceOutputTopComponent.class );
		dumpPrefs( prefs );
		pm = new SwingPreferencesMapper( prefs );
		p4cfg = new JRadioButton("Use P4CONFIG");
		p4vals = new JRadioButton("Use Separate P4 Vars");
		p4config = new JTextField(10);
		p4client = new JTextField(10);
		p4diff = new JTextField(20);
		p4port = new JTextField(10);
		p4user = new JTextField(10);
		ButtonGroup grp = new ButtonGroup();
		grp.add( p4cfg );
		grp.add( p4vals );
		mapPrefs("static launch");
		pm.commit();
	}

	private static final void mapPrefs(String how) {
		Logger.getLogger("org.wonderly.swing.prefs").setLevel(Level.FINER);
		pm.map("p4cfg", false, p4cfg );
		pm.map("p4vals", true, p4vals );
		log.info(how+": config: "+p4cfg.isSelected()+", vals: "+p4vals.isSelected() );
		pm.map("p4config", ".p4config", p4config);
		String u = System.getProperty("P4USER");
		log.info(how+":P4USER="+u+", userdir="+new File(System.getProperty("user.home") ).getName() );
		pm.map( "p4user", u == null ? new File(System.getProperty("user.home")).getName() : u, p4user );
		String nm;
		try {
			nm = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException ex) {
			Exceptions.printStackTrace(ex);
			nm = System.getProperty("P4CLIENT");
		}
		pm.map("p4client", nm, p4client);
		pm.map("p4diff", "p4merge", p4diff );
		log.info("using p4 diff command: \""+p4diff.getText());

		String port = System.getProperty("P4PORT");
		if( port == null )
			port = "perforce:1666";
		pm.map( "p4port", port, p4port );
	}
	public @Override OptionsPanelController create() {
		return new OptionsPanelController() {
			List<PropertyChangeListener>lis = new ArrayList<PropertyChangeListener>();
			public @Override void update() {
				log.info("update(), config: "+p4cfg.isSelected()+", vals: "+p4vals.isSelected() );
				pm.map("p4cfg", false, p4cfg );
				pm.map("p4vals", true, p4vals );
				pm.map("p4config", ".p4config", p4config);
				String u = System.getProperty("P4USER");
				log.info("P4USER="+u+", userdir="+new File(System.getProperty("user.home") ).getName() );
				pm.map( "p4user", u == null ? new File(System.getProperty("user.home")).getName() : u, p4user );
				String nm;
				try {
					nm = InetAddress.getLocalHost().getHostName();
				} catch (UnknownHostException ex) {
					Exceptions.printStackTrace(ex);
					nm = System.getProperty("P4CLIENT");
				}
				pm.map("p4client", nm, p4client);
				pm.map("p4diff", "p4merge", p4diff );

				String port = System.getProperty("P4PORT");
				if( port == null )
					port = "perforce:1666";
				pm.map( "p4port", port, p4port );
				map.put("p4port",p4port.getText());
				map.put("p4user",p4user.getText());
				map.put("p4client",p4client.getText());
				map.put("p4config",p4config.getText());
				map.put("p4cfg",p4cfg.isSelected());
				map.put("p4vals",p4vals.isSelected());
			}

			public @Override void applyChanges() {
				log.info("applyChanges(), config: "+p4cfg.isSelected()+", vals: "+p4vals.isSelected());
				pm.commit();
				pm.map("p4cfg", false, p4cfg );
				pm.map("p4vals", true, p4vals );
				log.info("applyChanges() after map, config: "+p4cfg.isSelected()+", vals: "+p4vals.isSelected());
				propChange( "p4config", p4cfg.getText() );
				propChange( "p4user", p4user.getText() );
				propChange( "p4port", p4port.getText() );
				propChange( "p4client", p4client.getText() );
				propChange( "p4diff", p4diff.getText() );
				propChange( "p4useconfig", p4cfg.isSelected() );
				propChange( "p4usevals", p4vals.isSelected() );
			}

			Map<String,Object> map = new ConcurrentHashMap<String,Object>();
			private void propChange( String name, Object value ) {
				Object last = map.get(name);
				if( last != null ) {
					if( last.equals(value) )
						return;
				}
				map.put( name, value );
				PropertyChangeEvent ev = new PropertyChangeEvent( this, name, last, value );
				for( PropertyChangeListener l : new ArrayList<PropertyChangeListener>( lis ) ) {
					l.propertyChange( ev );
				}
				pm.commit();
			}

			public @Override void cancel() {
				log.info("cancel()");
			}

			public @Override boolean isValid() {
				log.info("isValid()");
				return true;
			}

			public @Override boolean isChanged() {
				log.info("isChanged()");
				return isMod;
			}

			boolean isMod = false;

			public @Override JComponent getComponent(Lookup arg0) {
				log.info("getComponent(): "+pm.getPreferencesNode().absolutePath());
				JPanel p = new JPanel();
				Packer pk = new Packer( p) ;
//				final JCheckBox p4cfg;
				int y = -1;

				pk.pack( p4cfg ).gridx(0).gridy(++y).fillx().gridw(2);
				pk.pack( new JLabel("P4CONFIG:") ).gridx(0).gridy(++y).inset(2,0,0,0);
				pk.pack( p4config ).gridx(1).gridy(y).fillx().inset(2,4,0,0);
				pk.pack( new JSeparator() ).gridx(0).gridw(2).gridy(++y).fillx().inset(6,6,6,6);
				pk.pack( p4vals ).gridx(0).gridy(++y).fillx().gridw(2);
				ActionListener chg = new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						isMod = true;
					}
				};
				p4cfg.addActionListener( chg );
				p4vals.addActionListener( chg );
				JPanel lp = new JPanel();
				pk.pack( lp ).gridx(0).gridy(++y).fillboth().gridw(2);
				Packer lpk = new Packer( lp );
				y = -1;

				lpk.pack( new JLabel("P4USER:") ).gridx(0).gridy(++y).inset(2,0,0,0).west();
				lpk.pack( p4user ).gridx(1).gridy(y).fillx().inset(2,4,0,0);

				lpk.pack( new JLabel("P4CLIENT:") ).gridx(0).gridy(++y).inset(2,0,0,0).west();
				lpk.pack( p4client ).gridx(1).gridy(y).fillx().inset(2,4,0,0);

				lpk.pack( new JLabel("P4PORT:") ).gridx(0).gridy(++y).inset(2,0,0,0).west();
				lpk.pack( p4port ).gridx(1).gridy(y).fillx().inset(2,4,0,0);
				lpk.pack( new JSeparator() ).gridy(++y).fillx().gridw(2).inset(4,4,4,4);
				lpk.pack( new JLabel("P4DIFF command:") ).gridy(++y).gridx(0);
				lpk.pack( p4diff ).gridy(y).gridx(1).fillx();
				lpk.pack( new JPanel() ).gridy(++y).filly();

				DocumentListener dlis = new DocumentListener() {

					public void insertUpdate(DocumentEvent e) {
						isMod = true;
					}

					public void removeUpdate(DocumentEvent e) {
						isMod = true;
					}

					public void changedUpdate(DocumentEvent e) {
						isMod = true;
					}

				};
				p4config.getDocument().addDocumentListener( dlis );
				p4user.getDocument().addDocumentListener( dlis );
				p4client.getDocument().addDocumentListener( dlis );
				p4diff.getDocument().addDocumentListener( dlis );
				p4port.getDocument().addDocumentListener( dlis );

				ModalComponent cmc = new ModalComponent( p4cfg );
				ModalComponent mc = new ModalComponent( p4vals );
				mc.add( p4client );
				mc.add( p4user );
				mc.add( p4port );
				mc.configure();
				cmc.add( p4config );
				cmc.relate( mc );
				mc.relate( cmc );
				cmc.configure();
				mapPrefs("getComponent()");
				return p;
			}

			public @Override HelpCtx getHelpCtx() {
				log.info("getHelpCtx()");
				return null;
			}

			public @Override void addPropertyChangeListener(PropertyChangeListener listener) {
				log.info("addPropertyChangeListener()");
				lis.add( listener );
			}

			public @Override void removePropertyChangeListener(PropertyChangeListener listener) {
				log.info("removePropertyChangeListener()");
				lis.remove( listener );
			}
		};
	}
}
