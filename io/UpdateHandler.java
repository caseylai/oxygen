package oxygen.io;

import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import oxygen.entity.Entity;
import oxygen.entity.annotation.constraint.Index;
import oxygen.io.standardizer.DataFormatException;
import oxygen.io.standardizer.StandardizerProxy;
import oxygen.io.transaction.RollbackFileWriter;


/**
 * <p>实体修改处理器</p>
 * <p>实体修改处理器进行实体及其相关索引的修改操作。只有实体以及索引都修改成功，一次修改才算是成功的。若在修改
 * 中间出现了异常而修改失败的话，则修改处理器将放弃本次修改操作，而将对实体文件和索引文件的修改回滚到修改之前的
 * 状态。</p>
 * @author 赖昆
 * @since 1.0, 2007-04-06
 * @version 1.0
 * @param <E> 实体
 */
public class UpdateHandler<E extends Entity<E>> extends Handler<E> {
	
	// 要更新的实体
	private final E entitySrc;
	
	// 更新到的实体
	private final E entityDest;
	
	// 操作结果
	private final Response<Boolean> response;
	
	UpdateHandler( E entitySrc, E entityDest, LookupTable<E> table, Response<Boolean> response, RollbackFileWriter writer ) {
		super( table, writer );
		this.entitySrc = entitySrc;
		this.entityDest = entityDest;
		this.response = response;
	}
	
	void handle() {
		
		try {
		
			// 获得待修改目标实体的地址
			HashSet<Long> address = new Querist<E>( entitySrc, table ).query();
			// 若找不到待修改目标实体，则直接返回
			if ( address == null ) {
				response.setResponse( Boolean.FALSE );
				return;
			} else if ( address.size() == 0 ) {
				response.setResponse( Boolean.TRUE );
				return;
			}
			
			// 所有不为null的字段和特殊处理的null值字段都是待修改的字段
			Map<Field, Integer> offsetMap = new HashMap<Field, Integer>();
			Map<Field, String> indexMap = new HashMap<Field, String>();
			Collection<Field> validFieldSet = new HashSet<Field>();
			for ( Field field : entityDest.getClass().getDeclaredFields() ) {
				field.setAccessible( true );
				offsetMap.put( field, table.offset( field ) );
				Object o = entityDest.valueOf( field );
				if ( o != null || entityDest.isSpecialNull( field.getName() ) ) {
					validFieldSet.add( field );
					// 提取待修改字段中的索引字段，这些索引都是即将被修改的
					if ( field.getAnnotation( Index.class ) != null ) {
						indexMap.put( field, o == null ? null : o.toString() );
					}
				}
			}
	
			// 修改目标实体
			ByteBuffer old = ByteBuffer.allocate( table.size() );
			Collection<Long> protectedPosition = new HashSet<Long>();
			try {
				table.lock( address );
				FileChannel channel = table.channel();
				for ( long position : address ) {
					table.protect( position );
					protectedPosition.add( position );
					try {
						// 读出目标实体的数据，记录入回滚队列
						channel.read( old, position );
						transaction.addRollback( new EntityRollback<E>( table, old.array(), position ) );
						// 若此修改操作属于一个事务，则在向实体文件写入数据前，先写入回滚记录
						if ( writer != null ) {
							writer.write( table.name(), position, old.array() );
						}
						// 改写目标实体的数据，并写回到实体文件
						for ( Field field : validFieldSet ) {
							old.position( offsetMap.get( field ) );
							// 在这里要对待修改的值进行标准化处理
							try {
								byte[] b = StandardizerProxy.standardize( entityDest, field );
								old.put( b, 0, b.length );
							} catch ( DataFormatException e  ) {
								response.setResponse( Boolean.FALSE );
								transaction.rollback();
								for ( Long pos : protectedPosition ) {
									table.unprotect( pos );
								}
								logger.warning( e.getMessage() );
								return;
							}
						}
						old.rewind();
						channel.write( old, position );
						old.clear();
					} catch ( IOException e ) {
						response.setResponse( Boolean.FALSE );
						transaction.rollback();
						for ( Long pos : protectedPosition ) {
							table.unprotect( pos );
						}
						logger.warning( res.getResource( "UpdateHandler.handle.warning.UpdateFromEntityFileFailed", table.name(), String.valueOf( position ) ) );
						return;
					}
				}
			} finally {
				table.unlock();
			}
			
			// 修改实体相关的索引
			try {
				// 枚举带索引的字段
				for ( long position : address ) {
					for ( Field field : indexMap.keySet() ) {
						if ( !IndexManager.update( field, indexMap.get( field ), position, transaction ) ) {
							response.setResponse( Boolean.FALSE );
							IndexManager.unprotect( transaction, false );
							transaction.rollback();
							logger.warning( res.getResource( "UpdateHandler.handle.warning.UpdateIndexFailed", table.name(), field.getName() ) );
							return;
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
			logger.warning( res.getResource( "UpdateHandler.handle.warning.UnknownExceptionThrowing" ) );
			e.printStackTrace();
			response.setResponse( Boolean.FALSE );
		}
	}
}
