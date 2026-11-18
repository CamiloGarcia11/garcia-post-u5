package com.universidad.reservaslabs.web;

import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.service.ReservaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de ReservaWebController y ReservaWebExceptionHandler")
class ReservaWebControllerTest {

    @Mock
    private ReservaService service;

    @Mock
    private LaboratorioRepository laboratorioRepo;

    @InjectMocks
    private ReservaWebController controller;

    private final ReservaWebExceptionHandler exceptionHandler = new ReservaWebExceptionHandler();

    @Test
    @DisplayName("GET /reservas debe retornar la vista reservas/lista con el atributo reservas")
    void testListar() {
        Model model = new ConcurrentModel();
        when(service.findAll()).thenReturn(List.of());

        String view = controller.listar(model);

        assertEquals("reservas/lista", view);
        assertTrue(model.containsAttribute("reservas"));
    }

    @Test
    @DisplayName("GET /reservas/nueva debe retornar la vista reservas/nueva con formulario y laboratorios")
    void testFormularioNueva() {
        Model model = new ConcurrentModel();
        when(laboratorioRepo.findAll()).thenReturn(List.of(new Laboratorio(1L, "Lab 1", "Bloque A", 20, "COMPUTO")));

        String view = controller.formularioNueva(model);

        assertEquals("reservas/nueva", view);
        assertTrue(model.containsAttribute("reserva"));
        assertTrue(model.containsAttribute("laboratorios"));
    }

    @Test
    @DisplayName("POST /reservas debe delegar en service.crear y redirigir con mensaje flash de exito")
    void testCrearExitoso() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        Reserva r = new Reserva();

        String view = controller.crear(r, redirect);

        verify(service, times(1)).crear(r);
        assertEquals("redirect:/reservas", view);
        assertTrue(redirect.getFlashAttributes().containsKey("mensaje"));
    }

    @Test
    @DisplayName("ReservaWebExceptionHandler debe redirigir a /reservas/nueva con mensaje de error ante ReservaConflictException")
    void testManejoConflictoWeb() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        ReservaConflictException ex = new ReservaConflictException("El laboratorio ya tiene una reserva en ese horario");

        String view = exceptionHandler.conflicto(ex, redirect);

        assertEquals("redirect:/reservas/nueva", view);
        assertEquals("El laboratorio ya tiene una reserva en ese horario", redirect.getFlashAttributes().get("error"));
    }
}
