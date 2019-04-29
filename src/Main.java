import javax.swing.*;
import java.io.*;

public class Main
{
    private static final int CPPSTART = 0x4000;
    private static final int LVSTART  = 0x8000;
    private static final int SPSTART  = 0x8010;

    static Bus busA, busB, busC;
    static Bus busMarCtrlWr, busMdrCtrlWr, busPcCtrlWr, busSpCtrlWr, busLvCtrlWr, busCppCtrlWr, busTosCtrlWr, busOpcCtrlWr, busHCtrlWr, busMirCtrlWr;
    static Bus busMdrCtrlRd, busPcCtrlRd, busMbrCtrlRd, busSpCtrlRd, busLvCtrlRd, busCppCtrlRd, busTosCtrlRd, busOpcCtrlRd, busHCtrlRd, busMirCtrlRd;
    static Bus busMbrAddrOut;
    static Bus busAluOut;
    static Bus busAluCtrl, busShftCtrl;
    static Bus busDecOut;
    static Bus busAluNZ;
    static Bus busCMemIn, busCMemOut, busMirOut, busMirCtrl;
    static Bus busMirAddr, busMirJnz, busMirJmpc, busMirM, busMirB;
    static Bus busHbOut;
    static Bus busAOOut;

    static BusMemory busMem;

    static Register sp, lv, cpp, tos, opc, h;
    static RegisterMDR mdr;
    static RegisterPC pc;
    static RegisterMAR mar;
    static RegisterMBR mbr;
    static RegisterMPC mpc;
    static RegisterMIR mir;

    static ALU alu;
    static Shifter shifter;
    static Decoder decoder;

    static Memory mainMem;
    static ROM ctrlMem;

    static Splitter splitMir, splitDec;

    static HighBit highBit;
    static AddrOr addrOr;

    static File IJVMFile, MIC1File;

