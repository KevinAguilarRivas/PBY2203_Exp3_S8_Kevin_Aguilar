package com.bancoxyz.bff.cajero.controller;

import com.bancoxyz.bff.cajero.client.CoreCuentaInteresDTO;
import com.bancoxyz.bff.cajero.client.CoreServiceClient;
import com.bancoxyz.bff.cajero.dto.RetiroRequest;
import com.bancoxyz.bff.cajero.dto.RetiroResponseDTO;
import com.bancoxyz.bff.cajero.dto.SaldoCajeroDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * BFF Cajero: consulta de saldo y retiro con validacion de negocio.
 * Requiere JWT OAuth2 valido (Keycloak realm bancoxyz).
 * La publicacion del evento de retiro la hace core-service al persistir el saldo.
 */
@RestController
@RequestMapping("/cajero/cuentas")
public class CuentaCajeroController {

    private final CoreServiceClient coreClient;

    public CuentaCajeroController(CoreServiceClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping("/{cuentaId}/saldo")
    public SaldoCajeroDTO consultarSaldo(@PathVariable Long cuentaId) {
        CoreCuentaInteresDTO c = coreClient.obtenerInteres(cuentaId);
        return new SaldoCajeroDTO(c.cuentaId(), c.saldoFinal());
    }

    @PostMapping("/{cuentaId}/retiro")
    public RetiroResponseDTO retirar(@PathVariable Long cuentaId,
                                     @RequestBody RetiroRequest request) {
        CoreCuentaInteresDTO cuenta = coreClient.obtenerInteres(cuentaId);

        if (request.monto() == null || request.monto().compareTo(BigDecimal.ZERO) <= 0) {
            return RetiroResponseDTO.rechazado(cuentaId, "El monto debe ser mayor a 0.");
        }
        if (request.monto().compareTo(cuenta.saldoFinal()) > 0) {
            return RetiroResponseDTO.rechazado(cuentaId, "Saldo insuficiente.");
        }

        BigDecimal nuevoSaldo = cuenta.saldoFinal().subtract(request.monto());
        CoreCuentaInteresDTO actualizada = coreClient.actualizarSaldo(cuentaId, nuevoSaldo);
        return RetiroResponseDTO.aprobado(cuentaId, actualizada.saldoFinal());
    }
}
