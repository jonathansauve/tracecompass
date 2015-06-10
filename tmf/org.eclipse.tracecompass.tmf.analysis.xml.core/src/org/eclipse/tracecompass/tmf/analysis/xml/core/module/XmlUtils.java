/*******************************************************************************
 * Copyright (c) 2014 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.tmf.analysis.xml.core.module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.core.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.stateprovider.TmfXmlStrings;
import org.eclipse.tracecompass.tmf.analysis.xml.ui.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.tmf.core.util.Pair;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class containing some utilities for the XML plug-in packages: for example, it
 * manages the XML files and validates them
 *
 * @author Geneviève Bastien
 */
public class XmlUtils {

    /** Sub-directory of the plug-in where XML files are stored */
    private static final String XML_DIRECTORY = "xml_files"; //$NON-NLS-1$

    /** Name of the XSD schema file */
    private static final String XSD = "xmlDefinition.xsd"; //$NON-NLS-1$

    /** Map to keep the original file of xml analysis */
    private static final Map<File, File> xmlFiles = new HashMap<>();

    /** Make this class non-instantiable */
    private XmlUtils() {

    }

    /**
     * Get the path where the XML files are stored. Create it if it does not
     * exist
     *
     * @return path to XML files
     */
    public static IPath getXmlFilesPath() {
        IPath path = Activator.getDefault().getStateLocation();
        path = path.addTrailingSeparator().append(XML_DIRECTORY);

        /* Check if directory exists, otherwise create it */
        File dir = path.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        dir.deleteOnExit();
        return path;
    }

    /**
     * Validate the XML file input with the XSD schema
     *
     * @param xmlFile
     *            XML file to validate
     * @return True if the XML validates
     */
    public static IStatus xmlValidate(File xmlFile) {
        URL url = XmlUtils.class.getResource(XSD);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source xmlSource = new StreamSource(xmlFile);
        try {
            Schema schema = schemaFactory.newSchema(url);
            Validator validator = schema.newValidator();
            validator.validate(xmlSource);
        } catch (SAXParseException e) {
            String error = NLS.bind(Messages.XmlUtils_XmlParseError, e.getLineNumber(), e.getLocalizedMessage());
            Activator.logError(error);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        } catch (SAXException e) {
            String error = NLS.bind(Messages.XmlUtils_XmlValidationError, e.getLocalizedMessage());
            Activator.logError(error);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        } catch (IOException e) {
            String error = Messages.XmlUtils_XmlValidateError;
            Activator.logError("IO exception occurred", e); //$NON-NLS-1$
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }
        return Status.OK_STATUS;
    }

