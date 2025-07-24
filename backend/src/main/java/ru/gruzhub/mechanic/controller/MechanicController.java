package ru.gruzhub.mechanic.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mechanic")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class MechanicController {



}
