package org.wonderly.netbeans.perforce;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Action which shows PerforceOutput component.
 */
public class PerforceOutputAction extends AbstractAction {

    public PerforceOutputAction() {
        super(NbBundle.getMessage(PerforceOutputAction.class, "CTL_PerforceOutputAction"));
//        putValue(SMALL_ICON, new ImageIcon(Utilities.loadImage(PerforceOutputTopComponent.ICON_PATH, true)));
    }

    public void actionPerformed(ActionEvent evt) {
        TopComponent win = PerforceOutputTopComponent.findInstance();
        win.open();
        win.requestActive();
    }

}
