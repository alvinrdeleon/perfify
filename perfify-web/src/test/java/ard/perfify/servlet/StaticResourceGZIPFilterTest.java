package ard.perfify.servlet;

import org.junit.Before;
import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;

import static org.mockito.Mockito.*;

/**
 * Test for {@link StaticResourceGZIPFilter1} class.
 */
public class StaticResourceGZIPFilterTest {

    /**
     * Our test instance
     */
    private StaticResourceGZIPFilter1 filter;

    /**
     * mock request
     */
    private HttpServletRequest request;

    /**
     * mock response
     */
    private HttpServletResponse response;

    /**
     * mock chain
     */
    private FilterChain chain;

    /**
     * set up our test instance
     *
     * @throws Exception on error
     */
    @Before
    public void setUp() throws Exception {
        filter = new StaticResourceGZIPFilter1();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
    }

    /**
     * test for include uri
     *
     * @throws Exception on error
     */
    @Test
    public void testDoFilterIncludeURI() throws Exception {
        doReturn("/some_uri").when(request).getAttribute("javax.servlet.include.request_uri");

        filter.doFilterInternal(request, response, chain);

        verify(response, times(0)).setHeader("Content-Encoding", "gzip");
        verify(chain, times(1)).doFilter(request, response);
    }

    /**
     * test for not text content type response
     *
     * @throws Exception on error
     */
    @Test
    public void testDoFilterNotCSSorJSResource() throws Exception {
        doReturn("/test.zip").when(request).getRequestURI();

        filter.doFilterInternal(request, response, chain);

        verify(response, times(0)).setHeader("Content-Encoding", "gzip");
        verify(chain, times(1)).doFilter(request, response);
    }

    /**
     * test client which does not support gzip encoding
     *
     * @throws Exception on error
     */
    @Test
    public void testDoFilterClientGzipNotSupported() throws Exception {
        Enumeration enumeration = mock(Enumeration.class);

        doReturn(false).when(enumeration).hasMoreElements();
        doReturn(enumeration).when(request).getHeaders("Accept-Encoding");
        doReturn("/test.css").when(request).getRequestURI();

        filter.doFilterInternal(request, response, chain);

        verify(response, times(0)).setHeader("Content-Encoding", "gzip");
        verify(chain, times(1)).doFilter(request, response);
    }

    /**
     * test client that supports gzip encoding and that the request uri is a valid text content type resource
     *
     * @throws Exception on error
     */
    @Test
    public void testDoFilterSupported() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);

        doReturn(new SingleElementEnumeration("gzip,deflate")).when(request).getHeaders("Accept-Encoding");
        doReturn("/portal/test.css").when(request).getRequestURI();
        doReturn("/portal").when(request).getContextPath();
        doReturn(dispatcher).when(request).getRequestDispatcher(anyString());

        filter.doFilterInternal(request, response, chain);

        verify(response, times(1)).setHeader("Content-Encoding", "gzip");
        verify(request, times(1)).getRequestDispatcher(anyString());
        verify(dispatcher, times(1)).forward(request, response);
        verify(chain, times(0)).doFilter(request, response);
    }

    /**
     * test client that supports gzip encoding and that the request uri is a valid text content type resource using
     * wrapping of request not via request forward.
     *
     * @throws Exception on error
     */
    @Test
    public void testDoFilterSupportedWrapped() throws Exception {
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        filter.setWrapRequest(true);

        doReturn(new SingleElementEnumeration("gzip,deflate")).when(request).getHeaders("Accept-Encoding");
        doReturn("/portal/test.css").when(request).getRequestURI();
        doReturn("/portal").when(request).getContextPath();
        doReturn(dispatcher).when(request).getRequestDispatcher(anyString());

        filter.doFilterInternal(request, response, chain);

        verify(response, times(1)).setHeader("Content-Encoding", "gzip");
        verify(request, times(0)).getRequestDispatcher(anyString());
        verify(dispatcher, times(0)).forward(request, response);
        verify(chain, times(1)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    /**
     * An implementation of enumeration that only contains a single element.
     */
    private class SingleElementEnumeration implements Enumeration {

        /**
         * determines whether the nextElement method was invoked.
         */
        private boolean nextElementInvoked = false;

        /**
         * The element to be returned
         */
        private Object element;

        /**
         * Constructor.
         *
         * @param element the single element value
         */
        SingleElementEnumeration(Object element) {
            this.element = element;
        }

        /**
         * if {@link #nextElement()} is not yet invoked this will return <code>true<code>, <code>false</code> otherwise.
         *
         * @return whether there are more elements
         */
        public boolean hasMoreElements() {
            return !nextElementInvoked;
        }

        /**
         * always returns the single element value.
         *
         * @return the single element value
         */
        public Object nextElement() {
            nextElementInvoked = true;
            return element;
        }
    }
}
