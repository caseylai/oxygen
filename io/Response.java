package oxygen.io;

import java.util.HashSet;

/**
 * ���ڱ������ݿ�����Ĵ�
 * @author ����
 * @since 1.0, 2007-01-18
 * @version 1.0
 * @param <T> �����ࣨ��ѯ�����߲����ࣨ���롢ɾ�����޸ģ�
 */
public class Response<T> {
	
	// ��
	private T response;
	
	// �����漰�ĵ�ַ����
	private HashSet<Long> addressSet;
	
	// �Ƿ�մ�
	private volatile boolean isNullResponse = false;
	
	Response() {}
	
	// ��ô𸴡������ݿ������δ��������˷�����������ֱ�����ݿ����Ϊֹ
	synchronized T getResponse() {
		try {
			while ( response == null && !isNullResponse ) {
				wait();
			}
		} catch ( InterruptedException e ) {
		}
		return response;
	}
	
	// ���ô�
	synchronized void setResponse( T response ) {
		this.response = response;
		if ( response == null ) {
			isNullResponse = true;
		}
		notifyAll();
	}
	
	void setAddressSet( HashSet<Long> addressSet ) {
		this.addressSet = addressSet;
	}
	
	HashSet<Long> getAddressSet() {
		return addressSet;
	}
}
