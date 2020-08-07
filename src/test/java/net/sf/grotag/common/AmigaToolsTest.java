package net.sf.grotag.common;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * TestCase for AmigaTools.
 * 
 * @author Thomas Aglassinger
 */
public class AmigaToolsTest {
    private AmigaTools amigaTools;
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        amigaTools = AmigaTools.getInstance();
    }

    private void testGetFilePathString(String amigaPath, File expected) {
        File actual = amigaTools.getFileFor(amigaPath, new AmigaPathList());
        assertEquals(expected.getAbsolutePath(), actual.getAbsolutePath());
    }

    @Test
    public void testGetFilePathString() {
        File basicsFile = testTools.getTestInputFile("basics.guide");
        testGetFilePathString("tests/input/basics.guide", basicsFile);
    }

    @Test
    public void testCanEscapeForAmigaGuide() {
        assertEquals("", amigaTools.escapedForAmigaguide(""));
        assertEquals("abc", amigaTools.escapedForAmigaguide("abc"));
        assertEquals("\\\\", amigaTools.escapedForAmigaguide("\\"));
        assertEquals("\\@", amigaTools.escapedForAmigaguide("@"));
        assertEquals("someone\\@example.com", amigaTools.escapedForAmigaguide("someone@example.com"));
    }
}
