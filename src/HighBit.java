public class HighBit
{
    Bus ANZ, JNZ, out;

    public HighBit(Bus ANZ, Bus JNZ, Bus out)
    {
        this.ANZ = ANZ;
        this.JNZ = JNZ;
        this.out = out;
    }

    /**
     * Effettua il calcolo del circuito high bit
     */
    public void act()
    {
        //L'output è vero se sia Z che JAMZ sono asseriti o se sia N che JAMN sono asseriti
        out.set(0, (ANZ.get(1) && JNZ.get(1)) || (ANZ.get(0) && JNZ.get(0)));
    }
}
