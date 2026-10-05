package com.bancoxyz.core.controller;

import com.bancoxyz.core.dto.TransaccionDiariaDTO;
import com.bancoxyz.core.model.ResumenTransaccion;
import com.bancoxyz.core.repository.ResumenTransaccionRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class TransaccionesController {

    private final ResumenTransaccionRepository repository;

    public TransaccionesController(ResumenTransaccionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/internal/transacciones-diarias")
    public List<TransaccionDiariaDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        List<ResumenTransaccion> resultado = (fecha != null)
                ? repository.findByFecha(fecha)
                : repository.findAll();

        return resultado.stream()
                .map(t -> new TransaccionDiariaDTO(
                        t.getTransaccionId(),
                        t.getFecha(),
                        t.getMonto(),
                        t.getTipo(),
                        t.isAnomalia()))
                .toList();
    }
}
