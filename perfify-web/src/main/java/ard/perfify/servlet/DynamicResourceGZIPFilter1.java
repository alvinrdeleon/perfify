package ard.perfify.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * This is responsible for wrapping response object to write dynamic responses to GZIP stream. This will compress sent
 * response to the client.
 *
 */
public class DynamicResourceGZIPFilter1 extends BaseOncePerRequestFilter {

    /**
     * Logger
     */
    private static final Logger LOG = Logger.getLogger(DynamicResourceGZIPFilter1.class);

    /**
     * turn off GZIP.
     */
    public static final String GZIP_OFF_ATTRIBUTE = "gzipOff";

    /**
     * The eager buffer size flushing, is null then the whole dynamic page will be compressed
     * first before flushing.
     */
    private Integer eagerBufferFlushingSize;

    /**
     * Determines whether the response headers be flushed immediately when the first bytes comes in.
     */
    private boolean responseHeadersImmediateFlush;


    /**
     * The eager buffer size flushing
     *
     * @param eagerBufferFlushingSize the eager buffer flushing
     */
    public void setEagerBufferFlushingSize(Integer eagerBufferFlushingSize) {
        this.eagerBufferFlushingSize = eagerBufferFlushingSize;
    }

    /**
     * Setter for property {@link #responseHeadersImmediateFlush}.
     *
     * @param responseHeadersImmediateFlush determines whether the response headers be flushed immediately when the first
     * bytes comes in.
     */
    public void setResponseHeadersImmediateFlush(boolean responseHeadersImmediateFlush) {
        this.responseHeadersImmediateFlush = responseHeadersImmediateFlush;
    }

    /**
     * Wrap the response to use gzip output response instead of plain text. This will minimize response payload
     * at most 80%.
     *
     * @param request the current request
     * @param response the current response
     * @param chain the filter chain object
     * @throws IOException on IO error
     * @throws ServletException on unexpected servlet error
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if(acceptsGzipEncoding(request)) {
            if(LOG.isDebugEnabled()) {
                LOG.debug("Compressing response for '" + request.getRequestURI() + "' uri.");
            }

            GZIPResponseWrapper wrappedResponse = new GZIPResponseWrapper(request, response);

            try {
                chain.doFilter(request, wrappedResponse);
            } finally {
                wrappedResponse.finishResponse();
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Determine whether the user agent accepts GZIP encoding. This feature is part of HTTP1.1.
     * If a browser accepts GZIP encoding it will advertise this by including in its HTTP header:
     * <p/>
     * <code>
     * Accept-Encoding: gzip
     * </code>
     * <p/>
     * Requests which do not accept GZIP encoding fall into the following categories:
     * <ul>
     * <li>Old browsers, notably IE 5 on Macintosh.
     * <li>Internet Explorer through a proxy. By default HTTP1.1 is enabled but disabled when going
     * through a proxy. 90% of non gzip requests seen on the Internet are caused by this.
     * </ul>
     * As of September 2004, about 34% of Internet requests do not accept GZIP encoding.
     *
     * @param request the current request
     * @return true, if the User Agent request accepts GZIP encoding
     */
    protected boolean acceptsGzipEncoding(HttpServletRequest request) {
        return acceptsEncoding(request, "gzip");
    }

    /**
     * Response wrapper that return's GZIP enabled servlet stream.
     */
    public class GZIPResponseWrapper extends HttpServletResponseWrapper {

        /**
         * The original response object
         */
        protected HttpServletResponse origResponse = null;

        /**
         * the gzip servlet output stream
         */
        protected ServletOutputStream stream = null;

        /**
         * the print writer that wraps the gzup servlet output stream
         */
        protected PrintWriter writer = null;

        /**
         * the servlet request
         */
        protected HttpServletRequest request;

        /**
         * Constructor.
         *
         * @param request the request object.
         * @param response the response object to wrapped
         */
        public GZIPResponseWrapper(HttpServletRequest request, HttpServletResponse response) {
            super(response);

            this.request = request;
            origResponse = response;
        }

        /**
         * Creates a gzip response stream.
         *
         * @return the wrapped gzip servlet response stream
         * @throws IOException on IO error
         */
        public ServletOutputStream createOutputStream() throws IOException {
            int eagerFlushSize = -1;

            if(eagerBufferFlushingSize != null) {
                eagerFlushSize = eagerBufferFlushingSize;
            }

            GZIPResponseStream1 stream = new GZIPResponseStream1(origResponse, eagerFlushSize, responseHeadersImmediateFlush);

            // only set the header before the first byte is written to the gzip stream
            stream.setCallback(new GZIPResponseStream1.StartWriteCallback() {
                public void startWrite() {
                    setHeader("Content-Encoding", "gzip");
                }
            });

            return stream;
        }

        /**
         * Close the writer and stream quietly.
         *
         */
        public void finishResponse() {
            IOUtils.closeQuietly(writer);
            IOUtils.closeQuietly(stream);
        }

        /**
         * Flush the stream
         *
         * @see javax.servlet.http.HttpServletResponse#flushBuffer()
         */
        public void flushBuffer() throws IOException {
            stream.flush();
        }

        /**
         * Returns the servlet response. create it if not yet created, otherwise use the previously created instance.
         *
         * @see javax.servlet.http.HttpServletResponse#getOutputStream()
         */
        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if(isGZIPOff()) {
                return super.getOutputStream();
            }

            if (writer != null) {
                throw new IllegalStateException("getWriter() has already been called!");
            }

            if (stream == null) {
                stream = createOutputStream();
            }

            return stream;
        }

        /**
         * Determines whether gzip if turned off.
         *
         * @return {@code true} if gzip is turned off, {@code false} otherwise.
         */
        private boolean isGZIPOff() {
            Boolean gzipOff = (Boolean) request.getAttribute(GZIP_OFF_ATTRIBUTE);

            return gzipOff != null && gzipOff;
        }

        /**
         * Returns the response print writer. create it if not yet created, otherwise use the previously created instance.
         *
         * @see javax.servlet.http.HttpServletResponse#getWriter()
         */
        @Override
        public PrintWriter getWriter() throws IOException {
            if(isGZIPOff()) {
                return super.getWriter();
            }

            if (writer != null) {
                return (writer);
            }

            if (stream != null) {
                throw new IllegalStateException("getOutputStream() has already been called!");
            }

            stream = createOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(stream, "UTF-8"));

            return writer;
        }

        /**
         * Length is not predictable since it will be compressed, so just ignore it when set.
         *
         * @param length the content length
         * @see javax.servlet.http.HttpServletResponse#setContentLength(int)
         */
        @Override
        public void setContentLength(int length) {
        }
    }
}