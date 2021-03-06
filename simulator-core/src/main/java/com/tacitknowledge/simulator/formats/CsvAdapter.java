package com.tacitknowledge.simulator.formats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.Configurable;
import com.tacitknowledge.simulator.ConfigurableException;
import com.tacitknowledge.simulator.FormatAdapterException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import com.tacitknowledge.simulator.scripting.ObjectMapperException;
import com.tacitknowledge.simulator.scripting.SimulatorPojoPopulatorImpl;

/**
 * Implementation of the Adapter interface for the CSV format
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class CsvAdapter extends NativeObjectScriptingAdapter implements Adapter
{
    // --- Adapter parameters
    /**
     * Column Separator parameter name.
     */
    public static final String PARAM_COLUMN_SEPARATOR = "columnSeparator";
    /**
     * String describing what are the CSV contents. REQUIRED.
     * This will be used as the SimulatorPojo root's record key.
     * e.g.: employees, orders, products, etc.
     */
    public static final String PARAM_CSV_CONTENT = "csvContent";
    /**
     * String describing what each row represents. REQUIRED if isFirstRowHeader is false.
     * This will be used as bean name during the simulation.
     * .e.g.: employee, order, product, etc.
     */
    public static final String PARAM_ROW_CONTENT = "rowContent";
    /**
     * First row header parameter name.
     */
    public static final String PARAM_FIRST_ROW_HEADER = "isFirstRowHeader";

    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(CsvAdapter.class);



    /**
     * @inheritDoc
     */
    public CsvAdapter()
    {
        super();
    }

    public CsvAdapter(Configurable configurable) {
        super(configurable);
    }

    /**
     * @inheritDoc
     * @param exchange The Camel exchange
     * @return The generated PopulatorPojo
     */
    @SuppressWarnings("rawtypes")
    @Override
    protected SimulatorPojo createSimulatorPojo(final Exchange exchange)
    {
        String o = exchange.getIn().getBody(String.class);

        logger.debug("Attempting to generate SimulatorPojo from CSV content:\n{}", o);

        SimulatorPojo pojo = new StructuredSimulatorPojo();

        // --- The SimulatorPojo's root will contain only one key (csvContent) with a List value
        List<Map> rowObjects = new ArrayList<Map>();

        // --- First, split the incoming data into lines
        Pattern p = Pattern.compile("$", Pattern.MULTILINE);
        String[] rows = p.split(o);

        // --- For all rows, new line characters will be remove before calling getRowAs methods

        // --- If the first row will be treated as column names, get the list of col names
        if (isFirstRowHeader())
        {
            logger.debug("Expecting first row as headers. Getting header names");

            List<String> colNames = getRowAsList(rows[0].trim());

            // --- Now, populate the pojos using the col names as attribute names
            for (int i = 1; i < rows.length; i++)
            {
                rowObjects.add(getRowAsMap(colNames,rows[i].trim()));
            }
        }
        else
        {
            // --- ...otherwise, populate the List with each row represented as a Map containing
            // a List of values in key rowContent
            for (String rowString : rows)
            {
                Map<String, List<String>> row = new HashMap<String, List<String>>();
                row.put(configuration.getParamValue(PARAM_ROW_CONTENT), getRowAsList(rowString));
                rowObjects.add(row);
            }
        }

        // --- Set the resulting list in the SimulatorPojo root
        pojo.getRoot().put(configuration.getParamValue(PARAM_CSV_CONTENT), rowObjects);

        logger.debug("Finished generating SimulatorPojo from CSV content");
        return pojo;
    }

    /**
     * @inheritDoc
     * @param simulatorPojo The SimulatorPojo containing the script result
     * @param exchange The Camel exchange
     * @return The String representation in CSV format of the SimulatorPojo data
     * @throws FormatAdapterException if an error occurs
     */
    @SuppressWarnings("unchecked")
    @Override
    protected String getConversationResponseAsString(final SimulatorPojo simulatorPojo, final Exchange exchange)
        throws FormatAdapterException
    {

        StringBuilder sb1 = new StringBuilder();
        // --- Only one entry in the root should exist
        if (simulatorPojo.getRoot().size() > 1)
        {
            String errorMsg = "SimulatorPojo's root should only one Entry for CSV adaptToOutput";
            throw new FormatAdapterException(errorMsg);
        }

        // --- Use the CsvContent parameter to get the only object, which should be a List
        Map<String, Object> map = simulatorPojo.getRoot();
		List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map) map
		        .get(configuration.getParamValue(PARAM_CSV_CONTENT)))
