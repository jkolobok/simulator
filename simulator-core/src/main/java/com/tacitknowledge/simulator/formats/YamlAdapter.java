package com.tacitknowledge.simulator.formats;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.tacitknowledge.simulator.*;
import com.tacitknowledge.simulator.scripting.ObjectMapperException;
import com.tacitknowledge.simulator.scripting.SimulatorPojoPopulatorImpl;
import org.apache.camel.Exchange;
import org.ho.yaml.YamlDecoder;
import org.ho.yaml.YamlEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the Adapter interface for the YAML format
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class YamlAdapter extends NativeObjectScriptingAdapter implements Adapter
{
    /**
     * String describing what are the YAML contents. REQUIRED.
     * This will be used as the SimulatorPojo root's record key.
     * e.g.: employees, orders, products, etc.
     */
    public static final String PARAM_YAML_CONTENT = "yamlContent";

    /**
     * Is Array parameter. Determines if the YAML content is an Array. OPTIONAL.
     * Defaults to false.
     * If this parameter is true, it's recommended that #PARAM_YAML_CONTENT uses a plural
     * word and #PARAM_YAML_ARRAY_CONTENT its singular form.
     */
    public static final String PARAM_IS_ARRAY = "isArray";

    /**
     * JSON array content parameter. Describes each array element content. OPTIONAL.
     * Required if #PARAM_IS_ARRAY is true.
     * e.g.: employee, order, product, etc.
     */
    public static final String PARAM_YAML_ARRAY_CONTENT = "jsonArrayContent";

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(YamlAdapter.class);


    /**
     * @inheritDoc
     */
    public YamlAdapter()
    {
    }

    public YamlAdapter(Configurable configurable) {
        super(configurable);
    }

    /**
     * @inheritDoc
     * @param exchange - Exchange message
     * @return SimulatorPojo object
     * @throws FormatAdapterException - if an error occurs
     */
    @Override
    protected SimulatorPojo createSimulatorPojo(final Exchange exchange)
        throws FormatAdapterException
    {
        String object = exchange.getIn().getBody(String.class);
        logger.debug("Attempting to generate SimulatorPojo from YAML content:\n{}", object);

        SimulatorPojo pojo = new StructuredSimulatorPojo();

        YamlDecoder dec = new YamlDecoder(new StringReader(object));

        try
        {
            Object yaml = dec.readObject();

            if (isArray())
            {
                logger.debug("Expecting YAML array in content. Processing as such.");

                Map<String, Object> map = new HashMap<String, Object>();
                map.put(configuration.getParamValue(PARAM_YAML_ARRAY_CONTENT), yaml);
                pojo.getRoot().put(configuration.getParamValue(PARAM_YAML_CONTENT), map);
            }
            else
            {
                pojo.getRoot().put(configuration.getParamValue(PARAM_YAML_CONTENT), yaml);
            }
        }
        catch (EOFException e)
        {
            throw new FormatAdapterException("Unexpected error trying to read YAML object:", e);
        }

        logger.debug("Finished generating SimulatorPojo from YAML content");
        return pojo;
    }

    /**
     * @inheritDoc
     * @param pojo The object returned by the scenario excecution script,
     *      in its SimulatorPojo representation
     * @param exchange The Camel exchange
     * @return output in string format
     * @throws FormatAdapterException if an error occurs
     */
    @Override
    protected String getConversationResponseAsString(final SimulatorPojo pojo, final Exchange exchange)
        throws FormatAdapterException
    {
        // --- Only one entry in the root should exist
        if (pojo.getRoot().isEmpty() || pojo.getRoot().size() > 1)
        {
            String errorMsg = "SimulatorPojo's root should contain only one Entry for YAML adaptToOutput";
            throw new FormatAdapterException(errorMsg);
        }

        OutputStream os = new ByteArrayOutputStream();
        YamlEncoder enc = new YamlEncoder(os);

        enc.writeObject(pojo.getRoot().get(configuration.getParamValue(PARAM_YAML_CONTENT)));
        enc.close();

        return os.toString();
    }

    protected SimulatorPojo getSimulatorPojo(final Object object) throws ObjectMapperException
    {
        final SimulatorPojo payload = SimulatorPojoPopulatorImpl.getInstance().populateSimulatorPojoFromBean(object,
                configuration.getParamValue(PARAM_YAML_CONTENT));
        return payload;

    }

    /**
     * @throws ConfigurableException if any required parameter is missing
     * @inheritDoc
     */
    public void validateParameters() throws ConfigurableException
    {

        if (configuration.getParamValue(PARAM_YAML_CONTENT) == null)
        {
            throw new ConfigurableException("YAML content is required");
        }
        if (isArray() && configuration.getParamValue(PARAM_YAML_ARRAY_CONTENT) == null)
        {
            throw new ConfigurableException(
                "YAML Array Content parameter is required if YAML content is an array");
        }
    }

    protected boolean isArray() {
        return (configuration.getParamValue(PARAM_IS_ARRAY) != null)
                && Boolean.parseBoolean(configuration.getParamValue(PARAM_IS_ARRAY));

    }
}
