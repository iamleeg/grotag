package net.sf.grotag.guide;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.AmigaPathList;
import net.sf.grotag.common.TestTools;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DomWriterTest {
    private TestTools testTools;

    @Before
    public void setUp() throws Exception {
        testTools = TestTools.getInstance();
    }

    private void testWriteGuide(String baseName, AmigaPathList amigaPaths) throws IOException, ParserConfigurationException, TransformerException {
        File inFile = testTools.getTestInputFile(baseName + ".guide");
        File outFile = testTools.getTestActualFile(baseName + ".xml");
        GuidePile pile;

        if (!inFile.exists()) {
            inFile = testTools.getTestGuideFile(baseName + ".guide");
        }
        pile = GuidePile.createGuidePile(inFile, amigaPaths);

        DocBookDomFactory docBookDomFactory = new DocBookDomFactory(pile);
        Document dom = docBookDomFactory.createBook();
        DomWriter domWriter = new DomWriter(DomWriter.Dtd.DOCBOOK);
        domWriter.write(dom, outFile);
        assertTrue(outFile.exists());
    }

    @Test
    public void testWriteBasicsGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("basics", new AmigaPathList());
    }

    @Test
    public void testWriteRootGuide() throws IOException, ParserConfigurationException, TransformerException, SAXException {
        AmigaPathList amigaPaths = new AmigaPathList();
        amigaPaths.read(testTools.getTestInputFile("grotag_root.xml"));
        testWriteGuide("root", amigaPaths);
    }

    @Test
    public void testWriteAgrGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("agr", new AmigaPathList());
    }

    @Test
    public void testWriteAgrTestGuide() throws IOException, ParserConfigurationException, TransformerException {
        testWriteGuide("agr_test", new AmigaPathList());
    }
}
