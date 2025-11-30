package inter;
import simbolos.*;
public class While extends Instr {
    Expr expr;
    Instr instr;
    public While(){
        expr = null;
        instr = null;
    }
    
    public void inic(Expr x, Instr s){
        expr = x;
        instr = s;
        if(expr.tipo != Tipo.Bool)
            expr.error("se requiere booleano en while");
    }
    
    public void gen(int b, int a){
        despues = a;                    //guarda la etiqueta a
        expr.salto(0, a);
        int etiqueta = nuevaEtiqueta(); //etiqueta para instr
        emitirEtiqueta(etiqueta);
        instr.gen(etiqueta, b);
        emitir("goto L"+b);
    }
}
