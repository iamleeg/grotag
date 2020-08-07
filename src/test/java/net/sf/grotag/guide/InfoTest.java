package net.sf.grotag.guide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for DatabaseInfo and NodeInfo.
 * 
 * @author Thomas Aglassinger
 */
public class InfoTest {
    private TestTools testTools;
    private Logger log;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        log = Logger.getLogger(InfoTest.class.getName());
    }

    private Guide getBasicsGuide() throws IOException {
        testTools = TestTools.getInstance();
        File testFile = testTools.getTestInputFile("basics.guide");
        Guide result = Guide.createGuide(testFile, new AmigaPathList());
        assertNotNull(result);
        return result;
    }

    @Test
    public void testDatabaseInfo() throws IOException {
        Guide guide = getBasicsGuide();
        DatabaseInfo info = guide.getDatabaseInfo();
        assertNotNull(info);
        assertEquals("basics", info.getName());
        assertEquals("Thomas Aglassinger", info.getAuthor());
        assertEquals("2008 Thomas Aglassinger", info.getCopyright());
        assertEquals("Helvetica.font", info.getFontName());
        assertEquals(13, info.getFontSize());
        assertEquals(Wrap.NONE, info.getWrap());
    }

    @Test
    public void testNodeInfo() throws IOException {
        Guide guide = getBasicsGuide();
        NodeInfo info;

        info = guide.getNodeInfo("smartwrapped");
        assertNotNull(info);
        assertEquals(Wrap.SMART, info.getWrap());
        assertEquals("Helvetica.font", info.getFontName());
        assertEquals(13, info.getFontSize());
    }

    @Test
    public void testToString() throws IOException {
        Guide guide = getBasicsGuide();
        DatabaseInfo databaseInfo = guide.getDatabaseInfo();
        NodeInfo nodeInfo = guide.getNodeInfo("main");

        log.fine("database=" + databaseInfo.toString());
        log.fine("node=" + nodeInfo.toString());
    }

    @Test
    public void testParseCopyrightYearAndHolder() {
        DatabaseInfo dbInfo = new DatabaseInfo("hugo.guide");
        dbInfo.setCopyright("2008 hugo");
        assertEquals("2008", dbInfo.getCopyrightYear());
        assertEquals("hugo", dbInfo.getCopyrightHolder());

        dbInfo.setCopyright("2001-2008 hugo");
        assertEquals("2001-2008", dbInfo.getCopyrightYear());
        assertEquals("hugo", dbInfo.getCopyrightHolder());

        dbInfo.setCopyright("2001 -\t2008   \t  hugo");
        assertEquals("2001-2008", dbInfo.getCopyrightYear());
        assertEquals("hugo", dbInfo.getCopyrightHolder());

        dbInfo.setCopyright("2008");
        assertNull(dbInfo.getCopyrightYear());
        assertNull(dbInfo.getCopyrightHolder());
    }
}
