public class ALU
{
    private int nBit;
    private Bus A, B, control;
    private Bus output;
    private Bus NZ;

    public ALU(int nBit, Bus A, Bus B, Bus control, Bus output, Bus NZ)
    {
        this.nBit = nBit;
        this.control = control; //F0 F1 ENA ENB INVA INC
        this.A = A;
        this.B = B;
        this.output = output;
        this.NZ = NZ;

    }
    public void act()
    {
        Word wordOut;
        Word wordA, wordB;

        if(control.get(3)) //Se ENA è asserito
            wordA = A.getWord();
        else               //Se ENA è negato
            wordA = new Word(nBit);
        if(control.get(2)) //Se ENB è asserito
            wordB = B.getWord();
        else               //Se ENB è negato
            wordB = new Word(nBit);

        if(control.get(1)) //Se INVA è asserito
            wordA = wordA.getNot();

        if(!control.get(5) && !control.get(4))      //Se F0 e F1 sono negati
            wordOut = wordA.getAnd(wordB);          //A AND B
        else if(!control.get(5) && control.get(4))  //Se F0 è negato e F1 è asserito
            wordOut = wordA.getOr(wordB);           //A OR B
        else if(control.get(5) && !control.get(4))  //Se F0 è asserito e F1 è negato
            wordOut = wordA.getNot();               //NOT A
        else                                        //Se F0 e F1 sono negati
            wordOut = wordA.getSum(wordB);          //A + B

        if(control.get(0))  //Se INC è asserito
            wordOut = wordOut.getInc(); //incrementa l'output

        output.set(wordOut);

        NZ.set(1, wordOut.toIntSign() < 0);  //Se il risultato dell'alu è negativo asserisci N
        NZ.set(0, wordOut.toIntSign() == 0); //Se il risultato dell'alu è zero asserisci Z
    }
}
