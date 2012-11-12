package ard.perfify.servlet;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * Base filter
 */
public abstract class BaseOncePerRequestFilter extends OncePerRequestFilter {
    /**
     * Checks if request accepts the named encoding.
     *
     * @param request the current request
     * @param name the header name
     * @return <code>true</code> if the users browser supports GZIP encoding, <code>false</code> otherwise.
     */
    protected boolean acceptsEncoding(final HttpServletRequest request, final String name) {
        return headerContains(request, "Accept-Encoding", name);
    }

    /**
     * Checks if request contains the header value.
     *
     * @param request the current request
     * @param header the header name
     * @param value the value or partial value
     * @return <code>true</code> if the header contains the value, <code>false</code> otherwise.
     */
    protected boolean headerContains(final HttpServletRequest request, final String header, final String value) {
        final Enumeration accepted = request.getHeaders(header);
        while (accepted.hasMoreElements()) {
            final String headerValue = (String) accepted.nextElement();
            if (headerValue.contains(value)) {
                return true;
            }
        }
        return false;
    }
}
