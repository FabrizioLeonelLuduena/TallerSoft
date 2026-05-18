package com.tallersoft.model;

/**
 * User role enumeration
 * Defines the three types of users in the system
 */
public enum Rol {
    ADMIN("ADMIN"),
    TECNICO("TECNICO"),
    RECEPCION("RECEPCION");

    private final String value;

    Rol(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Rol fromString(String value) {
        for (Rol rol : Rol.values()) {
            if (rol.value.equalsIgnoreCase(value)) {
                return rol;
            }
        }
        throw new IllegalArgumentException("Rol inválido: " + value);
    }
}
