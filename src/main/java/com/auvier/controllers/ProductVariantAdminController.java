package com.auvier.controllers;


import com.auvier.dtos.ProductVariantDto;
import com.auvier.enums.Size;
import com.auvier.infrastructure.services.ProductService;
import com.auvier.infrastructure.services.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class ProductVariantAdminController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;

    @GetMapping("/products/{productId}/variants")
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findOne(productId));
        model.addAttribute("variants", productVariantService.findAllByProductId(productId));
        return "variants/list";
    }

    @GetMapping("/products/{productId}/variants/new")
    public String createForm(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findOne(productId));
        model.addAttribute("productVariantDto", new ProductVariantDto(null, productId, "", null, 0, "", null, true));
        model.addAttribute("sizes", Size.values());
        return "variants/new";
    }

    @PostMapping("/products/{productId}/variants/new")
    public String create(@PathVariable Long productId,
                         @Valid @ModelAttribute("productVariantDto") ProductVariantDto dto,
                         BindingResult br,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("product", productService.findOne(productId));
            model.addAttribute("sizes", Size.values());
            return "variants/new";
        }
        dto.setProductId(productId);
        productVariantService.add(dto);
        return "redirect:/admin/products/" + productId + "/variants";
    }

    @GetMapping("/variants/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductVariantDto dto = productVariantService.findOne(id);
        model.addAttribute("productVariantDto", dto);
        model.addAttribute("product", productService.findOne(dto.getProductId()));
        model.addAttribute("sizes", Size.values());
        return "variants/edit";
    }

    @PostMapping("/variants/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("productVariantDto") ProductVariantDto dto,
                       BindingResult br,
                       Model model) {
        ProductVariantDto existing = productVariantService.findOne(id);

        if (br.hasErrors()) {
            model.addAttribute("product", productService.findOne(existing.getProductId()));
            model.addAttribute("sizes", Size.values());
            return "variants/edit";
        }

        dto.setProductId(existing.getProductId()); // prevent changing product relation via form
        productVariantService.modify(id, dto);
        return "redirect:/admin/products/" + existing.getProductId() + "/variants";
    }

    @PostMapping("/variants/{id}/delete")
    public String delete(@PathVariable Long id) {
        ProductVariantDto existing = productVariantService.findOne(id);
        productVariantService.remove(id);
        return "redirect:/admin/products/" + existing.getProductId() + "/variants";
    }
}
