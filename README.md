# DocuMentor — Java Swing (MVC)

Aplicación de escritorio que convierte un documento (.txt / .pdf) en **flashcards** y **exámenes de opción múltiple** con IA real (OpenRouter, solo modelos gratuitos) o con un simulador local cuando no hay API key ni Internet.

## Estructura

```
src/documentor/
├── Main.java                         Ensambla Modelo, Vista y Controlador
├── modelo/                           Lógica de negocio (sin Swing)
│   ├── Documento.java                Entidad: archivo cargado
│   ├── Flashcard.java                Entidad: pregunta / respuesta
│   ├── PreguntaExamen.java           Entidad: 4 opciones + estado de respuesta
│   ├── TipoMaterial.java             FLASHCARDS | EXAMEN
│   ├── SesionEstudio.java            Estado en memoria (navegación, edición, puntaje)
│   ├── LimitesGeneracion.java        Máximo de preguntas según la extensión del texto
│   ├── GeneradorMaterial.java        Interfaz del "motor de IA"
│   ├── SimuladorIA.java              Implementación heurística (listas, causas, definiciones, datos)
│   ├── LectorDocumentos.java         Lee .txt (UTF-8 / ANSI) y .pdf
│   ├── ExtractorPdfBasico.java       Extractor PDF sin dependencias (respaldo)
│   └── ExportadorMaterial.java       Exporta a .txt / .csv con FileWriter
├── vista/                            Sólo UI
│   ├── GestorTema.java               Paleta 60-30-10, modo claro/oscuro, tipografía
│   ├── VentanaPrincipal.java         JFrame, divisor redimensionable y menú lateral animado
│   ├── HeaderView.java               Barra superior
│   ├── DocumentView.java             Panel izquierdo (texto, zoom, generación)
│   ├── InteractiveView.java          Panel derecho con CardLayout
│   ├── FlashcardView.java            Estado 2: tarjeta de repaso
│   ├── ExamenView.java               Estado 3: examen
│   ├── PanelConfiguracion.java       Menú lateral: tema y API key
│   ├── DialogoCantidad.java          Mini menú «¿cuántas preguntas?»
│   └── componentes/                  Botones, paneles redondeados, iconos vectoriales…
└── controlador/
    └── ControladorPrincipal.java     Listeners, KeyBindings, SwingWorker, exportación
```

## Tecnologías (SDC-000 v1.1)

| Necesidad | Tecnología usada | Nota |
|---|---|---|
| Lenguaje e interfaz | Java SE 8+ y Swing (MVC) | El equipo compila con JDK 21 usando `--release 8` |
| IA generativa | **OpenRouter**, solo modelos `:free` y `openrouter/free` | Sustituye a DeepSeek del documento de alcance |
| Lectura de JSON | `JsonSimple` (lector propio, sin dependencias) | Sustituye a Gson |
| Lectura de PDF | `ExtractorPdfBasico` (propio) + **Apache PDFBox opcional** | PDFBox se detecta por reflexión si está en el classpath |
| PDF de contexto (HU-18) | Motor gratuito `cloudflare-ai` de OpenRouter | Nunca se usa un motor de pago |
| HTTP | `HttpURLConnection` | Compatible con Java 8 |

## Repositorio (SDC-000 v1.1)

- **Se versiona:** `src/`, `DocuMentor.jar`, `README.md`, `documentor.properties.example`, `ejemplo_requisitos.txt`, `Ejecutar_DocuMentor.bat`, la configuración compartida de Eclipse (`.project`, `.classpath`, `.settings/`) y de VS Code (`.vscode/settings.json`, `.vscode/launch.json`).
- **Nunca se versiona:** `documentor.properties` (tu API key), `out/`, `build/`, `bin/`, `*.class` y archivos personales del IDE o del sistema. Todo está en `.gitignore`.
- Para configurar tu clave: copia `documentor.properties.example` como `documentor.properties` o guárdala desde ⚙ Configuración.
- Ramas: `feature/DM-n-descripcion`, `fix/DM-n-…`, `docs/…`, `chore/…`. `main` está protegida y cada Pull Request necesita 1 aprobación.

## Compilar y ejecutar

Requiere **Java 8 o superior** (JDK para compilar). Los fuentes están en **UTF-8**.

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out documentor.Main
```

En Windows (PowerShell):

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out documentor.Main
```

También se incluye `DocuMentor.jar` ya compilado (compatible con Java 8+): doble clic, `java -jar DocuMentor.jar`, o `Ejecutar_DocuMentor.bat`, que muestra en consola cualquier error.

**Eclipse:** *File → New → Java Project* (desmarca *Create module-info.java*). Copia la carpeta `src/documentor` dentro de `src` del proyecto, haz clic derecho en el proyecto → *Properties → Resource → Text file encoding → UTF-8*, y ejecuta `documentor.Main` con *Run As → Java Application*. Al guardar la API key desde la app, `documentor.properties` se crea en la raíz del proyecto.

**NetBeans / IntelliJ:** crea un proyecto Java, copia la carpeta `src/documentor` dentro de *Source Packages*, define `documentor.Main` como clase principal y verifica que la codificación del proyecto sea UTF-8.

## PDF

- Sin librerías extra, `ExtractorPdfBasico` lee PDFs sencillos (fuentes estándar, filtros Flate/ASCII85).
- Para PDFs de Word, escaneados o con fuentes incrustadas agrega **Apache PDFBox** (2.x o 3.x) al classpath; `LectorDocumentos` lo detecta automáticamente por reflexión:

```bash
java -cp "out:pdfbox-app-3.0.3.jar" documentor.Main      # Linux / macOS
java -cp "out;pdfbox-app-3.0.3.jar" documentor.Main      # Windows
```

