package com.auvier.controllers.admin;

import com.auvier.dtos.CategoryDto;
import com.auvier.dtos.ProductDto;
import com.auvier.infrastructure.services.AdminActivityLogService;
import com.auvier.infrastructure.services.CategoryService;
import com.auvier.infrastructure.services.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AdminActivityLogService activityLogService;

    @ModelAttribute("categories")
    public List<CategoryDto> populateCategories() {
       return categoryService.findParentCategories();
    }

    @ModelAttribute("subCategories")
    public List<CategoryDto> populateSubCategories() {
       return categoryService.findChildCategories();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productDto", new ProductDto());
        return "admin/products/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("productDto") ProductDto productDto,
                         BindingResult bindingResult,
                         Model model) {
        // Handle empty category
        if (productDto.getCategory() != null && productDto.getCategory().getId() == null) {
            productDto.setCategory(null);
        }
        // Handle empty subCategory
        if (productDto.getSubCategory() != null && productDto.getSubCategory().getId() == null) {
            productDto.setSubCategory(null);
        }

        if (bindingResult.hasErrors()) {
            return "admin/products/new";
        }

        ProductDto created = productService.add(productDto);
        activityLogService.log("CREATE", "Product", created.getId(), created.getName(), null);
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findOne(id));
        return "admin/products/view";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("productDto", productService.findOne(id));
        return "admin/products/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productDto") ProductDto productDto,
                         BindingResult bindingResult,
                         Model model) {
        // Handle empty category
        if (productDto.getCategory() != null && productDto.getCategory().getId() == null) {
            productDto.setCategory(null);
        }
        // Handle empty subCategory
        if (productDto.getSubCategory() != null && productDto.getSubCategory().getId() == null) {
            productDto.setSubCategory(null);
        }

        if (bindingResult.hasErrors()) {
            return "admin/products/edit";
        }

        productService.modify(id, productDto);
        activityLogService.log("UPDATE", "Product", id, productDto.getName(), null);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        ProductDto product = productService.findOne(id);
        productService.remove(id);
        activityLogService.log("DELETE", "Product", id, product.getName(), null);
        return "redirect:/admin/products";
    }
}
