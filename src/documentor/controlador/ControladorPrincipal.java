package documentor.controlador;

import documentor.modelo.ArchivoContexto;
import documentor.modelo.Documento;
import documentor.modelo.ExportadorMaterial;
import documentor.modelo.Flashcard;
import documentor.modelo.GeneradorMaterial;
import documentor.modelo.LectorDocumentos;
import documentor.modelo.LimitesGeneracion;
import documentor.modelo.PreguntaExamen;
import documentor.modelo.SesionEstudio;
import documentor.modelo.TipoMaterial;
import documentor.modelo.ia.ClienteOpenRouter;
import documentor.modelo.ia.ConfiguracionIA;
import documentor.modelo.ia.ExcepcionIA;
import documentor.modelo.ia.FabricaGeneradores;
import documentor.vista.DocumentView;
import documentor.vista.ExamenView;
import documentor.vista.FlashcardView;
import documentor.vista.GestorTema;
import documentor.vista.HeaderView;
import documentor.vista.InteractiveView;
import documentor.vista.PanelConfiguracion;
import documentor.vista.PanelConfiguracion.TipoMensaje;
import documentor.vista.VentanaPrincipal;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;

/**
 * Controlador: registra los listeners de la Vista, traduce las acciones del
 * usuario en operaciones del Modelo y decide qué tarjeta del CardLayout mostrar.
 * Todas las tareas lentas (lectura de archivos, generación, consultas a OpenRouter)
 * corren en SwingWorker.
 */
public class ControladorPrincipal {

    private static final int TAMANO_FUENTE_MINIMO = 10;
    private static final int TAMANO_FUENTE_MAXIMO = 40;
    private static final int PASO_ZOOM = 2;
    private static final long PAUSA_POR_PASO_MS = 450;
    private static final String URL_CLAVES = "https://openrouter.ai/keys";
    private static final String[] PASOS_GENERACION = {
        "Analizando la estructura del texto...",
        "Identificando conceptos clave...",
        "Redactando preguntas...",
        "Revisando respuestas y distractores..."
    };

    private final VentanaPrincipal vista;
    private final SesionEstudio sesion;
    private final LectorDocumentos lector;
    private final ExportadorMaterial exportador;

    private final HeaderView headerView;
    private final DocumentView documentView;
    private final InteractiveView interactiveView;
    private final FlashcardView flashcardView;
    private final ExamenView examenView;
    private final PanelConfiguracion panelConfiguracion;

    private ConfiguracionIA configuracion;
    private GeneradorMaterial generador;
    private SwingWorker<?, ?> tareaActual;

    public ControladorPrincipal(VentanaPrincipal vista, SesionEstudio sesion, ConfiguracionIA configuracion,
                                LectorDocumentos lector, ExportadorMaterial exportador) {
        this.vista = vista;
        this.sesion = sesion;
        this.configuracion = configuracion;
        this.generador = FabricaGeneradores.crear(configuracion);
        this.lector = lector;
        this.exportador = exportador;
        this.headerView = vista.getHeaderView();
        this.documentView = vista.getDocumentView();
        this.interactiveView = vista.getInteractiveView();
        this.flashcardView = interactiveView.getFlashcardView();
        this.examenView = interactiveView.getExamenView();
        this.panelConfiguracion = vista.getPanelConfiguracion();
    }

    public void iniciar() {
        registrarEventos();
        registrarAtajosTeclado();
        documentView.actualizarZoom(documentView.getTamanoFuente(), true, true);
        actualizarEstadoIA();
        mostrarVistaSegunModo();
        vista.setVisible(true);
        if (!configuracion.getModelosDescartados().isEmpty()) {
            vista.mostrarAdvertencia("Modelos ignorados",
                    "Estos modelos no son gratuitos y se ignoraron:\n"
                            + String.join("\n", configuracion.getModelosDescartados())
                            + "\n\nSolo se permiten modelos terminados en \":free\" o \"openrouter/free\".");
        }
    }

    // ================================================================== Registro de eventos

