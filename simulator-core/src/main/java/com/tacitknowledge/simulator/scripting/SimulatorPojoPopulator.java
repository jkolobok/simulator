package com.tacitknowledge.simulator.scripting;

import com.tacitknowledge.simulator.SimulatorException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This classes will take an dynamically-generated-class object and its attributes
 * and return a properly populated simulator pojo with the use of reflection
 *
 * @author galo
 */
public class SimulatorPojoPopulator
{
    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(SimulatorPojoPopulator.class);

    /**
     * Singleton instance
     */
    private static SimulatorPojoPopulator _instance;

    /**
     * Hide default constructor
     */
    private SimulatorPojoPopulator()
    {
    }

    /**
     * @return The singleton instance
     */
    public static SimulatorPojoPopulator getInstance()
    {
        if (_instance == null)
        {
            _instance = new SimulatorPojoPopulator();
        }
        return _instance;
    }

    /**
     * This object returns a SimulatorPojo populated with the object contained in beansMap.
     * It's exactly the opposite of PojoClassGenerator#generateBeansMap
     *
     * @param beansMap The benas map generated by PojoClassGenerator#generateBeansMap
     * @return A SimulatorPojo instance populated from the map and its included attributes
     * @throws SimulatorException If anything goes wrong during analyzing the object fields
     */
    public SimulatorPojo populateSimulatorPojoFromBean(Map<String, Object> beansMap)
            throws SimulatorException
    {
        // ---
        SimulatorPojo pojo = new StructuredSimulatorPojo();

        // --- Beans Map should only contain ONE entry
        if (beansMap.isEmpty() || beansMap.size() > 1)
        {
            throw new SimulatorException(
                    "beansMap should be neither empty nor contain more than 1 entry.");
        }

        // --- Use the incoming object's class name as root's entry key
        for (Map.Entry<String, Object> entry : beansMap.entrySet())
        {
            pojo.getRoot().put(entry.getKey(), getMapFromObject(entry.getValue()));
        }

        return pojo;
    }

    /**
     * Returns a Map from an object. Each attribute in the object will become a key in the Map.
     *
     * @param o The object to be mapped
     * @return The map representation of the passed object
     * @throws SimulatorException If anything goes wrong
     */
    private Map<String, Object> getMapFromObject(Object o) throws SimulatorException
    {
        Map<String, Object> map = new HashMap<String, Object>();

        // --- Iterate through the attributes
        for (Field field : o.getClass().getDeclaredFields())
        {
            String fieldName = field.getName();
            Object fieldValue = null;
            try
            {
                fieldValue = field.get(o);
            }
            catch (IllegalAccessException iae)
            {
                logger.error(iae.getMessage());
                throw new SimulatorException(
                        "Unexpected error accesing field value: " + iae.getMessage(),
                        iae);
            }

            // --- Depending on the value type, get it's representation
            Object mapValue = null;
            if (fieldValue instanceof String)
            {
                // --- If it's a String, use as it is
                mapValue = fieldValue;
            }
            else if (fieldValue.getClass().isArray())
            {
                // --- If it's an Array, get a List from it
                mapValue = getListFromArray((Object[]) fieldValue);
            }
            else
            {
                // --- By default, if it's not a String nor Array, we assume it's a custom class
                mapValue = getMapFromObject(fieldValue);
            }

            map.put(fieldName, mapValue);
        }

        return map;
    }

    /**
     * Returns a List from an array. The array will populated with either Maps or Strings,
     * depending on the Array contents
     *
     * @param objects The Array to be List-ified
     * @return The list populated with eithert Strings or the Map representation of its items
     * @throws SimulatorException If anything goes wrong
     */
    private List getListFromArray(Object[] objects) throws SimulatorException
    {
        List list = new ArrayList();
        for (Object o : objects)
        {
            if (o instanceof String)
            {
                list.add(o);
            }
            else
            {
                list.add(getMapFromObject(o));
            }
        }
        return list;
    }
}