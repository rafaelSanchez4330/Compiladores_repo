package analizador;
import java.io.*;
import analizadorLexico.*;
import simbolos.*;
import inter.*;

public class Analizador {
    private AnalizadorLexico lex;
    private Token busca;
    Env top = null;
    int usado = 0;

    public Analizador(AnalizadorLexico l) throws IOException {
        lex = l;
        mover();
    }

    void mover() throws IOException {
        busca = lex.escanear();
    }

    void error(String s) {
        throw new Error("cerca de linea " + AnalizadorLexico.linea + ": " + s);
    }

    void coincidir(int t) throws IOException {
        if(busca.etiqueta == t) mover();
        else error("error de sintaxis");
    }

    public void programa() throws IOException {
        Instr s = bloque();
        int inicio = s.nuevaEtiqueta();
        int despues = s.nuevaEtiqueta();
        s.emitirEtiqueta(inicio);
        s.gen(inicio, despues);
        s.emitirEtiqueta(despues);
    }

    Instr bloque() throws IOException {
        coincidir('{');
        Env envGuardado = top;
        top = new Env(top);
        declaraciones();
        Instr s = instrucciones();
        coincidir('}');
        top = envGuardado;
        return s;
    }

    void declaraciones() throws IOException {
        while(busca.etiqueta == Etiqueta.BASIC) {
            Tipo p = tipo();
            Token tok = busca;
            coincidir(Etiqueta.ID);
            coincidir(';');
            Id id = new Id((Palabra)tok, p, usado);
            top.put(tok, id);
            usado = usado + p.ancho;
        }
    }

    Tipo tipo() throws IOException {
        Tipo p = (Tipo)busca;
        coincidir(Etiqueta.BASIC);
        if(busca.etiqueta != '[') return p;
        else return dims(p);
    }

    Tipo dims(Tipo p) throws IOException {
        coincidir('[');
        Token tok = busca;
        coincidir(Etiqueta.NUM);
        coincidir(']');
        if(busca.etiqueta == '[')
            p = dims(p);
        return new Arreglo(((Num)tok).valor, p);
    }

    Instr instrucciones() throws IOException {
        if(busca.etiqueta == '}') return Instr.Null;
        else return new Sec(instruccion(), instrucciones());
    }

    Instr instruccion() throws IOException {
        Expr x; Instr s1, s2; Instr instrGuardada; 
        switch(busca.etiqueta) {
            case ';':
                mover();
                return Instr.Null;
            case Etiqueta.IF:
                coincidir(Etiqueta.IF); coincidir('('); x = bool(); coincidir(')');
                s1 = instruccion();
                if(busca.etiqueta != Etiqueta.ELSE) return new If(x, s1);
                coincidir(Etiqueta.ELSE);
                s2 = instruccion();
                return new Else(x, s1, s2);
            case Etiqueta.WHILE:
                While nodowhile = new While();
                instrGuardada = Instr.Circundante; Instr.Circundante = nodowhile;
                coincidir(Etiqueta.WHILE); coincidir('('); x = bool(); coincidir(')');
                s1 = instruccion();
                nodowhile.inic(x, s1);
                Instr.Circundante = instrGuardada;
                return nodowhile;
            case Etiqueta.DO:
                Do nododo = new Do();
                instrGuardada = Instr.Circundante; Instr.Circundante = nododo;
                coincidir(Etiqueta.DO);
                s1 = instruccion();
                coincidir(Etiqueta.WHILE); coincidir('('); x = bool(); coincidir(')'); coincidir(';');
                nododo.inic(s1, x);
                Instr.Circundante = instrGuardada;
                return nododo;
            case Etiqueta.BREAK:
                coincidir(Etiqueta.BREAK); coincidir(';');
                return new Break();
            case '{':
                return bloque();
            default:
                return asignacion();
        }
    }

    Instr asignacion() throws IOException {
        Instr instr; Token t = busca;
        coincidir(Etiqueta.ID);
        Id id = top.get(t);
        if(id == null) error(t.toString() + " no declarado");
        if(busca.etiqueta == '=') {
            mover(); instr = new Est(id, bool());
        } else {
            Acceso x = acceso(id);
            coincidir('='); instr = new EstElem(x, bool());
        }
        coincidir(';');
        return instr;
    }

    Expr bool() throws IOException {
        Expr x = unir();
        while(busca.etiqueta == Etiqueta.OR) {
            Token tok = busca; mover(); x = new Or(tok, x, unir());
        }
        return x;
    }

    Expr unir() throws IOException {
        Expr x = igualdad();
        while(busca.etiqueta == Etiqueta.AND) {
            Token tok = busca; mover(); x = new And(tok, x, igualdad());
        }
        return x;
    }

    Expr igualdad() throws IOException {
        Expr x = rel();
        while(busca.etiqueta == Etiqueta.EQ || busca.etiqueta == Etiqueta.NE) {
            Token tok = busca; mover(); x = new Rel(tok, x, rel());
        }
        return x;
    }

    Expr rel() throws IOException {
        Expr x = expr();
        switch(busca.etiqueta) {
            case '<': case Etiqueta.LE: case Etiqueta.GE: case '>':
                Token tok = busca; mover(); return new Rel(tok, x, expr());
            default:
                return x;
        }
    }

    Expr expr() throws IOException {
        Expr x = termino();
        while(busca.etiqueta == '+' || busca.etiqueta == '-') {
            Token tok = busca; mover(); x = new Arit(tok, x, termino());
        }
        return x;
    }

    Expr termino() throws IOException {
        Expr x = unario();
        while(busca.etiqueta == '*' || busca.etiqueta == '/') {
            Token tok = busca; mover(); x = new Arit(tok, x, unario());
        }
        return x;
    }

    Expr unario() throws IOException {
        if(busca.etiqueta == '-') {
            mover(); return new Unario(Palabra.minus, unario());
        } else if(busca.etiqueta == '!') {
            Token tok = busca; mover(); return new Not(tok, unario());
        } else return factor();
    }

    Expr factor() throws IOException {
        Expr x = null;
        switch(busca.etiqueta) {
            case '(':
                mover(); x = bool(); coincidir(')');
                return x;
            case Etiqueta.NUM:
                x = new Constante(busca, Tipo.Int); mover(); return x;
            case Etiqueta.REAL:
                x = new Constante(busca, Tipo.Float); mover(); return x;
            case Etiqueta.TRUE:
                x = Constante.True; mover(); return x;
            case Etiqueta.FALSE:
                x = Constante.False; mover(); return x;
            case Etiqueta.ID:
                String s = busca.toString();
                Id id = top.get(busca);
                if(id == null) error(busca.toString() + " no declarado");
                mover();
                if(busca.etiqueta != '[') return id;
                else return acceso(id);
            default:
                error("error de sintaxis");
                return x;
        }
    }

    Acceso acceso(Id a) throws IOException {
        Expr i; Expr ancho; Expr t1, t2; Expr loc;
        Tipo p = a.tipo;
        coincidir('['); i = bool(); coincidir(']');
        p = ((Arreglo)p).de;
        ancho = new Constante(p.ancho);
        t1 = new Arit(new Token('*'), i, ancho);
        loc = t1;
        while(busca.etiqueta == '[') {
            coincidir('['); i = bool(); coincidir(']');
            p = ((Arreglo)p).de;
            ancho = new Constante(p.ancho);
            t1 = new Arit(new Token('*'), i, ancho);
            t2 = new Arit(new Token('+'), loc, t1);
            loc = t2;
        }
        return new Acceso(a, loc, p);
    }
}
