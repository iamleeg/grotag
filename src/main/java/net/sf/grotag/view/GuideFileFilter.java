package net.sf.grotag.view;

import java.io.File;
import java.io.IOException;

import javax.swing.filechooser.FileFilter;

import net.sf.grotag.common.AmigaTools;

/**
 * FileFilter accepting only Amigaguide documents.
 * 
 * @author Thomas Aglassinger
 */
public class GuideFileFilter extends FileFilter {

    private AmigaTools amigaTools;

    public GuideFileFilter() {
        super();
        amigaTools = AmigaTools.getInstance();
    }

    @Override
    public boolean accept(File file) {
        boolean result;

        if (file.isDirectory()) {
            result = true;
        } else {
            try {
                result = amigaTools.isAmigaguide(file);
            } catch (IOException error) {
                result = false;
            }
        }

        return result;
    }

    @Override
    public String getDescription() {
        return "Amigaguide document";
    }
}
