import java.io.IOException;


public class MainClass {
	public static void main(String[] argc) throws IOException
	{
		final boolean _Server = false;
		OneByteTransfer oneByteTransfer = new OneByteTransfer();
		
		//컴퓨터는 작가다.
		if(_Server)
			oneByteTransfer.clientTrans();
			
		else
			oneByteTransfer.clientReceive();
			
	}
}

/*
import java.io.IOException;


public class MainClass {
	public static void main(String[] argc) throws IOException
	{
		final boolean _Server = false;
		ByteArrayTransCeiver byteArrayTransCeiver = new ByteArrayTransCeiver();
		OneByteTransfer oneByteTransfer = new OneByteTransfer();
		
		//컴퓨터는 작가다.
		if(_Server)
			//oneByteTransfer.clientTrans();
			byteArrayTransCeiver.clientTrans();
		else
			//oneByteTransfer.clientReceive();
			byteArrayTransCeiver.clientReceive();
	}
}

*/
