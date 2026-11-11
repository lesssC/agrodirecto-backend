package pe.agrodirecto.pedidos.dominio;

/** RN-12: cómo llega el producto de la chacra al comprador. */
public enum ModalidadEntrega {
    /** El comprador recoge en la chacra: sin flete ni restricción de zona. */
    RECOJO_EN_CHACRA,
    /** El agricultor (o su agente) lleva el pedido. */
    ENTREGA_DEL_PRODUCTOR,
    /** Se despacha por una agencia de carga interprovincial. */
    AGENCIA_DE_CARGA;

    public boolean requiereTransporte() {
        return this != RECOJO_EN_CHACRA;
    }
}
