package com.tacitknowledge.simulator;

import java.util.List;

/**
 * The Simulator conversation as set up by the user.
 * Works as a wrapper arround Camel route definition for entry and exit endpoints.
 *
 * @author Jorge Galindo (jgalindo@tacitknowledge.com)
 */
public class Conversation
{
    /**
     * Conversation ID.
     */
    private Integer id;
    /**
     * Wrapper for inbound transport configuration
     */
    private Transport inboundTransport;
    /**
     * Wrapper for outbound transport configuration
     */
    private Transport outboundTransport;
    /**
     * Wrapper for inbound format adapter
     */
    private Adapter inboundAdapter;
    /**
     * Wrapper for outbound format adapter
     */
    private Adapter outboundAdapter;

    /**
     * List of configured scenarios for this conversation
     */
    private List<ConversationScenario> scenarios;

    /**
     *
     * @param id Conversation ID
     * @param inboundTransport Wrapper for inbound transport configuration
     * @param outboundTransport Wrapper for outbound transport configuration
     * @param inboundAdapter Wrapper for inbound format adapter
     * @param outboundAdapter Wrapper for outbound format adapter
     */
    public Conversation(Integer id, Transport inboundTransport, Transport outboundTransport, Adapter inboundAdapter, Adapter outboundAdapter)
    {
        this.id = id;
        this.inboundTransport = inboundTransport;
        this.outboundTransport = outboundTransport;
        this.inboundAdapter = inboundAdapter;
        this.outboundAdapter = outboundAdapter;
    }

    /**
     * Adds a new Scenario to this Conversation
     * @param language The scripting language to be used.
     * @param criteria
     * @param transformation
     * @return
     */
    public ConversationScenario addScenario(String language, String criteria, String transformation)
    {
        return null;
    }

    /**
     * Returns this conversation inbound transport
     * @return
     */
    public Transport getInboundTransport()
    {
        return inboundTransport;
    }

    /**
     * Returns this conversation outbound transport
     * @return
     */
    public Transport getOutboundTransport()
    {
        return outboundTransport;
    }

    /**
     * Retuns this conversation inbound format adapter
     * @return
     */
    public Adapter getInboundAdapter()
    {
        return inboundAdapter;
    }

    /**
     * Retuns this conversation outbound format adapter
     * @return
     */
    public Adapter getOutboundAdapter()
    {
        return outboundAdapter;
    }

    /**
     * Returns the current list of configured scenarios for this conversation
     * @return
     */
    public List<ConversationScenario> getScenarios()
    {
        return scenarios;
    }
}
