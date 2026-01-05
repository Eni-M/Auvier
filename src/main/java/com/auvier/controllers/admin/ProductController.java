package com.auvier.controllers.admin;

import com.auvier.dtos.CategoryDto;
import com.auvier.dtos.ProductDto;
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

    // This runs before every GetMapping/PostMapping
    @ModelAttribute("categories")
    public List<CategoryDto> populateCategories() {
        return categoryService.findAll();
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productDto", new ProductDto());
        return "products/new";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("productDto") ProductDto productDto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "products/new";

        productService.add(productDto);
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productDto") ProductDto productDto,
                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) return "products/edit";

        productService.modify(id, productDto);
        return "redirect:/admin/products";
    }
}
