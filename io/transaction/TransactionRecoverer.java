package oxygen.io.transaction;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.EntityProvider;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.IndexManager;
import oxygen.io.LookupTable;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.standardizer.TypeSizeDefiner;


/**
 * <p>事务恢复器</p>
 * <p>事务恢复器可以根据事务的回滚日志，将事务对数据文件的更改恢复到事务发生之前。</p>
 * @author 赖昆
 * @since 1.0, 2007-05-05
 * @version 1.0
 */
public class TransactionRecoverer {

	// 回滚文件
	private final File rollbackFile;
		
	// 当事务被强制中断（电源故障、死机等硬中断），数据库在重启后对事务进行恢复时，使用文件构造恢复器
	public TransactionRecoverer( File rollbackFile ) {
		this.rollbackFile = rollbackFile;
	}
	
	// 当事务执行中途发生异常（软中断），使用回滚文件记录器构造恢复器
	TransactionRecoverer( RollbackFileWriter writer ) {
		rollbackFile = writer.getRollbackFile();
	}
	
	// 是否是空实体（标准化后的字节数组为全\u0000）
	private static boolean isEmptyEntity( byte[] bytes ) {
		for ( byte b : bytes ) {
			if ( b != (byte) 0 ) return false;
		}
		return true;
	}
	
	// 从实体的字节数组中获得字段的字符串值
	private static String getFieldString( Field field, byte[] b, Map<Field, Integer> offsetMap ) {
		
		int fieldSize = TypeSizeDefiner.define( field );
		byte[] byteCode = new byte[ fieldSize ];
		System.arraycopy( b, offsetMap.get( field ), byteCode, 0, fieldSize );
		Object o = StandardizerProxy.unstandardize( byteCode, (Class<?>) field.getGenericType() );
		return o == null ? null : o.toString();
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * 进行恢复。若恢复成功，回滚文件将被删除；反之回滚文件将继续保留
	 */
	public void recover() {
		
		try {
			// 先按字节数组形式读入回滚文件
			FileChannel channel = new RandomAccessFile( rollbackFile, "rw" ).getChannel();
			ByteBuffer bb = ByteBuffer.allocate( (int) rollbackFile.length() );
			while ( bb.position() < bb.limit() ) {
				channel.read( bb );
			}
			channel.close();
			
			// 将字节数组解开为回滚记录，对其进行恢复
			Collection<LookupTable<? extends Entity>> lockedTableSet = new HashSet<LookupTable<? extends Entity>>();
			try {
				for ( RollbackRecord record : RollbackRecord.toRecords( bb.array() ) ) {

					String entityName = record.getEntityName();
					long address = record.getAddress();
					byte[] bytes = record.getBytes();
					
					
					// 根据实体名获取查找表，并获取查找表的锁定
					LookupTable<? extends Entity> table = LookupTable.getLookupTable( entityName );
					if ( !lockedTableSet.contains( table ) ) {
						table.use();
						lockedTableSet.add( table );
						table.lock( address );
					}
					
					FileChannel fc = table.channel();
					// 读出即将被覆盖的实体
					ByteBuffer old = ByteBuffer.allocate( table.size() );
					fc.read( old, address );
					byte[] oldBytes = old.array();
					// 覆写实体
					ByteBuffer buffer = ByteBuffer.wrap( bytes );
					fc.write( buffer, address );

					// 准备索引
					Map<Field, Integer> indexMap = new HashMap<Field, Integer>();
					Entity entity = EntityProvider.provide( entityName );
					for ( Field field : entity.getClass().getDeclaredFields() ) {
						if ( field.getAnnotation( Index.class ) != null ) {
							indexMap.put( field, table.offset( field ) );
						}
					}

					// 处理空位集合和索引
					if ( isEmptyEntity( bytes ) ) {
						// 若待回滚的实体字节数组为空，表明这是一个添加操作的失败回滚，添加地址到空位集合
						table.putFree( address );
						// 删除索引
						for ( Field field : indexMap.keySet() ) {
							if ( !IndexManager.delete( field, address, null ) ) {
								// 若删除失败，直接返回，这样不删除回滚文件
								return;
							}
						}
					} else if ( isEmptyEntity( oldBytes ) ) {
						// 若原始实体的字节数组为空，表明这是一个删除操作的失败回滚，从空位集合删除此地址
						table.removeFree( address );
						// 添加索引
						for ( Field field : indexMap.keySet() ) {
							String key = getFieldString( field, bytes, indexMap );
							if ( !IndexManager.insert( field, key, address, null ) ) {
								return;
							}
						}
					} else {
						// 修改操作。不修改空位集合，仅更新索引
						for ( Field field : indexMap.keySet() ) {
							String key = getFieldString( field, bytes, indexMap );
							if ( !IndexManager.update( field, key, address, null ) ) {
								return;
							}
						}
					}
				}
			} finally {
				// 统一解锁
				for ( LookupTable<? extends Entity> table : lockedTableSet ) {
					table.unlock();
					table.away();
				}
			}
			
			// 恢复顺利完成，删除回滚文件
			rollbackFile.delete();
			
		} catch ( IOException e ) {
			// 有异常抛出，表示恢复失败，不作处理，但没有删除回滚文件
		}
	}
}
