package com.auvier.repositories;

import com.auvier.entities.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    Optional<OrderItemEntity> findByOrderIdAndProductVariantId(Long orderId, Long productVariantId);

    Optional<OrderItemEntity> findByIdAndOrderId(Long id, Long orderId);

    void deleteByOrderId(Long orderId);

    @Query("SELECT COUNT(oi) FROM OrderItemEntity oi WHERE oi.order.id = :orderId")
    int countByOrderId(@Param("orderId") Long orderId);

    boolean existsByOrderIdAndProductVariantId(Long orderId, Long productVariantId);
}