    private void registrarEventos() {
        // Barra superior
        headerView.getBtnAbrirDocumento().addActionListener(e -> abrirDocumento());
        headerView.getBtnExportar().addActionListener(e -> exportarMaterial());
        headerView.getBtnAmpliarResultados().addActionListener(e -> vista.alternarAmpliacionResultados());
        headerView.getBtnConfiguracion().addActionListener(e -> {
            actualizarEstadoIA();
            vista.alternarConfiguracion();
        });

        // Menú lateral de configuración
        panelConfiguracion.getBtnModoTema().addActionListener(e -> alternarTema());
        panelConfiguracion.getBtnGuardarClave().addActionListener(e -> guardarClave());
        panelConfiguracion.getTxtApiKey().addActionListener(e -> guardarClave());   // Enter
        panelConfiguracion.getBtnVerificarClave().addActionListener(e -> verificarClave());
        panelConfiguracion.getBtnEliminarClave().addActionListener(e -> eliminarClave());
        panelConfiguracion.getBtnObtenerClave().addActionListener(e -> {
            if (!vista.abrirEnNavegador(URL_CLAVES)) {
                vista.mostrarInformacion("Obtener API key", "Abre esta página en tu navegador:\n" + URL_CLAVES);
            }
        });

        // Panel izquierdo
        documentView.getBtnGenerarFlashcards().addActionListener(
                e -> generarMaterial(documentView.getTextoCompleto(), TipoMaterial.FLASHCARDS));
        documentView.getBtnGenerarExamen().addActionListener(
                e -> generarMaterial(documentView.getTextoCompleto(), TipoMaterial.EXAMEN));
        documentView.getBtnGenerarSeleccion().addActionListener(e -> documentView.mostrarMenuSeleccion());
        documentView.getMniFlashcardsSeleccion().addActionListener(e -> generarDesdeSeleccion(TipoMaterial.FLASHCARDS));
        documentView.getMniExamenSeleccion().addActionListener(e -> generarDesdeSeleccion(TipoMaterial.EXAMEN));
        documentView.getBtnZoomIn().addActionListener(e -> cambiarZoom(PASO_ZOOM));
        documentView.getBtnZoomOut().addActionListener(e -> cambiarZoom(-PASO_ZOOM));
        documentView.getTxtContenidoPDF().addCaretListener(e -> actualizarEstadoControles());
        documentView.getBtnAgregarContexto().addActionListener(e -> agregarContextos());
        documentView.setAccionQuitarContexto(this::quitarContexto);

        // Flashcards
        flashcardView.getBtnVoltear().addActionListener(e -> voltearTarjeta());
        flashcardView.getBtnAnterior().addActionListener(e -> navegarFlashcard(false));
        flashcardView.getBtnSiguiente().addActionListener(e -> navegarFlashcard(true));
        flashcardView.getBtnEditarTarjeta().addActionListener(e -> editarFlashcard());
        flashcardView.getBtnGuardarEdicion().addActionListener(e -> guardarEdicionFlashcard());
        flashcardView.getBtnEliminarTarjeta().addActionListener(e -> eliminarFlashcard());
        alDobleClic(flashcardView.getComponenteContenido(), this::editarFlashcard);

        // Examen
        List<JRadioButton> radios = examenView.getRdbOpciones();
        for (int i = 0; i < radios.size(); i++) {
            final int indice = i;
            radios.get(i).addItemListener(e -> {
                PreguntaExamen actual = sesion.getPreguntaActual();
                if (e.getStateChange() == ItemEvent.SELECTED && actual != null && !actual.isConfirmada()) {
                    examenView.marcarSeleccion(indice);
                }
            });
        }
        examenView.getBtnConfirmar().addActionListener(e -> confirmarRespuesta());
        examenView.getBtnSiguiente().addActionListener(e -> siguientePregunta());
        examenView.getBtnEditarPregunta().addActionListener(e -> editarPregunta());
        examenView.getBtnGuardarPregunta().addActionListener(e -> guardarEdicionPregunta());
        examenView.getBtnEliminarPregunta().addActionListener(e -> eliminarPregunta());
        alDobleClic(examenView.getComponenteEnunciado(), this::editarPregunta);
        interactiveView.getBtnRepetirExamen().addActionListener(e -> repetirExamen());
    }

