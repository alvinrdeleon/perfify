package ard.perfify.servlet;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * This will forward to a pre-GZIP resource when the current request is GZIP encoding supported.
 */
public class StaticResourceGZIPFilter extends BaseOncePerRequestFilter {

    /**
     * Regular expression for css and js resource from a request uri
     */
    protected static final Pattern JS_CSS_FILE_TYPE_PATTERN = Pattern.compile(".*(css|js)", Pattern.CASE_INSENSITIVE);

    /**
     * determines whether to wrap the request instead of forward
     */
    protected boolean wrapRequest;


    /**
     * Determine whether the request will be wrapped instead of forward.
     *
     * @param wrapRequest <code>true<code> when request is wrapped, <code>false</code> otherwise.
     */
    public void setWrapRequest(boolean wrapRequest) {
        this.wrapRequest = wrapRequest;
    }

    /**
     * Do a forward to a pre GZIP resource when {@link #wrapRequest} is set to false, otherwise
     * wrap th request to ensure that will be forwarded to GZIP request when the client browser
     * supports it.
     *
     * @param request the current request
     * @param response the current response
     * @param chain the filter chain object
     * @throws IOException on IO error
     * @throws ServletException on unexpected servlet error
     */
    @Override
    public void doFilterInternal(final HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        if(!isIncluded(request) && isTextResource(request) && acceptsGzipEncoding(request)) {
            response.setHeader("Content-Encoding", "gzip");

            if(wrapRequest) {
                HttpServletRequest wrapper = new HttpServletRequestWrapper(request) {
                    @Override
                    public RequestDispatcher getRequestDispatcher(String s) {
                        return super.getRequestDispatcher("/gzip" + s);
                    }
                };

                chain.doFilter(wrapper, response);
            } else {
                String requestUri = request.getRequestURI();
                String localUri = requestUri.substring(request.getContextPath().length());

                request.getRequestDispatcher("/gzip" + localUri).forward(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * Determines whether the the current request requested is a Text resource.
     *
     * @param request the current request
     * @return <code>true</code> if the current resource requested is CSS or JS, <code>false</code> otherwise.
     */
    private boolean isTextResource(final HttpServletRequest request) {
        return JS_CSS_FILE_TYPE_PATTERN.matcher(request.getRequestURI()).find();
    }

    /**
     * Checks if the request uri is an include.
     * These cannot be GZip.
     *
     * @param request the current request
     * @return <code>true</code> if the current request is not an include, <code>false</code> otherwise.
     */
    private boolean isIncluded(final HttpServletRequest request) {
        return request.getAttribute("javax.servlet.include.request_uri") != null;
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
}
