public class Register
{
    protected int size;
    protected Word word;
    protected Bus read, write, controlWr, controlRd;
    protected String field;
    protected Mic1Form mic1Form;

    public Register(int size, Bus write, Bus read, Bus controlWr, Bus controlRd, Mic1Form form, String field)
    {
        this.field = field;
        this.mic1Form = form;
        this.size = size;

        this.word = new Word(size);

        this.write = write;
        this.read = read;
        this.controlWr = controlWr;
        this.controlRd = controlRd;

        updateForm();
    }

    /**
     * Copia il valore del registro nel bus read
     */
    protected void read()
    {
        read.set(word);
    }

    /**
     * Copia il valore del bus write nel registro
     */
    protected void write()
    {
        word.set(write.getWord());
        updateForm();
    }

    /**
     * Se la linea di controllo per la scrittura è asserita, scrive il valore del bus write nel registro
     */
    public void store()
    {
        if(controlWr.get(0))
            write();
    }

    /**
     * Se la linea di controllo per la lettura è asserita, legge il valore del registro nel bus read
     */
    public void load()
    {
        if(controlRd.get(0))
            read();
    }

    /**
     * Forza il registro ad assumere il valore value
     * @param value il valore che il registro deve assmuere
     */
    public void force(long value)
    {
        word.set(value);
        updateForm();
    }

    /**
     * Aggiorna il form del registro scrivendo il proprio valore in esadecimale
     */
    protected void updateForm()
    {
        mic1Form.setRegister(field, String.format("0x%08X",word.toInt()));
    }
}