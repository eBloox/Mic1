public class RegisterPC extends Register
{
    BusMemory memory;

    public RegisterPC(int size, Bus write, Bus read, Bus controlWr, Bus controlRd, BusMemory memory, Mic1Form form, String field)
    {
        super(size, write, read, controlWr, controlRd, form, field);

        this.memory = memory;
    }

    /**
     * Carica il valore del registro nel bus memory (usando la linea dati fetch) se la linea di controllo fetch è asserita
     */
    public void loadToMemory()
    {
        if(memory.getWrf().get(BusMemory.FETCH))
            memory.getFetchAddress().set(word);
    }
}
