/*
 * P4ChangeList.java
 *
 * Created on April 11, 2006, 1:33 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.netbeans.perforce;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.wonderly.awt.Packer;

/**
 *
 * @author gregg
 */
public class P4ChangeList implements InputStreamProvider {
	int change;
	String user, client;
	List<String>files;
	String comment;
	List<CheckControl>clist;
	Logger log = Logger.getLogger( getClass().getName() );

	public String toString() {
		return "changeList#"+change+"("+user+","+client+")";
	}

	/** Creates a new instance of P4ChangeList */
	public P4ChangeList( String user, String client, int cno, String comment ) {
		change = cno;
		this.user = user;
		this.client = client;
		this.comment = comment;
		files = new ArrayList<String>();
	}

	/** Creates a new instance of P4ChangeList */
	public P4ChangeList( int cno ) {
		this( cno, "" );
	}

	/** Creates a new instance of P4ChangeList */
	public P4ChangeList( int cno, String comment ) {
		this( System.getProperty("P4USER"), System.getProperty("P4CLIENT"), cno, comment );
	}

	public void add( FileObject fob ) {
		add( fob.getPath() );
	}
	
	public void add( String file ) {
		log.info("Adding file to changelist #"+change+" list: "+file);
		if( files.contains(file) == false )
			files.add( file );
	}

	public void cancel() {
		try {
			if( PerforceOps.removeChangeList(change) )
				PerforceCommand.infoText("Change",change+" deleted");
			else
				PerforceCommand.errorText("Change",change+" deletion failed");

		} catch (IOException ex) {
			Exceptions.printStackTrace(ex);
		}
	}

	public boolean prepareSubmit() {
		return( getDescription() != null && clist.size() > 0 );
	}


	public void writeTo( OutputStream os ) throws IOException {
		writeTo( new OutputStreamWriter( os ) );
	}
	public void writeTo( Writer os ) throws IOException {
		log.info("Writing change spec to: "+os);
		String desc = getDescription();
		if( desc == null ) {
			throw new IOException("Cancelled Operation");
		}
		PrintWriter ps = new PrintWriter(os);
		ps.println("# A Perforce Change Specification.");
		ps.println("#");
		ps.println("#  Change:      The change number. 'new' on a new changelist.  Read-only.");
		ps.println("#  Date:        The date this specification was last modified.  Read-only.");
		ps.println("#  Client:      The client on which the changelist was created.  Read-only.");
		ps.println("#  User:        The user who created the changelist. Read-only.");
		ps.println("#  Status:      Either 'pending' or 'submitted'. Read-only.");
		ps.println("#  Description: Comments about the changelist.  Required.");
		ps.println("#  Jobs:        What opened jobs are to be closed by this changelist.");
		ps.println("#               You may delete jobs from this list.  (New changelists only.)");
		ps.println("#  Files:       What opened files from the default changelist are to be added");
		ps.println("#               to this changelist.  You may delete files from this list.");
		ps.println("#               (New changelists only.)\n");
		ps.println("Change: new\n");
		ps.println("Client: "+client+"\n");
		ps.println("User: "+user+"\n");
		ps.println("Status: new\n");
		ps.println("Description:");
		ps.println("	"+desc.replace("\n","\n\t")+"\n");
		ps.println("Files:");
		for( CheckControl n: clist ) {
//			PerforceCommand.infoText("Submitting:",n.getText());
			if( n.isSelected() )
				ps.println("\t"+n.getText());
		}
		ps.flush();
	}
	
	String descr;
	
