package net.sf.grotag.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.grotag.Grotag;
import net.sf.grotag.guide.DomWriter;
import net.sf.grotag.guide.DomWriter.Dtd;

/**
 * JPanel to specify settings for "Export" dialog.
 * 
 * @author Thomas Aglassinger
 */
public class ExportAccessory extends JPanel {
    /**
     * Action to set the export format to a specific one.
     * 
     * @author Thomas Aglassinger
     */
    public class SetExportFormatAction extends AbstractAction {
        private Dtd formatToSet;

        public SetExportFormatAction(Dtd newFormatToSet) {
            assert newFormatToSet != null;
            formatToSet = newFormatToSet;
        }

        public void actionPerformed(ActionEvent event) {
            setFormat(formatToSet);
        }
    }

    private static final Dtd DEFAULT_EXPORT_FORMAT = DomWriter.Dtd.HTML;
    private static final String SETTINGS_EXPORT_FORMAT = "exportFormat";
    private Preferences settings;
    private Logger log;

    public ExportAccessory() {
        log = Logger.getLogger(ExportAccessory.class.getName());
        settings = Preferences.userNodeForPackage(Grotag.class);
        add(createFormatButtonPane());
    }

    private JPanel createFormatButtonPane() {
        JPanel result = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        ButtonGroup buttons = new ButtonGroup();
        JRadioButton htmlButton = createFormatRadioButton("HTML", Dtd.HTML);
        JRadioButton xhtmlButton = createFormatRadioButton("XHTML", Dtd.XHTML);
        JRadioButton docBookButton = createFormatRadioButton("DocBook", Dtd.DOCBOOK);

        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        result.add(htmlButton, constraints);
        buttons.add(htmlButton);
        result.add(xhtmlButton, constraints);
        buttons.add(xhtmlButton);
        result.add(docBookButton, constraints);
        buttons.add(docBookButton);

        result.setBorder(BorderFactory.createTitledBorder("Format"));
        return result;
    }

    public void setFormat(Dtd format) {
        settings.put(SETTINGS_EXPORT_FORMAT, format.toString());
    }

    private JRadioButton createFormatRadioButton(String label, Dtd format) {
        JRadioButton result = new JRadioButton(new SetExportFormatAction(format));
        result.setText(label);
        if (format == getFormat()) {
            result.setSelected(true);
        }
        return result;
    }

    public Dtd getFormat() {
        Dtd result;
        String resultText = settings.get(SETTINGS_EXPORT_FORMAT, DEFAULT_EXPORT_FORMAT.toString());
        try {
            result = Dtd.valueOf(resultText);
        } catch (IllegalArgumentException error) {
            log.warning("replaced broken value for setting " + SETTINGS_EXPORT_FORMAT + ":" + error.getMessage());
            result = DEFAULT_EXPORT_FORMAT;
        }
        return result;
    }
}
