package com.tacitknowledge.simulator.formats;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.FormatAdapterException;
import com.tacitknowledge.simulator.SimulatorPojo;
import com.tacitknowledge.simulator.StructuredSimulatorPojo;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Implementation of the Adapter interface for the CSV format
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class CsvAdapter extends BaseAdapter implements Adapter<Object>
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
     * String describing what each row represents. REQUIRED if firstRowHeader is false.
     * This will be used as bean name during the simulation.
     * .e.g.: employee, order, product, etc.
     */
    public static final String PARAM_ROW_CONTENT = "rowContent";
    /**
     * First row header parameter name.
     */
    public static final String PARAM_FIRST_ROW_HEADER = "firstRowHeader";

    /**
     * Adapter parameters definition.
     */
    private List<List> parametersList = new ArrayList<List>()
    {
        {
            add(new ArrayList<String>()
            {
                {
                    add(PARAM_CSV_CONTENT);
                    add("CSV Contents (e.g. employees, orders, etc.)");
                    add("string");
                    add("required");
                }
            });

            add(new ArrayList<String>()
            {
                {
                    add(PARAM_ROW_CONTENT);
                    add("Row Contents (What each row represents. e.g. employee, order, etc.)");
                    add("string");
                    add("optional");
                }
            });

            add(new ArrayList<String>()
            {
                {
                    add(PARAM_FIRST_ROW_HEADER);
                    add("Is first row headers row? (If not, Row Contents is required)");
                    add("boolean");
                    add("optional   ");
                }
            });

            add(new ArrayList<String>()
            {
                {
                    add(PARAM_COLUMN_SEPARATOR);
                    add("Column Separator (defaults to comma ',')");
                    add("string");
                    add("optional");
                }
            });
        }
    };

    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(CsvAdapter.class);

    /**
     * Column value separator. Defaults to comma (,)
     */
    private String columnSeparator = ",";

    /**
     * If true, the first row in the CSV will be treated as column names. Defaults to true.
     * If so, each rowContent will be a map with attributes defined by the column names.
     * Otherwise, each rowContent's attributes will be represented as a List.
     */
    private boolean firstRowHeader = true;

    /**
     * List of column names of the CSV being processed.
     * Used only if firstRowHeader is true
     */
    private List<String> colNames;

    /**
     * @inheritDoc
     */
    public CsvAdapter()
    {
    }

    /**
     * @inheritDoc
     * @param parameters @see Adapter#parameters
     */
    public CsvAdapter(Map<String, String> parameters)
    {
        super(parameters);
    }

    protected SimulatorPojo createSimulatorPojo(String o)
    {
        SimulatorPojo pojo = new StructuredSimulatorPojo();

        // --- The SimulatorPojo's root will contain only one key (csvContent) with a List value
        List<Map> rowObjects = new ArrayList<Map>();

        // --- First, split the incoming data into lines
        Pattern p = Pattern.compile("$", Pattern.MULTILINE);
        String[] rows = p.split(o);

        // --- For all rows, new line characters will be remove before calling getRowAs methods

        // --- If the first row will be treated as column names, get the list of col names
        if (firstRowHeader)
        {
            colNames = getRowAsList(rows[0].trim());

            // --- Now, populate the pojos using the col names as attribute names
            for (int i = 1; i < rows.length; i++)
            {
                rowObjects.add(getRowAsMap(rows[i].trim()));
            }
        }
        else
        {
            // --- ...otherwise, populate the List with each row represented as a Map containing
            // a List of values in key rowContent
            for (String rowString : rows)
            {
                Map<String, List<String>> row = new HashMap<String, List<String>>();
                row.put(getParamValue(PARAM_ROW_CONTENT), getRowAsList(rowString));
                rowObjects.add(row);
            }
        }

        // --- Set the resulting list in the SimulatorPojo root
        pojo.getRoot().put(getParamValue(PARAM_CSV_CONTENT), rowObjects);
        return pojo;
    }


    protected String getString(SimulatorPojo simulatorPojo)
            throws FormatAdapterException
    {

        StringBuilder sb1 = new StringBuilder();
        // --- Only one entry in the root should exist
        if (simulatorPojo.getRoot().size() > 1)
        {
            String errorMsg = "SimulatorPojo's root should only one Entry for CSV adaptTo";
            logger.error(errorMsg);
            throw new FormatAdapterException(errorMsg);
        }

        // --- Before going further, set the instance variables
        // from the received parameters and validate them
        validateParameters();

        // --- Use the CsvContent parameter to get the only object, which should be a List
        List<Map<String, Object>> list =
                (List<Map<String, Object>>) simulatorPojo.getRoot().get(getParamValue(PARAM_CSV_CONTENT));

        // --- If using first row as headers, get the headers from the first row's keys
        if (this.firstRowHeader)
        {
            sb1.append(getHeadersFromKeys(list.get(0).keySet())).append(LINE_SEP);
        }

        // --- Iterate through each list item
        for (int i = 0; i < list.size(); i++)
        {
            sb1.append(getValuesFromMap(list.get(i)));

            if (i < (list.size() - 1))
            {
                sb1.append(LINE_SEP);
            }
        }

        return sb1.toString();
    }

    /**
     * @inheritDoc
     * @throws FormatAdapterException if any required parameter is missing
     */
    void validateParameters() throws FormatAdapterException
    {
        // --- Check that if firstRowHeader is false, that rowContent has been provided
        this.firstRowHeader =
                getParamValue(PARAM_FIRST_ROW_HEADER) == null
                        || Boolean.parseBoolean(getParamValue(PARAM_FIRST_ROW_HEADER));
        // --- Override the default column separator if it has been set
        if (getParamValue(PARAM_COLUMN_SEPARATOR) != null)
        {
            this.columnSeparator = getParamValue(PARAM_COLUMN_SEPARATOR);
        }

        // ---
        if (getParamValue(PARAM_CSV_CONTENT) == null)
        {
            throw new FormatAdapterException("CSV Content parameter is required.");
        }
        if (!firstRowHeader && getParamValue(PARAM_ROW_CONTENT) == null)
        {
            String errorMsg = "RowContent parameter is required if CSV has no headers.";
            logger.error(errorMsg);
            throw new FormatAdapterException(errorMsg);
        }
    }

    /**
     * Returns a Map representing the given row. colNames will be used as keys.
     *
     * @param row      The row values as a String, separated by #columnSeparator
     * @return The Map populated with the given column keys and row values
     */
    private Map<String, String> getRowAsMap(String row)
    {
        Map<String, String> rowMap = new HashMap<String, String>();

        String[] values = row.split(this.columnSeparator);
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
    private List<String> getRowAsList(String row)
    {
        List<String> rowList = new ArrayList<String>();

        String[] values = row.split(this.columnSeparator);
        for (String value : values)
        {
            // --- Make sure we remove leading and trailing spaces from the actual value
            rowList.add(value.trim());
        }

        return rowList;
    }

    /**
     * @param keySet The key set where to get the header names from
     * @return A String containing the key names, separated by #columnSeparator
     */
    private String getHeadersFromKeys(Set<String> keySet)
    {
        StringBuilder sb = new StringBuilder();
        colNames = new ArrayList<String>();

        boolean firstKey = true;
        for (String key : keySet)
        {
            // ---
            if (firstKey)
            {
                firstKey = false;
            }
            else
            {
                sb.append(this.columnSeparator);
            }
            colNames.add(key);
            sb.append(key);
        }

        return sb.toString();
    }

    /**
     * @param row The Map representing a CSV row
     * @return A String containing the row values, separated by #columnSeparator
     */
    private String getValuesFromMap(Map<String, Object> row)
    {
        StringBuilder sb = new StringBuilder();
        // --- If using first-row headers, use them for keeping columns in synch
        if (this.firstRowHeader)
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
                    sb.append(this.columnSeparator);
                }
                sb.append(row.get(col));
            }
        }
        else
        {
            // --- ...otherwise, just pump the values as they come from the contained List
            List<String> list = (List<String>) row.get(getParamValue(PARAM_ROW_CONTENT));
            boolean firstCol = true;
            for (String value : list)
            {
                if (firstCol)
                {
                    firstCol = false;
                }
                else
                {
                    sb.append(this.columnSeparator);
                }
                sb.append(value);
            }
        }

        return sb.toString();
    }

    /**
     * @inheritDoc
     * @return @see Adapter#getParametersList
     */
    public List<List> getParametersList()
    {
        return parametersList;
    }
}
