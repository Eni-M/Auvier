package com.auvier.controllers.admin;

import com.auvier.dtos.CategoryDto;
import com.auvier.infrastructure.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        return "categories/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                         BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "categories/new";
        }

        categoryService.add(categoryDto);
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("categoryDto", categoryService.findOne(id));
        return "categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                         BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "categories/edit";
        }

        categoryService.modify(id, categoryDto);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        categoryService.remove(id);
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findOne(id));
        return "categories/view";
    }

}
