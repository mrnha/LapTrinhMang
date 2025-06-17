package com.chat.messmini.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        log.error("Error occurred: status={}, message={}, exception={}",
            status, message, exception != null ? exception.toString() : "No exception");

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());
            model.addAttribute("statusCode", statusCode);
            model.addAttribute("statusText", HttpStatus.valueOf(statusCode).getReasonPhrase());
        } else {
            model.addAttribute("statusCode", 500);
            model.addAttribute("statusText", "Internal Server Error");
        }

        model.addAttribute("errorMessage", message != null ? message : "An unexpected error occurred");
        model.addAttribute("exception", exception != null ? exception.toString() : "No exception details available");

        return "error";
    }
}