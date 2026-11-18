package com.universidad.reservaslabs.controller;

import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de ReservaController REST")
class ReservaControllerTest {

    @Mock
    private ReservaService service;

    @InjectMocks
    private ReservaController controller;

    @Test
    @DisplayName("GET /api/reservas debe listar todas las reservas")
    void testListar() {
        when(service.findAll()).thenReturn(List.of());
        List<Reserva> lista = controller.listar();
        assertNotNull(lista);
        verify(service, times(1)).findAll();
    }

    @Test
    @DisplayName("GET /api/reservas/{id} debe retornar 200 si existe")
    void testObtenerExistente() {
        Reserva r = new Reserva(1L, new Laboratorio(1L, "Lab", "A", 20, "COMPUTO"), "Ana", "ana@udes.edu.co",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), "Clase", EstadoReserva.CONFIRMADA);
        when(service.findById(1L)).thenReturn(Optional.of(r));

        ResponseEntity<Reserva> response = controller.obtener(1L);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    @DisplayName("POST /api/reservas debe retornar 201 Created al crear")
    void testCrear() {
        Reserva r = new Reserva();
        when(service.crear(r)).thenReturn(r);

        ResponseEntity<Reserva> response = controller.crear(r);

        assertEquals(201, response.getStatusCode().value());
        verify(service, times(1)).crear(r);
    }

    @Test
    @DisplayName("DELETE /api/reservas/{id} debe delegar y retornar 204 No Content")
    void testCancelar() {
        ResponseEntity<Void> response = controller.cancelar(1L);
        assertEquals(204, response.getStatusCode().value());
        verify(service, times(1)).cancelar(1L);
    }
}
