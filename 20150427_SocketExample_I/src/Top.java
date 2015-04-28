
public class Top {

	public static void main(String[] args) {
		final String serverIP = "192.168.0.3"; 
		final boolean oper = false;	//true서버	false클라이언트
		//final boolean oper = true;	
		final int interval = 5000;	//새로고침 간격(단위 : 밀리초)
		final int portNum = 9000;
		if(oper)
		{
			ServerTop server = new ServerTop(interval, portNum);
			server.mainServer();
		} 
		else
		{
			ClientTop client = new ClientTop(serverIP, portNum, interval);
			client.mainCilent();
		}
	}
}
