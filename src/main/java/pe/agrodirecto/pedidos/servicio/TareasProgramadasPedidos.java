package pe.agrodirecto.pedidos.servicio;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Procesos que corren solos cada 10 minutos. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TareasProgramadasPedidos {

    private final PedidoService pedidoService;

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT1M")
    public void ejecutar() {
        int liberados = pedidoService.liberarPagosVencidos();
        int sinRespuesta = pedidoService.expirarPedidosSinRespuesta();
        int expirados = pedidoService.expirarPedidosImpagos();
        if (liberados + sinRespuesta + expirados > 0) {
            log.info("Pagos liberados: {}. Pedidos sin respuesta cancelados: {}. Pedidos impagos cancelados: {}.",
                    liberados, sinRespuesta, expirados);
        }
    }
}
