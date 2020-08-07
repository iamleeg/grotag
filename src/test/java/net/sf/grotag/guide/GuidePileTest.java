package net.sf.grotag.guide;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.TestTools;
import net.sf.grotag.common.Tools;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class GuidePileTest {
    private TestTools testTools;
    private Tools tools;
    private Logger log;

    @Before
    public void setUp() throws Exception {
        tools = Tools.getInstance();
        testTools = TestTools.getInstance();
        log = Logger.getLogger(GuidePileTest.class.getName());
    }

    @Test
    public void testAdd() throws IOException, SAXException, ParserConfigurationException {
        File rootGuideFile = testTools.getTestInputFile("root.guide");
        AmigaPathList amigaPaths = new AmigaPathList();
        amigaPaths.read(testTools.getTestInputFile("grotag_root.xml"));
        GuidePile pile = GuidePile.createGuidePile(rootGuideFile, amigaPaths);
        assertEquals(4, pile.getGuides().size());
    }

    @Test
    public void testRkrm() throws IOException, SAXException, ParserConfigurationException {
        File rkrmDevicesFolder = testTools.getTestGuideFile("reference_library");
        File rkrmDevicesGuideFile = new File(new File(rkrmDevicesFolder, "devices"), "Dev_1");
        AmigaPathList amigaPaths = new AmigaPathList();
        amigaPaths.read(testTools.getTestInputFile("grotag_rkrm.xml"));

        try {
            GuidePile pile = GuidePile.createGuidePile(rkrmDevicesGuideFile, amigaPaths);
            assertEquals(29, pile.getGuides().size());
        } catch (FileNotFoundException errorToIgnore) {
            log.warning("skipped test for " + tools.sourced(rkrmDevicesGuideFile));
        }
    }
}
