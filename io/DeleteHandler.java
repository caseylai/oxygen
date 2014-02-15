package oxygen.io;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashSet;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>实体删除处理器</p>
 * <p>实体删除处理器能删除实体以及相关实体的索引。只有删除实体和索引都成功，删除才算是成功的。若删除在中间某个步骤失败，
 * 则该处理器将放弃本次删除操作，并将对实体文件和索引文件的修改回滚到删除之前，并返回失败。</p>
 * @author 赖昆
 * @since 1.0, 2007-04-03
 * @version 1.0
 * @param <E> 实体
 */
public class DeleteHandler<E extends Entity<E>> extends Handler<E> {
	
	// 要删除的实体
	private final E entity;
	
	// 操作结果
	private final Response<Boolean> response;
	
	DeleteHandler( E entity, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entity = entity;
		this.response = response;
	}
	
	void handle() {
		
		try {
		
			// 获得目标实体的地址
			HashSet<Long> address = new Querist<E>( entity, table ).query();
			// 若找不到待修改目标实体，则直接返回
			if ( address == null ) {
				response.setResponse( Boolean.FALSE );
				return;
			} else if ( address.size() == 0 ) {
				response.setResponse( Boolean.TRUE );
				return;
			}
			
			// 删除目标地址处的实体
			ByteBuffer bb = ByteBuffer.allocate( table.size() );
			bb.putLong( 0L ).putLong( 0L ).rewind();
			ByteBuffer old = ByteBuffer.allocate( table.size() );
			Collection<Long> protectedPosition = new HashSet<Long>();
			try {
				table.lock( address );
				FileChannel channel = table.channel();
				for ( long position : address ) {
					// 保护待操作的地址
					table.protect( position );
					protectedPosition.add( position );
					try {
						// 读出地址的原有实体,之后添加到回滚队列
						channel.read( old, position );
						transaction.addRollback( new EntityRollback<E>( table, old.array(), position ) );
						// 若此删除操作属于一个事务，则在向实体文件写入数据前，先写入回滚记录
						if ( writer != null ) {
							writer.write( table.name(), position, old.array() );
						}
						old.clear();
						// 删除实体
						channel.write( bb, position );
						bb.rewind();
					} catch ( IOException e ) {
						response.setResponse( Boolean.FALSE );
						transaction.rollback();
						for ( Long pos : protectedPosition ) {
							table.unprotect( pos );
						}
						logger.warning( res.getResource( "DeleteHandler.handle.warning.DeleteFromEntityFileFailed", table.name(), String.valueOf( position ) ) );
						return;
					}
					// 将删除后的实体地址记录入查找表的空位集合
					table.putFree( position );
				}
			} finally {
				table.unlock();
			}
			
			// 删除与实体相关的索引
			try {
				// 枚举带索引的字段
				for ( Field field : entity.getClass().getDeclaredFields() ) {
					field.setAccessible( true );
					if ( field.getAnnotation( Index.class ) != null ) {
						// 枚举各个地址
						for ( long position : address ) {
							if ( !IndexManager.delete( field, position, transaction ) ) {
								response.setResponse( Boolean.FALSE );
								IndexManager.unprotect( transaction, false );
								transaction.rollback();
								logger.warning( res.getResource( "DeleteHandler.handle.warning.DeleteIndexFailed", table.name(), field.getName() ) );
								return;
							}
						}
					}
				}
				// 解除索引地址的保护，索引生效
				IndexManager.unprotect( transaction, true );
			} finally {
				// 解除各个实体的保护
				for ( long pos : protectedPosition ) {
					table.unprotect( pos );
				}
			}

			response.setAddressSet( address );
			response.setResponse( Boolean.TRUE );
			
		} catch ( Exception e ) {
			logger.warning( res.getResource( "DeleteHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
