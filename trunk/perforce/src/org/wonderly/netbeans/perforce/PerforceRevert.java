package org.wonderly.netbeans.perforce;

import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import java.util.*;
import java.io.*;
import org.openide.filesystems.*;
import org.openide.loaders.*;
import java.util.logging.*;

public final class PerforceRevert extends PerforceCommand {
   public PerforceRevert() {
       super( "revert", "CTL_PerforceRevert", "revert.gif", true );
   }
}


