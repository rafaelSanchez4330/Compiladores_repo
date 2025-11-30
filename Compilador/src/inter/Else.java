package inter;
import simbolos.*;

public class Else extends Instr{
    Expr expr;
    Instr instr1;
    Instr instr2;

    public Else(Expr x, Instr s1, Instr s2) {
        this.expr = x;
        this.instr1 = s1;
        this.instr2 = s2;
        
        if(expr.tipo != Tipo.Bool)
            expr.error("se requiere booleano en if");
    }
    
    public void gen(int b, int a){
        int etiqueta1 = nuevaEtiqueta();    //etiqueta1 para instr1
        int etiqueta2 = nuevaEtiqueta();    //etiqueta2 para instr2
        expr.salto(0, etiqueta2);           //pasa hacia instr1 en true
        emitirEtiqueta(etiqueta1);
        instr1.gen(etiqueta1, a);
        emitir("goto L" + a);
        emitirEtiqueta(etiqueta2);
        instr2.gen(etiqueta2, a);
    }
}