	public String getDescription() {
		synchronized( this ) {
			if( descr != null )
				return descr;
		}
		
		JPanel p = new JPanel();
		Packer pk = new Packer( p );
		int y = -1;
		JTextField t;

		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setTopComponent(p);
		pk.pack( new JLabel("User: ")).gridx(0).gridy(++y).east().inset(4,4,4,4);
		pk.pack( t= new JTextField( user )).gridx(1).gridy(y).fillx();
		t.setEditable(false);

		pk.pack( new JLabel("Client: ")).gridx(0).gridy(++y).east().inset(4,4,4,4);
		pk.pack( t = new JTextField( client )).gridx(1).gridy(y).fillx();
		t.setEditable(false);

		pk.pack( new JLabel("Description: ")).gridx(0).gridy(++y).east().inset(4,4,4,4);
		final JTextArea txt;
		pk.pack( new JScrollPane( txt = new JTextArea( 6, 40 ) ) ).gridx(1).gridy(y).fillboth();
		txt.setWrapStyleWord( true );
		txt.setText( comment.length() == 0 ? "<enter description here>" : comment );
		txt.setLineWrap( true );
		JPanel fp = new JPanel();
		Packer fpk = new Packer(fp);
		clist = new ArrayList<CheckControl>();
		for( String nm: files ) {
			CheckControl cc = new CheckControl( nm );
			fpk.pack( cc ).gridx(0).gridy(++y).fillx(0);
			cc.setSelected(true); 
			clist.add( cc) ;
		}
		fpk.pack( new JPanel() ).gridx(0).gridy(++y).filly(0);
		JPanel span = new JPanel();
		Packer spk = new Packer( span );
		spk.pack( new JScrollPane( fp ) ).gridx(0).gridy(2).gridw(2).fillboth();
		sp.setBottomComponent( span );
//		pk.pack(  ).gridx(0).gridy(++y).fillboth().gridw(2).inset(4,4,4,4);
	
		final JButton bs[] = new JButton[2];
		bs[1] = new JButton( NbBundle.getMessage( getClass(), "cancel_button" ) );
		bs[0] = new JButton( NbBundle.getMessage( getClass(), "okay_button" ) );
		final JButton selall = new JButton( NbBundle.getMessage( getClass(), "select_all_button" ) );
		final JButton deselall = new JButton( NbBundle.getMessage( getClass(), "deselect_all_button" ) );
		spk.pack( selall ).gridx(0).gridy(0).west().inset(2,2,2,2);
		spk.pack( deselall ).gridx(1).gridy(0).east().inset(2,2,2,2);
		spk.pack( new JSeparator() ).gridx(0).gridw(2).gridy(1).fillx().inset(5,5,5,5);
		selall.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ev) {
				for( CheckControl cc : clist ) {
					cc.setSelected(true);
				}

			}
		});
		deselall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				for( CheckControl cc : clist ) {
					cc.setSelected(false);
				}
			}
		});
//		final boolean cancelled[] = new boolean[1];
//
//		bs[1].addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent ev ) {
//				cancelled[0] = true;
//			}
//		});
//
//		bs[0].addActionListener( new ActionListener() {
//			public void actionPerformed( ActionEvent ev ) {
//				cancelled[0] = false;
//			}
//		});
		JPanel psp = new JPanel();
		Packer pspk = new Packer(psp);
		pspk.pack( sp ).fillboth();

		final Dialog dlg[] = new Dialog[1];
		DialogDescriptor desc = new DialogDescriptor( psp, "Enter Change Description",
				true, JOptionPane.OK_CANCEL_OPTION, JOptionPane.OK_OPTION,
				new ActionListener() {
				public void actionPerformed( ActionEvent ev ) {
					dlg[0].setVisible(false);
					dlg[0].dispose();
				}
			} );
		dlg[0] = DialogDisplayer.getDefault().createDialog( desc );
//		dlg[0] = DialogSupport.createDialog( "Enter Change Description",
//			psp, true, bs, true, 1, 1,
//			new ActionListener() {
//				public void actionPerformed( ActionEvent ev ) {
//					dlg[0].setVisible(false);
//					dlg[0].dispose();
//				}
//			}
//		);
		dlg[0].pack();
		txt.requestFocus();
		txt.setSelectionStart( 0);
		txt.setSelectionEnd( txt.getDocument().getLength() );
		dlg[0].setVisible( true );
//		PerforceCommand.infoText("Closing option: ", desc.getValue()+"" );
		if( desc.getValue() == desc.CANCEL_OPTION ) {
			PerforceCommand.errorText("Submit","Cancelled by User");
			log.log(Level.SEVERE, "Submit cancelled by user from", new Throwable("Cancellation Context") );
			return null;
		}
		
		log.info("descr text: "+txt.getText() );
		return descr = txt.getText();
	}
	
	public class CheckControl extends JCheckBox {
		public String getFile() {
			return getText();
		}
		public CheckControl( String nm ) {
			super(nm);
		}
	}
}
