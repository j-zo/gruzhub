package ru.gruzhub.orders.orders.commands;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.orders.auto.AutoService;
import ru.gruzhub.orders.auto.models.Auto;
import ru.gruzhub.orders.orders.models.Order;
import ru.gruzhub.orders.orders.repositories.OrderRepository;

@Service
@RequiredArgsConstructor
public class UpdateAutoCommand {
    private final AutoService autoService;
    private final OrderRepository orderRepository;

    public void updateAuto(Auto autoToUpdate) {
        Auto updatedAuto = this.autoService.updateAuto(autoToUpdate);

        if (!autoToUpdate.getId().equals(updatedAuto.getId())) {
            this.moveOrdersFromDuplicatedAutoToOriginalAuto(autoToUpdate, updatedAuto);
        }

    }

    private void moveOrdersFromDuplicatedAutoToOriginalAuto(Auto duplicatedAuto,
                                                            Auto originalAuto) {
        List<Order> orders = this.orderRepository.findOrdersByAuto(duplicatedAuto.getId());

        for (Order order : orders) {
            order.getAutos().removeIf(auto -> auto.getId().equals(duplicatedAuto.getId()));
            order.getAutos().add(originalAuto);
        }

        this.orderRepository.saveAll(orders);
    }
}
