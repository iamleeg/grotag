package net.sf.grotag.guide;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import net.sf.grotag.common.Tools;

import org.w3c.dom.Document;

/**
 * Tools to export Amigaguide documents to another format.
 * 
 * @author Thomas Aglassinger
 */
public class ExportTools {
    private static ExportTools instance;

    public static synchronized final ExportTools getInstance() {
        if (instance == null) {
            instance = new ExportTools();
        }
        return instance;
    }

    private ExportTools() {
        super();
    }

    public File targetFileFor(File sourceFile, File targetFolder, String targetSuffix) {
        assert sourceFile != null;
        assert targetFolder != null;
        assert targetSuffix != null;
        String sourceName = sourceFile.getName();
        String targetName = Tools.getInstance().getWithoutLastSuffix(sourceName) + "." + targetSuffix;
        File result = new File(targetFolder, targetName);
        return result;
    }

    public void exportAsDocBookXml(GuidePile pile, File outputFile) throws ParserConfigurationException, IOException,
            TransformerConfigurationException, TransformerException {
        DocBookDomFactory domFactory = new DocBookDomFactory(pile);
        Document dom = domFactory.createBook();
        DomWriter domWriter = new DomWriter(DomWriter.Dtd.DOCBOOK);

        domWriter.write(dom, outputFile);
    }

    public void exportAsHtml(GuidePile pile, File outputFolder, DomWriter.Dtd dtd) throws ParserConfigurationException,
            IOException, TransformerConfigurationException, TransformerException {
        for (Guide guide : pile.getGuides()) {
            HtmlDomFactory factory = new HtmlDomFactory(pile, outputFolder);
            factory.copyStyleFile();
            for (NodeInfo nodeInfo : guide.getNodeInfos()) {
                Document htmlDocument = factory.createNodeDocument(guide, nodeInfo);
                File targetFile = factory.getTargetFileFor(guide, nodeInfo);
                DomWriter htmlWriter = new DomWriter(dtd);
                htmlWriter.write(htmlDocument, targetFile);
            }
        }
    }
}
