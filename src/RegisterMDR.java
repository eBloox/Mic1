public class RegisterMDR extends Register
{
    BusMemory memory;

    public RegisterMDR(int size, Bus write, Bus read, Bus controlWr, Bus controlRd, BusMemory memory, Mic1Form form, String field)
    {
        super(size, write, read, controlWr, controlRd, form, field);

        this.memory = memory;
    }

    /**
     * Salva il valore del bus memory (linea dati fetch) nel registro se la linea di controllo readyread è asserita
     */
    public void storeFromMemory()
    {
        if (memory.getRdyTransfer().get(BusMemory.READYREAD))
        {
            word.set(memory.getData().getWord());
            updateForm();
        }
    }

    /**
     * Carica il valore del registro nel bus memory (usando la linea dati) se la linea di controllo write è asserita
     */
    public void loadToMemory()
    {
        if(memory.getWrf().get(BusMemory.WRITE))
            memory.getData().set(word);
    }
}