    /**
     * Adds an XML file to the plugin's path. The XML file should have been
     * validated using the {@link XmlUtils#xmlValidate(File)} method before
     * calling this method.
     *
     * @param fromFile
     *            The XML file to add
     * @return Whether the file was successfully added
     */
    public static IStatus addXmlFile(File fromFile) {

        /* Copy file to path */
        File toFile = getXmlFilesPath().addTrailingSeparator().append(fromFile.getName()).toFile();

        try {
            if (!toFile.exists()) {
                toFile.createNewFile();
            }
        } catch (IOException e) {
            String error = Messages.XmlUtils_ErrorCopyingFile;
            Activator.logError(error, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }

        try (FileInputStream fis = new FileInputStream(fromFile);
                FileOutputStream fos = new FileOutputStream(toFile);
                FileChannel source = fis.getChannel();
                FileChannel destination = fos.getChannel();) {
            destination.transferFrom(source, 0, source.size());
        } catch (IOException e) {
            String error = Messages.XmlUtils_ErrorCopyingFile;
            Activator.logError(error, e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, error, e);
        }

        xmlFiles.put(toFile, fromFile);
        return Status.OK_STATUS;
    }


    /**
     * Deletes an XML file from the plugin's path
     *
     * @param toDelete The XML file to delete
     * @return Whether the file was successfully deleted
     * @since 1.0
     */
    public static IStatus removeXmlFile(File toDelete)
    {
        boolean valid = xmlFileIsActive(toDelete);

        if(valid) {
            xmlFiles.remove(toDelete);
            toDelete.delete();
            XmlAnalysisModuleSource.notifyModuleChange();
            return Status.OK_STATUS;
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The XML file is not an active XML Analysis"); //$NON-NLS-1$
    }

    /**
     * Get only the XML element children of an XML element.
     *
     * @param parent
     *            The parent element to get children from
     * @return The list of children Element of the parent
     */
    public static List<Element> getChildElements(Element parent) {
        NodeList childNodes = parent.getChildNodes();
        List<Element> childElements = new ArrayList<>();
        for (int index = 0; index < childNodes.getLength(); index++) {
            if (childNodes.item(index).getNodeType() == Node.ELEMENT_NODE) {
                childElements.add((Element) childNodes.item(index));
            }
        }
        return childElements;
    }

    /**
     * Get the XML children element of an XML element, but only those of a
     * certain type
     *
     * @param parent
     *            The parent element to get the children from
     * @param elementTag
     *            The tag of the elements to return
     * @return The list of children {@link Element} of the parent
     */
    public static List<Element> getChildElements(Element parent, String elementTag) {
        /* get the state providers and find the corresponding one */
        NodeList nodes = parent.getElementsByTagName(elementTag);
        List<Element> childElements = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element node = (Element) nodes.item(i);
            if (node.getParentNode().equals(parent)) {
                childElements.add(node);
            }
        }
        return childElements;
    }

    /**
     * Return the node element corresponding to the requested type in the file.
     *
     * TODO: Nothing prevents from having duplicate type -> id in a same file.
     * That should not be allowed. If you want an element with the same ID as
     * another one, it should be in a different file and we should check it at
     * validation time.
     *
     * @param filePath
     *            The absolute path to the XML file
     * @param elementType
     *            The type of top level element to search for
     * @param elementId
     *            The ID of the desired element
     * @return The XML element or <code>null</code> if not found
     */
    public static Element getElementInFile(String filePath, @NonNull String elementType, @NonNull String elementId) {

        if (filePath == null) {
            return null;
        }

        IPath path = new Path(filePath);
        File file = path.toFile();
        if (file == null || !file.exists() || !file.isFile() || !xmlValidate(file).isOK()) {
            return null;
        }

        try {
            /* Load the XML File */
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;

            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            /* get the state providers and find the corresponding one */
            NodeList nodes = doc.getElementsByTagName(elementType);
            Element foundNode = null;

            for (int i = 0; i < nodes.getLength(); i++) {
                Element node = (Element) nodes.item(i);
                String id = node.getAttribute(TmfXmlStrings.ID);
                if (id.equals(elementId)) {
                    foundNode = node;
                }
            }
            return foundNode;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }

    /**
     * This function returns the analysis ID of a file. This ID is the same
     * for each Analysis node and it's also the ID the stateProvider.
     * @param filePath
     *              The path of the file to search in
     * @return
     *              The analysis ID, or empty string if not found
     * @since 1.0
     */
    public static String getAnalysisId(String filePath) {
        if (filePath == null) {
            return null;
        }

        IPath path = new Path(filePath);
        File file = path.toFile();
        if (file == null || !file.exists() || !file.isFile() || !xmlValidate(file).isOK()) {
            return null;
        }

        try {
            /* Load the XML File */
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;

            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            /* Get the state provider, if present */
            NodeList stateProviders = doc.getElementsByTagName(TmfXmlStrings.STATE_PROVIDER);
            if(stateProviders.getLength() != 0) {
                /* Take the first one and return his id */
                return stateProviders.item(0).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();
            }

            NodeList nodes = doc.getElementsByTagName(TmfXmlStrings.ANALYSIS);
            if(nodes.getLength() != 0) {
                /* Take the first one and return his id */
                return nodes.item(0).getAttributes().getNamedItem(TmfXmlStrings.ID).getNodeValue();
            }

            return ""; //$NON-NLS-1$
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }

    /**
     * This function allows to save a new value for an attribute to both
     * original XML file and the one in xml_files runtime folder.
     *
     * @param copyFile
     *              The file in xml_files folder
     * @param originalFile
     *              The original file
     * @param node
     *              The node to set the new value
     * @param attribute
     *              The attribute to change
     * @param value
     *              The new value
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     * @return Whether the attribute was successfully setted
     * @since 1.0
     */
    @SuppressWarnings("javadoc")
    public static IStatus setNewAttribute(File copyFile, File originalFile, Node node, String attribute, String value) throws ParserConfigurationException, SAXException, IOException, TransformerException {

        if(xmlFileIsActive(copyFile) == false) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "One or both of these XML file are not valid"); //$NON-NLS-1$
        }

        // Parse the files
        DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFact.newDocumentBuilder();
        Document doc = dBuilder.parse(copyFile);
        Document originalDoc = dBuilder.parse(originalFile);

        // Find the node to be modified
        boolean docChanged = false;
        NodeList nodes = doc.getElementsByTagName(node.getNodeName());
        for(int i = 0; i < nodes.getLength(); i++) {
            if(nodes.item(i).isEqualNode(node)) {
                nodes.item(i).getAttributes().getNamedItem(attribute).setNodeValue(value);
                docChanged = true;
                break;
            }
        }

        boolean originalDocChanged = false;
        NodeList originalNodes = originalDoc.getElementsByTagName(node.getNodeName());
        for(int i = 0; i < originalNodes.getLength(); i++) {
            if(originalNodes.item(i).isEqualNode(node)) {
                originalNodes.item(i).getAttributes().getNamedItem(attribute).setNodeValue(value);
                originalDocChanged = true;
                break;
            }
        }

        // update the files
        if(docChanged) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(copyFile.getAbsolutePath()));
            transformer.transform(source, result);
        }