    /**
     * Inizializzazione delle componenti
     * @param args
     */
    public static void main(String[] args)
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){}

        Mic1Form mic1form = new Mic1Form();

        mic1form.setVisible(true);

        //Crea bus A B e C (tutti a 32 bit)
        busA = new Bus(32);
        busB = new Bus(32);
        busC = new Bus(32);

        //Linee di controllo per la scrittura dei registri
        busMarCtrlWr = new Bus(1);
        busMdrCtrlWr = new Bus(1);
        busPcCtrlWr  = new Bus(1);
        busSpCtrlWr  = new Bus(1);
        busLvCtrlWr  = new Bus(1);
        busCppCtrlWr = new Bus(1);
        busTosCtrlWr = new Bus(1);
        busOpcCtrlWr = new Bus(1);
        busHCtrlWr   = new Bus(1);
        busMirCtrlWr = new Bus(1);

        //Linee di controllo per la lettura dei registri
        busMdrCtrlRd = new Bus(1);
        busPcCtrlRd  = new Bus(1);
        busMbrCtrlRd = new Bus(2);
        busSpCtrlRd  = new Bus(1);
        busLvCtrlRd  = new Bus(1);
        busCppCtrlRd = new Bus(1);
        busTosCtrlRd = new Bus(1);
        busOpcCtrlRd = new Bus(1);
        busHCtrlRd   = new Bus(1);
        busMirCtrlRd = new Bus(1);

        busMbrAddrOut = new Bus(8); //Bus che collega il registro mbr al circuito per il calcolo della prossima microistruzione

        busAluNZ = new Bus(2);    //Linee in uscita dall'alu che segnalano se l'output è zero (Z) o negativo (N)
        busAluOut = new Bus(32);  //Bus in uscita dell'alu (32 bit)

        //Linee di controllo per alu(f0, f1, ena, enb, inva, inc) e per lo shifter (shift right, shift left)
        busAluCtrl = new Bus(6);
        busShftCtrl = new Bus(2);

        busDecOut = new Bus(16);  //Bus in uscita dal decodificatore

        busCMemIn = new Bus(9);   //Bus in entrata della memoria di controllo
        busCMemOut = new Bus(40); //Bus in uscita della memoria di controllo
        busMirOut = new Bus(36);  //Bus in uscita del mir

        busMirAddr = new Bus(9);  //Porzione next address del bus in uscita del mir
        busMirJmpc = new Bus(1);  //Porzione jmpc del bus in uscita del mir
        busMirJnz = new Bus(2);   //Porzione jamn e jamz del bus in uscita del mir
        busMirM = new Bus(3);     //Porzione write read e fetch del bus in uscita del mir
        busMirB = new Bus(4);     //Porzione per la scelta del bus B del bus in uscita del mir

        busHbOut = new Bus(1); //Bus in uscita dal circuito di HighBit
        busAOOut = new Bus(9); //Bus in uscita dal circuito per il calcolo della prossima microistruzione

        busMem = new BusMemory(32, 32,8, busMirM); //Bus per le comunicazioni con la memoria

        //Creazione dei registri (tutti a 32 bit tranne mbr:8bit mir:36bit)
        mar = new RegisterMAR(32, busC, busMarCtrlWr, busMem, mic1form, "MAR");
        mdr = new RegisterMDR(32, busC, busB, busMdrCtrlWr, busMdrCtrlRd, busMem, mic1form, "MDR");
        pc  = new RegisterPC(32, busC, busB, busPcCtrlWr, busPcCtrlRd, busMem, mic1form, "PC");
        mbr = new RegisterMBR(8, busB, busMbrCtrlRd, busMem, busMbrAddrOut, mic1form, "MBR");
        sp  = new Register(32, busC, busB, busSpCtrlWr, busSpCtrlRd, mic1form, "SP");
        lv  = new Register(32, busC, busB, busLvCtrlWr, busLvCtrlRd, mic1form, "LV");
        cpp = new Register(32, busC, busB, busCppCtrlWr, busCppCtrlRd, mic1form, "CPP");
        tos = new Register(32, busC, busB, busTosCtrlWr, busTosCtrlRd, mic1form, "TOS");
        opc = new Register(32, busC, busB, busOpcCtrlWr, busOpcCtrlRd, mic1form, "OPC");
        h   = new Register(32, busC, busA, busHCtrlWr, busHCtrlRd, mic1form, "H");
        mir = new RegisterMIR(36, busCMemOut, busMirOut, busMirCtrlWr, busMirCtrlRd, mic1form, "MIR");

        /*
         * ad mpc non viene data una grandezza perchè non si comporta esattamente come un registro, ma viene usato più
         * come una porta per far passare o no il nuovo indirizzo
         */
        mpc = new RegisterMPC(busHbOut, busAOOut, busCMemIn);

        alu = new ALU(32, busA, busB, busAluCtrl, busAluOut, busAluNZ); //ALU a 32 bit
        shifter = new Shifter(busAluOut, busShftCtrl, busC);        //Shifter a 32 bit
        decoder = new Decoder(4, busMirB, busDecOut);                    //Decodificatore 4-16

        mainMem = new Memory(4, 262144, busMem);   //Memoria di 2^18 byte
        ctrlMem = new ROM(40, 512, busCMemIn, busCMemOut);  //Memoria di 512 parole da 5 byte

        //La linea in uscita del mir viene splittata nei diversi sottobus
        splitMir = new Splitter(busMirOut);
        /*
        splitMir.add(0, 9, busMirAddr);     //next address
        splitMir.add(9, 1, busMirJmpc);     //jmpc
        splitMir.add(10, 2, busMirJnz);     //jamn / jamz
        splitMir.add(12, 2, busShftCtrl);   //controllo dello shifter
        splitMir.add(14, 6, busAluCtrl);    //controllo dell'alu
        splitMir.add(20, 1, busHCtrlWr);    //controllo scrittura registro h
        splitMir.add(21, 1, busOpcCtrlWr);  //controllo scrittura registro opc
        splitMir.add(22, 1, busTosCtrlWr);  //controllo scrittura registro tos
        splitMir.add(23, 1, busCppCtrlWr);  //controllo scrittura registro cpp
        splitMir.add(24, 1, busLvCtrlWr);   //controllo scrittura registro lv
        splitMir.add(25, 1, busSpCtrlWr);   //controllo scrittura registro sp
        splitMir.add(26, 1, busPcCtrlWr);   //controllo scrittura registro pc
        splitMir.add(27, 1, busMdrCtrlWr);  //controllo scrittura registro mdr
        splitMir.add(28, 1, busMarCtrlWr);  //controllo scrittura registro mar
        splitMir.add(29, 3, busMirM);       //linee di controllo della memoria
        splitMir.add(32, 4, busMirB);       //scelta di chi userà il bus B (collegato al decodificatore)
        */

        splitMir.add(27, 9, busMirAddr);     //next address
        splitMir.add(26, 1, busMirJmpc);     //jmpc
        splitMir.add(24, 2, busMirJnz);     //jamn / jamz
        splitMir.add(22, 2, busShftCtrl);   //controllo dello shifter
        splitMir.add(16, 6, busAluCtrl);    //controllo dell'alu
        splitMir.add(15, 1, busHCtrlWr);    //controllo scrittura registro h
        splitMir.add(14, 1, busOpcCtrlWr);  //controllo scrittura registro opc
        splitMir.add(13, 1, busTosCtrlWr);  //controllo scrittura registro tos
        splitMir.add(12, 1, busCppCtrlWr);  //controllo scrittura registro cpp
        splitMir.add(11, 1, busLvCtrlWr);   //controllo scrittura registro lv
        splitMir.add(10, 1, busSpCtrlWr);   //controllo scrittura registro sp
        splitMir.add(9, 1, busPcCtrlWr);   //controllo scrittura registro pc
        splitMir.add(8, 1, busMdrCtrlWr);  //controllo scrittura registro mdr
        splitMir.add(7, 1, busMarCtrlWr);  //controllo scrittura registro mar
        splitMir.add(4, 3, busMirM);       //linee di controllo della memoria
        splitMir.add(0, 4, busMirB);       //scelta di chi userà il bus B (collegato al decodificatore)

        //La linea in uscita del decodificatore viene divisa nelle diverse linee di controllo per la lettura dei registri
        splitDec = new Splitter(busDecOut);
        splitDec.add(0,1, busMdrCtrlRd);    //controllo lettura registro mdr
        splitDec.add(1,1, busPcCtrlRd);     //controllo lettura registro pc
        splitDec.add(2,2, busMbrCtrlRd);    //controllo lettura registro mbr
        splitDec.add(4,1, busSpCtrlRd);     //controllo lettura registro sp
        splitDec.add(5,1, busLvCtrlRd);     //controllo lettura registro lv
        splitDec.add(6,1, busCppCtrlRd);    //controllo lettura registro cpp
        splitDec.add(7,1, busTosCtrlRd);    //controllo lettura registro tos
        splitDec.add(8,1, busOpcCtrlRd);    //controllo lettura registro opc

        highBit = new HighBit(busAluNZ, busMirJnz, busHbOut); //Circuito per il calcolo del bit più rilevante dell'mpc
        addrOr = new AddrOr(busMirAddr, busMbrAddrOut, busAOOut, busMirJmpc); //Circuito per il calcolo dell'mpc
    }

    /**
     * Controlla se il file mic1 è valido, e in caso di validità, lo fissa come file da utilizzare
     * @param MIC1File il file mic1 da settare
     * @return restituisce true se il file MIC1File è un file mic1 valido
     */
    public static boolean setMIC1File(File MIC1File)
    {
        boolean flag = ctrlMem.checkIfValid(MIC1File.getPath());

        if(flag)
            Main.MIC1File = MIC1File;
        else
            Main.MIC1File = null;

        init(); //Essendo il file mic1 cambiato, inizializza di nuovo la microarchitettura

        return flag;
    }

    /**
     * Controlla se il file ijvm è valido, e in caso di validità, lo fissa come file da utilizzare
     * @param IJVMFile il file ijvm da settare
     * @return restituisce true se il file IJVMFile è un file ijvm valido
     */
    public static boolean setIJVMFile(File IJVMFile)
    {
        boolean flag = mainMem.checkIfValid(IJVMFile.getPath());

        if(flag)
            Main.IJVMFile = IJVMFile;
        else
            Main.IJVMFile = null;

        init(); //Essendo il file ijvm cambiato, inizializza di nuovo la microarchitettura

        return flag;
    }

    /**
     * Resetta i registri al loro stato iniziale
     */
    public static void resetRegisters()
    {
        mar.force(0x0);
        mdr.force(0x0);
        pc.force((long)Math.pow(2,32)-1); //0xFFFFFFFF
        mbr.force(0x0);
        sp.force(SPSTART);
        lv.force(LVSTART);
        cpp.force(CPPSTART);
        tos.force(0x0);
        opc.force(0x0);
        h.force(0x0);

        mir.force(0x0);
        mpc.out.set(0x0);

        //Queste linee di controllo devono sempre essere vere
        busHCtrlRd.set(0, true);
        busMirCtrlWr.set(0, true);
        busMirCtrlRd.set(0, true);
    }

    /**
     * Inizializza la microarchitettura resettando i registri, e se sia il file mic1 sia il file ijvm sono validi,
     * inizializza anche la memoria di controllo e la memoria principale
     */
    public static void init()
    {
        resetRegisters();
        if(MIC1File != null && IJVMFile != null)
        {
            mainMem.init(IJVMFile.getPath());
            ctrlMem.init(MIC1File.getPath());
        }
    }

    /**
     * Ciclo di clock
     * @return restituisce il contenuto di mpc alla fine del ciclo di clock (la microistruzione successiva)
     */
    public static long cycle()
    {
        mainMem.resetReady(); //Nega le linee di controllo della memoria in fronte di discesa

        ctrlMem.read(); //La memoria di controllo viene letta per ottenere la microistruzione da utilizzare nel ciclo
        mir.store();    //Il mir scrive dal bus di input la microistruzione
        mir.load();     //Il mir legge la microistruzione nel bus di output
        splitMir.act(); //La linea del mir viene splittata nelle sue diverse parti
        decoder.act();  //Il decodificatore calcola la linea di controllo per la lettura nel bus B
        splitDec.act(); //La linea del decodificatore viene splittata nelle diverse linee di controllo dei registri

        //I registri caricano il loro valore sul bus B (se la loro linea di controllo per la lettura è asserita)
        mdr.load();
        pc.load();
        mbr.load();
        sp.load();
        lv.load();
        cpp.load();
        tos.load();
        opc.load();
        h.load();

        //Dopo il caricamento dei registri nel bus B, l'alu e lo shifter svolgono le loro operazioni
        alu.act();
        shifter.act();

        /*
         * Dopo che l'alu e lo shifter calcolano il loro valore in uscita e lo caricano nel bus C, i registri salvano
         * il valore presente nello stesso bus (solo se la loro linea di controllo per la scrittura è asserita)
         */
        mar.store();
        mdr.store();
        pc.store();
        sp.store();
        lv.store();
        cpp.store();
        tos.store();
        opc.store();
        h.store();

        //I registri che parlano con la memoria caricano nel bus il loro valore (se la loro linea di controllo è asserita)
        mdr.loadToMemory(); //se WRITE è asserito
        mar.loadToMemory(); //se WRITE o READ sono asseriti
        pc.loadToMemory();  //se FETCH è asserito

        /*
         * La memoria principale controlla il suo stato e lo stato delle diverse linee del bus e a seconda di essi
         * scrive/legge nel bus o "salta un ciclo"
         */
        mainMem.act();

        //I registri che parlano con la memoria vengono scritti col valore dei loro bus (solo se la loro linea di controllo è asserita)
        mdr.storeFromMemory(); //se READYREAD  è asserito
        mbr.storeFromMemory(); //se READYFETCH è asserito

        //I circuiti per calcolare l'indirizzo della prossima istruzione fanno il loro lavoro
        highBit.act();
        addrOr.act();

        mpc.act(); //L'mpc carica nel suo bus in uscita l'indirizzo della prossima istruzione, calcolato usando il valore dei circuiti highBit e addrOr

        return mpc.out.getWord().toLong(); //La funzione restituisce l'indirizzo della prossima microistruzione
    }
}
