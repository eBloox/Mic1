public class BusMemory
{
    public static int
            WRITE = 2,
            READ = 1,
            FETCH = 0,
        READYREAD = 0,
        READYFETCH = 1;

    protected Bus address, fetchAddress, data, fetchData, wrf, rdyTransfer;

    public BusMemory(int sizeAddr, int sizeData, int sizeFetchData, Bus wrf)
    {
        address = new Bus(sizeAddr);
        fetchAddress = new Bus(sizeAddr);
        data = new Bus(sizeData);
        fetchData = new Bus(sizeFetchData);

        this.wrf = wrf;
        this.rdyTransfer = new Bus(2);
    }

    public Bus getAddress()
    {
        return address;
    }
    public Bus getFetchAddress()
    {
        return fetchAddress;
    }
    public Bus getFetchData()
    {
        return fetchData;
    }
    public Bus getData()
    {
        return data;
    }
    public Bus getWrf()
    {
        return wrf;
    }
    public Bus getRdyTransfer()
    {
        return rdyTransfer;
    }
}
