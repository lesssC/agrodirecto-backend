package pe.agrodirecto.catalogo.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.agrodirecto.catalogo.dominio.FormatoVenta;

public interface FormatoVentaRepository extends JpaRepository<FormatoVenta, Long> {
}
