package io.moyada.feign.help.support;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * @author xueyikang
 * @since 1.0
 **/
public class Printer {

    /**
     *
     # LOG_CLRSTR_NONE = "\033[m"
     # LOG_CLRSTR_RED = "\033[0;32;31m"
     # LOG_CLRSTR_GREEN = "\033[0;32;32m"
     # LOG_CLRSTR_YELLOW = "\033[1;33m"
     # LOG_CLRSTR_CYAN = "\033[0;36m"
     # LOG_CLRSTR_BROWN = "\033[0;33m"
     # LOG_CLRSTR_WHITE = "\033[1;37m"
     */

    private static final String TAG_RED     =   "\033[31;4m";
    private static final String TAG_GREEN   =   "\033[32;4m";
    private static final String TAG_YELLOW  =   "\033[33;4m";
    private static final String TAG_CYAN    =   "\033[34;4m";
    private static final String TAG_END     =   "\033[0m";

    private static final String INFO_TAG    =   "["+TAG_CYAN+"INFO"+TAG_END+"] ";
    private static final String WARNING_TAG =   "["+TAG_YELLOW+"WARNING"+TAG_END+"] ";
    private static final String ERROR_TAG   =   "["+TAG_RED+"ERROR"+TAG_END+"] ";

    public static void main(String[] args) {
        System.out.println(INFO_TAG);
        System.out.println(WARNING_TAG);
        System.out.println(ERROR_TAG);
    }

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
