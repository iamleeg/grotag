package net.sf.grotag;

import static org.junit.Assert.assertNotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

/**
 * Test case for IFFReader (just to see how it works).
 * 
 * @author Thomas Aglassinger
 */
public class IlbmImageTest {
    private static final int MILLISECONDS_TO_SHOW_IMAGE = 3000;
    private static final boolean SHOW_AMIGABALL = false;

    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    @Test
    public void testReadIlbmImage() throws IOException {
        File ilbmFile = testTools.getTestInputFile("amigaball.ilbm");
        BufferedImage amigaBall = ImageIO.read(ilbmFile);
        assertNotNull(amigaBall);

        if (SHOW_AMIGABALL) {
            JPanel pane = new JPanel();
            pane.add(new JButton(new ImageIcon(amigaBall)));
            JFrame frame = new JFrame(IlbmImageTest.class.getName());
            frame.setContentPane(pane);
            frame.pack();
            frame.setVisible(true);
            try {
                Thread.sleep(MILLISECONDS_TO_SHOW_IMAGE);
            } catch (InterruptedException error) {
                error.printStackTrace();
            }
        }
    }
}
