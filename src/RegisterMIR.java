public class RegisterMIR extends Register
{
    public RegisterMIR(int size, Bus write, Bus read, Bus controlWr, Bus controlRd, Mic1Form form, String field)
    {
        super(size, write, read, controlWr, controlRd, form, field);
    }

    /**
     * Scrive nel suo bus di uscita trocando i 4 bit meno significativi
     */
    @Override
    protected void write()
    {
        word.set(write.getWord().getShift(true, 4));
        updateForm();
    }
}