        if(originalDocChanged) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(originalDoc);
            StreamResult result = new StreamResult(new File(originalFile.getAbsolutePath()));
            transformer.transform(source, result);
        }

        return Status.OK_STATUS;
    }

    /**
     * @param nodeName
     *              The node name : one in <code>TmfXmlStrings</code> class or
     *              <code>TmfXmlUiStrings</code> class
     * @param parent
     *              The parent of the node or <code>null</code>. This parent must be present in
     *              <code>xmlFile</code> (if not <code>null</code>)
     * @param xmlFile
     *              The input file
     * @return
     *              A pair that contains both status of the creation and the new node (<code>parent</code>
     *              if the returned status isn't <code>Status.OK_STATUS</code>)
     * @since 1.0
     */
    @SuppressWarnings("null")
    public static Pair<IStatus, Node> createNewNode(String nodeName, @NonNull Node parent, File xmlFile) {
        boolean valid = xmlFileIsActive(xmlFile);

        if(valid) {
            /**
             * 1- Get the original file
             * 2- Parse the files
             * 3- Find the parent
             * 4- Create the new node
             * 5- Save the files
             */

            File originalFile = xmlFiles.get(xmlFile);
            if(originalFile == null) {
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid XML file"), parent); //$NON-NLS-1$;
            }

            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            Document doc;
            Document originalDoc;
            try {
                dBuilder = dbFact.newDocumentBuilder();
                doc = dBuilder.parse(xmlFile);
                originalDoc = dBuilder.parse(originalFile);
            } catch (SAXException e) {
                e.printStackTrace();
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "SAX error"), parent); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IO error"), parent); //$NON-NLS-1$
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Parsing error"), parent); //$NON-NLS-1$
            } catch (Throwable e) {
                e.printStackTrace();
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unknown error"), parent); //$NON-NLS-1$
            }

            boolean docChanged = false;
            Element newNode = null;
            NodeList nodes = doc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < nodes.getLength(); i++) {
                if(nodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    Node parentNode = nodes.item(i);
                    newNode = doc.createElement(nodeName);
                    newNode.setAttribute(TmfXmlStrings.TYPE, TmfXmlStrings.TYPE_CONSTANT);
                    parentNode.appendChild(newNode); // Not sure if its work
                    docChanged = true;
                }
            }

            boolean originalDocChanged = false;
            NodeList originalNodes = originalDoc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < originalNodes.getLength(); i++) {
                if(originalNodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    Node parentNode = originalNodes.item(i);
                    Element newOriginalNode = originalDoc.createElement(nodeName);
                    newOriginalNode.setAttribute(TmfXmlStrings.TYPE, TmfXmlStrings.TYPE_CONSTANT);
                    parentNode.appendChild(newOriginalNode);
                    originalDocChanged = true;
                }
            }

            try {
                if(docChanged) {
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    DOMSource source = new DOMSource(doc);
                    StreamResult result = new StreamResult(new File(xmlFile.getAbsolutePath()));
                    transformer.transform(source, result);
                }

                if(originalDocChanged) {
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    DOMSource source = new DOMSource(originalDoc);
                    StreamResult result = new StreamResult(new File(originalFile.getAbsolutePath()));
                    transformer.transform(source, result);
                }
            } catch(Throwable e) {
                e.printStackTrace();
                return new Pair<IStatus, Node>(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error when writing in files"), parent); //$NON-NLS-1$
            }

            if(newNode != null) {
                final IStatus ok_STATUS2 = Status.OK_STATUS;
                if (ok_STATUS2 != null) {
                    return new Pair<IStatus, Node>(ok_STATUS2, newNode);
                }
            }
        }
        return new Pair<>(Status.CANCEL_STATUS, parent);
    }

    /**
     * @param newNode
     *              The node to be appended
     * @param parent
     *              The parent of the new node
     * @param xmlFile
     *              The input file
     * @param applyChanges
     *              Whether we apply the changes to the file or not
     * @return
     *              Whether the element was successfully appended
     * @since 1.0
     */
    public static IStatus appendElementInFile(Node newNode, Node parent, File xmlFile, boolean applyChanges) {
        boolean valid = xmlFileIsActive(xmlFile);

        if(valid) {
            /**
             * 1- Get the original file
             * 2- Parse the files
             * 3- Find the parent
             * 4- Check if the parent already have a child same as <code>newNode</code>
             * 4- Append the new node
             * 5- Save the files
             */
            File originalFile = xmlFiles.get(xmlFile);
            if(originalFile == null) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid XML file"); //$NON-NLS-1$
            }

            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            Document doc;
            Document originalDoc;
            try {
                dBuilder = dbFact.newDocumentBuilder();
                doc = dBuilder.parse(xmlFile);
                originalDoc = dBuilder.parse(originalFile);
            } catch (SAXException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "SAX error"); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IO error"); //$NON-NLS-1$
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Parsing error"); //$NON-NLS-1$
            } catch (Throwable e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unknown error"); //$NON-NLS-1$
            }

            boolean docChanged = false;
            boolean isPresentInDoc = false;
            NodeList nodes = doc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < nodes.getLength(); i++) {
                if(nodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    Node parentNode = nodes.item(i);
                    NodeList parentNodeChildren = parentNode.getChildNodes();
                    for(int j = 0; j < parentNodeChildren.getLength(); j++) {
                        Node parentNodeChild = parentNodeChildren.item(j);
                        if(parentNodeChild.isEqualNode(newNode)) {
                            isPresentInDoc = true;
                            break;
                        }
                    }
                    if(!isPresentInDoc) {
                        Node newDocNode = doc.importNode(newNode, true);
                        nodes.item(i).appendChild(newDocNode);
                        docChanged = true;
                        break;
                    }
                }
            }

            boolean originalDocChanged = false;
            boolean isPresentInOriginalDoc = false;
            NodeList originalNodes = originalDoc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < originalNodes.getLength(); i++) {
                if(originalNodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    Node parentNode = originalNodes.item(i);
                    NodeList parentNodeChildren = parentNode.getChildNodes();
                    for(int j = 0; j < parentNodeChildren.getLength(); j++) {
                        Node parentNodeChild = parentNodeChildren.item(j);
                        if(parentNodeChild.isEqualNode(newNode)) {
                            isPresentInOriginalDoc = true;
                            break;
                        }
                    }
                    if(!isPresentInOriginalDoc) {
                        Node newOriginalDocNode = originalDoc.importNode(newNode, true);
                        originalNodes.item(i).appendChild(newOriginalDocNode);
                        originalDocChanged = true;
                        break;
                    }
                }
            }

            if(isPresentInDoc && isPresentInOriginalDoc) {
                ErrorDialog.openError(Display.getDefault().getActiveShell(), "Append error",  //$NON-NLS-1$
                        "An error occured when appending the newNode", new Status(IStatus.ERROR, Activator.PLUGIN_ID,  //$NON-NLS-1$
                                "The newNode is already present under the parent")); //$NON-NLS-1$
                return Status.CANCEL_STATUS;
            }
            if(applyChanges) {
                try {
                    if(docChanged) {
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        DOMSource source = new DOMSource(doc);
                        StreamResult result = new StreamResult(new File(xmlFile.getAbsolutePath()));
                        transformer.transform(source, result);
                    }

                    if(originalDocChanged) {
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        DOMSource source = new DOMSource(originalDoc);
                        StreamResult result = new StreamResult(new File(originalFile.getAbsolutePath()));
                        transformer.transform(source, result);
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error when writing in files"); //$NON-NLS-1$
                }
            }
            return Status.OK_STATUS;
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The file is not valid"); //$NON-NLS-1$
    }

    /**
     * @param oldNode
     *              The node to be removed
     * @param parent
     *              The parent of the oldNode
     * @param xmlFile
     *              The file that contains the node
     * @param applyChanges
     *              Whether we apply the changes to the file or not
     * @return
     *              Whether the element was successfully removed
     * @since 1.0
     */
    public static IStatus removeElementFromFile(Node oldNode, Node parent, File xmlFile, boolean applyChanges) {
        boolean valid = xmlFileIsActive(xmlFile);

        if(valid) {
            /**
             * 1- Get the original file
             * 2- Parse the files
             * 3- Find the parent
             * 4- Delete the node
             * 5- Save the files
             */
            File originalFile = xmlFiles.get(xmlFile);
            if(originalFile == null) {
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Invalid XML file"); //$NON-NLS-1$
            }

            DocumentBuilderFactory dbFact = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            Document doc;
            Document originalDoc;
            try {
                dBuilder = dbFact.newDocumentBuilder();
                doc = dBuilder.parse(xmlFile);
                originalDoc = dBuilder.parse(originalFile);
            } catch (SAXException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "SAX error"); //$NON-NLS-1$
            } catch (IOException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "IO error"); //$NON-NLS-1$
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Parsing error"); //$NON-NLS-1$
            } catch (Throwable e) {
                e.printStackTrace();
                return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unknown error"); //$NON-NLS-1$
            }

            boolean docChanged = false;
            NodeList nodes = doc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < nodes.getLength(); i++) {
                if(nodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    //remove the node
                    Node parentNode = nodes.item(i);
                    NodeList parentChildrenNodes = parentNode.getChildNodes();
                    boolean found = false;
                    for(int j = 0; j < parentChildrenNodes.getLength(); j++) {
                        if(parentChildrenNodes.item(j).isEqualNode(oldNode)) {
                            found = true;
                            parentNode.removeChild(parentChildrenNodes.item(j));
                            docChanged = true;
                            break;
                        }
                    }
                    if(found) {
                        break;
                    }
                }
            }

            boolean originalDocChanged = false;
            NodeList originalNodes = originalDoc.getElementsByTagName(parent.getNodeName());
            for(int i = 0; i < originalNodes.getLength(); i++) {
                if(originalNodes.item(i).getNodeName().equals(parent.getNodeName())) {
                    Node parentNode = originalNodes.item(i);
                    NodeList parentChildrenNodes = parentNode.getChildNodes();
                    boolean found = false;
                    for(int j = 0; j < parentChildrenNodes.getLength(); j++) {
                        if(parentChildrenNodes.item(j).isEqualNode(oldNode)) {
                            found = true;
                            parentNode.removeChild(parentChildrenNodes.item(j));
                            originalDocChanged = true;
                            break;
                        }
                    }
                    if(found) {
                        break;
                    }
                }
            }
            if(applyChanges) {
                try {
                    if(docChanged) {
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        DOMSource source = new DOMSource(doc);
                        StreamResult result = new StreamResult(new File(xmlFile.getAbsolutePath()));
                        transformer.transform(source, result);
                    }

                    if(originalDocChanged) {
                        TransformerFactory transformerFactory = TransformerFactory.newInstance();
                        Transformer transformer = transformerFactory.newTransformer();
                        DOMSource source = new DOMSource(originalDoc);
                        StreamResult result = new StreamResult(new File(originalFile.getAbsolutePath()));
                        transformer.transform(source, result);
                    }
                } catch(Throwable e) {
                    e.printStackTrace();
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Error when writing in files"); //$NON-NLS-1$
                }
            }
            return Status.OK_STATUS;
        }
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The file is not valid"); //$NON-NLS-1$
    }

    /**
     * @param root The root to count his children
     * @return The number of children + 1 (the root)
     * @since 1.0
     */
    public static int getNodeCount(Node root) {
        int count = 0;
        if(root != null) {
            NodeList children = root.getChildNodes();
            count = 1;
            if(children.getLength() > 0) {
                for(int i = 0; i < children.getLength(); i++) {
                    if(children.item(i).getNodeName() != "#text") { //$NON-NLS-1$
                        count += getNodeCount(children.item(i));
                    }
                }
            }
        }
        return count;
    }

    /**
     * @param file The file in the runtime folder
     * @return The original file of the copy, or <code>null</code> if not found
     * @since 1.0
     */
    public static File getOriginalXmlFile(File file) {
        /*
         * FIXME Importing for the first time an XML file
         * will save his original path in the map declare above.
         * On the other hand, if the file is already loaded
         * (from the previous execution), then the original path
         * have never been save. This function will return <code>null</code>
         * in this case.
         */
        File original = xmlFiles.get(file);

        return original == null ? null:original;
    }

    /**
     * @param file
     *              The file to verify
     * @return
     *              Whether the file is present in the xml_files folder
     * @since 1.0
     */
    public static boolean xmlFileIsActive(File file) {
        // Get the active XML Analysis
        File activeXMLFolder = new File(XmlUtils.getXmlFilesPath().toString());
        File[] activeXMLs = activeXMLFolder.listFiles();

        if(!xmlValidate(file).isOK()) {
            return false;
        }

        // Validate the file
        boolean valid = false;
        for(int i = 0; i < activeXMLs.length; i++) {
            if(activeXMLs[i].getName().equals(file.getName()))
            {
                valid = true;
                break;
            }
        }
        return valid;
    }

    /**
     * This function shall not be used anywhere except for
     * {@link Activator#stop(BundleContext)} method.
     * @since 1.0
     *
     */
    public static void clearXmlDirectory() {
        File file = getXmlFilesPath().toFile();
        if(file != null) {
            File[] files = file.listFiles();
            for(int i = 0; i < files.length; i++)
            {
                files[i].delete();
            }
            file.delete();
        }
    }
}