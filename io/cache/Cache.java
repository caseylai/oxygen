package oxygen.io.cache;

import java.util.HashSet;
import java.util.List;

import oxygen.entity.Entity;

/**
 * ʵ��-����б���
 * @author ����
 * @since 1.0, 2007-05-12
 * @version 1.0
 * @param <E> ʵ��
 */
public class Cache<E extends Entity<E>> implements Comparable<Cache<E>> {
	
	// ��Ϊ����ʵ��
	private final E keyEntity;
	
	// ��Ϊֵ�Ľ���б�
	private final List<E> resultList;
	
	// �������Ӧ�ĵ�ַ����
	private final HashSet<Long> addressSet;
	
	// �˻���Ŀ�ʼʱ��
	private final long startTime = System.nanoTime();
	
	// �˻���ĵ��ô���
	private int counter = 0;
	
	Cache( E keyEntity, List<E> resultList, HashSet<Long> addressSet ) {
		this.keyEntity = keyEntity;
		this.resultList = resultList;
		this.addressSet = addressSet;
	}
	
	HashSet<Long> getAddressSet() {
		return addressSet;
	}

	E getKeyEntity() {
		return keyEntity;
	}

	List<E> getResultList() {
		return resultList;
	}

	// �õ��û����ʹ��Ƶ��
	double getFrequency( long endTime ) {
		return counter / ( endTime - startTime );
	}
	
	// ����һ�λ�����ô���
	void count() {
		counter++;
	}

	/**
	 * ��ʹ��Ƶ�ʽ�������
	 */
	public int compareTo( Cache<E> cache ) {
		
		long endTime = System.nanoTime();
		double f1 = getFrequency( endTime );
		double f2 = cache.getFrequency( endTime );
		if ( f1 < f2 ) {
			return -1;
		} else if ( f1 > f2 ) {
			return 1;
		}
		return 0;
	}
}
