# Post-contenido — Unidad 5: Arquitectura en Capas y MVC + REST

**Estudiante:** Camilo García  
**Curso:** Patrones de Diseño de Software — Sexto Semestre  
**Unidad:** Unidad 5 — Arquitectura en Capas y MVC + REST  
**Actividad:** Post-contenido — Sistema de Reservas de Laboratorios de Cómputo  
**Repositorio:** `garcia-post-u5`  
**Plataforma:** NPLAD UFPS  

---

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`reservas-labs-api`) para la gestión de reservas de laboratorios de cómputo universitarios, estructurado en dos partes sobre una arquitectura limpia en capas:
1. **Parte 1 — API REST en Capas:** Entidades JPA, Repositorios con Spring Data JPA sobre H2 en memoria, capa de Servicio con lógica de negocio no anémica (detección de solapamientos con JPQL y validación de horarios) y controladores REST.
2. **Parte 2 — Vista MVC con Thymeleaf:** Controlador `@Controller` que reutiliza exactamente la misma capa de `ReservaService`, plantillas HTML interactivas y manejo de excepciones desacoplado por superficie de presentación.
