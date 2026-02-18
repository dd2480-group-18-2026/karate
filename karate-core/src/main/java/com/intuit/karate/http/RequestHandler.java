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
        if (stripHostContextPath != null) {
            // It should handle potential foreseen prefix in the path
            // by removing it from the request path.
            if (request.getPath().startsWith(stripHostContextPath)) {
                request.setPath(request.getPath().substring(stripHostContextPath.length()));
            }
        }
        if (SLASH.equals(request.getPath())) {
            // Set path to the home page path if the path is "/"
            request.setPath(config.getHomePagePath());
        }
        //Until this point is about ensuring the path is properly set for further handling

        ServerContext context = contextFactory.apply(request);
        if (request.getResourceType() == null) { // can be set by context factory
            // It should be able to handle requests for static resources even
            // when no content type is specified in the request by inferring it
            // from the type of the requested resource.
            request.setResourceType(ResourceType.fromFileExtension(request.getPath()));
        }
        if (!context.isApi() && request.isHttpGetForStaticResource() && context.isHttpGetAllowed()) {
            if (request.getResourcePath() == null) { // can be set by context factory
                // It should be able to handle lack of resource path when static resource is
                // requested by instead using the request path
                request.setResourcePath(request.getPath()); // static resource
            }
            try {
                // If the the context is not an API context and the request is for a static resource
                // that static resource will be returned as long as GET requests are allowed by the
                // context.
                return response().buildStatic(request);
            } finally {
                if (logger.isDebugEnabled()) {
                    logger.debug("{} {} [{} ms]", request, 200, System.currentTimeMillis() - request.getStartTime());
                }
            }
        }
        Session session = context.getSession(); // can be pre-resolved by context-factory
        if (session == null && !context.isStateless()) {
            // It should get/create a session if there is none and the context is not stateless
            String sessionId = context.getSessionCookieValue();
            if (sessionId != null) {
                // It should get the existing session if it can find it using the sessionId
                session = sessionStore.get(sessionId);
                if (session != null && isExpired(session)) {
                    logger.debug("session expired: {}", session);
                    sessionStore.delete(sessionId);
                    session = null;
                }
            }
            if (session == null) {
                if (config.isUseGlobalSession()) {
                    // If it cannot find an existing session but can use the global session it should do so
                    session = ServerConfig.GLOBAL_SESSION;
                } else {
                    // If it cannot find an existing session or use the global one it must create a session
                    if (config.isAutoCreateSession()) {
                        // If it is allowed it should create a session automatically.
                        context.init();
                        session = context.getSession();
                        logger.debug("auto-created session: {} - {}", request, session);
                    } else if (config.getSigninPagePath().equals(request.getPath())
                            || config.getSignoutPagePath().equals(request.getPath())) {
                        // If the request is to sign in or out the session is temporary and therefore
                        // set to that.
                        session = Session.TEMPORARY;
                        logger.debug("auth flow: {}", request);
                    } else {
                        logger.warn("session not found: {}", request);
                        ResponseBuilder rb = response();
                        if (sessionId != null) {
                            rb.deleteSessionCookie(sessionId);
                        }
                        if (request.isAjax()) {
                            // Set HX-Redirect header with the signin url.
                            rb.ajaxRedirect(signInPath());
                        } else {
                            // Otherwise set the location header with that url.
                            rb.locationHeader(signInPath());
                        }
                        // If the session cannot be created automatically the requester and the path
                        // of the request is not to sign in or out, the requester is redirected to
                        // the signin path
                        return rb.buildWithStatus(302);
                    }
                }
            }
            context.setSession(session);
        }
        RequestCycle rc = RequestCycle.init(templateEngine, context);
        return rc.handle();
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
