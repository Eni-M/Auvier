package com.auvier.mappers;

import com.auvier.dtos.order.*;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.entities.OrderEntity;
import com.auvier.entities.OrderItemEntity;
import com.auvier.entities.UserEntity;
import com.auvier.entities.catalog.ProductVariantEntity;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // ==================== ORDER MAPPINGS ====================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(OrderCreateDto dto);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "itemCount", expression = "java(entity.getOrderItems() != null ? entity.getOrderItems().size() : 0)")
    OrderResponseDto toResponseDto(OrderEntity entity);

    @Mapping(target = "itemCount", expression = "java(entity.getOrderItems() != null ? entity.getOrderItems().size() : 0)")
    @Mapping(target = "customerUsername", source = "user.username")
    OrderSummaryDto toSummaryDto(OrderEntity entity);

    List<OrderSummaryDto> toSummaryDtoList(List<OrderEntity> entities);

    // ==================== USER SUMMARY MAPPING ====================

    @Mapping(target = "role", source = "role")
    UserSummaryDto toUserSummaryDto(UserEntity user);

    // ==================== ORDER ITEM MAPPINGS ====================

    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "variantName", expression = "java(buildVariantName(item.getProductVariant()))")
    @Mapping(target = "color", source = "productVariant.color")
    @Mapping(target = "size", source = "productVariant.size")
    @Mapping(target = "sku", source = "productVariant.sku")
    @Mapping(target = "imageUrl", ignore = true) // Product doesn't have imageUrl field yet
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    OrderItemResponseDto toItemResponseDto(OrderItemEntity item);

    List<OrderItemResponseDto> toItemResponseDtoList(List<OrderItemEntity> items);

    default String buildVariantName(ProductVariantEntity variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null) sb.append(variant.getColor());
        if (variant.getSize() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(variant.getSize().name());
        }
        return sb.length() > 0 ? sb.toString() : variant.getSku();
    }

    default BigDecimal calculateSubtotal(OrderItemEntity item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
