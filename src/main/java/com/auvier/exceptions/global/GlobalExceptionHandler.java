package com.auvier.exceptions.global;

import com.auvier.exceptions.ConflictException;
import com.auvier.exceptions.NotFoundException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Handles ProductNotFoundException, CategoryNotFoundException, etc.
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFound(NotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", 404);
        return "errors/error-page"; // Points to src/main/resources/templates/errors/error-page.html
    }

    // Handles Slug conflicts or ID conflicts
    @ExceptionHandler(ConflictException.class)
    public String handleConflict(ConflictException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        model.addAttribute("status", 409);
        return "errors/error-page";
    }
}
