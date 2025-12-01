package simbolos;
import java.util.*;
import analizadorLexico.*;
import inter.*;

public class Env {
    private Hashtable tabla;
    protected Env prev;
    public Env(Env n) {
        tabla = new Hashtable();
        prev = n;
    }
    public void put(Token w, Id i) {
        tabla.put(w, i);
    }
    public Id get(Token w) {
        for(Env e = this; e != null; e = e.prev) {
            Id found = (Id)(e.tabla.get(w));
            if(found != null) return found;
        }
        return null;
    }
}
