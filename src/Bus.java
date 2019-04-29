public class Bus
{
    boolean[] bits;

    public Bus(int size)
    {
        this.bits = new boolean[size];
    }

    /**
     * Setta il bit all'indice index col valore val
     * @param index l'indice del bit da modificare
     * @param val il valore da dare al bit
     */
    public void set(int index, boolean val)
    {
        bits[index] = val;
    }

    /**
     * Inserisci la parola word all'interno del bus
     * @param word la parola da inserire nel bus
     */
    public void set(Word word)
    {
        for(int i = 0; i < bits.length; i++) //Per ogni bit del bus
        {
            if(i < word.getSize())      //Se il bit i è presente nella parola
                bits[i] = word.get(i);  //Usa il valore di quel bit
            else                        //Se il bit i non è presente nella parola (quindi la parola è più piccola del bus)
                bits[i] = false;        //Setta il bit a false
        }
    }

    /**
     * Inserisce il valore value all'interno del bus
     * @param value il valore da inserire nel bus
     */
    public void set(long value)
    {
        for(int i = 0; i < bits.length; i++) //Per ogni bit nel bus
        {
            /*
             * Math.floor(Math.log(value)/Math.log(2.0)+1) restituisce il numero di cifre binarie di value
             */
            if(i < Math.floor(Math.log(value)/Math.log(2.0))+1) //Se il bit i è un bit rilevante nel valore value
                bits[i] = ((value >> i) & 1) != 0;              //Assegna al bit i il valore dell'i-esimo bit del valore value
            else                                                //Alrimenti
                bits[i] = false;                                //Assegna al bit i il valore 0
        }
    }

    /**
     * Inserisce la parola data nel bus a partire dall'indice start
     * @param start l'indice del bus in cui iniziare a copiare la parola
     * @param word la parola da inserire nel bus
     */
    public void setPortion(int start, Word word)
    {
        for(int i = 0; i < word.getSize(); i++) //Per ogni bit i della parola
        {
            if(i+start < bits.length)           //Se il bit i sta nella parola
                bits[i+start] = word.get(i);    //Inserisce il bit nella posizione i+start
        }
    }

    /**
     * Restituisce il bit alla posizione index
     * @param index l'indice del bit da restituire
     * @return restituisce il bit alla posizione index
     */
    public boolean get(int index)
    {
        return bits[index];
    }

    /**
     * Restituisce una copia della parola presente nel bus
     * @return restituisce una copia della parola presente nel bus
     */
    public Word getWord()
    {
        return new Word(bits);
    }

    /**
     * Restituisce la grandezza del bus
     * @return restituisce la grandezza del bus
     */
    public int getSize()
    {
        return bits.length;
    }
}
