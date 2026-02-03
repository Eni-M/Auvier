package com.auvier.controllers;

import com.auvier.dtos.order.OrderSummaryDto;
import com.auvier.entities.UserEntity;
import com.auvier.infrastructure.services.OrderService;
import com.auvier.infrastructure.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CustomerOrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/orders")
    public String myOrders(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<OrderSummaryDto> orders = orderService.getOrdersForUser(user);
        model.addAttribute("orders", orders);

        return "store/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Model model,
                              @AuthenticationPrincipal UserDetails userDetails) {
        UserEntity user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        var order = orderService.getOrderForUser(id, user);
        model.addAttribute("order", order);

        return "store/order-detail";
    }
}
