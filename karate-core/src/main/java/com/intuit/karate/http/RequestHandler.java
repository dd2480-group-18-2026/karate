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

import com.intuit.karate.BranchCoverageInfo;

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
			branchFlags[0][0] = true;
            if (request.getPath().startsWith(stripHostContextPath)) {
				branchFlags[0][2] = true;
                request.setPath(request.getPath().substring(stripHostContextPath.length()));
            }
			else { branchFlags[0][3] = true; }
        }
		else { branchFlags[0][1] = true; }
        if (SLASH.equals(request.getPath())) {
			branchFlags[0][4] = true;
            request.setPath(config.getHomePagePath());
        } 
		else { branchFlags[0][5] = true; }
        ServerContext context = contextFactory.apply(request);
        if (request.getResourceType() == null) { // can be set by context factory
			branchFlags[0][6] = true;
            request.setResourceType(ResourceType.fromFileExtension(request.getPath()));
        }
		else { branchFlags[0][7] = true; }
        if (!context.isApi() && request.isHttpGetForStaticResource() && context.isHttpGetAllowed()) {
			branchFlags[0][8] = true;
            if (request.getResourcePath() == null) { // can be set by context factory
				branchFlags[0][10] = true;
                request.setResourcePath(request.getPath()); // static resource
            } 
			else { branchFlags[0][11] = true; }
            try {
                return response().buildStatic(request);
            } finally {
                if (logger.isDebugEnabled()) {
					branchFlags[0][12] = true;
                    logger.debug("{} {} [{} ms]", request, 200, System.currentTimeMillis() - request.getStartTime());
                }
				else {branchFlags[0][13] = true;}
            }
        }
		else { branchFlags[0][9] = true;}
        Session session = context.getSession(); // can be pre-resolved by context-factory
        if (session == null && !context.isStateless()) {
			branchFlags[0][14] = true;
            String sessionId = context.getSessionCookieValue();
            if (sessionId != null) {
				branchFlags[0][16] = true;
                session = sessionStore.get(sessionId);
                if (session != null && isExpired(session)) {
					branchFlags[0][18] = true;
                    logger.debug("session expired: {}", session);
                    sessionStore.delete(sessionId);
                    session = null;
                }
				else {branchFlags[0][19] = true;}
            }
			else { branchFlags[0][17] = true; }
            if (session == null) {
				branchFlags[0][20] = true;
                if (config.isUseGlobalSession()) {
					branchFlags[0][22] = true;
                    session = ServerConfig.GLOBAL_SESSION;
                } else {
					branchFlags[0][23] = true;
                    if (config.isAutoCreateSession()) {
						branchFlags[0][24] = true;
                        context.init();
                        session = context.getSession();
                        logger.debug("auto-created session: {} - {}", request, session);
                    } else if (config.getSigninPagePath().equals(request.getPath())
                            || config.getSignoutPagePath().equals(request.getPath())) {
						branchFlags[0][25] = true;
                        session = Session.TEMPORARY;
                        logger.debug("auth flow: {}", request);
                    } else {
						branchFlags[0][26] = true;
                        logger.warn("session not found: {}", request);
                        ResponseBuilder rb = response();
                        if (sessionId != null) {
							branchFlags[0][27] = true;
                            rb.deleteSessionCookie(sessionId);
                        }
						else { branchFlags[0][28] = true; }
                        if (request.isAjax()) {
							branchFlags[0][29] = true;
                            rb.ajaxRedirect(signInPath());
                        } else {
							branchFlags[0][30] = true;
                            rb.locationHeader(signInPath());
                        }
                        return rb.buildWithStatus(302);
                    }
                }
            }
			else { branchFlags[0][21] = true; }
            context.setSession(session);
        }
		else { branchFlags[0][15] = true; }
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
