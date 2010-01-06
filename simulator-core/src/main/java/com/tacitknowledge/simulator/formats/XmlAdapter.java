package com.tacitknowledge.simulator.formats;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.FormatAdapterException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder;
import static com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder.name;
import static com.tacitknowledge.simulator.configuration.ParametersListBuilder.parameters;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Adapter interface for the XML format
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class XmlAdapter extends BaseAdapter implements Adapter<Object>
{
    /**
     * Validate parameter. If the XML text should DTD validated. OPTIONAL.
     * Defaults to false (no DTD validation)
     */
    public static final String PARAM_VALIDATE = "validate";

    /**
     *
     */
    public static final String PARAM_ROOT_TAG_NAME = "rootTagName";

    /**
     * XML indentation value
     */
    private static final int XML_INDENT = 4;

    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(XmlAdapter.class);

    /**
     * Adapter parameters definition.
     */
    private List<ParameterDefinitionBuilder.ParameterDefinition> parametersList =
        parameters().
            add(
                name(PARAM_VALIDATE).
                    label("Validate?").
                    type(ParameterDefinitionBuilder.ParameterDefinition.TYPE_BOOLEAN)
            ).
            add(
                name(PARAM_ROOT_TAG_NAME).
                    label("XML root tag name " +
                    "(outbound only)")
            );

    /**
     * The Document object used for XML generation in adaptTo() and helper methods
     */
    private Document doc;

    /**
     * @see #PARAM_VALIDATE
     */
    private boolean validate = false;

    /**
     * @inheritDoc
     */
    public XmlAdapter()
    {
    }

    /**
     * @param parameters @see Adapter#parameters
     * @inheritDoc
     */
    public XmlAdapter(Map<String, String> parameters)
    {
        super(parameters);
    }


    protected SimulatorPojo createSimulatorPojo(String o)
        throws FormatAdapterException
    {
        logger.debug("Attempting to generate SimulatorPojo from XML content:\n" + o);

        SimulatorPojo pojo = new StructuredSimulatorPojo();

        try
        {
            // --- First, parse the XML string into a document
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(this.validate);

            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(o));

            doc = db.parse(is);

            // --- Well-formatted Xml should have a single root, right?
            Element docElem = doc.getDocumentElement();
            Map<String, Object> root = new HashMap<String, Object>();
            root.put(docElem.getTagName(), getStructuredChilds(docElem));

            pojo.setRoot(root);
        }
        catch (ParserConfigurationException e)
        {
            String errorMessage = "Unexpected parser configuration exception";
            logger.error(errorMessage, e);
            throw new FormatAdapterException(errorMessage, e);
        }
        catch (SAXException e)
        {
            String errorMessage = "Unexpected SAX exception";
            logger.error(errorMessage, e);
            throw new FormatAdapterException(errorMessage, e);
        }
        catch (IOException e)
        {
            String errorMessage = "Unexpected IO exception";
            logger.error(errorMessage, e);
            throw new FormatAdapterException(errorMessage, e);
        }

        logger.debug("Finished generating SimulatorPojo from XML content");
        return pojo;
    }


    protected String getString(SimulatorPojo simulatorPojo)
        throws FormatAdapterException
    {
        try
        {
            // --- The SimulatorPojo for XmlAdapter should contain only one key in its root
            if (simulatorPojo.getRoot().isEmpty() || simulatorPojo.getRoot().size() > 1)
            {
                logger.error("Incorrect SimulatorPojo's root size. Expecting 1, but found"
                    + simulatorPojo.getRoot().size());
                throw new
                    FormatAdapterException(
                    "Incorrect SimulatorPojo's root size. Expecting 1, but found"
                        + simulatorPojo.getRoot().size());
            }

            // --- Get a DOM DocumentFactoryBuilder and the corresponding builder
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();

            doc = db.newDocument();

            // --- Generate the XML document - In XmlAdapter,
            // the only element in root is guaranteed to be a Map
            for (Map.Entry<String, Object> entry : simulatorPojo.getRoot().entrySet())
            {
                String rootTagName = entry.getKey();
                // --- Override the default root name is one was provided
                if (getParamValue(PARAM_ROOT_TAG_NAME) != null)
                {
                    rootTagName = getParamValue(PARAM_ROOT_TAG_NAME);
                }

                doc.appendChild(
                    getStructuredElement(
                        rootTagName,
                        (Map<String, Object>) entry.getValue()));
            }

            // --- Serialize the generated XML document
            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            tf.setAttribute("indent-number", XML_INDENT);

            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);
            return writer.toString();
        }
        catch (TransformerConfigurationException tce)
        {
            String errorMsg = "Error trying to transform XML document into String: "
                + tce.getMessage();
            logger.error(errorMsg, tce);
            throw new FormatAdapterException(errorMsg, tce);
        }
        catch (TransformerException te)
        {
            String errorMsg = "Error trying to transform XML document into String: "
                + te.getMessage();
            logger.error(errorMsg, te);
            throw new FormatAdapterException(errorMsg, te);
        }
        catch (ParserConfigurationException pce)
        {
            throw new FormatAdapterException("Error trying to get a DocumentBuilder", pce);
        }
    }

    /**
     * Returns a structured representation of the XML element on a Map,
     * in which container nodes are represented as
     * Map value, duplicated-name nodes as List value and the leaf nodes as String values
     *
     * @param elem the element to structure the child nodes for
     * @return a Map of child nodes of the XML document
     */
    private Map getStructuredChilds(Element elem)
    {
        // --- The Map to be returned
        Map<String, Object> structuredChild = new HashMap<String, Object>();

        // --- Get first child
        Node nd = elem.getFirstChild();
        // --- Iterate throu all the elem childs
        while (nd != null)
        {
            if (!(nd instanceof Element))
            {
                // --- If nd is not an Element, we skip it and go to the next child
                nd = nd.getNextSibling();
                continue;
            }

            // --- We make sure we get an Element so we can get the underlying node name
            Element child = (Element) nd;
            String currNodeName = child.getTagName();

            // --- Check if the structuredChild Map contains the current node name
            if (structuredChild.containsKey(currNodeName))
            {
                // --- Get the original attribute,
                // if this attribute name is already registered, it means this should be a List
                Object tmp = structuredChild.get(currNodeName);
                // --- Check if it's already a List
                List currList;
                if (tmp instanceof List)
                {
                    // --- If it is, just keep the reference
                    currList = (List) tmp;
                }
                else
                {
                    // --- If it isn't, create a new list...
                    currList = new ArrayList();
                    // --- ...insert the previous attribute value to the list
                    currList.add(tmp);
                    // --- ...and place the list into its corresponding attribute name
                    structuredChild.put(currNodeName, currList);
                }

                Map childValue = getStructuredChilds(child);
                // --- If there was only one text child, use its value
                if (childValue == null || childValue.isEmpty())
                {
                    // --- Add the String value
                    currList.add(child.getFirstChild().getNodeValue());
                }
                else
                {
                    // --- Add the current node as a structured child
                    currList.add(getStructuredChilds(child));
                }
            }
            else
            {
                // --- If the currNodeName hasn't been registered,
                // try to get its structured childs
                Map childValue = getStructuredChilds(child);
                if (childValue == null || childValue.isEmpty())
                {
                    // --- If the childValue is null or empty, means the
                    // underlying child is a Text node.
                    // Just assign the child's text value as it is.
                    structuredChild.put(currNodeName, child.getFirstChild().getNodeValue());
                }
                else
                {
                    // --- otherwise, assign the obtained structured Map
                    structuredChild.put(currNodeName, childValue);
                }
            }

            nd = child.getNextSibling();
        }

        return structuredChild;
    }

    /**
     * Returns an new XML Element with its inners children
     *
     * @param elemName The node name of the element to be returned
     * @param childs   Map containing the childs to be contained within the generated Element
     * @return The generated XML Element with its inner children
     */
    private Element getStructuredElement(String elemName, Map<String, Object> childs)
    {
        Element element = doc.createElement(elemName);

        // --- Iterate throu all the childs
        for (Map.Entry<String, Object> entry : childs.entrySet())
        {
            Element child;
            // --- If the Entry value is...
            if (entry.getValue() instanceof Map)
            {
                // --- ...a Map, get the structured element
                child = getStructuredElement(
                    entry.getKey(),
                    (Map<String, Object>) entry.getValue());
            }
            else if (entry.getValue() instanceof List)
            {
                // --- ...a List, add all List members to the current element
                for (Object item : ((List) entry.getValue()))
                {
                    // --- If the item is...
                    if (item instanceof Map)
                    {
                        // --- ...a Map, add the structured element and corresponding children
                        String newElName = entry.getKey();
                        if (newElName == null)
                        {
                            newElName = "anonymous-list";
                        }
                        element.appendChild(
                            getStructuredElement(newElName, (Map<String, Object>) item));
                    }
                    else if (item instanceof List)
                    {
                        // --- If it's a recurring list, get an anonymously-name-elements list
                        element.appendChild(getListElement(entry.getKey(), (List) item));
                    }
                    else
                    {
                        // --- ...a String (should be safe to assume),
                        // add the text element   directly
                        String newElName = entry.getKey();
                        if (newElName == null)
                        {
                            newElName = "anonymous-list-element";
                        }
                        element.appendChild(getTextElement(newElName, item.toString()));
                    }
                }
                // --- We don't return a child Element here,
                // because we already added the List members, so skip to the next Entry
                continue;
            }
            else
            {
                // ...a String (should be), add a text element
                child = getTextElement(entry.getKey(), entry.getValue().toString());
            }
            // --- Append the returned child to the current Element
            element.appendChild(child);
        }
        return element;
    }

    /**
     * Returns an new XML Element with its inner children
     *
     * @param elemName The node name of the element to be returned
     * @param childs   List containing the children to be contained within the generated Element
     * @return The generated XML Element with its inner children
     */
    private Element getListElement(String elemName, List childs)
    {
        Element element = doc.createElement(elemName);

        for (Object o : childs)
        {
            Element child;
            // --- If the Entry value is...
            if (o instanceof Map)
            {
                // --- ...a Map, get the structured element
                child = getStructuredElement(
                    "list-element",
                    (Map<String, Object>) o);
            }
            else if (o instanceof List)
            {
                child = getListElement("list-element", (List) o);
            }
            else
            {
                // ...a String (should be), add a text element
                child = getTextElement("list-element", o.toString());
            }
            // --- Append the returned child to the current Element
            element.appendChild(child);
        }
        return element;
    }

    /**
     * Returns an XML Element containing only one TextNode child.
     *
     * @param elemName The containing element node name
     * @param text     The text for the TextNode child
     * @return The XML Element containing the TextNode
     */
    private Element getTextElement(String elemName, String text)
    {
        Node textNode = doc.createTextNode(text);

        try
        {
            Element container = doc.createElement(elemName);
            container.appendChild(textNode);

            return container;
        }
        catch (DOMException de)
        {
            logger.debug("Unexpected DOM Exception for element name: " + elemName + ". \n"
                + de.getMessage());
            throw de;
        }
    }

    /**
     * @throws FormatAdapterException
     * @inheritDoc
     */
    @Override
    void validateParameters() throws FormatAdapterException
    {
        if (getParamValue(PARAM_VALIDATE) != null)
        {
            this.validate = Boolean.parseBoolean(getParamValue(PARAM_VALIDATE));
        }
    }

    public List<List> getParametersList()
    {
        return getParametersDefinitionsAsList(parametersList);
    }
}
