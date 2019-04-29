public class AddrOr
{
    Bus addr, mbr, out, jmpc;

    public AddrOr(Bus addr, Bus mbr, Bus out, Bus jmpc)
    {
        this.addr = addr;
        this.mbr = mbr;
        this.out = out;
        this.jmpc = jmpc;
    }

    public void act()
    {
        /*
         * Se jmpc è asserito carica in output il bus addr con ogni bit messo in or con l'mbr, altrimenti
         * carica semplicemente il contenuto del bus addr
         */
        if(jmpc.getWord().get(0))
            out.set(addr.getWord().getOr(mbr.getWord()));
        else
            out.set(addr.getWord());
    }
}
