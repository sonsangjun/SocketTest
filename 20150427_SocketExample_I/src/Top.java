
public class Top {

	public static void main(String[] args) {
		final boolean oper = true;	//true서버	false클라이언트
		final int interval = 5000;
		final int portNum = 9000;
		if(oper)
		{
			ServerTop server = new ServerTop(interval, portNum);
			server.mainServer();
		} 
		else
		{
			
		}
	}
}
