package com.universidad.reservaslabs.service;

import com.universidad.reservaslabs.exception.RecursoNoEncontradoException;
import com.universidad.reservaslabs.exception.ReservaConflictException;
import com.universidad.reservaslabs.model.EstadoReserva;
import com.universidad.reservaslabs.model.Laboratorio;
import com.universidad.reservaslabs.model.Reserva;
import com.universidad.reservaslabs.repository.LaboratorioRepository;
import com.universidad.reservaslabs.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias de ReservaService (Reglas de Negocio)")
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepo;

    @Mock
    private LaboratorioRepository laboratorioRepo;

    @InjectMocks
    private ReservaService reservaService;

    private Laboratorio lab1;

    @BeforeEach
    void setUp() {
        lab1 = new Laboratorio(1L, "Lab. Cómputo 1", "Bloque A, piso 1", 30, "COMPUTO");
    }

    @Test
    @DisplayName("Debe crear una reserva válida dentro del horario y sin solapamientos")
    void testCrearReservaValida() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        LocalDateTime fin = inicio.plusHours(2);
        Reserva nueva = new Reserva(null, lab1, "Ana Torres", "ana@udes.edu.co", inicio, fin, "Clase POO", EstadoReserva.PENDIENTE);

        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab1));
        when(reservaRepo.buscarSolapamientos(eq(1L), eq(inicio), eq(fin))).thenReturn(Collections.emptyList());
        when(reservaRepo.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = reservaService.crear(nueva);

        assertNotNull(creada);
        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        verify(reservaRepo, times(1)).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe rechazar reserva si se solapa con una reserva existente (409)")
    void testRechazarPorSolapamiento() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fin = inicio.plusHours(2);
        Reserva nueva = new Reserva(null, lab1, "Carlos Pérez", "carlos@udes.edu.co", inicio, fin, "Práctica Redes", EstadoReserva.PENDIENTE);

        Reserva existente = new Reserva(10L, lab1, "Luis Gómez", "luis@udes.edu.co", inicio.minusMinutes(30), fin.minusMinutes(30), "Existente", EstadoReserva.CONFIRMADA);

        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab1));
        when(reservaRepo.buscarSolapamientos(eq(1L), eq(inicio), eq(fin))).thenReturn(List.of(existente));

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () -> reservaService.crear(nueva));
        assertTrue(ex.getMessage().contains("ya tiene una reserva en ese horario"));
        verify(reservaRepo, never()).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe rechazar reserva fuera del horario de atención (antes de 07:00 o después de 21:00)")
    void testRechazarFueraDeHorario() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(6).withMinute(0);
        LocalDateTime fin = inicio.plusHours(1);
        Reserva nueva = new Reserva(null, lab1, "Estudiante", "est@udes.edu.co", inicio, fin, "Temprano", EstadoReserva.PENDIENTE);

        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab1));

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () -> reservaService.crear(nueva));
        assertTrue(ex.getMessage().contains("horario de atención"));
    }

    @Test
    @DisplayName("Debe rechazar reserva con duración menor a 30 minutos")
    void testRechazarDuracionCorta() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fin = inicio.plusMinutes(20);
        Reserva nueva = new Reserva(null, lab1, "Estudiante", "est@udes.edu.co", inicio, fin, "Muy corta", EstadoReserva.PENDIENTE);

        when(laboratorioRepo.findById(1L)).thenReturn(Optional.of(lab1));

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () -> reservaService.crear(nueva));
        assertTrue(ex.getMessage().contains("duración de la reserva debe estar entre 30 minutos y 3 horas"));
    }

    @Test
    @DisplayName("Debe rechazar cancelación de una reserva cuyo inicio ya pasó")
    void testRechazarCancelacionTardia() {
        LocalDateTime inicioPasado = LocalDateTime.now().minusHours(2);
        LocalDateTime finPasado = inicioPasado.plusHours(1);
        Reserva reservaPasada = new Reserva(5L, lab1, "Estudiante", "est@udes.edu.co", inicioPasado, finPasado, "Pasada", EstadoReserva.CONFIRMADA);

        when(reservaRepo.findById(5L)).thenReturn(Optional.of(reservaPasada));

        ReservaConflictException ex = assertThrows(ReservaConflictException.class, () -> reservaService.cancelar(5L));
        assertTrue(ex.getMessage().contains("cuyo horario de inicio ya pasó"));
    }

    @Test
    @DisplayName("Debe lanzar RecursoNoEncontradoException si el laboratorio no existe")
    void testLaboratorioNoExiste() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        LocalDateTime fin = inicio.plusHours(1);
        Reserva nueva = new Reserva(null, new Laboratorio(99L, "Inexistente", "X", 10, "COMPUTO"), "A", "a@mail.com", inicio, fin, "Motivo", EstadoReserva.PENDIENTE);

        when(laboratorioRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> reservaService.crear(nueva));
    }
}
