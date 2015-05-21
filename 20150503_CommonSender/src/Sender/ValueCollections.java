package Sender;

//20150517
//자주 쓰는 상수들을 모아놓았습니다.
//자주쓰는걸 이렇게 관리하니 편하군요.
public class ValueCollections {
	final String upLine = new String("──────────────────────────────");
	final String downLine = new String("──────────────────────────────");
	
	final String unname = new String("unname");	//방에 참여안했을때, unname으로 할당
	final String ServerIP = "221.156.9.145";	//외부에서 테스트할때 IP
	//final String ServerIP = "192.168.0.3";	//사설IP 서버
	final int waitTime = 100;					//정말 기다려야 하는 시간
	final int coolTime = 10000;					//위치 정보등을 받는 주기(interval)
	final int portNum = 9000;
	final double basicLatitude = 35.159773;		//기본 위도
	final double BasicLongitude = 126.851568;	//기본 경도
	
	final boolean _Server = false;				//코드가 서버로 작동하는 경우 true
	final String fileName = "Test.jpeg";		//카메라 프리뷰 이미지 전송이 파일 전송과 비슷하므로(테스트 변수) 
}
