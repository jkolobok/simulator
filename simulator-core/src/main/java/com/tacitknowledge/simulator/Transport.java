package com.tacitknowledge.simulator;

/**
 * Transport interface for different transports.
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public interface Transport
{
    /**
     * Returns the Transport type
     *
     * @return transport type
     */
    String getType();

    /**
     * Returns a valid String URI representation of this transport for Camel route creation e.g.:
     * file://path/to/file/directory , jms:queue/myqueue ,
     *
     * @return URI representation of the transport
     * @throws ConfigurableException If a required parameter is missing or not properly formatted.
     * @throws TransportException If any other error occurs
     */
    String toUriString() throws ConfigurableException, TransportException;

    void validateParameters() throws ConfigurableException;

    //todo - mws - currently ONLY supports unit testing for Files.  Remove when unit test uses class loader
    Configurable getConfigurable();
}
