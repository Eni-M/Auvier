package com.auvier.infrastructure.services;

import com.auvier.entities.catalog.ProductVariantEntity;

/**
 * Service for managing product variant inventory/stock.
 * Handles stock validation, reservation, and deduction.
 */
public interface InventoryService {

    /**
     * Check if a variant has sufficient stock.
     * @param variantId the product variant ID
     * @param quantity required quantity
     * @return true if stock is available
     */
    boolean hasStock(Long variantId, int quantity);

    /**
     * Validate stock availability - throws exception if insufficient.
     * @param variantId the product variant ID
     * @param quantity required quantity
     * @throws IllegalStateException if stock is insufficient
     */
    void validateStock(Long variantId, int quantity);

    /**
     * Get current stock level for a variant.
     * @param variantId the product variant ID
     * @return current stock quantity
     */
    int getStock(Long variantId);

    /**
     * Reserve stock (decrement) when adding to order.
     * @param variantId the product variant ID
     * @param quantity quantity to reserve
     */
    void reserveStock(Long variantId, int quantity);

    /**
     * Release reserved stock (increment) when removing from order or cancelling.
     * @param variantId the product variant ID
     * @param quantity quantity to release
     */
    void releaseStock(Long variantId, int quantity);

    /**
     * Adjust stock when updating order item quantity.
     * @param variantId the product variant ID
     * @param oldQuantity previous quantity
     * @param newQuantity new quantity
     */
    void adjustStock(Long variantId, int oldQuantity, int newQuantity);

    /**
     * Get the variant entity (for price lookup, etc.).
     * @param variantId the product variant ID
     * @return the variant entity
     */
    ProductVariantEntity getVariant(Long variantId);
}
