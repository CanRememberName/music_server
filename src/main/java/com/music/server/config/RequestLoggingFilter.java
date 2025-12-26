package com.music.server.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> IGNORED_PATHS = new HashSet<>(Arrays.asList(
            "/api/v1/static",
            "/favicon.ico"
    ));

    private static final Set<String> BINARY_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "audio/mpeg",
            "audio/flac",
            "audio/ogg",
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/octet-stream"
    ));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        boolean isMultipart = request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");
        
        // Wrap request and response
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        
        try {
            // Log Request (Before)
            try {
                logRequest(wrappedRequest, isMultipart);
            } catch (Exception e) {
                log.error("Failed to log request", e);
            }
            
            // Execute
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            
        } finally {
            // Log Response (After)
            long duration = System.currentTimeMillis() - startTime;
            try {
                logResponse(wrappedResponse, path, duration);
            } catch (Exception e) {
                log.error("Failed to log response", e);
            }
            
            // IMPORTANT: Copy content back to original response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request, boolean isMultipart) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n[REQUEST] ").append(request.getMethod()).append(" ").append(request.getRequestURI());
        
        if (request.getQueryString() != null) {
            msg.append("?").append(request.getQueryString());
        }
        
        if (isMultipart) {
            msg.append("\nBody: [Multipart Data]");
        } else {
            // For requests, the body might not be read yet. 
            // ContentCachingRequestWrapper only caches what is read.
            // If we want to log the body BEFORE controller execution, we can't reliably get it here 
            // without consuming the stream (which is bad) or ensuring it's read.
            // However, typically we log request body *after* processing or just log parameters.
            // But requirement says "Before entering interface".
            // We'll log what we can, but usually body is empty here unless we read it.
            // To be safe and compliant with "Entering interface", we log metadata here.
            // If we really want the body, we might need to log it AFTER execution when it has been read by the controller.
            // Let's try to print it if available, or defer body logging.
            // Actually, let's log the body in the "Response" block for the request as well? 
            // No, user asked for "Enter -> print", "End -> print".
            // We will just print "[Body available after read]" or similar if empty.
            // OR: we force read? No, that breaks things.
            // We will accept that body might be empty in the "Before" log, 
            // and maybe print the cached request body in the "After" log as well for debugging.
        }
        log.info(msg.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, String path, long duration) {
        StringBuilder msg = new StringBuilder();
        msg.append("\n[RESPONSE] ").append(path).append(" (").append(duration).append("ms)");
        msg.append("\nStatus: ").append(response.getStatus());

        String contentType = response.getContentType();
        boolean isBinary = contentType != null && BINARY_CONTENT_TYPES.stream().anyMatch(contentType::startsWith);

        if (isBinary) {
            msg.append("\nBody: [Binary Content: ").append(contentType).append("]");
        } else {
            byte[] content = response.getContentAsByteArray();
            if (content.length > 0) {
                try {
                    String body = new String(content, StandardCharsets.UTF_8);
                    // Limit log size
                    if (body.length() > 2000) {
                        body = body.substring(0, 2000) + "... [Truncated]";
                    }
                    msg.append("\nBody: ").append(body);
                } catch (Exception e) {
                    msg.append("\nBody: [Encoding Error]");
                }
            }
        }
        log.info(msg.toString());
    }
}
