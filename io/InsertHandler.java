package oxygen.io;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.standardizer.DataFormatException;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>实体插入处理器</p>
 * <p>实体插入处理器能够将实体插入到实体文件并建立相应的索引。只有插入实体到实体文件和建立索引都成功，一次插入操作才算是成功，
 * 并返回成功标记。若插入操作在中间某步骤失败，则该处理器将放弃此次插入操作，并将对实体文件和索引的修改回滚到插入前的状态，
 * 并返回失败标记。</p>
 * @author 赖昆
 * @since 1.0, 2007-01-15
 * @version 1.0
 * @param <E> 实体
 */
public class InsertHandler<E extends Entity<E>> extends Handler<E> {
	
	// 要插入的实体
	private final E entity;
	
	// 操作结果
	private final Response<Boolean> response;
	
	InsertHandler( E entity, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entity = entity;
		this.response = response;
	}
	
	// 执行插入操作
	void handle() {
		
		try {
		
			Map<Field, String> indexMap = new HashMap<Field, String>();
			// 标准化实体
			byte[] entityBytes;
			try {
				entityBytes = StandardizerProxy.standardize( entity );
			} catch ( DataFormatException e ) {
				response.setResponse( Boolean.FALSE );
				logger.warning( e.getMessage() );
				return;
			}
			// 记录索引字段
			for ( Field field : table.getFields() ) {
				field.setAccessible( true );
				if ( field.getAnnotation( Index.class ) != null ) {
					Object o = entity.valueOf( field );
					indexMap.put( field, o == null ? null : o.toString() );
				}
			}
			ByteBuffer bb = ByteBuffer.wrap( entityBytes );
			
			// 获得一个空位地址。若没有空位，则写入文件末尾。
			Long address = table.getFree();
			try {
				// 使用文件通道前，要先获得通道锁定。仅仅锁定实体文件的操作部分以获得最大的实体文件处理吞吐率
				// 获取锁定的同时检查要写入的地址是否处于保护状态
				table.lock( address );
				FileChannel channel = table.channel();
				if ( address == null ) {
					address = channel.size();
				}
				// 若此插入操作属于一个事务，则在向实体文件写入数据前，先写入回滚记录
				if ( writer != null ) {
					writer.write( table.name(), address, new byte[ table.size() ] );
				}
				// 将待插入实体保护起来。这样，在完成全部插入操作前，其他线程不能干扰这个实体
				table.protect( address );
				// 记录实体的回滚
				transaction.addRollback( new EntityRollback<E>( table, new byte[ table.size() ], address ) );
				// 写实体文件，实现实体插入
				channel.write( bb, address );
			} catch ( IOException e ) {
				if ( address != null ) {
					table.unprotect( address );
				}
				response.setResponse( Boolean.FALSE );
				logger.warning( res.getResource( "InsertHandler.handle.warning.CannotWriteEntityFile", table.name() ) );
				return;
			} finally {
				table.unlock();
			}
	
			try {
				// 统计索引字段，这些索引将被插入到索引文件和实体映射表
				for ( Field field : indexMap.keySet() ) {
					if ( !IndexManager.insert( field, indexMap.get( field ), address, transaction ) ) {
						response.setResponse( Boolean.FALSE );
						// 解除索引地址的保护，且索引不生效
						IndexManager.unprotect( transaction, false );
						// 回滚
						transaction.rollback();
						logger.warning( res.getResource( "InsertHandler.handle.warning.InsertIndexFailed", table.name(), field.getName() ) );
						return;
					}
				}
				// 解除索引地址的保护，索引生效
				IndexManager.unprotect( transaction, true );
			} finally {
				// 解除实体的保护
				table.unprotect( address );
			}
	
			response.setResponse( Boolean.TRUE );
			
		} catch ( Exception e ) {
			logger.warning( res.getResource( "InsertHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
