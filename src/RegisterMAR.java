public class RegisterMAR extends Register
{
    BusMemory memory;

    public RegisterMAR(int size, Bus write, Bus controlWr, BusMemory memory, Mic1Form form, String field)
    {
        super(size, write, null, controlWr, null, form, field);

        this.memory = memory;
    }

    /**
     * Carica il valore del registro nel bus memory shiftato di due (usando la linea address) se la linea di
     * controllo write o la linea di controllo read è asserita
     *
     * Il valore viene shiftato di due dato che il valore del registro MAR indica l'indice in termini di parole e non
     * di byte, ma essendo la memoria orientata a byte, dobbiamo convertirlo.
     */
    public void loadToMemory()
    {
        if(memory.getWrf().get(BusMemory.WRITE) || memory.getWrf().get(BusMemory.READ))
            memory.getAddress().set(word.getShift(false, 2));
    }
}