## Funciones

| Requisito | Dónde |
|---|---|
| MVC estricto | paquetes `modelo` / `vista` / `controlador` |
| CardLayout (vacío, carga, flashcards, examen, resultado) | `InteractiveView` |
| SwingWorker con progreso | `ControladorPrincipal.ejecutarGeneracion` / `abrirDocumento` |
| Editar / eliminar tarjetas y preguntas | iconos lápiz y papelera |
| Exportar .txt / .csv (FileWriter) | botón **Exportar** → `ExportadorMaterial` |
| KeyBindings (Espacio, ← →) | `ControladorPrincipal.registrarAtajosTeclado` |
| Zoom ±2 pt | `btnZoomIn` / `btnZoomOut` |
| Generar desde selección | `btnGenerarSeleccion` (menú: flashcards o examen) |
| Modo claro / oscuro | Menú lateral ⚙ → `PanelConfiguracion` + `GestorTema` |
| API key desde la app (guardar, verificar, quitar) | Menú lateral ⚙ → `ControladorPrincipal.guardarClave/verificarClave` |
| Tooltips en todos los controles (con estilo del tema) | `GestorTema.configurarTooltips` |
| Vista de resultados redimensionable | `JSplitPane` arrastrable + botón «Ampliar resultados» (doble clic en el divisor) |
| Editar flashcards y preguntas en la misma vista | Lápiz o doble clic → `FlashcardView` / `ExamenView` (Ctrl+Enter guarda, Esc cancela) |
| Exportar examen con respuestas marcadas | TXT con `[X]` y `<-- RESPUESTA CORRECTA`; CSV con letra y texto correcto |
| PDF «puros» como contexto (HU-18) | «Agregar PDF» → `ArchivoContexto`; se envían completos a OpenRouter con el lector **gratuito** `cloudflare-ai` (nunca `mistral-ocr`, que cobra) |
| Elegir cuántas preguntas generar | `DialogoCantidad`; límite en `LimitesGeneracion` (≈1 por cada 20 palabras, máx. 25; el simulador, según los conceptos que detecta) |

## IA real con OpenRouter (solo modelos gratis)

Sin configuración, DocuMentor usa el **simulador local**. Para usar IA real:

1. Crea una cuenta y una API key en https://openrouter.ai/keys (no requiere tarjeta).
2. En https://openrouter.ai/settings/privacy permite los modelos/endpoints gratuitos (si no, OpenRouter responde 404).
3. Abre la app → botón **⚙ (Configuración)** → pega la clave → **Guardar**. La app la verifica con OpenRouter (sin gastar peticiones), la guarda en `documentor.properties` y cambia de motor al instante.
   - Alternativas: copiar `documentor.properties.example` como `documentor.properties`, o la variable de entorno `OPENROUTER_API_KEY` (se usa solo si el archivo no tiene clave).
4. Abre la app: el título de la ventana dice qué motor está activo.

**Solo modelos gratuitos:** se aceptan únicamente IDs terminados en `:free` y el enrutador `openrouter/free` (elige al azar un modelo gratis disponible). Cualquier otro modelo se ignora, y el cliente bloquea por código cualquier petición a un modelo de pago.

**Comportamiento ante fallos:** si un modelo está saturado (429), no existe (404) o responde con JSON inválido, se prueba el siguiente y después hasta 3 modelos `:free` del catálogo. Si todo falla (sin Internet, clave inválida, límite diario), el material se genera con el simulador local y se muestra un aviso con el motivo.

**Antivirus (Kaspersky, Avast, ESET…):** estos programas revisan las conexiones HTTPS con un certificado propio que Java no conoce ("PKIX path building failed"). `ConfianzaCertificados` hace que DocuMentor confíe también en los certificados de Windows, sin desactivar la verificación. Si aun así falla, agrega `openrouter.ai` a las exclusiones del antivirus.

**Límites del plan gratis:** ~20 peticiones por minuto y 50 por día (1000 por día si alguna vez compraste 10 créditos). Cada clic en «Generar» usa al menos una petición.

**PDF de contexto (HU-18):** con «Agregar PDF» puedes adjuntar hasta 5 PDF (10 MB c/u, 20 MB en total). La IA en línea los recibe **tal cual** (base64) y los lee con el motor gratuito de OpenRouter, así que funcionan aunque el visor local no muestre su texto (por ejemplo, PDF exportados desde Word). Se puede generar material solo con estos PDF, sin abrir un documento. Sin API key, el simulador usa únicamente el texto que se pueda extraer localmente.

**Privacidad:** los proveedores de modelos gratuitos pueden registrar el texto enviado. No envíes documentos confidenciales. **No subas `documentor.properties` a GitHub** (ya está en `.gitignore`).

| Clase nueva (`modelo/ia`) | Función |
|---|---|
| `GeneradorOpenRouter` | Implementa `GeneradorMaterial`: prompts, interpretación del JSON y cambio de modelo |
| `GeneradorConRespaldo` | Si OpenRouter falla, usa `SimuladorIA` y avisa |
| `ClienteOpenRouter` | Peticiones HTTP (`HttpURLConnection`, compatible con Java 8) |
| `ConfiguracionIA` | Lee clave y opciones; filtra modelos que no sean gratis |
| `FabricaGeneradores` | Elige OpenRouter (con respaldo) o el simulador según haya clave |
| `ConfianzaCertificados` | Confía en los certificados de Java **y** de Windows (antivirus) |
| `JsonSimple` / `ExcepcionIA` | Lector JSON mínimo y errores con mensajes legibles |

Prueba rápida: abre `ejemplo_requisitos.txt`.
