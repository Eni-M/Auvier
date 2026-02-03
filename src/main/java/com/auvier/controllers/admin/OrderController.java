package com.auvier.controllers.admin;

import com.auvier.dtos.order.*;
import com.auvier.enums.OrderStatus;
import com.auvier.infrastructure.services.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @ModelAttribute("orderStatuses")
    public List<OrderStatus> populateStatuses() {
        return Arrays.asList(OrderStatus.values());
    }

    // ==================== LIST & VIEW ====================

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        return "admin/orders/view";
    }

    // ==================== STATUS MANAGEMENT ====================

    @GetMapping("/{id}/status")
    public String statusForm(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        model.addAttribute("statusUpdateDto", new OrderStatusUpdateDto());
        return "admin/orders/status";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @Valid @ModelAttribute("statusUpdateDto") OrderStatusUpdateDto dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/orders/status";
        }

        try {
            orderService.updateStatus(id, dto);
            redirectAttributes.addFlashAttribute("success", "Order status updated successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/orders/" + id;
    }

    // ==================== QUICK STATUS ACTIONS ====================

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order confirmed successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by admin") String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, reason);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/mark-paid")
    public String markAsPaid(@PathVariable Long id,
                             @RequestParam(required = false) String transactionId,
                             RedirectAttributes redirectAttributes) {
        try {
            orderService.markAsPaid(id, transactionId != null ? transactionId : "MANUAL-" + System.currentTimeMillis());
            redirectAttributes.addFlashAttribute("success", "Order marked as paid");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/mark-shipped")
    public String markAsShipped(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.markAsShipped(id);
            redirectAttributes.addFlashAttribute("success", "Order marked as shipped");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/mark-delivered")
    public String markAsDelivered(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.markAsDelivered(id);
            redirectAttributes.addFlashAttribute("success", "Order marked as delivered");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ==================== ITEM MANAGEMENT ====================

    @PostMapping("/{orderId}/items/{itemId}/remove")
    public String removeItem(@PathVariable Long orderId,
                             @PathVariable Long itemId,
                             RedirectAttributes redirectAttributes) {
        try {
            orderService.removeItem(orderId, itemId);
            redirectAttributes.addFlashAttribute("success", "Item removed from order");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    @PostMapping("/{orderId}/items/{itemId}/update-quantity")
    public String updateItemQuantity(@PathVariable Long orderId,
                                     @PathVariable Long itemId,
                                     @RequestParam int quantity,
                                     RedirectAttributes redirectAttributes) {
        try {
            orderService.updateItemQuantity(orderId, itemId, quantity);
            redirectAttributes.addFlashAttribute("success", "Item quantity updated");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + orderId;
    }

    // ==================== DELETE ORDER ====================

    @PostMapping("/{id}/delete")
    public String deleteOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.deleteOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order deleted successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders";
    }
}
