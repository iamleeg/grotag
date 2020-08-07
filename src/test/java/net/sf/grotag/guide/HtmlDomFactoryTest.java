package net.sf.grotag.guide;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.TestTools;
import net.sf.grotag.common.Tools;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test case for HtmlDocFactory.
 * 
 * @author Thomas Aglassinger
 */
public class HtmlDomFactoryTest {
    private TestTools testTools;
    private Tools tools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
        tools = Tools.getInstance();
    }

    private void testCreateNodeDocument(String guideBaseName) throws Exception {
        File guideFile = testTools.getTestInputFile(guideBaseName + ".guide");
        String testName = testTools.getTestName(HtmlDomFactoryTest.class, "testCreateNodeDocument");
        File targetFolder = testTools.getTestActualFile(testName);
        AmigaPathList amigaPaths = new AmigaPathList();
        amigaPaths.read(testTools.getTestInputFile("grotag_root.xml"));

        GuidePile pile = GuidePile.createGuidePile(guideFile, amigaPaths);
        Guide guide = pile.getGuides().get(0);
        HtmlDomFactory factory = new HtmlDomFactory(pile, targetFolder);
        tools.mkdirs(factory.getStyleFile().getParentFile());
        factory.copyStyleFile();
        for (NodeInfo nodeInfo : guide.getNodeInfos()) {
            Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
            assertNotNull(htmlDocument);

            File targetFile = factory.getTargetFileFor(guide, nodeInfo);
            DomWriter htmlWriter = new DomWriter(DomWriter.Dtd.XHTML);
            htmlWriter.write(htmlDocument, targetFile);
        }
    }

    @Test
    public void testCreateBasicsDocument() throws Exception {
        testCreateNodeDocument("basics");
    }

    @Test
    public void testCreateRootDocument() throws Exception {
        testCreateNodeDocument("root");
    }
}
