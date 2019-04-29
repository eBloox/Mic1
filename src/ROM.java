import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;

public class ROM
{
    int wordSize, wordQty;
    Bus address, data;
    Word[] words;

    public ROM(int wordSize, int wordQty, Bus address, Bus data)
    {
        this.wordSize = wordSize;
        this.wordQty = wordQty;

        this.address = address;
        this.data = data;
    }

    public void read()
    {
        data.set(words[address.getWord().toInt()]);
    }

    public boolean checkIfValid(String MIC1File)
    {
        Path p = FileSystems.getDefault().getPath("", MIC1File);
        try(ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(p)))
        {
            //Se i magic numbers corrispondono, il file è valido
            return (in.read() == 0x12 && in.read() == 0x34 && in.read() == 0x56 && in.read() == 0x78);
        }
        catch(IOException e) { return false; }
    }

    /**
     * Inizializza la memoria di controllo caricando il file mic1
     * @param MIC1File il file mic1 da caricare
     * @return restituisce true se il file è stato caricato correttamente
     */
    public boolean init(String MIC1File)
    {
        words = new Word[wordQty];
        for(int i = 0; i < wordQty; i++)
            words[i] = new Word(wordSize);

        Path p = FileSystems.getDefault().getPath("", MIC1File);
        try
        {
            int i = 0;
            int k = 0;
            ByteArrayInputStream in = new ByteArrayInputStream(Files.readAllBytes(p));

            //Se i magic numbers non corrispondono con quelli di un file mic1 valido, restituisce false
            if(in.read() != 0x12 || in.read() != 0x34 || in.read() != 0x56 || in.read() != 0x78)
                return false;

            long inbyte;
            while((inbyte = in.read()) != -1)
            {
                //Salva in value 5 byte alla volta (la grandezza di una microistruzione
                long value = (inbyte << 32) + ((long)in.read() << 24) + ((long)in.read() << 16) + ((long)in.read() << 8) + ((long)in.read());

                words[i].set(value); //Salva la microistruzione
                i++;
            }
        }
        catch(IOException e) {}

        return true;
    }
}
