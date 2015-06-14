package Sender;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.*;

public class LocationManage extends Thread{
	ValueCollections value = new ValueCollections();
	RoomData roomData = null;
	
	Socket eventSocket = null;
	SocketEventUsed socketEventUsed;
	
	RoomDataToArray locationList = null;
	RoomDataToArray myLocation = null;
	
	//서버측 생성자
	public LocationManage(Socket eventSocket, RoomData roomData) {
		this.eventSocket = eventSocket;
		this.roomData = roomData;
	}
	
	//스레드로 실행시키는 클라이언트
	public LocationManage(Socket eventSocket,SocketEventUsed socketEventUsed , RoomDataToArray myLocation, RoomDataToArray locationList)
	{
		this.eventSocket = eventSocket;
		this.socketEventUsed = socketEventUsed;
		this.myLocation = myLocation;
		this.locationList = locationList;
	}
	
	//클라이언트의 주기적 위치 정보 전송을 위해 스레드를 상속.
	@Override
	public void run() {
		boolean cycle = true;
		SignalData signal = new SignalData(eventSocket);
		signal.initial();
		while(true)
		{
			if(socketEventUsed.socketEventUsed)
			{
				try {
					Thread.sleep(value.waitTime);	//100ms주기로 대기한다. 	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}
				
				
			else
			{
				synchronized (socketEventUsed) {
					socketEventUsed.socketEventUsed = true;
				}
				if(cycle)
				{
					//서버에게 위치 정보를 보낸다.(toDoRequest로 설정하면 세부 설정을 할수 없어 toDoResponse쓴다.)
					if(signal.toDoResponse(signal.location))
					{
						if(signal.toCatchResponse(signal.response))
						{
							if(!clientsender())
								break;	
							synchronized (socketEventUsed) {
								socketEventUsed.socketEventUsed = false;
							}
							try {
								Thread.sleep(value.coolTime);
							} catch (InterruptedException e) {
								System.out.println("LocationManage.run() (if(signal.toCatchResponse==true)인터럽트예외, exception : "+e.getMessage());
							}
							cycle = !cycle;						
						}
						
						else
						{							
							synchronized (socketEventUsed) {
								socketEventUsed.socketEventUsed = false;
							}
							try {
								Thread.sleep(value.coolTime);
							} catch (InterruptedException e) {
								System.out.println("LocationManage.run() (else) 인터럽트예외, exception : "+e.getMessage());
								e.printStackTrace();
							}	
							cycle = !cycle;								
						}
					}
					//연결이 끊긴경우 false이기 때문에 
					else
						break;
					
					//서버에게 위치 정보를 보낸다. 끝
				}
				else
				{
					//서버에게 위치 정보를 받아온다.
					if(signal.toDoResponse(signal.locationList))
					{
						if(signal.toCatchResponse(signal.response))
						{
							if(clientListReceiver() == null)
								break;
							synchronized (socketEventUsed) {
								socketEventUsed.socketEventUsed = false;
							}
							try {
								Thread.sleep(value.coolTime);
							} catch (InterruptedException e) {
								System.out.println("LocationManage.run() 예외, exception : "+e.getMessage());								
							}
							cycle = !cycle;								
						}
						else
						{
							//todoRequest까지는 잘못 할 수 있다.
							synchronized (socketEventUsed) {
								socketEventUsed.socketEventUsed = false;
							}
							try {
								Thread.sleep(value.coolTime);
							} catch (InterruptedException e) {
								System.out.println("LocationManage.run() 인터럽트예외, exception : "+e.getMessage());
							}	
							cycle = !cycle;							
						}					
					}
					//연결이 끊긴경우 false이기 때문에 
					else
						break;
				}	//서버에게 위치 정보를 받아온다. 끝
				continue;
			}			
		}
	}
	
	
	//클라가 서버에게 주기적으로 보내는 위치정보 (그런데 이부분은 스레드로 처리해야 할듯.)
	public boolean clientsender()
	{
		ObjectOutputStream outputLocation = null;

		try {
			outputLocation = new ObjectOutputStream(eventSocket.getOutputStream());
			synchronized (myLocation) {
				outputLocation.writeObject(myLocation);
				outputLocation.flush();					
			}		
		} catch (java.net.SocketException e) {
			System.out.println("LocationManage.clientsender() SocketException 발생");
			return false;
		} 
		
		catch (IOException e) {
			System.out.println("LocationManage.clientsender() IOException발생");
			return false;
		}
		return true;
	}
	
	//위치목록을 받음. 클라이언트가 호출하는 메소드(클라꺼)
	public RoomDataToArray clientListReceiver()
	{
		RoomDataToArray temp = null;
		ObjectInputStream inputLocationList = null;
		try {
			inputLocationList = new ObjectInputStream(eventSocket.getInputStream());
			temp = (RoomDataToArray) inputLocationList.readObject();
			locationList.clientID = temp.clientID;
			locationList.latitude = temp.latitude;
			locationList.longitude= temp.longitude;
		}catch (java.net.SocketException e) {
			System.out.println("LocationManage.clientListReceiver() SocketException 발생");
			return null;
		}
		catch (ClassNotFoundException | IOException e) {
			System.out.println("LocationManage.clientListReceiver() ClassNotFound or IO Exception 발생");
			return null;
		}
		return locationList;		
	}
	
	
	//서버측은 SocketEventUsed를 수정하지 않음. 서버스레드에서 미리 거르고 시작함.
	//서버측(그냥 위치하나를 받음)
	//위치 받고 새위치로 수정까지 함.
	public RoomDataToArray serverReceiver()
	{		
		ObjectInputStream inputLocation = null;
		RoomDataToArray clientLocation = null;
		try {
			inputLocation = new ObjectInputStream(eventSocket.getInputStream());
			clientLocation = (RoomDataToArray) inputLocation.readObject();
			synchronized (roomData) {
				int index = roomData.clientManage.clientID.indexOf(clientLocation.clientID.get(0));
				roomData.clientManage.latitude.set(index, clientLocation.latitude.get(0));
				roomData.clientManage.longitude.set(index, clientLocation.longitude.get(0));				
			}
			return clientLocation;	//혹시 필요할지도 모르니 리턴한다.
		}catch (java.net.SocketException e) {
			System.out.println("LocationManage.serverReceiver() SocketException 발생");
			return null;
		}
		catch (IOException | ClassNotFoundException e) {
			System.out.println("LocationManage.serverReceiver() SocketException IO or ClassNotFound Exception 발생");
			return null;
		}catch(java.lang.NullPointerException e) {
			System.out.println("LocationManage.serverReceiver() NullPointerException 발생	");
			return null;
		}
		
	}	
	
	//방에 참가한 인원의 위치 정보를 보내는자.
	//서버가 호출하는 메소드(써버꺼)
	public boolean serverListSender()
	{		
		ObjectOutputStream outputLocationList = null;
		try {
			locationList = new RoomDataToArray(roomData.clientManage.clientID, roomData.clientManage.latitude, roomData.clientManage.longitude);
			outputLocationList = new ObjectOutputStream(eventSocket.getOutputStream());
			synchronized (roomData) {
				outputLocationList.writeObject(locationList);
				outputLocationList.flush();				
			}			
		}catch (java.net.SocketException e) {
			System.out.println("LocationManage.serverListSender() SocketException발생");
			return false;
		}
		catch (IOException e) {
			System.out.println("LocationManage.serverListSender() IOException발생");
			return false;
		}
		catch(java.lang.NullPointerException e)
		{
			System.out.println("LocationManage.serverListSender() NullException발생 ");
			return false;
		}
		return true;
	}	
}
