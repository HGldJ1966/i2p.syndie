package syndie.gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * encapsulate standard tree interaction functionality - override selected(), 
 * returnHit(), and/or resized() as necessary
 */
public class SyndieTreeListener implements KeyListener, TraverseListener, SelectionListener, ControlListener, MouseListener {
    private Tree _tree;

    public SyndieTreeListener(Tree tree) { _tree = tree; }
    
    /** the tree's selection was updated */
    public void selected() {}
    /** the user doubleclicked on the selected row */
    public void doubleclick() {}
    /** the user hit return on the selected row */
    public void returnHit() {}
    /** the tree was resized */
    public void resized() {}
    /** always expand on return, but we may not always want to collapse on return.  default is to do so though */
    public boolean collapseOnReturn() { return true; }
    
    // remaining are the implementations of the listeners
    
    public void widgetDefaultSelected(SelectionEvent selectionEvent) { selected(); }
    public void widgetSelected(SelectionEvent selectionEvent) { selected(); }
    
    public void keyTraversed(TraverseEvent evt) {
        if (evt.detail == SWT.TRAVERSE_RETURN) {
            TreeItem selected = getSelected();
            if (collapseOnReturn() && selected.getExpanded() && (selected.getItemCount() > 0))
                selected.setExpanded(!selected.getExpanded());
            returnHit();
        }
    }
    
    public void keyPressed(KeyEvent keyEvent) {}
    public void keyReleased(KeyEvent evt) {
        if (evt.keyCode == SWT.ARROW_LEFT) {
            TreeItem selected = getSelected();
            if (selected.getExpanded()) {
                selected.setExpanded(false);
            } else {
                TreeItem parent = selected.getParentItem();
                if (parent != null) {
                    parent.setExpanded(false);
                    _tree.setSelection(parent);
                }
            }
        } else if (evt.keyCode == SWT.ARROW_RIGHT) {
            TreeItem selected = getSelected();
            selected.setExpanded(true);
        } else if (evt.character == ' ') {
            TreeItem selected = getSelected();
            selected.setExpanded(!selected.getExpanded());
        }
    }

    public void controlMoved(ControlEvent controlEvent) {}
    public void controlResized(ControlEvent evt) { resized(); }

    public void mouseDoubleClick(MouseEvent evt) { doubleclick(); }
    public void mouseDown(MouseEvent evt) {}
    public void mouseUp(MouseEvent evt) {}
    
    protected TreeItem getSelected() { if (_tree.getSelectionCount() > 0) return _tree.getSelection()[0]; return null; }
}
