package com.auvier.controllers.admin;

import com.auvier.dtos.ProductVariantDto;
import com.auvier.enums.Size;
import com.auvier.infrastructure.services.AdminActivityLogService;
import com.auvier.infrastructure.services.FileStorageService;
import com.auvier.infrastructure.services.ProductService;
import com.auvier.infrastructure.services.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class ProductVariantAdminController {

    private final ProductService productService;
    private final ProductVariantService productVariantService;
    private final FileStorageService fileStorageService;
    private final AdminActivityLogService activityLogService;

    // LIST: /admin/products/{productId}/variants
    @GetMapping("/{productId}/variants")
    public String list(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findOne(productId));
        model.addAttribute("variants", productVariantService.findAllByProductId(productId));
        return "admin/variants/list";
    }

    // CREATE FORM: /admin/products/{productId}/variants/new
    @GetMapping("/{productId}/variants/new")
    public String createForm(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.findOne(productId));
        ProductVariantDto dto = new ProductVariantDto();
        dto.setProductId(productId);
        dto.setActive(true);
        model.addAttribute("productVariantDto", dto);
        model.addAttribute("sizes", Size.values());
        return "admin/variants/new";
    }

    // CREATE POST: /admin/products/{productId}/variants/new
    @PostMapping("/{productId}/variants/new")
    public String create(@PathVariable Long productId,
                         @Valid @ModelAttribute("productVariantDto") ProductVariantDto dto,
                         BindingResult br,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model) {
        if (br.hasErrors()) {
            model.addAttribute("product", productService.findOne(productId));
            model.addAttribute("sizes", Size.values());
            return "admin/variants/new";
        }

        // Handle file upload
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(imageFile, "variants");
            dto.setImageUrl(imageUrl);
        }

        dto.setProductId(productId);
        ProductVariantDto created = productVariantService.add(dto);
        activityLogService.log("CREATE", "Variant", created.getId(), created.getSku(), "Product ID: " + productId);
        return "redirect:/admin/products/" + productId + "/variants";
    }

    // VIEW: /admin/products/variants/{id}/view
    @GetMapping("/variants/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        ProductVariantDto dto = productVariantService.findOne(id);
        model.addAttribute("variant", dto);
        model.addAttribute("product", productService.findOne(dto.getProductId()));
        return "admin/variants/view";
    }

    // EDIT FORM: /admin/products/variants/{id}/edit
    @GetMapping("/variants/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        ProductVariantDto dto = productVariantService.findOne(id);
        model.addAttribute("productVariantDto", dto);
        model.addAttribute("product", productService.findOne(dto.getProductId()));
        model.addAttribute("sizes", Size.values());
        return "admin/variants/edit";
    }

    // EDIT POST: /admin/products/variants/{id}/edit
    @PostMapping("/variants/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("productVariantDto") ProductVariantDto dto,
                       BindingResult br,
                       @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                       Model model) {
        ProductVariantDto existing = productVariantService.findOne(id);

        if (br.hasErrors()) {
            model.addAttribute("product", productService.findOne(existing.getProductId()));
            model.addAttribute("sizes", Size.values());
            return "admin/variants/edit";
        }

        // Handle file upload
        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old file if it was an uploaded file
            if (existing.getImageUrl() != null && existing.getImageUrl().startsWith("/uploads/")) {
                fileStorageService.deleteFile(existing.getImageUrl());
            }
            String imageUrl = fileStorageService.storeFile(imageFile, "variants");
            dto.setImageUrl(imageUrl);
        } else if (dto.getImageUrl() == null || dto.getImageUrl().isEmpty()) {
            // Keep existing URL if no new file and no URL provided
            dto.setImageUrl(existing.getImageUrl());
        }

        dto.setProductId(existing.getProductId()); // lock relation
        productVariantService.modify(id, dto);
        activityLogService.log("UPDATE", "Variant", id, dto.getSku(), "Product ID: " + existing.getProductId());
        return "redirect:/admin/products/" + existing.getProductId() + "/variants";
    }

    // DELETE: /admin/products/variants/{id}/delete
    @PostMapping("/variants/{id}/delete")
    public String delete(@PathVariable Long id) {
        ProductVariantDto existing = productVariantService.findOne(id);

        // Delete uploaded image if exists
        if (existing.getImageUrl() != null && existing.getImageUrl().startsWith("/uploads/")) {
            fileStorageService.deleteFile(existing.getImageUrl());
        }

        productVariantService.remove(id);
        activityLogService.log("DELETE", "Variant", id, existing.getSku(), "Product ID: " + existing.getProductId());
        return "redirect:/admin/products/" + existing.getProductId() + "/variants";
    }
}
