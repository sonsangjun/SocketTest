

import java.net.Socket;

public class ByteArrayTransCeiverRule {
	int counter = 0;				//파일 분할 횟수
	int fileSize = 0;				//클라이언트간 통신을 위해 선언(파일사이즈)	
	int extra = 0;					//파일의 마지막 분할부분의 남은부분(인덱스경계초과 예외 방지)
	int fileUnitSize = 16384;		//파일 분할 단위는 8KByte
	
	//서버 끝
	
	public void Calc(int fileSize)
	{
		counter = fileSize/fileUnitSize;
		extra = fileSize%fileUnitSize;		
	}
}
