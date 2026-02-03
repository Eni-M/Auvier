package com.auvier.infrastructure.services.impl;

import com.auvier.entities.catalog.ProductVariantEntity;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.infrastructure.services.InventoryService;
import com.auvier.repositories.ProductVariantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository variantRepository;

    @Override
    public boolean hasStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);
        return variant.getStock() >= quantity;
    }

    @Override
    public void validateStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);

        if (!variant.isActive()) {
            throw new IllegalStateException(
                    "Product variant '" + variant.getSku() + "' is not available for purchase"
            );
        }

        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                    "Insufficient stock for '" + variant.getSku() + "'. " +
                            "Available: " + variant.getStock() + ", Requested: " + quantity
            );
        }
    }

    @Override
    public int getStock(Long variantId) {
        return getVariant(variantId).getStock();
    }

    @Override
    public void reserveStock(Long variantId, int quantity) {
        validateStock(variantId, quantity);
        ProductVariantEntity variant = getVariant(variantId);
        variant.setStock(variant.getStock() - quantity);
        variantRepository.save(variant);
    }

    @Override
    public void releaseStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);
        variant.setStock(variant.getStock() + quantity);
        variantRepository.save(variant);
    }

    @Override
    public void adjustStock(Long variantId, int oldQuantity, int newQuantity) {
        int difference = newQuantity - oldQuantity;

        if (difference > 0) {
            // Need more stock - validate and reserve additional
            validateStock(variantId, difference);
            reserveStock(variantId, difference);
        } else if (difference < 0) {
            // Releasing stock back
            releaseStock(variantId, Math.abs(difference));
        }
        // If difference == 0, no change needed
    }

    @Override
    public ProductVariantEntity getVariant(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
    }
}
