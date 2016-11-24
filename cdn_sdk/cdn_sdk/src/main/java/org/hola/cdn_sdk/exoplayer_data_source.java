package org.hola.cdn_sdk;
import com.google.android.exoplayer.upstream.DataSpec;
import com.google.android.exoplayer.upstream.HttpDataSource;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.exoplayer.C;
import com.google.android.exoplayer.upstream.TransferListener;
import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Predicate;
import com.google.android.exoplayer.util.Util;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
class exoplayer_data_source implements HttpDataSource {
    private static final int MAX_REDIRECTS = 20; // Same limit as okhttp.
    private static final String TAG = "exoplayer_data_source";
    private static final Pattern CONTENT_RANGE_HEADER =
            Pattern.compile("^bytes (\\d+)-(\\d+)/(\\d+)$");
    private static final AtomicReference<byte[]> skipBufferReference = new AtomicReference<>();
    private final boolean allowCrossProtocolRedirects;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final String userAgent;
    private final Predicate<String> contentTypePredicate;
    private final HashMap<String, String> requestProperties;
    private final TransferListener listener;
    private DataSpec dataSpec;
    private HttpURLConnection connection;
    private InputStream inputStream;
    private boolean opened;
    private long bytesToSkip;
    private long bytesToRead;
    private long bytesSkipped;
    private long bytesRead;
    private DataSpec m_orig_dataspec;

    public exoplayer_data_source(String userAgent, Predicate<String> contentTypePredicate,
                                 TransferListener listener, int connectTimeoutMillis, int readTimeoutMillis,
                                 boolean allowCrossProtocolRedirects) {
        this.userAgent = Assertions.checkNotEmpty(userAgent);
        this.contentTypePredicate = contentTypePredicate;
        this.listener = listener;
        this.requestProperties = new HashMap<>();
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
    }

    @Override
    public String getUri() {
        return m_orig_dataspec.uri.toString();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return connection == null ? null : connection.getHeaderFields();
    }

