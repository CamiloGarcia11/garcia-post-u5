# Post-contenido — Unidad 5: Arquitectura en Capas y MVC + REST

**Estudiante:** Camilo García  
**Curso:** Patrones de Diseño de Software — Sexto Semestre  
**Unidad:** Unidad 5 — Arquitectura en Capas y MVC + REST  
**Actividad:** Post-contenido — Sistema de Reservas de Laboratorios de Cómputo  
**Repositorio:** `garcia-post-u5`  
**Plataforma:** NPLAD UFPS  

---

## Descripción
Repositorio del post-contenido de la Unidad 5 de Patrones de Diseño de Software. Un único proyecto Spring Boot (`reservas-labs-api`) para la gestión integral de reservas de laboratorios de cómputo universitarios. El proyecto demuestra la correcta separación de responsabilidades arquitectónicas mediante dos superficies de presentación independientes que comparten la misma capa de lógica de negocio y persistencia:
* **Parte 1 — API REST en Capas:** Entidades JPA (`model/`), Repositorios JPA con Spring Data (`repository/`), Capa de Servicio con lógica de negocio no anémica (`service/`), Manejo de Excepciones de Dominio (`exception/`) y Controladores REST (`controller/`).
* **Parte 2 — Vista MVC con Thymeleaf:** Controlador web clásico (`web/`) que consume la misma instancia del `ReservaService`, vistas interactivas HTML (`templates/reservas/`) y manejador de excepciones web dedicado para una experiencia de usuario consistente.

---

## Estructura del Proyecto

```text
garcia-post-u5/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── universidad/
    │   │           └── reservaslabs/
    │   │               ├── ReservasLabsApiApplication.java
    │   │               ├── controller/
    │   │               │   ├── LaboratorioController.java       # API REST /api/laboratorios
    │   │               │   └── ReservaController.java           # API REST /api/reservas
    │   │               ├── exception/
    │   │               │   ├── GlobalRestExceptionHandler.java  # @RestControllerAdvice (JSON 400, 404, 409)
    │   │               │   ├── RecursoNoEncontradoException.java# Excepción de dominio (404)
    │   │               │   └── ReservaConflictException.java    # Excepción de dominio (409)
    │   │               ├── model/
    │   │               │   ├── EstadoReserva.java               # Enum PENDIENTE, CONFIRMADA, CANCELADA
    │   │               │   ├── Laboratorio.java                 # Entidad JPA laboratorios
    │   │               │   └── Reserva.java                     # Entidad JPA reservas con FK laboratorio
    │   │               ├── repository/
    │   │               │   ├── LaboratorioRepository.java       # JpaRepository catálogo
    │   │               │   └── ReservaRepository.java           # Consulta JPQL optimizada de solapamiento
    │   │               ├── service/
    │   │               │   └── ReservaService.java              # Lógica de negocio (horarios, solapamiento)
    │   │               └── web/
    │   │                   ├── ReservaWebController.java        # @Controller MVC /reservas (Thymeleaf)
    │   │                   └── ReservaWebExceptionHandler.java  # @ControllerAdvice MVC (redirecciones)
    │   └── resources/
    │       ├── application.properties                           # Configuración H2 y JPA
    │       └── templates/
    │           └── reservas/
    │               ├── lista.html                               # Vista Thymeleaf: listado y cancelación
    │               └── nueva.html                               # Vista Thymeleaf: formulario de reserva
    └── test/
        └── java/
            └── com/
                └── universidad/
                    └── reservaslabs/
                        ├── controller/
                        │   └── ReservaControllerTest.java       # Pruebas unitarias API REST
                        ├── service/
                        │   └── ReservaServiceTest.java          # Pruebas unitarias de reglas de negocio
                        └── web/
                            └── ReservaWebControllerTest.java    # Pruebas unitarias vistas MVC y Web Handler
```

---

## Cómo ejecutar

Desde la raíz del repositorio:

```bash
# Compilar y empaquetar
mvn clean package

# Ejecutar la aplicación Spring Boot
mvn spring-boot:run

# Ejecutar pruebas automatizadas
mvn test
```

### URLs de acceso:
* **API REST:** `http://localhost:8080/api/reservas` y `http://localhost:8080/api/laboratorios`
* **Vistas Web Thymeleaf:** `http://localhost:8080/reservas` y `http://localhost:8080/reservas/nueva`
* **Consola H2 Database:** `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:reservas_labs_db`, Usuario: `sa`, Contraseña: en blanco)

---

## Decisiones de diseño

### Punto de decisión 1 — Ubicación de la validación de solapamiento de horarios (Parte 1)

