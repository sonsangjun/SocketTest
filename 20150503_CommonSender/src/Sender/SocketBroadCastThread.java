package Sender;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class SocketBroadCastThread extends Thread{ 
	ValueCollections value = new ValueCollections();
	RoomData roomData = null;
	Socket socket = null;
	SocketBroadCastUsed socketBroadCastUsed;
	
	//서버인경우
	public SocketBroadCastThread(RoomData roomData,SocketBroadCastUsed socketBroadCastUsed) {
		this.roomData = roomData;
		this.socketBroadCastUsed = socketBroadCastUsed;
	}
	
	//클라이언트인 경우 (안드로이드인 경우, 텍스트를 받아 출력하고 싶다면 생성자 매개변수를 하나더 추가한다. 레퍼런스로 주면 값 변경시 바로 적용된다.)
	//아니면 socketBroadCastUsed.message를 이용하자.
	public SocketBroadCastThread(Socket socket, SocketBroadCastUsed socketBroadCastUsed) {
		this.socket = socket;
		this.socketBroadCastUsed = socketBroadCastUsed;
	}
	@Override
	public void run() {
		if(socket == null)				//서버인 경우
		{
			while(true)
			{
				synchronized (socketBroadCastUsed) {
					if(socketBroadCastUsed.broadCastKill)
					{
						socketBroadCastUsed.broadCastDead = true;
						socketBroadCastUsed.broadCastKill = false;
						break;
					}
						
					else if(socketBroadCastUsed.message == null)
					{
						try {
							Thread.sleep(value.waitTime);
							continue;
						} catch (InterruptedException e) {
							e.printStackTrace();
							continue;
						}
					}					
				}
				
				//문자열이 계시는 경우 전 소켓에 날려준다.
				synchronized (roomData) {
					for(BufferedWriter B: roomData.clientManage.broadCast)
					{
						try {
							B.write(socketBroadCastUsed.message);
							B.newLine();
							B.flush();			
						} catch (IOException e) {
							e.printStackTrace();
							continue;
						}			
					}
				}				
			}
		}
		
		else if(roomData == null)		//클라이언트인 경우
		{
			String temp = null;
			try {
				BufferedReader inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				while(true)
				{
					synchronized (socketBroadCastUsed) {
						if(socketBroadCastUsed.broadCastKill)
						{
							socketBroadCastUsed.broadCastDead = true;
							socketBroadCastUsed.broadCastKill = false;
							break;
						}						
					}
					temp = inputReader.readLine();
					if(temp == null)
					{
						Thread.sleep(value.waitTime);
						continue;						
					}
					else
					{
						synchronized (socketBroadCastUsed) {
							socketBroadCastUsed.message = new String(temp);
							System.out.println("temp");
							temp = null;
						}						
					}						
				}								
			} catch (IOException | InterruptedException e) {
				System.out.println("inputReader만드는데 실패");
				e.printStackTrace();
				socketBroadCastUsed.broadCastDead=true;
				return ;
			}			
		}		
	}
}
