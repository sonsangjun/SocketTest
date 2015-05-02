
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
/* 혹시모르니 
if(fileSize%unitSize == 0)
counter = (fileSize/unitSize);
else 
{
counter = (fileSize/unitSize) + 1;
extra = fileSize%unitSize;
}
*/