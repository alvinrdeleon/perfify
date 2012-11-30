package ard.perfify.servlet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Test for {@link DynamicResourceGZIPFilter1} class.
 */
public class DynamicResourceGZIPFilterTest {

    /**
     * test filter instance
     */
    private DynamicResourceGZIPFilter1 filter;

    /**
     * mock response
     */
    private HttpServletResponse response;

    /**
     * mock request
     */
    private HttpServletRequest request;

    /**
     * mock filter chain
     */
    private FilterChain chain;

    /**
     * determines whether the header enum has more element
     */
    private boolean hasMoreElement;

    /**
     * the filter chain passed response
     */
    private HttpServletResponse filterChainResponse;

    /**
     * The response output as byte
     */
    private ByteArrayOutputStream responseOut;

    /**
     * the mock servlet output stream
     */
    private ServletOutputStream out;

    /**
     * Initialize test instance and mock objects.
     *
     * @throws Exception on error
     */
    @Before
    public void setUp() throws Exception {
        filter = new DynamicResourceGZIPFilter1();

        hasMoreElement = true;
        responseOut = new ByteArrayOutputStream();

        // mock instances
        response = mock(HttpServletResponse.class);
        request = mock(HttpServletRequest.class);
        out = mock(ServletOutputStream.class);
        chain = mock(FilterChain.class);

        // catch the filter chain response
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                filterChainResponse = (HttpServletResponse) invocationOnMock.getArguments()[1];
                return null;
            }
        }).when(chain).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

        // mock returns
        doReturn(out).when(response).getOutputStream();

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                responseOut.write((byte[]) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(out).write(any(byte[].class));

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                responseOut.write((Integer) invocationOnMock.getArguments()[0]);
                return null;
            }
        }).when(out).write(anyInt());

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                responseOut.write((byte[]) invocationOnMock.getArguments()[0],
                        (Integer) invocationOnMock.getArguments()[1], (Integer) invocationOnMock.getArguments()[2]);
                return null;
            }
        }).when(out).write(any(byte[].class), anyInt(), anyInt());

        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                responseOut.close();
                return null;
            }
        }).when(out).close();
    }

    /**
     * Mock the request to set the accept-encoding header.
     *
     * @param encoding the header accepted encoding
     */
    @SuppressWarnings("unchecked")
    protected void mockGzipHeaderEncodingRequest(final String encoding) {
        Enumeration<String> headerEnum = mock(Enumeration.class);

        Answer<Boolean> answer = new Answer<Boolean>() {
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                boolean value = hasMoreElement;
                hasMoreElement = !hasMoreElement;

                return value;
            }
        };

        doAnswer(answer).when(headerEnum).hasMoreElements();
        doReturn(encoding).when(headerEnum).nextElement();

        doReturn(headerEnum).when(request).getHeaders("Accept-Encoding");
    }

    /**
     * Returns the ungzip response
     *
     * @param size the size of gzip content
     * @return the ungzip response
     * @throws IOException on error
     */
    protected String getUnGzipResponse(int size) throws IOException {
        Reader in = new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(responseOut.toByteArray())));

        char ch[] = new char[size];
        in.read(ch);

        return new String(ch);
    }

    /**
     * test for none acceptable gzip encoding
     *
     * @throws java.io.IOException on error
     * @throws javax.servlet.ServletException on error
     */
    @Test
    public void testNotAcceptedGzipEncoding() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("text");

        filter.doFilterInternal(request, response, chain);

        // the response should not be wrapped
        // so the response passed should also be passed on the chain
        verify(chain, times(1)).doFilter(request, response);
        assertSame("filter chain response and filter response should be the same.", response, filterChainResponse);
    }


    /**
     * test for acceptable gzip encoding
     *
     * @throws java.io.IOException on error
     * @throws javax.servlet.ServletException on error
     */
    @Test
    public void testAcceptedGzipEncoding() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);
        assertTrue("the servlet output stream should be gzip response stream", GZIPResponseStream1.class.isInstance(filterChainResponse.getOutputStream()));
    }

    /**
     * Ensure that wrapping is only done on first call on same thread.
     *
     * @throws java.io.IOException on error
     * @throws javax.servlet.ServletException on error
     */
    @Test
    public void testMultipleCallOnSameThread() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        // second call
        filter.doFilterInternal(request, response, chain);

        // the response should not be wrapped
        // so the response passed should also be passed on the chain
        verify(chain, times(1)).doFilter(request, response);
        assertSame("filter chain response and filter response should be the same.", response, filterChainResponse);
    }

    /**
     * Ensure that ungzip response is correct.
     *
     * @throws IOException on error
     * @throws ServletException on error
     */
    @Test
    public void testSameUnGzipResponse() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.setEagerBufferFlushingSize(10);
        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        // some random expected response value
        final String expectedResponseValue = "hello world " + System.currentTimeMillis() + UUID.randomUUID().toString();

        PrintWriter writer = filterChainResponse.getWriter();
        writer.write(expectedResponseValue);
        writer.close();

        assertEquals("invalid unzip response", expectedResponseValue, getUnGzipResponse(expectedResponseValue.length()));
    }

    /**
     * Test eager buffer flushing
     *
     * @throws java.io.IOException on error
     * @throws javax.servlet.ServletException on error
     */
    @Test
    public void testEagerFlushing() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.setEagerBufferFlushingSize(20);
        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        ServletOutputStream servletOut = filterChainResponse.getOutputStream();
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(10).getBytes());
        servletOut.write(generateString(10).getBytes());
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(5).getBytes());
        servletOut.write(3);
        servletOut.close();

        // ensure that flush was triggered four times
        verify(out, times(4)).flush();
    }

    /**
     * Test eager buffer flushing and immediate response header flushing
     *
     * @throws java.io.IOException on error
     * @throws javax.servlet.ServletException on error
     */
    @Test
    public void testEagerFlushAndHeadersImmediateFlush() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.setResponseHeadersImmediateFlush(true);
        filter.setEagerBufferFlushingSize(20);
        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        ServletOutputStream servletOut = filterChainResponse.getOutputStream();
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(10).getBytes());
        servletOut.write(generateString(10).getBytes());
        servletOut.write(generateString(20).getBytes());
        servletOut.write(generateString(5).getBytes());
        servletOut.write(3);
        servletOut.close();

        // ensure that flush was triggered four times
        verify(out, times(5)).flush();
    }

    /**
     * Call to {@link DynamicResourceGZIPFilter1.GZIPResponseWrapper#setContentLength(int)} method should not be
     * delegated to actual response and just be ignored.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testContentLengthIgnored() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.setEagerBufferFlushingSize(20);
        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        // this should not be invoked
        verify(response, times(0)).setContentLength(anyInt());
    }

    /**
     * build some string with count characters
     *
     * @param count the number of characters for the generated string
     * @return the generated string
     */
    private String generateString(int count) {
        StringBuilder buf = new StringBuilder();

        for(int i = 0; i < count; i++) {
            buf.append(String.valueOf(i).charAt(0));
        }

        return buf.toString();
    }

    /**
     * Ensure that every call to {@link DynamicResourceGZIPFilter1.GZIPResponseWrapper#getOutputStream()} method
     * should result to the same instance.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testSameOutputStreamOnEveryCall() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        ServletOutputStream servletOut = filterChainResponse.getOutputStream();

        assertSame("Every call to #getOutputStream() should result to the same instance.", servletOut, filterChainResponse.getOutputStream());
        assertSame("Every call to #getOutputStream() should result to the same instance.", servletOut, filterChainResponse.getOutputStream());
        assertSame("Every call to #getOutputStream() should result to the same instance.", servletOut, filterChainResponse.getOutputStream());
    }

    /**
     * Ensure that every call to {@link DynamicResourceGZIPFilter1.GZIPResponseWrapper#getWriter()} method
     * should result to the same instance.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testSameWriterOnEveryCall() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        PrintWriter out = filterChainResponse.getWriter();

        assertSame("Every call to #getWriter() should result to the same instance.", out, filterChainResponse.getWriter());
        assertSame("Every call to #getWriter() should result to the same instance.", out, filterChainResponse.getWriter());
        assertSame("Every call to #getWriter() should result to the same instance.", out, filterChainResponse.getWriter());
    }

    /**
     * Test such that when the gzip stream was closed any invocation to stream operation results to exception, since
     * stream was already closed.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testOnCloseOnGzipStreamAndInvocationToAnyOperation() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        ServletOutputStream servletOut = filterChainResponse.getOutputStream();
        servletOut.close();

        try {
            servletOut.write(1);
            fail("should have thrown io exception since the stream was already closed.");
        } catch(IOException ignored) {}

        try {
            servletOut.write(new byte[] {1, 2, 3});
            fail("should have thrown io exception since the stream was already closed.");
        } catch(IOException ignored) {}

        try {
            servletOut.write(new byte[] {1, 2, 3}, 1, 2);
            fail("should have thrown io exception since the stream was already closed.");
        } catch(IOException ignored) {}
    }

    /**
     * Test the throws exception when the writer was retrieved and the output stream is retrieved as well.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testGetWriterOnly() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        filterChainResponse.getWriter();
        try {
            filterChainResponse.getOutputStream();
            fail("should have thrown exception since writer was already invoked, and output stream was invoked next.");
        } catch(IllegalStateException ignored) {}
    }

    /**
     * Test the throws exception when the output stream was retrieved and the writer is retrieved as well.
     *
     * @throws java.io.IOException on io error
     * @throws javax.servlet.ServletException on servlet error
     */
    @Test
    public void testGetOutputStreamOnly() throws IOException, ServletException {
        mockGzipHeaderEncodingRequest("gzip");

        filter.doFilterInternal(request, response, chain);

        // the response should be wrapped
        // so the response passed should not be passed on the chain
        verify(chain, times(0)).doFilter(request, response);
        assertNotSame("filter chain response and filter response should not be the same.", response, filterChainResponse);

        filterChainResponse.getOutputStream();
        try {
            filterChainResponse.getWriter();
            fail("should have thrown exception since output stream was already invoked, and writer was invoked next.");
        } catch(IllegalStateException ignored) {}
    }
}


