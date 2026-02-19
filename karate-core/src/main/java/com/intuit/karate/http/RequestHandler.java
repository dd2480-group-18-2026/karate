/*
 * The MIT License
 *
 * Copyright 2022 Karate Labs Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.http;

import com.intuit.karate.template.KarateTemplateEngine;
import com.intuit.karate.template.TemplateUtils;
import java.time.Instant;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class RequestHandler implements ServerHandler {

    private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);

    private static final String SLASH = "/";

    private final SessionStore sessionStore;
    private final KarateTemplateEngine templateEngine;
    private final ServerConfig config;
    private final Function<Request, ServerContext> contextFactory;
    private final String stripHostContextPath;

    public RequestHandler(ServerConfig config) {
        this.config = config;
        contextFactory = config.getContextFactory();
        templateEngine = TemplateUtils.forServer(config);
        sessionStore = config.getSessionStore();
        stripHostContextPath = config.isStripContextPathFromRequest() ? config.getHostContextPath() : null;
    }

    @Override
    public Response handle(Request request) {
        adjustPath(request);

        ServerContext context = contextFactory.apply(request);
        ensureResourceType(request);
        
        Response staticResponse = tryHandleStaticRequest(request, context);
        if (staticResponse != null) {
            return staticResponse;
        }

        resolveSessionIfRequired(request, context);
        
        if (context.getSession() == null) {
            return redirectToSignIn(request, context);
        }

        RequestCycle rc = RequestCycle.init(templateEngine, context);
        return rc.handle();
    }

    private void adjustPath(Request request) {
        if (stripHostContextPath != null) {
            if (request.getPath().startsWith(stripHostContextPath)) {
                request.setPath(request.getPath().substring(stripHostContextPath.length()));
            }
        }
        if (SLASH.equals(request.getPath())) {
            request.setPath(config.getHomePagePath());
        }
    }

    private void ensureResourceType(Request request) {
        if (request.getResourceType() == null) { // can be set by context factory
            request.setResourceType(ResourceType.fromFileExtension(request.getPath()));
        }
    } 

    private Response tryHandleStaticRequest(Request request, ServerContext context) {
        if (!context.isApi() && request.isHttpGetForStaticResource() && context.isHttpGetAllowed()) {
            if (request.getResourcePath() == null) { // can be set by context factory
                request.setResourcePath(request.getPath()); // static resource
            }
            try {
                return response().buildStatic(request);
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} {} [{} ms]", request, 200, System.currentTimeMillis() - request.getStartTime());
                }
            }
        }

        return null;
    }

    private void resolveSessionIfRequired(Request request, ServerContext context) {
        Session session = context.getSession(); // can be pre-resolved by context-factory
        if (session != null || context.isStateless()) {
            context.setSession(session);
            return;
        }

        Session retreivedSession = resolveSession(request, context);
        
        context.setSession(retreivedSession);
    }

    private Session resolveSession(Request request, ServerContext context) {
        String sessionId = context.getSessionCookieValue();
        
        Session session = loadValidSession(sessionId);

        if (session != null) {
            return session;
        }

        if (config.isUseGlobalSession()) {
            return ServerConfig.GLOBAL_SESSION;
        } 
        
        if (config.isAutoCreateSession()) {
            context.init();
            Session newSession = context.getSession();
            logger.debug("auto-created session: {} - {}", request, newSession);
            return newSession;
        } else if (isAuthFlow(request)) {
            logger.debug("auth flow: {}", request);
            return Session.TEMPORARY;
        }
        
        return null;
    }

    private Session loadValidSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        Session session = sessionStore.get(sessionId);
        if (session != null && isExpired(session)) {
            logger.debug("session expired: {}", session);
            sessionStore.delete(sessionId);
            session = null;
        }
        
        return session;
    }

    private boolean isAuthFlow(Request request) {
        return config.getSigninPagePath().equals(request.getPath())
            || config.getSignoutPagePath().equals(request.getPath());
    }

    private Response redirectToSignIn(Request request, ServerContext context) {
        logger.warn("session not found: {}", request);
        
        ResponseBuilder rb = response();
        String sessionId = context.getSessionCookieValue();
        
        if (sessionId != null) {
            rb.deleteSessionCookie(sessionId);
        }
        
        if (request.isAjax()) {
            rb.ajaxRedirect(signInPath());
        } else {
            rb.locationHeader(signInPath());
        }
        
        return rb.buildWithStatus(302);
    }

    private String signInPath() {
        String path = config.getSigninPagePath();
        String contextPath = config.getHostContextPath();
        return contextPath == null ? path : contextPath + path.substring(1);
    }

    private boolean isExpired(Session session) {
        int configExpirySeconds = config.getSessionExpirySeconds();
        if (configExpirySeconds == -1) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        long expires = session.getUpdated() + configExpirySeconds;
        if (now > expires) {
            return true;
        }
        session.setUpdated(now);
        session.setExpires(expires);
        return false;
    }

    private ResponseBuilder response() {
        return new ResponseBuilder(config, null);
    }

}