* **Decisión adoptada:** El filtrado de reservas coincidentes se delega al motor relacional mediante una consulta JPQL personalizada en `ReservaRepository.buscarSolapamientos()`, mientras que la **evaluación y decisión de negocio** (rechazar la creación y emitir un mensaje descriptivo con `ReservaConflictException`) reside exclusivamente en `ReservaService`.
* **Justificación técnica:**  
  * **Separación de roles:** El Repository responde a una pregunta puramente de datos (*"¿qué registros en la base de datos colisionan en el intervalo $[inicio, fin)$?"*), mientras que el Service responde a una pregunta de dominio (*"¿es admisible esta reserva bajo las políticas institucionales?"*).
  * **Rendimiento y escalabilidad:** Traer todas las reservas históricas del laboratorio a la memoria de la JVM para comparar intervalos en bucles Java no escalaría a medida que el historial crezca a miles de registros. La base de datos resuelve el filtro indexado en milisegundos.
  * **Riesgo de que el Controller llame directamente a `buscarSolapamientos()`:** Si el Controller accediera directamente al Repository, violaría la arquitectura en capas provocando una fuga de lógica de datos hacia la capa web. Cualquier otro punto de entrada (como `ReservaWebController` o un proceso batch) se vería forzado a reimplementar la misma consulta y las mismas comprobaciones, destruyendo la coherencia del sistema y dejando al Service anémico.

---

### Punto de decisión 2 — Reglas con y sin apoyo del Repository (Parte 1)

* **Decisión adoptada:** Las reglas de horario institucional (atención únicamente entre 07:00 y 21:00) y duración permitida (mínimo 30 minutos, máximo 3 horas) se validan mediante Java puro en el método privado `validarHorarioYDuracion()` dentro de `ReservaService`, **sin invocar al Repository**.
* **Criterio arquitectónico:**
  * **Reglas intrínsecas del objeto (sin apoyo de BD):** Si la regla depende únicamente de los atributos del propio objeto que se intenta persistir (como verificar que `fin.isAfter(inicio)` o que la duración esté en el rango permitido), involucrar a la base de datos o al Repository introduce I/O innecesario y acoplamiento artificial. Estas reglas se evalúan en memoria en $O(1)$.
  * **Reglas contextuales de estado global (con apoyo de BD):** Si la regla requiere contrastar la solicitud contra el estado persistido del sistema (como verificar que ningún otro usuario haya reservado el mismo laboratorio en ese horario), es mandatorio apoyarse en una consulta específica del Repository.
  * *Nota sobre `LaboratorioController`:* Es la única excepción intencional donde el Controller inyecta `LaboratorioRepository` directamente: se trata de un catálogo CRUD simple sin reglas de negocio propias. Crear un `LaboratorioService` que solo delegue sin añadir valor constituiría un antipatrón de *Service anémico*.

---

### Punto de decisión 3 — Cómo comparten Service el Controller MVC y el REST (Parte 2)

* **Decisión adoptada:** Tanto `ReservaController` (`@RestController`) como `ReservaWebController` (`@Controller`) reciben por inyección de dependencias en su constructor la **misma instancia Singleton de `ReservaService`**.
* **Justificación técnica:**  
  * **Reutilización y DRY:** Ninguno de los dos controladores contiene lógica de validación ni reimplementa las reglas de solapamiento o duración. Ambos delegan en `service.crear(reserva)` y `service.cancelar(id)`.
  * **Alternativa descartada:** Duplicar la validación dentro de `ReservaWebController` o crear un segundo `ReservaWebService` casi idéntico. Esa alternativa habría generado código duplicado frágil: cualquier ajuste futuro en las políticas de reserva (por ejemplo, ampliar el horario de cierre a las 22:00) obligaría a modificar múltiples clases, con el riesgo inminente de que la web y la API REST presentaran comportamientos divergentes.

---

### Punto de decisión 4 — Manejo de errores consistente entre MVC y REST (Parte 2)

* **Decisión adoptada:** Se implementaron dos manejadores de excepciones separados e independientes basados en `@ControllerAdvice`, cada uno acotado a su respectiva superficie de presentación:
  1. `GlobalRestExceptionHandler`: Restringido con `annotations = RestController.class`, captura `ReservaConflictException` y `RecursoNoEncontradoException`, respondiendo payloads JSON estructurados con códigos HTTP 409 Conflict y 404 Not Found.
  2. `ReservaWebExceptionHandler`: Restringido con `assignableTypes = ReservaWebController.class`, captura las mismas excepciones de dominio pero realiza una redirección web HTTP agregando el mensaje legible como atributo flash (`RedirectAttributes.addFlashAttribute("error", ...)`), mostrándolo en la vista Thymeleaf.
* **Justificación técnica:**  
  * Ambas superficies comparten exactamente el mismo vocabulario de excepciones de negocio (`exception/`), pero difieren radicalmente en su protocolo de entrega: una API REST requiere JSON y códigos de estado semánticos, mientras que un navegador web requiere una redirección fluida a la vista HTML con feedback visual al usuario.
  * Unificar ambos manejadores en una única clase analizando cabeceras como `Accept` aumentaría innecesariamente la complejidad ciclomática y mezclaría responsabilidades (violando SRP). La delimitación mediante `annotations` y `assignableTypes` garantiza que ningún manejador interfiera con el otro.

---

## Evidencias de Funcionamiento

