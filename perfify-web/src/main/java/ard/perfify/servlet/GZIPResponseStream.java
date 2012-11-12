package ard.perfify.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * This is wrapper class that wraps {@link javax.servlet.http.HttpServletResponse#getOutputStream()} stream with
 * a {@link GZIPOutputStream} to compress response text contents.
 *
 * @version $Id: $
 */
public class GZIPResponseStream extends ServletOutputStream {

    /**
     * Call back when the first bytes is written.
     */
    public static interface StartWriteCallback {

        /**
         * method invoked when first byte is written.
         */
        void startWrite();
    }


    /**
     * gzip stream instance, this wraps the servlet output stream to compress text response contents
     */
    protected GZIPOutputStream gzipstream = null;

    /**
     * determines whether the servlet output stream is already closed or not.
     */
    protected boolean closed = false;

    /**
     * the response object that we want to wrap its output stream
     */
    protected HttpServletResponse response = null;

    /**
     * the response object output stream
     */
    protected ServletOutputStream output = null;

    /**
     * When -1 the eager buffer size is not supported, otherwise the value corresponds to the flush interval.
     */
    protected int eagerBufferSize = -1;

    /**
     * the currently buffered size
     */
    protected int currentBufferedSize = 0;

    /**
     * Determines whether the response headers be flushed immediately when the first bytes comes in.
     */
    private boolean responseHeadersImmediateFlush;

    /**
     * Determines whether initial flush was done.
     */
    private boolean doneInitialFlush = false;


    /**
     * Start write callback.
     */
    private StartWriteCallback callback;

    /**
     * Constructor.
     *
     * @param response the response object we want to wrap its output stream
     * @param eagerBufferSize the eager buffer size before flushing
     * @param responseHeadersImmediateFlush determines whether the response headers be flushed immediately when the first
     *        bytes comes in.
     *
     * @throws IOException on IO error
     */
    public GZIPResponseStream(HttpServletResponse response, int eagerBufferSize, boolean responseHeadersImmediateFlush) throws IOException {
        this.response = response;
        this.eagerBufferSize = eagerBufferSize;
        this.responseHeadersImmediateFlush = responseHeadersImmediateFlush;

        output = response.getOutputStream();
        gzipstream = new GZIPOutputStream(output);
    }

    /**
     * Sets the start write callback.
     *
     * @param callback the callback object.
     */
    public void setCallback(StartWriteCallback callback) {
        this.callback = callback;
    }

    /**
     * Close all open IO streams.
     *
     * @see javax.servlet.ServletOutputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            throw new IOException("This output stream has already been closed");
        }

        try {
            gzipstream.close();
        } finally {
            output.close();
        }

        closed = true;
    }

    /**
     * Flush the buffer when buffer is supported ad the increment is more than the allotted eager buffer size
     *
     * @param increment the
     * @throws IOException on IO error
     */
    public void applyEagerBufferFlush(int increment) throws IOException {
        if(responseHeadersImmediateFlush && !doneInitialFlush) {
            callback.startWrite();
            flush();
        }

        if(eagerBufferSize == -1) {
            return;
        }

        if(currentBufferedSize >= eagerBufferSize) {
            flush();
            currentBufferedSize = 0;
        }

        currentBufferedSize += increment;
    }

    /**
     * Delegates writing to the gzip stream wrapper.
     *
     * @see ServletOutputStream#write(byte[], int, int)
     */
    @Override
    public void flush() throws IOException {
        if (closed) {
            throw new IOException("Cannot flush a closed output stream");
        }

        doneInitialFlush = true;
        gzipstream.flush();
    }

    /**
     * Delegates writing to the gzip stream wrapper.
     *
     * @see ServletOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(int b) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        applyEagerBufferFlush(1);
        gzipstream.write((byte) b);
    }

    /**
     * Delegates writing to the gzip stream wrapper.
     *
     * @see ServletOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte b[]) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        applyEagerBufferFlush(b.length);
        gzipstream.write(b);
    }

    /**
     * Delegates writing to the gzip stream wrapper.
     *
     * @see ServletOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        if (closed) {
            throw new IOException("Cannot write to a closed output stream");
        }

        applyEagerBufferFlush(len - off);
        gzipstream.write(b, off, len);
    }

}
