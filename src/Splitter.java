import java.util.ArrayList;
import java.util.List;

public class Splitter
{
    Bus in;
    List<BusConnection> out;


    public Splitter(Bus in)
    {
        this.in = in;
        out = new ArrayList<>();
    }

    class BusConnection
    {
        Bus bus;
        int start;
        int qty;

        public BusConnection(int start, int qty, Bus bus)
        {
            this.bus = bus;
            this.start = start;
            this.qty = qty;
        }
    }

    /**
     * Aggiunge un bus che prende i dati dal bus in entrata, dal bit start, per una quantità di size bit
     * @param start il primo bit da collegare dal bus in entrata al bus in uscita
     * @param size la quantità di bit da collegare dal bus in entrata al bus in uscita
     * @param newBus il bus da collegare allo splitter
     */
    public void add(int start, int size, Bus newBus)
    {
        out.add(new BusConnection(start, size, newBus));
    }

    /**
     * Lo splitter propaga il segnale nei sottobus
     */
    public void act()
    {
        for(BusConnection busConn : out)                     //Per ogni bus busConn collegato alo shifter
            for(int i = 0; i < busConn.qty; i++)             //Per ogni bit i del bus busConn
                busConn.bus.set(i, in.get(busConn.start+i)); //Setta il bit i del busConn col bit busConn.start+i del bus in entrata
    }
}
