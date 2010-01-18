package com.tacitknowledge.simulator.transports;

import java.util.List;
import java.util.Map;

/**
 * Marker class to be used for rest transport
 * @author Raul Huerta (rhuerta@acitknowledge.com)
 */
public class RestTransport extends HttpTransport {
    //Marker class for REST


    /**
     * Default Cosntructor
     */
    public RestTransport() {
        super(TransportConstants.REST);
    }


    /**
     * Constructor to initialize parameters
     * @param bound Configurable bound
     * @param parameters - Map of String, String values
     */
    public RestTransport(int bound, Map<String, String> parameters) {
        super(bound, TransportConstants.REST, parameters);
    }

    /**
     * Returns a List of parameters the implementing instance uses.
     * Each list element is itself a List to describe the parameter as follows:
     * <p/>
     * - 0 : Parameter name
     * - 1 : Parameter description. Useful for GUI rendition
     * - 2 : Parameter type. Useful for GUI rendition.
     * - 3 : Required or Optional parameter. Useful for GUI validation.
     * - 4 : Parameter usage. Useful for GUI rendition.
     * - 5 : Default value
     *
     * @return List of Parameters for the implementing Transport.
     * @see com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder
     * @see com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder.ParameterDefinition
     * @see com.tacitknowledge.simulator.BaseConfigurable#parametersList
     */
    @Override
    public List<List> getParametersList()
    {
        return getParametersDefinitionsAsList(parametersList);
    }
}