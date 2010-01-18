package com.tacitknowledge.simulator.formats;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.ConfigurableException;
import com.tacitknowledge.simulator.FormatAdapterException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder;

import static com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder.name;
import static com.tacitknowledge.simulator.configuration.ParametersListBuilder.parameters;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.camel.Exchange;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Implementation of the Adapter interface for the REST format
 *
 * @author Raul Huerta (rhuerta@tacitknowledge.com)
 */

public class RestAdapter extends BaseAdapter implements Adapter<Object>
{

    private static final String PARAM_EXTRACTION_PATTERN = "extractionPattern";

    private static final String PARAM_OBJECT_NAME = "objectName";

    private static final String RESPONSE = "response";

    private static final String REQUEST = "request";

    private static final String BODY = "body";

    private static final String CONTENT_TYPE = "contentType";

    private static final String STATUS_CODE = "statusCode";

    private static final String DEFAULT_STATUS_CODE = "200";

    private static final String DEFAULT_EXTRACTION_PATTERN = "/";

    private static final String HTML_CONTENT_TYPE = "text/html";

    private static final String DEFAULT_OBJECT_NAME = "obj";

    /**
     * Adapter parameters definition.
     */
    private List<ParameterDefinitionBuilder.ParameterDefinition> parametersList =
        parameters()
            .add(
                name(PARAM_EXTRACTION_PATTERN).
                    label("Pattern used to generate objects. (e.g. URL: '" + DEFAULT_EXTRACTION_PATTERN + "system/1' with pattern:'/system/:system_id' will generate a " +
                            "'system' object with an attribute call'system_id' equals to '1')").
                    inOnly().required()
             ).add(
                name(PARAM_OBJECT_NAME).
                    label("Object Name to access attributes from the execution script. Defaults to 'obj'").
                    inOnly().required()
            );

    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(RestAdapter.class);
    private static final String PARAMS = "params";
    private static final String METHOD = "method";

    /**
     * @inheritDoc
     */
    public RestAdapter() {
        super();
    }

    /**
     * @inheritDoc
     */
    public RestAdapter(int bound, Map<String, String> parameters)
    {
        super(bound, parameters);
    }

    /**
     * @throws ConfigurableException
     * @inheritDoc
     */
    @Override
    public void validateParameters() throws ConfigurableException {

    }

    /**
     * Get all parameters for this adapter
     * @return List of Lists
     */
    public List<List> getParametersList() {
        return getParametersDefinitionsAsList(parametersList);
    }


    /**
     * {@inheritDoc}
     */
    @Override
     protected SimulatorPojo createSimulatorPojo(Exchange o) throws FormatAdapterException {

        logger.debug("Attempting to generate SimulatorPojo from REST content:\n" + o);

        HttpServletRequest request = o.getIn().getBody(HttpServletRequest.class);

        SimulatorPojo pojo = new StructuredSimulatorPojo();

        Map attributes = new HashMap<String, Object>();
        String parameterExtractionPattern = getParamValue(PARAM_EXTRACTION_PATTERN) == null ? DEFAULT_EXTRACTION_PATTERN : getParamValue(PARAM_EXTRACTION_PATTERN);
        attributes.put(REQUEST, populateRequestAttributes(request, parameterExtractionPattern));
        attributes.put(RESPONSE, populateResponseAttributes());

        String parameterObjectName = getParamValue(PARAM_OBJECT_NAME) == null ? DEFAULT_OBJECT_NAME : getParamValue(PARAM_OBJECT_NAME);
        pojo.getRoot().put(parameterObjectName, attributes);

        logger.debug("Finished generating SimulatorPojo from REST content");

        return pojo;
    }

    /**
     * Populate a map with default response values for status code, content type and response body.
     * @return Map of String, Object entries
     */
    private Map<String, Object> populateResponseAttributes() {
        Map<String, Object> responseMap = new HashMap<String, Object>();
        responseMap.put(STATUS_CODE, DEFAULT_STATUS_CODE);
        responseMap.put(CONTENT_TYPE, HTML_CONTENT_TYPE);
        responseMap.put(BODY, "");
        return responseMap;
    }

    /**
     * Populate a map with values coming from the HTTP request plus the ones coming from the REST request.
     * @param request - HttpServlet Request
     * @param extractionPattern - Pattern used to parse REST URL
     * @return Map of String, Object entries
     */
    private Map<String, Object> populateRequestAttributes(HttpServletRequest request, String extractionPattern) {
        Map<String, Object> requestMap = new HashMap<String, Object>();

        Map<String, Object> paramMap = new HashMap<String,Object>();
        if (request != null) {

            requestMap.put(METHOD, request.getMethod());

            Enumeration enumer  = request.getParameterNames();
            while(enumer.hasMoreElements()){
                String element = (String)enumer.nextElement();
                paramMap.put(element, request.getParameter(element));
            }

            Map<String, Object> values = extractValuesFromUri(request.getRequestURI(), extractionPattern);

            if(null != values && !values.isEmpty()) {
                paramMap.putAll(values);
            }
        }
        requestMap.put(PARAMS, paramMap);

        return requestMap;
    }

    /**
     * @inheritDoc
     */
    @Override
    protected Object getString(SimulatorPojo scriptExecutionResult, Exchange exchange) throws FormatAdapterException {

        HttpServletResponse response = null;
        String body = "";
        //get the first entry
        Map<String, Object> pojo = null;
        if(scriptExecutionResult.getRoot() != null){
            for(Map.Entry<String, Object> entry : scriptExecutionResult.getRoot().entrySet()){
                pojo = (Map<String, Object>) entry.getValue();
                break;
            }
        }

        if(pojo != null && pojo.size() > 0) {
            Map<String,Object> responseMap = (Map<String, Object>) pojo.get(RESPONSE);
            body = (String) responseMap.get(BODY);
            String contentType = (String) responseMap.get(CONTENT_TYPE);
            String statusCode = (String) responseMap.get(STATUS_CODE);

            response = exchange.getIn().getBody(HttpServletResponse.class);

            response.setContentType(contentType);
            response.setStatus(Integer.parseInt(statusCode));
        }
        return body;
    }

    /**
     * Extracts information from the URL given a REST pattern
     * @param url URL
     * @param pattern pattern to use
     * @return Map with parameters read from the URL
     */
    protected Map<String, Object> extractValuesFromUri(String url, String pattern) {


        String[] patternArr = pattern.split(DEFAULT_EXTRACTION_PATTERN);
        Object[] urlArr = url.split(DEFAULT_EXTRACTION_PATTERN);
        Map<String, Object> parameterMap = new HashMap<String, Object>();

        int patternArrLength = patternArr.length;
        for(int i = 0; i < patternArrLength ; i ++){
            String str = patternArr[i];
            if(str.startsWith(":")){
                try{
                    parameterMap.put(str.substring(1), urlArr[i]);
                }catch(ArrayIndexOutOfBoundsException e){
                    //This is fine, since it means that the url is shorter that the pattern.
                    //We just swallow the exception and continue.
                }
            }
        }
        return parameterMap;
    }

}
