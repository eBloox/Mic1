import java.util.Iterator;

public class Word
{
    private int size;
    private boolean[] bits;

    public Word(int size)
    {
        this.size = size;
        bits = new boolean[size];
    }

    public Word(boolean[] bits)
    {
        this.size = bits.length;
        this.bits = new boolean[size];

        for(int i = 0; i < size; i++)
        {
            this.bits[i] = bits[i];
        }

    }

    /**
     * Restituisce il bit in posizione i
     * @param i l'indice del bit da restituire
     * @return restituisce il bit in posizione i
     */
    public boolean get(int i)
    {
        return bits[i];
    }

    /**
     * Restituisce la grandezza della parola
     * @return restituisce la grandezza della parola
     */
    public int getSize()
    {
        return size;
    }

    /**
     * Setta il bit all'indice index con il valore value
     * @param index l'indice del bit da settare
     * @param value il valore da assegnare al bit
     */
    public void set(int index, boolean value)
    {
        bits[index] = value;
    }

    /**
     * Assegna ai bit della parola il valore degli indici dell'array di booleani
     * @param data array di booleani da assegnare alla parola
     */
    public void set(boolean[] data)
    {
        for(int i = 0; i < size; i++)
        {
            if(i < data.length)
                bits[i] = data[i];
            else
                bits[i] = false;
        }
    }

    /**
     * Copia i valori della parola data all'interno della parola
     * @param data parola da copiare
     */
    public void set(Word data)
    {
        for (int i = 0; i < bits.length; i++)
        {
            if(i < data.getSize())
                bits[i] = data.get(i);
            else
                bits[i] = false;
        }
    }

    /**
     * Assegna ai bit della parola i bit del valore value interpretato come numero binario
     * @param value il numero da copiare nella parola
     */
    public void set(long value)
    {
        for(int i = 0; i < bits.length; i++)
        {
            //Math.floor(Math.log(value)/Math.log(2.0))+1) restituisce il numero di cifre del numero value interpretato come numero binario
            if(i < Math.floor(Math.log(value)/Math.log(2.0))+1)
                bits[i] = ((value >> i) & 1) != 0;
            else
                bits[i] = false;
        }
    }

    /**
     * Calcola il valore della parola interpretata come numero binario
     * @return restituisce il valore della parola con un long
     */
    public long toLong()
    {
        long result = 0;

        for(int i = 0; i < size; i++)
            if(bits[i])
                result += Math.pow(2,i);

        return result;
    }

    /**
     * Calcola il valore della parola interpretata come numero binario con segno
     * @return restituisce il valore della parola (interpretata come numero con segno) con un long
     */
    public long toLongSign()
    {
        long result = 0;

        if(!bits[size-1])
        {

            for(int i = 0; i < size; i++)
            {
                if(bits[i])
                    result += Math.pow(2,i);
            }
        }
        else
        {
            for(int i = 0; i < size; i++)
            {
                if(!bits[i])
                    result += Math.pow(2,i);
            }
            result *= -1;
            result -= 1;
        }

        return result;
    }

    /**
     * Calcola il valore della parola interpretata come numero binario
     * @return restituisce il valore della parola con un int
     */
    public int toInt()
    {
        return (int)toLong();
    }

    /**
     * Calcola il valore della parola interpretata come numero binario con segno
     * @return restituisce il valore della parola (interpretata come numero con segno) con un int
     */
    public int toIntSign()
    {
        return (int)toLongSign();
    }

    /**
     * Restituice una porzione della parola di dimensione size a partire dal bit in posizione start
     * @param start la posizione da cui la porzione di parole comincia
     * @param size la grandezza della porzione di parola
     * @return restituice una porzione della parola di dimensione size a partire dal bit in posizione start
     */
    public Word getPortion(int start, int size)
    {
        Word result = new Word(size);
        for(int i = 0; i < size; i++)
            result.set(i, bits[start+i]);

        return result;
    }

    /**
     * Calcola l'and bitwise tra la parola stessa e la parola passata via parametro
     * @param b l'operando dell'and
     * @return restituisce una parola ottenuta facendo l'and bitwise tra la parola stessa e la parola passata via parametro
     */
    public Word getAnd(Word b)
    {
        Word result = new Word(size);

        for(int i = 0; i < size; i++)
        {
            if(i < b.getSize())
                result.set(i, this.get(i) && b.get(i));
            else
                result.set(i, this.get(i));
        }

        return result;
    }

    /**
     * Calcola l'or bitwise tra la parola stessa e la parola passata via parametro
     * @param b l'operando dell'or
     * @return restituisce una parola ottenuta facendo l'or bitwise tra la parola stessa e la parola passata via parametro
     */
    public Word getOr(Word b)
    {
        Word result = new Word(size);

        for(int i = 0; i < size; i++)
        {
            if(i < b.getSize())
                result.set(i, this.get(i) || b.get(i));
            else
                result.set(i, this.get(i));
        }

        return result;
    }

    /**
     * Calcola il not bitwise della parola
     * @return restituisce una parola dove ogni bit ha valore opposto a quello iniziale
     */
    public Word getNot()
    {
        Word result = new Word(size);

        for(int i = 0; i < size; i++)
            result.set(i, !this.get(i));

        return result;
    }

    /**
     * Calcola la somma tra la parola stessa e la parola passata via parametro
     * @param b l'operando della somma
     * @return restituisce una parola ottenuta facendo la somma tra la parola stessa e la parola passata via parametro
     */
    public Word getSum(Word b)
    {
        boolean carry = false;
        Word result = new Word(size);

        for(int i = 0; i < size; i++)
        {
            result.set(i, this.get(i) ^ b.get(i) ^ carry);
            carry = (this.get(i) && b.get(i)) || (this.get(i) && carry) || (b.get(i) && carry);
        }

        return result;
    }

    /**
     * Calcola la somma tra la parola e 1
     * @return restituisce una parola ottenuta incrementando la parola originale
     */
    public Word getInc()
    {
        Word one = new Word(size);
        one.set(0, true);

        return this.getSum(one);
    }

    /**
     * Fa uno shift a destra o a sinistra della parola di num posizioni
     * @param right true se lo shift deve essere fatto a destra, false se lo shift deve essere fatto a sinistra
     * @param num il numero di posizioni da shiftare
     * @return restituisce una parola ottenuta facendo lo shift della parola a destra o a sinistra di num posizioni
     */
    public Word getShift(boolean right, int num)
    {
        Word result = new Word(size);

        if(right)
            for (int i = num; i < size; i++)
                result.set(i-num, this.get(i));
        else
            for (int i = 0; i < size-num; i++)
                result.set(i+num, this.get(i));

        return result;
    }
}