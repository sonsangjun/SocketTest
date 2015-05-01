
public class SignalData {
	final int signalSize = 2;
	final byte[] OK = {1,1};
	final byte[] NO = {0,1};
	
	public boolean OKChecking(byte[] argc)
	{
		if(argc[0] == 1 && argc[1] == 1)
			return true;
		else 
			return false;		
	}
}
