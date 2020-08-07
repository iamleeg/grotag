package net.sf.grotag.guide;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

/**
 * TestCase for <code>Guide</code>.
 * 
 * @author Thomas Aglassinger
 */
public class GuideTest {
    private TestTools testTools;
    
    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    private void testGuide(String fileName) throws IOException {
        File inFile = testTools.getTestInputFile(fileName);
        File outFile = testTools.getTestActualFile(fileName);
        Guide guide = Guide.createGuide(inFile, new AmigaPathList());

        assertNotNull(guide);
        guide.writePretty(outFile);
        testTools.assertFilesAreEqual(fileName);
    }

    @Test
    public void testNodeGuide() throws Exception {
        testGuide("nodes.guide");
    }

    @Test
    public void testUniqueGuide() throws Exception {
        testGuide("unique.guide");
    }

    @Test
    public void testMacroGuide() throws Exception {
        Guide guide = Guide.createGuide(testTools.getTestInputFile("macros.guide"), new AmigaPathList());
        assertNotNull(guide);
    }
}
