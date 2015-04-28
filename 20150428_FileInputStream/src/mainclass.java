import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class mainclass{

	public static void main(String[] args) {
		FileInputStream fileinput;
		FileOutputStream fileoutput;
		BufferedOutputStream filesender;
		try {
			//서버에서 파일보내기에 앞서 파일스트림 테스트를 해본다.
			//available()메소드는 남아있는 파일사이즈는 Byte크기로 반환한다. 아예안읽었을경우 크기는 정확히 일치했다.
			//예외처리로는 IO익셉션처리다.
			//테스트한 이미지 파일은 정확히 521981Byte이다. (509KB 디스크 할당크기는 이보다 더크다.)
			fileinput = new FileInputStream("C:\\Users\\sohn\\Documents\\1406723231578.jpeg");
			fileoutput = new FileOutputStream("C:\\Users\\sohn\\Documents\\javafile.jpeg");
			filesender = new BufferedOutputStream(fileoutput);
			
			System.out.println("아직 읽어야할 크기 : " +fileinput.available()+" Byte");
			System.out.println("10초후에 파일을 복사합니다. 복사된 파일명은 javafile.jpeg");
			Thread.sleep(10000);			
			
			System.out.println("파일 복사를 시작합니다.");
			byte[] filebag = new byte[fileinput.available()];
			fileinput.read(filebag);
			filesender.write(filebag);
			
			System.out.println("5초후 버퍼 아웃풋 스트림을 닫습니다.");
			Thread.sleep(5000);
			
			filesender.close();	//버퍼 아웃풋 스트림을 닫을수 있는건 슈퍼클래스 filteroutputstream 클래스 때문
			System.out.println("5초후 파일 아웃풋 스트림을 닫습니다.");
			Thread.sleep(5000);	//그 필터아웃풋스트림 클래스를 닫으면 flush()호출해서 버퍼 내용물을 파일아웃풋스트림에 뿌린다.
			
			fileoutput.close();
			System.out.println("5초후 파일 인풋 스트림을 닫습니다.");
			Thread.sleep(5000);
			
			fileinput.close();
		} catch (FileNotFoundException e) {
			System.out.println("파일없슴"+e.getMessage());
		} catch (IOException e) {
			System.out.println("남아있는 크기를 알수 없습니다. "+e.getMessage());
		} catch (InterruptedException e) {
			System.out.println("스레드가 자질 못합니다."+e.getMessage());
		}
	}
}