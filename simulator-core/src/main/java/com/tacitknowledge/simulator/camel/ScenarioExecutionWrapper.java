package com.tacitknowledge.simulator.camel;

import com.tacitknowledge.simulator.Adapter;
import com.tacitknowledge.simulator.Conversation;
import com.tacitknowledge.simulator.Scenario;
import com.tacitknowledge.simulator.configuration.EventDispatcher;
import com.tacitknowledge.simulator.configuration.SimulatorEventType;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Wrapper for the scenario execution.
 *
 * @author Nikita Belenkiy (nbelenkiy@tacitknowledge.com)
 * @author Alexandru Dereveanco (adereveanco@tacitknowledge.com)
 */
public class ScenarioExecutionWrapper
{
    /**
     * Logger for the ScenarioExecutionWrapper class.
     */
    private static Logger logger  = LoggerFactory.getLogger(ScenarioExecutionWrapper.class);
    
    /**
     * inAdapter adapter which will take the information from the exchange
     * and adapt it to SimulatorPojo for scenario execution. *
     */
    private Adapter inAdapter;
    /**
     * outAdapter adapter which will take the information from the SimulatorPojo
     * and adapt it to the desired format. *
     */
    private Adapter outAdapter;

    /**
     * Container of the scenarios to run
     */
    private Collection<Scenario> scenarios;

    /**
     * The conversation related to this execution.
     */
    private Conversation conversation;

    /**
     * Constructor for the ScenarioExecutionWrapper.
     *
     * @param conv Conversation
     */
    public ScenarioExecutionWrapper(final Conversation conv)
    {
        this.conversation = conv;

        this.inAdapter = conv.getInboundAdapter();
        this.outAdapter = conv.getOutboundAdapter();
        this.scenarios = conv.getScenarios().values();

        if (this.inAdapter == null || this.outAdapter == null || this.scenarios == null)
        {
            logger.warn("Something is probably wrong: One of the adapters or scenarios "
                    + "list is null");
        }
    }

    /**
     * Processes the body object received from the previous step of the route. Iterates through
     * all of the provided scenarios and return the processed result of the first matched scenario.
     *
     * @param exchange body.getIn().getBody() contains data from inbound adapter
     * @return
     * @throws Exception in case of error.
     */
    public void process(final Exchange exchange) throws Exception
    {
        EventDispatcher.getInstance().dispatchEvent(SimulatorEventType.NEW_MESSAGE,
                this.conversation, exchange);
        /**
         * Beans needed for the script executions service to run the simulation against *
         */
        Map<String, Object> scriptExecutionBeans = inAdapter.generateBeans(exchange);

        Object result = null;
        // here we are looking for first matching scenario and ignore all other scenarios
        for (Scenario scenario : scenarios)
        {
            synchronized (scenario)
            {
                logger.info("Evaluating scenario : {}", scenario.toString());

                boolean matchesCondition = scenario.matchesCondition(scriptExecutionBeans);
                logger.info("matches condition: {}", matchesCondition);
                if (matchesCondition)
                {
                    EventDispatcher.getInstance().dispatchEvent(SimulatorEventType.SCENARIO_MATCHED,
                            this.conversation, exchange);

                    logger.info("Executing the transformation script.");
                    result = scenario.executeTransformation(scriptExecutionBeans);
                    EventDispatcher.getInstance().dispatchEvent(SimulatorEventType.RESPONSE_BUILT,
                            this.conversation, exchange);
                    break;
                }
            }
        }
        exchange.getOut().setBody(outAdapter.adaptTo(result, exchange));
    }


}
