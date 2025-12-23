package de.othr.traintogether.controller;

import de.othr.traintogether.service.customExceptions.EmailAlreadyRegisteredException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
class GlobalDefaultExceptionHandler {
    public static final String DEFAULT_ERROR_VIEW = "error";

    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
        // If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it.
        if (AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
            throw e;
        }

        // If this is an API request, rethrow so REST/Security handlers can produce proper JSON status codes
        String uri = req.getRequestURI();
        String accept = req.getHeader("Accept");
        if (uri != null && uri.startsWith("/api/")) {
            throw e;
        }
        if (accept != null && accept.contains("application/json")) {
            throw e;
        }

        // Otherwise setup and send the user to a default error-view.
        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", e);
        mav.addObject("url", req.getRequestURL());
        mav.setViewName(DEFAULT_ERROR_VIEW);
        return mav;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public Object handleEmailConflict(EmailAlreadyRegisteredException ex, HttpServletRequest req) {
        ModelAndView mav = new ModelAndView("register");
        mav.addObject("registerError", ex.getMessage());
        return mav;
    }
}
