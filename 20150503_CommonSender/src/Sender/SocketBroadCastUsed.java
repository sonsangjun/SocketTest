package Sender;

//브로드캐스트 소켓이 사용중인지 체크
public class SocketBroadCastUsed {			
	public final String nullString = new String("null");
											//true일때의 의미
	public boolean broadCastUsed = false; 	//사용중이다.
	public boolean broadCastKill = false;	//Thread를 죽여라
	
	public String message = new String(nullString);			//초기값은 "null"
	
	
	public void init()
	{
		broadCastUsed = false;
		broadCastKill = false;
		message = new String(nullString);
	}
	

}
