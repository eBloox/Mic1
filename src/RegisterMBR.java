public class RegisterMBR extends Register
{
    Bus busAddrMPC;
    BusMemory memory;

    public RegisterMBR(int size, Bus read, Bus controlRd, BusMemory memory, Bus busAddrMPC, Mic1Form form, String field)
    {
        super(size, null, read, null, controlRd, form, field);

        this.busAddrMPC = busAddrMPC;
        this.memory = memory;
    }

    /**
     * Legge il valore del registro interpretato come numero intero senza segno
     */
    private void readU()
    {
        read.set(word);
    }

    /**
     * Legge il valore del registro interpretato come numero intero con segno
     */
    private void readS()
    {
        Word signed = new Word(read.getSize());
        boolean sign = word.get(size-1);

        //Copia la parola nel bus
        signed.set(word);

        //Copia il valore dell'ultimo bit del registro in tutti gli altri bit del bus
        for(int i = size; i < signed.getSize(); i++)
            signed.set(i, sign);

        read.set(signed);
    }

    /**
     * Se la linea di controllo per la lettura (MBR O MBRU) è asserita, legge il valore del registro nel bus read
     * (interpretato come intero con o senza segno)
     */
    @Override
    public void load()
    {
        if(controlRd.get(1)) //Se la linea di controllo per la lettura di MBR è asserita
            readS();         //Carica il valore del registro interpretato come intero col segno nel bus
        if(controlRd.get(0)) //Se la linea di controllo per la lettura di MBRU è asserita
            readU();
        busAddrMPC.set(word); //Copia il valore del registro nel bus busAddrMPC (usato per il calcolo della microistruzione successiva)
    }

    /**
     * Salva il valore del bus memory (linea dati fetch) nel registro se la linea di ontrollo readyfetch è asserita
     */
    public void storeFromMemory()
    {
        if(memory.getRdyTransfer().get(BusMemory.READYFETCH))
        {
            word.set(memory.getFetchData().getWord());
            updateForm();
        }
    }

    @Override
    protected void updateForm()
    {
        mic1Form.setRegister(field, String.format("0x%02X",word.toInt()));
    }
}
