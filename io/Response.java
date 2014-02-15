package oxygen.io;

import java.util.HashSet;

/**
 * 用于保存数据库操作的答复
 * @author 赖昆
 * @since 1.0, 2007-01-18
 * @version 1.0
 * @param <T> 集合类（查询）或者布尔类（插入、删除、修改）
 */
public class Response<T> {
	
	// 答复
	private T response;
	
	// 操作涉及的地址集合
	private HashSet<Long> addressSet;
	
	// 是否空答复
	private volatile boolean isNullResponse = false;
	
	Response() {}
	
	// 获得答复。若数据库操作尚未结束，则此方法将阻塞，直到数据库操作为止
	synchronized T getResponse() {
		try {
			while ( response == null && !isNullResponse ) {
				wait();
			}
		} catch ( InterruptedException e ) {
		}
		return response;
	}
	
	// 设置答复
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
