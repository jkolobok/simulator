package com.tacitknowledge.simulator.scripting;

import com.tacitknowledge.simulator.SimulatorException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * This classes will take an dynamically-generated-class object and its attributes
 * and return a properly populated simulator pojo with the use of reflection
 *
 * @author galo
 */
public class SimulatorPojoPopulatorImpl
{
    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(SimulatorPojoPopulatorImpl.class);

    /**
     * Singleton instance
     */
    private static SimulatorPojoPopulatorImpl _instance;

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
        if (_instance == null)
        {
            _instance = new SimulatorPojoPopulatorImpl();
        }
        return _instance;
    }

    /**
     * This object returns a SimulatorPojo populated with the object contained in beansMap.
     * It's exactly the opposite of PojoClassGenerator#generateBeansMap
     *
     * @param bean The main bean generated by PojoClassGenerator#generateBeansMap. This is the
     *             bean returned by the execution script and the result's expected root element
     * @return A SimulatorPojo instance populated from the map and its included attributes
     * @throws SimulatorException If anything goes wrong during analyzing the object fields
     */
    public SimulatorPojo populateSimulatorPojoFromBean(Object bean)
        throws ObjectMapperException
    {
        logger.debug("Attempting to create SimulatorPojo from bean " + bean);

        // ---
        SimulatorPojo pojo = new StructuredSimulatorPojo();


        // --- Bean should only contain ONE entry
        if (bean == null)
        {
            throw new ObjectMapperException(
                "Bean is empty.");
        }

        Map<String, Object> stringObjectMap;

        String className = bean.getClass().getName();
        if (className.startsWith("org.mozilla.javascript"))
        {
            stringObjectMap = new JavaScriptObjectMapper().getMapFromObject(bean);
        }
        else if (className.startsWith("org.jruby"))
        {
            stringObjectMap = new RubyObjectMapper().getMapFromObject(bean);
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


}
