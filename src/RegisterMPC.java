public class RegisterMPC
{
    Bus highBit, addrOr, out;

    public RegisterMPC(Bus highBit, Bus addrOr, Bus out)
    {
        this.highBit = highBit;
        this.addrOr = addrOr;
        this.out = out;
    }

    /**
     * Calcola il valore dell'mpc mettendo in or il bus addror e il bus highbit (usato come bit più alto)
     */
    public void act()
    {
        Word result = new Word(out.getSize());
        result.set(addrOr.getWord());
        result.set(result.getSize()-1, result.get(result.getSize()-1) || highBit.get(0));
        out.set(result);
    }

}
