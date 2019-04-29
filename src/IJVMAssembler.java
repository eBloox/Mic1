import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IJVMAssembler
{
    Path JASFile, IJVMFile;
    boolean parsedConstants;
    boolean parsedMain;

    List<IJVMConstant> constants;
    IJVMMethod main;
    List<IJVMMethod> methods;

    int     magic1=0x1D,
            magic2=0xEA,
            magic3=0xDF,
            magic4=0xAD;


    public IJVMAssembler(String JASFile, String IJVMFile)
    {
        this.IJVMFile = Paths.get(IJVMFile);
        this.JASFile = Paths.get(JASFile);
        constants = new ArrayList<>();
        methods = new ArrayList<>();
    }

    public void assemble()
    {
        int offset = main.bytesize;
        int methodsSize = main.bytesize;

        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(IJVMFile.toString())))
        {
            //Magic numbers
            output.writeByte(magic1);
            output.writeByte(magic2);
            output.writeByte(magic3);
            output.writeByte(magic4);


            output.writeInt(0x00010000);                          //Origine del blocco delle costanti
            output.writeInt(4*(constants.size()+methods.size())); //Bytesize del blocco delle costanti

            for(IJVMConstant constant : constants) //Per ogni costante constant
                output.writeInt(constant.value);   //Scrive la costante constant come intero (4 byte)

            //Scrive la posizione di ogni metodo nel blocco dei metodi
            for(IJVMMethod method : methods)
            {
                output.writeInt(offset);
                offset += method.bytesize+4; //Grandezza del metodo + 2 byte per ll numero dei parametri e 2 byte per il numero delle variabili
            }

            //Calcola la grandezza del blocco dei metodi
            for(IJVMMethod method : methods)
                methodsSize += method.bytesize+4;

            output.writeInt(0x00);     //Origine del blocco dei metodi
            output.writeInt(methodsSize); //Bytesize del blocco dei metodi

            writeMethod(output, main);        //Scrive le istruzioni del metodo main
            for(IJVMMethod method : methods)  //Per ogni metodo oltre al main
            {
                output.writeShort(method.parameters.size()+1); //Scrive come short il numero di parametri+objref (2 byte)
                output.writeShort(method.variables.size());       //Scrive come short il numero di variabili (2 byte)
                writeMethod(output, method);                      //Scrive le istruzioni del metodo
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void writeMethod(DataOutputStream output, IJVMMethod method) throws IOException
    {
        Map<String, Integer> varMap = new HashMap<>();
        Map<String, Integer> constMap = new HashMap<>();
        Map<String, Integer> funcMap = new HashMap<>();
        Map<String, Integer> labelMap = new HashMap<>();

        //Mappa il nome delle costanti con il loro id
        for(int i = 0; i < constants.size(); i++)
            constMap.put(constants.get(i).name, i);

        //Mappa il nome delle funzioni con il loro id
        for(int i = 0; i < methods.size(); i++)
            funcMap.put(methods.get(i).name, i+constants.size());

        //Mappa il nome dei parametri con il loro id
        for(IJVMFuncParameter parameter : method.parameters)
            varMap.put(parameter.name, parameter.id);

        //Mappa il nome delle variabili con il loro id
        for(IJVMVariable variable : method.variables)
            varMap.put(variable.name, variable.id);

        //Mappa il nome dei label con la loro posizione
        for(IJVMLabel label : method.labels)
            labelMap.put(label.name, label.id);


        for(IJVMInstruction instruction : method.instructions) //Per ogni istruzione nel metodo
        {
            if(instruction.words != null)         //Se l'istruzione non è nulla (se non è solo un label)
                output.writeByte(instruction.op); //Scrivi il suo codice operativo (1 byte)

            if(instruction.parameters != null)    //Se l'istruzione ha dei parametri
                for(int i = 0; i < instruction.parameters.length; i++) //Per ogni parametro i
                {
                    Integer data = null;
                    switch(instruction.parameters[i].argType) //Controlla il tipo dell'argomento
                    {

                        case NUM: data = Integer.decode(instruction.words[i+1]); //Se è un numero lo decodifica
                            break;
                        case VAR: data = varMap.get(instruction.words[i+1]);     //Se è una variabile ottiene il suo id
                            break;
                        case CONST: data = constMap.get(instruction.words[i+1]); //Se è una costante ottiene il suo id
                            break;
                        case FUNC: data = funcMap.get(instruction.words[i+1]);   //Se è una funzione ottiene il suo id
                            break;
                        case LABEL: data = labelMap.get(instruction.words[i+1])-instruction.byteStart; //Se è un label ottiene la distanza dall'istruzione
                            break;
                    }

                    switch(instruction.parameters[i].bytesize) //Controlla la grandezza dell'argomento
                    {
                        case 1:                      //Se è grande 1 byte
                            output.writeByte(data);  //Lo scrive come byte
                            break;
                        case 2:                      //Se è grande 2 byte
                            output.writeShort(data); //Lo scrive come short
                            break;
                    }
                }
        }
    }


    public void parse() throws IJVMException
    {
        try (LineNumberReader buffer = new LineNumberReader(new FileReader(JASFile.toString()))) //Apre il file JASFile
        {
            String line;
            while ((line = buffer.readLine()) != null) //Legge ogni riga del file fino all'EOF
            {
                line = line.replaceFirst("(\\/\\/.*)", "").trim(); //Elimina i commenti

                if(line.equals(".constant")) //Se la riga è l'intestazione del blocco delle costanti
                {
                    if(parsedConstants) throw new IJVMException(buffer.getLineNumber(),
                            "Sono presenti due blocchi per le costanti"); //Può esserci un solo blocco di costanti
                    if(parsedMain) throw new IJVMException(buffer.getLineNumber(),
                            "Il main è stato trovato prima delle costanti"); //Le costanti devono trovarsi prima del main

                    parseConstants(buffer);
                    parsedConstants = true;
                }
                else if(line.equals(".main")) //Se la riga è l'intestazione del blocco main
                {
                    if(parsedMain) throw new IJVMException(buffer.getLineNumber(),
                            "Sono presenti due main"); //Può esserci un solo main

                    main = parseMain(buffer);
                    parsedMain = true;
                }
                else if(line.startsWith(".method")) //Se la riga è l'intestazione di un metodo
                {
                    if(!parsedMain) throw new IJVMException(buffer.getLineNumber(),
                            "Il metodo è stato trovato prima del main"); //Il main deve trovarsi prima dei metodi

                    methods.add(parseMethod(buffer, line));
                    parsedConstants = true;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void parseConstants(LineNumberReader buffer) throws IOException, IJVMException
    {
        String line;

        //Ciclo che continua fino alla fine del file o fino alla riga ".end-constant"
        while ((line = buffer.readLine()) != null && !line.trim().equals(".end-constant"))
        {
            line = line.replaceFirst("(\\/\\/.*)", "").trim(); //Elimina i commenti
            if(!line.isBlank()) //Se la linea non è vuota
            {
                String[] words = line.split("\\s+"); //Divide la riga in parole (separate da spazi)

                /*
                 * Se la riga è formata da un numero di parole diverso da 2 lancia un errore (le costanti hanno
                 * formato NOMECOSTANTE VALORECOSTANTE)
                 */
                if (words.length != 2) throw new IJVMException(buffer.getLineNumber(),
                        "Le costanti devono avere formato NOMECOSTANTE VALORECOSTANTE");

                constants.add(new IJVMConstant(words[0], Integer.decode(words[1])));
            }
        }

        if(line == null) throw new IJVMException(buffer.getLineNumber(),
                "Il file è finito prima della chiusura del blocco delle costanti");
    }

    private IJVMMethod parseMain(LineNumberReader buffer) throws IOException, IJVMException
    {
        IJVMMethod method = new IJVMMethod();
        List<IJVMVariable> vars;

        boolean parsedVars = false;
        boolean parsedInstr = false;
        boolean nextWide = false; //nextWide è una flag che indica se l'istruzione successiva è di tipo wide

        method.setName("main");

        String line;

        //Ciclo che continua fino alla fine del file o fino alla riga ".end-main"
        while ((line = buffer.readLine()) != null && !line.trim().equals(".end-main"))
        {
            line = line.replaceFirst("(\\/\\/.*)", "").trim(); //Elimina i commenti
            if(!line.isBlank())
            {
                if(line.equals(".var")) //Se la riga è l'intestazione del blocco delle variabili
                {
                    if(parsedVars) throw new IJVMException(buffer.getLineNumber(),
                            "Sono presenti due blocchi per le variabili nel metodo");
                    if(parsedInstr) throw new IJVMException(buffer.getLineNumber(),
                            "Il blocco delle variabili si trova dopo un'istruzione");

                    vars = parseVars(buffer);
                    method.setVariables(vars);

                    parsedVars = true;

                }
                else //Se la riga è un'istruzione
                {
                    IJVMInstruction instruction = parseInstruction(buffer, line, nextWide);
                    if(instruction.words != null)
                        //Se la prima parola dell'istruzione è "WIDE" ricorda che la prossima istruzione sarà wide
                        nextWide = (instruction.words[0].toUpperCase().equals("WIDE"));
                    method.addInstruction(instruction);

                    parsedInstr = true;
                }
            }
        }
        if(line == null) throw new IJVMException(buffer.getLineNumber(),
                "Il file è finito prima della chiusura del blocco del main");

        return method;
    }

    private IJVMMethod parseMethod(LineNumberReader buffer, String head) throws IOException, IJVMException
    {
        IJVMMethod method = new IJVMMethod();

        List<IJVMVariable> vars;
        List<IJVMFuncParameter> params = new ArrayList<>();

        boolean parsedVars = false;
        boolean parsedInstr = false;
        boolean nextWide = false;

        //primo gruppo: nome della funzione; secondo gruppo: contenuto delle parentesi
        Matcher paramMatcher = Pattern.compile(".method\\s+(\\w+)\\s*\\(([^()]*)\\)").matcher(head);
        if(paramMatcher.find())
        {
            method.setName(paramMatcher.group(1));
            if (paramMatcher.group(1).startsWith(",") || paramMatcher.group(0).endsWith(",")) //Se il contenuto delle parentesi inizia o finisce con una virgola
                throw new IJVMException(buffer.getLineNumber(),
                        "Errore nella dichiarazione dei parametri");

            for (String param : paramMatcher.group(2).split(",")) //Ottiene i parametri separati da una virgola
                params.add(new IJVMFuncParameter(param.trim()));

            method.setParameters(params);
        }
        else throw new IJVMException(buffer.getLineNumber(), "Errore nell'intestazione del metodo");

        String line;
        //Ciclo che continua fino alla fine del file o fino alla riga ".end-method"
        while ((line = buffer.readLine()) != null && !line.trim().equals(".end-method"))
        {
            line = line.replaceFirst("(\\/\\/.*)", "").trim(); //Elimina i commenti

            if(!line.isBlank())
            {
                if(line.equals(".var")) //Se la riga è l'intestazione del blocco delle variabili
                {
                    if(parsedVars) throw new IJVMException(buffer.getLineNumber(),
                            "Sono presenti due blocchi per le variabili nel metodo");
                    if(parsedInstr) throw new IJVMException(buffer.getLineNumber(),
                            "Il blocco delle variabili si trova dopo un'istruzione");

                    vars = parseVars(buffer);
                    method.setVariables(vars);

                    parsedVars = true;
                }
                else //Se la riga è un'istruzione
                {
                    IJVMInstruction instruction = parseInstruction(buffer, line, nextWide);
                    if(instruction.words != null)
                        //Se la prima parola dell'istruzione è "WIDE" ricorda che la prossima istruzione sarà wide
                        nextWide = (instruction.words[0].toUpperCase().equals("WIDE"));
                    method.addInstruction(instruction);

                    parsedInstr = true;
                }
            }
        }
        if(line == null) throw new IJVMException(buffer.getLineNumber(),
                "Il file è finito prima della chiusura del blocco del metodo");

        return method;
    }

    private List<IJVMVariable> parseVars(LineNumberReader buffer) throws IOException, IJVMException
    {
        List<IJVMVariable> vars = new ArrayList<>();

        String line;
        //Ciclo che continua fino alla fine del file o fino alla riga ".end-var"
        while ((line = buffer.readLine()) != null && !line.trim().equals(".end-var"))
        {
            line = line.replaceFirst("(\\/\\/.*)", "").trim(); //Elimina i commenti

            if(!line.isBlank())
            {
                /*
                 * Se la riga è formata da un numero di parole diverso da 2 lancia un errore (le costanti hanno
                 * formato NOMECOSTANTE VALORECOSTANTE)
                 */
                String[] words = line.split("\\s+");
                if(words.length != 1) throw new IJVMException(buffer.getLineNumber(),
                        "Le variabili devono avere formato NOMEVARIABILE");

                vars.add(new IJVMVariable(words[0]));
            }
        }
        if(line == null) throw new IJVMException(buffer.getLineNumber(),
                "Il file è finito prima della chiusura del blocco del metodo");

        return vars;
    }

    private IJVMInstruction parseInstruction(LineNumberReader buffer, String instruction, boolean isWide) throws IJVMException
    {
        IJVMLabel label = null;

        String[] words = instruction.trim().split("\\s+");

        if(words[0].endsWith(":")) //Se all'inizio dell'istruzione si trova un label
        {
            label = new IJVMLabel(words[0].substring(0, words[0].length()-1));
            if(words.length > 1) //Se ci stanno altre parti oltre al label
                words = Arrays.copyOfRange(words, 1, words.length); //Rimuovi il label dalle parole dell'istruzione
            else
                return new IJVMInstruction(label);
        }
        switch(words[0].toUpperCase())
        {
            //Istruzioni senza parametri
            case "DUP":
            case "ERR":
            case "HALT":
            case "IADD":
            case "IAND":
            case "IN":
            case "IOR":
            case "IRETURN":
            case "ISUB":
            case "NOP":
            case "OUT":
            case "POP":
            case "SWAP":
            case "WIDE":
                return new IJVMInstruction(label, words, buffer);
            //Istruzioni con un parametro di tipo label
            case "GOTO":
            case "IFEQ":
            case "IFLT":
            case "IF_ICMPEQ":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.LABEL, 2));
            //Istruzioni con un parametro di tipo variabile (possibile wide)
            case "ILOAD":
            case "ISTORE":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.VAR, ((isWide)?2:1)));
            //Istruzioni con un parametro di tipo numerico
            case "BIPUSH":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.NUM, 1));
            //Istruzioni con un parametro di tipo funzione
            case "INVOKEVIRTUAL":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.FUNC, 2));
            //Istruzioni con un parametro di tipo costante
            case "LDC_W":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.CONST, 2));
            //Istruzioni con due parametri: variabile e costante
            case "IINC":
                return new IJVMInstruction(label, words, buffer, new IJVMInstrParameter(IJVMInstrParameter.ArgType.VAR, 1), new IJVMInstrParameter(IJVMInstrParameter.ArgType.NUM, 1));
            default:
                throw new IJVMException(buffer.getLineNumber(), "L'istruzione non esiste");
        }
    }
}

