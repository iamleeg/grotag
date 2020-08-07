package net.sf.grotag.view;

import javax.swing.AbstractAction;

/**
 * AbstractAction that can tell if it is enabled for a certain GuiState.
 * 
 * @author Thomas Aglassinger
 */
abstract class AbstractGuiAction extends AbstractAction {
    public AbstractGuiAction(String name) {
        super(name);
    }

    public abstract boolean isEnabledFor(GuiState guiState);
}
