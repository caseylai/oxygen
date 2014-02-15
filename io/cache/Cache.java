package oxygen.io.cache;

import java.util.HashSet;
import java.util.List;

import oxygen.entity.Entity;

/**
 * 实体-结果列表缓冲
 * @author 赖昆
 * @since 1.0, 2007-05-12
 * @version 1.0
 * @param <E> 实体
 */
public class Cache<E extends Entity<E>> implements Comparable<Cache<E>> {
	
	// 作为键的实体
	private final E keyEntity;
	
	// 作为值的结果列表
	private final List<E> resultList;
	
	// 结果集对应的地址集合
	private final HashSet<Long> addressSet;
	
	// 此缓冲的开始时间
	private final long startTime = System.nanoTime();
	
	// 此缓冲的调用次数
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

	// 得到该缓冲的使用频率
	double getFrequency( long endTime ) {
		return counter / ( endTime - startTime );
	}
	
	// 增加一次缓冲调用次数
	void count() {
		counter++;
	}

	/**
	 * 按使用频率进行排序
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
