package ru.gruzhub.mechanic;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.gruzhub.mechanic.dto.MechanicDto;
import ru.gruzhub.mechanic.model.Mechanic;
import ru.gruzhub.transport.TransportColumnRepository;
import ru.gruzhub.transport.model.TransportColumn;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MechanicService {

    private final MechanicRepository mechanicRepository;
    private final TransportColumnRepository transportColumnRepository;

    @Transactional
    public Mechanic createMechanic(MechanicDto mechanicDto) {
        Optional<TransportColumn> transportColumn = this.transportColumnRepository.findById(mechanicDto.getTransportColumn().getId());
        if (!transportColumn.isPresent()) {
            transportColumn = this.transportColumnRepository.findByColumnNumber(mechanicDto.getTransportColumn().getColumnNumber());
        }

        if (transportColumn.isEmpty()) {
            throw new MechanicCreationException("Cannot create mechanic: Transport column with the number %s and ID %s wasn't found!");
        }
        return this.mechanicRepository.save(
                Mechanic.builder()
                        .phone(mechanicDto.getPhone())
                        .email(mechanicDto.getEmail())
                        .name(mechanicDto.getName())
                        .transportColumn(transportColumn.get())
                        .build());
    }
}
