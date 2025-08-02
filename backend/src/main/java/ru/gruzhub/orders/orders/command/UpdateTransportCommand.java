package ru.gruzhub.orders.orders.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.transport.dto.TransportDto;
import ru.gruzhub.transport.TransportService;
import ru.gruzhub.transport.model.Transport;
import ru.gruzhub.orders.orders.model.Order;
import ru.gruzhub.orders.orders.repository.OrderRepository;

@Service
@RequiredArgsConstructor
public class UpdateTransportCommand {
    private final TransportService transportService;
    private final OrderRepository orderRepository;

    public void updateTransport(Transport transportToUpdate, TransportDto updatedValues) {
        Transport updatedTransport = this.transportService.updateTransport(transportToUpdate, updatedValues);

        if (!transportToUpdate.getId().equals(updatedTransport.getId())) {
            this.moveOrdersFromDuplicatedTransportToOriginalTransport(transportToUpdate, updatedTransport);
        }

    }

    private void moveOrdersFromDuplicatedTransportToOriginalTransport(Transport duplicatedTransport,
                                                            Transport originalTransport) {
        List<Order> orders = this.orderRepository.findOrdersByTransport(duplicatedTransport.getId());

        for (Order order : orders) {
            order.getTransport().removeIf(transport -> transport.getId().equals(duplicatedTransport.getId()));
            order.getTransport().add(originalTransport);
        }

        this.orderRepository.saveAll(orders);
    }
}
