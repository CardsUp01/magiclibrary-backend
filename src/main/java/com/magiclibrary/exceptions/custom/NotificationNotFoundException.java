package com.magiclibrary.exceptions.custom;

/**
 * =============================================================================
 *  EXCEPTION : NotificationNotFoundException
 * =============================================================================
 *  Levée lorsqu’une notification demandée n’existe pas.
 * =============================================================================
 */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String message) {
        super(message);
    }

    public static NotificationNotFoundException forId(Integer id) {
        return new NotificationNotFoundException("Notification introuvable avec l'id : " + id);
    }
}