### 1. Pruebas de Endpoints REST (cURL / Postman)

#### A. Creación de Laboratorio (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/laboratorios \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Lab. Cómputo 3","ubicacion":"Bloque B, piso 2","capacidad":30,"tipo":"COMPUTO"}'
```
**Respuesta:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{"id":1,"nombre":"Lab. Cómputo 3","ubicacion":"Bloque B, piso 2","capacidad":30,"tipo":"COMPUTO"}
```

#### B. Creación de Reserva Válida (201 Created)
```bash
curl -i -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{"laboratorio":{"id":1},"nombreSolicitante":"Ana Torres","correoSolicitante":"ana@udes.edu.co","inicio":"2026-08-10T09:00:00","fin":"2026-08-10T11:00:00","motivo":"Práctica de Bases de Datos"}'
```
**Respuesta:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{"id":1,"laboratorio":{"id":1,"nombre":"Lab. Cómputo 3","ubicacion":"Bloque B, piso 2","capacidad":30,"tipo":"COMPUTO"},"nombreSolicitante":"Ana Torres","correoSolicitante":"ana@udes.edu.co","inicio":"2026-08-10T09:00:00","fin":"2026-08-10T11:00:00","motivo":"Práctica de Bases de Datos","estado":"CONFIRMADA"}
```

#### C. Intento de Reserva con Horario Solapado (409 Conflict)
```bash
curl -i -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{"laboratorio":{"id":1},"nombreSolicitante":"Luis Gómez","correoSolicitante":"luis@udes.edu.co","inicio":"2026-08-10T10:00:00","fin":"2026-08-10T12:00:00","motivo":"Proyecto de Redes"}'
```
**Respuesta:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{"error":"El laboratorio Lab. Cómputo 3 ya tiene una reserva en ese horario"}
```

#### D. Intento de Reserva Fuera de Horario de Atención (400 Bad Request)
```bash
curl -i -X POST http://localhost:8080/api/reservas \
  -H "Content-Type: application/json" \
  -d '{"laboratorio":{"id":1},"nombreSolicitante":"Carlos Ruiz","correoSolicitante":"carlos@udes.edu.co","inicio":"2026-08-10T22:00:00","fin":"2026-08-10T23:00:00","motivo":"Estudio nocturno"}'
```
**Respuesta:**
```http
HTTP/1.1 409 Conflict
Content-Type: application/json

{"error":"La reserva debe estar dentro del horario de atención (07:00 - 21:00)"}
```

---

### 2. Evidencia de la Vista MVC con Thymeleaf

| Ruta | Operación | Resultado en Navegador |
|---|---|---|
| `GET /reservas` | Listado general | Renderiza la tabla HTML con todas las reservas registradas, solicitante, horario, estado y botón "Cancelar". |
| `GET /reservas/nueva` | Formulario | Muestra el formulario con los campos solicitante, correo, selector dinámico de laboratorios cargados desde H2, fechas y motivo. |
| `POST /reservas` (Solapada) | Intento de reserva conflictiva | Redirige automáticamente a `/reservas/nueva` mostrando en rojo: `El laboratorio Lab. Cómputo 3 ya tiene una reserva en ese horario`. |
| `POST /reservas/{id}/cancelar` | Cancelación de reserva | Invoca a `service.cancelar(id)` y actualiza el estado a `CANCELADA` en la tabla con mensaje flash de confirmación. |

---

## Herramientas utilizadas

* **Java JDK:** 17+ (Desarrollado y probado sobre Java 21 LTS)
* **Spring Boot:** 3.2.0 (Spring Web MVC, Spring Data JPA, Hibernate, Thymeleaf, Validation)
* **H2 Database:** Base de datos relacional embebida en memoria
* **Lombok:** Generación automática de boilerplate (getters, setters, constructores)
* **Apache Maven:** 3.8+ (Gestión de dependencias y empaquetado)
* **JUnit 5 / Mockito:** Pruebas unitarias de Service, Controladores REST y MVC
* **Git & GitHub:** Control de versiones distribuido

---

## Conclusiones

El desarrollo de este laboratorio permitió afianzar los principios fundamentales de la arquitectura en capas y el patrón MVC en aplicaciones Spring Boot del mundo real:
1. La capa **Service** demostró ser indispensable como el núcleo donde residen las reglas de negocio, evitando el antipatrón de *Service anémico* y protegiendo a los controladores web de asumir responsabilidades de dominio.
2. La combinación de **Spring Data JPA** con consultas JPQL explícitas como `buscarSolapamientos()` evidenció la sinergia entre el filtrado eficiente en el motor de base de datos y la toma de decisiones algorítmicas en Java.
3. La integración simultánea de **REST** y **Thymeleaf MVC** sobre el mismo `ReservaService` demostró el verdadero poder del desacoplamiento arquitectónico: una única fuente de verdad para la lógica institucional que alimenta múltiples interfaces de usuario, garantizando consistencia, mantenibilidad y ausencia total de código duplicado.
