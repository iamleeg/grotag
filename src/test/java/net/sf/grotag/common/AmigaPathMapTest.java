package net.sf.grotag.common;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import net.sf.grotag.common.AmigaPathList.AmigaPathFilePair;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class AmigaPathMapTest {
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    @Test
    public void testRead() throws SAXException, IOException, ParserConfigurationException {
        AmigaPathList amigaMap = new AmigaPathList();
        amigaMap.read(testTools.getTestInputFile("grotag_root.xml"));
        List<AmigaPathFilePair> items = amigaMap.items();
        assertNotNull(items);
        assertEquals(2, items.size());
        AmigaPathFilePair pair = items.get(0);
        assertEquals("stuff:", pair.getAmigaPath());
        assertEquals(new File("tests").getAbsolutePath(), pair.getLocalFolder().getAbsolutePath());

        pair = items.get(1);
        assertEquals("input:", pair.getAmigaPath());
        assertEquals(new File("tests", "input").getAbsolutePath(), pair.getLocalFolder().getAbsolutePath());
    }

}
