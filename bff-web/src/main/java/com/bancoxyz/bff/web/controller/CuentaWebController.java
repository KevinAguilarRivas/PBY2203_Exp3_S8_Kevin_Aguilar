package com.bancoxyz.bff.web.controller;

import com.bancoxyz.bff.web.client.CoreMovimientoAnualDTO;
import com.bancoxyz.bff.web.client.CoreServiceClient;
import com.bancoxyz.bff.web.dto.CuentaWebDTO;
import com.bancoxyz.bff.web.dto.MovimientoWebDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * BFF Web: agrega interes + movimientos en una sola respuesta completa
 * para pantallas de detalle y auditoria. Requiere JWT OAuth2 valido.
 */
@RestController
@RequestMapping("/web/cuentas")
public class CuentaWebController {

    private final CoreServiceClient coreClient;

    public CuentaWebController(CoreServiceClient coreClient) {
        this.coreClient = coreClient;
    }

    @GetMapping("/{cuentaId}")
    public CuentaWebDTO obtenerCuenta(@PathVariable Long cuentaId) {
        var interes = coreClient.obtenerInteres(cuentaId);
        List<MovimientoWebDTO> movs = coreClient.obtenerMovimientos(cuentaId).stream()
                .map(m -> new MovimientoWebDTO(m.fecha(), m.tipoOperacion(),
                        m.monto(), m.descripcion(), m.anomalia()))
                .toList();

        return new CuentaWebDTO(
                interes.cuentaId(), interes.nombre(), interes.edad(),
                interes.tipoCuenta(), interes.saldoInicial(), interes.saldoFinal(),
                interes.anomalia(), movs
        );
    }
}
