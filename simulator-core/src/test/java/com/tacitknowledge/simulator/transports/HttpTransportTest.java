package com.tacitknowledge.simulator.transports;

import com.tacitknowledge.simulator.Configurable;
import com.tacitknowledge.simulator.Transport;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author galo
 */
public class HttpTransportTest
{
    private Transport transport;
    private Map<String, String> params;

    RouteBuilder builder;

    @Before
    public void setUp()
    {
        transport = new RestTransport();
        params = new HashMap<String, String>();

        builder = new RouteBuilder()
        {
            public void configure()
            {
            }
        };
    }

    @Test
    public void testGetUriWithoutParams()
    {
        assertEquals(TransportConstants.REST, transport.getType());

        // --- Try to get the URI
        try
        {
            transport.toUriString();
            fail("Transport should not work without required parameters");
        }
        catch (Exception e)
        {
            // --- That's ok
        }
    }

    @Test
    public void testGetSimplestInUri()
    {
        // --- Try to get this URI: jetty:http://localhost/mytestapp/myservices
        params.put(HttpTransport.PARAM_RESOURCE_URI, "/mytestapp/myservices");
        transport.setBoundAndParameters(Configurable.BOUND_IN, params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                uri.indexOf("jetty:http://0.0.0.0/mytestapp/myservices") > -1);
        }
        catch (Exception e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    @Test
    public void testGetFullInUri()
    {
        // --- Try to get this URI: jetty:http://localhost:8080/mytestapp/myservices
        params.put(HttpTransport.PARAM_PORT, "8080");
        params.put(HttpTransport.PARAM_RESOURCE_URI, "/mytestapp/myservices");
        transport.setParameters(params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                uri.indexOf("jetty:http://0.0.0.0:8080/mytestapp/myservices") > -1);
        }
        catch (Exception e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    @Test
    public void testGetOutUri()
    {
        // --- Try to get this URI: direct:end
        params.put(HttpTransport.PARAM_HTTP_OUT, "true");
        transport.setParameters(params);

        try
        {
            String uri = transport.toUriString();

            assertEquals("Returned uri isn't as expected: " + uri,
                "direct:end", uri);
        }
        catch (Exception e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    @Test
    public void testHttpInRoute()
    {
        params.put(HttpTransport.PARAM_PORT, "9696");
        params.put(HttpTransport.PARAM_RESOURCE_URI, "/mytestapp");
        transport.setParameters(params);

        Transport out_tr = new RestTransport();
        Map<String, String> pars = new HashMap<String, String>();
        pars.put(HttpTransport.PARAM_HTTP_OUT, "true");
        out_tr.setParameters(pars);

        try
        {
            CamelContext context = new DefaultCamelContext();
            context.start();

            System.out.println(transport.toUriString());

            RouteDefinition def = builder.from(transport.toUriString());
            def.bean(new Processor(){
                public void process(Exchange exchange)
                {
                    // just get the body as a string
                    String body = exchange.getIn().getBody(String.class);
                    System.out.println("\n\n\n\n\n\n\n\nbody:\n" + body + "\n\n\n\n\n\n\n\n\n\n\n");

                    HttpServletRequest req = exchange.getIn().getBody(HttpServletRequest.class);
                    assertNotNull(req);

                    System.out.println("\n\n\n\n\n\n\n\nrequest:\n" + req + "\n\n\n\n\n\n\n\n\n");
                    assertEquals("My Test System", req.getParameter("system_name"));

                    exchange.getOut().setBody("<html><body>We got pinged!</body></html>");
                }
            });
            def.to(out_tr.toUriString());

            context.addRoutes(builder);

            // --- Now send the HTTP request
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod("http://0.0.0.0:9696/mytestapp");


            method.addParameter("system_id", "1");
            method.addParameter("system_name", "My Test System");
            method.addParameter("system_description", "My test system for HTTP camel route");
            method.addParameter("system_language", "javascript");


            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                fail("Failed with status code " + statusCode + ": " + method.getStatusLine());
            }

            // Read the response body.
            byte[] responseBody = method.getResponseBody();

            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            System.out.println(new String(responseBody));
            assertEquals("<html><body>We got pinged!</body></html>", new String(responseBody));

            context.stop();

        } catch(Exception e)
        {
            e.printStackTrace();
            fail();
        }
    }
}
