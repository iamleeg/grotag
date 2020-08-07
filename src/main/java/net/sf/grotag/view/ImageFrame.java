package net.sf.grotag.view;

import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

/**
 * JFrame that shows the image stored in <code>imageFile</code>. If the image
 * is larger than the frame, offer scroll bars.
 * 
 * @author Thomas Aglassinger
 */
public class ImageFrame extends JFrame {
    public ImageFrame(BufferedImage image) {
        super();
        ImageIcon icon = new ImageIcon(image);
        JLabel imageLabel = new JLabel(icon);
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane);
    }
}
