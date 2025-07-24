package ru.gruzhub.mechanic.service;

import lombok.RequiredArgsConstructor;
import org.jvnet.hk2.annotations.Service;
import ru.gruzhub.mechanic.repository.MechanicRepository;

@Service
@RequiredArgsConstructor
public class MechanicService {

    private final MechanicRepository mechanicRepository;




}
