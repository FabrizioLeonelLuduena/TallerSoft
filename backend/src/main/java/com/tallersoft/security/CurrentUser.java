package com.tallersoft.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for injecting the current authenticated user into controller methods
 *
 * Usage:
 * @GetMapping("/profile")
 * public ResponseEntity<?> getProfile(@CurrentUser Usuario user) {
 *     // user is automatically injected
 * }
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