class IJVMInstrParameter
{
    ArgType argType;
    int bytesize;

    public enum ArgType {
        NUM,
        VAR,
        CONST,
        FUNC,
        LABEL
    }

    IJVMInstrParameter(ArgType argType, int bytesize)
    {
        this.argType = argType;
        this.bytesize = bytesize;
    }
}

class IJVMInstruction
{
    int op;

    IJVMLabel label;
    String[] words;
    IJVMInstrParameter[] parameters;

    int bytesize, byteStart;

    IJVMInstruction(IJVMLabel label)
    {
        this.label = label;
    }

    IJVMInstruction(IJVMLabel label, String[] words, LineNumberReader buffer, IJVMInstrParameter... parameters) throws IJVMException
    {
        if(parameters.length+1 != words.length) throw new IJVMException(buffer.getLineNumber(),
                "L'istruzione ha un numero di parametri non corretto");

        bytesize = 1;
        if(parameters.length > 0)
            for(IJVMInstrParameter param : parameters)
                bytesize += param.bytesize;

        this.label = label;
        this.words = words;
        this.parameters = parameters;

        //Salva l'opcode dell'istruzione
        switch(words[0].toUpperCase())
        {
            case "DUP":     op = 0x59; break;
            case "ERR":     op = 0xFE; break;
            case "HALT":    op = 0xFF; break;
            case "IADD":    op = 0x60; break;
            case "IAND":    op = 0x7E; break;
            case "IN":      op = 0xFC; break;
            case "IOR":     op = 0xB0; break;
            case "IRETURN": op = 0xAC; break;
            case "ISUB":    op = 0x64; break;
            case "NOP":     op = 0x00; break;
            case "OUT":     op = 0xFD; break;
            case "POP":     op = 0x57; break;
            case "SWAP":    op = 0x5F; break;
            case "GOTO":    op = 0xA7; break;
            case "IFEQ":    op = 0x99; break;
            case "IFLT":    op = 0x9B; break;
            case "IF_ICMPEQ":       op = 0x9F; break;
            case "ILOAD":   op = 0x15; break;
            case "ISTORE":  op = 0x36; break;
            case "BIPUSH":  op = 0x10; break;
            case "INVOKEVIRTUAL":   op = 0xB6; break;
            case "LDC_W":   op = 0x13; break;
            case "IINC":    op = 0x84; break;
            case "WIDE":    op = 0xC4; break;
        }
    }

