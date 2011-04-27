package org.wonderly.netbeans.perforce;

/**
 * Action for performing the "p4 add XXX" command line operations.
 * 
 * @author gregg
 */
public final class PerforceAddSource extends PerforceCommand {
   public PerforceAddSource() {
       super( "add", "CTL_PerforceAddSource", "add.gif"); 
   }
}
