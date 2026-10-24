package es.ual.sadual.dto;

/** Estado de una franja tal y como la ve el usuario en la pantalla de disponibilidad (F03). */
public enum EstadoFranja {
    LIBRE,
    RESERVADA,
    BLOQUEADA,
    /** Libre, pero empieza antes de la antelación mínima (P2: 3 horas). */
    FUERA_DE_PLAZO,
    PASADA
}
