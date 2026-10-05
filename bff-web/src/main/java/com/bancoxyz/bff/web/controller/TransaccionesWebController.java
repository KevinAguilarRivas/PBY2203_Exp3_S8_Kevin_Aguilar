package com.bancoxyz.bff.web.controller;

import com.bancoxyz.bff.web.client.CoreServiceClient;
import com.bancoxyz.bff.web.client.CoreTransaccionDiariaDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/web/transacciones")
public class TransaccionesWebController {

    private final CoreServiceClient coreClient;

    public TransaccionesWebController(CoreServiceClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping
    public List<CoreTransaccionDiariaDTO> listar() {
        return coreClient.obtenerTransacciones();
    }
}
