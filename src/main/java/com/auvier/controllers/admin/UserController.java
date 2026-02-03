package com.auvier.controllers.admin;

import com.auvier.dtos.user.UserRegistrationDto;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.infrastructure.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAll());
        model.addAttribute("canDelete", userService.isDeleteAllowed());
        return "admin/users/list";
    }

    @GetMapping("/{id}/edit")
    public String editUser(@PathVariable Long id, Model model) {
        UserSummaryDto user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        model.addAttribute("userDto", user);
        return "admin/users/edit";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id, @ModelAttribute("userDto") UserRegistrationDto dto, RedirectAttributes redirectAttributes) {
        userService.updateUser(id, dto);
        redirectAttributes.addFlashAttribute("success", "User updated successfully.");
        return "redirect:/admin/users"; // Redirect to user listing
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.deleteById(id);
            ra.addFlashAttribute("success", "User deleted successfully.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }


    @GetMapping("/{id}/view")
    public String viewUser(@PathVariable Long id, Model model) {
        UserSummaryDto user = userService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        model.addAttribute("userDto", user);
        model.addAttribute("canDelete", userService.isDeleteAllowed());
        return "admin/users/view";
    }
}