    private static void alDobleClic(JComponent componente, Runnable accion) {
        componente.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    accion.run();
                }
            }
        });
    }

    /** KeyBindings: Espacio voltea, flechas izquierda/derecha navegan entre flashcards. */
    private void registrarAtajosTeclado() {
        flashcardView.registrarAtajo("voltearTarjeta", KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                crearAccion(this::voltearTarjeta));
        flashcardView.registrarAtajo("tarjetaAnterior", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0),
                crearAccion(() -> navegarFlashcard(false)));
        flashcardView.registrarAtajo("tarjetaSiguiente", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0),
                crearAccion(() -> navegarFlashcard(true)));
    }

    private Action crearAccion(Runnable accion) {
        return new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                // Sin atajos mientras se edita texto o está abierto el menú de configuración
                if (sesion.getModo() == SesionEstudio.Modo.FLASHCARDS && !estaGenerando()
                        && !flashcardView.estaEditando() && !vista.isConfiguracionAbierta()) {
                    accion.run();
                }
            }
        };
    }

    // ================================================================== Documento

    private void abrirDocumento() {
        if (estaGenerando()) {
            return;
        }
        File archivo = vista.seleccionarDocumento();
        if (archivo == null) {
            return;
        }
        interactiveView.mostrarCarga("Leyendo documento...", archivo.getName());
        SwingWorker<Documento, Void> tarea = new SwingWorker<Documento, Void>() {
            @Override
            protected Documento doInBackground() throws Exception {
                return lector.leer(archivo);
            }

            @Override
            protected void done() {
                tareaActual = null;
                try {
                    Documento documento = get();
                    sesion.cargarDocumento(documento);
                    documentView.mostrarDocumento(documento.getNombreArchivo(), documento.getContenido());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    vista.mostrarError("No se pudo abrir el documento", mensajeDe(ex));
                }
                mostrarVistaSegunModo();
            }
        };
        iniciarTarea(tarea);
    }

    // ================================================================== PDF de contexto (HU-18)

    /** Valida y prepara los PDF en segundo plano (extraer texto puede tardar) y los agrega a la sesión. */
    private void agregarContextos() {
        List<File> archivos = vista.seleccionarPdfsContexto();
        if (archivos.isEmpty()) {
            return;
        }
        documentView.habilitarAgregarContexto(false, sesion.getContextos().size());
        new SwingWorker<List<Object>, Void>() {
            @Override
            protected List<Object> doInBackground() {
                List<Object> resultados = new ArrayList<>();   // ArchivoContexto o String (error)
                for (File archivo : archivos) {
                    try {
                        resultados.add(ArchivoContexto.crear(archivo, lector));
                    } catch (IOException e) {
                        resultados.add(e.getMessage());
                    }
                }
                return resultados;
            }

            @Override
            protected void done() {
                List<String> errores = new ArrayList<>();
                List<String> soloEnLinea = new ArrayList<>();
                try {
                    for (Object r : get()) {
                        if (r instanceof ArchivoContexto) {
                            ArchivoContexto contexto = (ArchivoContexto) r;
                            try {
                                sesion.agregarContexto(contexto);
                                if (!contexto.esLegibleLocalmente()) {
                                    soloEnLinea.add(contexto.getNombre());
                                }
                            } catch (IllegalArgumentException ex) {
                                errores.add(ex.getMessage());
                            }
                        } else {
                            errores.add(String.valueOf(r));
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    errores.add(mensajeDe(ex));
                }
                refrescarContextos();
                if (!errores.isEmpty()) {
                    vista.mostrarAdvertencia("Algunos PDF no se agregaron", String.join("\n", errores));
                }
                if (!soloEnLinea.isEmpty() && !generador.leePdfCompletos()) {
                    vista.mostrarInformacion("PDF solo para la IA en línea",
                            "Estos PDF no tienen texto legible sin Internet:\n" + String.join("\n", soloEnLinea)
                                    + "\n\nSe enviarán completos cuando configures tu API key (botón ⚙). "
                                    + "Mientras tanto, el simulador local no puede usarlos.");
                }
            }
        }.execute();
    }

    private void quitarContexto(int indice) {
        if (estaGenerando()) {
            return;
        }
        sesion.quitarContexto(indice);
        refrescarContextos();
    }

    private void refrescarContextos() {
        List<String> nombres = new ArrayList<>();
        List<String> descripciones = new ArrayList<>();
        for (ArchivoContexto c : sesion.getContextos()) {
            nombres.add(c.getNombre());
            descripciones.add(c.getDescripcion());
        }
        documentView.mostrarContextos(nombres, descripciones);
        actualizarEstadoControles();
    }

    // ================================================================== Generación (SwingWorker)

    private void generarDesdeSeleccion(TipoMaterial tipo) {
        String seleccion = documentView.getTextoSeleccionado();
        if (seleccion == null || seleccion.trim().isEmpty()) {
            vista.mostrarAdvertencia("Sin selección",
                    "Resalta con el ratón un fragmento del documento y vuelve a intentarlo.");
            return;
        }
        generarMaterial(seleccion, tipo);
    }

    private void generarMaterial(String texto, TipoMaterial tipo) {
        if (estaGenerando()) {
            return;
        }
        String fuente = texto == null ? "" : texto;
        List<ArchivoContexto> contextos = new ArrayList<>(sesion.getContextos());
        if (fuente.trim().isEmpty() && contextos.isEmpty()) {
            vista.mostrarAdvertencia("Sin texto", "Primero abre un documento o agrega un PDF de contexto.");
            return;
        }
        // 1) El Modelo calcula el máximo según la información del texto (y de los PDF de contexto)
        int maximo = generador.calcularMaximo(fuente, contextos);
        if (maximo <= 0) {
            vista.mostrarAdvertencia("Texto insuficiente", !contextos.isEmpty() && !generador.leePdfCompletos()
                    ? "Los PDF de contexto no tienen texto legible sin Internet.\n"
                            + "Configura tu API key (botón ⚙) para que la IA en línea los lea completos."
                    : "No se encontraron conceptos suficientes en el texto.\n"
                            + "Prueba con un fragmento más largo o con oraciones completas.");
            return;
        }
        // 2) Mini menú: ¿cuántas?
        String sustantivo = tipo == TipoMaterial.FLASHCARDS ? "flashcards" : "preguntas";
        Integer elegida = vista.pedirCantidad(sustantivo, maximo, LimitesGeneracion.predeterminado(maximo),
                explicarLimite(fuente, contextos, maximo));
        if (elegida == null) {
            return;   // el usuario canceló
        }
        int cantidad = LimitesGeneracion.ajustar(elegida, maximo);

        // 3) Generación en segundo plano (los PDF de contexto viajan completos si el motor lo permite)
        GeneradorMaterial motor = generador;
        if (tipo == TipoMaterial.FLASHCARDS) {
            ejecutarGeneracion(motor, contextos.size(),
                    () -> motor.generarFlashcards(fuente, cantidad, contextos), sesion::cargarFlashcards);
        } else {
            ejecutarGeneracion(motor, contextos.size(),
                    () -> motor.generarExamen(fuente, cantidad, contextos), sesion::cargarExamen);
        }
    }

    private String explicarLimite(String texto, List<ArchivoContexto> contextos, int maximo) {
        int palabras = LimitesGeneracion.contarPalabras(texto);
        String pdfs = contextos.isEmpty() ? "" : " + " + contextos.size()
                + (contextos.size() == 1 ? " PDF de contexto" : " PDF de contexto");
        if (generador.esSimulado()) {
            return "El simulador local detectó " + maximo + (maximo == 1 ? " concepto" : " conceptos")
                    + " (" + palabras + " palabras" + pdfs + "), así que no puede generar más.";
        }
        String estimado = contextos.isEmpty() ? ""
                : " Las palabras de los PDF que no se leen localmente se estiman por su tamaño.";
        return "Según la extensión (" + palabras + " palabras" + pdfs + ") se permite aproximadamente 1 por cada "
                + LimitesGeneracion.PALABRAS_POR_PREGUNTA + " palabras, con un tope de "
                + LimitesGeneracion.MAXIMO_ABSOLUTO + "." + estimado;
    }

    /**
     * Ejecuta la generación en segundo plano y, al terminar, entrega el resultado en el
     * hilo de eventos (EDT). Con el simulador local se simulan los pasos y la latencia;
     * con una IA real (OpenRouter) solo se informa del progreso, sin esperas artificiales.
     */
    private <T> void ejecutarGeneracion(GeneradorMaterial motor, int numeroPdfs, Callable<List<T>> generacion,
                                        Consumer<List<T>> alTerminar) {
        boolean simulado = motor.esSimulado();
        String adjuntos = numeroPdfs == 0 ? "" : " y " + numeroPdfs + (numeroPdfs == 1 ? " PDF" : " PDF");
        interactiveView.mostrarCarga("Generando material...",
                simulado ? PASOS_GENERACION[0] : "Enviando el texto" + adjuntos + " a " + motor.getNombre() + "...");
        SwingWorker<List<T>, String> tarea = new SwingWorker<List<T>, String>() {
            @Override
            protected List<T> doInBackground() throws Exception {
                if (simulado) {
                    for (String paso : PASOS_GENERACION) {
                        publish(paso);
                        Thread.sleep(PAUSA_POR_PASO_MS);   // simula la latencia de un modelo de IA
                    }
                } else {
                    publish("Esperando la respuesta del modelo (puede tardar hasta un minuto)...");
                }
                return generacion.call();
            }

            @Override
            protected void process(List<String> pasos) {
                interactiveView.actualizarPasoCarga(pasos.get(pasos.size() - 1));
            }

            @Override
            protected void done() {
                tareaActual = null;
                try {
                    List<T> resultado = get();
                    if (resultado.isEmpty()) {
                        vista.mostrarAdvertencia("Sin resultados",
                                "No se encontraron conceptos suficientes en el texto.\n"
                                        + "Prueba con un fragmento más largo o con oraciones completas.");
                    } else {
                        alTerminar.accept(resultado);
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    vista.mostrarError("Error al generar", mensajeDe(ex));
                }
                mostrarVistaSegunModo();
                actualizarTitulo();
                String aviso = motor.consumirAviso();
                if (aviso != null) {
                    vista.mostrarAdvertencia("Aviso de la IA", aviso);
                }
            }
        };
        iniciarTarea(tarea);
    }

    private void iniciarTarea(SwingWorker<?, ?> tarea) {
        tareaActual = tarea;
        actualizarEstadoControles();
        tarea.execute();
    }

    private boolean estaGenerando() {
        return tareaActual != null && !tareaActual.isDone();
    }

    // ================================================================== Flashcards

    private void voltearTarjeta() {
        sesion.voltearFlashcard();
        mostrarFlashcardActual();
    }

    private void navegarFlashcard(boolean haciaAdelante) {
        boolean movio = haciaAdelante ? sesion.siguienteFlashcard() : sesion.anteriorFlashcard();
        if (movio) {
            mostrarFlashcardActual();
        }
    }

    /** Edición en línea del lado visible de la tarjeta (pregunta o respuesta). */
    private void editarFlashcard() {
        Flashcard actual = sesion.getFlashcardActual();
        if (actual == null || flashcardView.estaEditando()) {
            return;
        }
        flashcardView.iniciarEdicion(sesion.isRespuestaVisible() ? actual.getRespuesta() : actual.getPregunta());
    }

    private void guardarEdicionFlashcard() {
        Flashcard actual = sesion.getFlashcardActual();
        if (actual == null) {
            return;
        }
        String texto = flashcardView.getTextoEditado();
        if (texto.isEmpty()) {
            vista.mostrarAdvertencia("Texto vacío", "La tarjeta no puede quedar vacía.");
            return;
        }
        if (sesion.isRespuestaVisible()) {
            sesion.editarFlashcardActual(actual.getPregunta(), texto);
        } else {
            sesion.editarFlashcardActual(texto, actual.getRespuesta());
        }
        mostrarFlashcardActual();
    }

    private void eliminarFlashcard() {
        if (sesion.getFlashcardActual() == null) {
            return;
        }
        if (vista.confirmar("¿Eliminar esta flashcard? Esta acción no se puede deshacer.", "Eliminar flashcard")) {
            sesion.eliminarFlashcardActual();
        }
        mostrarVistaSegunModo();
    }

    private void mostrarFlashcardActual() {
        Flashcard actual = sesion.getFlashcardActual();
        if (actual == null) {
            mostrarVistaSegunModo();
            return;
        }
        boolean respuesta = sesion.isRespuestaVisible();
        flashcardView.mostrarTarjeta(respuesta ? actual.getRespuesta() : actual.getPregunta(), respuesta,
                sesion.getIndiceFlashcard() + 1, sesion.getTotalFlashcards());
    }

    // ================================================================== Examen

    private void confirmarRespuesta() {
        PreguntaExamen actual = sesion.getPreguntaActual();
        int seleccion = examenView.getIndiceSeleccionado();
        if (actual == null || actual.isConfirmada()) {
            return;
        }
        if (seleccion < 0) {
            vista.mostrarAdvertencia("Sin respuesta", "Selecciona una opción antes de confirmar.");
            return;
        }
        sesion.responderPreguntaActual(seleccion);
        mostrarPreguntaActual();
    }

    private void siguientePregunta() {
        sesion.avanzarPregunta();
        mostrarVistaSegunModo();
    }

    /** Edición en línea: enunciado, 4 opciones y cuál es la correcta. */
    private void editarPregunta() {
        PreguntaExamen actual = sesion.getPreguntaActual();
        if (actual == null || examenView.estaEditando()) {
            return;
        }
        examenView.iniciarEdicion(actual.getEnunciado(), actual.getOpciones(), actual.getIndiceCorrecto());
    }

    private void guardarEdicionPregunta() {
        if (sesion.getPreguntaActual() == null) {
            return;
        }
        int correcta = examenView.getCorrectaEditada();
        if (correcta < 0) {
            vista.mostrarAdvertencia("Falta la respuesta correcta", "Marca cuál de las opciones es la correcta.");
            return;
        }
        try {
            // El Modelo valida (enunciado y opciones no vacíos); si falla, se avisa y se sigue editando
            sesion.editarPreguntaActual(examenView.getEnunciadoEditado(), examenView.getOpcionesEditadas(), correcta);
            mostrarPreguntaActual();
        } catch (IllegalArgumentException ex) {
            vista.mostrarAdvertencia("Datos incompletos", ex.getMessage());
        }
    }

    private void eliminarPregunta() {
        if (sesion.getPreguntaActual() == null) {
            return;
        }
        if (vista.confirmar("¿Eliminar esta pregunta del examen?", "Eliminar pregunta")) {
            sesion.eliminarPreguntaActual();
        }
        mostrarVistaSegunModo();
    }

    private void repetirExamen() {
        sesion.reiniciarExamen();
        mostrarVistaSegunModo();
    }

    private void mostrarPreguntaActual() {
        PreguntaExamen actual = sesion.getPreguntaActual();
        if (actual == null) {
            mostrarVistaSegunModo();
            return;
        }
        examenView.mostrarPregunta(actual.getEnunciado(), actual.getOpciones(),
                sesion.getIndicePregunta() + 1, sesion.getTotalPreguntas(),
                actual.isConfirmada(), actual.getIndiceSeleccionado(), actual.getIndiceCorrecto());
    }

    // ================================================================== Exportación (FileWriter)

    private void exportarMaterial() {
        TipoMaterial tipo = sesion.getMaterialExportable();
        Documento documento = sesion.getDocumento() != null ? sesion.getDocumento()
                : new Documento("contexto_pdf.txt", "", null);   // material generado solo con PDF de contexto
        if (tipo == null) {
            vista.mostrarAdvertencia("Nada que exportar", "Genera flashcards o un examen antes de exportar.");
            return;
        }
        File destino = vista.seleccionarDestinoExportacion(documento.getNombreBase() + "_" + tipo.getSufijoArchivo());
        if (destino == null) {
            return;
        }
        try {
            int cantidad;
            if (tipo == TipoMaterial.FLASHCARDS) {
                exportador.exportarFlashcards(sesion.getListaFlashcards(), destino, documento.getNombreArchivo());
                cantidad = sesion.getTotalFlashcards();
            } else {
                exportador.exportarExamen(sesion.getListaPreguntas(), destino, documento.getNombreArchivo());
                cantidad = sesion.getTotalPreguntas();
            }
            vista.mostrarInformacion("Exportación completada",
                    "Se guardaron " + cantidad + (tipo == TipoMaterial.FLASHCARDS ? " flashcards" : " preguntas")
                            + " en:\n" + destino.getAbsolutePath()
                            + (tipo == TipoMaterial.EXAMEN ? "\n\nLas respuestas correctas van marcadas con [X]." : ""));
        } catch (IOException ex) {
            vista.mostrarError("Error al exportar", "No se pudo escribir el archivo:\n" + ex.getMessage());
        }
    }

    // ================================================================== Configuración de IA (API key)

    /** Verifica la clave escrita (o la guardada) sin gastar peticiones de modelos. */
    private void verificarClave() {
        String escrita = panelConfiguracion.getClaveEscrita();
        ConfiguracionIA aProbar = escrita.isEmpty() ? configuracion : configuracion.conClave(escrita);
        if (!aProbar.tieneClave()) {
            panelConfiguracion.mostrarResultadoClave("Escribe una API key o guarda una primero.", TipoMensaje.ERROR);
            return;
        }
        panelConfiguracion.setOcupado(true);
        panelConfiguracion.mostrarResultadoClave("Consultando OpenRouter...", TipoMensaje.INFORMACION);
        new SwingWorker<ClienteOpenRouter.InfoClave, Void>() {
            @Override
            protected ClienteOpenRouter.InfoClave doInBackground() {
                return new ClienteOpenRouter(aProbar).verificarClave();
            }

            @Override
            protected void done() {
                panelConfiguracion.setOcupado(false);
                try {
                    panelConfiguracion.mostrarResultadoClave(get().resumen()
                            + (escrita.isEmpty() ? "" : "\nPulsa «Guardar» para usarla."), TipoMensaje.EXITO);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    panelConfiguracion.mostrarResultadoClave(mensajeDe(ex), TipoMensaje.ERROR);
                }
            }
        }.execute();
    }

    /**
     * Verifica la clave escrita y la guarda. Si OpenRouter la rechaza (401) no se guarda;
     * si no se pudo verificar por otra causa (p. ej. sin Internet), se guarda y se avisa.
     */
    private void guardarClave() {
        String escrita = panelConfiguracion.getClaveEscrita();
        if (escrita.isEmpty()) {
            panelConfiguracion.mostrarResultadoClave("Pega tu API key en el campo de arriba.", TipoMensaje.ERROR);
            return;
        }
        ConfiguracionIA candidata = configuracion.conClave(escrita);
        panelConfiguracion.setOcupado(true);
        panelConfiguracion.mostrarResultadoClave("Verificando la clave con OpenRouter...", TipoMensaje.INFORMACION);
        new SwingWorker<String, Void>() {
            private boolean rechazada;

            @Override
            protected String doInBackground() throws IOException {
                String verificacion;
                try {
                    verificacion = new ClienteOpenRouter(candidata).verificarClave().resumen();
                } catch (ExcepcionIA e) {
                    if (e.getMessage() != null && e.getMessage().contains("(401)")) {
                        rechazada = true;
                        return e.getMessage();
                    }
                    verificacion = "No se pudo verificar ahora (" + e.getMessage() + "), pero se guardó.";
                }
                File archivo = ConfiguracionIA.guardarClave(ControladorPrincipal.class, configuracion, escrita);
                return verificacion + "\nGuardada en: " + archivo.getAbsolutePath();
            }

            @Override
            protected void done() {
                panelConfiguracion.setOcupado(false);
                try {
                    String mensaje = get();
                    if (rechazada) {
                        panelConfiguracion.mostrarResultadoClave("No se guardó: " + mensaje, TipoMensaje.ERROR);
                        return;
                    }
                    panelConfiguracion.limpiarCampoClave();
                    recargarConfiguracion();
                    panelConfiguracion.mostrarResultadoClave(mensaje, TipoMensaje.EXITO);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    panelConfiguracion.mostrarResultadoClave("No se pudo guardar el archivo: " + mensajeDe(ex),
                            TipoMensaje.ERROR);
                }
            }
        }.execute();
    }

    private void eliminarClave() {
        if (!vista.confirmar("¿Quitar la API key guardada? DocuMentor usará el simulador local (sin Internet).",
                "Quitar API key")) {
            return;
        }
        try {
            ConfiguracionIA.guardarClave(ControladorPrincipal.class, configuracion, "");
            recargarConfiguracion();
            panelConfiguracion.mostrarResultadoClave(configuracion.tieneClave()
                    ? "Se quitó del archivo, pero sigue activa la variable de entorno OPENROUTER_API_KEY."
                    : "Clave eliminada. Ahora se usa el simulador local.", TipoMensaje.INFORMACION);
        } catch (IOException ex) {
            panelConfiguracion.mostrarResultadoClave("No se pudo modificar el archivo: " + ex.getMessage(),
                    TipoMensaje.ERROR);
        }
    }

    /** Vuelve a leer documentor.properties y cambia de motor sin reiniciar la app. */
    private void recargarConfiguracion() {
        configuracion = ConfiguracionIA.cargar(ControladorPrincipal.class);
        generador = FabricaGeneradores.crear(configuracion);
        actualizarEstadoIA();
    }

    private void actualizarEstadoIA() {
        String ubicacion = configuracion.getArchivoOrigen() != null
                ? configuracion.getArchivoOrigen().getAbsolutePath()
                : "documentor.properties (se crea al guardar, junto a DocuMentor.jar)";
        if (configuracion.tieneClave() && configuracion.getArchivoOrigen() == null) {
            ubicacion = configuracion.getOrigenClave();
        }
        panelConfiguracion.mostrarEstadoIA(generador.getNombre(), !generador.esSimulado(),
                configuracion.tieneClave() ? configuracion.getClaveEnmascarada() : null, ubicacion);
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        vista.setTitle("DocuMentor — " + generador.getNombre());
    }

    // ================================================================== Apariencia

    private void alternarTema() {
        GestorTema.alternar();
        vista.aplicarTema();
    }

    private void cambiarZoom(int delta) {
        int nuevo = Math.max(TAMANO_FUENTE_MINIMO,
                Math.min(TAMANO_FUENTE_MAXIMO, documentView.getTamanoFuente() + delta));
        documentView.actualizarZoom(nuevo, nuevo < TAMANO_FUENTE_MAXIMO, nuevo > TAMANO_FUENTE_MINIMO);
    }

    // ================================================================== Sincronización Vista ↔ Modelo

    /** Muestra la tarjeta del CardLayout que corresponde al estado del Modelo. */
    private void mostrarVistaSegunModo() {
        switch (sesion.getModo()) {
            case FLASHCARDS:
                mostrarFlashcardActual();
                interactiveView.mostrarFlashcards();
                flashcardView.solicitarFoco();
                break;
            case EXAMEN:
                mostrarPreguntaActual();
                interactiveView.mostrarExamen();
                break;
            case RESULTADO:
                interactiveView.mostrarResultado(sesion.contarAciertos(), sesion.getTotalPreguntas(),
                        sesion.contarSinResponder());
                break;
            case VACIO:
            default:
                interactiveView.mostrarVacio();
        }
        actualizarEstadoControles();
    }

    private void actualizarEstadoControles() {
        boolean generando = estaGenerando();
        boolean hayTexto = sesion.tieneDocumento();
        String seleccion = documentView.getTextoSeleccionado();
        boolean haySeleccion = seleccion != null && !seleccion.trim().isEmpty();
        SesionEstudio.Modo modo = sesion.getModo();

        headerView.getBtnAbrirDocumento().setEnabled(!generando);
        headerView.getBtnExportar().setEnabled(!generando && sesion.getMaterialExportable() != null);
        documentView.habilitarGeneracion(sesion.tieneFuenteDeGeneracion() && !generando);
        documentView.habilitarAgregarContexto(!generando, sesion.getContextos().size());
        documentView.habilitarGeneracionSeleccion(hayTexto && haySeleccion && !generando);
        documentView.resaltarModo(modo == SesionEstudio.Modo.FLASHCARDS,
                modo == SesionEstudio.Modo.EXAMEN || modo == SesionEstudio.Modo.RESULTADO);
    }

    private static String mensajeDe(ExecutionException ex) {
        Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
        return causa.getMessage() != null ? causa.getMessage() : causa.getClass().getSimpleName();
    }
}
