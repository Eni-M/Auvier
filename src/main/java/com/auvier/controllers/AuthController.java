package com.auvier.controllers;


import com.auvier.dtos.user.UserRegistrationDto;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.entities.UserEntity;
import com.auvier.exception.DuplicateResourceException;
import com.auvier.infrastructure.services.UserService;
import com.auvier.mappers.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        return "auth/register";
    }


    @PostMapping("/register")
    public String registerUser(
            @Valid @ModelAttribute("user") UserRegistrationDto user,
            BindingResult result,
            RedirectAttributes redirectAttributes
    ) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(user);
        } catch (DuplicateResourceException ex) {
            result.reject(null, ex.getMessage());
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Registration successful! Please login."
        );

        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profile(Model model,
                          @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity userEntity = userService.findByUsername(userDetails.getUsername())
                .orElseThrow();
        UserSummaryDto userDto = userMapper.toSummaryDto(userEntity);
        model.addAttribute("user", userDto);
        return "auth/profile";
    }



    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        // 1. Clear Security Context (Removes roles/user from Spring Security)
        userService.logout();

        // 2. Invalidate the Session (Deletes the session cookie on the server)
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 3. Redirect to login page with the 'logout' parameter
        // This matches your @GetMapping("/login") logic!
        return "redirect:/login?logout";
    }

}

