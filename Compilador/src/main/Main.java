package main;
import java.io.*;
import analizadorLexico.*;
import analizador.*;

public class Main {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        AnalizadorLexico lex = new AnalizadorLexico();
        Analizador analizar = new Analizador(lex);
        analizar.programa();
        System.out.println("\n");
    }
    
}
