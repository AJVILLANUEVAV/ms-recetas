package com.nttdata.recetas;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calcula la firma de una receta. (RN-04)
 * La clave no se guarda aqui: llega como parametro desde el recurso,
 * que a su vez la recibe de HashiCorp Vault.
 */
@ApplicationScoped
public class FirmaService {

    private static final int LONGITUD_FIRMA = 16;

    public String firmar(String codigoReceta, String clave) {
        byte[] resumen = sha256(codigoReceta + ":" + clave);
        StringBuilder hexadecimal = new StringBuilder(resumen.length * 2);
        for (byte b : resumen) {
            hexadecimal.append(String.format("%02x", b));
        }
        return hexadecimal.substring(0, LONGITUD_FIRMA);
    }

    /** Utilidad: devuelve el resumen SHA-256 del texto. (Resuelto) */
    private byte[] sha256(String texto) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
