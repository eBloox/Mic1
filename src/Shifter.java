public class Shifter
{
    private Bus input, control;
    private Bus output;

    public Shifter(Bus input, Bus control, Bus output)
    {
        this.input = input;
        this.control = control;
        this.output = output;
    }

    /**
     * Shifta il valore del bus input e lo inserisce nel bus output
     */
    public void act()
    {
        Word wordOut = input.getWord();

        if(control.get(1)) //Se la linea di controllo shift left è asserita
            wordOut = wordOut.getShift(false, 8); //Shifta a sinistra di 8 bit
        if(control.get(0)) //Se la linea di controllo shift right è asserita
            wordOut = wordOut.getShift(true, 1);  //Shifta a destra di 1 bit
        output.set(wordOut);
    }
}
