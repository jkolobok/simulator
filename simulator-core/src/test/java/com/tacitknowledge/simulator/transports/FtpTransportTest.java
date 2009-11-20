package com.tacitknowledge.simulator.transports;

import com.tacitknowledge.simulator.Transport;
import com.tacitknowledge.simulator.TransportException;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author galo
 */
public class FtpTransportTest extends TestCase
{
    private Map<String, String> params;

    public void setUp()
    {
        params = new HashMap<String, String>();
    }

    public void testGetUriWithoutParams()
    {
        Transport transport = new FtpTransport();

        assertEquals("ftp", transport.getType());

        // --- Try to get the URI
        try
        {
            transport.toUriString();
            fail("Transport should not work without required parameters");
        } catch (TransportException e)
        {
            // --- That's ok
        }
    }

    public void testGetSimplestUri()
    {
        // --- Try to get this URI: ftp://127.0.0.1
        params.put(FtpTransport.PARAM_HOST, "127.0.0.1");
        Transport transport = new FtpTransport(params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                    uri.indexOf("ftp://127.0.0.1") > -1);
        } catch (TransportException e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    public void testGetSftpUri()
    {
        // --- Try to get this URI: sftp://127.0.0.1
        params.put(FtpTransport.PARAM_HOST, "127.0.0.1");
        params.put(FtpTransport.PARAM_SFTP, "true");
        Transport transport = new FtpTransport(params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                    uri.indexOf("sftp://127.0.0.1") > -1);
        } catch (TransportException e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    public void testGetUriWithUserPasswordAndDirectory()
    {
        // --- Try to get this URI: ftp://meandmyself@127.0.0.1:2121/inbox?password=secret
        params.put(FtpTransport.PARAM_HOST, "127.0.0.1");
        params.put(FtpTransport.PARAM_PORT, "2121");
        params.put(FtpTransport.PARAM_USERNAME, "meandmyself");
        params.put(FtpTransport.PARAM_PASSWORD, "secret");
        params.put(FtpTransport.PARAM_DIRECTORY_NAME, "inbox");

        Transport transport = new FtpTransport(params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                    uri.indexOf("ftp://meandmyself@127.0.0.1:2121/inbox?password=secret") > -1);
        } catch (TransportException e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }

    public void testGetUriWithFileOptionsAndBinary()
    {
        // --- Try to get this URI: ftp://127.0.0.1/inbox?include=^.*(i)(.csv)&binary=true
        params.put(FtpTransport.PARAM_HOST, "127.0.0.1");
        params.put(FtpTransport.PARAM_DIRECTORY_NAME, "inbox");
        params.put(FtpTransport.PARAM_FILE_EXTENSION, "csv");
        params.put(FtpTransport.PARAM_BINARY, "true");

        Transport transport = new FtpTransport(params);

        try
        {
            String uri = transport.toUriString();

            assertTrue("Returned uri isn't as expected: " + uri,
                    uri.indexOf("ftp://127.0.0.1/inbox?include=^.*(i)(.csv)&binary=true") > -1);
        } catch (TransportException e)
        {
            fail("Shouldn't be getting an exception here: " + e.getMessage());
        }
    }
}