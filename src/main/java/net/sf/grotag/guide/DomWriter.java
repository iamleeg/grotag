package net.sf.grotag.guide;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.grotag.common.Tools;

import org.w3c.dom.Document;

/**
 * Writer for a DOM. This is simply hides a lot of complexity of some Java
 * standard classes.
 * 
 * @author Thomas Aglassinger
 */
public class DomWriter {
    /**
     * Enum to identify the supported DTDs.
     * 
     * @author Thomas Aglassinger
     */
    public enum Dtd {
        DOCBOOK, HTML, XHTML
    }

    public static final String DEFAULT_ENCODING = "UTF-8";

    private Logger log;
    private Tools tools;
    private String encoding;
    private Transformer transformer;

    public DomWriter(Dtd dtd) throws TransformerConfigurationException {
        assert dtd != null;

        log = Logger.getLogger(DomWriter.class.getName());
        tools = Tools.getInstance();

        encoding = DEFAULT_ENCODING;

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        if (dtd == Dtd.DOCBOOK) {
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//OASIS//DTD DocBook XML V4.5//EN");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
                    "http://www.oasis-open.org/docbook/xml/4.5/docbookx.dtd");
        } else if (dtd == Dtd.HTML) {
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.01//EN");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/html4/strict.dtd");
        } else if (dtd == Dtd.XHTML) {
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD XHTML 1.0 Strict//EN");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
        } else {
            assert false : "dtd=" + dtd;
        }
    }

    public void setEncoding(String newEncoding) {
        assert newEncoding != null;
        encoding = newEncoding;
    }

    public void write(Document dom, File targetFile) throws IOException, TransformerException {
        assert dom != null;
        assert targetFile != null;

        log.log(Level.INFO, "write dom to {0} using encoding {1}", new Object[] { tools.sourced(targetFile),
                tools.sourced(encoding) });
        tools.mkdirs(targetFile.getParentFile());
        Writer targetWriter = tools.createBufferedWriter(targetFile, encoding);
        try {
            transformer.setOutputProperty(OutputKeys.ENCODING, encoding);
            transformer.transform(new DOMSource(dom), new StreamResult(targetWriter));
        } finally {
            targetWriter.close();
        }
    }
}
