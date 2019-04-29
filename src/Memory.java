import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class Memory
{
    private int nAddr;
    private int wordSize;
    private Word[] words;
    private BusMemory bus;

    boolean write, read, fetch;

    Word addr, data, fetchAddr;

    public Memory(int wordSize, int nAddr, BusMemory bus)
    {
        this.nAddr = nAddr;
        this.wordSize = wordSize;
        this.bus = bus;
    }

    /**
     * Effettua le operazioni di lettura/scrittura/fetch o si prepara ad effettuarle per il ciclo successivo
     */
    public void act()
    {
        if(write) //Se al ciclo di clock precedente è iniziata un'operazione di scrittura
        {
            for(int i = 0; i < wordSize; i++) //Per ogni byte che compone una parola
            {
                //Scrive nella memoria i dati del bus partendo dall'ultimo byte fino al primo (little endian a big endian)
                words[addr.toInt()+ 4-(i+1)].set(data.getPortion(i*8, 8));
            }
            write = false;
        }
        if(bus.getWrf().get(BusMemory.WRITE)) //Se al ciclo di clock attuale va iniziata un'operazione di scrittura
        {
            write = true;
            addr = bus.getAddress().getWord(); //Salva l'indirizzo in cui scrivere i dati
            data = bus.getData().getWord();    //Salva i dati da scrivere
        }

        if(read) //Se al ciclo di clock precedente è iniziata un'operazione di lettura
        {
            for(int i = 0; i < wordSize; i++) //Per ogni byte che compone una parola
            {
                //Scrive nel bus i byte partendo dall'ultimo fino al prima (little endian a big endian)
                bus.getData().setPortion(i*8, words[addr.toInt()+4-(i+1)]);
            }
            bus.getRdyTransfer().set(BusMemory.READYREAD, true);
            read = false;
        }
        if(bus.getWrf().get(BusMemory.READ)) //Se al ciclo di clock attuale va iniziata un'operazione di lettura
        {
            read = true;
            addr = bus.getAddress().getWord(); //Salva l'indirizzo della parola da leggere
        }

        if(fetch) //Se al ciclo di clock precedente è iniziata un'operazione di fetch
        {
            bus.getFetchData().set(words[fetchAddr.toInt()]); //Scrive nel bus dei dati fetch i dati presenti nell'address richiesto

            bus.getRdyTransfer().set(BusMemory.READYFETCH, true);
            fetch = false;
        }
        if(bus.getWrf().get(BusMemory.FETCH)) //Se al ciclo di clock attuale va iniziata un'operazione di fetch
        {
            fetch = true;
            fetchAddr = bus.getFetchAddress().getWord(); //Salva l'indirizzo del byte da leggere
        }
    }

    /**
     * Nega le linee di controllo readyread e readyfetch
     */
    public void resetReady()
    {
        bus.getRdyTransfer().set(BusMemory.READYREAD, false);
        bus.getRdyTransfer().set(BusMemory.READYFETCH, false);
    }

    /**
     * Controlla se il file IJVM è valido
     * @param IJVMFile il file da controllare
     * @return restituisce true se il file è valido
     */
    public boolean checkIfValid(String IJVMFile)
    {
        Path p = FileSystems.getDefault().getPath("", IJVMFile);
        try(ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(p)))
        {
            //Se i magic numbers corrispondono, il file è valido
            return (in.read() == 0x1D && in.read() == 0xEA && in.read() == 0xDF && in.read() == 0xAD);
        }
        catch(IOException e) { return false; }
    }

    /**
     * Inizializza la memoria caricando il file IJVMFile
     * @param IJVMFile il file da caricare in memoria
     * @return restituisce true se l'operazione è andata a buon fine
     */
    public boolean init(String IJVMFile)
    {
        words = new Word[nAddr];
        for(int i=0; i < nAddr; i++)
            words[i] = new Word(8);

        Path p = FileSystems.getDefault().getPath("", IJVMFile);
        try
        {
            int inbyte;
            int origin;
            int blocksize;

            ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(p));

            //Se i magic numbers non corrispondono con quelli di un file ijvm valido, restituisce false
            if(in.read() != 0x1D || in.read() != 0xEA || in.read() != 0xDF || in.read() != 0xAD)
                return false;


            while((inbyte = in.read()) != -1) //Fino a che il file non è finito
            {
                //Salva i primi 4 byte in origin
                origin = (inbyte << 24) + (in.read() << 16) + (in.read() << 8) + (in.read());
                //Salva i successivi 4 byte in blocksize
                blocksize = (in.read() << 24) + (in.read() << 16) + (in.read() << 8) + (in.read());

                //Scrive i byte del blocco nella memoria, partendo dall'indirizzo origin
                for (int j = 0; j < blocksize; j++)
                {
                    words[origin+j].set(in.read());
                }
            }
        }
        catch(IOException e) {}

        return true;
    }
}