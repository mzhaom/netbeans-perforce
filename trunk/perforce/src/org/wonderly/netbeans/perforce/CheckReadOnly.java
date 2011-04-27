package org.wonderly.netbeans.perforce;

import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

/**
 *  Not used yet...
 */
public final class CheckReadOnly extends CookieAction {
    volatile boolean readonly;
    protected void performAction(Node[] activatedNodes) {
        EditorCookie c = (EditorCookie) activatedNodes[0].getCookie(EditorCookie.class);
        FileObject fc = ((DataObject)activatedNodes[0].getCookie(DataObject.class)).getPrimaryFile();
        if( fc.canWrite() == false )
            readonly = true;
        System.out.println("readonly: "+readonly);
    }

    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    public String getName() {
        return NbBundle.getMessage(CheckReadOnly.class, "CTL_CheckReadOnly");
    }

    protected Class[] cookieClasses() {
        return new Class[] {
            DataObject.class,
            EditorCookie.class
        };
    }
    
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    
    protected boolean asynchronous() {
        return false;
    }

}