    public void setByteStart(int byteStart)
    {
        this.byteStart = byteStart;
    }
}

class IJVMMethod
{
    String name;

    List<IJVMFuncParameter> parameters;
    List<IJVMVariable> variables;
    List<IJVMInstruction> instructions;
    List<IJVMLabel> labels;

    int bytesize;

    public IJVMMethod()
    {
        parameters = new ArrayList<>();
        variables = new ArrayList<>();
        instructions = new ArrayList<>();
        labels = new ArrayList<>();
        bytesize = 0;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setParameters(List<IJVMFuncParameter> parameters)
    {
        this.parameters = parameters;

        for(int i = 0; i < parameters.size(); i++)
        {
            parameters.get(i).setID(i+1);
        }
    }
    public void setVariables(List<IJVMVariable> variables)
    {
        this.variables = variables;

        for(int i = 0; i < variables.size(); i++)
        {
            if(name.equals("main"))
                this.variables.get(i).setID(i);
            else
                this.variables.get(i).setID(i+parameters.size()+1); //Le variabili vengono dopo i parametri (+1 per via di objref)
        }
    }
    public void addInstruction(IJVMInstruction instruction)
    {
        instruction.setByteStart(bytesize);
        this.instructions.add(instruction);

        if(instruction.label != null)
        {
            instruction.label.setID(bytesize);
            labels.add(instruction.label);
        }

        bytesize += instruction.bytesize;

    }
}

class IJVMLabel
{
    String name;
    int id;

    IJVMLabel(String name)
    {
        this.name = name;
    }

    public void setID(int id)
    {
        this.id = id;
    }
}

class IJVMFuncParameter
{
    String name;
    int id;

    IJVMFuncParameter(String name)
    {
        this.name = name;
    }

    public void setID(int id)
    {
        this.id = id;
    }
}

class IJVMVariable
{
    String name;

    int id;

    IJVMVariable(String id)
    {
        this.name = id;
    }

    public void setID(int id)
    {
        this.id = id;
    }
}

class IJVMConstant
{
    String name;
    int value;

    IJVMConstant(String id, int value)
    {
        this.name = id;
        this.value = value;
    }
}

class IJVMException extends Exception
{
    int line;
    String error;

    IJVMException(int line, String error)
    {
        this.line = line;
        this.error = error;
    }
}