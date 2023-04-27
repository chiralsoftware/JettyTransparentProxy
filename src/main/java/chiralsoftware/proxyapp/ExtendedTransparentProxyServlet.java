package chiralsoftware.proxyapp;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.proxy.ProxyServlet;

import java.net.URI;
import java.util.*;
import java.util.logging.Logger;

public class ExtendedTransparentProxyServlet extends ProxyServlet.Transparent {
    private static final Logger LOG = Logger.getLogger(InitializerServlet.class.getName());
    private String targetHost = "localhost";
    private String targetPort = "8888";
    private String targetScheme = "http";
    private boolean useBaseMode = false;
    private boolean rewriteMultipleHeaders = false;
    private boolean _preserveHost;
    private String _hostHeader;

    @Override
    protected String rewriteTarget(HttpServletRequest request) {
        if(useBaseMode) {
            return super.rewriteTarget(request);
        } else {
            String s = request.getRequestURL().toString();
            URI original = URI.create(s).normalize();
            if (s.contains("https")) {
                s = s.replaceAll("https", targetScheme);
            } else {
                s = s.replaceAll("http", targetScheme);
            }
            if (s.contains(":" + original.getPort())) {
                s = s.replace(":" + original.getPort(), "");
            }
            if (!targetPort.equals("80") && !targetPort.equals("443")) {
                s = s.replace(original.getHost(), targetHost + ":" + targetPort);
            } else {
                s = s.replace(original.getHost(), targetHost);
            }
            if (request.getQueryString() != null) {
                s = s + "?" + request.getQueryString();
            }
            return s;
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        useBaseMode = Boolean.parseBoolean(config.getInitParameter("useBaseMode"));
        rewriteMultipleHeaders = Boolean.parseBoolean(config.getInitParameter("rewriteMultipleHeaders"));
        _preserveHost = Boolean.parseBoolean(config.getInitParameter("preserveHost"));
        _hostHeader = config.getInitParameter("hostHeader");
        if(!useBaseMode) {
            String theTargetPort = config.getInitParameter("targetPort");
            if (theTargetPort != null) {
                targetPort = theTargetPort;
            }
            String theTargetHost = config.getInitParameter("targetHost");
            if (theTargetHost != null) {
                targetHost = theTargetHost;
            }
            String theTargetScheme = config.getInitParameter("targetScheme");
            if (theTargetScheme != null) {
                targetScheme = theTargetScheme;
            }
        }

        super.init(config);
    }


    /**
     * Overrides the base class to allow joining of multiple headers to one header,
     * to resolve issues when proxyin HTTP/2 traffic to a HTTP/1.1 target.
     * @param clientRequest input request
     * @param proxyRequest to proxy to
     */
    protected void copyRequestHeaders(HttpServletRequest clientRequest, Request proxyRequest)
    {
        if(!rewriteMultipleHeaders) {
            super.copyRequestHeaders(clientRequest,proxyRequest);
        } else {
            // First clear possibly existing headers, as we are going to copy those from the client request.
            HttpFields.Mutable newHeaders = HttpFields.build();

            Set<String> headersToRemove = findConnectionHeaders(clientRequest);

            for (Enumeration<String> headerNames = clientRequest.getHeaderNames(); headerNames.hasMoreElements(); ) {
                String headerName = headerNames.nextElement();
                String lowerHeaderName = headerName.toLowerCase(Locale.ENGLISH);

                if (HttpHeader.HOST.is(headerName) && !_preserveHost)
                    continue;

                // Remove hop-by-hop headers.
                if (HOP_HEADERS.contains(lowerHeaderName))
                    continue;
                if (headersToRemove != null && headersToRemove.contains(lowerHeaderName))
                    continue;

                //Modification : join multiple headers together
                ArrayList<String> list = Collections.list(clientRequest.getHeaders(headerName));
                if (list.size() > 1) {
                    String joined = String.join("; ", list);
                    newHeaders.add("Cookie", joined);
                } else {
                    for (Enumeration<String> headerValues = clientRequest.getHeaders(headerName); headerValues.hasMoreElements(); ) {
                        String headerValue = headerValues.nextElement();
                        if (headerValue != null)
                            newHeaders.add(headerName, headerValue);
                    }
                }
            }

            // Force the Host header if configured
            if (_hostHeader != null)
                newHeaders.add(HttpHeader.HOST, _hostHeader);

            proxyRequest.headers(headers -> headers.clear().add(newHeaders));
        }
    }
}