.get(null);

        List<String> colNames = getColumnsFromKeys(list.get(0).keySet());
        // --- If using first row as headers, get the headers from the first row's keys
        if (isFirstRowHeader())
        {
            sb1.append(getDelimitedHeaderFromColumns(colNames)).append(LINE_SEP);
        }

        // --- Iterate through each list item
        for (int i = 0; i < list.size(); i++)
        {
            sb1.append(getValuesFromMap(colNames,list.get(i)));

            if (i < (list.size() - 1))
            {
                sb1.append(LINE_SEP);
            }
        }

        return sb1.toString();
    }

    @Override
    protected SimulatorPojo getSimulatorPojo(final Object object) throws ObjectMapperException
    {
        final SimulatorPojo payload = SimulatorPojoPopulatorImpl.getInstance().populateSimulatorPojoFromBean(object,
                configuration.getParamValue(PARAM_CSV_CONTENT));
        return payload;

    }


    /**
     * @throws ConfigurableException If any required parameter is missing
     * @inheritDoc
     */
    @Override
    public void validateParameters() throws ConfigurableException
    {
        // ---
        if (configuration.getParamValue(PARAM_CSV_CONTENT) == null)
        {
            throw new ConfigurableException("CSV Content parameter is required.");
        }
        if (!isFirstRowHeader() && configuration.getParamValue(PARAM_ROW_CONTENT) == null)
        {
            String errorMsg = "RowContent parameter is required if CSV has no headers.";
            throw new ConfigurableException(errorMsg);
        }
    }
    protected String getColumnSeparator() {
        if (configuration.getParamValue(PARAM_COLUMN_SEPARATOR) != null)
        {
            return configuration.getParamValue(PARAM_COLUMN_SEPARATOR);
        }
        return ",";

    }
    protected boolean isFirstRowHeader() {
        //defaults to true
        boolean result = true;
        if (configuration.getParamValue(PARAM_FIRST_ROW_HEADER) != null)
            result = Boolean.parseBoolean(configuration.getParamValue(PARAM_FIRST_ROW_HEADER));
        return result;
    }


    /**
     * Returns a Map representing the given row. colNames will be used as keys.
     *
     * @param row The row values as a String, separated by #columnSeparator
     * @return The Map populated with the given column keys and row values
     */
    private Map<String, String> getRowAsMap(final List<String> colNames, final String row)
    {
        Map<String, String> rowMap = new HashMap<String, String>();

        String[] values = row.split(getColumnSeparator());
        for (int i = 0; i < values.length; i++)
        {
            // --- Make sure we remove leading and trailing spaces from the actual value
            rowMap.put(colNames.get(i), values[i].trim());
        }

        return rowMap;
    }

    /**
     * Returns a List of values from the row, as separated with #columnSeparator.
     *
     * @param row The row values as a String, separated by #columnSeparator
     * @return The List populated with the given row values
     */
    private List<String> getRowAsList(final String row)
    {
        List<String> rowList = new ArrayList<String>();

        String[] values = row.split(getColumnSeparator());
        for (String value : values)
        {
            // --- Make sure we remove leading and trailing spaces from the actual value
            rowList.add(value.trim());
        }

        return rowList;
    }

    /**
     * @param colNames The column names to get the delimited header string from
     * @return A String containing the key names, separated by #columnSeparator
     */
    private String getDelimitedHeaderFromColumns(final List<String> colNames)
    {
        StringBuilder sb = new StringBuilder();

        boolean firstKey = true;
        for (String column : colNames)
        {
            // ---
            if (firstKey)
            {
                firstKey = false;
            }
            else
            {
                sb.append(getColumnSeparator());
            }

            sb.append(column);
        }

        return sb.toString();
    }
    /**
     * @param keySet The key set where to get the header names from
     * @return A String containing the key names, separated by #columnSeparator
     */
    private List<String> getColumnsFromKeys(final Set<String> keySet)
    {
        List<String> colNames = new ArrayList<String>();
        colNames.addAll(keySet);
        return colNames;
    }



    /**
     * @param row The Map representing a CSV row
     * @return A String containing the row values, separated by #columnSeparator
     */
    @SuppressWarnings("unchecked")
    private String getValuesFromMap(final List<String> colNames,final Map<String, Object> row)
    {
        StringBuilder sb = new StringBuilder();
        // --- If using first-row headers, use them for keeping columns in synch
        if (isFirstRowHeader())
        {
            boolean firstCol = true;
            for (String col : colNames)
            {
                if (firstCol)
                {
                    firstCol = false;
                }
                else
                {
                    sb.append(getColumnSeparator());
                }
                sb.append(row.get(col));
            }
        }
        else
        {
            // --- ...otherwise, just pump the values as they come from the contained List
            List<String> list = (List<String>) row.get(configuration.getParamValue(PARAM_ROW_CONTENT));
            boolean firstCol = true;
            for (String value : list)
            {
                if (firstCol)
                {
                    firstCol = false;
                }
                else
                {
                    sb.append(getColumnSeparator());
                }
                sb.append(value);
            }
        }

        return sb.toString();
    }
}
