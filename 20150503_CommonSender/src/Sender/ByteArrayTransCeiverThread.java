package Sender;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

//서버,클라이언트 스레드와 독립적으로 돌아감 .
//크기가 큰 카메라프리뷰나 음성 송수신에 사용.
public class ByteArrayTransCeiverThread {
	boolean transCeive = true; //(true 송신, false 수신)(송수신이니깐 순서대로 불값줌)
	byte[] fileByteArray;
	
	SignalData signal;
	BufferedInputStream input;
	BufferedOutputStream output;
	
	SharedData shared; 
	
	//생성자가 크고 아름답다.
	public ByteArrayTransCeiverThread(boolean transCeive, byte[] fileByteArray,SignalData signal, BufferedInputStream input, BufferedOutputStream output,SharedData shared) {
		this.transCeive = transCeive;
		this.fileByteArray = fileByteArray;
		this.signal = signal;
		this.input = input;
		this.output = output;
		this.shared = shared;
	}
	
	public void run()
	{
		if(transCeive)
			Trans();
		else
			receive();
	}
	
	public void Trans()
	{
		
	}
	
	public void receive()
	{
		
	}

}