    @Override
    public void setRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (requestProperties) {
            requestProperties.put(name, value);
        }
    }

    @Override
    public void clearRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (requestProperties) {
            requestProperties.remove(name);
        }
    }

    @Override
    public void clearAllRequestProperties() {
        synchronized (requestProperties) {
            requestProperties.clear();
        }
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        this.m_orig_dataspec = dataSpec;
        if (!dataSpec.uri.getHost().equals("127.0.0.1"))
        {
            dataSpec = new DataSpec(
                    Uri.parse(service.mangle_request(dataSpec.uri)), dataSpec.postBody,
                    dataSpec.absoluteStreamPosition, dataSpec.position, dataSpec.length,
                    dataSpec.key, dataSpec.flags);
        }
        this.dataSpec = dataSpec;
        this.bytesRead = 0;
        this.bytesSkipped = 0;
        try {
            connection = makeConnection(dataSpec);
        } catch (IOException e) {
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), e,
                    dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        int responseCode;
        try {
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            closeConnectionQuietly();
            throw new HttpDataSourceException("Unable to connect to " + dataSpec.uri.toString(), e,
                    dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        // Check for a valid response code.
        if (responseCode < 200 || responseCode > 299) {
            Map<String, List<String>> headers = connection.getHeaderFields();
            closeConnectionQuietly();
            throw new InvalidResponseCodeException(responseCode, headers, dataSpec);
        }

        // Check for a valid content type.
        String contentType = connection.getContentType();
        if (contentTypePredicate != null && !contentTypePredicate.evaluate(contentType)) {
            closeConnectionQuietly();
            throw new InvalidContentTypeException(contentType, dataSpec);
        }

        // If we requested a range starting from a non-zero position and received a 200 rather than a
        // 206, then the server does not support partial requests. We'll need to manually skip to the
        // requested position.
        bytesToSkip = responseCode == 200 && dataSpec.position != 0 ? dataSpec.position : 0;

        // Determine the length of the data to be read, after skipping.
        if ((dataSpec.flags & DataSpec.FLAG_ALLOW_GZIP) == 0) {
            long contentLength = getContentLength(connection);
            bytesToRead = dataSpec.length != C.LENGTH_UNBOUNDED ? dataSpec.length
                    : contentLength != C.LENGTH_UNBOUNDED ? contentLength - bytesToSkip
                    : C.LENGTH_UNBOUNDED;
        } else {
            // Gzip is enabled. If the server opts to use gzip then the content length in the response
            // will be that of the compressed data, which isn't what we want. Furthermore, there isn't a
            // reliable way to determine whether the gzip was used or not. Always use the dataSpec length
            // in this case.
            bytesToRead = dataSpec.length;
        }

        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            closeConnectionQuietly();
            throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        opened = true;
        if (listener != null) {
            listener.onTransferStart();
        }

        return bytesToRead;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws HttpDataSourceException {
        try {
            skipInternal();
            return readInternal(buffer, offset, readLength);
        } catch (IOException e) {
            throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_READ);
        }
    }

    @Override
    public void close() throws HttpDataSourceException {
        try {
            if (inputStream != null) {
                Util.maybeTerminateInputStream(connection, bytesRemaining());
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new HttpDataSourceException(e, dataSpec, HttpDataSourceException.TYPE_CLOSE);
                }
            }
        } finally {
            inputStream = null;
            closeConnectionQuietly();
            if (opened) {
                opened = false;
                if (listener != null) {
                    listener.onTransferEnd();
                }
            }
        }
    }

    protected final long bytesRemaining() {
        return bytesToRead == C.LENGTH_UNBOUNDED ? bytesToRead : bytesToRead - bytesRead;
    }

    private HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        URL url = new URL(dataSpec.uri.toString());
        byte[] postBody = dataSpec.postBody;
        long position = dataSpec.position;
        long length = dataSpec.length;
        boolean allowGzip = (dataSpec.flags & DataSpec.FLAG_ALLOW_GZIP) != 0;
        if (!allowCrossProtocolRedirects) {
            // HttpURLConnection disallows cross-protocol redirects, but otherwise performs redirection
            // automatically. This is the behavior we want, so use it.
            HttpURLConnection connection = makeConnection(
                    url, postBody, position, length, allowGzip, true /* followRedirects */);
            return connection;
        }
        // We need to handle redirects ourselves to allow cross-protocol redirects.
        int redirectCount = 0;
        while (redirectCount++ <= MAX_REDIRECTS) {
            HttpURLConnection connection = makeConnection(
                    url, postBody, position, length, allowGzip, false /* followRedirects */);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MULT_CHOICE
                    || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                    || responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                    || responseCode == HttpURLConnection.HTTP_SEE_OTHER
                    || (postBody == null
                    && (responseCode == 307 /* HTTP_TEMP_REDIRECT */
                    || responseCode == 308 /* HTTP_PERM_REDIRECT */))) {
                // For 300, 301, 302, and 303 POST requests follow the redirect and are transformed into
                // GET requests. For 307 and 308 POST requests are not redirected.
                postBody = null;
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                url = handleRedirect(url, location);
            } else {
                return connection;
            }
        }
        // If we get here we've been redirected more times than are permitted.
        throw new NoRouteToHostException("Too many redirects: " + redirectCount);
    }

    private HttpURLConnection makeConnection(URL url, byte[] postBody, long position,
                                             long length, boolean allowGzip, boolean followRedirects) throws IOException {
        HttpURLConnection connection;
        if (url.getHost().equals("127.0.0.1"))
            connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        else
            connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        synchronized (requestProperties) {
            for (Map.Entry<String, String> property : requestProperties.entrySet()) {
                connection.setRequestProperty(property.getKey(), property.getValue());
            }
        }
        if (!(position == 0 && length == C.LENGTH_UNBOUNDED)) {
            String rangeRequest = "bytes=" + position + "-";
            if (length != C.LENGTH_UNBOUNDED) {
                rangeRequest += (position + length - 1);
            }
            connection.setRequestProperty("Range", rangeRequest);
        }
        connection.setRequestProperty("User-Agent", userAgent);
        if (!allowGzip) {
            connection.setRequestProperty("Accept-Encoding", "identity");
        }
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setDoOutput(postBody != null);
        if (postBody != null) {
            connection.setFixedLengthStreamingMode(postBody.length);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            os.write(postBody);
            os.close();
        } else {
            connection.connect();
        }
        return connection;
    }

    private static URL handleRedirect(URL originalUrl, String location) throws IOException {
        if (location == null) {
            throw new ProtocolException("Null location redirect");
        }
        // Form the new url.
        URL url = new URL(originalUrl, location);
        // Check that the protocol of the new url is supported.
        String protocol = url.getProtocol();
        if (!"https".equals(protocol) && !"http".equals(protocol)) {
            throw new ProtocolException("Unsupported protocol redirect: " + protocol);
        }
        return url;
    }

    private static long getContentLength(HttpURLConnection connection) {
        long contentLength = C.LENGTH_UNBOUNDED;
        String contentLengthHeader = connection.getHeaderField("Content-Length");
        if (!TextUtils.isEmpty(contentLengthHeader)) {
            try {
                contentLength = Long.parseLong(contentLengthHeader);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Unexpected Content-Length [" + contentLengthHeader + "]");
            }
        }
        String contentRangeHeader = connection.getHeaderField("Content-Range");
        if (!TextUtils.isEmpty(contentRangeHeader)) {
            Matcher matcher = CONTENT_RANGE_HEADER.matcher(contentRangeHeader);
            if (matcher.find()) {
                try {
                    long contentLengthFromRange =
                            Long.parseLong(matcher.group(2)) - Long.parseLong(matcher.group(1)) + 1;
                    if (contentLength < 0) {
                        // Some proxy servers strip the Content-Length header. Fall back to the length
                        // calculated here in this case.
                        contentLength = contentLengthFromRange;
                    } else if (contentLength != contentLengthFromRange) {
                        // If there is a discrepancy between the Content-Length and Content-Range headers,
                        // assume the one with the larger value is correct. We have seen cases where carrier
                        // change one of them to reduce the size of a request, but it is unlikely anybody would
                        // increase it.
                        Log.w(TAG, "Inconsistent headers [" + contentLengthHeader + "] [" + contentRangeHeader
                                + "]");
                        contentLength = Math.max(contentLength, contentLengthFromRange);
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Unexpected Content-Range [" + contentRangeHeader + "]");
                }
            }
        }
        return contentLength;
    }

    private void skipInternal() throws IOException {
        if (bytesSkipped == bytesToSkip) {
            return;
        }
        // Acquire the shared skip buffer.
        byte[] skipBuffer = skipBufferReference.getAndSet(null);
        if (skipBuffer == null) {
            skipBuffer = new byte[4096];
        }
        while (bytesSkipped != bytesToSkip) {
            int readLength = (int) Math.min(bytesToSkip - bytesSkipped, skipBuffer.length);
            int read = inputStream.read(skipBuffer, 0, readLength);
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            if (read == -1) {
                throw new EOFException();
            }
            bytesSkipped += read;
            if (listener != null) {
                listener.onBytesTransferred(read);
            }
        }
        // Release the shared skip buffer.
        skipBufferReference.set(skipBuffer);
    }

    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        readLength = bytesToRead == C.LENGTH_UNBOUNDED ? readLength
                : (int) Math.min(readLength, bytesToRead - bytesRead);
        if (readLength == 0) {
            // We've read all of the requested data.
            return C.RESULT_END_OF_INPUT;
        }
        int read = inputStream.read(buffer, offset, readLength);
        if (read == -1) {
            if (bytesToRead != C.LENGTH_UNBOUNDED && bytesToRead != bytesRead) {
                // The server closed the connection having not sent sufficient data.
                throw new EOFException();
            }
            return C.RESULT_END_OF_INPUT;
        }
        bytesRead += read;
        if (listener != null) {
            listener.onBytesTransferred(read);
        }
        return read;
    }

    private void closeConnectionQuietly() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while disconnecting", e);
            }
            connection = null;
        }
    }
}
