package com.bancoxyz.bff.mobile.controller;

import com.bancoxyz.bff.mobile.client.CoreServiceClient;
import com.bancoxyz.bff.mobile.dto.MovimientoMobileDTO;
import com.bancoxyz.bff.mobile.dto.ResumenCuentaMobileDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BFF Móvil: respuestas mínimas para ahorrar ancho de banda.
 * Requiere JWT OAuth2 válido.
 */
@RestController
@RequestMapping("/mobile/cuentas")
public class CuentaMobileController {

    private final CoreServiceClient coreClient;

    public CuentaMobileController(CoreServiceClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping("/{cuentaId}/resumen")
    public ResumenCuentaMobileDTO resumen(@PathVariable Long cuentaId) {
        var c = coreClient.obtenerInteres(cuentaId);
        return new ResumenCuentaMobileDTO(c.cuentaId(), c.nombre(), c.tipoCuenta(), c.saldoFinal());
    }

    @GetMapping("/{cuentaId}/movimientos")
    public List<MovimientoMobileDTO> movimientos(
            @PathVariable Long cuentaId,
            @RequestParam(defaultValue = "5") int limite) {
        return coreClient.obtenerMovimientos(cuentaId).stream()
                .limit(limite)
                .map(m -> new MovimientoMobileDTO(m.fecha(), m.tipoOperacion(), m.monto()))
                .toList();
    }
}
