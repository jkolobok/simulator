package com.tacitknowledge.simulator.transports;

import com.tacitknowledge.simulator.Transport;
import com.tacitknowledge.simulator.TransportException;

import static com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder.parameter;
import static com.tacitknowledge.simulator.configuration.ParametersListBuilder.parameters;

import com.tacitknowledge.simulator.configuration.ParameterDefinitionBuilder;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transport implementation for File endpoints.
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class FileTransport extends BaseTransport implements Transport
{
    /**
     * Directory name parameter. The underlying directory path to poll from/write to. REQUIRED
     */
    public static final String PARAM_DIRECTORY_NAME = "directoryName";

    /**
     * File name parameter. Name of the file to listen for/write to. OPTIONAL.
     * Mutually exclusive to File Extension & Regex filter.
     * File Name has highest priority if more than one is passed.
     */
    public static final String PARAM_FILE_NAME = "fileName";

    /**
     * File extension parameter. Extension of files the transport will only poll from. OPTIONAL.
     * Mutually exclusive to File Name & Regex filter.
     * File extension has higher priority than Regex filter.
     */
    public static final String PARAM_FILE_EXTENSION = "fileExtension";

    /**
     * Regex filter parameter. Only file name matching the provided regex will be polled. OPTIONAL.
     * Mutually exclusive to File Name & Regex filter.
     * Will be applied only it's the only filter passed.
     */
    public static final String PARAM_REGEX_FILTER = "regexFilter";

    /**
     * Polling interval parameter. Milliseconds before the next poll of the directory. OPTIONAL
     * Defaults to 500 (Camel default)
     */
    public static final String PARAM_POLLING_INTERVAL = "pollingInterval";

    /**
     * Delete file parameter. Determines if file should be deleted after processing. OPTIONAL.
     * Defaults to false. For inbound transports only.
     */
    public static final String PARAM_DELETE_FILE = "deleteFile";

    /**
     * Logger for this class.
     */
    private static Logger logger = Logger.getLogger(FileTransport.class);

    /**
     * Transport parameters definition.
     */
    private List<ParameterDefinitionBuilder.ParameterDefinition> parametersList =
        parameters().
            add(parameter().
                name(PARAM_DIRECTORY_NAME).
                label("Directory Name").
                required(true)
            ).
            add(parameter().
                name(PARAM_FILE_NAME).
                label("File Name")
            ).
            add(parameter().
                name(PARAM_FILE_EXTENSION).
                label("File Extension the transport will only poll from " +
                    "(without dot, inbound only)")
            ).
            add(parameter().
                name(PARAM_REGEX_FILTER).
                label("Regex filter " +
                        "(will only be applied if neither " +
                        "file name nor extension filters are provided, inbound only)")
            ).
            add(parameter().
                name(PARAM_POLLING_INTERVAL).
                label("Milliseconds before the next poll").
                defaultValue("500")
            ).
            add(parameter().
                name(PARAM_DELETE_FILE).
                label("Delete file after simulation?").
                type(ParameterDefinitionBuilder.ParameterDefinition.TYPE_BOOLEAN)
            ).
        build();

    /**
     * If true, the processed file will be deleted.
     * Defaults to false.
     */
    private boolean deleteFile = false;

    /**
     * @inheritDoc
     */
    public FileTransport()
    {
        super(TransportConstants.FILE);
    }

    /**
     * Used only for inheriting Transports (e.g.: FTP transport)
     *
     * @param type @see #type
     */
    protected FileTransport(String type)
    {
        super(type);
    }

    /**
     * Used only for inheriting Transports
     *
     * @param type       @see #type
     * @param parameters @see #parameters
     */
    protected FileTransport(String type, Map<String, String> parameters)
    {
        super(type, parameters);
    }

    /**
     * Constructor
     *
     * @param parameters @see #parameters
     */
    public FileTransport(Map<String, String> parameters)
    {
        super(TransportConstants.FILE, parameters);
    }

    /**
     * @return @see #Transport.toUriString()
     * @throws TransportException If a required parameter is missing or not properly formatted.
     * @inheritDoc
     */
    public String toUriString() throws TransportException
    {
        validateParameters();

        StringBuilder sb = new StringBuilder("file://");

        // --- directory name
        sb.append(getParamValue(PARAM_DIRECTORY_NAME));
        sb.append("?");
        // --- Options. Take a 2 seconds delay before polling the directory after the route is
        // registered.
        //sb.append("?initialDelay=2000").append(AMP);

        // --- fileName, fileExtension & Regex filter should be mutually exclusive options
        if (getParamValue(PARAM_FILE_NAME) != null)
        {
            sb.append("fileName=").append(getParamValue(PARAM_FILE_NAME)).append(AMP);
        }
        else if (getParamValue(PARAM_FILE_EXTENSION) != null)
        {
            // --- File extension is used as a RegEx filter for transport routing
            sb.append("include=^.*(i)(.").append(getParamValue(PARAM_FILE_EXTENSION)).append(")");
            sb.append(AMP);
        }
        else if (getParamValue(PARAM_REGEX_FILTER) != null)
        {
            // --- Regex filter is the last filter to be applied, only if neither of the other 2
            // were provided.
            sb.append("include=").append(getParamValue(PARAM_REGEX_FILTER)).append(AMP);
        }

        if (getParamValue(PARAM_POLLING_INTERVAL) != null)
        {
            sb.append("initialDelay=").append(getParamValue(PARAM_POLLING_INTERVAL)).append(AMP);
            sb.append("delay=").append(getParamValue(PARAM_POLLING_INTERVAL)).append(AMP);
        }

        if (this.deleteFile)
        {
            sb.append("delete=true");
        }

        return sb.toString();
    }

    /**
     * @return List of Parameters for File Transport.
     * @inheritDoc
     */
    public List<List> getParametersList()
    {
        return getParametersDefinitionsAsList(parametersList);
    }

    /**
     * @throws TransportException If any required parameter is missing or incorrect
     * @inheritDoc
     */
    @Override
    void validateParameters() throws TransportException
    {
        if (getParamValue(PARAM_DELETE_FILE) != null)
        {
            this.deleteFile = Boolean.parseBoolean(getParamValue(PARAM_DELETE_FILE));
        }

        // ---
        if (getParamValue(PARAM_DIRECTORY_NAME) == null)
        {
            throw new TransportException("Directory Name parameter is required");
        }
    }

    /**
     * @return @see #deleteFile
     */
    protected boolean isDeleteFile()
    {
        return deleteFile;
    }

    /**
     * @param deleteFile @see #deleteFile 
     */
    protected void setDeleteFile(boolean deleteFile)
    {
        this.deleteFile = deleteFile;
    }
}
