package com.magiclibrary.exceptions.custom;

/**
 * Exception levée lorsqu’un administrateur tente de répondre
 * à un message CONTACT ayant déjà reçu une réponse.
 */
public class ContactAlreadyAnsweredException extends RuntimeException {

    public ContactAlreadyAnsweredException(String message) {
        super(message);
    }
}