package ru.gruzhub.tools.telegram.exceptions;

public class BotBlockedByUserException extends Exception {
  public BotBlockedByUserException(String message) {
    super(message);
  }
}
