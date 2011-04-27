/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.wonderly.netbeans.perforce;

public final class PerforceReopenWithWrite extends PerforceCommand {
   public PerforceReopenWithWrite() {
       super( new String[]{ "reopen", "-t", "+w" },
			   "CTL_PerforceReopenWrite", null);
   }
}