package Sender;

public class SignalData {
	final int signalSize = 2;
	final byte[] request =		{ 0,0 }; //요청
	final byte[] response =		{ 0,1 }; //응답함
	
	final byte[] Location = 	{ 1,0 }; //위치
	final byte[] Camera	=		{ 1,1 }; //카메라 프리뷰 이미지
	final byte[] Voice	=		{ 1,2 }; //목소리
	
	final byte[] fileSend = 	{ 2,0 }; //파일보냄
	final byte[] fileReceive=	{ 2,1 }; //파일받음
	
	public boolean signalChecking(byte[] target, byte[] wantChecking)
	{
		for(int i=0; i<signalSize; i++)
		{
			if(target[i] == wantChecking[i])
				continue;
			else
				return false;
		}
		return true;
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