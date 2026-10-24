package es.ual.sadual.integracion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("F01 · Inicio de sesión con perfiles")
class AuthIT extends AbstractIntegracionIT {

    private static String credenciales(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @ParameterizedTest(name = "{0} entra con perfil {1} (puede reservar: {2})")
    @CsvSource({
            "alumna.demo@ual.es,       ALUMNADO, true",
            "pdi.demo@ual.es,          PDI,      true",
            "pas.demo@ual.es,          PAS,      true",
            "externo.demo@correo.test, EXTERNO,  false",
            "admin.deportes@ual.es,    ADMIN,    true"
    })
    void cada_perfil_inicia_sesion_y_el_sistema_reconoce_su_rol(String email, String rol, boolean puedeReservar)
            throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales(email, PASSWORD_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.expiraEn").exists())
                .andExpect(jsonPath("$.usuario.email").value(email))
                .andExpect(jsonPath("$.usuario.rol").value(rol))
                .andExpect(jsonPath("$.usuario.puedeReservar").value(puedeReservar)) // P4
                .andExpect(jsonPath("$.usuario.passwordHash").doesNotExist());
    }

    @Test
    void el_email_no_distingue_mayusculas() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("Alumna.Demo@UAL.es", PASSWORD_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("ALUMNADO"));
    }

    @Test
    void contrasena_incorrecta_da_401_sin_revelar_el_motivo() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("alumna.demo@ual.es", "otra")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void email_inexistente_da_el_mismo_401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("nadie@ual.es", PASSWORD_DEMO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIALES_INVALIDAS"));
    }

    @Test
    void un_usuario_desactivado_no_puede_entrar() throws Exception {
        jdbc.update("UPDATE usuario SET activo = FALSE WHERE email = 'pas.demo@ual.es'");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credenciales("pas.demo@ual.es", PASSWORD_DEMO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void datos_incompletos_dan_400_con_el_campo_en_error() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-un-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.password").exists());
    }

    @Test
    void sin_token_la_api_responde_401() throws Exception {
        mvc.perform(get("/api/instalaciones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reservas/mias")).andExpect(status().isUnauthorized());
    }

    @Test
    void un_token_manipulado_se_rechaza() throws Exception {
        String token = token("alumna.demo@ual.es");
        String manipulado = token.substring(0, token.length() - 4) + "AAAA";

        mvc.perform(get("/api/instalaciones").header("Authorization", bearer(manipulado)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_devuelve_el_usuario_del_token() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token("pdi.demo@ual.es"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Andrés"))
                .andExpect(jsonPath("$.rol").value("PDI"));
    }

    @Test
    void solo_el_admin_entra_en_rutas_de_administracion() throws Exception {
        // En V1 aún no hay endpoints /api/admin: el admin obtiene 404 y el resto 403.
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token("alumna.demo@ual.es"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", bearer(token("admin.deportes@ual.es"))))
                .andExpect(status().isNotFound());
    }
}
