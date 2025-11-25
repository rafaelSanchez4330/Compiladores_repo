package main;            //Archivo  Main.java
import java.io.*;
public class Main{
    public static void main(String[] args) throws IOException{
        AnlizadorLexico lex = new AnalizadorLexico();
        Analizador analizar = new Analizador(lex);
        analizar.programa();
        System.out.write('\n');
    }
}
