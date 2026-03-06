package com.nocountry.authservice.security;

import com.nocountry.authservice.service.outbox.UserRegisteredEventPayload;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class OAuth2AttributionCookieService {

    private static final int COOKIE_MAX_AGE_SECONDS = 10 * 60;

    private static final String GCLID = "cf_attr_gclid";
    private static final String FBCLID = "cf_attr_fbclid";
    private static final String FBP = "cf_attr_fbp";
    private static final String FBC = "cf_attr_fbc";
    private static final String UTM_SOURCE = "cf_attr_utm_source";
    private static final String UTM_CAMPAIGN = "cf_attr_utm_campaign";

    public void captureFromRequest(HttpServletRequest request, HttpServletResponse response) {
        writeCookie(response, GCLID, normalize(request.getParameter("gclid")), request.isSecure());
        writeCookie(response, FBCLID, normalize(request.getParameter("fbclid")), request.isSecure());
        writeCookie(response, FBP, normalize(request.getParameter("fbp")), request.isSecure());
        writeCookie(response, FBC, normalize(request.getParameter("fbc")), request.isSecure());
        writeCookie(response, UTM_SOURCE, normalize(request.getParameter("utm_source")), request.isSecure());
        writeCookie(response, UTM_CAMPAIGN, normalize(request.getParameter("utm_campaign")), request.isSecure());
    }

    public UserRegisteredEventPayload.Attribution readAttribution(HttpServletRequest request) {
        return new UserRegisteredEventPayload.Attribution(
                readCookie(request, GCLID),
                readCookie(request, FBCLID),
                readCookie(request, FBP),
                readCookie(request, FBC),
                readCookie(request, UTM_SOURCE),
                readCookie(request, UTM_CAMPAIGN)
        );
    }

    public void clear(HttpServletResponse response, boolean secure) {
        clearCookie(response, GCLID, secure);
        clearCookie(response, FBCLID, secure);
        clearCookie(response, FBP, secure);
        clearCookie(response, FBC, secure);
        clearCookie(response, UTM_SOURCE, secure);
        clearCookie(response, UTM_CAMPAIGN, secure);
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return normalize(cookie.getValue());
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String name, String value, boolean secure) {
        if (value == null) {
            clearCookie(response, name, secure);
            return;
        }
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
    }

    private void clearCookie(HttpServletResponse response, String name, boolean secure) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
