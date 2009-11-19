package com.tacitknowledge.simulator.formats;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.FormatAdapterException;
import com.tacitknowledge.simulator.SimulatorPojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Adapter interface for the JSON format
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class JsonAdapter extends BaseAdapter implements Adapter<Object>
{
    /**
     * Adapter parameters definition.
     */
    private static List<List> parametersList = new ArrayList<List>();

    /**
     * Adapts the data received from the inbound transport into JSON format.
     *
     * @param object object the incoming data object to adapt to JSON format.
     * @return an object constructed based on the inboud transport data.
     */
    public SimulatorPojo adaptFrom(Object object)
    {
        //TODO Implement this functionality.
        return null;
    }

    /**
     * Adapts the data from simulation to the JSON formatted object
     *
     * @param pojo simulator pojo with data to be adapted to JSON format
     * @return an object constructed based on the data received from execution of the simulation
     */
    public Object adaptTo(SimulatorPojo pojo)
    {
        //TODO Implement this functionality.
        return null;
    }

    /**
     * @inheritDoc
     * @return @see Adapter#getParametersList
     */
    public List<List> getParametersList()
    {
        return null;
    }

    /**
     * @inheritDoc
     * @throws FormatAdapterException If a required parameter is missing or not properly formatted
     */
    @Override
    void validateParameters() throws FormatAdapterException
    {

    }
}
