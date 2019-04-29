import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MIC1Assembler
{
    Path MALFile, MIC1File;
    Map<String, Integer> labels;             //Hashmap per mappare alcuni nomi di istruzioni con la loro posizione nella memoria
    MALInstruction defaultInstr;             //Istruzione di default usata in ogni locazione vuota
    List<MALInstruction> instructions;       //Lista di istruzioni in ordine di scrittura enl file
    Map<String, MALInstruction> nameToInstr; //Hashmap per mappare i nomi delle istruzzioni alle istruzioni stesse
    MALInstruction[] locations;              //Array di parole dove a ogni indice i corrisponde l'istruzione che verrà scritta alla posizione i
    Map<String, String> ifElsePairs;         //Coppie di nomi di istruzioni usate per i goto biforcati if else

    LineNumberReader bufferedReader;

    int NOREG = 9;

    //Intestazione del file
    int     magic1=0x12,
            magic2=0x34,
            magic3=0x56,
            magic4=0x78;

    public MIC1Assembler(String MALFile, String MIC1File)
    {
        this.MIC1File = Paths.get(MIC1File);
        this.MALFile = Paths.get(MALFile);
        labels = new HashMap<>();
        instructions = new ArrayList<>();
        ifElsePairs = new HashMap<>();
        nameToInstr = new HashMap<>();
        locations = new MALInstruction[512];
    }

    public void parse() throws MIC1Exception
    {
        try
        {
            String line;

            this.bufferedReader = new LineNumberReader(new FileReader(MALFile.toString()));
            while ((line = bufferedReader.readLine()) != null)
            {
                line = line.replaceFirst("(\\/\\/.*)", ""); //Elimina i commenti
                if(!line.isBlank())
                {
                    String words[] = line.split("\\s+");

                    //Se l'istruzione inizia con label, deve essere di formato .label NOMEISTRUZIONE POSIZIONEISTRUZIONE, e indica dove verrà salvata l'istruzione
                    if (words[0].equals(".label"))
                    {
                        if (words.length != 3) throw new MIC1Exception(bufferedReader.getLineNumber(),
                                "L'istruzione label non rispetta la sintassi .label NOMEISTRUZIONE POSIZIONEISTRUZIONE");
                        if (labels.containsKey(words[1])) throw new MIC1Exception(bufferedReader.getLineNumber(),
                                "L'istruzione inserita è già presente in un altro .label");
                        if (labels.containsValue(Integer.decode(words[2]))) throw new MIC1Exception(bufferedReader.getLineNumber(),
                                "La posizione utilizzata è già stata usata in precedenza");
                        labels.put(words[1], Integer.decode(words[2]));
                    }
                    //Se l'istruzione inizia con default indica che istruzione va inserita in tutte le parole non usate
                    else if (words[0].equals(".default"))
                    {
                        defaultInstr = parseMALInstruction(line.substring((".default").length()).trim());
                    }
                    //Se la riga non inizia con .label o .default si tratta di una microistruzione
                    else
                    {
                        MALInstruction instr = parseMALInstruction(line.substring(words[0].length()).trim());
                        instr.name = words[0];
                        nameToInstr.put(words[0], instr);
                        if(instr.nextIf != null) //Se l'istruzione contiene un salto condizionale
                            ifElsePairs.put(instr.nextIf, instr.nextElse); //Salva le destinazioni nella mappa ifelsepair

                        if(labels.containsKey(words[0])) //Se all'istruzione è stata assegnata una posizione
                        {
                            instr.address = labels.get(words[0]);    //Salva la posizione all'interno dell'istruzione
                            locations[labels.get(words[0])] = instr; //Salva l'istruzione alla posizione assegnata delle locazioni
                        }
                        instructions.add(instr);
                    }
                }
            }
            assignLocations(); //Assegna le locazioni di memoria alle istruzioni
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Assegna le locazioni di memoria alle istruzioni
     */
    public void assignLocations()
    {
        int currentFreeLocation = 0, currentPairFreeLocation = 0;

        /*
         * Per ogni copia di istruzioni usate in biforcazioni, salva l'else nella prima locazione di memoria disponibile,
         * e l'if alla stessa posizione + 0x100 (entrambe devono essere libere, altrimenti deve essere scelta una posizione
         * differente). Questo viene fatto per via del funzionamento dei salti condizionali all'interno delle microistruzioni,
         * infatti, l'unico modo per saltare da un'istruzione all'altra è usando il jamn e il jamz, che possono solamente
         * assegnare il valore 1 o no al 9° bit dell'mpc
         */
        for (Map.Entry<String, String> ifElsePair : ifElsePairs.entrySet())
        {
            //Cerca una locazione tale che sia la locazione stessa che la locazione a quella posizione+0x100 siano libere
            while(locations[currentPairFreeLocation] != null || locations[currentPairFreeLocation+0x100] != null) currentPairFreeLocation++;
            nameToInstr.get(ifElsePair.getKey()).address = currentPairFreeLocation+0x100;
            locations[currentPairFreeLocation+0x100] = nameToInstr.get(ifElsePair.getKey());
            nameToInstr.get(ifElsePair.getValue()).address = currentPairFreeLocation;
            locations[currentPairFreeLocation] = nameToInstr.get(ifElsePair.getValue());
        }

        for (MALInstruction instruction : instructions) //Per ogni microistruzione
        {
            while(locations[currentFreeLocation] != null) currentFreeLocation++; //Cerca una locazione libera

            if(instruction.address == -1) //Se la microistruzione non è stata ancora salvata
            {
                //Assegna all'istruzione un indirizzo e la salva nell'array delle locazioni
                instruction.address = currentFreeLocation;
                locations[currentFreeLocation] = instruction;
            }
        }

        //Per ogni locazione, se la locazione dopo l'assegnamento è ancora vuota, assegna l'istruzione di default
        for (int i = 0; i < locations.length; i++)
        {
            if(locations[i] == null)
                locations[i] = defaultInstr;
        }

        for (int i = 0; i < instructions.size(); i++) //Per ogni istruzione
        {
            if(instructions.get(i).nextAddress == -1) //Se l'istruzione non ha ancora un'indirizzo alla microistruzione successiva
            {
                if (instructions.get(i).next != null) //Se è saputo il nome dell'istruzione successiva
                    instructions.get(i).nextAddress = nameToInstr.get(instructions.get(i).next).address; //Assegna l'indirizzo dell'istruzione
                else if (instructions.get(i).nextIf != null) //Se l'istruzione presenta un bivio
                    //Assegna l'indirizzo dell'else (che poi verrà eventualmente messo in or con 0x100 durante l'esecuzione)
                    instructions.get(i).nextAddress = nameToInstr.get(instructions.get(i).nextElse).address;
                else if (!instructions.get(i).hasAnyNext()) //Se l'istruzione non presenta alcuna istruzione successiva esplicita
                    instructions.get(i).nextAddress = instructions.get(i + 1).address; //Assegna come istruzione successiva quella scritta subito dopo nel file
            }
        }

        /*
         * Stesso lavoro fatto nel for subito sopra ma per l'istruzione di default.
         * Il caso finale non è presente dato che, essendo l'istruzione di default non scritta in un punto particolare del file,
         * non è possibile collegarla all'istruzione scritta subito dopo
         */
        if(defaultInstr.nextAddress == -1)
        {
            if(defaultInstr.next != null)
                defaultInstr.nextAddress = nameToInstr.get(defaultInstr.next).address;
            else if(defaultInstr.nextIf != null)
                defaultInstr.nextAddress = nameToInstr.get(defaultInstr.nextElse).address;
        }
    }

    public void assemble()
    {
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(MIC1File.toString())))
        {
            //Scrive l'intestazione del file
            output.writeByte(magic1);
            output.writeByte(magic2);
            output.writeByte(magic3);
            output.writeByte(magic4);

            for(MALInstruction instr : locations) //Per ogni microistruzione
            {
                byte    b1=0x0,
                        b2=0x0,
                        b3=0x0,
                        b4=0x0,
                        b5=0x0;

                b1 = (byte) (b1 | (instr.nextAddress >> 1)); //Scrive gli 8 bit più significativi dell'indirizzo nel primo byte
                b2 = (byte) (b2 | (instr.nextAddress << 7)); //Scrive il bit meno significativo dell'indirizzo nel bit più significativo del secondo byte

                if(instr.jmpc) b2 = (byte) (b2 | 0x40); //Se jmpc è true assegna 1 al secondo bit più significativo del secondo byte
                if(instr.jamn) b2 = (byte) (b2 | 0x20); //Se jamn è true assegna 1 al terzo bit più significativo del secondo byte
                if(instr.jamz) b2 = (byte) (b2 | 0x10); //Se jamz è true assegna 1 al quarto bit più significativo del secondo byte
                if(instr.operation != null) //Se l'istruzione deve eseguire un operazione con l'alu non nulla
                {
                    if (instr.operation.aluControl[0]) b2 = (byte) (b2 | 0x08); //Assegna al quinto bit più significativo del secondo byte il valore di ShiftLeft
                    if (instr.operation.aluControl[1]) b2 = (byte) (b2 | 0x04); //Assegna al sesto bit più significativo del secondo byte il valore di ShiftRight
                    if (instr.operation.aluControl[2]) b2 = (byte) (b2 | 0x02); //Assegna al settimo bit più significativo del secondo byte il valore di F0
                    if (instr.operation.aluControl[3]) b2 = (byte) (b2 | 0x01); //Assegna all'ottavo bit più significativo del secondo byte il valore di F1
                    if (instr.operation.aluControl[4]) b3 = (byte) (b3 | 0x80); //Assegna al bit più significativo del terzo byte il valore di ENA
                    if (instr.operation.aluControl[5]) b3 = (byte) (b3 | 0x40); //Assegna al secondo bit più significativo del terzo byte il valore di ENB
                    if (instr.operation.aluControl[6]) b3 = (byte) (b3 | 0x20); //Assegna al terzo bit più significativo del terzo byte il valore di INVA
                    if (instr.operation.aluControl[7]) b3 = (byte) (b3 | 0x10); //Assegna al quarto bit più significativo del terzo byte il valore di INC
                }
                if(instr.busC[0]) b3 = (byte) (b3 | 0x08); //Se il registro H va scritto, assegna 1 al quinto bit più significativo del terzo byte
                if(instr.busC[1]) b3 = (byte) (b3 | 0x04); //Se il registro OPC va scritto, assegna 1 al sesto bit più significativo del terzo byte
                if(instr.busC[2]) b3 = (byte) (b3 | 0x02); //Se il registro TOS va scritto, assegna 1 al settimo bit più significativo del terzo byte
                if(instr.busC[3]) b3 = (byte) (b3 | 0x01); //Se il registro CPP va scritto, assegna 1 all'ottavo bit più significativo del terzo byte
                if(instr.busC[4]) b4 = (byte) (b4 | 0x80); //Se il registro LV va scritto, assegna 1 al bit più significativo del terzo byte
                if(instr.busC[5]) b4 = (byte) (b4 | 0x40); //Se il registro SP va scritto, assegna 1 al secondo bit più significativo del quarto byte
                if(instr.busC[6]) b4 = (byte) (b4 | 0x20); //Se il registro PC va scritto, assegna 1 al terzo bit più significativo del quarto byte
                if(instr.busC[7]) b4 = (byte) (b4 | 0x10); //Se il registro MDR va scritto, assegna 1 al quarto bit più significativo del quarto byte
                if(instr.busC[8]) b4 = (byte) (b4 | 0x08); //Se il registro MAR va scritto, assegna 1 al quinto bit più significativo del quarto byte
                if(instr.write) b4 = (byte) (b4 | 0x04); //Se l'istruzione deve scrivere in memoria, assegna 1 al sesto bit più significativo del quarto byte
                if(instr.read)  b4 = (byte) (b4 | 0x02); //Se l'istruzione deve leggere la memoria, assegna 1 al sesto bit più significativo del quarto byte
                if(instr.fetch) b4 = (byte) (b4 | 0x01); //Se l'istruzione deve prelevare un byte dalla memoria, assegna 1 all'ottavo bit più significativo del quarto byte
                if(instr.operation != null) //Se l'istruzione utilizza l'alu
                    b5 =  (byte) (b5 | instr.operation.register << 4); //Scrive nel bus 5 l'id del registro in cui il bus B deve scrivere nei 4 bit più significativi del quinto byte

                //Scrive la microistruzione
                output.writeByte(b1);
                output.writeByte(b2);
                output.writeByte(b3);
                output.writeByte(b4);
                output.writeByte(b5);
            }

        } catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    public MALInstruction parseMALInstruction(String instruction) throws MIC1Exception
    {
        MALInstruction instr = new MALInstruction();
        String[] parts = instruction.trim().split(";");

        for(String part : parts)
        {
            Matcher paramMatcher;

            //Cerca gli assegnamenti (es. MAR = PC = MDR =)
            if((paramMatcher = Pattern.compile("(\\w+)\\s*=\\s*").matcher(part)).find())
            {
                do
                    instr.addBusC(regToID(paramMatcher.group(1), false));
                while(paramMatcher.find());

                //Cerca l'operazione (es. PC + H + 1)
                paramMatcher = Pattern.compile("=\\s*([^=]+)$").matcher(part);
                if(paramMatcher.find())
                {
                    instr.operation = parseOperation(paramMatcher.group(1));
                }
            }
            //Cerca informazioni sull'istruzione successiva dalla forma if (N) goto INSTR o if (Z) goto INSTR
            else if((paramMatcher = Pattern.compile("if\\s*\\(([NZ])\\)\\s*goto\\s*(\\w+)").matcher(part)).find())
            {
                instr.setJam(paramMatcher.group(1));
                instr.nextIf = paramMatcher.group(2);
            }
            //Cerca informazioni sull'istruzione successiva dalla forma else goto INSTR
            else if((paramMatcher = Pattern.compile("else\\s+goto\\s+(\\w+)").matcher(part)).find())
            {
                instr.nextElse = paramMatcher.group(1);
            }
            //Cerca informazioni sull'istruzione successiva dalla forma goto INSTR
            else if((paramMatcher = Pattern.compile("goto\\s+(\\w+)").matcher(part)).find())
            {
                instr.next = paramMatcher.group(1);
            }
            //Cerca informazioni sull'istruzione successiva dalla forma goto (MBR) o goto (MBR OR 123)
            else if((paramMatcher = Pattern.compile("goto\\s+\\(MBR(?:\\s+OR\\s+(\\w+))?\\)").matcher(part)).find())
            {
                instr.jmpc = true;

                try
                {
                    instr.nextAddress = Integer.decode(paramMatcher.group(1));
                }
                catch(NullPointerException e)
                {
                    instr.nextAddress = 0;
                }
            }
            else if(part.trim().equals("wr"))
                instr.write = true;
            else if(part.trim().equals("rd"))
                instr.read = true;
            else if(part.trim().equals("fetch"))
                instr.fetch = true;
            else if(part.trim().equals("nop") || part.isBlank())
            {}
            else
            {
                throw new MIC1Exception(bufferedReader.getLineNumber(), "L'istruzione non rispetta la sintassi");
            }
        }

        return instr;
    }

    public MALOperation parseOperation(String operation) throws MIC1Exception
    {
        try
        {
            //Sostituisce << 8 e >> 1 con un singolo < o > per semplificare le operazioni successive
            operation = operation.replaceAll("<<\\s*8", "<");
            operation = operation.replaceAll(">>\\s*1", ">");

            boolean shiftR = false, shiftL = false;

            List<String> tokens = new ArrayList<>();
            StreamTokenizer streamTokenizer = new StreamTokenizer(new StringReader(operation));
            streamTokenizer.resetSyntax();
            streamTokenizer.wordChars('_', '_');
            streamTokenizer.wordChars('.', '.');
            streamTokenizer.wordChars( 'A' , 'Z' ) ;
            streamTokenizer.wordChars( 'a' , 'z' ) ;
            streamTokenizer.wordChars( '0' , '9' ) ;
            streamTokenizer.whitespaceChars(0, ' ');

            int t;
            while((t = streamTokenizer.nextToken()) != StreamTokenizer.TT_EOF){

                if(streamTokenizer.ttype == StreamTokenizer.TT_WORD) {
                    tokens.add(streamTokenizer.sval);
                } else {
                    if(t == '<') shiftL = true;
                    else if(t == '>') shiftR = true;
                    else tokens.add(Character.toString(t));
                }
            }

            /*
             * Cerca il formato dell'istruzione corretto e restituisce la corretta impostazione dei bit di controllo per shifter e alu
             */
            if(tokens.size() == 1) //Se l'operazione è formata da un solo token (H/REG/0/1)
            {
                if(tokens.get(0).equals("H"))           //H
                    return new MALOperation(new boolean[]{shiftL, shiftR, false, true, true, false, false, false}, NOREG);
                else if((isBRegister(tokens.get(0))))   //REG
                    return new MALOperation(new boolean[]{shiftL, shiftR, false, true, false, true, false, false},regToID(tokens.get(0), true));
                else if(tokens.get(0).equals("0"))      //0
                    return new MALOperation(new boolean[]{shiftL, shiftR, false, true, false, false, false, false}, NOREG);
                else if(tokens.get(0).equals("1"))      //1
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, false, false, false, true}, NOREG);
            }
            else if(tokens.size() == 2) //Se l'operazione è formata da due token (-H/+1/-1)
            {
                if(tokens.get(0).equals("-") && tokens.get(1).equals("H"))  //-H
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, true, false, true, true}, NOREG);
                if(tokens.get(0).equals("+") && tokens.get(1).equals("1"))  //+1
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, false, false, false, true}, NOREG);
                if(tokens.get(0).equals("-") && tokens.get(1).equals("1"))  //-1
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, false, false, true, false}, NOREG);
            }
            else if(tokens.size() == 3)//Se l'operazione è formata da tre token (REG-H/H+1/1+H/REG+1/1+REG/REG-1/H+REG/REG+H/H AND REG/REG AND H/H OR REG/REG OR H)
            {
                if(isBRegister(tokens.get(0)) && tokens.get(1).equals("-") && tokens.get(2).equals("H"))      //REG-H
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, true, true, true, true}, regToID(tokens.get(0), true));
                if(tokens.get(0).equals("H") && tokens.get(1).equals("+") && tokens.get(2).equals("1") ||
                        tokens.get(0).equals("1") && tokens.get(1).equals("+") && tokens.get(2).equals("H"))  //H+1 / 1+H
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, true, false, false, true}, NOREG);
                if(isBRegister(tokens.get(0)) && tokens.get(1).equals("+") && tokens.get(2).equals("1") ||
                        tokens.get(2).equals("1") && tokens.get(1).equals("+") && isBRegister(tokens.get(0))) //REG+1 / 1+REG
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, false, true, false, true}, regToID(tokens.get(0), true));
                if(isBRegister(tokens.get(0)) && tokens.get(1).equals("-") && tokens.get(2).equals("1"))      //REG-1
                    return new MALOperation(new boolean[]{shiftL, shiftR, true, true, false, true, true, false}, regToID(tokens.get(0), true));

                if((tokens.get(0).equals("H") && isBRegister(tokens.get(2))) || (isBRegister(tokens.get(0)) && tokens.get(2).equals("H"))) // H (+/AND/OR) REG
                {
                    boolean f0, f1;
                    int register = (tokens.get(0).equals("H") ?regToID(tokens.get(2), true) : regToID(tokens.get(0), true));

                    switch (tokens.get(1))
                    {
                        case "+": //H+REG / REG+H
                            f0 = true;
                            f1 = true;
                            break;
                        case "AND": //H AND REG / REG AND H
                            f0 = false;
                            f1 = false;
                            break;
                        case "OR": //H OR REG / REG OR H
                            f0 = false;
                            f1 = true;
                            break;
                        default:
                            throw new MIC1Exception(bufferedReader.getLineNumber(), "L'operazione non è consentita");
                    }

                    return new MALOperation(new boolean[]{shiftL, shiftR, f0, f1, true, true, false, false},register);
                }
            }
            else if(tokens.size() == 5) //Se l'operazione è formata da cinque token (H+REG+1 o una sua permutazione)
            {
                if(tokens.get(1).equals("+") && tokens.get(3).equals("+"))
                {
                    boolean hPresent=false, regPresent=false, onePresent=false;
                    int register = -1;
                    if(tokens.get(0).equals("H") || tokens.get(2).equals("H") || tokens.get(4).equals("H"))
                        hPresent = true;
                    if(tokens.get(0).equals("1") || tokens.get(2).equals("1") || tokens.get(4).equals("1"))
                        onePresent = true;

                    if(isBRegister(tokens.get(0)))
                    {
                        register = regToID(tokens.get(0), true);
                        regPresent = true;
                    }
                    if(isBRegister(tokens.get(2)))
                    {
                        register = regToID(tokens.get(2), true);
                        regPresent = true;
                    }
                    if(isBRegister(tokens.get(4)))
                    {
                        register = regToID(tokens.get(4), true);
                        regPresent = true;
                    }

                    if(onePresent && hPresent && regPresent)
                        return new MALOperation(new boolean[]{shiftL, shiftR, true, true, true, true, false, true}, register);
                }
            }
        }
        catch (IOException e){}


        throw new MIC1Exception(bufferedReader.getLineNumber(), "L'operazione non è consentita");
    }

    public boolean isBRegister(String register)
    {
        switch(register.toUpperCase())
        {
            case "MDR":
            case "PC":
            case "MBR":
            case "MBRU":
            case "SP":
            case "LV":
            case "CPP":
            case "TOS":
            case "OPC":
                return true;
            default:
                return false;
        }
    }

    public int regToID(String register, boolean B) throws MIC1Exception
    {
        if(B)
            switch(register.trim())
            {
                case "MDR":     return 0;
                case "PC":      return 1;
                case "MBR":     return 2;
                case "MBRU":    return 3;
                case "SP":      return 4;
                case "LV":      return 5;
                case "CPP":     return 6;
                case "TOS":     return 7;
                case "OPC":     return 8;
                default: throw new MIC1Exception(bufferedReader.getLineNumber(), "Il registro non esiste");
            }
        else
            switch(register.trim())
            {
                case "H":       return 0;
                case "OPC":     return 1;
                case "TOS":     return 2;
                case "CPP":     return 3;
                case "LV":      return 4;
                case "SP":      return 5;
                case "PC":      return 6;
                case "MDR":     return 7;
                case "MAR":     return 8;
                case "N":
                case "Z":       return NOREG;
                default: throw new MIC1Exception(bufferedReader.getLineNumber(), "Il registro non esiste");
            }
    }
}

class MIC1Exception extends Exception
{
    int line;
    String error;

    MIC1Exception(int line, String error)
    {
        this.line = line;
        this.error = error;
    }
}

class MALOperation
{
    boolean[] aluControl;
    int register;

    MALOperation(boolean[] aluControl, int register)
    {
        this.aluControl = aluControl;
        this.register = register;
    }
}

class MALInstruction
{
    String next;
    int nextAddress;
    boolean jmpc, jamn, jamz;
    String nextIf, nextElse;
    MALOperation operation;
    boolean[] busC;
    boolean write, read, fetch;
    int address;
    String name;

    MALInstruction()
    {
        address = -1;
        nextAddress = -1;
        busC = new boolean[9];
    }

    public void addBusC(int destination)
    {
        if (destination < busC.length)
            busC[destination] = true;
    }

    public void setJam(String nz)
    {
        switch (nz)
        {
            case "N":
                this.jamn = true;
                this.jamz = false;
                break;
            case "Z":
                this.jamn = false;
                this.jamz = true;
                break;
        }
    }
    public boolean hasAnyNext()
    {
        return next != null || jmpc || jamz || jamn;
    }
}