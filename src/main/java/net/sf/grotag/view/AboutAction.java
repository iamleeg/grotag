package net.sf.grotag.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import net.sf.grotag.common.Tools;
import net.sf.grotag.common.Version;

/**
 * Action to show About window.
 * 
 * @author Thomas Aglassinger
 */
public class AboutAction extends AbstractGuiAction {
    private static final String ABOUT_MESSAGE = "Grotag " + Version.VERSION_TAG
            + "\n" + Version.COPYRIGHT;
    private String aboutMessage;
    private Logger log;
    private Tools tools;

    public AboutAction() {
        super("About");
        tools = Tools.getInstance();
        log = Logger.getLogger(AboutAction.class.getName());
    }

    public void actionPerformed(ActionEvent actionEvent) {
        try {
            log.info("handle \"about\" event");
            JOptionPane.showMessageDialog(
                    null, ABOUT_MESSAGE, "Grotag", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception error) {
            tools.showError("cannot show about dialog", error);
        }
    }

    @Override
    public boolean isEnabledFor(GuiState state) {
        return true;
    }
}
