package io.moyada.feign.help.support;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class Printer {

    private static final String INFO_TAG = "[INFO] ";
    private static final String WARNING_TAG = "[WARNING] ";
    private static final String ERROR_TAG = "[ERROR] ";

    private final Messager messager;

    public Printer(Messager messager) {
        this.messager = messager;
    }

    public void info(String msg) {
        String message = INFO_TAG + msg;
        out(message, Diagnostic.Kind.NOTE);
    }

    public void warning(String msg) {
        String message = WARNING_TAG + msg;
        out(message, Diagnostic.Kind.WARNING);
    }

    public void error(String msg) {
        String message = ERROR_TAG + msg;
        out(message, Diagnostic.Kind.ERROR);
    }

    private void out(String msg, Diagnostic.Kind kind) {
        messager.printMessage(kind, msg);
        if (kind == Diagnostic.Kind.ERROR) {
            System.err.println(msg);
        } else {
            System.out.println(msg);
        }
    }
}
