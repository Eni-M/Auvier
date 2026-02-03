package com.auvier.controllers;

import com.auvier.dtos.CategoryDto;
import com.auvier.dtos.ProductDto;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.infrastructure.services.CategoryService;
import com.auvier.infrastructure.services.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class StoreController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model) {
        List<ProductDto> products = productService.findAll();
        List<ProductDto> featuredProducts = products.stream()
                .filter(ProductDto::isActive)
                .limit(4)
                .toList();
        model.addAttribute("featuredProducts", featuredProducts.isEmpty() ? null : featuredProducts);
        return "store/home";
    }

    @GetMapping("/shop")
    public String shop(@RequestParam(required = false) String category,
                       @RequestParam(required = false) String q,
                       Model model) {
        List<ProductDto> products = productService.findAll().stream()
                .filter(ProductDto::isActive)
                .toList();

        // Filter by category if provided
        if (category != null && !category.isEmpty()) {
            products = products.stream()
                    .filter(p -> p.getCategory() != null &&
                            category.equalsIgnoreCase(p.getCategory().getName()))
                    .toList();
        }

        // Filter by search query if provided
        if (q != null && !q.isEmpty()) {
            String searchLower = q.toLowerCase();
            products = products.stream()
                    .filter(p -> p.getName().toLowerCase().contains(searchLower) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(searchLower)))
                    .toList();
        }

        List<CategoryDto> categories = categoryService.findParentCategories();
        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchQuery", q);
        return "store/shop";
    }

    @GetMapping("/shop/product/{slug}")
    public String productDetail(@PathVariable String slug, Model model) {
        List<ProductDto> allProducts = productService.findAll();
        ProductDto product = allProducts.stream()
                .filter(p -> p.getSlug().equals(slug) && p.isActive())
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + slug));

        // Get related products from same category
        List<ProductDto> relatedProducts = allProducts.stream()
                .filter(p -> p.isActive() && !p.getSlug().equals(slug))
                .filter(p -> product.getCategory() != null && p.getCategory() != null &&
                        p.getCategory().getId().equals(product.getCategory().getId()))
                .limit(4)
                .toList();

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", relatedProducts);
        return "store/product";
    }

    @GetMapping("/product/{slug}")
    public String productDetailAlternate(@PathVariable String slug) {
        return "redirect:/shop/product/" + slug;
    }

    @GetMapping("/about")
    public String about() {
        return "store/about";
    }

    @GetMapping("/account")
    public String account() {
        return "redirect:/profile";
    }

    @GetMapping("/collections")
    public String collections(Model model) {
        List<CategoryDto> categories = categoryService.findParentCategories();
        model.addAttribute("categories", categories);
        return "store/collections";
    }

    @GetMapping("/cart")
    public String cart() {
        return "store/cart";
    }

    @GetMapping("/contact")
    public String contact() {
        return "store/contact";
    }

    @GetMapping("/faq")
    public String faq() {
        return "store/faq";
    }

    @GetMapping("/shipping")
    public String shipping() {
        return "store/shipping";
    }

    @GetMapping("/returns")
    public String returns() {
        return "redirect:/shipping";
    }

    @GetMapping("/size-guide")
    public String sizeGuide() {
        return "store/size-guide";
    }
}
