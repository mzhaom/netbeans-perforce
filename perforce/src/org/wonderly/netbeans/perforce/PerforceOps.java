/*
 * PerforceOps.java
 *
 * Created on April 8, 2006, 11:00 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.wonderly.netbeans.perforce;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.openide.util.NbBundle;
import org.wonderly.awt.Packer;

/**
 *
 * @author gregg
 */
public class PerforceOps {
	private static final Logger log = Logger.getLogger( PerforceOps.class.getName() );
	/** Creates a new instance of PerforceOps */
	public PerforceOps() {
	}

	public static boolean removeChangeList( int cno ) throws IOException {
		String [] p4c = new String[]{ "p4","change","-d",""+cno };
		PerforceProcess p = new PerforceProcess( p4c );
		if( p.exitCode() == 0 ) {
			
			return true;
		}
		log.severe( "Change list delete for "+cno+" exited "+p.exitCode()+": "+p);
		return false;
	}
	public static P4ChangeList newChangeList() throws IOException {
		return newChangeList("<place description here>");
	}
	public static P4ChangeList newChangeList(String comment) throws IOException {
		int cno = -1;
		PerforceProcess p = new PerforceProcess( new String[]{ "p4","change","-o" } );
		if( p.exitCode() == 0 ) {
			String outstr = p.out.buf.toString();
			outstr = outstr.substring( 0, outstr.indexOf("<enter de") );
			PerforceProcess pi = new PerforceProcess( new String[]{ "p4","change","-i" }, 
					new StringReader( outstr+
						comment.replace("\n","\n\t")+"\n\nFiles:\n"), "Files:", true );
			String res = pi.out.buf.toString();
			PerforceCommand.infoText( res );
			if( res.contains("Change ") && res.contains(" created.") ) {
				cno = Integer.parseInt( res.substring( res.indexOf("Change ")+7, res.indexOf( " created.") ).trim() );
			}
		}
		if( cno == -1 ) {
			throw new IllegalStateException("Can't get new change list number");
		}
		return new P4ChangeList( getUser(), getClient(), cno, comment );
	}
	public static boolean syncChangeList(P4ChangeList cl) throws IOException {
		int cno = -1;
		StringWriter wr;

		// make sure comment in change list is correct
		wr = new StringWriter();
		cl.writeTo( wr, false );
		log.fine("reformulating change list comment with content:\n--------\n"+wr+"\n--------------");
		PerforceProcess pi = new PerforceProcess( new String[]{ "p4", "change", "-i"},
			new StringReader( wr.toString() ), null, false );
		String res = pi.out.buf.toString();
		int code = pi.exitCode();
		if( code != 0 ) {
			throw new IOException("Error syncing change list to fileset: "+res+"\nErrors: "+pi.err.buf );
		}
		log.fine("comment text returned messages: "+pi.out.buf+"\nError: "+pi.err.buf );
		PerforceCommand.infoText( res );

		// make sure all the right files are listed in the change list.
		wr = new StringWriter();
		for( String f : cl.files) {
			wr.append( f );
			wr.append("\n");
		}
		log.fine("reopening these files to change list #"+cl.change+"\n------------\n"+wr+"\n--------------");
		pi = new PerforceProcess( new String[]{ "p4","-x","-", "reopen","-c", cl.change+"" },
			new StringReader( wr.toString()), null, false );
		code = pi.exitCode();
		if( code != 0 ) {
			throw new IOException("Error syncing change list to fileset:\n"+res+"\nErrors: "+pi.err.buf );
		}
		res = pi.out.buf.toString();

		PerforceCommand.infoText( res );
		return true;
	}

	public static String getUser() throws IOException {
		int cno = -1;
		PerforceProcess p = new PerforceProcess( new String[]{ "p4","user","-o" } );
		if( p.code != 0 )
			throw new IOException("Can't read user with \"p4 user -o\"");
		return readField( "User:", p );
	}

	public static String getClient() throws IOException {
		int cno = -1;
		PerforceProcess p = new PerforceProcess( new String[]{ "p4","client","-o" } );
		if( p.code != 0 )
			throw new IOException("Can't read client with \"p4 client -o\"");
		return readField( "Client:", p );
	}
	
