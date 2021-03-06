package com.tacitknowledge.simulator.formats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.tacitknowledge.simulator.*;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Adapter interface for the Properties format.
 * Properties should come in an "inheritance" structure.
 * e.g.:
 * employee.firstName=
 * employee.address.street=
 * employee.title
 * <p/>
 * Only one "root" object should exist.
 * Duplicate property names are not allowed and would throw an error.
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class PropertiesAdapter extends NativeObjectScriptingAdapter implements Adapter
{
    // --- Adapter parameters
    /**
     * Property level separator parameter name. OPTIONAL.
     * Defaults to dot (".")
     */
    public static final String PARAM_PROPERTY_SEPARATOR = "propertySeparator";

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(PropertiesAdapter.class);

    /**
     * @inheritDoc
     */
    public PropertiesAdapter()
    {
    }

    public PropertiesAdapter(Configurable configurable) {
        super(configurable);
    }

    /**
     * @inheritDoc
     * @param exchange - Exchange object
     * @return SimulatorPojo
     * @throws FormatAdapterException - if generated pojo cannot be transformed
     */
    @Override
    protected SimulatorPojo createSimulatorPojo(final Exchange exchange)
        throws FormatAdapterException
    {
        String object = exchange.getIn().getBody(String.class);

        logger.debug("Attempting to generate SimulatorPojo from Properties content:\n{}", object);

        SimulatorPojo pojo = new StructuredSimulatorPojo();

        // --- First, split the incoming data into lines
        Pattern p = Pattern.compile("$", Pattern.MULTILINE);
        String[] rows = p.split(object);

        // --- Iterate through the lines
        for (String rowString : rows)
        {
            // --- Get property name/path and value
            String[] propNameValue = rowString.split("=");

            // --- Check the property separator in case we're using dot (.)
            String splitterRegEx = getPropertySeparator();
            if (getPropertySeparator().equals("."))
            {
                splitterRegEx = "\\" + getPropertySeparator();
            }

            List<String> propName =
                new ArrayList<String>(
                    Arrays.asList(propNameValue[0].split(splitterRegEx)));
            String propValue = propNameValue[1];

            setPropertyToMap(pojo.getRoot(), propName, propValue);
        }

        logger.debug("Finished generating SimulatorPojo from Properties content");
        return pojo;
    }

    /**
     * @inheritDoc
     * @param simulatorPojo - SimulatorPojo instance
     * @param exchange The Camel exchange
     * @return representation of properties as string
     * @throws FormatAdapterException If an exception occurs when converting a pojo into a string
     */
    @Override
    protected String getConversationResponseAsString(final SimulatorPojo simulatorPojo, final Exchange exchange)
        throws FormatAdapterException
    {
        if (simulatorPojo.getRoot().isEmpty())
        {
            throw new FormatAdapterException("Simulator Pojo root is empty.");
        }

        return getPropertiesAsString("", simulatorPojo.getRoot());
    }

    /**
     * @throws ConfigurableException if any required parameter is missing
     * @inheritDoc
     */
    public void validateParameters() throws ConfigurableException
    {

    }

    private String getPropertySeparator() {
        if (configuration.getParamValue(PARAM_PROPERTY_SEPARATOR) != null)
        {
            return configuration.getParamValue(PARAM_PROPERTY_SEPARATOR);
        }
        return ".";
    }

    /**
     * 
     * @param container The properties container Map
     * @param path The property path as a list
     * @param value The property value
     * @throws FormatAdapterException If any error occurs
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setPropertyToMap(
        final Map<String, Object> container,
        final List<String> path,
        final String value)
        throws FormatAdapterException
    {
        // --- Get the "current" path name.
        // Make sure to remove any potential line-break or space character
        String current = path.remove(0).trim();

        // --- Check if current exists as a key in the container map already
        if (container.containsKey(current))
        {
            // --- Get the key's value
            Object keyValue = container.get(current);

            // --- Check its instance
            if (keyValue instanceof Map)
            {
                // --- If the keyValue is a Map, there should be more path elements
                if (path.size() > 0)
                {
                    // --- So, go down the tree
                    setPropertyToMap((Map<String, Object>) keyValue, path, value);
                }
                else
                {
                    // --- If there are no path entries left, something is wrong
                    throw new FormatAdapterException(
                        "Expecting either leaf path name or further path declaration. "
                                + "Current path name: " + current);
                }
            }
            else if (keyValue instanceof String)
            {
                // --- If the instance is a String, we got a duplicate property name
                throw new FormatAdapterException(
                    "Duplicate property name. Current path name: " + current
                );
            }
        }
        else
        {
            // --- If it's a new key and...
            if (path.size() > 0)
            {
                // --- ...there are more path entries, create a new Map
                Map currentValue = new HashMap<String, Object>();
                // --- Go down
                setPropertyToMap(currentValue, path, value);
                // --- And set the map into the current key
                container.put(current, currentValue);
            }
            else
            {
                // --- ...this is a leaf name, set the value in the current key
                container.put(current, value);
            }
        }
    }

    /**
     * 
     * @param path  Accumulated property path
     * @param map   Map containing properties
     * @return      The String representation of the properties in #map
     */
    @SuppressWarnings("unchecked")
    private String getPropertiesAsString(final String path, final Map<String, Object> map)
    {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            Object value = entry.getValue();
            String fullPath = path + entry.getKey();

            // --- Check the value instance
            if (value instanceof Map)
            {
                // --- If it's a Map, append the key to the current path and go down
                sb.append(
                    getPropertiesAsString(
                        fullPath + getPropertySeparator(),
                        (Map<String, Object>) value));
            }
            else
            {
                // --- If it's a string, get property's string representation
                sb.append(fullPath + "=" + value + LINE_SEP);
            }
        }
        return sb.toString();
    }
}
