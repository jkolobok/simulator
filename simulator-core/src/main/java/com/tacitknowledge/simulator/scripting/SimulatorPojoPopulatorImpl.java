package com.tacitknowledge.simulator.scripting;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;

/**
 * This classes will take an dynamically-generated-class object and its attributes
 * and return a properly populated simulator pojo with the use of reflection
 *
 * @author galo
 */
public final class SimulatorPojoPopulatorImpl
{
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(SimulatorPojoPopulatorImpl.class);

    /**
     * Singleton instance
     */
    private static SimulatorPojoPopulatorImpl aINSTANCE;
    public static final String NATIVEOBJECT = "nativeobject";

    /**
     * Hide default constructor
     */
    private SimulatorPojoPopulatorImpl()
    {
    }

    /**
     * @return The singleton instance
     */
    public static SimulatorPojoPopulatorImpl getInstance()
    {
        if (aINSTANCE == null)
        {
            aINSTANCE = new SimulatorPojoPopulatorImpl();
        }
        return aINSTANCE;
    }

    /**
     * This object returns a SimulatorPojo populated with the object contained in beansMap.
     * It's exactly the opposite of PojoClassGenerator#generateBeansMap
     *
     * @param bean The main bean. This is the
     *             bean returned by the execution script and the result's expected root element
     * @return A SimulatorPojo instance populated from the map and its included attributes
     * @throws ObjectMapperException If anything goes wrong during analyzing the object fields
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SimulatorPojo populateSimulatorPojoFromBean(final Object bean)
        throws ObjectMapperException
    {
        logger.debug("Attempting to create SimulatorPojo from bean {}", bean);

        // ---
        SimulatorPojo pojo = new StructuredSimulatorPojo();


        // --- Bean should only contain ONE entry
        if (bean == null)
        {
            return pojo;
        }

        Map<String, Object> stringObjectMap;

        String className = bean.getClass().getName();
        //todo - currently only has been tested as supporting org.mozilla.javascript
		if (className.startsWith("org.mozilla.javascript"))
        {
            stringObjectMap = new JavaScriptObjectMapper().getMapFromObject(bean);
        }
        else if (className.startsWith("org.jruby"))
        {
            stringObjectMap = new RubyObjectMapper().getMapFromObject(bean);
        }
        //this was added to facilitate work with java maps created specifically for SOAP.
        //the plan is to use java maps with arbitrary content to build the SOAP response, not the POJO
        //built from values in the SOAP request.
        //This code could be moved somewhere in the SoapAdapter classes if it conflicts with other adapters.
        else if (bean instanceof Map)
        {
        	pojo.getRoot().putAll((Map) bean);
    		logger.info("Object parameter is already a map. Just adding it to POJO's root: " + bean);

        	return pojo;
        }
        else
        {
            stringObjectMap = new JavaObjectMapper().getMapFromObject(bean);
        }


        // --- Use the incoming object's class name (in lowercase) as root's entry key
        String simpleName = bean.getClass().getSimpleName().toLowerCase();
        pojo.getRoot().put(simpleName, stringObjectMap);

        return pojo;
    }

    public SimulatorPojo populateSimulatorPojoFromBean(final Object bean, String rootOverride) throws ObjectMapperException {
        StructuredSimulatorPojo pojo = (StructuredSimulatorPojo) populateSimulatorPojoFromBean(bean);
		if (pojo.getRoot() != null && pojo.getRoot().entrySet().size() > 0) {
			Map map = (Map) pojo.getRoot().entrySet().iterator().next()
			        .getValue();
			pojo.getRoot().clear();
			pojo.getRoot().put(rootOverride, map);
    		}
        return pojo;
    }
}
