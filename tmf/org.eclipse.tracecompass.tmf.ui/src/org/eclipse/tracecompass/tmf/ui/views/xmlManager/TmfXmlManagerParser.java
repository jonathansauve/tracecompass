package org.eclipse.tracecompass.tmf.ui.views.xmlManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.TmfXmlUiStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Jonathan Sauvé
 * @since 1.0
 *
 */
@SuppressWarnings("restriction")
public class TmfXmlManagerParser {

    private static List<Node> froot = new ArrayList<>();
    private static NodeList fstateProviderNodes;
    private static NodeList fXYViewNodes;
    private static NodeList ftimeGraphViewNodes;
    private static NodeList ffiltersNodes;

    /**
     * The XMLParser constructor
     *
     * @param uri The XML file to parse
     * @throws ParserConfigurationException
     * @throws SAXException  SAX exception
     * @throws IOException  IO exception
     */
    @SuppressWarnings("javadoc")
    public TmfXmlManagerParser(final String uri) throws ParserConfigurationException, SAXException, IOException {
        IPath path = new Path(uri);
        File xmlFile = path.toFile();
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFact.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        doc.normalize();

        froot = new ArrayList<>();
        fstateProviderNodes = doc.getDocumentElement().getElementsByTagName(TmfXmlStrings.STATE_PROVIDER);
        fXYViewNodes = doc.getDocumentElement().getElementsByTagName(TmfXmlUiStrings.XY_VIEW);
        ftimeGraphViewNodes = doc.getDocumentElement().getElementsByTagName(TmfXmlUiStrings.TIME_GRAPH_VIEW);
        ffiltersNodes = doc.getDocumentElement().getElementsByTagName("filter"); //$NON-NLS-1$

        // Join the nodeLists
        for(int i = 0; fstateProviderNodes != null && i < fstateProviderNodes.getLength(); i++)
        {
            froot.add(fstateProviderNodes.item(i));
        }
        for(int i = 0; fXYViewNodes != null && i < fXYViewNodes.getLength(); i++)
        {
            froot.add(fXYViewNodes.item(i));
        }
        for(int i = 0; ftimeGraphViewNodes != null && i < ftimeGraphViewNodes.getLength(); i++)
        {
            froot.add(ftimeGraphViewNodes.item(i));
        }
        for(int i = 0; ffiltersNodes != null && i < ffiltersNodes.getLength(); i++)
        {
            froot.add(ffiltersNodes.item(i));
        }
    }

    @SuppressWarnings("javadoc")
    public List<Node> getRoots()
    {
        if(froot == null)
        {
            return null;
        }
        return froot;
    }
}
