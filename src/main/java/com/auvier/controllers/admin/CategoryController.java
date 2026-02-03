package com.auvier.controllers.admin;

import com.auvier.dtos.CategoryDto;
import com.auvier.infrastructure.services.AdminActivityLogService;
import com.auvier.infrastructure.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final AdminActivityLogService activityLogService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        // Only show root categories (those without a parent) as potential parents
        List<CategoryDto> parentCategories = categoryService.findAll().stream()
                .filter(c -> c.getParentId() == null)
                .toList();
        model.addAttribute("parentCategories", parentCategories);
        return "admin/categories/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            List<CategoryDto> parentCategories = categoryService.findAll().stream()
                    .filter(c -> c.getParentId() == null)
                    .toList();
            model.addAttribute("parentCategories", parentCategories);
            return "admin/categories/new";
        }

        CategoryDto created = categoryService.add(categoryDto);
        activityLogService.log("CREATE", "Category", created.getId(), created.getName(), null);
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CategoryDto current = categoryService.findOne(id);
        model.addAttribute("categoryDto", current);
        // Only show root categories (excluding self) as potential parents
        List<CategoryDto> parentCategories = categoryService.findAll().stream()
                .filter(c -> c.getParentId() == null && !c.getId().equals(id))
                .toList();
        model.addAttribute("parentCategories", parentCategories);
        return "admin/categories/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryDto") CategoryDto categoryDto,
                         BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            List<CategoryDto> parentCategories = categoryService.findAll().stream()
                    .filter(c -> c.getParentId() == null && !c.getId().equals(id))
                    .toList();
            model.addAttribute("parentCategories", parentCategories);
            return "admin/categories/edit";
        }

        categoryService.modify(id, categoryDto);
        activityLogService.log("UPDATE", "Category", id, categoryDto.getName(), null);
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        CategoryDto category = categoryService.findOne(id);
        categoryService.remove(id);
        activityLogService.log("DELETE", "Category", id, category.getName(), null);
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        CategoryDto category = categoryService.findOne(id);
        model.addAttribute("category", category);

        // Get parent category name if exists
        if (category.getParentId() != null) {
            try {
                CategoryDto parent = categoryService.findOne(category.getParentId());
                model.addAttribute("parentCategory", parent);
            } catch (Exception e) {
                // Parent not found, ignore
            }
        }
        return "admin/categories/view";
    }
}