	private static String readField( String fld, PerforceProcess p ) throws IOException {
		BufferedReader rd = new BufferedReader( new StringReader( p.out.buf.toString() ) );
		String v;
		while( ( v = rd.readLine() ) != null ) {
			if( v.startsWith(fld) ) {
				return v.substring(fld.length()).trim();
			}
		}
		throw new EOFException("Can't find field \""+fld+"\" in data stream");
	}

	public static List<String> changeListDescription( int cno ) {
		final PerforceProcess p4c = new PerforceProcess( new String[]{ "p4", "change","-o", cno+"" } );
		List<String>ls = new ArrayList<String>();

		for( String s : p4c.out.buf.toString().split("\n") ) {
			ls.add( s );
		}
		return ls;
	}
	private static class PerforceProcess {
		String[] cmdline;
		Logger log = Logger.getLogger( getClass().getName() );
		OutputReader out, err;
		int code;

		public int exitCode() {
			return code;
		}

		private void showError( String[] p4c, int code ) {
			PerforceCommand.showError( p4c, code, out.buf.toString().split("\n"), err.buf.toString().split("\n") );
		}
		
		private void showDialog( JPanel p ) {
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

	    private void reportException(Throwable exx){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter( sw );
			exx.printStackTrace(pw);
			pw.close();
			JPanel p = new JPanel();
			Packer pk = new Packer(p);
			JTextArea area;
			pk.pack( new JScrollPane( area = new JTextArea( 8, 70 ) ) ).fillboth();
			area.setText( sw.toString() );
			area.setEditable(false);
			area.setWrapStyleWord( true );
			area.setLineWrap( true );
			showDialog( p );

			log.log(Level.SEVERE,exx.toString(),exx);
		}

		public PerforceProcess( String cmd[] ) {
			cmdline = cmd;
			try {
				Process p = Runtime.getRuntime().exec( cmdline,PerforceOptions.getPropertyValues() );
				out = new OutputReader( p.getInputStream() );
				err = new OutputReader( p.getErrorStream() );
				p.getOutputStream().close();
				code = p.waitFor();
				if( code != 0 ) {
					showError( cmd, code );
				}
			} catch ( Exception ex) {
				reportException( ex );
				code = -1;
			}
		}

		public PerforceProcess( String cmd[], Reader rd, String stopat, boolean after ) {
			cmdline = cmd;
			try {
				PerforceCommand.infoText( "Executing", Arrays.toString( cmd ) );
				Process p = Runtime.getRuntime().exec( cmdline,PerforceOptions.getPropertyValues() );
				out = new OutputReader( p.getInputStream() );
				err = new OutputReader( p.getErrorStream() );
				InputWriter wr = new InputWriter( rd, p.getOutputStream(), stopat, after );
				new Thread(wr).start();
				code = p.waitFor();
				if( code != 0 ) {
					showError( cmd, code );
				} else if( wr.code != 0 ) {
					showError( cmd, code );
				}
			} catch ( Exception ex) {
				reportException( ex );
				code = -1;
			}
		}
		
		private class InputWriter implements Runnable {
			Reader rd;
			OutputStream os;
			String stopat;
			boolean after;
			int code;
	
			public InputWriter( Reader rd, OutputStream os, String stopat, boolean after ) {
				this.rd = rd;
				this.os = os;
				this.stopat = stopat;
				this.after = after;
			}
			
			public void run() {
				try {
					BufferedReader br = new BufferedReader( rd );
					String str;
					while( ( str = br.readLine() ) != null ) {
						if( stopat != null && str.startsWith( stopat ) && !after )
							break;
						os.write( str.getBytes() );
						os.write("\r\n".getBytes());
						if( stopat != null && str.startsWith( stopat ) )
							break;
					}
					rd.close();
					code = 0;
				} catch( Exception ex ) {
					reportException(ex);
					code = -1;
				} finally {
					try {
						os.close();
					} catch( IOException ex ) {
						log.log( Level.FINE, ex.toString(), ex );
					}
				}
			}
		}

		private class OutputReader implements Runnable {
			StringBuffer buf;
			BufferedReader rd;
			public OutputReader( InputStream in ) {
				rd = new BufferedReader( new InputStreamReader( in ) );
				buf = new StringBuffer();
				new Thread( this ).start();
			}
			public void run() {
				try {
					String res;
					while( ( res = rd.readLine() ) != null ) {
						buf.append(res);
						buf.append("\n");
					}
				} catch( Exception ex ) {
					log.log( Level.WARNING, ex.toString(), ex );
				}
			}
		}
	}
}
