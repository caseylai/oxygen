package oxygen.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.standardizer.DataFormatException;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.standardizer.TypeSizeDefiner;
import oxygen.util.i18n.ResourceLoader;
import oxygen.util.i18n.ResourceLoaderProvider;


/**
 * 实体查询者。由于查询、删除、修改操作都需要用到实体的查询，故将查询部分提取出来单独成一个类。
 * @author 赖昆
 * @since 1.0, 2007-04-01
 * @version 1.0
 */
public class Querist<E extends Entity<E>> {

	// 要查询的实体
	private final E entity;
	
	// 实体查找表
	private final LookupTable<E> table;
	
	protected static final Logger logger = Logger.getLogger( Querist.class.getName() );
	
	protected static final ResourceLoader res = ResourceLoaderProvider.provide( Querist.class );
	
	Querist( E entity, LookupTable<E> table ) {
		this.entity = entity;
		this.table = table;
	}
	
	// 执行查询，返回符合条件的地址集合
	HashSet<Long> query() {
		
		// 提取不为null的字段和需要特殊处理null的字段作为查询条件
		Map<Field, Object> condition = new HashMap<Field, Object>();
		for ( Field field : table.getFields() ) {
			Object o = entity.valueOf( field );
			if ( o != null || entity.isSpecialNull( field.getName() ) ) {
				condition.put( field, o == null ? null : o );
			}
		}

		// 保存查询结果的地址表
		HashSet<Long> addressSet = new HashSet<Long>();
		if ( condition.size() == 0 ) {
			// 无查询条件，则选取数据库中所有实体
			long size = 0L;
			long length = table.size();
			try {
				table.lock();
				FileChannel channel = table.channel();
				size = channel.size();
			} catch ( IOException e ) {
				logger.warning( res.getResource( "Querist.query.warning.CannotGetEntityFileLength", table.name() ) );
			} finally {
				table.unlock();
			}
			for ( long i = 0 ; i < size ; i += length ) {
				addressSet.add( i );
			}
		} else {
			for ( Field field : condition.keySet() ) {
				HashSet<Long> tempAddressSet = new HashSet<Long>();
				field.setAccessible( true );
				if ( field.getAnnotation( Index.class ) == null ) {
					// 条件字段不是索引字段
					try {
						byte[] byteCode = StandardizerProxy.standardize( entity, field );
						table.lock();
						FileChannel channel = table.channel();
						long fileLength = channel.size();
						int entitySize = table.size();
						int fieldSize = TypeSizeDefiner.define( field );
						int offset = table.offset( field );
						ByteBuffer bb = ByteBuffer.allocate( fieldSize );
						// 这个循环写在锁的内部十分影响性能，但目前没有想出更好的解决方案
						for ( long position = offset ; position < fileLength ; position += entitySize ) {
							channel.read( bb, position );
							if ( Arrays.equals( byteCode, bb.array() ) ) {
								tempAddressSet.add( position - offset );
							}
							bb.clear();
						}
					} catch ( DataFormatException e ) {
						logger.warning( e.getMessage() );
						return null;
					} catch ( IOException e ) {
						logger.warning( res.getResource( "Querist.query.warning.CannotReadEntityFile", table.name() ) );
						return null;
					} finally {
						table.unlock();
					}
				} else {
					// 条件字段是索引字段
					tempAddressSet = IndexManager.query( field, condition.get( field ).toString() );
				}
				// 若某字段的查询结果为空集，则整个查询的结果也为空集
				if ( tempAddressSet.size() == 0 ) {
					return new HashSet<Long>();
				} else if ( addressSet.size() == 0 ) {
					addressSet = tempAddressSet;
				} else {
					addressSet.retainAll( tempAddressSet );
				}
			}
		}
		return addressSet;
	}
}
