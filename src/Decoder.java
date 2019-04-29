public class Decoder
{
    int bit;
    Bus in, out;

    public Decoder(int bit, Bus in, Bus out)
    {
        this.bit = bit;
        this.in = in;
        this.out = out;
    }

    /**
     * Il decodificatore asserisce il bit in uscita con indice corrispondente al valore del bus in entrata interpretato come numero binario
     */
    public void act()
    {
        //Crea una parola dove l'unico bit uguale a 1 è il bit alla posizione data dal valore del bus in interpretato come numero binario
        Word wordOut = new Word((int)Math.pow(2, bit));
        wordOut.set(in.getWord().toInt(), true);

        out.set(wordOut);
    }
}
