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
 * ʵ���ѯ�ߡ����ڲ�ѯ��ɾ�����޸Ĳ�������Ҫ�õ�ʵ��Ĳ�ѯ���ʽ���ѯ������ȡ����������һ���ࡣ
 * @author ����
 * @since 1.0, 2007-04-01
 * @version 1.0
 */
public class Querist<E extends Entity<E>> {

	// Ҫ��ѯ��ʵ��
	private final E entity;
	
	// ʵ����ұ�
	private final LookupTable<E> table;
	
	protected static final Logger logger = Logger.getLogger( Querist.class.getName() );
	
	protected static final ResourceLoader res = ResourceLoaderProvider.provide( Querist.class );
	
	Querist( E entity, LookupTable<E> table ) {
		this.entity = entity;
		this.table = table;
	}
	
	// ִ�в�ѯ�����ط��������ĵ�ַ����
	HashSet<Long> query() {
		
		// ��ȡ��Ϊnull���ֶκ���Ҫ���⴦��null���ֶ���Ϊ��ѯ����
		Map<Field, Object> condition = new HashMap<Field, Object>();
		for ( Field field : table.getFields() ) {
			Object o = entity.valueOf( field );
			if ( o != null || entity.isSpecialNull( field.getName() ) ) {
				condition.put( field, o == null ? null : o );
			}
		}

		// �����ѯ����ĵ�ַ��
		HashSet<Long> addressSet = new HashSet<Long>();
		if ( condition.size() == 0 ) {
			// �޲�ѯ��������ѡȡ���ݿ�������ʵ��
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
					// �����ֶβ��������ֶ�
					try {
						byte[] byteCode = StandardizerProxy.standardize( entity, field );
						table.lock();
						FileChannel channel = table.channel();
						long fileLength = channel.size();
						int entitySize = table.size();
						int fieldSize = TypeSizeDefiner.define( field );
						int offset = table.offset( field );
						ByteBuffer bb = ByteBuffer.allocate( fieldSize );
						// ���ѭ��д�������ڲ�ʮ��Ӱ�����ܣ���Ŀǰû��������õĽ������
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
					// �����ֶ��������ֶ�
					tempAddressSet = IndexManager.query( field, condition.get( field ).toString() );
				}
				// ��ĳ�ֶεĲ�ѯ���Ϊ�ռ�����������ѯ�Ľ��ҲΪ�ռ�
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
